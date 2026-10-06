package com.tubalrr.agrivision.data.local

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class FarmRepository(
    private val dao: FarmDao
) {
    fun observeFarms(): Flow<List<FarmEntity>> = dao.observeFarms()

    fun observeLivestock(farmId: String): Flow<List<LivestockEntity>> =
        dao.observeLivestock(farmId)

    fun observeCrops(farmId: String): Flow<List<CropEntity>> =
        dao.observeCrops(farmId)

    fun observeFeedLogs(farmId: String): Flow<List<FeedLogEntity>> =
        dao.observeFeedLogs(farmId)

    fun observeProduction(farmId: String): Flow<List<ProductionEntity>> =
        dao.observeProduction(farmId)

    fun observeExpenses(farmId: String): Flow<List<ExpenseEntity>> =
        dao.observeExpenses(farmId)

    fun observeSales(farmId: String): Flow<List<SaleEntity>> =
        dao.observeSales(farmId)

    fun observeInventory(farmId: String): Flow<List<InventoryEntity>> =
        dao.observeInventory(farmId)

    fun observeTotalSales(farmId: String): Flow<Double> =
        dao.observeTotalSales(farmId)

    fun observeTotalExpenses(farmId: String): Flow<Double> =
        dao.observeTotalExpenses(farmId)

    fun observeProductionCount(farmId: String): Flow<Int> =
        dao.observeProductionCount(farmId)

    fun observeTotalLivestock(farmId: String): Flow<Int> =
        dao.observeTotalLivestock(farmId)

    suspend fun saveFarm(
        farmerId: String,
        farmerName: String,
        farmName: String,
        province: String,
        municipality: String,
        barangay: String,
        farmSize: String,
        landTenure: String,
        commodities: String,
        farmId: String = UUID.randomUUID().toString()
    ): String {
        dao.upsertFarm(
            FarmEntity(
                farmId = farmId,
                farmerId = farmerId,
                farmerName = farmerName,
                farmName = farmName,
                province = province,
                municipality = municipality,
                barangay = barangay,
                farmSize = farmSize,
                landTenure = landTenure,
                commodities = commodities
            )
        )
        return farmId
    }

    suspend fun saveLivestock(
        farmId: String,
        name: String,
        kind: String,
        count: Int,
        status: String,
        livestockId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertLivestock(
            LivestockEntity(
                livestockId = livestockId,
                farmId = farmId,
                name = name,
                kind = kind,
                count = count,
                status = status
            )
        )
    }

    suspend fun saveCrop(
        farmId: String,
        name: String,
        crop: String,
        area: String,
        stage: String,
        cropId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertCrop(
            CropEntity(
                cropId = cropId,
                farmId = farmId,
                name = name,
                crop = crop,
                area = area,
                stage = stage
            )
        )
    }

    suspend fun saveFeedLog(
        farmId: String,
        date: String,
        feedName: String,
        quantityKg: Double,
        unit: String,
        livestockId: String? = null,
        notes: String = "",
        feedLogId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertFeedLog(
            FeedLogEntity(
                feedLogId = feedLogId,
                farmId = farmId,
                livestockId = livestockId,
                date = date,
                feedName = feedName,
                quantityKg = quantityKg,
                unit = unit,
                notes = notes
            )
        )
    }

    suspend fun saveProduction(
        farmId: String,
        date: String,
        productionType: String,
        commodity: String,
        quantity: Double,
        unit: String,
        source: String = "",
        notes: String = "",
        productionId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertProduction(
            ProductionEntity(
                productionId = productionId,
                farmId = farmId,
                date = date,
                productionType = productionType,
                commodity = commodity,
                quantity = quantity,
                unit = unit,
                source = source,
                notes = notes
            )
        )
    }

    suspend fun saveExpense(
        farmId: String,
        date: String,
        category: String,
        amount: Double,
        note: String = "",
        expenseId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertExpense(
            ExpenseEntity(
                expenseId = expenseId,
                farmId = farmId,
                date = date,
                category = category,
                amount = amount,
                note = note
            )
        )
    }

    suspend fun saveSale(
        farmId: String,
        date: String,
        product: String,
        amount: Double,
        notes: String = "",
        saleId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertSale(
            SaleEntity(
                saleId = saleId,
                farmId = farmId,
                date = date,
                product = product,
                amount = amount,
                notes = notes
            )
        )
    }

    suspend fun saveInventory(
        farmId: String,
        name: String,
        quantity: Double,
        unit: String,
        status: String,
        inventoryId: String = UUID.randomUUID().toString()
    ) {
        dao.upsertInventory(
            InventoryEntity(
                inventoryId = inventoryId,
                farmId = farmId,
                name = name,
                quantity = quantity,
                unit = unit,
                status = status
            )
        )
    }
}
