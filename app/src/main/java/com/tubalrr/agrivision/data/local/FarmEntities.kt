package com.tubalrr.agrivision.data.local

import androidx.room.Entity
import androidx.room.Index

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
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "crops",
    primaryKeys = ["cropId"],
    indices = [Index(value = ["farmId"])]
)
data class CropEntity(
    val cropId: String,
    val farmId: String,
    val name: String,
    val crop: String,
    val area: String,
    val stage: String,
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
    val updatedAt: Long = System.currentTimeMillis()
)
