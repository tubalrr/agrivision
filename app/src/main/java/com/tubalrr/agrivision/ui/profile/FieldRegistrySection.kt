package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun FieldRegistrySection(
    farm: FarmRecord,
    fields: List<FieldRecord>,
    onAdd: (FieldRecord) -> Unit,
    onUpdate: (FieldRecord) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<FieldRecord?>(null) }

    if (showDialog) {
        FieldRegistryDialog(
            farmId = farm.farmId,
            existing = editingField,
            onDismiss = {
                showDialog = false
                editingField = null
            },
            onSave = { field ->
                if (editingField == null) onAdd(field) else onUpdate(field)
                showDialog = false
                editingField = null
            }
        )
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Field Registry", fontWeight = FontWeight.Bold, color = AgriGreen)
                    Text(
                        "Field-level records prepared for future map, monitoring and DA validation workflows.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        editingField = null
                        showDialog = true
                    },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("+ Add Field") }
            }

            if (fields.isEmpty()) {
                Text(
                    "No field records yet. Add Field 1, Field 2, Field 3, etc. Coordinates are optional and can be entered manually.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                fields.forEach { field ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AgriCream)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(field.name, fontWeight = FontWeight.Bold)
                                    Text(field.fieldId, color = AgriGreen, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(field.currentStatus, color = AgriGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                "${formatFieldArea(field.areaHectares)} · " +
                                        field.crop.ifBlank { "No crop recorded" },
                                color = AgriText,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                field.location.ifBlank { "Location not recorded" },
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Planting: ${field.plantingDate.ifBlank { "—" }} · Harvest: ${field.expectedHarvest.ifBlank { "—" }}",
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Tenure: ${field.landTenure.ifBlank { "—" }}",
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                coordinateLabel(field),
                                color = if (field.latitude != null && field.longitude != null) AgriGreen else AgriMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = {
                                    editingField = field
                                    showDialog = true
                                }) { Text("Edit") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldRegistryDialog(
    farmId: String,
    existing: FieldRecord?,
    onDismiss: () -> Unit,
    onSave: (FieldRecord) -> Unit
) {
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time) }

    var fieldId by remember(existing?.fieldId) {
        mutableStateOf(existing?.fieldId ?: "FLD-" + System.currentTimeMillis())
    }
    var name by remember(existing?.fieldId) { mutableStateOf(existing?.name ?: "Field 1") }
    var area by remember(existing?.fieldId) { mutableStateOf(existing?.areaHectares?.toString() ?: "") }
    var location by remember(existing?.fieldId) { mutableStateOf(existing?.location ?: "") }
    var latitude by remember(existing?.fieldId) { mutableStateOf(existing?.latitude?.toString() ?: "") }
    var longitude by remember(existing?.fieldId) { mutableStateOf(existing?.longitude?.toString() ?: "") }
    var landTenure by remember(existing?.fieldId) { mutableStateOf(existing?.landTenure ?: "") }
    var crop by remember(existing?.fieldId) { mutableStateOf(existing?.crop ?: "") }
    var plantingDate by remember(existing?.fieldId) { mutableStateOf(existing?.plantingDate ?: "") }
    var expectedHarvest by remember(existing?.fieldId) { mutableStateOf(existing?.expectedHarvest ?: "") }
    var status by remember(existing?.fieldId) { mutableStateOf(existing?.currentStatus ?: "Planned") }
    var dateTarget by remember { mutableStateOf("") }

    if (dateTarget.isNotBlank()) {
        val calendar = Calendar.getInstance()
        val raw = if (dateTarget == "planting") plantingDate else expectedHarvest
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.ifBlank { today }) ?: calendar.time
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                if (dateTarget == "planting") plantingDate = value else expectedHarvest = value
                dateTarget = ""
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { dateTarget = "" }
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Register Field" else "Edit Field", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = fieldId,
                        onValueChange = { fieldId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Field ID") },
                        singleLine = true,
                        enabled = existing == null
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Field name") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Area (hectares)") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Location / local description") },
                        minLines = 2
                    )
                }
                item { Text("Coordinates", color = AgriGreen, fontWeight = FontWeight.Bold) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latitude,
                            onValueChange = { latitude = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Latitude") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = longitude,
                            onValueChange = { longitude = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Longitude") },
                            singleLine = true
                        )
                    }
                }
                item {
                    Text(
                        "Coordinates are stored only when entered. No GPS permission is required for this registry form.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                item {
                    OutlinedTextField(
                        value = landTenure,
                        onValueChange = { landTenure = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Land tenure") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = crop,
                        onValueChange = { crop = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Crop / commodity") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { dateTarget = "planting" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Planting date: " + plantingDate.ifBlank { "Set date" }) }
                }
                item {
                    OutlinedButton(
                        onClick = { dateTarget = "harvest" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Expected harvest: " + expectedHarvest.ifBlank { "Set date" }) }
                }
                item {
                    Text("Current status", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Planned", "Planted", "Growing", "Harvesting", "Fallow", "Inactive").forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val areaValue = area.toDoubleOrNull()
            val latValue = latitude.toDoubleOrNull()
            val lonValue = longitude.toDoubleOrNull()
            TextButton(
                enabled = fieldId.isNotBlank() &&
                        name.isNotBlank() &&
                        areaValue != null &&
                        areaValue >= 0.0 &&
                        ((latValue == null && lonValue == null) || (latValue != null && lonValue != null)),
                onClick = {
                    onSave(
                        FieldRecord(
                            fieldId = fieldId.trim(),
                            farmId = farmId,
                            name = name.trim(),
                            areaHectares = areaValue ?: 0.0,
                            location = location.trim(),
                            latitude = latValue,
                            longitude = lonValue,
                            landTenure = landTenure.trim(),
                            crop = crop.trim(),
                            plantingDate = plantingDate.trim(),
                            expectedHarvest = expectedHarvest.trim(),
                            currentStatus = status
                        )
                    )
                }
            ) { Text("Save Field") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatFieldArea(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} ha" else String.format(Locale.US, "%.2f ha", value)

private fun coordinateLabel(field: FieldRecord): String =
    if (field.latitude != null && field.longitude != null)
        "Coordinates ready: ${field.latitude}, ${field.longitude}"
    else
        "Coordinates: not set"
