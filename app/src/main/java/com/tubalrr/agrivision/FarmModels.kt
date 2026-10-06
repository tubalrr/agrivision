package com.tubalrr.agrivision

data class FarmAsset(val name: String, val type: String, val detail: String)
data class Livestock(
    val name: String,
    val kind: String,
    val count: Int,
    val status: String,
    val groupId: String = "",
    val initialPopulation: Int = count,
    val currentPopulation: Int = count
)

data class LivestockLifecycleEvent(
    val eventId: String = "",
    val livestockId: String,
    val stage: String,
    val date: String,
    val notes: String = "",
    val inputName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val amount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
data class CropRecord(
    val name: String,
    val crop: String,
    val area: String,
    val stage: String,
    val cropId: String = "",
    val fieldId: String = "",
    val plantingDate: String = "",
    val expectedHarvest: String = "",
    val currentStatus: String = stage
)

data class CropLifecycleEvent(
    val eventId: String = "",
    val cropId: String,
    val fieldId: String,
    val stage: String,
    val date: String,
    val notes: String = "",
    val inputName: String = "",
    val quantity: String = "",
    val unit: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
data class ProductionRecord(
    val product: String,
    val quantity: String,
    val period: String,
    val productionId: String = "",
    val sourceType: String = "",
    val sourceId: String = "",
    val fieldId: String = "",
    val areaHectares: Double = 0.0,
    val productionType: String = "Harvest"
)
data class ExpenseRecord(
    val category: String,
    val amount: Double,
    val note: String,
    val date: String = ""
)
data class InventoryItem(
    val name: String,
    val quantity: String,
    val status: String,
    val inventoryId: String = "",
    val category: String = "Other",
    val stock: Double = 0.0,
    val unit: String = "",
    val purchasePrice: Double = 0.0,
    val supplier: String = "",
    val dateAcquired: String = "",
    val expiryDate: String = ""
)

data class InventoryTransaction(
    val transactionId: String = "",
    val inventoryId: String,
    val type: String,
    val quantity: Double,
    val unit: String,
    val date: String,
    val sourceType: String = "",
    val sourceId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
data class EquipmentRecord(val name: String, val status: String, val note: String)
data class FarmTask(val title: String, val category: String, val date: String, val done: Boolean)
data class SaleRecord(
    val product: String,
    val amount: Double,
    val date: String,
    val incomeCategory: String = "Other Income"
)
data class AssistanceRecord(
    val program: String,
    val assistanceType: String,
    val dateReceived: String,
    val quantity: String,
    val status: String,
    val source: String,
    val incidentId: String = "",
    val requestId: String = "",
    val approvedDate: String = "",
    val distributedDate: String = "",
    val completedDate: String = "",
    val distributionDetails: String = "",
    val outcome: String = "",
    val reviewNotes: String = ""
)
data class ReportSubmission(
    val status: String,
    val submittedDate: String,
    val referenceNo: String
)
data class FieldIncident(
    val id: String,
    val type: String,
    val commodity: String,
    val affectedArea: String,
    val date: String,
    val severity: String,
    val description: String,
    val status: String = "Draft",
    val evidenceUri: String = "",
    val reviewNotes: String = "",
    val farmerId: String = "",
    val farmId: String = "",
    val fieldId: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val reviewer: String = "",
    val assistanceRequestId: String = "",
    val resolution: String = ""
)
data class IncidentEvent(
    val incidentId: String,
    val status: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fromStatus: String = "",
    val actor: String = "",
    val eventId: String = ""
)
data class FarmerProfile(
    val farmerName: String,
    val farmerId: String,
    val contact: String,
    val province: String,
    val municipality: String,
    val barangay: String,
    val farmName: String,
    val farmSize: String,
    val landTenure: String,
    val commodities: String,
    val registryStatus: String = "For Review",
    val reviewNotes: String = ""
)


object FinancialCategories {
    val incomeCategories = listOf(
        "Crop Sales",
        "Livestock Sales",
        "Other Income"
    )

    val expenseCategories = listOf(
        "Seeds",
        "Fertilizer",
        "Feed",
        "Labor",
        "Fuel",
        "Medicine",
        "Equipment"
    )
}
