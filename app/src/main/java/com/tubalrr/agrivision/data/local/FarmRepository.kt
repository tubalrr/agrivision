package com.tubalrr.agrivision.data.local

import com.tubalrr.agrivision.AssistanceRecord
import com.tubalrr.agrivision.CropRecord
import com.tubalrr.agrivision.CropLifecycleEvent
import com.tubalrr.agrivision.LivestockLifecycleEvent
import com.tubalrr.agrivision.domain.calculator.CropLifecycleCalculator
import com.tubalrr.agrivision.EquipmentRecord
import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.FarmTask
import com.tubalrr.agrivision.FarmerProfile
import com.tubalrr.agrivision.FarmerFarmRegistryEntry
import com.tubalrr.agrivision.FieldIncident
import com.tubalrr.agrivision.IncidentEvent
import com.tubalrr.agrivision.InventoryItem
import com.tubalrr.agrivision.InventoryTransaction
import com.tubalrr.agrivision.Livestock
import com.tubalrr.agrivision.ProductionRecord
import com.tubalrr.agrivision.ReportSubmission
import com.tubalrr.agrivision.SaleRecord
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FarmerRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import com.tubalrr.agrivision.domain.model.MapPoint
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.Locale
import kotlin.math.roundToInt

data class FeedLogRecord(
    val feedLogId: String = "",
    val livestockId: String = "",
    val date: String = "",
    val feedName: String = "",
    val quantityKg: Double = 0.0,
    val unit: String = "kg",
    val notes: String = "",
    val costAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

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
    val reportSubmission: ReportSubmission,
    val fields: List<FieldRecord> = emptyList(),
    val cropLifecycleEvents: List<CropLifecycleEvent> = emptyList(),
    val livestockLifecycleEvents: List<LivestockLifecycleEvent> = emptyList(),
    val inventoryTransactions: List<InventoryTransaction> = emptyList(),
    val feedLogs: List<FeedLogRecord> = emptyList(),
    val farm: FarmRecord? = null
)

