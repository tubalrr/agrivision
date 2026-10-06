package com.tubalrr.agrivision

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tubalrr.agrivision.data.local.AgriDatabase
import com.tubalrr.agrivision.data.local.FarmRepository
import com.tubalrr.agrivision.data.local.FarmSnapshot
import com.tubalrr.agrivision.data.local.LegacyPreferencesMigrator
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FarmerRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import com.tubalrr.agrivision.domain.model.MapPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AgriDatabase.getInstance(application)
    private val repository = FarmRepository(database)

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
        repository.saveFarmBoundary(points)
    }

    fun saveFieldMapping(field: FieldRecord, points: List<MapPoint>) = launch {
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
    fun addEquipment(value: EquipmentRecord) = launch { repository.saveEquipment(value) }

    fun addProduction(value: ProductionRecord) = launch { repository.saveProduction(value) }
    fun addExpense(value: ExpenseRecord) = launch { repository.saveExpense(value) }
    fun addSale(value: SaleRecord) = launch { repository.saveSale(value) }

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
        val snapshot = currentSnapshot()
        val json = snapshotToJson(snapshot)
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
            it.write(json.toString(2).toByteArray(Charsets.UTF_8))
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
            })
        }
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
            it.write(json.toString(2).toByteArray(Charsets.UTF_8))
        }
    }

    fun importBackup(uri: Uri) = launch {
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: return@launch
        val text = input.bufferedReader().use { it.readText() }
        val json = JSONObject(text)
        val snapshot = jsonToSnapshot(json)
        repository.replaceAll(snapshot)
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
        put("version", 10)
        put("app", "AgriVision")
        put("farmerProfile", snapshot.profile.toJson())
        put("farm", (snapshot.farm ?: snapshot.profile.toFarmRecord()).toJson())
        put("livestock", JSONArray(snapshot.livestock.map { it.toJson() }))
        put("livestockLifecycleEvents", JSONArray(snapshot.livestockLifecycleEvents.map { it.toJson() }))
        put("crops", JSONArray(snapshot.crops.map { it.toJson() }))
        put("cropLifecycleEvents", JSONArray(snapshot.cropLifecycleEvents.map { it.toJson() }))
        put("production", JSONArray(snapshot.production.map { it.toJson() }))
        put("expenses", JSONArray(snapshot.expenses.map { it.toJson() }))
        put("sales", JSONArray(snapshot.sales.map { it.toJson() }))
        put("inventory", JSONArray(snapshot.inventory.map { it.toJson() }))
        put("inventoryTransactions", JSONArray(snapshot.inventoryTransactions.map { it.toJson() }))
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
    put("category", category)
    put("amount", amount)
    put("note", note)
    put("date", date)
}
private fun SaleRecord.toJson() = JSONObject().apply {
    put("product", product)
    put("amount", amount)
    put("date", date)
    put("incomeCategory", incomeCategory)
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
            date = o.optString("date")
        ) }
    val sales = mutableListOf<SaleRecord>()
    parseList(salesArray) { o, _ -> sales += SaleRecord(
            product = o.optString("product"),
            amount = o.optDouble("amount", 0.0),
            date = o.optString("date"),
            incomeCategory = o.optString("incomeCategory", "Other Income")
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
