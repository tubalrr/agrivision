package com.tubalrr.agrivision

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Small feature cache kept separate from the existing farm database so these
 * add-on screens can be introduced without changing the schema of farm records.
 * The active farm tables remain the source of truth for operational records.
 */
@Entity(
    tableName = "scan_history",
    indices = [Index(value = ["farmId", "createdAt"])]
)
data class ScanHistoryEntity(
    @PrimaryKey val scanId: String,
    val farmId: String,
    val farmName: String,
    val cropId: String,
    val cropName: String,
    val imagePath: String,
    val diagnosis: String,
    val confidencePercent: Int,
    val severity: String,
    val treatment: String,
    val detectorVersion: String = "mock-v1",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val farmId: String,
    val latitude: Double,
    val longitude: Double,
    val payloadJson: String,
    val fetchedAt: Long = System.currentTimeMillis(),
    val source: String = "Open-Meteo"
)

@Entity(
    tableName = "market_prices",
    indices = [Index(value = ["category"]), Index(value = ["commodity"])]
)
data class MarketPriceEntity(
    @PrimaryKey val priceId: String,
    val commodity: String,
    val category: String,
    val pricePerKg: Double,
    /** UP, DOWN, or STABLE relative to a mock/reference period. */
    val trend: String,
    val changePercent: Double,
    val unit: String = "kg",
    val source: String = "MOCK DATA",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface AgriExtrasDao {
    @Query("SELECT * FROM scan_history WHERE farmId = :farmId ORDER BY createdAt DESC")
    fun observeScanHistory(farmId: String): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveScan(record: ScanHistoryEntity)

    @Query("SELECT * FROM weather_cache WHERE farmId = :farmId LIMIT 1")
    suspend fun getWeatherCache(farmId: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeatherCache(record: WeatherCacheEntity)

    @Query("SELECT * FROM market_prices ORDER BY category ASC, commodity COLLATE NOCASE ASC")
    fun observeMarketPrices(): Flow<List<MarketPriceEntity>>

    @Query("SELECT COUNT(*) FROM market_prices")
    suspend fun countMarketPrices(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMarketPrices(records: List<MarketPriceEntity>)

    @Query("DELETE FROM market_prices")
    suspend fun clearMarketPrices()
}

@Database(
    entities = [ScanHistoryEntity::class, WeatherCacheEntity::class, MarketPriceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgriExtrasDatabase : RoomDatabase() {
    abstract fun extrasDao(): AgriExtrasDao

    companion object {
        @Volatile private var instance: AgriExtrasDatabase? = null

        fun getInstance(context: Context): AgriExtrasDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgriExtrasDatabase::class.java,
                    "agrivision-extras.db"
                ).build().also { instance = it }
            }
    }
}
