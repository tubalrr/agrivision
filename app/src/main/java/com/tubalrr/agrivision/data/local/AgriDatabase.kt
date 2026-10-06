package com.tubalrr.agrivision.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppMetaEntity::class,
        FarmerEntity::class,
        FarmEntity::class,
        FieldEntity::class,
        CropLifecycleEventEntity::class,
        LivestockEntity::class,
        CropEntity::class,
        FeedLogEntity::class,
        ProductionEntity::class,
        ExpenseEntity::class,
        SaleEntity::class,
        InventoryEntity::class,
        EquipmentEntity::class,
        FarmTaskEntity::class,
        AssistanceEntity::class,
        FieldIncidentEntity::class,
        IncidentEventEntity::class,
        ReportSubmissionEntity::class
    ],
    version = 4,
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS app_meta (metaKey TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(metaKey))")
                db.execSQL("ALTER TABLE farms ADD COLUMN contact TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE farms ADD COLUMN registryStatus TEXT NOT NULL DEFAULT 'For Review'")
                db.execSQL("ALTER TABLE farms ADD COLUMN reviewNotes TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS equipment (
                        equipmentId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        status TEXT NOT NULL,
                        note TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(equipmentId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_equipment_farmId ON equipment(farmId)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS farm_tasks (
                        taskId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        date TEXT NOT NULL,
                        done INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(taskId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_farm_tasks_farmId_date ON farm_tasks(farmId,date)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS assistance (
                        requestId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        incidentId TEXT NOT NULL,
                        program TEXT NOT NULL,
                        assistanceType TEXT NOT NULL,
                        dateReceived TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        status TEXT NOT NULL,
                        source TEXT NOT NULL,
                        approvedDate TEXT NOT NULL,
                        distributedDate TEXT NOT NULL,
                        completedDate TEXT NOT NULL,
                        distributionDetails TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        reviewNotes TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(requestId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assistance_farmId ON assistance(farmId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assistance_incidentId ON assistance(incidentId)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS field_incidents (
                        incidentId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        commodity TEXT NOT NULL,
                        affectedArea TEXT NOT NULL,
                        date TEXT NOT NULL,
                        severity TEXT NOT NULL,
                        description TEXT NOT NULL,
                        status TEXT NOT NULL,
                        evidenceUri TEXT NOT NULL,
                        reviewNotes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(incidentId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_field_incidents_farmId_status_date ON field_incidents(farmId,status,date)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS incident_events (
                        eventId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        incidentId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        PRIMARY KEY(eventId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incident_events_farmId_incidentId ON incident_events(farmId,incidentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_incident_events_timestamp ON incident_events(timestamp)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS report_submissions (
                        submissionId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        submittedDate TEXT NOT NULL,
                        referenceNo TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(submissionId)
                    )"""
                )
            }
        }
        
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS farmers (
                        farmerId TEXT NOT NULL,
                        fullName TEXT NOT NULL,
                        contact TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(farmerId)
                    )"""
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO farmers(farmerId, fullName, contact, updatedAt) " +
                            "SELECT farmerId, farmerName, contact, updatedAt FROM farms"
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS fields (
                        fieldId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        areaHectares REAL NOT NULL,
                        location TEXT NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        landTenure TEXT NOT NULL,
                        crop TEXT NOT NULL,
                        plantingDate TEXT NOT NULL,
                        expectedHarvest TEXT NOT NULL,
                        currentStatus TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(fieldId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fields_farmId ON fields(farmId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fields_farmId_currentStatus ON fields(farmId,currentStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fields_farmId_crop ON fields(farmId,crop)")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE crops ADD COLUMN fieldId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE crops ADD COLUMN plantingDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE crops ADD COLUMN expectedHarvest TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE crops ADD COLUMN currentStatus TEXT NOT NULL DEFAULT 'Land Preparation'")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crops_fieldId ON crops(fieldId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crops_farmId_currentStatus ON crops(farmId,currentStatus)")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS crop_lifecycle_events (
                        eventId TEXT NOT NULL,
                        farmId TEXT NOT NULL,
                        cropId TEXT NOT NULL,
                        fieldId TEXT NOT NULL,
                        stage TEXT NOT NULL,
                        date TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        inputName TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(eventId)
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crop_lifecycle_events_cropId ON crop_lifecycle_events(cropId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crop_lifecycle_events_fieldId ON crop_lifecycle_events(fieldId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crop_lifecycle_events_cropId_date ON crop_lifecycle_events(cropId,date)")
            }
        }

    }
}
