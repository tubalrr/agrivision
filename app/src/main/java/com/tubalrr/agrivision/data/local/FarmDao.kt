package com.tubalrr.agrivision.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {

    @Query("SELECT * FROM farms ORDER BY updatedAt DESC")
    fun observeFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE farmId = :farmId LIMIT 1")
    suspend fun getFarm(farmId: String): FarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFarm(farm: FarmEntity)

    @Delete
    suspend fun deleteFarm(farm: FarmEntity)

    @Query("SELECT * FROM livestock WHERE farmId = :farmId ORDER BY updatedAt DESC")
    fun observeLivestock(farmId: String): Flow<List<LivestockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLivestock(record: LivestockEntity)

    @Delete
    suspend fun deleteLivestock(record: LivestockEntity)

    @Query("SELECT * FROM crops WHERE farmId = :farmId ORDER BY updatedAt DESC")
    fun observeCrops(farmId: String): Flow<List<CropEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrop(record: CropEntity)

    @Delete
    suspend fun deleteCrop(record: CropEntity)

    @Query("SELECT * FROM feed_logs WHERE farmId = :farmId ORDER BY date DESC, createdAt DESC")
    fun observeFeedLogs(farmId: String): Flow<List<FeedLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedLog(record: FeedLogEntity)

    @Delete
    suspend fun deleteFeedLog(record: FeedLogEntity)

    @Query("SELECT * FROM production_records WHERE farmId = :farmId ORDER BY date DESC, createdAt DESC")
    fun observeProduction(farmId: String): Flow<List<ProductionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduction(record: ProductionEntity)

    @Delete
    suspend fun deleteProduction(record: ProductionEntity)

    @Query("SELECT * FROM expenses WHERE farmId = :farmId ORDER BY date DESC, createdAt DESC")
    fun observeExpenses(farmId: String): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExpense(record: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(record: ExpenseEntity)

    @Query("SELECT * FROM sales WHERE farmId = :farmId ORDER BY date DESC, createdAt DESC")
    fun observeSales(farmId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(record: SaleEntity)

    @Delete
    suspend fun deleteSale(record: SaleEntity)

    @Query("SELECT * FROM inventory WHERE farmId = :farmId ORDER BY updatedAt DESC")
    fun observeInventory(farmId: String): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventory(record: InventoryEntity)

    @Delete
    suspend fun deleteInventory(record: InventoryEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sales WHERE farmId = :farmId")
    fun observeTotalSales(farmId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE farmId = :farmId")
    fun observeTotalExpenses(farmId: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM production_records WHERE farmId = :farmId")
    fun observeProductionCount(farmId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM livestock WHERE farmId = :farmId")
    fun observeTotalLivestock(farmId: String): Flow<Int>
}
