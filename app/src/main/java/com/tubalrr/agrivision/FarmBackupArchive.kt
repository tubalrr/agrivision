package com.tubalrr.agrivision

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class StagedArchiveEvidence(
    val entryPath: String,
    val file: File,
    val sha256: String,
    val sizeBytes: Long
)

internal data class ReadFarmBackup(
    val json: JSONObject,
    val isArchive: Boolean,
    val stagingDirectory: File?,
    val entries: Map<String, StagedArchiveEvidence>
)

internal data class FarmBackupExportSummary(
    val incidentPhotosIncluded: Int,
    val photoBytesIncluded: Long
)

/**
 * The ZIP format stores the JSON manifest alongside the original incident photos.
 * Reading is streamed to private cache files with strict size and path limits so a
 * malformed archive cannot write outside its staging directory or exhaust memory.
 */
internal object FarmBackupArchive {
    const val ARCHIVE_VERSION = 1
    const val MANIFEST_ENTRY = "backup.json"
    private const val PHOTO_PREFIX = "photos/"
    private const val MAX_PHOTO_BYTES = 12L * 1024L * 1024L
    private const val MAX_TOTAL_PHOTO_BYTES = 250L * 1024L * 1024L
    private const val MAX_MANIFEST_BYTES = 64L * 1024L * 1024L
    private const val MAX_ARCHIVE_ENTRIES = 10_001
    private const val BUFFER_SIZE = 32 * 1024

