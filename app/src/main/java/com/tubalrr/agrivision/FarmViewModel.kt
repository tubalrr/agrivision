package com.tubalrr.agrivision

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tubalrr.agrivision.data.local.AgriDatabase
import com.tubalrr.agrivision.data.local.FarmRepository
import com.tubalrr.agrivision.data.local.FarmSnapshot
import com.tubalrr.agrivision.data.local.FeedLogRecord
import com.tubalrr.agrivision.data.local.LegacyPreferencesMigrator
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FarmerRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import com.tubalrr.agrivision.domain.model.MapPoint
import com.tubalrr.agrivision.domain.validation.MapGeometryValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupRestorePreview(
    val farmerName: String,
    val farmName: String,
    val fieldCount: Int,
    val cropCount: Int,
    val livestockCount: Int,
    val cropHistoryCount: Int,
    val livestockHistoryCount: Int,
    val productionCount: Int,
    val saleCount: Int,
    val expenseCount: Int,
    val inventoryCount: Int,
    val inventoryMovementCount: Int,
    val feedLogCount: Int,
    val equipmentCount: Int,
    val taskCount: Int,
    val incidentCount: Int,
    val incidentEventCount: Int,
    val assistanceCount: Int,
    val photoCount: Int,
    val unavailablePhotoCount: Int,
    val isLegacyJson: Boolean,
    val exportedAt: Long?
)

private data class PendingRestorePhoto(
    val entry: StagedArchiveEvidence,
    val mimeType: String
)

