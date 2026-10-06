package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tubalrr.agrivision.domain.calculator.CropLifecycleCalculator
import com.tubalrr.agrivision.domain.model.FieldRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun CropLifecycleSection(
    crops: List<CropRecord>,
    fields: List<FieldRecord>,
    lifecycleEvents: List<CropLifecycleEvent>,
    inventory: List<InventoryItem>,
    onAddCrop: (CropRecord) -> Unit,
    onAddLifecycleEvent: (CropLifecycleEvent) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var manageCrop by remember { mutableStateOf<CropRecord?>(null) }

    if (showAdd) {
        AddCropLifecycleDialog(
            fields = fields,
            onDismiss = { showAdd = false },
            onSave = {
                onAddCrop(it)
                showAdd = false
            }
        )
    }

    manageCrop?.let { crop ->
        CropLifecycleEventDialog(
            crop = crop,
            field = fields.firstOrNull { it.fieldId == crop.fieldId },
            inventory = inventory,
            onDismiss = { manageCrop = null },
            onSave = {
                onAddLifecycleEvent(it)
                manageCrop = null
            }
        )
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Crop Lifecycle", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Follow each crop from land preparation to planting, production and sales.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { showAdd = true },
                    enabled = fields.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("+ Add Crop") }
            }

            if (fields.isEmpty()) {
                InfoCard(
                    "Register a field first",
                    "Each crop cycle must be linked to a Field ID so the lifecycle can follow the field and later feed the map system."
                )
            } else if (crops.isEmpty()) {
                Text(
                    "No crop cycles yet. Add a crop and link it to one of your registered fields.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                crops.forEach { crop ->
                    CropLifecycleCard(
                        crop = crop,
                        field = fields.firstOrNull { it.fieldId == crop.fieldId },
                        events = lifecycleEvents.filter { it.cropId == crop.cropId },
                        onLogEvent = { manageCrop = crop }
                    )
                }
            }
        }
    }
}

@Composable
private fun CropLifecycleCard(
    crop: CropRecord,
    field: FieldRecord?,
    events: List<CropLifecycleEvent>,
    onLogEvent: () -> Unit
) {
    val normalizedStage = CropLifecycleCalculator.normalize(crop.currentStatus.ifBlank { crop.stage })
    val currentIndex = CropLifecycleCalculator.indexOf(normalizedStage)
    val progress = ((currentIndex + 1).toFloat() / CropLifecycleCalculator.stages.size.toFloat()).coerceIn(0f, 1f)
    val stageEvents = events.map { CropLifecycleCalculator.normalize(it.stage) }.toSet()

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCream)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(crop.name, fontWeight = FontWeight.Bold)
                    Text(
                        crop.crop + " · Field " + (field?.name ?: crop.fieldId.ifBlank { "Unlinked" }),
                        color = AgriGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = AgriGreenSoft) {
                    Text(
                        normalizedStage,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = AgriGreen,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                (currentIndex + 1).toString() + "/" + CropLifecycleCalculator.stages.size + " lifecycle stages",
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CropLifecycleCalculator.stages.forEachIndexed { index, stage ->
                    val completed = index <= currentIndex || stageEvents.contains(stage)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (completed) AgriGreenSoft else AgriCard
                    ) {
                        Text(
                            if (completed) "✓ " + stage else stage,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                            color = if (completed) AgriGreen else AgriMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                "Field area: " + (field?.areaHectares?.let { formatCropArea(it) } ?: crop.area),
                color = AgriText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Planting: " + crop.plantingDate.ifBlank { "—" } +
                        " · Expected harvest: " + crop.expectedHarvest.ifBlank { "—" },
                color = AgriMuted,
                style = MaterialTheme.typography.bodySmall
            )

            if (events.isNotEmpty()) {
                Text(
                    "Lifecycle evidence: " + events.size + " recorded update(s)",
                    color = AgriGreen,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLogEvent) {
                    Text("Log Lifecycle Event")
                }
            }
        }
    }
}

