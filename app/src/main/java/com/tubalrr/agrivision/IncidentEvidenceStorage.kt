package com.tubalrr.agrivision

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal fun storeIncidentEvidence(context: Context, source: Uri): Uri {
    val directory = File(context.filesDir, "incident-evidence")
    check(directory.isDirectory || directory.mkdirs()) { "Unable to create private evidence storage." }
    val mime = context.contentResolver.getType(source).orEmpty()
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
        ?.lowercase()
        ?.takeIf { it.matches(Regex("[a-z0-9]{2,5}")) }
        ?: "img"
    val target = File(directory, "evidence_${UUID.randomUUID()}.$extension")
    try {
        val input = context.contentResolver.openInputStream(source)
            ?: error("Unable to read the selected photo.")
        input.use { stream ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0L
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_IMAGE_BYTES) { "Photo is larger than 12 MB. Choose a smaller image." }
                    output.write(buffer, 0, count)
                }
            }
        }
        check(target.length() > 0L) { "The selected photo is empty." }
        return Uri.fromFile(target)
    } catch (error: Exception) {
        target.delete()
        throw error
    }
}

private const val MAX_IMAGE_BYTES = 12L * 1024L * 1024L
