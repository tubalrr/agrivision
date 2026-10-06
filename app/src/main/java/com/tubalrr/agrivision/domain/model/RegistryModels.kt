package com.tubalrr.agrivision.domain.model

data class FarmerRecord(
    val farmerId: String,
    val fullName: String,
    val contact: String = ""
)

data class FarmRecord(
    val farmId: String,
    val farmerId: String,
    val farmName: String,
    val province: String = "",
    val municipality: String = "",
    val barangay: String = "",
    val totalArea: String = "",
    val landTenure: String = "",
    val commodities: String = "",
    val registryStatus: String = "For Review",
    val reviewNotes: String = ""
)

data class FieldRecord(
    val fieldId: String,
    val farmId: String,
    val name: String,
    val areaHectares: Double,
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val landTenure: String = "",
    val crop: String = "",
    val plantingDate: String = "",
    val expectedHarvest: String = "",
    val currentStatus: String = "Planned"
)