class FarmRepository(
    private val database: AgriDatabase,
    private val dao: FarmDao = database.farmDao()
) {

    companion object {
        const val DEFAULT_FARM_ID = "default-farm"
        const val DEFAULT_SUBMISSION_ID = "default"
    }

    fun observeFarmer(): Flow<FarmerRecord> =
        dao.observeFarm(DEFAULT_FARM_ID)
            .flatMapLatest { farm ->
                dao.observeFarmer(farm?.farmerId.orEmpty())
            }
            .map { farmer ->
                if (farmer == null) FarmerRecord("", "")
                else FarmerRecord(farmer.farmerId, farmer.fullName, farmer.contact)
            }

    fun observeFarm(): Flow<FarmRecord> =
        dao.observeFarm(DEFAULT_FARM_ID).map { farm ->
            if (farm == null) FarmRecord(DEFAULT_FARM_ID, "", "")
            else FarmRecord(
                farmId = farm.farmId,
                farmerId = farm.farmerId,
                farmName = farm.farmName,
                province = farm.province,
                municipality = farm.municipality,
                barangay = farm.barangay,
                totalArea = farm.farmSize,
                landTenure = farm.landTenure,
                commodities = farm.commodities,
                registryStatus = farm.registryStatus,
                reviewNotes = farm.reviewNotes,
                boundaryPoints = decodeMapPoints(farm.boundaryPointsJson)
            )
        }

    fun observeFields(): Flow<List<FieldRecord>> =
        dao.observeFields(DEFAULT_FARM_ID).map { list ->
            list.map {
                FieldRecord(
                    fieldId = it.fieldId,
                    farmId = it.farmId,
                    name = it.name,
                    areaHectares = it.areaHectares,
                    location = it.location,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    landTenure = it.landTenure,
                    crop = it.crop,
                    plantingDate = it.plantingDate,
                    expectedHarvest = it.expectedHarvest,
                    currentStatus = it.currentStatus,
                    boundaryPoints = decodeMapPoints(it.boundaryPointsJson)
                )
            }
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

    /**
     * All submitted farmer/farm applications. The operational screens still use DEFAULT_FARM_ID;
     * this registry stream is intentionally separate until the active-farm selector is wired through
     * the operational modules.
     */
    fun observeRegistryEntries(): Flow<List<FarmerFarmRegistryEntry>> =
        dao.observeFarms().map { farms ->
            farms
                .filter { it.farmerName.isNotBlank() || it.farmerId.isNotBlank() || it.farmName.isNotBlank() }
                .map { farm ->
                    FarmerFarmRegistryEntry(
                        farmId = farm.farmId,
                        profile = FarmerProfile(
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
                    )
                }
        }

    /**
     * Saves a new application or edits an existing one. When the default farm slot is still empty,
     * the first application uses it so the existing single-farm modules remain usable. Later
     * applications get their own stable farm IDs and never overwrite another applicant.
     */
    suspend fun saveRegistryEntry(profile: FarmerProfile, existingFarmId: String? = null) {
        require(profile.farmerName.isNotBlank()) { "Farmer name is required." }

        val farmerId = profile.farmerId.trim().ifBlank {
            "FAR-" + System.currentTimeMillis().toString().takeLast(8)
        }
        val defaultFarm = dao.observeFarm(DEFAULT_FARM_ID).first()
        val farmId = existingFarmId?.takeIf { it.isNotBlank() } ?: run {
            if (
                defaultFarm == null ||
                (defaultFarm.farmerName.isBlank() && defaultFarm.farmerId.isBlank() && defaultFarm.farmName.isBlank())
            ) {
                DEFAULT_FARM_ID
            } else {
                "farm-" + UUID.randomUUID().toString()
            }
        }
        val existingFarm = dao.observeFarm(farmId).first()
        val now = System.currentTimeMillis()

        dao.upsertFarmer(
            FarmerEntity(
                farmerId = farmerId,
                fullName = profile.farmerName.trim(),
                contact = profile.contact.trim(),
                updatedAt = now
            )
        )
        dao.upsertFarm(
            FarmEntity(
                farmId = farmId,
                farmerId = farmerId,
                farmerName = profile.farmerName.trim(),
                farmName = profile.farmName.trim(),
                province = profile.province.trim(),
                municipality = profile.municipality.trim(),
                barangay = profile.barangay.trim(),
                farmSize = profile.farmSize.trim(),
                landTenure = profile.landTenure.trim(),
                commodities = profile.commodities.trim(),
                contact = profile.contact.trim(),
                registryStatus = profile.registryStatus.ifBlank { "For Review" },
                reviewNotes = profile.reviewNotes,
                boundaryPointsJson = existingFarm?.boundaryPointsJson ?: "[]",
                updatedAt = now
            )
        )
    }

    fun observeLivestock(): Flow<List<Livestock>> =
        dao.observeLivestock(DEFAULT_FARM_ID).map { list ->
            list.map {
                Livestock(
                    name = it.name,
                    kind = it.kind,
                    count = it.currentPopulation,
                    status = it.status,
                    groupId = it.groupId,
                    initialPopulation = it.initialPopulation,
                    currentPopulation = it.currentPopulation
                )
            }
        }

    fun observeLivestockLifecycleEvents(): Flow<List<LivestockLifecycleEvent>> =
        dao.observeLivestockLifecycleEvents(DEFAULT_FARM_ID).map { list ->
            list.map {
                LivestockLifecycleEvent(
                    eventId = it.eventId,
                    livestockId = it.livestockId,
                    stage = it.stage,
                    date = it.date,
                    notes = it.notes,
                    inputName = it.inputName,
                    quantity = it.quantity,
                    unit = it.unit,
                    amount = it.amount,
                    createdAt = it.createdAt
                )
            }
        }

    fun observeCrops(): Flow<List<CropRecord>> =
        dao.observeCrops(DEFAULT_FARM_ID).map { list ->
            list.map {
                CropRecord(
                    name = it.name,
                    crop = it.crop,
                    area = it.area,
                    stage = it.stage,
                    cropId = it.cropId,
                    fieldId = it.fieldId,
                    plantingDate = it.plantingDate,
                    expectedHarvest = it.expectedHarvest,
                    currentStatus = it.currentStatus
                )
            }
        }

    fun observeCropLifecycleEvents(): Flow<List<CropLifecycleEvent>> =
        dao.observeCropLifecycleEvents(DEFAULT_FARM_ID).map { list ->
            list.map {
                CropLifecycleEvent(
                    eventId = it.eventId,
                    cropId = it.cropId,
                    fieldId = it.fieldId,
                    stage = it.stage,
                    date = it.date,
                    notes = it.notes,
                    inputName = it.inputName,
                    quantity = it.quantity,
                    unit = it.unit,
                    createdAt = it.createdAt
                )
            }
        }

    fun observeProduction(): Flow<List<ProductionRecord>> =
        dao.observeProduction(DEFAULT_FARM_ID).map { list ->
            list.map {
                ProductionRecord(
                    product = it.commodity,
                    quantity = formatQuantity(it.quantity, it.unit),
                    period = it.date,
                    productionId = it.productionId,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    fieldId = it.fieldId,
                    areaHectares = it.areaHectares,
                    productionType = it.productionType
                )
            }
        }

    fun observeExpenses(): Flow<List<ExpenseRecord>> =
        dao.observeExpenses(DEFAULT_FARM_ID).map { list ->
            list.map {
                ExpenseRecord(
                    category = it.category,
                    amount = it.amount,
                    note = it.note,
                    date = it.date,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    fieldId = it.fieldId,
                    expenseId = it.expenseId,
                    paymentStatus = it.paymentStatus
                )
            }
        }

    fun observeSales(): Flow<List<SaleRecord>> =
        dao.observeSales(DEFAULT_FARM_ID).map { list ->
            list.map {
                SaleRecord(
                    product = it.product,
                    amount = it.amount,
                    date = it.date,
                    incomeCategory = it.incomeCategory,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    fieldId = it.fieldId,
                    saleId = it.saleId,
                    paymentStatus = it.paymentStatus
                )
            }
        }

    fun observeInventory(): Flow<List<InventoryItem>> =
        dao.observeInventory(DEFAULT_FARM_ID).map { list ->
            list.map {
                InventoryItem(
                    name = it.name,
                    quantity = formatQuantity(it.quantity, it.unit),
                    status = if (it.quantity <= 0.0) "Out of Stock" else it.status,
                    inventoryId = it.inventoryId,
                    category = it.category,
                    stock = it.quantity,
                    unit = it.unit,
                    purchasePrice = it.purchasePrice,
                    supplier = it.supplier,
                    dateAcquired = it.dateAcquired,
                    expiryDate = it.expiryDate
                )
            }
        }

    fun observeFeedLogs(): Flow<List<FeedLogRecord>> =
        dao.observeFeedLogs(DEFAULT_FARM_ID).map { list ->
            list.map {
                FeedLogRecord(
                    feedLogId = it.feedLogId,
                    livestockId = it.livestockId.orEmpty(),
                    date = it.date,
                    feedName = it.feedName,
                    quantityKg = it.quantityKg,
                    unit = it.unit,
                    notes = it.notes,
                    costAmount = it.costAmount,
                    createdAt = it.createdAt
                )
            }
        }

    /**
     * Persists feed usage and, when the feed can be matched safely, deducts inventory
     * in the same Room transaction. Re-saving a feedLogId does not deduct stock twice.
     * Name or unit mismatches and insufficient stock are logged for the user instead
     * of silently changing the inventory balance.
     */
    suspend fun saveFeedLog(record: FeedLogRecord) {
        val feedLogId = record.feedLogId.ifBlank { UUID.randomUUID().toString() }
        val feedName = record.feedName.trim()
        val quantity = record.quantityKg
        val costAmount = record.costAmount
        require(feedName.isNotBlank()) { "Feed name is required." }
        require(quantity.isFinite() && quantity > 0.0) { "Feed quantity must be greater than zero." }
        require(costAmount.isFinite() && costAmount >= 0.0) { "Feed cost must be zero or a positive amount." }

        database.withTransaction {
            val existingLog = dao.getFeedLog(DEFAULT_FARM_ID, feedLogId)
            var notes = record.notes.trim()

            if (existingLog == null) {
                val existingMovement = dao.getInventoryTransactionForSource(
                    DEFAULT_FARM_ID,
                    "FeedLog",
                    feedLogId
                )
                if (existingMovement != null) {
                    notes = appendInventoryNote(notes, "Inventory already reconciled for this feed record.")
                } else {
                    val item = dao.findInventoryByName(DEFAULT_FARM_ID, feedName)
                    val inventoryNote = when {
                        item == null ->
                            "Inventory not deducted: no inventory item named $feedName was found."

                        normalizeInventoryUnit(item.unit) != normalizeInventoryUnit(record.unit.ifBlank { "kg" }) ->
                            "Inventory not deducted: unit mismatch (inventory ${item.unit}, feed log ${record.unit.ifBlank { "kg" }})."

                        item.quantity < quantity ->
                            "Inventory not deducted: insufficient ${item.name} stock (${formatQuantity(item.quantity, item.unit)} available)."

                        else -> {
                            val updatedQuantity = (item.quantity - quantity).coerceAtLeast(0.0)
                            dao.upsertInventory(
                                item.copy(
                                    quantity = updatedQuantity,
                                    status = if (updatedQuantity <= 0.0) "Out of Stock"
                                        else if (item.status.equals("Out of Stock", ignoreCase = true)) "In Stock"
                                        else item.status,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            dao.upsertInventoryTransaction(
                                InventoryTransactionEntity(
                                    transactionId = UUID.randomUUID().toString(),
                                    farmId = DEFAULT_FARM_ID,
                                    inventoryId = item.inventoryId,
                                    type = "USAGE",
                                    quantity = quantity,
                                    unit = item.unit,
                                    date = record.date,
                                    sourceType = "FeedLog",
                                    sourceId = feedLogId,
                                    notes = "Automatic stock deduction from feed log"
                                )
                            )
                            "Inventory deducted: ${formatQuantity(quantity, item.unit)} from ${item.name}."
                        }
                    }
                    notes = appendInventoryNote(notes, inventoryNote)
                }
            }

            dao.upsertFeedLog(
                FeedLogEntity(
                    feedLogId = feedLogId,
                    farmId = DEFAULT_FARM_ID,
                    livestockId = record.livestockId.ifBlank { null },
                    date = record.date.trim(),
                    feedName = feedName,
                    quantityKg = quantity,
                    unit = record.unit.trim().ifBlank { "kg" },
                    notes = notes,
                    costAmount = costAmount,
                    createdAt = if (record.createdAt > 0L) record.createdAt else System.currentTimeMillis()
                )
            )
        }
    }

    fun observeInventoryTransactions(): Flow<List<InventoryTransaction>> =
        dao.observeInventoryTransactions(DEFAULT_FARM_ID).map { list ->
            list.map {
                InventoryTransaction(
                    transactionId = it.transactionId,
                    inventoryId = it.inventoryId,
                    type = it.type,
                    quantity = it.quantity,
                    unit = it.unit,
                    date = it.date,
                    sourceType = it.sourceType,
                    sourceId = it.sourceId,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }
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
                    reviewNotes = it.reviewNotes,
                    farmerId = it.farmerId,
                    farmId = it.farmId,
                    fieldId = it.fieldId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    reviewer = it.reviewer,
                    assistanceRequestId = it.assistanceRequestId,
                    resolution = it.resolution,
                    affectedAreaBoundary = decodeMapPoints(it.affectedAreaBoundaryJson)
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
                    timestamp = it.timestamp,
                    fromStatus = it.fromStatus,
                    actor = it.actor,
                    eventId = it.eventId
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

    suspend fun saveProfile(profile: FarmerProfile, farmBoundary: List<MapPoint>? = null) {
        val existingFarm = dao.observeFarm(DEFAULT_FARM_ID).first()
        val boundaryJson = farmBoundary?.let(::encodeMapPoints)
            ?: existingFarm?.boundaryPointsJson
            ?: "[]"
        dao.upsertFarmer(
            FarmerEntity(
                farmerId = profile.farmerId,
                fullName = profile.farmerName,
                contact = profile.contact
            )
        )
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
                reviewNotes = profile.reviewNotes,
                boundaryPointsJson = boundaryJson
            )
        )
    }

suspend fun saveField(record: FieldRecord) {
        require(record.fieldId.isNotBlank()) { "Field ID is required." }
        require(record.name.isNotBlank()) { "Field name is required." }
        require(record.areaHectares.isFinite() && record.areaHectares >= 0.0) { "Field area must be a finite non-negative number." }
        require((record.latitude == null && record.longitude == null) || (record.latitude != null && record.longitude != null)) {
            "Latitude and longitude must both be supplied."
        }
        require(record.latitude == null || (record.latitude.isFinite() && record.latitude in -90.0..90.0)) {
            "Latitude must be between -90 and 90."
        }
        require(record.longitude == null || (record.longitude.isFinite() && record.longitude in -180.0..180.0)) {
            "Longitude must be between -180 and 180."
        }
        require(record.boundaryPoints.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 }) {
            "Field boundary contains invalid coordinates."
        }
        dao.upsertField(
            FieldEntity(
                fieldId = record.fieldId,
                farmId = record.farmId.ifBlank { DEFAULT_FARM_ID },
                name = record.name,
                areaHectares = record.areaHectares,
                location = record.location,
                latitude = record.latitude,
                longitude = record.longitude,
                landTenure = record.landTenure,
                crop = record.crop,
                plantingDate = record.plantingDate,
                expectedHarvest = record.expectedHarvest,
                currentStatus = record.currentStatus,
                boundaryPointsJson = encodeMapPoints(record.boundaryPoints)
            )
        )
    }

    suspend fun saveFarmBoundary(points: List<MapPoint>) {
        require(points.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 }) {
            "Farm boundary contains invalid coordinates."
        }
        val farm = dao.observeFarm(DEFAULT_FARM_ID).first()
            ?: error("Farm registry is required before mapping the farm boundary.")
        dao.upsertFarm(
            farm.copy(
                boundaryPointsJson = encodeMapPoints(points),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteField(record: FieldRecord) {
        dao.deleteField(
            FieldEntity(
                fieldId = record.fieldId,
                farmId = record.farmId.ifBlank { DEFAULT_FARM_ID },
                name = record.name,
                areaHectares = record.areaHectares,
                location = record.location,
                latitude = record.latitude,
                longitude = record.longitude,
                landTenure = record.landTenure,
                crop = record.crop,
                plantingDate = record.plantingDate,
                expectedHarvest = record.expectedHarvest,
                currentStatus = record.currentStatus,
                boundaryPointsJson = encodeMapPoints(record.boundaryPoints)
            )
        )
    }

    suspend fun saveLivestock(record: Livestock, livestockId: String = record.groupId.ifBlank { UUID.randomUUID().toString() }) {
        dao.upsertLivestock(
            LivestockEntity(
                livestockId = livestockId,
                farmId = DEFAULT_FARM_ID,
                name = record.name,
                kind = record.kind,
                count = record.currentPopulation.coerceAtLeast(0),
                status = record.status,
                groupId = livestockId,
                initialPopulation = record.initialPopulation.coerceAtLeast(0),
                currentPopulation = record.currentPopulation.coerceAtLeast(0)
            )
        )
    }

    suspend fun saveLivestockLifecycleEvent(event: LivestockLifecycleEvent) {
        val livestock = dao.getLivestock(event.livestockId) ?: return
        database.withTransaction {
            val inventoryNote = if (event.stage.equals("Feed", true) || event.stage.equals("Health", true)) {
                consumeInventoryForActivity(
                    inputName = event.inputName,
                    quantity = event.quantity,
                    unit = event.unit,
                    sourceType = "Livestock",
                    sourceId = event.livestockId,
                    date = event.date
                )
            } else {
                null
            }

            dao.upsertLivestockLifecycleEvent(
                LivestockLifecycleEventEntity(
                    eventId = event.eventId.ifBlank { UUID.randomUUID().toString() },
                    farmId = DEFAULT_FARM_ID,
                    livestockId = event.livestockId,
                    stage = event.stage,
                    date = event.date,
                    notes = appendInventoryNote(event.notes, inventoryNote),
                    inputName = event.inputName,
                    quantity = event.quantity,
                    unit = event.unit,
                    amount = event.amount,
                    createdAt = event.createdAt
                )
            )

            val updatedPopulation = when {
                event.stage.equals("Mortality", true) ->
                    (livestock.currentPopulation - event.quantity.toInt().coerceAtLeast(0)).coerceAtLeast(0)
                event.stage.equals("Population", true) ->
                    event.quantity.toInt().coerceAtLeast(0)
                else -> livestock.currentPopulation
            }

            if (updatedPopulation != livestock.currentPopulation) {
                dao.upsertLivestock(
                    livestock.copy(
                        count = updatedPopulation,
                        currentPopulation = updatedPopulation,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun saveCrop(record: CropRecord, cropId: String = record.cropId.ifBlank { UUID.randomUUID().toString() }) {
        val normalizedStatus = CropLifecycleCalculator.normalize(record.currentStatus.ifBlank { record.stage })
        dao.upsertCrop(
            CropEntity(
                cropId = cropId,
                farmId = DEFAULT_FARM_ID,
                fieldId = record.fieldId,
                name = record.name,
                crop = record.crop,
                area = record.area,
                stage = normalizedStatus,
                plantingDate = record.plantingDate,
                expectedHarvest = record.expectedHarvest,
                currentStatus = normalizedStatus
            )
        )
    }

    suspend fun saveCropLifecycleEvent(event: CropLifecycleEvent) {
        require(CropLifecycleCalculator.isValidStage(event.stage)) { "Unknown crop lifecycle stage" }
        dao.upsertCropLifecycleEvent(
            CropLifecycleEventEntity(
                eventId = event.eventId.ifBlank { UUID.randomUUID().toString() },
                farmId = DEFAULT_FARM_ID,
                cropId = event.cropId,
                fieldId = event.fieldId,
                stage = CropLifecycleCalculator.normalize(event.stage),
                date = event.date,
                notes = event.notes,
                inputName = event.inputName,
                quantity = event.quantity,
                unit = event.unit,
                createdAt = event.createdAt
            )
        )
        val crop = dao.getCrop(event.cropId) ?: return
        val stage = CropLifecycleCalculator.normalize(event.stage)
        dao.upsertCrop(
            crop.copy(
                fieldId = event.fieldId.ifBlank { crop.fieldId },
                stage = stage,
                currentStatus = stage,
                plantingDate = if (stage == "Planting" && event.date.isNotBlank()) event.date else crop.plantingDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveInventory(
        record: InventoryItem,
        inventoryId: String = record.inventoryId.ifBlank { UUID.randomUUID().toString() }
    ) {
        val parsed = parseQuantity(record.quantity)
        val stock = if (record.inventoryId.isNotBlank()) record.stock else parsed.first
        val unit = record.unit.ifBlank { parsed.second }.ifBlank { "unit" }

        dao.upsertInventory(
            InventoryEntity(
                inventoryId = inventoryId,
                farmId = DEFAULT_FARM_ID,
                name = record.name.trim(),
                quantity = stock.coerceAtLeast(0.0),
                unit = unit,
                status = if (stock <= 0.0) "Out of Stock" else record.status,
                category = record.category,
                purchasePrice = record.purchasePrice.coerceAtLeast(0.0),
                supplier = record.supplier.trim(),
                dateAcquired = record.dateAcquired,
                expiryDate = record.expiryDate
            )
        )
    }

    suspend fun saveInventoryWithPurchase(
        record: InventoryItem,
        inventoryId: String = record.inventoryId.ifBlank { UUID.randomUUID().toString() }
    ) {
        database.withTransaction {
            val parsed = parseQuantity(record.quantity)
            val incomingStock = if (record.inventoryId.isNotBlank()) record.stock else parsed.first
            val incomingUnit = record.unit.ifBlank { parsed.second }.ifBlank { "unit" }
            val existing = dao.findInventoryByName(DEFAULT_FARM_ID, record.name.trim())
            val sameUnit = existing != null &&
                    normalizeInventoryUnit(existing.unit) == normalizeInventoryUnit(incomingUnit)

            val effectiveId = if (sameUnit) existing!!.inventoryId else inventoryId
            val newStock = if (sameUnit) existing!!.quantity + incomingStock else incomingStock

            dao.upsertInventory(
                InventoryEntity(
                    inventoryId = effectiveId,
                    farmId = DEFAULT_FARM_ID,
                    name = record.name.trim(),
                    quantity = newStock.coerceAtLeast(0.0),
                    unit = incomingUnit,
                    status = if (newStock <= 0.0) "Out of Stock" else "In Stock",
                    category = record.category,
                    purchasePrice = record.purchasePrice.coerceAtLeast(0.0),
                    supplier = record.supplier.trim(),
                    dateAcquired = record.dateAcquired,
                    expiryDate = record.expiryDate
                )
            )

            if (incomingStock > 0.0) {
                dao.upsertInventoryTransaction(
                    InventoryTransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        farmId = DEFAULT_FARM_ID,
                        inventoryId = effectiveId,
                        type = "PURCHASE",
                        quantity = incomingStock,
                        unit = incomingUnit,
                        date = record.dateAcquired,
                        sourceType = "Inventory",
                        sourceId = effectiveId,
                        notes = if (sameUnit) "Additional stock acquired" else "Stock acquired"
                    )
                )
            }
        }
    }

    /**
     * Records a manual movement against an existing inventory item.
     * PURCHASE is stock in, USAGE is stock used, LOSS is waste or damage.
     * The updated balance and transaction history are written atomically.
     */
    suspend fun recordInventoryMovement(
        inventoryId: String,
        type: String,
        quantity: Double,
        date: String,
        notes: String
    ): String? {
        val movementType = type.trim().uppercase()
        if (movementType !in setOf("PURCHASE", "USAGE", "LOSS")) {
            return "Choose stock in, usage, or loss."
        }
        if (!quantity.isFinite() || quantity <= 0.0) {
            return "Enter a quantity greater than zero."
        }

        return database.withTransaction {
            val item = dao.getInventory(DEFAULT_FARM_ID, inventoryId)
                ?: return@withTransaction "This inventory item could not be found. Refresh and try again."

            val addsStock = movementType == "PURCHASE"
            if (!addsStock && item.quantity < quantity) {
                return@withTransaction "Not enough stock. Available: " +
                    formatQuantity(item.quantity, item.unit) + "."
            }

            val updatedQuantity = if (addsStock) item.quantity + quantity else item.quantity - quantity
            val updatedStatus = when {
                updatedQuantity <= 0.0 -> "Out of Stock"
                item.status.equals("Out of Stock", ignoreCase = true) -> "In Stock"
                else -> item.status
            }
            dao.upsertInventory(
                item.copy(
                    quantity = updatedQuantity.coerceAtLeast(0.0),
                    status = updatedStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
            dao.upsertInventoryTransaction(
                InventoryTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    farmId = DEFAULT_FARM_ID,
                    inventoryId = item.inventoryId,
                    type = movementType,
                    quantity = quantity,
                    unit = item.unit,
                    date = date,
                    sourceType = "Manual",
                    sourceId = item.inventoryId,
                    notes = notes.trim().ifBlank {
                        when (movementType) {
                            "PURCHASE" -> "Manual stock received"
                            "USAGE" -> "Manual farm usage"
                            else -> "Manual stock loss or waste"
                        }
                    }
                )
            )
            null
        }
    }

    private fun normalizeInventoryUnit(unit: String): String =
        unit.trim().lowercase().let {
            when (it) {
                "kilogram", "kilograms", "kg", "kgs" -> "kg"
                "liter", "liters", "l" -> "l"
                "piece", "pieces", "pc" -> "pc"
                "bag", "bags" -> "bag"
                "sack", "sacks" -> "sack"
                else -> it
            }
        }

    private suspend fun consumeInventoryForActivity(
        inputName: String,
        quantity: Double,
        unit: String,
        sourceType: String,
        sourceId: String,
        date: String
    ): String? {
        if (inputName.isBlank() || quantity <= 0.0) return null

        val item = dao.findInventoryByName(DEFAULT_FARM_ID, inputName.trim())
            ?: return "Inventory not deducted: " + inputName.trim() + " was not found."

        if (normalizeInventoryUnit(item.unit) != normalizeInventoryUnit(unit)) {
            return "Inventory not deducted: unit mismatch (" + item.unit + " vs " + unit.ifBlank { "unknown" } + ")."
        }

        if (item.quantity < quantity) {
            return "Inventory not deducted: insufficient " + item.name + " stock (" +
                    formatQuantity(item.quantity, item.unit) + " remaining)."
        }

        dao.upsertInventory(
            item.copy(
                quantity = (item.quantity - quantity).coerceAtLeast(0.0),
                status = if (item.quantity - quantity <= 0.0) "Out of Stock" else item.status,
                updatedAt = System.currentTimeMillis()
            )
        )

        dao.upsertInventoryTransaction(
            InventoryTransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                farmId = DEFAULT_FARM_ID,
                inventoryId = item.inventoryId,
                type = "USAGE",
                quantity = quantity,
                unit = item.unit,
                date = date,
                sourceType = sourceType,
                sourceId = sourceId,
                notes = "Automatic deduction from farm activity"
            )
        )
        return null
    }

    private fun appendInventoryNote(original: String, note: String?): String =
        if (note.isNullOrBlank()) original
        else if (original.isBlank()) note
        else original + " • " + note

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

    suspend fun saveProduction(record: ProductionRecord, productionId: String = record.productionId.ifBlank { UUID.randomUUID().toString() }) {
        val parsed = parseQuantity(record.quantity)
        require(record.sourceType == "Crop" || record.sourceType == "Livestock") {
            "Production must be linked to a crop or livestock source."
        }
        require(record.sourceId.isNotBlank()) { "Production source is required." }
        require(parsed.first > 0.0) { "Production quantity must be greater than zero." }

        dao.upsertProduction(
            ProductionEntity(
                productionId = productionId,
                farmId = DEFAULT_FARM_ID,
                date = record.period,
                productionType = record.productionType.ifBlank { "Harvest" },
                commodity = record.product,
                quantity = parsed.first,
                unit = parsed.second.ifBlank { "unit" },
                source = record.sourceType + ":" + record.sourceId,
                notes = "",
                sourceType = record.sourceType,
                sourceId = record.sourceId,
                fieldId = record.fieldId,
                areaHectares = record.areaHectares.coerceAtLeast(0.0),
                            )
        )
    }

    private suspend fun validateFinancialSource(sourceType: String, sourceId: String, fieldId: String) {
        val type = sourceType.trim()
        if (type.isBlank()) {
            require(sourceId.isBlank() && fieldId.isBlank()) {
                "An unlinked financial record cannot keep a source ID or field ID."
            }
            return
        }
        require(sourceId.isNotBlank()) { "Choose a source record before saving." }
        when (type.lowercase(Locale.US)) {
            "crop" -> {
                val crop = dao.getCrop(sourceId)
                require(crop != null && crop.farmId == DEFAULT_FARM_ID) {
                    "The selected crop no longer exists in this farm."
                }
                require(fieldId.isBlank() || crop.fieldId.isBlank() || crop.fieldId == fieldId) {
                    "The selected field does not match the chosen crop."
                }
            }
            "livestock" -> {
                val group = dao.getLivestock(sourceId)
                require(group != null && group.farmId == DEFAULT_FARM_ID) {
                    "The selected livestock group no longer exists in this farm."
                }
                require(fieldId.isBlank()) { "Livestock financial records cannot be linked to a crop field." }
            }
            else -> throw IllegalArgumentException("Unsupported financial source type: " + type)
        }
    }

    suspend fun saveExpense(
        record: ExpenseRecord,
        expenseId: String = record.expenseId.ifBlank { UUID.randomUUID().toString() }
    ) {
        require(record.category.isNotBlank()) { "Expense category is required." }
        require(record.amount.isFinite() && record.amount > 0.0) { "Expense amount must be a finite positive number." }
        require(record.paymentStatus in setOf("Paid", "Unpaid")) { "Expense payment status must be Paid or Unpaid." }
        validateFinancialSource(record.sourceType, record.sourceId, record.fieldId)
        val date = record.date.ifBlank { "Today" }

        dao.upsertExpense(
            ExpenseEntity(
                expenseId = expenseId,
                farmId = DEFAULT_FARM_ID,
                date = date,
                category = record.category,
                amount = record.amount,
                note = record.note,
                sourceType = record.sourceType,
                sourceId = record.sourceId,
                fieldId = record.fieldId,
                paymentStatus = record.paymentStatus
            )
        )
    }

    suspend fun saveSale(
        record: SaleRecord,
        saleId: String = record.saleId.ifBlank { UUID.randomUUID().toString() }
    ) {
        require(record.product.isNotBlank()) { "Income source is required." }
        require(record.incomeCategory.isNotBlank()) { "Income category is required." }
        require(record.amount.isFinite() && record.amount > 0.0) { "Income amount must be a finite positive number." }
        require(record.paymentStatus in setOf("Paid", "Unpaid")) { "Income payment status must be Paid or Unpaid." }
        validateFinancialSource(record.sourceType, record.sourceId, record.fieldId)

        dao.upsertSale(
            SaleEntity(
                saleId = saleId,
                farmId = DEFAULT_FARM_ID,
                date = record.date.ifBlank { "Today" },
                product = record.product,
                amount = record.amount,
                incomeCategory = record.incomeCategory,
                sourceType = record.sourceType,
                sourceId = record.sourceId,
                fieldId = record.fieldId,
                paymentStatus = record.paymentStatus
            )
        )
    }

    suspend fun updateExpensePaymentStatus(record: ExpenseRecord, nextStatus: String) {
        require(nextStatus in setOf("Paid", "Unpaid")) { "Payment status must be Paid or Unpaid." }
        require(record.expenseId.isNotBlank()) { "Expense ID missing. Reopen Finance and try again." }
        val entity = dao.observeExpenses(DEFAULT_FARM_ID).first().firstOrNull { it.expenseId == record.expenseId }
            ?: error("Expense record not found.")
        dao.upsertExpense(entity.copy(paymentStatus = nextStatus))
    }

    suspend fun updateSalePaymentStatus(record: SaleRecord, nextStatus: String) {
        require(nextStatus in setOf("Paid", "Unpaid")) { "Payment status must be Paid or Unpaid." }
        require(record.saleId.isNotBlank()) { "Sale ID missing. Reopen Finance and try again." }
        val entity = dao.observeSales(DEFAULT_FARM_ID).first().firstOrNull { it.saleId == record.saleId }
            ?: error("Sales record not found.")
        dao.upsertSale(entity.copy(paymentStatus = nextStatus))
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
        require((record.latitude == null && record.longitude == null) || (record.latitude != null && record.longitude != null)) {
            "Incident latitude and longitude must both be supplied."
        }
        require(record.latitude == null || (record.latitude.isFinite() && record.latitude in -90.0..90.0)) {
            "Incident latitude must be between -90 and 90."
        }
        require(record.longitude == null || (record.longitude.isFinite() && record.longitude in -180.0..180.0)) {
            "Incident longitude must be between -180 and 180."
        }
        require(record.affectedAreaBoundary.all { it.latitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude.isFinite() && it.longitude in -180.0..180.0 }) {
            "Incident boundary contains invalid coordinates."
        }
        val now = System.currentTimeMillis()
        dao.upsertFieldIncident(
            FieldIncidentEntity(
                incidentId = record.id,
                farmId = record.farmId.ifBlank { DEFAULT_FARM_ID },
                type = record.type,
                commodity = record.commodity,
                affectedArea = record.affectedArea,
                date = record.date,
                severity = record.severity,
                description = record.description,
                status = record.status,
                evidenceUri = record.evidenceUri,
                reviewNotes = record.reviewNotes,
                farmerId = record.farmerId,
                fieldId = record.fieldId,
                latitude = record.latitude,
                longitude = record.longitude,
                reviewer = record.reviewer,
                assistanceRequestId = record.assistanceRequestId,
                resolution = record.resolution,
                affectedAreaBoundaryJson = encodeMapPoints(record.affectedAreaBoundary),
                updatedAt = now
            )
        )
    }

    suspend fun createIncident(record: FieldIncident) {
        require(record.id.isNotBlank()) { "Incident ID is required." }
        require(record.farmerId.isNotBlank()) { "Farmer is required." }
        require(record.farmId.isNotBlank()) { "Farm is required." }
        require(record.fieldId.isNotBlank()) { "Field is required." }
        require(record.commodity.isNotBlank()) { "Commodity is required." }
        require(record.description.isNotBlank()) { "Incident description is required." }
        database.withTransaction {
            saveFieldIncident(record.copy(status = "Draft"))
            saveIncidentEvent(
                IncidentEvent(
                    incidentId = record.id,
                    status = "Draft",
                    note = "Incident draft created",
                    fromStatus = "",
                    actor = record.farmerId
                )
            )
        }
    }

    suspend fun transitionIncident(
        incidentId: String,
        nextStatus: String,
        actor: String = "",
        note: String = "",
        resolution: String? = null
    ) {
        val current = dao.getFieldIncident(incidentId)
            ?: error("Incident not found.")
        val allowed = when (current.status) {
            "Draft" -> setOf("Submitted")
            "Submitted" -> setOf("Under Review")
            "Under Review" -> setOf("Verified", "Returned")
            "Returned" -> setOf("Submitted")
            "Verified" -> setOf("Assistance")
            "Assistance" -> setOf("Completed")
            "Completed" -> emptySet()
            else -> emptySet()
        }
        require(nextStatus in allowed) {
            "Invalid incident transition: " + current.status + " → " + nextStatus
        }
        if (nextStatus == "Under Review" || nextStatus == "Verified" || nextStatus == "Returned") {
            require(actor.isNotBlank()) { "Reviewer is required for this workflow step." }
        }
        if (nextStatus == "Completed") {
            require(!resolution.isNullOrBlank()) { "Resolution is required before completion." }
            require(actor.isNotBlank()) { "Reviewer is required for completion." }
        }

        database.withTransaction {
            dao.upsertFieldIncident(
                current.copy(
                    status = nextStatus,
                    reviewer = if (actor.isNotBlank()) actor else current.reviewer,
                    resolution = resolution?.trim()?.ifBlank { current.resolution } ?: current.resolution,
                    updatedAt = System.currentTimeMillis()
                )
            )
            saveIncidentEvent(
                IncidentEvent(
                    incidentId = incidentId,
                    fromStatus = current.status,
                    status = nextStatus,
                    note = note.ifBlank { "Status changed to " + nextStatus },
                    actor = actor
                )
            )
        }
    }

    suspend fun saveIncidentEvent(event: IncidentEvent) {
        dao.upsertIncidentEvent(
            IncidentEventEntity(
                eventId = event.eventId.ifBlank { UUID.randomUUID().toString() },
                farmId = DEFAULT_FARM_ID,
                incidentId = event.incidentId,
                status = event.status,
                note = event.note,
                timestamp = event.timestamp,
                fromStatus = event.fromStatus,
                actor = event.actor
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
        val current = dao.getFieldIncident(incident.id)
            ?: error("Incident not found.")
        require(current.status == "Verified") {
            "Assistance can only be requested after verification."
        }
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
        database.withTransaction {
            saveAssistance(request)
            dao.upsertFieldIncident(
                current.copy(
                    status = "Assistance",
                    assistanceRequestId = request.requestId,
                    updatedAt = System.currentTimeMillis()
                )
            )
            saveIncidentEvent(
                IncidentEvent(
                    incidentId = incident.id,
                    fromStatus = "Verified",
                    status = "Assistance",
                    note = "Assistance request linked: " + request.requestId,
                    actor = request.requestId
                )
            )
        }
        return request
    }

    suspend fun completeIncident(incidentId: String, resolution: String, reviewer: String) {
        val current = dao.getFieldIncident(incidentId)
            ?: error("Incident not found.")
        require(current.status == "Assistance") {
            "Incident must be in Assistance before completion."
        }
        val assistance = dao.getAssistanceForIncident(DEFAULT_FARM_ID, incidentId)
            .firstOrNull { it.status.equals("Completed", ignoreCase = true) }
        require(assistance != null) {
            "Linked assistance must be completed before closing the incident."
        }
        transitionIncident(
            incidentId = incidentId,
            nextStatus = "Completed",
            actor = reviewer,
            note = "Resolution recorded",
            resolution = resolution
        )
    }

    suspend fun replaceAll(snapshot: FarmSnapshot) {
        database.withTransaction {
            dao.clearFields()
            dao.clearFarmers()
            dao.clearCropLifecycleEvents()
            dao.clearLivestockLifecycleEvents()
            dao.clearLivestock()
            dao.clearCrops()
            dao.clearFeedLogs()
            dao.clearProduction()
            dao.clearExpenses()
            dao.clearSales()
            dao.clearInventoryTransactions()
            dao.clearInventory()
            dao.clearEquipment()
            dao.clearTasks()
            dao.clearAssistance()
            dao.clearFieldIncidents()
            dao.clearIncidentEvents()
            dao.clearReportSubmissions()
            dao.clearFarms()

            saveProfile(snapshot.profile, snapshot.farm?.boundaryPoints)
            snapshot.fields.forEach { saveField(it) }
            snapshot.livestock.forEach { saveLivestock(it) }
            snapshot.livestockLifecycleEvents.forEach { saveLivestockLifecycleEvent(it) }
            snapshot.crops.forEach { saveCrop(it) }
            snapshot.feedLogs.forEach { saveFeedLog(it) }
            snapshot.cropLifecycleEvents.forEach { saveCropLifecycleEvent(it) }
            snapshot.production.forEach { saveProduction(it) }
            snapshot.expenses.forEach { saveExpense(it) }
            snapshot.sales.forEach { saveSale(it) }
            snapshot.inventory.forEach { saveInventory(it) }
            snapshot.inventoryTransactions.forEach {
                dao.upsertInventoryTransaction(
                    InventoryTransactionEntity(
                        transactionId = it.transactionId.ifBlank { UUID.randomUUID().toString() },
                        farmId = DEFAULT_FARM_ID,
                        inventoryId = it.inventoryId,
                        type = it.type,
                        quantity = it.quantity,
                        unit = it.unit,
                        date = it.date,
                        sourceType = it.sourceType,
                        sourceId = it.sourceId,
                        notes = it.notes,
                        createdAt = it.createdAt
                    )
                )
            }
            snapshot.equipment.forEach { saveEquipment(it) }
            snapshot.tasks.forEach { saveTask(it) }
            snapshot.assistance.forEach { saveAssistance(it) }
            snapshot.fieldIncidents.forEach { saveFieldIncident(it) }
            snapshot.incidentEvents.forEach { saveIncidentEvent(it) }
            saveReportSubmission(snapshot.reportSubmission)
        }
    }

    private fun encodeMapPoints(points: List<MapPoint>): String =
        org.json.JSONArray().apply {
            points.forEach { point ->
                put(
                    org.json.JSONObject().apply {
                        put("latitude", point.latitude)
                        put("longitude", point.longitude)
                    }
                )
            }
        }.toString()

    private fun decodeMapPoints(raw: String): List<MapPoint> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val point = array.optJSONObject(i) ?: continue
                    val lat = point.optDouble("latitude", Double.NaN)
                    val lon = point.optDouble("longitude", Double.NaN)
                    if (lat.isFinite() && lon.isFinite()) add(MapPoint(lat, lon))
                }
            }
        }.getOrDefault(emptyList())
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
