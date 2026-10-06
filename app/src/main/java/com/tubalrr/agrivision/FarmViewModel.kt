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
    val crops: StateFlow<List<CropRecord>> = repository.observeCrops().stateIn(
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

    fun addLivestock(value: Livestock) = launch { repository.saveLivestock(value) }
    fun addCrop(value: CropRecord) = launch { repository.saveCrop(value) }
    fun addInventory(value: InventoryItem) = launch { repository.saveInventory(value) }
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
        repository.saveFieldIncident(value)
        repository.saveIncidentEvent(
            IncidentEvent(value.id, value.status, "Report created")
        )
    }

    fun updateFieldIncident(value: FieldIncident, previousStatus: String) = launch {
        repository.saveFieldIncident(value)
        if (value.status != previousStatus) {
            repository.saveIncidentEvent(
                IncidentEvent(
                    incidentId = value.id,
                    status = value.status,
                    note = when (value.status) {
                        "Submitted" -> "Farmer submitted field report"
                        "Resolved" -> "Case closed after closure gate"
                        else -> "Case status updated"
                    }
                )
            )
        }
    }

    fun reviewFieldIncident(previous: FieldIncident, updated: FieldIncident) = launch {
        repository.saveFieldIncident(updated)
        if (updated.status != previous.status || updated.reviewNotes != previous.reviewNotes) {
            repository.saveIncidentEvent(
                IncidentEvent(
                    incidentId = updated.id,
                    status = updated.status,
                    note = updated.reviewNotes.ifBlank { "Reviewer updated case status" }
                )
            )
        }
    }

    fun requestAssistance(incident: FieldIncident) = launch {
        val alreadyLinked = assistance.value.any { it.incidentId == incident.id }
        if (!alreadyLinked) {
            repository.createAssistanceForIncident(incident)
            repository.saveIncidentEvent(
                IncidentEvent(
                    incidentId = incident.id,
                    status = "Assistance Requested",
                    note = "Assistance request linked to verified incident"
                )
            )
        }
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
            put("packageVersion", 3)
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
                put("crops", JSONArray(snapshot.crops.map { it.toJson() }))
                put("production", JSONArray(snapshot.production.map { it.toJson() }))
                put("expenses", JSONArray(snapshot.expenses.map { it.toJson() }))
                put("sales", JSONArray(snapshot.sales.map { it.toJson() }))
                put("inventory", JSONArray(snapshot.inventory.map { it.toJson() }))
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
            equipment = repository.observeEquipment().first(),
            tasks = repository.observeTasks().first(),
            assistance = repository.observeAssistance().first(),
            fieldIncidents = repository.observeFieldIncidents().first(),
            incidentEvents = repository.observeIncidentEvents().first(),
            reportSubmission = reportSubmission.value,
            fields = repository.observeFields().first()
        )
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

private fun snapshotToJson(snapshot: FarmSnapshot): JSONObject =
    JSONObject().apply {
        put("version", 3)
        put("app", "AgriVision")
        put("farmerProfile", snapshot.profile.toJson())
        put("livestock", JSONArray(snapshot.livestock.map { it.toJson() }))
        put("crops", JSONArray(snapshot.crops.map { it.toJson() }))
        put("production", JSONArray(snapshot.production.map { it.toJson() }))
        put("expenses", JSONArray(snapshot.expenses.map { it.toJson() }))
        put("sales", JSONArray(snapshot.sales.map { it.toJson() }))
        put("inventory", JSONArray(snapshot.inventory.map { it.toJson() }))
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
    put("name", name); put("kind", kind); put("count", count); put("status", status)
}
private fun CropRecord.toJson() = JSONObject().apply {
    put("name", name); put("crop", crop); put("area", area); put("stage", stage)
}
private fun ProductionRecord.toJson() = JSONObject().apply {
    put("product", product); put("quantity", quantity); put("period", period)
}
private fun ExpenseRecord.toJson() = JSONObject().apply {
    put("category", category); put("amount", amount); put("note", note)
}
private fun SaleRecord.toJson() = JSONObject().apply {
    put("product", product); put("amount", amount); put("date", date)
}
private fun InventoryItem.toJson() = JSONObject().apply {
    put("name", name); put("quantity", quantity); put("status", status)
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
    put("id", id); put("type", type); put("commodity", commodity)
    put("affectedArea", affectedArea); put("date", date); put("severity", severity)
    put("description", description); put("status", status)
    put("evidenceUri", evidenceUri); put("reviewNotes", reviewNotes)
}
private fun IncidentEvent.toJson() = JSONObject().apply {
    put("incidentId", incidentId); put("status", status); put("note", note); put("timestamp", timestamp)
}

private fun jsonToSnapshot(json: JSONObject): FarmSnapshot {
    fun array(key: String): JSONArray = json.optJSONArray(key) ?: JSONArray()
    fun op(key: String): JSONObject = json.optJSONObject(key) ?: JSONObject()
    val p = op("farmerProfile")
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
    parseList(livestockArray) { o, _ -> livestock += Livestock(o.optString("name"), o.optString("kind"), o.optInt("count"), o.optString("status")) }
    val crops = mutableListOf<CropRecord>()
    parseList(cropsArray) { o, _ -> crops += CropRecord(o.optString("name"), o.optString("crop"), o.optString("area"), o.optString("stage")) }
    val production = mutableListOf<ProductionRecord>()
    parseList(productionArray) { o, _ -> production += ProductionRecord(o.optString("product"), o.optString("quantity"), o.optString("period")) }
    val expenses = mutableListOf<ExpenseRecord>()
    parseList(expensesArray) { o, _ -> expenses += ExpenseRecord(o.optString("category"), o.optDouble("amount"), o.optString("note")) }
    val sales = mutableListOf<SaleRecord>()
    parseList(salesArray) { o, _ -> sales += SaleRecord(o.optString("product"), o.optDouble("amount"), o.optString("date")) }
    val inventory = mutableListOf<InventoryItem>()
    parseList(inventoryArray) { o, _ -> inventory += InventoryItem(o.optString("name"), o.optString("quantity"), o.optString("status")) }
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
            currentStatus = o.optString("currentStatus", "Planned")
        )
    }

    val incidents = mutableListOf<FieldIncident>()
    parseList(array("fieldIncidents")) { o, _ ->
        incidents += FieldIncident(
            o.optString("id"), o.optString("type", "Other"), o.optString("commodity"),
            o.optString("affectedArea"), o.optString("date"), o.optString("severity", "Moderate"),
            o.optString("description"), o.optString("status", "Draft"), o.optString("evidenceUri"),
            o.optString("reviewNotes")
        )
    }
    val events = mutableListOf<IncidentEvent>()
    parseList(array("incidentEvents")) { o, _ ->
        events += IncidentEvent(
            o.optString("incidentId"), o.optString("status"), o.optString("note"),
            o.optLong("timestamp")
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
        fields
    )
}