private data class PendingBackupRestore(
    val snapshot: FarmSnapshot,
    val archive: ReadFarmBackup,
    val photosByIncidentId: Map<String, PendingRestorePhoto>,
    val preview: BackupRestorePreview
)

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val BACKUP_FORMAT_VERSION = 1
        const val ROOM_SCHEMA_VERSION = 12
    }

    private val _backupStatus = MutableStateFlow("")
    val backupStatus: StateFlow<String> = _backupStatus
    private val _backupPreview = MutableStateFlow<BackupRestorePreview?>(null)
    val backupPreview: StateFlow<BackupRestorePreview?> = _backupPreview
    private val _backupRestoreInProgress = MutableStateFlow(false)
    val backupRestoreInProgress: StateFlow<Boolean> = _backupRestoreInProgress
    private var pendingBackupRestore: PendingBackupRestore? = null


    private val staffProfilePreferences = application.getSharedPreferences(
        "agrivision_da_staff_profile",
        Context.MODE_PRIVATE
    )
    private val _daStaffProfile = MutableStateFlow(readDaStaffProfile())
    val daStaffProfile: StateFlow<DaStaffProfile> = _daStaffProfile

    private val database = AgriDatabase.getInstance(application)
    private val repository = FarmRepository(database)

    private fun readDaStaffProfile(): DaStaffProfile = DaStaffProfile(
        fullName = staffProfilePreferences.getString("full_name", "").orEmpty(),
        staffId = staffProfilePreferences.getString("staff_id", "").orEmpty(),
        designation = staffProfilePreferences.getString("designation", "DA Staff").orEmpty(),
        office = staffProfilePreferences.getString("office", "").orEmpty(),
        region = staffProfilePreferences.getString("region", "").orEmpty(),
        province = staffProfilePreferences.getString("province", "").orEmpty(),
        municipality = staffProfilePreferences.getString("municipality", "").orEmpty(),
        contactNumber = staffProfilePreferences.getString("contact_number", "").orEmpty(),
        officialEmail = staffProfilePreferences.getString("official_email", "").orEmpty()
    )

    fun saveDaStaffProfile(value: DaStaffProfile) {
        val saved = value.copy(
            fullName = value.fullName.trim(),
            staffId = value.staffId.trim(),
            designation = value.designation.trim().ifBlank { "DA Staff" },
            office = value.office.trim(),
            region = value.region.trim(),
            province = value.province.trim(),
            municipality = value.municipality.trim(),
            contactNumber = value.contactNumber.trim(),
            officialEmail = value.officialEmail.trim()
        )
        staffProfilePreferences.edit()
            .putString("full_name", saved.fullName)
            .putString("staff_id", saved.staffId)
            .putString("designation", saved.designation)
            .putString("office", saved.office)
            .putString("region", saved.region)
            .putString("province", saved.province)
            .putString("municipality", saved.municipality)
            .putString("contact_number", saved.contactNumber)
            .putString("official_email", saved.officialEmail)
            .apply()
        _daStaffProfile.value = saved
    }

    val profile: StateFlow<FarmerProfile> = repository.observeProfile().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FarmerProfile("", "", "", "", "", "", "", "", "", "")
    )

    val farmer: StateFlow<FarmerRecord> = repository.observeFarmer().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), FarmerRecord("", "")
    )

    val farm: StateFlow<FarmRecord> = repository.observeFarm().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        FarmRecord(FarmRepository.DEFAULT_FARM_ID, "", "")
    )

    val fields: StateFlow<List<FieldRecord>> = repository.observeFields().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val livestock: StateFlow<List<Livestock>> = repository.observeLivestock().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val livestockLifecycleEvents: StateFlow<List<LivestockLifecycleEvent>> =
        repository.observeLivestockLifecycleEvents().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )
    val crops: StateFlow<List<CropRecord>> = repository.observeCrops().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val cropLifecycleEvents: StateFlow<List<CropLifecycleEvent>> = repository.observeCropLifecycleEvents().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val production: StateFlow<List<ProductionRecord>> = repository.observeProduction().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val expenses: StateFlow<List<ExpenseRecord>> = repository.observeExpenses().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val sales: StateFlow<List<SaleRecord>> = repository.observeSales().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val inventory: StateFlow<List<InventoryItem>> = repository.observeInventory().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val feedLogs: StateFlow<List<FeedLogRecord>> = repository.observeFeedLogs().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val inventoryTransactions: StateFlow<List<InventoryTransaction>> =
        repository.observeInventoryTransactions().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )
    val equipment: StateFlow<List<EquipmentRecord>> = repository.observeEquipment().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val tasks: StateFlow<List<FarmTask>> = repository.observeTasks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val assistance: StateFlow<List<AssistanceRecord>> = repository.observeAssistance().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val fieldIncidents: StateFlow<List<FieldIncident>> = repository.observeFieldIncidents().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val incidentEvents: StateFlow<List<IncidentEvent>> = repository.observeIncidentEvents().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val reportSubmission: StateFlow<ReportSubmission> = repository.observeReportSubmission().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportSubmission("Draft", "", "")
    )

    val totalSales: StateFlow<Double> = repository.observeTotalSales().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0
    )
    val totalExpenses: StateFlow<Double> = repository.observeTotalExpenses().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0
    )
    val productionCount: StateFlow<Int> = repository.observeProductionCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )
    val totalLivestock: StateFlow<Int> = repository.observeTotalLivestock().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )

    init {
        viewModelScope.launch {
            LegacyPreferencesMigrator.migrateIfNeeded(
                context = getApplication<Application>(),
                database = database,
                repository = repository
            )
        }
    }

    fun saveProfile(value: FarmerProfile) = launch { repository.saveProfile(value) }

    fun addField(value: FieldRecord) = launch { repository.saveField(value) }
    fun updateField(value: FieldRecord) = launch { repository.saveField(value) }
    fun deleteField(value: FieldRecord) = launch { repository.deleteField(value) }

    fun saveFarmBoundary(points: List<MapPoint>) = launch {
        val validation = MapGeometryValidator.validatePolygon(points, "Farm")
        require(validation.valid) { validation.message }
        repository.saveFarmBoundary(points)
    }

    fun saveFieldMapping(field: FieldRecord, points: List<MapPoint>) = launch {
        if (points.size >= 3) {
            val validation = MapGeometryValidator.validatePolygon(points, "Field")
            require(validation.valid) { validation.message }
        } else {
            require(points.size == 1) { "Field mapping needs one location point or at least 3 boundary points." }
        }
        val center = points.centroid()
        repository.saveField(
            field.copy(
                boundaryPoints = points,
                latitude = center?.latitude ?: field.latitude,
                longitude = center?.longitude ?: field.longitude
            )
        )
    }

    fun saveIncidentMapping(incident: FieldIncident, points: List<MapPoint>) = launch {
        val validation = MapGeometryValidator.validatePolygon(points, "Incident")
        require(validation.valid) { validation.message }
        val center = points.centroid()
        repository.saveFieldIncident(
            incident.copy(
                affectedAreaBoundary = points,
                latitude = center?.latitude ?: incident.latitude,
                longitude = center?.longitude ?: incident.longitude
            )
        )
    }

    fun addLivestock(value: Livestock) = launch { repository.saveLivestock(value) }

    fun addLivestockLifecycleEvent(value: LivestockLifecycleEvent) = launch {
        repository.saveLivestockLifecycleEvent(value)
    }
    fun addCrop(value: CropRecord) = launch { repository.saveCrop(value) }

    fun addCropLifecycleEvent(value: CropLifecycleEvent) = launch {
        repository.saveCropLifecycleEvent(value)
    }
    fun addInventory(value: InventoryItem) = launch { repository.saveInventoryWithPurchase(value) }

    fun recordInventoryMovement(
        inventoryId: String,
        type: String,
        quantity: Double,
        date: String,
        notes: String,
        onResult: (String?) -> Unit = {}
    ) = launch {
        onResult(repository.recordInventoryMovement(inventoryId, type, quantity, date, notes))
    }

    fun addFeedLog(value: FeedLogRecord) = launch { repository.saveFeedLog(value) }
    fun addEquipment(value: EquipmentRecord) = launch { repository.saveEquipment(value) }

    fun addProduction(value: ProductionRecord) = launch { repository.saveProduction(value) }
    fun addExpense(value: ExpenseRecord) = launch { repository.saveExpense(value) }
    fun addSale(value: SaleRecord) = launch { repository.saveSale(value) }
    fun setExpensePaymentStatus(value: ExpenseRecord, status: String) = launch {
        repository.updateExpensePaymentStatus(value, status)
    }
    fun setSalePaymentStatus(value: SaleRecord, status: String) = launch {
        repository.updateSalePaymentStatus(value, status)
    }

    fun addTask(value: FarmTask) = launch { repository.saveTask(value) }

    fun toggleTask(value: FarmTask) = launch {
        repository.updateTask(value)
    }

    fun addAssistance(value: AssistanceRecord) = launch {
        repository.saveAssistance(value)
    }

    fun updateAssistance(value: AssistanceRecord) = launch {
        repository.updateAssistance(value)
    }

    fun addFieldIncident(value: FieldIncident) = launch {
        repository.createIncident(value)
    }

    fun updateFieldIncident(value: FieldIncident, previousStatus: String) = launch {
        if (value.status == previousStatus) return@launch
        repository.transitionIncident(
            incidentId = value.id,
            nextStatus = value.status,
            actor = value.reviewer.ifBlank { value.farmerId },
            note = when (value.status) {
                "Submitted" -> "Farmer submitted field report"
                else -> "Incident workflow updated"
            }
        )
    }

    fun reviewFieldIncident(previous: FieldIncident, updated: FieldIncident) = launch {
        if (updated.status != previous.status) {
            repository.transitionIncident(
                incidentId = updated.id,
                nextStatus = updated.status,
                actor = updated.reviewer,
                note = updated.reviewNotes.ifBlank { "Reviewer updated case status" }
            )
        } else if (updated.reviewNotes != previous.reviewNotes || updated.reviewer != previous.reviewer) {
            repository.saveFieldIncident(updated)
            repository.saveIncidentEvent(
                IncidentEvent(
                    incidentId = updated.id,
                    fromStatus = updated.status,
                    status = updated.status,
                    note = updated.reviewNotes.ifBlank { "Reviewer notes updated" },
                    actor = updated.reviewer
                )
            )
        }
    }

    fun requestAssistance(incident: FieldIncident) = launch {
        if (assistance.value.none { it.incidentId == incident.id }) {
            repository.createAssistanceForIncident(incident)
        }
    }

    fun completeFieldIncident(incident: FieldIncident, resolution: String, reviewer: String) = launch {
        repository.completeIncident(incident.id, resolution, reviewer)
    }

    fun saveReportSubmission(value: ReportSubmission) = launch {
        repository.saveReportSubmission(value)
    }

    fun exportBackup(uri: Uri) = launch {
        runCatching {
            val snapshot = currentSnapshot()
            val json = snapshotToJson(snapshot)
            val summary = withContext(Dispatchers.IO) {
                FarmBackupArchive.export(
                    context = getApplication<Application>(),
                    destination = uri,
                    json = json,
                    incidents = snapshot.fieldIncidents
                )
            }
            _backupStatus.value = "Backup exported successfully with ${summary.incidentPhotosIncluded} incident photo(s)."
        }.onFailure {
            _backupStatus.value = "Backup export failed: " + (it.message ?: "Unknown error")
        }
    }

    /**
     * Read and validate first, then ask the user to review a summary before touching Room.
     * JSON files from older app versions remain importable; new .zip archives also carry photos.
     */
    fun previewBackup(uri: Uri) = launch {
        var readBackup: ReadFarmBackup? = null
        try {
            val parsed = withContext(Dispatchers.IO) {
                FarmBackupArchive.read(getApplication<Application>(), uri)
            }
            readBackup = parsed
            validateBackup(parsed.json)
            require(parsed.isArchive || parsed.json.optInt("archiveVersion", 0) == 0) {
                "This backup manifest is missing its photo archive. Select the original .zip backup."
            }

            val snapshot = jsonToSnapshot(parsed.json)
            validateSnapshot(snapshot)
            val photos = validateArchiveEvidence(parsed, snapshot)
            val context = getApplication<Application>()
            val unavailableLegacyPhotos = if (parsed.isArchive) 0 else {
                snapshot.fieldIncidents.count { incident ->
                    incident.evidenceUri.isNotBlank() &&
                        !FarmBackupArchive.isEvidenceReadable(context, incident.evidenceUri)
                }
            }
            val availableLegacyPhotos = if (parsed.isArchive) 0 else {
                snapshot.fieldIncidents.count { incident ->
                    incident.evidenceUri.isNotBlank() &&
                        FarmBackupArchive.isEvidenceReadable(context, incident.evidenceUri)
                }
            }
            val preview = BackupRestorePreview(
                farmerName = snapshot.profile.farmerName,
                farmName = snapshot.farm?.farmName ?: snapshot.profile.farmName,
                fieldCount = snapshot.fields.size,
                cropCount = snapshot.crops.size,
                livestockCount = snapshot.livestock.size,
                cropHistoryCount = snapshot.cropLifecycleEvents.size,
                livestockHistoryCount = snapshot.livestockLifecycleEvents.size,
                productionCount = snapshot.production.size,
                saleCount = snapshot.sales.size,
                expenseCount = snapshot.expenses.size,
                inventoryCount = snapshot.inventory.size,
                inventoryMovementCount = snapshot.inventoryTransactions.size,
                feedLogCount = snapshot.feedLogs.size,
                equipmentCount = snapshot.equipment.size,
                taskCount = snapshot.tasks.size,
                incidentCount = snapshot.fieldIncidents.size,
                incidentEventCount = snapshot.incidentEvents.size,
                assistanceCount = snapshot.assistance.size,
                photoCount = photos.size + availableLegacyPhotos,
                unavailablePhotoCount = unavailableLegacyPhotos,
                isLegacyJson = !parsed.isArchive,
                exportedAt = parsed.json.optLong("exportedAt").takeIf { it > 0L }
            )

            val previous = pendingBackupRestore
            pendingBackupRestore = PendingBackupRestore(snapshot, parsed, photos, preview)
            _backupPreview.value = preview
            _backupStatus.value = "Backup validated. Review the restore summary before continuing."
            if (previous != null && previous.archive !== parsed) {
                withContext(Dispatchers.IO) { FarmBackupArchive.cleanupStaging(previous.archive) }
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            withContext(Dispatchers.IO) { FarmBackupArchive.cleanupStaging(readBackup) }
            _backupStatus.value = "Backup validation failed: " + (error.message ?: "Invalid backup file.")
            _backupPreview.value = null
            pendingBackupRestore = null
        }
    }

    fun cancelBackupRestore() {
        if (_backupRestoreInProgress.value) return
        val pending = pendingBackupRestore
        pendingBackupRestore = null
        _backupPreview.value = null
        if (pending != null) {
            viewModelScope.launch(Dispatchers.IO) {
                FarmBackupArchive.cleanupStaging(pending.archive)
            }
        }
        _backupStatus.value = "Restore cancelled. Your current farm records were not changed."
    }

    fun confirmBackupRestore() = launch {
        if (_backupRestoreInProgress.value) return@launch
        val pending = pendingBackupRestore
        if (pending == null) {
            _backupStatus.value = "No validated backup is waiting to be restored."
            _backupPreview.value = null
            return@launch
        }

        _backupRestoreInProgress.value = true
        val context = getApplication<Application>()
        var copiedEvidenceUris = emptyList<String>()
        var databaseCommitted = false
        try {
            val prepared = withContext(Dispatchers.IO) {
                val copied = mutableListOf<String>()
                try {
                    val incidents = pending.snapshot.fieldIncidents.map { incident ->
                        val photo = pending.photosByIncidentId[incident.id]
                        when {
                            photo != null -> {
                                val newUri = FarmBackupArchive.copyEvidenceIntoPrivateStorage(
                                    context,
                                    photo.entry,
                                    photo.mimeType
                                ).toString()
                                copied += newUri
                                incident.copy(evidenceUri = newUri)
                            }
                            incident.evidenceUri.isNotBlank() -> {
                                if (FarmBackupArchive.isEvidenceReadable(context, incident.evidenceUri)) {
                                    val newUri = FarmBackupArchive.copyLegacyEvidenceIntoPrivateStorage(
                                        context,
                                        incident.evidenceUri
                                    ).toString()
                                    copied += newUri
                                    incident.copy(evidenceUri = newUri)
                                } else {
                                    incident.copy(evidenceUri = "")
                                }
                            }
                            else -> incident
                        }
                    }
                    incidents to copied.toList()
                } catch (error: Exception) {
                    copied.forEach { FarmBackupArchive.deletePrivateEvidence(context, it) }
                    throw error
                }
            }
            copiedEvidenceUris = prepared.second
            val restoredSnapshot = pending.snapshot.copy(fieldIncidents = prepared.first)
            validateSnapshot(restoredSnapshot)
            repository.replaceAll(restoredSnapshot)
            databaseCommitted = true

            withContext(Dispatchers.IO) {
                runCatching {
                    FarmBackupArchive.cleanupUnreferencedEvidence(
                        context,
                        restoredSnapshot.fieldIncidents.map { it.evidenceUri }.filter { it.isNotBlank() }.toSet()
                    )
                }
                FarmBackupArchive.cleanupStaging(pending.archive)
            }
            val droppedPhotos = pending.preview.unavailablePhotoCount
            _backupStatus.value = buildString {
                append("Backup restored successfully. ")
                append("Replaced records atomically; ")
                append(pending.preview.fieldCount).append(" field(s), ")
                append(pending.preview.cropCount).append(" crop(s), ")
                append(pending.preview.livestockCount).append(" livestock group(s), and ")
                append(pending.preview.incidentCount).append(" incident(s) restored.")
                if (droppedPhotos > 0) append(" ").append(droppedPhotos).append(" unavailable legacy photo(s) were detached.")
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            if (!databaseCommitted) {
                withContext(Dispatchers.IO) {
                    copiedEvidenceUris.forEach { FarmBackupArchive.deletePrivateEvidence(context, it) }
                }
            }
            _backupStatus.value = "Restore failed: " + (error.message ?: "Backup could not be restored.") +
                if (databaseCommitted) "" else " Existing database records were preserved."
        } finally {
            withContext(Dispatchers.IO) { FarmBackupArchive.cleanupStaging(pending.archive) }
            pendingBackupRestore = null
            _backupPreview.value = null
            _backupRestoreInProgress.value = false
        }
    }

    fun exportDaReport(uri: Uri, reportType: String, format: DaReportFormat) = launch {
        runCatching {
            require(reportType in DaReportExporter.reportTypes) { "Unsupported DA report: " + reportType }
            val snapshot = currentSnapshot()
            val output = getApplication<Application>().contentResolver.openOutputStream(uri)
                ?: error("Unable to open the selected report destination.")
            output.use { DaReportExporter.write(snapshot, reportType, format) { bytes -> it.write(bytes) } }
        }.onSuccess {
            _backupStatus.value = reportType + " " + format.extension.uppercase() + " report exported."
        }.onFailure {
            _backupStatus.value = reportType + " export failed: " + (it.message ?: "Unknown error")
        }
    }

    fun exportCasePackage(uri: Uri) = launch {
        val snapshot = currentSnapshot()
        val json = JSONObject().apply {
            put("packageVersion", 10)
            put("app", "AgriVision")
            put("packageType", "DA Case Package")
            put("generatedAt", System.currentTimeMillis())
            put("farmerProfile", snapshot.profile.toJson())
        put("farmer", snapshot.profile.toFarmerRecord().toJson())
        put("farm", snapshot.profile.toFarmRecord().toJson())
        put("fields", JSONArray(snapshot.fields.map { it.toJson() }))
            put("fieldIncidents", JSONArray(snapshot.fieldIncidents.map { it.toJson() }))
            put("incidentEvents", JSONArray(snapshot.incidentEvents.map { it.toJson() }))
            put("assistance", JSONArray(snapshot.assistance.map { it.toJson() }))
            put("operationalRecords", JSONObject().apply {
                put("livestock", JSONArray(snapshot.livestock.map { it.toJson() }))
                put("livestockLifecycleEvents", JSONArray(snapshot.livestockLifecycleEvents.map { it.toJson() }))
                put("crops", JSONArray(snapshot.crops.map { it.toJson() }))
                put("production", JSONArray(snapshot.production.map { it.toJson() }))
                put("expenses", JSONArray(snapshot.expenses.map { it.toJson() }))
                put("sales", JSONArray(snapshot.sales.map { it.toJson() }))
                put("inventory", JSONArray(snapshot.inventory.map { it.toJson() }))
                put("inventoryTransactions", JSONArray(snapshot.inventoryTransactions.map { it.toJson() }))
                put("feedLogs", JSONArray(snapshot.feedLogs.map { it.toJson() }))
            })
        }
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
            it.write(json.toString(2).toByteArray(Charsets.UTF_8))
        }
    }

    private fun validateBackup(json: JSONObject) {
        require(json.optString("app") == "AgriVision") { "This is not an AgriVision backup." }
        val backupVersion = json.optInt("backupVersion", 0)
        val legacyVersion = json.optInt("version", 0)
        val effectiveBackupVersion = if (backupVersion == 0 && legacyVersion > 0) 1 else backupVersion
        require(effectiveBackupVersion == BACKUP_FORMAT_VERSION) {
            "Unsupported backup format version: " + effectiveBackupVersion
        }
        val schemaVersion = json.optInt("schemaVersion", json.optInt("version", effectiveBackupVersion))
        require(schemaVersion in 1..ROOM_SCHEMA_VERSION) {
            "Unsupported Room schema version: " + schemaVersion
        }
        require(json.optJSONObject("farmerProfile") != null) { "Backup is missing farmerProfile." }
        require(json.optJSONObject("farm") != null) { "Backup is missing farm data." }

        val rootArrays = listOf(
            "fields", "livestock", "livestockLifecycleEvents", "crops", "cropLifecycleEvents",
            "production", "expenses", "sales", "inventory", "inventoryTransactions", "feedLogs",
            "equipment", "tasks", "assistance", "fieldIncidents", "incidentEvents"
        )
        rootArrays.forEach { key ->
            if (json.has(key)) {
                val value = json.opt(key)
                require(value is JSONArray) { "Backup section '$key' is not a valid list." }
                requireJsonObjectEntries(key, value)
            }
        }
        if (json.has("reportSubmission")) {
            require(json.optJSONObject("reportSubmission") != null) { "Backup reportSubmission section is invalid." }
        }
        if (json.has("operationalRecords")) {
            val operational = json.optJSONObject("operationalRecords")
                ?: error("Backup operationalRecords section is invalid.")
            listOf("livestock", "livestockLifecycleEvents", "crops", "production", "expenses", "sales", "inventory", "inventoryTransactions", "feedLogs")
                .forEach { key ->
                    if (operational.has(key)) {
                        val value = operational.opt(key)
                        require(value is JSONArray) { "Backup operational section '$key' is not a valid list." }
                        requireJsonObjectEntries("operationalRecords.$key", value)
                    }
                }
        }
        if (json.has("incidentEvidence")) {
            val value = json.opt("incidentEvidence")
            require(value is JSONArray) { "Backup incidentEvidence section is invalid." }
            requireJsonObjectEntries("incidentEvidence", value)
        }
        validateBackupCoordinates(json)
    }

    private fun validateBackupCoordinates(json: JSONObject) {
        validateCoordinatePair(json.optJSONObject("farm"), "Farm")
        val fields = json.optJSONArray("fields")
        if (fields != null) {
            for (index in 0 until fields.length()) {
                val field = fields.optJSONObject(index) ?: error("Backup contains an invalid field record.")
                val name = field.optString("name").ifBlank { "at position ${index + 1}" }
                validateCoordinatePair(field, "Field $name")
                validatePointArray(field, "boundaryPoints", "Field $name")
            }
        }
        json.optJSONObject("farm")?.let { validatePointArray(it, "boundaryPoints", "Farm") }

        val incidents = json.optJSONArray("fieldIncidents")
        if (incidents != null) {
            for (index in 0 until incidents.length()) {
                val incident = incidents.optJSONObject(index)
                    ?: error("Backup contains an invalid incident record.")
                val id = incident.optString("id").ifBlank { "at position ${index + 1}" }
                validateCoordinatePair(incident, "Incident $id")
                validatePointArray(incident, "affectedAreaBoundary", "Incident $id")
            }
        }
    }

    private fun validateCoordinatePair(record: JSONObject?, label: String) {
        if (record == null) return
        val rawLat = record.opt("latitude")
        val rawLon = record.opt("longitude")
        val hasLat = rawLat != null && rawLat !== JSONObject.NULL
        val hasLon = rawLon != null && rawLon !== JSONObject.NULL
        require(hasLat == hasLon) { "$label must have both latitude and longitude, or neither." }
        if (!hasLat) return
        val latitude = coordinateNumber(rawLat)
            ?: error("$label has a non-numeric latitude.")
        val longitude = coordinateNumber(rawLon)
            ?: error("$label has a non-numeric longitude.")
        require(latitude.isFinite() && latitude in -90.0..90.0) { "$label latitude must be between -90 and 90." }
        require(longitude.isFinite() && longitude in -180.0..180.0) { "$label longitude must be between -180 and 180." }
    }

    private fun validatePointArray(record: JSONObject, key: String, label: String) {
        if (!record.has(key) || record.isNull(key)) return
        val points = record.optJSONArray(key)
            ?: error("$label has an invalid $key list.")
        for (index in 0 until points.length()) {
            val point = points.optJSONObject(index)
                ?: error("$label has an invalid boundary point at position ${index + 1}.")
            val latitude = coordinateNumber(point.opt("latitude"))
                ?: error("$label boundary point ${index + 1} has an invalid latitude.")
            val longitude = coordinateNumber(point.opt("longitude"))
                ?: error("$label boundary point ${index + 1} has an invalid longitude.")
            require(latitude.isFinite() && latitude in -90.0..90.0) {
                "$label boundary point ${index + 1} latitude must be between -90 and 90."
            }
            require(longitude.isFinite() && longitude in -180.0..180.0) {
                "$label boundary point ${index + 1} longitude must be between -180 and 180."
            }
        }
    }

    private fun coordinateNumber(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun requireJsonObjectEntries(section: String, array: JSONArray) {
        for (index in 0 until array.length()) {
            require(array.optJSONObject(index) != null) {
                "Backup section '$section' has an invalid record at position ${index + 1}."
            }
        }
    }

    private fun validateArchiveEvidence(
        archive: ReadFarmBackup,
        snapshot: FarmSnapshot
    ): Map<String, PendingRestorePhoto> {
        if (!archive.isArchive) {
            require(archive.entries.isEmpty()) { "Invalid legacy backup state." }
            require(archive.json.optInt("archiveVersion", 0) == 0) {
                "This backup manifest requires its original .zip archive to restore incident photos."
            }
            return emptyMap()
        }

        require(archive.json.optInt("archiveVersion", 0) == FarmBackupArchive.ARCHIVE_VERSION) {
            "Unsupported backup archive version."
        }
        val manifest = archive.json.optJSONArray("incidentEvidence")
            ?: error("Backup archive is missing incident photo metadata.")
        val incidentIds = snapshot.fieldIncidents.map { it.id }.toSet()
        val photosByIncident = linkedMapOf<String, PendingRestorePhoto>()
        val referencedPaths = linkedSetOf<String>()

        for (index in 0 until manifest.length()) {
            val row = manifest.getJSONObject(index)
            val incidentId = row.optString("incidentId").trim()
            val entryPath = row.optString("entryPath")
            val expectedHash = row.optString("sha256").lowercase()
            val expectedSize = row.optLong("sizeBytes", -1L)
            val mimeType = row.optString("mimeType").ifBlank { "application/octet-stream" }
            require(incidentId.isNotBlank() && incidentId in incidentIds) {
                "Backup photo points to an incident that is not in the backup."
            }
            require(!photosByIncident.containsKey(incidentId)) {
                "Backup contains multiple photos for incident $incidentId."
            }
            require(entryPath.startsWith("photos/") && !entryPath.contains("..")) {
                "Backup contains an invalid photo path."
            }
            val staged = archive.entries[entryPath]
                ?: error("Backup photo file is missing for incident $incidentId.")
            require(expectedSize > 0L && staged.sizeBytes == expectedSize && staged.sha256 == expectedHash) {
                "Photo integrity check failed for incident $incidentId. The backup may be damaged."
            }
            require(mimeType.startsWith("image/") || mimeType == "application/octet-stream") {
                "Backup includes an unsupported incident attachment type."
            }
            require(referencedPaths.add(entryPath)) { "Backup reuses a photo file more than once." }
            photosByIncident[incidentId] = PendingRestorePhoto(staged, mimeType)
        }

        require(referencedPaths == archive.entries.keys) {
            "Backup includes missing or unreferenced photo files. Nothing has been restored."
        }
        val incidentsWithEvidence = snapshot.fieldIncidents.filter { it.evidenceUri.isNotBlank() }.map { it.id }.toSet()
        require(incidentsWithEvidence == photosByIncident.keys) {
            "Backup incident photo metadata does not match the incident records."
        }
        return photosByIncident
    }

    private fun validateSnapshot(snapshot: FarmSnapshot) {
        fun uniqueIds(label: String, ids: List<String>, required: Boolean = false): Set<String> {
            if (required) require(ids.all { it.isNotBlank() }) { "Backup contains a record with a missing $label ID." }
            val nonBlank = ids.filter { it.isNotBlank() }
            val duplicates = nonBlank.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            require(duplicates.isEmpty()) {
                "Backup contains duplicate $label IDs: " + duplicates.take(3).joinToString(", ")
            }
            return nonBlank.toSet()
        }

        val fieldIds = uniqueIds("field", snapshot.fields.map { it.fieldId }, required = snapshot.fields.isNotEmpty())
        val cropIds = uniqueIds("crop", snapshot.crops.map { it.cropId })
        val livestockIds = uniqueIds("livestock group", snapshot.livestock.map { it.groupId })
        val incidentIds = uniqueIds("incident", snapshot.fieldIncidents.map { it.id }, required = snapshot.fieldIncidents.isNotEmpty())
        uniqueIds("crop lifecycle event", snapshot.cropLifecycleEvents.map { it.eventId })
        uniqueIds("livestock lifecycle event", snapshot.livestockLifecycleEvents.map { it.eventId })
        uniqueIds("inventory item", snapshot.inventory.map { it.inventoryId })
        uniqueIds("expense", snapshot.expenses.map { it.expenseId })
        uniqueIds("sale", snapshot.sales.map { it.saleId })
        uniqueIds("inventory transaction", snapshot.inventoryTransactions.map { it.transactionId })
        uniqueIds("feed log", snapshot.feedLogs.map { it.feedLogId })
        uniqueIds("incident event", snapshot.incidentEvents.map { it.eventId })

        require(snapshot.fields.all { it.areaHectares.isFinite() && it.areaHectares >= 0.0 }) {
            "Backup contains an invalid field area."
        }
        require(snapshot.crops.all {
            val rawArea = it.area.trim()
            val leadingNumber = Regex("""^-?\d+(?:\.\d+)?""").find(rawArea)?.value?.toDoubleOrNull()
            rawArea.isBlank() || leadingNumber == null || (leadingNumber.isFinite() && leadingNumber >= 0.0)
        }) {
            "Backup contains an invalid crop area."
        }
        require(snapshot.livestock.all { it.count >= 0 && it.initialPopulation >= 0 && it.currentPopulation >= 0 }) {
            "Backup contains an invalid livestock count."
        }
        require(snapshot.production.all { it.areaHectares.isFinite() && it.areaHectares >= 0.0 }) {
            "Backup contains an invalid production area."
        }
        require(snapshot.expenses.all { it.amount.isFinite() && it.amount > 0.0 && it.paymentStatus in setOf("Paid", "Unpaid") }) {
            "Backup contains an invalid expense amount or payment status."
        }
        require(snapshot.sales.all { it.amount.isFinite() && it.amount > 0.0 && it.paymentStatus in setOf("Paid", "Unpaid") }) {
            "Backup contains an invalid sale amount or payment status."
        }
        require(snapshot.inventory.all {
            it.stock.isFinite() && it.stock >= 0.0 && it.purchasePrice.isFinite() && it.purchasePrice >= 0.0
        }) { "Backup contains invalid inventory values." }
        require(snapshot.inventoryTransactions.all { it.quantity.isFinite() && it.quantity >= 0.0 }) {
            "Backup contains an invalid inventory transaction quantity."
        }
        require(snapshot.feedLogs.all { it.quantityKg.isFinite() && it.quantityKg >= 0.0 }) {
            "Backup contains an invalid feed log quantity."
        }
        require(snapshot.cropLifecycleEvents.all { it.quantity.isBlank() || it.quantity.trim().toDoubleOrNull()?.let { v -> v.isFinite() && v >= 0.0 } == true }) {
            "Backup contains an invalid crop lifecycle quantity."
        }
        require(snapshot.livestockLifecycleEvents.all {
            it.quantity.isFinite() && it.quantity >= 0.0 && it.amount.isFinite() && it.amount >= 0.0
        }) { "Backup contains invalid livestock lifecycle values." }

        snapshot.fields.forEach { field ->
            if (field.boundaryPoints.size >= 3) {
                val geometry = MapGeometryValidator.validatePolygon(field.boundaryPoints, "Field " + field.name)
                require(geometry.valid) { geometry.message }
            }
            require((field.latitude == null && field.longitude == null) || (field.latitude != null && field.longitude != null)) {
                "Field ${field.name} has incomplete GPS coordinates."
            }
            require(field.latitude == null || (field.latitude.isFinite() && field.latitude in -90.0..90.0)) {
                "Field ${field.name} has invalid latitude."
            }
            require(field.longitude == null || (field.longitude.isFinite() && field.longitude in -180.0..180.0)) {
                "Field ${field.name} has invalid longitude."
            }
            require(field.boundaryPoints.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 }) {
                "Field ${field.name} has invalid boundary coordinates."
            }
        }
        snapshot.fieldIncidents.forEach { incident ->
            if (incident.affectedAreaBoundary.size >= 3) {
                val geometry = MapGeometryValidator.validatePolygon(incident.affectedAreaBoundary, "Incident " + incident.id)
                require(geometry.valid) { geometry.message }
            }
            require((incident.latitude == null && incident.longitude == null) || (incident.latitude != null && incident.longitude != null)) {
                "Incident ${incident.id} has incomplete GPS coordinates."
            }
            require(incident.latitude == null || (incident.latitude.isFinite() && incident.latitude in -90.0..90.0)) {
                "Incident ${incident.id} has invalid latitude."
            }
            require(incident.longitude == null || (incident.longitude.isFinite() && incident.longitude in -180.0..180.0)) {
                "Incident ${incident.id} has invalid longitude."
            }
            require(incident.affectedAreaBoundary.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 }) {
                "Incident ${incident.id} has invalid boundary coordinates."
            }
        }
        require(snapshot.incidentEvents.all { it.incidentId.isBlank() || it.incidentId in incidentIds }) {
            "Backup contains an incident event linked to a missing incident."
        }
        require(snapshot.assistance.all { it.incidentId.isBlank() || it.incidentId in incidentIds }) {
            "Backup contains an assistance record linked to a missing incident."
        }
        require(snapshot.crops.all { it.fieldId.isBlank() || it.fieldId in fieldIds }) {
            "Backup contains a crop linked to a field that does not exist."
        }
        require(snapshot.cropLifecycleEvents.all { it.fieldId.isBlank() || it.fieldId in fieldIds }) {
            "Backup contains a crop history record linked to a missing field."
        }
        require(snapshot.cropLifecycleEvents.all { it.cropId.isBlank() || it.cropId in cropIds }) {
            "Backup contains crop history linked to a missing crop."
        }
        require(snapshot.livestockLifecycleEvents.all { it.livestockId.isBlank() || it.livestockId in livestockIds }) {
            "Backup contains livestock history linked to a missing livestock group."
        }
        require(snapshot.feedLogs.all { it.livestockId.isBlank() || it.livestockId in livestockIds }) {
            "Backup contains a feed log linked to a missing livestock group."
        }
        require(snapshot.inventoryTransactions.all { it.inventoryId.isBlank() || it.inventoryId in snapshot.inventory.map { item -> item.inventoryId }.toSet() }) {
            "Backup contains a stock movement linked to a missing inventory item."
        }
        require(snapshot.fieldIncidents.all { it.fieldId.isBlank() || it.fieldId in fieldIds }) {
            "Backup contains an incident linked to a field that does not exist."
        }
        snapshot.expenses.forEach { validateFinancialLink(it.sourceType, it.sourceId, it.fieldId, cropIds, livestockIds, fieldIds) }
        snapshot.sales.forEach { validateFinancialLink(it.sourceType, it.sourceId, it.fieldId, cropIds, livestockIds, fieldIds) }
    }

    private fun validateFinancialLink(
        sourceType: String,
        sourceId: String,
        fieldId: String,
        cropIds: Set<String>,
        livestockIds: Set<String>,
        fieldIds: Set<String>
    ) {
        when (sourceType.trim().lowercase(java.util.Locale.US)) {
            "", "unlinked" -> require(sourceId.isBlank()) { "Backup has an unlinked financial record with a source ID." }
            "crop" -> require(sourceId.isNotBlank() && sourceId in cropIds) {
                "Backup has a financial record linked to a missing crop."
            }
            "livestock" -> require(sourceId.isNotBlank() && sourceId in livestockIds) {
                "Backup has a financial record linked to a missing livestock group."
            }
            else -> error("Backup contains an unsupported financial source type: $sourceType.")
        }
        require(fieldId.isBlank() || fieldId in fieldIds) {
            "Backup has a financial record linked to a missing field."
        }
    }

    private suspend fun currentSnapshot(): FarmSnapshot {
        return FarmSnapshot(
            profile = profile.value,
            livestock = repository.observeLivestock().first(),
            crops = repository.observeCrops().first(),
            production = repository.observeProduction().first(),
            expenses = repository.observeExpenses().first(),
            sales = repository.observeSales().first(),
            inventory = repository.observeInventory().first(),
            inventoryTransactions = repository.observeInventoryTransactions().first(),
            feedLogs = repository.observeFeedLogs().first(),
            equipment = repository.observeEquipment().first(),
            tasks = repository.observeTasks().first(),
            assistance = repository.observeAssistance().first(),
            fieldIncidents = repository.observeFieldIncidents().first(),
            incidentEvents = repository.observeIncidentEvents().first(),
            reportSubmission = reportSubmission.value,
            fields = repository.observeFields().first(),
            cropLifecycleEvents = repository.observeCropLifecycleEvents().first(),
            livestockLifecycleEvents = repository.observeLivestockLifecycleEvents().first(),
            farm = farm.value
        )
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

private fun snapshotToJson(snapshot: FarmSnapshot): JSONObject =
    JSONObject().apply {
        put("backupVersion", FarmViewModel.BACKUP_FORMAT_VERSION)
        put("schemaVersion", FarmViewModel.ROOM_SCHEMA_VERSION)
        put("version", FarmViewModel.ROOM_SCHEMA_VERSION)
        put("app", "AgriVision")
        put("farmerProfile", snapshot.profile.toJson())
        put("farm", (snapshot.farm ?: snapshot.profile.toFarmRecord()).toJson())
        put("fields", JSONArray(snapshot.fields.map { it.toJson() }))
        put("livestock", JSONArray(snapshot.livestock.map { it.toJson() }))
        put("livestockLifecycleEvents", JSONArray(snapshot.livestockLifecycleEvents.map { it.toJson() }))
        put("crops", JSONArray(snapshot.crops.map { it.toJson() }))
        put("cropLifecycleEvents", JSONArray(snapshot.cropLifecycleEvents.map { it.toJson() }))
        put("production", JSONArray(snapshot.production.map { it.toJson() }))
        put("expenses", JSONArray(snapshot.expenses.map { it.toJson() }))
        put("sales", JSONArray(snapshot.sales.map { it.toJson() }))
        put("inventory", JSONArray(snapshot.inventory.map { it.toJson() }))
        put("inventoryTransactions", JSONArray(snapshot.inventoryTransactions.map { it.toJson() }))
        put("feedLogs", JSONArray(snapshot.feedLogs.map { it.toJson() }))
        put("equipment", JSONArray(snapshot.equipment.map { it.toJson() }))
        put("tasks", JSONArray(snapshot.tasks.map { it.toJson() }))
        put("assistance", JSONArray(snapshot.assistance.map { it.toJson() }))
        put("fieldIncidents", JSONArray(snapshot.fieldIncidents.map { it.toJson() }))
        put("incidentEvents", JSONArray(snapshot.incidentEvents.map { it.toJson() }))
        put("reportSubmission", JSONObject().apply {
            put("status", snapshot.reportSubmission.status)
            put("submittedDate", snapshot.reportSubmission.submittedDate)
            put("referenceNo", snapshot.reportSubmission.referenceNo)
        })
    }

private fun FarmerProfile.toFarmerRecord() = FarmerRecord(farmerId, farmerName, contact)
private fun FarmerProfile.toFarmRecord() = FarmRecord(
    farmId = FarmRepository.DEFAULT_FARM_ID,
    farmerId = farmerId,
    farmName = farmName,
    province = province,
    municipality = municipality,
    barangay = barangay,
    totalArea = farmSize,
    landTenure = landTenure,
    commodities = commodities,
    registryStatus = registryStatus,
    reviewNotes = reviewNotes
)

private fun FarmerRecord.toJson() = JSONObject().apply {
    put("farmerId", farmerId)
    put("fullName", fullName)
    put("contact", contact)
}

private fun FarmRecord.toJson() = JSONObject().apply {
    put("farmId", farmId)
    put("farmerId", farmerId)
    put("farmName", farmName)
    put("province", province)
    put("municipality", municipality)
    put("barangay", barangay)
    put("totalArea", totalArea)
    put("landTenure", landTenure)
    put("commodities", commodities)
    put("registryStatus", registryStatus)
    put("reviewNotes", reviewNotes)
    put("boundaryPoints", JSONArray(boundaryPoints.map { point ->
        JSONObject().apply {
            put("latitude", point.latitude)
            put("longitude", point.longitude)
        }
    }))
}

private fun FieldRecord.toJson() = JSONObject().apply {
    put("fieldId", fieldId)
    put("farmId", farmId)
    put("name", name)
    put("areaHectares", areaHectares)
    put("location", location)
    if (latitude == null) put("latitude", JSONObject.NULL) else put("latitude", latitude)
    if (longitude == null) put("longitude", JSONObject.NULL) else put("longitude", longitude)
    put("landTenure", landTenure)
    put("crop", crop)
    put("plantingDate", plantingDate)
    put("expectedHarvest", expectedHarvest)
    put("currentStatus", currentStatus)
    put("boundaryPoints", JSONArray(boundaryPoints.map { point ->
        JSONObject().apply {
            put("latitude", point.latitude)
            put("longitude", point.longitude)
        }
    }))
}

private fun FarmerProfile.toJson() = JSONObject().apply {
    put("farmerName", farmerName)
    put("farmerId", farmerId)
    put("contact", contact)
    put("province", province)
    put("municipality", municipality)
    put("barangay", barangay)
    put("farmName", farmName)
    put("farmSize", farmSize)
    put("landTenure", landTenure)
    put("commodities", commodities)
    put("registryStatus", registryStatus)
    put("reviewNotes", reviewNotes)
}

private fun Livestock.toJson() = JSONObject().apply {
    put("name", name)
    put("kind", kind)
    put("count", count)
    put("status", status)
    put("groupId", groupId)
    put("initialPopulation", initialPopulation)
    put("currentPopulation", currentPopulation)
}

private fun LivestockLifecycleEvent.toJson() = JSONObject().apply {
    put("eventId", eventId)
    put("livestockId", livestockId)
    put("stage", stage)
    put("date", date)
    put("notes", notes)
    put("inputName", inputName)
    put("quantity", quantity)
    put("unit", unit)
    put("amount", amount)
    put("createdAt", createdAt)
}
private fun CropRecord.toJson() = JSONObject().apply {
    put("name", name)
    put("crop", crop)
    put("area", area)
    put("stage", stage)
    put("cropId", cropId)
    put("fieldId", fieldId)
    put("plantingDate", plantingDate)
    put("expectedHarvest", expectedHarvest)
    put("currentStatus", currentStatus)
}

private fun CropLifecycleEvent.toJson() = JSONObject().apply {
    put("eventId", eventId)
    put("cropId", cropId)
    put("fieldId", fieldId)
    put("stage", stage)
    put("date", date)
    put("notes", notes)
    put("inputName", inputName)
    put("quantity", quantity)
    put("unit", unit)
    put("createdAt", createdAt)
}
private fun ProductionRecord.toJson() = JSONObject().apply {
    put("product", product)
    put("quantity", quantity)
    put("period", period)
    put("productionId", productionId)
    put("sourceType", sourceType)
    put("sourceId", sourceId)
    put("fieldId", fieldId)
    put("areaHectares", areaHectares)
    put("productionType", productionType)
}
private fun ExpenseRecord.toJson() = JSONObject().apply {
    put("expenseId", expenseId)
    put("category", category)
    put("amount", amount)
    put("note", note)
    put("date", date)
    put("sourceType", sourceType)
    put("sourceId", sourceId)
    put("fieldId", fieldId)
    put("paymentStatus", paymentStatus)
}
private fun SaleRecord.toJson() = JSONObject().apply {
    put("saleId", saleId)
    put("product", product)
    put("amount", amount)
    put("date", date)
    put("incomeCategory", incomeCategory)
    put("sourceType", sourceType)
    put("sourceId", sourceId)
    put("fieldId", fieldId)
    put("paymentStatus", paymentStatus)
}
private fun InventoryItem.toJson() = JSONObject().apply {
    put("name", name)
    put("quantity", quantity)
    put("status", status)
    put("inventoryId", inventoryId)
    put("category", category)
    put("stock", stock)
    put("unit", unit)
    put("purchasePrice", purchasePrice)
    put("supplier", supplier)
    put("dateAcquired", dateAcquired)
    put("expiryDate", expiryDate)
}

private fun InventoryTransaction.toJson() = JSONObject().apply {
    put("transactionId", transactionId)
    put("inventoryId", inventoryId)
    put("type", type)
    put("quantity", quantity)
    put("unit", unit)
    put("date", date)
    put("sourceType", sourceType)
    put("sourceId", sourceId)
    put("notes", notes)
    put("createdAt", createdAt)
}
private fun FeedLogRecord.toJson() = JSONObject().apply {
    put("feedLogId", feedLogId)
    put("livestockId", livestockId)
    put("date", date)
    put("feedName", feedName)
    put("quantityKg", quantityKg)
    put("unit", unit)
    put("notes", notes)
    put("createdAt", createdAt)
}

private fun EquipmentRecord.toJson() = JSONObject().apply {
    put("name", name); put("status", status); put("note", note)
}
private fun FarmTask.toJson() = JSONObject().apply {
    put("title", title); put("category", category); put("date", date); put("done", done)
}
private fun AssistanceRecord.toJson() = JSONObject().apply {
    put("program", program)
    put("assistanceType", assistanceType)
    put("dateReceived", dateReceived)
    put("quantity", quantity)
    put("status", status)
    put("source", source)
    put("incidentId", incidentId)
    put("requestId", requestId)
    put("approvedDate", approvedDate)
    put("distributedDate", distributedDate)
    put("completedDate", completedDate)
    put("distributionDetails", distributionDetails)
    put("outcome", outcome)
    put("reviewNotes", reviewNotes)
}
private fun FieldIncident.toJson() = JSONObject().apply {
    put("id", id)
    put("type", type)
    put("commodity", commodity)
    put("affectedArea", affectedArea)
    put("date", date)
    put("severity", severity)
    put("description", description)
    put("status", status)
    put("evidenceUri", evidenceUri)
    put("reviewNotes", reviewNotes)
    put("farmerId", farmerId)
    put("farmId", farmId)
    put("fieldId", fieldId)
    if (latitude == null) put("latitude", JSONObject.NULL) else put("latitude", latitude)
    if (longitude == null) put("longitude", JSONObject.NULL) else put("longitude", longitude)
    put("reviewer", reviewer)
    put("assistanceRequestId", assistanceRequestId)
    put("resolution", resolution)
    put("affectedAreaBoundary", JSONArray(affectedAreaBoundary.map { point ->
        JSONObject().apply {
            put("latitude", point.latitude)
            put("longitude", point.longitude)
        }
    }))
}
private fun IncidentEvent.toJson() = JSONObject().apply {
    put("incidentId", incidentId)
    put("status", status)
    put("note", note)
    put("timestamp", timestamp)
    put("fromStatus", fromStatus)
    put("actor", actor)
    put("eventId", eventId)
}

private fun jsonToSnapshot(json: JSONObject): FarmSnapshot {
    fun array(key: String): JSONArray = json.optJSONArray(key) ?: JSONArray()
    fun op(key: String): JSONObject = json.optJSONObject(key) ?: JSONObject()
    val p = op("farmerProfile")
    val farmJson = op("farm")
    fun decodeMapPoints(value: JSONArray?): List<MapPoint> {
        if (value == null) return emptyList()
        return buildList {
            for (i in 0 until value.length()) {
                val point = value.optJSONObject(i) ?: continue
                val lat = point.optDouble("latitude", Double.NaN)
                val lon = point.optDouble("longitude", Double.NaN)
                if (lat.isFinite() && lon.isFinite()) add(MapPoint(lat, lon))
            }
        }
    }
    val profile = FarmerProfile(
        p.optString("farmerName"),
        p.optString("farmerId"),
        p.optString("contact"),
        p.optString("province"),
        p.optString("municipality"),
        p.optString("barangay"),
        p.optString("farmName"),
        p.optString("farmSize"),
        p.optString("landTenure"),
        p.optString("commodities"),
        p.optString("registryStatus", "For Review"),
        p.optString("reviewNotes")
    )
    val backupFarm = FarmRecord(
        farmId = farmJson.optString("farmId", FarmRepository.DEFAULT_FARM_ID),
        farmerId = farmJson.optString("farmerId", profile.farmerId),
        farmName = farmJson.optString("farmName", profile.farmName),
        province = farmJson.optString("province", profile.province),
        municipality = farmJson.optString("municipality", profile.municipality),
        barangay = farmJson.optString("barangay", profile.barangay),
        totalArea = farmJson.optString("totalArea", profile.farmSize),
        landTenure = farmJson.optString("landTenure", profile.landTenure),
        commodities = farmJson.optString("commodities", profile.commodities),
        registryStatus = farmJson.optString("registryStatus", profile.registryStatus),
        reviewNotes = farmJson.optString("reviewNotes", profile.reviewNotes),
        boundaryPoints = decodeMapPoints(farmJson.optJSONArray("boundaryPoints"))
    )
    val operational = op("operationalRecords")
    val livestockArray = operational.optJSONArray("livestock") ?: array("livestock")
    val cropsArray = operational.optJSONArray("crops") ?: array("crops")
    val productionArray = operational.optJSONArray("production") ?: array("production")
    val expensesArray = operational.optJSONArray("expenses") ?: array("expenses")
    val salesArray = operational.optJSONArray("sales") ?: array("sales")
    val inventoryArray = operational.optJSONArray("inventory") ?: array("inventory")
    val equipmentArray = array("equipment")
    val tasksArray = array("tasks")

    fun parseList(a: JSONArray, parser: (JSONObject, Int) -> Unit) {
        for (i in 0 until a.length()) parser(a.getJSONObject(i), i)
    }

    val livestock = mutableListOf<Livestock>()
    parseList(livestockArray) { o, _ ->
        val count = o.optInt("count")
        val legacyGroupId = o.optString("groupId").ifBlank { "LIV-" + o.optString("name") }
        livestock += Livestock(
            name = o.optString("name"),
            kind = o.optString("kind"),
            count = o.optInt("currentPopulation", count),
            status = o.optString("status"),
            groupId = legacyGroupId,
            initialPopulation = o.optInt("initialPopulation", count),
            currentPopulation = o.optInt("currentPopulation", count)
        )
    }

    val livestockLifecycleEvents = mutableListOf<LivestockLifecycleEvent>()
    parseList(array("livestockLifecycleEvents")) { o, _ ->
        livestockLifecycleEvents += LivestockLifecycleEvent(
            eventId = o.optString("eventId"),
            livestockId = o.optString("livestockId"),
            stage = o.optString("stage", "Population"),
            date = o.optString("date"),
            notes = o.optString("notes"),
            inputName = o.optString("inputName"),
            quantity = o.optDouble("quantity", 0.0),
            unit = o.optString("unit"),
            amount = o.optDouble("amount", 0.0),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
    val crops = mutableListOf<CropRecord>()
    parseList(cropsArray) { o, _ ->
        val legacyStage = o.optString("stage", "Land Preparation")
        crops += CropRecord(
            name = o.optString("name"),
            crop = o.optString("crop"),
            area = o.optString("area"),
            stage = legacyStage,
            cropId = o.optString("cropId"),
            fieldId = o.optString("fieldId"),
            plantingDate = o.optString("plantingDate"),
            expectedHarvest = o.optString("expectedHarvest"),
            currentStatus = o.optString("currentStatus", legacyStage)
        )
    }

    val cropLifecycleEvents = mutableListOf<CropLifecycleEvent>()
    parseList(array("cropLifecycleEvents")) { o, _ ->
        cropLifecycleEvents += CropLifecycleEvent(
            eventId = o.optString("eventId"),
            cropId = o.optString("cropId"),
            fieldId = o.optString("fieldId"),
            stage = o.optString("stage", "Land Preparation"),
            date = o.optString("date"),
            notes = o.optString("notes"),
            inputName = o.optString("inputName"),
            quantity = o.optString("quantity"),
            unit = o.optString("unit"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
    val production = mutableListOf<ProductionRecord>()
    parseList(productionArray) { o, _ ->
        production += ProductionRecord(
            product = o.optString("product"),
            quantity = o.optString("quantity"),
            period = o.optString("period"),
            productionId = o.optString("productionId"),
            sourceType = o.optString("sourceType"),
            sourceId = o.optString("sourceId"),
            fieldId = o.optString("fieldId"),
            areaHectares = o.optDouble("areaHectares", 0.0),
            productionType = o.optString("productionType", "Harvest")
        )
    }
    val expenses = mutableListOf<ExpenseRecord>()
    parseList(expensesArray) { o, _ -> expenses += ExpenseRecord(
            category = o.optString("category"),
            amount = o.optDouble("amount", 0.0),
            note = o.optString("note"),
            date = o.optString("date"),
            sourceType = o.optString("sourceType"),
            sourceId = o.optString("sourceId"),
            fieldId = o.optString("fieldId"),
            expenseId = o.optString("expenseId"),
            paymentStatus = o.optString("paymentStatus", "Paid")
        ) }
    val sales = mutableListOf<SaleRecord>()
    parseList(salesArray) { o, _ -> sales += SaleRecord(
            product = o.optString("product"),
            amount = o.optDouble("amount", 0.0),
            date = o.optString("date"),
            incomeCategory = o.optString("incomeCategory", "Other Income"),
            sourceType = o.optString("sourceType"),
            sourceId = o.optString("sourceId"),
            fieldId = o.optString("fieldId"),
            saleId = o.optString("saleId"),
            paymentStatus = o.optString("paymentStatus", "Paid")
        ) }
    val inventory = mutableListOf<InventoryItem>()
    parseList(inventoryArray) { o, _ ->
        val legacyQuantity = o.optString("quantity")
        val parts = legacyQuantity.trim().split(" ", limit = 2)
        val legacyStock = o.optDouble("stock", parts.firstOrNull()?.toDoubleOrNull() ?: 0.0)
        val legacyUnit = o.optString("unit").ifBlank { parts.getOrNull(1).orEmpty() }
        inventory += InventoryItem(
            name = o.optString("name"),
            quantity = legacyQuantity,
            status = o.optString("status"),
            inventoryId = o.optString("inventoryId"),
            category = o.optString("category", "Other"),
            stock = legacyStock,
            unit = legacyUnit,
            purchasePrice = o.optDouble("purchasePrice", 0.0),
            supplier = o.optString("supplier"),
            dateAcquired = o.optString("dateAcquired"),
            expiryDate = o.optString("expiryDate")
        )
    }

    val inventoryTransactions = mutableListOf<InventoryTransaction>()
    parseList(array("inventoryTransactions")) { o, _ ->
        inventoryTransactions += InventoryTransaction(
            transactionId = o.optString("transactionId"),
            inventoryId = o.optString("inventoryId"),
            type = o.optString("type"),
            quantity = o.optDouble("quantity", 0.0),
            unit = o.optString("unit"),
            date = o.optString("date"),
            sourceType = o.optString("sourceType"),
            sourceId = o.optString("sourceId"),
            notes = o.optString("notes"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
    val feedLogs = mutableListOf<FeedLogRecord>()
    parseList(array("feedLogs")) { o, _ ->
        feedLogs += FeedLogRecord(
            feedLogId = o.optString("feedLogId"),
            livestockId = o.optString("livestockId"),
            date = o.optString("date"),
            feedName = o.optString("feedName"),
            quantityKg = o.optDouble("quantityKg", 0.0),
            unit = o.optString("unit", "kg"),
            notes = o.optString("notes"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    val equipment = mutableListOf<EquipmentRecord>()
    parseList(equipmentArray) { o, _ -> equipment += EquipmentRecord(o.optString("name"), o.optString("status"), o.optString("note")) }
    val tasks = mutableListOf<FarmTask>()
    parseList(tasksArray) { o, _ -> tasks += FarmTask(o.optString("title"), o.optString("category"), o.optString("date"), o.optBoolean("done")) }

    val assistance = mutableListOf<AssistanceRecord>()
    parseList(array("assistance")) { o, _ ->
        assistance += AssistanceRecord(
            o.optString("program"), o.optString("assistanceType"), o.optString("dateReceived"),
            o.optString("quantity"), o.optString("status", "Applied"), o.optString("source"),
            o.optString("incidentId"), o.optString("requestId"), o.optString("approvedDate"),
            o.optString("distributedDate"), o.optString("completedDate"),
            o.optString("distributionDetails"), o.optString("outcome"), o.optString("reviewNotes")
        )
    }
    val fields = mutableListOf<FieldRecord>()
    parseList(array("fields")) { o, _ ->
        fields += FieldRecord(
            fieldId = o.optString("fieldId"),
            farmId = o.optString("farmId", FarmRepository.DEFAULT_FARM_ID),
            name = o.optString("name"),
            areaHectares = o.optDouble("areaHectares", 0.0),
            location = o.optString("location"),
            latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
            longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
            landTenure = o.optString("landTenure"),
            crop = o.optString("crop"),
            plantingDate = o.optString("plantingDate"),
            expectedHarvest = o.optString("expectedHarvest"),
            currentStatus = o.optString("currentStatus", "Planned"),
            boundaryPoints = decodeMapPoints(o.optJSONArray("boundaryPoints"))
        )
    }

    val incidents = mutableListOf<FieldIncident>()
    parseList(array("fieldIncidents")) { o, _ ->
        incidents += FieldIncident(
            id = o.optString("id"),
            type = o.optString("type", "Other"),
            commodity = o.optString("commodity"),
            affectedArea = o.optString("affectedArea"),
            date = o.optString("date"),
            severity = o.optString("severity", "Moderate"),
            description = o.optString("description"),
            status = o.optString("status", "Draft"),
            evidenceUri = o.optString("evidenceUri"),
            reviewNotes = o.optString("reviewNotes"),
            farmerId = o.optString("farmerId"),
            farmId = o.optString("farmId"),
            fieldId = o.optString("fieldId"),
            latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
            longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
            reviewer = o.optString("reviewer"),
            assistanceRequestId = o.optString("assistanceRequestId"),
            resolution = o.optString("resolution"),
            affectedAreaBoundary = decodeMapPoints(o.optJSONArray("affectedAreaBoundary"))
        )
    }
    val events = mutableListOf<IncidentEvent>()
    parseList(array("incidentEvents")) { o, _ ->
        events += IncidentEvent(
            incidentId = o.optString("incidentId"),
            status = o.optString("status"),
            note = o.optString("note"),
            timestamp = o.optLong("timestamp"),
            fromStatus = o.optString("fromStatus"),
            actor = o.optString("actor"),
            eventId = o.optString("eventId")
        )
    }

    val report = op("reportSubmission")
    return FarmSnapshot(
        profile, livestock, crops, production, expenses, sales, inventory, equipment,
        tasks, assistance, incidents, events,
        ReportSubmission(
            report.optString("status", "Draft"),
            report.optString("submittedDate"),
            report.optString("referenceNo")
        ),
        fields,
        cropLifecycleEvents,
        livestockLifecycleEvents,
        inventoryTransactions,
        feedLogs,
        farm = backupFarm
    )
}


private fun List<MapPoint>.centroid(): MapPoint? {
    if (isEmpty()) return null
    return MapPoint(
        latitude = map { it.latitude }.average(),
        longitude = map { it.longitude }.average()
    )
}
