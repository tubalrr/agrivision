package com.tubalrr.agrivision.data.local

import com.tubalrr.agrivision.AssistanceRecord
import com.tubalrr.agrivision.CropRecord
import com.tubalrr.agrivision.EquipmentRecord
import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.FarmTask
import com.tubalrr.agrivision.FarmerProfile
import com.tubalrr.agrivision.FieldIncident
import com.tubalrr.agrivision.IncidentEvent
import com.tubalrr.agrivision.InventoryItem
import com.tubalrr.agrivision.Livestock
import com.tubalrr.agrivision.ProductionRecord
import com.tubalrr.agrivision.ReportSubmission
import com.tubalrr.agrivision.SaleRecord
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.math.roundToInt

data class FarmSnapshot(
    val profile: FarmerProfile,
    val livestock: List<Livestock>,
    val crops: List<CropRecord>,
    val production: List<ProductionRecord>,
    val expenses: List<ExpenseRecord>,
    val sales: List<SaleRecord>,
    val inventory: List<InventoryItem>,
    val equipment: List<EquipmentRecord>,
    val tasks: List<FarmTask>,
    val assistance: List<AssistanceRecord>,
    val fieldIncidents: List<FieldIncident>,
    val incidentEvents: List<IncidentEvent>,
    val reportSubmission: ReportSubmission
)