    fun export(
        context: Context,
        destination: Uri,
        json: JSONObject,
        incidents: List<FieldIncident>
    ): FarmBackupExportSummary {
        val photos = mutableListOf<ExportPhoto>()
        var totalBytes = 0L

        incidents.filter { it.evidenceUri.isNotBlank() }.forEachIndexed { index, incident ->
            val uri = runCatching { Uri.parse(incident.evidenceUri) }.getOrNull()
                ?: error("Invalid photo reference for incident ${incident.id}.")
            val info = runCatching { digestEvidence(context, uri) }.getOrElse {
                throw IllegalStateException(
                    "Cannot export the backup because the attached photo for incident ${incident.id} is unavailable. Reattach the photo and try again.",
                    it
                )
            }
            require(info.sizeBytes > 0L) { "Incident ${incident.id} has an empty evidence photo." }
            require(info.sizeBytes <= MAX_PHOTO_BYTES) {
                "Incident ${incident.id} has a photo larger than 12 MB. Replace it with a smaller image before exporting."
            }
            totalBytes += info.sizeBytes
            require(totalBytes <= MAX_TOTAL_PHOTO_BYTES) {
                "Incident photos exceed the 250 MB backup limit. Export fewer photo attachments at a time."
            }
            val path = PHOTO_PREFIX + index + "_" + UUID.randomUUID().toString().replace("-", "") + ".bin"
            photos += ExportPhoto(
                incidentId = incident.id,
                sourceUri = uri,
                entryPath = path,
                sha256 = info.sha256,
                sizeBytes = info.sizeBytes,
                mimeType = info.mimeType
            )
        }

        val photoManifest = JSONArray()
        photos.forEach { photo ->
            photoManifest.put(JSONObject().apply {
                put("incidentId", photo.incidentId)
                put("entryPath", photo.entryPath)
                put("sha256", photo.sha256)
                put("sizeBytes", photo.sizeBytes)
                put("mimeType", photo.mimeType)
            })
        }
        json.put("archiveVersion", ARCHIVE_VERSION)
        json.put("exportedAt", System.currentTimeMillis())
        json.put("incidentEvidence", photoManifest)

        val tempArchive = File(context.cacheDir, "agrivision-backup-${UUID.randomUUID()}.zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempArchive))).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                photos.forEach { photo ->
                    zip.putNextEntry(ZipEntry(photo.entryPath))
                    val digest = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    openEvidenceInput(context, photo.sourceUri).use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            require(copied <= MAX_PHOTO_BYTES) {
                                "Incident photo changed during backup and now exceeds 12 MB."
                            }
                            digest.update(buffer, 0, count)
                            zip.write(buffer, 0, count)
                        }
                    }
                    zip.closeEntry()
                    val currentHash = digest.digest().toHex()
                    require(copied == photo.sizeBytes && currentHash == photo.sha256) {
                        "Incident photo changed while the backup was being created. Please export again."
                    }
                }
            }

            val output = context.contentResolver.openOutputStream(destination)
                ?: error("Unable to open the selected backup destination.")
            output.use { sink ->
                FileInputStream(tempArchive).use { source -> source.copyTo(sink, BUFFER_SIZE) }
            }
        } finally {
            tempArchive.delete()
        }

        return FarmBackupExportSummary(photos.size, totalBytes)
    }

    fun read(context: Context, sourceUri: Uri): ReadFarmBackup {
        val input = context.contentResolver.openInputStream(sourceUri)
            ?: error("Unable to open the selected backup file.")
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val signature = ByteArray(4)
        var readCount = 0
        while (readCount < signature.size) {
            val count = buffered.read(signature, readCount, signature.size - readCount)
            if (count < 0) break
            readCount += count
        }
        buffered.reset()
        val looksLikeZip = readCount >= 2 &&
            signature[0] == 0x50.toByte() && signature[1] == 0x4B.toByte()

        return if (looksLikeZip) readArchive(buffered) else {
            val bytes = buffered.use { readLimited(it, MAX_MANIFEST_BYTES) }
            ReadFarmBackup(
                json = JSONObject(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")),
                isArchive = false,
                stagingDirectory = null,
                entries = emptyMap()
            )
        }
    }

    fun copyEvidenceIntoPrivateStorage(context: Context, staged: StagedArchiveEvidence, mimeType: String): Uri {
        require(staged.file.isFile && staged.file.length() == staged.sizeBytes) {
            "A staged incident photo is missing or has changed."
        }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z0-9]{2,5}")) }
            ?: "img"
        val directory = File(context.filesDir, "incident-evidence")
        check(directory.isDirectory || directory.mkdirs()) { "Unable to create private photo storage." }
        val target = File(directory, "evidence_${UUID.randomUUID()}.$extension")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            FileInputStream(staged.file).use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_PHOTO_BYTES) { "Incident photo exceeds the allowed size." }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(copied == staged.sizeBytes && digest.digest().toHex() == staged.sha256) {
                "Incident photo failed its integrity check during restore."
            }
            check(target.length() > 0L) { "Restored incident photo is empty." }
            return Uri.fromFile(target)
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    fun isEvidenceReadable(context: Context, rawUri: String): Boolean {
        if (rawUri.isBlank()) return true
        return runCatching {
            openEvidenceInput(context, Uri.parse(rawUri)).use { it.read() >= 0 }
        }.getOrDefault(false)
    }

    fun cleanupStaging(archive: ReadFarmBackup?) {
        archive?.stagingDirectory?.deleteRecursively()
    }

    fun cleanupUnreferencedEvidence(context: Context, keepUris: Set<String>) {
        val directory = File(context.filesDir, "incident-evidence")
        if (!directory.isDirectory) return
        val keepPaths = keepUris.mapNotNull { raw ->
            runCatching { Uri.parse(raw).path }.getOrNull()
        }.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in keepPaths) file.delete()
        }
    }

    fun deletePrivateEvidence(context: Context, uri: String) {
        val path = runCatching { Uri.parse(uri).path }.getOrNull() ?: return
        val evidenceDirectory = File(context.filesDir, "incident-evidence").canonicalFile
        val target = runCatching { File(path).canonicalFile }.getOrNull() ?: return
        if (target.parentFile == evidenceDirectory) target.delete()
    }

    private fun readArchive(input: InputStream): ReadFarmBackup {
        val staging = File(context.cacheDir, "agrivision-restore-${UUID.randomUUID()}")
        check(staging.isDirectory || staging.mkdirs()) { "Unable to prepare a private restore workspace." }
        val extracted = linkedMapOf<String, StagedArchiveEvidence>()
        var manifestBytes: ByteArray? = null
        var totalPhotoBytes = 0L
        var entryCount = 0

        try {
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    require(entryCount <= MAX_ARCHIVE_ENTRIES) { "Backup has too many files." }
                    require(isSafeArchivePath(entry.name)) { "Backup contains an unsafe file path." }

                    if (entry.isDirectory) {
                        require(entry.name == PHOTO_PREFIX) { "Backup contains an unexpected directory." }
                        zip.closeEntry()
                        continue
                    }

                    when {
                        entry.name == MANIFEST_ENTRY -> {
                            require(manifestBytes == null) { "Backup contains more than one manifest." }
                            manifestBytes = readLimited(zip, MAX_MANIFEST_BYTES)
                        }
                        entry.name.startsWith(PHOTO_PREFIX) -> {
                            require(entry.name.matches(Regex("photos/[A-Za-z0-9_.-]+\\.bin"))) {
                                "Backup contains an invalid photo entry name."
                            }
                            require(!extracted.containsKey(entry.name)) { "Backup contains duplicate photo paths." }
                            val target = File(staging, "photo_${extracted.size}.bin")
                            val digest = MessageDigest.getInstance("SHA-256")
                            var copied = 0L
                            FileOutputStream(target).use { output ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    copied += count
                                    require(copied <= MAX_PHOTO_BYTES) { "A backup photo exceeds the 12 MB limit." }
                                    totalPhotoBytes += count
                                    require(totalPhotoBytes <= MAX_TOTAL_PHOTO_BYTES) {
                                        "Backup photos exceed the 250 MB restore limit."
                                    }
                                    digest.update(buffer, 0, count)
                                    output.write(buffer, 0, count)
                                }
                                output.fd.sync()
                            }
                            extracted[entry.name] = StagedArchiveEvidence(
                                entryPath = entry.name,
                                file = target,
                                sha256 = digest.digest().toHex(),
                                sizeBytes = copied
                            )
                        }
                        else -> error("Backup contains an unsupported entry: ${entry.name}")
                    }
                    zip.closeEntry()
                }
            }

            val bytes = manifestBytes ?: error("Backup archive is missing backup.json.")
            val json = JSONObject(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
            return ReadFarmBackup(json, true, staging, extracted.toMap())
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    private fun isSafeArchivePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\") ||
            path.contains('\\') || path.contains('\u0000') || path.split('/').any { it == ".." || it == "." }
        ) return false
        return path == MANIFEST_ENTRY || path == PHOTO_PREFIX ||
            (path.startsWith(PHOTO_PREFIX) && path.count { it == '/' } == 1)
    }

    private fun readLimited(input: InputStream, limit: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Backup file exceeds the allowed size." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun digestEvidence(context: Context, uri: Uri): EvidenceDigest {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        openEvidenceInput(context, uri).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_PHOTO_BYTES) { "Incident photo exceeds 12 MB." }
                digest.update(buffer, 0, count)
            }
        }
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: mimeFromPath(uri.path.orEmpty())
        return EvidenceDigest(digest.digest().toHex(), total, mime)
    }

    private fun mimeFromPath(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path).lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun openEvidenceInput(context: Context, uri: Uri): InputStream {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = uri.path ?: error("Photo file path is missing.")
            return FileInputStream(File(path))
        }
        if (uri.scheme.isNullOrBlank()) return FileInputStream(File(uri.toString()))
        return context.contentResolver.openInputStream(uri) ?: error("Cannot open incident photo.")
    }

    private data class EvidenceDigest(val sha256: String, val sizeBytes: Long, val mimeType: String)

    private data class ExportPhoto(
        val incidentId: String,
        val sourceUri: Uri,
        val entryPath: String,
        val sha256: String,
        val sizeBytes: Long,
        val mimeType: String
    )

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }
    private fun ByteArray.toString(charset: java.nio.charset.Charset): String = String(this, charset)
}
