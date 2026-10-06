package com.tubalrr.agrivision.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "app_meta",
    primaryKeys = ["metaKey"]
)
data class AppMetaEntity(
    val metaKey: String,
    val value: String
)

@Entity(
    tableName = "farmers",
    primaryKeys = ["farmerId"]
)
data class FarmerEntity(
    val farmerId: String,
    val fullName: String,
    val contact: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "farms",
    primaryKeys = ["farmId"],
    indices = [Index(value = ["farmerId"])]
)
data class FarmEntity(
    val farmId: String,
    val farmerId: String,
    val farmerName: String,
    val farmName: String,
    val province: String,
    val municipality: String,
    val barangay: String,
    val farmSize: String,
    val landTenure: String,
    val commodities: String,
    val contact: String = "",
    val registryStatus: String = "For Review",
    val reviewNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "livestock",
    primaryKeys = ["livestockId"],
    indices = [Index(value = ["farmId"])]
)
data class LivestockEntity(
    val livestockId: String,
    val farmId: String,
    val name: String,
    val kind: String,
    val count: Int,
    val status: String,
    val groupId: String = "",
    val initialPopulation: Int = count,
    val currentPopulation: Int = count,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "livestock_lifecycle_events",
    primaryKeys = ["eventId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["livestockId"]),
        Index(value = ["livestockId", "date"])
    ]
)
data class LivestockLifecycleEventEntity(
    val eventId: String,
    val farmId: String,
    val livestockId: String,
    val stage: String,
    val date: String,
    val notes: String,
    val inputName: String,
    val quantity: Double,
    val unit: String,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "crops",
    primaryKeys = ["cropId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["fieldId"]),
        Index(value = ["farmId", "currentStatus"])
    ]
)
data class CropEntity(
    val cropId: String,
    val farmId: String,
    val fieldId: String = "",
    val name: String,
    val crop: String,
    val area: String,
    val stage: String,
    val plantingDate: String = "",
    val expectedHarvest: String = "",
    val currentStatus: String = "Land Preparation",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "crop_lifecycle_events",
    primaryKeys = ["eventId"],
    indices = [
        Index(value = ["cropId"]),
        Index(value = ["fieldId"]),
        Index(value = ["cropId", "date"])
    ]
)
data class CropLifecycleEventEntity(
    val eventId: String,
    val farmId: String,
    val cropId: String,
    val fieldId: String,
    val stage: String,
    val date: String,
    val notes: String,
    val inputName: String,
    val quantity: String,
    val unit: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fields",
    primaryKeys = ["fieldId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["farmId", "currentStatus"]),
        Index(value = ["farmId", "crop"])
    ]
)
data class FieldEntity(
    val fieldId: String,
    val farmId: String,
    val name: String,
    val areaHectares: Double,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    val landTenure: String,
    val crop: String,
    val plantingDate: String,
    val expectedHarvest: String,
    val currentStatus: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "feed_logs",
    primaryKeys = ["feedLogId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["livestockId"])
    ]
)
data class FeedLogEntity(
    val feedLogId: String,
    val farmId: String,
    val livestockId: String?,
    val date: String,
    val feedName: String,
    val quantityKg: Double,
    val unit: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "production_records",
    primaryKeys = ["productionId"],
    indices = [Index(value = ["farmId", "date"])]
)
data class ProductionEntity(
    val productionId: String,
    val farmId: String,
    val date: String,
    val productionType: String,
    val commodity: String,
    val quantity: Double,
    val unit: String,
    val source: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    primaryKeys = ["expenseId"],
    indices = [Index(value = ["farmId", "date"])]
)
data class ExpenseEntity(
    val expenseId: String,
    val farmId: String,
    val date: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    primaryKeys = ["saleId"],
    indices = [Index(value = ["farmId", "date"])]
)
data class SaleEntity(
    val saleId: String,
    val farmId: String,
    val date: String,
    val product: String,
    val amount: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory",
    primaryKeys = ["inventoryId"],
    indices = [Index(value = ["farmId"])]
)
data class InventoryEntity(
    val inventoryId: String,
    val farmId: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val status: String,
    val category: String = "Other",
    val purchasePrice: Double = 0.0,
    val supplier: String = "",
    val dateAcquired: String = "",
    val expiryDate: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inventory_transactions",
    primaryKeys = ["transactionId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["inventoryId"]),
        Index(value = ["inventoryId", "date"])
    ]
)
data class InventoryTransactionEntity(
    val transactionId: String,
    val farmId: String,
    val inventoryId: String,
    val type: String,
    val quantity: Double,
    val unit: String,
    val date: String,
    val sourceType: String,
    val sourceId: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "equipment",
    primaryKeys = ["equipmentId"],
    indices = [Index(value = ["farmId"])]
)
data class EquipmentEntity(
    val equipmentId: String,
    val farmId: String,
    val name: String,
    val status: String,
    val note: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "farm_tasks",
    primaryKeys = ["taskId"],
    indices = [Index(value = ["farmId", "date"])]
)
data class FarmTaskEntity(
    val taskId: String,
    val farmId: String,
    val title: String,
    val category: String,
    val date: String,
    val done: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "assistance",
    primaryKeys = ["requestId"],
    indices = [
        Index(value = ["farmId"]),
        Index(value = ["incidentId"])
    ]
)
data class AssistanceEntity(
    val requestId: String,
    val farmId: String,
    val incidentId: String,
    val program: String,
    val assistanceType: String,
    val dateReceived: String,
    val quantity: String,
    val status: String,
    val source: String,
    val approvedDate: String = "",
    val distributedDate: String = "",
    val completedDate: String = "",
    val distributionDetails: String = "",
    val outcome: String = "",
    val reviewNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "field_incidents",
    primaryKeys = ["incidentId"],
    indices = [Index(value = ["farmId", "status", "date"])]
)
data class FieldIncidentEntity(
    val incidentId: String,
    val farmId: String,
    val type: String,
    val commodity: String,
    val affectedArea: String,
    val date: String,
    val severity: String,
    val description: String,
    val status: String,
    val evidenceUri: String,
    val reviewNotes: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "incident_events",
    primaryKeys = ["eventId"],
    indices = [
        Index(value = ["farmId", "incidentId"]),
        Index(value = ["timestamp"])
    ]
)
data class IncidentEventEntity(
    val eventId: String,
    val farmId: String,
    val incidentId: String,
    val status: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "report_submissions",
    primaryKeys = ["submissionId"]
)
data class ReportSubmissionEntity(
    val submissionId: String = "default",
    val farmId: String,
    val status: String = "Draft",
    val submittedDate: String = "",
    val referenceNo: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