class FarmRepository(
    private val database: AgriDatabase,
    private val dao: FarmDao = database.farmDao()
) {

    companion object {
        const val DEFAULT_FARM_ID = "default-farm"
        const val DEFAULT_SUBMISSION_ID = "default"
    }

    fun observeProfile(): Flow<FarmerProfile> =
        dao.observeFarm(DEFAULT_FARM_ID).map { farm ->
            if (farm == null) {
                FarmerProfile("", "", "", "", "", "", "", "", "", "")
            } else {
                FarmerProfile(
                    farmerName = farm.farmerName,
                    farmerId = farm.farmerId,
                    contact = farm.contact,
                    province = farm.province,
                    municipality = farm.municipality,
                    barangay = farm.barangay,
                    farmName = farm.farmName,
                    farmSize = farm.farmSize,
                    landTenure = farm.landTenure,
                    commodities = farm.commodities,
                    registryStatus = farm.registryStatus,
                    reviewNotes = farm.reviewNotes
                )
            }
        }

    fun observeLivestock(): Flow<List<Livestock>> =
        dao.observeLivestock(DEFAULT_FARM_ID).map { list ->
            list.map { Livestock(it.name, it.kind, it.count, it.status) }
        }

    fun observeCrops(): Flow<List<CropRecord>> =
        dao.observeCrops(DEFAULT_FARM_ID).map { list ->
            list.map { CropRecord(it.name, it.crop, it.area, it.stage) }
        }

    fun observeProduction(): Flow<List<ProductionRecord>> =
        dao.observeProduction(DEFAULT_FARM_ID).map { list ->
            list.map { ProductionRecord(it.commodity, formatQuantity(it.quantity, it.unit), it.date) }
        }

    fun observeExpenses(): Flow<List<ExpenseRecord>> =
        dao.observeExpenses(DEFAULT_FARM_ID).map { list ->
            list.map { ExpenseRecord(it.category, it.amount, it.note) }
        }

    fun observeSales(): Flow<List<SaleRecord>> =
        dao.observeSales(DEFAULT_FARM_ID).map { list ->
            list.map { SaleRecord(it.product, it.amount, it.date) }
        }

    fun observeInventory(): Flow<List<InventoryItem>> =
        dao.observeInventory(DEFAULT_FARM_ID).map { list ->
            list.map { InventoryItem(it.name, formatQuantity(it.quantity, it.unit), it.status) }
        }

    fun observeEquipment(): Flow<List<EquipmentRecord>> =
        dao.observeEquipment(DEFAULT_FARM_ID).map { list ->
            list.map { EquipmentRecord(it.name, it.status, it.note) }
        }

    fun observeTasks(): Flow<List<FarmTask>> =
        dao.observeTasks(DEFAULT_FARM_ID).map { list ->
            list.map { FarmTask(it.title, it.category, it.date, it.done) }
        }

    fun observeAssistance(): Flow<List<AssistanceRecord>> =
        dao.observeAssistance(DEFAULT_FARM_ID).map { list ->
            list.map {
                AssistanceRecord(
                    program = it.program,
                    assistanceType = it.assistanceType,
                    dateReceived = it.dateReceived,
                    quantity = it.quantity,
                    status = it.status,
                    source = it.source,
                    incidentId = it.incidentId,
                    requestId = it.requestId,
                    approvedDate = it.approvedDate,
                    distributedDate = it.distributedDate,
                    completedDate = it.completedDate,
                    distributionDetails = it.distributionDetails,
                    outcome = it.outcome,
                    reviewNotes = it.reviewNotes
                )
            }
        }

    fun observeFieldIncidents(): Flow<List<FieldIncident>> =
        dao.observeFieldIncidents(DEFAULT_FARM_ID).map { list ->
            list.map {
                FieldIncident(
                    id = it.incidentId,
                    type = it.type,
                    commodity = it.commodity,
                    affectedArea = it.affectedArea,
                    date = it.date,
                    severity = it.severity,
                    description = it.description,
                    status = it.status,
                    evidenceUri = it.evidenceUri,
                    reviewNotes = it.reviewNotes
                )
            }
        }

    fun observeIncidentEvents(): Flow<List<IncidentEvent>> =
        dao.observeIncidentEvents(DEFAULT_FARM_ID).map { list ->
            list.map {
                IncidentEvent(
                    incidentId = it.incidentId,
                    status = it.status,
                    note = it.note,
                    timestamp = it.timestamp
                )
            }
        }

    fun observeReportSubmission(): Flow<ReportSubmission> =
        dao.observeReportSubmission(DEFAULT_FARM_ID).map {
            if (it == null) ReportSubmission("Draft", "", "")
            else ReportSubmission(it.status, it.submittedDate, it.referenceNo)
        }

    fun observeTotalSales(): Flow<Double> = dao.observeTotalSales(DEFAULT_FARM_ID)
    fun observeTotalExpenses(): Flow<Double> = dao.observeTotalExpenses(DEFAULT_FARM_ID)
    fun observeProductionCount(): Flow<Int> = dao.observeProductionCount(DEFAULT_FARM_ID)
    fun observeTotalLivestock(): Flow<Int> = dao.observeTotalLivestock(DEFAULT_FARM_ID)

    suspend fun saveProfile(profile: FarmerProfile) {
        dao.upsertFarm(
            FarmEntity(
                farmId = DEFAULT_FARM_ID,
                farmerId = profile.farmerId,
                farmerName = profile.farmerName,
                farmName = profile.farmName,
                province = profile.province,
                municipality = profile.municipality,
                barangay = profile.barangay,
                farmSize = profile.farmSize,
                landTenure = profile.landTenure,
                commodities = profile.commodities,
                contact = profile.contact,
                registryStatus = profile.registryStatus,
                reviewNotes = profile.reviewNotes
            )
        )
    }

    suspend fun saveLivestock(record: Livestock, livestockId: String = UUID.randomUUID().toString()) {
        dao.upsertLivestock(
            LivestockEntity(
                livestockId = livestockId,
                farmId = DEFAULT_FARM_ID,
                name = record.name,
                kind = record.kind,
                count = record.count,
                status = record.status
            )
        )
    }

    suspend fun saveCrop(record: CropRecord, cropId: String = UUID.randomUUID().toString()) {
        dao.upsertCrop(
            CropEntity(
                cropId = cropId,
                farmId = DEFAULT_FARM_ID,
                name = record.name,
                crop = record.crop,
                area = record.area,
                stage = record.stage
            )
        )
    }

    suspend fun saveInventory(record: InventoryItem, inventoryId: String = UUID.randomUUID().toString()) {
        val parsed = parseQuantity(record.quantity)
        dao.upsertInventory(
            InventoryEntity(
                inventoryId = inventoryId,
                farmId = DEFAULT_FARM_ID,
                name = record.name,
                quantity = parsed.first,
                unit = parsed.second,
                status = record.status
            )
        )
    }

    suspend fun saveEquipment(record: EquipmentRecord, equipmentId: String = UUID.randomUUID().toString()) {
        dao.upsertEquipment(
            EquipmentEntity(
                equipmentId = equipmentId,
                farmId = DEFAULT_FARM_ID,
                name = record.name,
                status = record.status,
                note = record.note
            )
        )
    }

    suspend fun saveProduction(record: ProductionRecord, productionId: String = UUID.randomUUID().toString()) {
        val parsed = parseQuantity(record.quantity)
        dao.upsertProduction(
            ProductionEntity(
                productionId = productionId,
                farmId = DEFAULT_FARM_ID,
                date = record.period,
                productionType = "Farm Production",
                commodity = record.product,
                quantity = parsed.first,
                unit = parsed.second
            )
        )
    }

    suspend fun saveExpense(record: ExpenseRecord, expenseId: String = UUID.randomUUID().toString()) {
        dao.upsertExpense(
            ExpenseEntity(
                expenseId = expenseId,
                farmId = DEFAULT_FARM_ID,
                date = record.note,
                category = record.category,
                amount = record.amount,
                note = record.note
            )
        )
    }

    suspend fun saveSale(record: SaleRecord, saleId: String = UUID.randomUUID().toString()) {
        dao.upsertSale(
            SaleEntity(
                saleId = saleId,
                farmId = DEFAULT_FARM_ID,
                date = record.date,
                product = record.product,
                amount = record.amount
            )
        )
    }

    suspend fun saveTask(record: FarmTask, taskId: String = UUID.randomUUID().toString()) {
        dao.upsertTask(
            FarmTaskEntity(
                taskId = taskId,
                farmId = DEFAULT_FARM_ID,
                title = record.title,
                category = record.category,
                date = record.date,
                done = record.done
            )
        )
    }

    suspend fun updateTask(task: FarmTask) {
        val entity = dao.observeTasks(DEFAULT_FARM_ID).first().firstOrNull {
            it.title == task.title &&
                    it.category == task.category &&
                    it.date == task.date
        } ?: return
        dao.upsertTask(entity.copy(done = task.done, updatedAt = System.currentTimeMillis()))
    }

    suspend fun saveAssistance(record: AssistanceRecord) {
        val requestId = record.requestId.ifBlank { "DAR-" + System.currentTimeMillis() }
        dao.upsertAssistance(
            AssistanceEntity(
                requestId = requestId,
                farmId = DEFAULT_FARM_ID,
                incidentId = record.incidentId,
                program = record.program,
                assistanceType = record.assistanceType,
                dateReceived = record.dateReceived,
                quantity = record.quantity,
                status = record.status,
                source = record.source,
                approvedDate = record.approvedDate,
                distributedDate = record.distributedDate,
                completedDate = record.completedDate,
                distributionDetails = record.distributionDetails,
                outcome = record.outcome,
                reviewNotes = record.reviewNotes
            )
        )
    }

    suspend fun updateAssistance(record: AssistanceRecord) {
        saveAssistance(record)
    }

    suspend fun saveFieldIncident(record: FieldIncident) {
        val now = System.currentTimeMillis()
        dao.upsertFieldIncident(
            FieldIncidentEntity(
                incidentId = record.id,
                farmId = DEFAULT_FARM_ID,
                type = record.type,
                commodity = record.commodity,
                affectedArea = record.affectedArea,
                date = record.date,
                severity = record.severity,
                description = record.description,
                status = record.status,
                evidenceUri = record.evidenceUri,
                reviewNotes = record.reviewNotes,
                updatedAt = now
            )
        )
    }

    suspend fun saveIncidentEvent(event: IncidentEvent) {
        dao.upsertIncidentEvent(
            IncidentEventEntity(
                eventId = UUID.randomUUID().toString(),
                farmId = DEFAULT_FARM_ID,
                incidentId = event.incidentId,
                status = event.status,
                note = event.note,
                timestamp = event.timestamp
            )
        )
    }

    suspend fun saveReportSubmission(record: ReportSubmission) {
        dao.upsertReportSubmission(
            ReportSubmissionEntity(
                submissionId = DEFAULT_SUBMISSION_ID,
                farmId = DEFAULT_FARM_ID,
                status = record.status,
                submittedDate = record.submittedDate,
                referenceNo = record.referenceNo
            )
        )
    }

    suspend fun createAssistanceForIncident(incident: FieldIncident): AssistanceRecord {
        val request = AssistanceRecord(
            program = "Field Incident Assistance",
            assistanceType = incident.type,
            dateReceived = "",
            quantity = incident.affectedArea,
            status = "Applied",
            source = "Linked to " + incident.id,
            incidentId = incident.id,
            requestId = "DAR-" + System.currentTimeMillis()
        )
        saveAssistance(request)
        return request
    }

    suspend fun replaceAll(snapshot: FarmSnapshot) {
        database.withTransaction {
            dao.clearLivestock()
            dao.clearCrops()
            dao.clearFeedLogs()
            dao.clearProduction()
            dao.clearExpenses()
            dao.clearSales()
            dao.clearInventory()
            dao.clearEquipment()
            dao.clearTasks()
            dao.clearAssistance()
            dao.clearFieldIncidents()
            dao.clearIncidentEvents()
            dao.clearReportSubmissions()
            dao.clearFarms()

            saveProfile(snapshot.profile)
            snapshot.livestock.forEach { saveLivestock(it) }
            snapshot.crops.forEach { saveCrop(it) }
            snapshot.production.forEach { saveProduction(it) }
            snapshot.expenses.forEach { saveExpense(it) }
            snapshot.sales.forEach { saveSale(it) }
            snapshot.inventory.forEach { saveInventory(it) }
            snapshot.equipment.forEach { saveEquipment(it) }
            snapshot.tasks.forEach { saveTask(it) }
            snapshot.assistance.forEach { saveAssistance(it) }
            snapshot.fieldIncidents.forEach { saveFieldIncident(it) }
            snapshot.incidentEvents.forEach { saveIncidentEvent(it) }
            saveReportSubmission(snapshot.reportSubmission)
        }
    }

    private fun parseQuantity(raw: String): Pair<Double, String> {
        val cleaned = raw.trim()
        val number = Regex("""-?\d+(?:\.\d+)?""").find(cleaned)?.value?.toDoubleOrNull() ?: 0.0
        val unit = cleaned.replace(Regex("""-?\d+(?:\.\d+)?"""), "").trim().ifBlank { "unit" }
        return number to unit
    }

    private fun formatQuantity(value: Double, unit: String): String {
        val number = if (value % 1.0 == 0.0) value.roundToInt().toString() else String.format(java.util.Locale.US, "%.2f", value)
        return if (unit.isBlank() || unit == "unit") number else "$number $unit"
    }
}
