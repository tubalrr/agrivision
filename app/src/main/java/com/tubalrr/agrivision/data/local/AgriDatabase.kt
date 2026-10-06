package com.tubalrr.agrivision.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FarmEntity::class,
        LivestockEntity::class,
        CropEntity::class,
        FeedLogEntity::class,
        ProductionEntity::class,
        ExpenseEntity::class,
        SaleEntity::class,
        InventoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AgriDatabase : RoomDatabase() {

    abstract fun farmDao(): FarmDao

    companion object {
        @Volatile
        private var INSTANCE: AgriDatabase? = null

        fun getInstance(context: Context): AgriDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgriDatabase::class.java,
                    "agrivision.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
