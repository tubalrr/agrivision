package com.tubalrr.agrivision.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {

    @Query("SELECT * FROM farms ORDER BY updatedAt DESC")
    fun observeFarms(): Flow<List<FarmEntity>>

    @Query("SELECT * FROM farms WHERE farmId = :farmId LIMIT 1")
    fun observeFarm(farmId: String): Flow<FarmEntity?>

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

    @Query("SELECT * FROM equipment WHERE farmId = :farmId ORDER BY updatedAt DESC")
    fun observeEquipment(farmId: String): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEquipment(record: EquipmentEntity)

    @Delete
    suspend fun deleteEquipment(record: EquipmentEntity)

    @Query("SELECT * FROM farm_tasks WHERE farmId = :farmId ORDER BY date ASC, updatedAt DESC")
    fun observeTasks(farmId: String): Flow<List<FarmTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(record: FarmTaskEntity)

    @Delete
    suspend fun deleteTask(record: FarmTaskEntity)

    @Query("SELECT * FROM assistance WHERE farmId = :farmId ORDER BY updatedAt DESC")
    fun observeAssistance(farmId: String): Flow<List<AssistanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssistance(record: AssistanceEntity)

    @Delete
    suspend fun deleteAssistance(record: AssistanceEntity)

    @Query("SELECT * FROM field_incidents WHERE farmId = :farmId ORDER BY createdAt DESC")
    fun observeFieldIncidents(farmId: String): Flow<List<FieldIncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFieldIncident(record: FieldIncidentEntity)

    @Delete
    suspend fun deleteFieldIncident(record: FieldIncidentEntity)

    @Query("SELECT * FROM incident_events WHERE farmId = :farmId ORDER BY timestamp DESC")
    fun observeIncidentEvents(farmId: String): Flow<List<IncidentEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncidentEvent(record: IncidentEventEntity)

    @Delete
    suspend fun deleteIncidentEvent(record: IncidentEventEntity)

    @Query("SELECT * FROM report_submissions WHERE submissionId = 'default' AND farmId = :farmId LIMIT 1")
    fun observeReportSubmission(farmId: String): Flow<ReportSubmissionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReportSubmission(record: ReportSubmissionEntity)

    @Query("SELECT value FROM app_meta WHERE metaKey = :metaKey LIMIT 1")
    suspend fun getMeta(metaKey: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: AppMetaEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sales WHERE farmId = :farmId")
    fun observeTotalSales(farmId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE farmId = :farmId")
    fun observeTotalExpenses(farmId: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM production_records WHERE farmId = :farmId")
    fun observeProductionCount(farmId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(count), 0) FROM livestock WHERE farmId = :farmId")
    fun observeTotalLivestock(farmId: String): Flow<Int>

    @Query("DELETE FROM livestock")
    suspend fun clearLivestock()

    @Query("DELETE FROM crops")
    suspend fun clearCrops()

    @Query("DELETE FROM feed_logs")
    suspend fun clearFeedLogs()

    @Query("DELETE FROM production_records")
    suspend fun clearProduction()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM inventory")
    suspend fun clearInventory()

    @Query("DELETE FROM equipment")
    suspend fun clearEquipment()

    @Query("DELETE FROM farm_tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM assistance")
    suspend fun clearAssistance()

    @Query("DELETE FROM field_incidents")
    suspend fun clearFieldIncidents()

    @Query("DELETE FROM incident_events")
    suspend fun clearIncidentEvents()

    @Query("DELETE FROM farms")
    suspend fun clearFarms()

    @Query("DELETE FROM report_submissions")
    suspend fun clearReportSubmissions()
}