@Composable
private fun AddCropLifecycleDialog(
    fields: List<FieldRecord>,
    onDismiss: () -> Unit,
    onSave: (CropRecord) -> Unit
) {
    var selectedFieldId by remember { mutableStateOf(fields.firstOrNull()?.fieldId.orEmpty()) }
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var plantingDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var expectedHarvest by remember { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf("") }
    val context = LocalContext.current

    if (pickerTarget.isNotBlank()) {
        val raw = if (pickerTarget == "planting") plantingDate else expectedHarvest
        val calendar = Calendar.getInstance()
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw.ifBlank { plantingDate }) ?: calendar.time
        } catch (_: Exception) {
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                if (pickerTarget == "planting") plantingDate = value else expectedHarvest = value
                pickerTarget = ""
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { pickerTarget = "" }
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Crop Cycle", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Link to Field", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        fields.forEach { field ->
                            FilterChip(
                                selected = selectedFieldId == field.fieldId,
                                onClick = { selectedFieldId = field.fieldId },
                                label = { Text(field.name) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Crop record name") },
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
                        onClick = { pickerTarget = "planting" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Planting date: " + plantingDate) }
                }
                item {
                    OutlinedButton(
                        onClick = { pickerTarget = "harvest" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Expected harvest: " + expectedHarvest.ifBlank { "Set date" }) }
                }
                item {
                    Text(
                        "The crop starts at Land Preparation. Use lifecycle events to record each operational step.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            val field = fields.firstOrNull { it.fieldId == selectedFieldId }
            TextButton(
                enabled = name.isNotBlank() && crop.isNotBlank() && field != null,
                onClick = {
                    onSave(
                        CropRecord(
                            name = name.trim(),
                            crop = crop.trim(),
                            area = field!!.areaHectares.toString(),
                            stage = "Land Preparation",
                            cropId = "CRP-" + System.currentTimeMillis(),
                            fieldId = field.fieldId,
                            plantingDate = plantingDate,
                            expectedHarvest = expectedHarvest.trim(),
                            currentStatus = "Land Preparation"
                        )
                    )
                }
            ) { Text("Save Crop Cycle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CropLifecycleEventDialog(
    crop: CropRecord,
    field: FieldRecord?,
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSave: (CropLifecycleEvent) -> Unit
) {
    val context = LocalContext.current
    var stage by remember(crop.cropId) {
        mutableStateOf(
            CropLifecycleCalculator.nextStage(crop.currentStatus.ifBlank { crop.stage })
                ?: CropLifecycleCalculator.stages.last()
        )
    }
    var date by remember(crop.cropId) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var notes by remember(crop.cropId) { mutableStateOf("") }
    var inputName by remember(crop.cropId) { mutableStateOf("") }
    var quantity by remember(crop.cropId) { mutableStateOf("") }
    var unit by remember(crop.cropId) { mutableStateOf("") }
    var showPicker by remember(crop.cropId) { mutableStateOf(false) }

    if (showPicker) {
        val calendar = Calendar.getInstance()
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time
        } catch (_: Exception) {
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                showPicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showPicker = false }
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crop Lifecycle Event", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        crop.name + " · " + (field?.name ?: crop.fieldId),
                        color = AgriGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Text("Lifecycle stage", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CropLifecycleCalculator.stages.forEach { option ->
                            FilterChip(
                                selected = stage == option,
                                onClick = { stage = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Event date: " + date) }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What happened?") },
                        minLines = 2
                    )
                }
                item {
                    val inventoryStage =
                        stage == "Land Preparation" ||
                                stage == "Planting" ||
                                stage == "Fertilization" ||
                                stage == "Pest/Disease Monitoring"

                    if (inventoryStage && inventory.isNotEmpty()) {
                        Text("Inventory input", color = AgriGreen, fontWeight = FontWeight.Bold)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(
                                androidx.compose.foundation.rememberScrollState()
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            inventory.forEach { item ->
                                FilterChip(
                                    selected = inputName.equals(item.name, ignoreCase = true),
                                    onClick = {
                                        inputName = item.name
                                        unit = item.unit
                                    },
                                    label = {
                                        Text(
                                            item.name + " (" +
                                                    formatInventoryNumber(item.stock) + " " +
                                                    item.unit + ")",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Input / activity") },
                        singleLine = true
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Quantity") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Unit") },
                            singleLine = true
                        )
                    }
                }
                item {
                    Text(
                        "This event becomes part of the crop's operational history and updates the current lifecycle stage.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = date.isNotBlank(),
                onClick = {
                    onSave(
                        CropLifecycleEvent(
                            cropId = crop.cropId,
                            fieldId = crop.fieldId,
                            stage = stage,
                            date = date,
                            notes = notes.trim(),
                            inputName = inputName.trim(),
                            quantity = quantity.trim(),
                            unit = unit.trim()
                        )
                    )
                }
            ) { Text("Save Event") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatInventoryNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value)

private fun formatCropArea(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() + " ha"
    else String.format(Locale.US, "%.2f ha", value)
