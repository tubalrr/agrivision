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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val LivestockLifecycleStages = listOf(
    "Population",
    "Feed",
    "Health",
    "Mortality",
    "Production",
    "Sales"
)

@Composable
internal fun LivestockLifecycleSection(
    livestock: List<Livestock>,
    lifecycleEvents: List<LivestockLifecycleEvent>,
    inventory: List<InventoryItem>,
    onAddLivestock: (Livestock) -> Unit,
    onAddLifecycleEvent: (LivestockLifecycleEvent) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var manageGroup by remember { mutableStateOf<Livestock?>(null) }

    if (showAdd) {
        AddLivestockGroupDialog(
            onDismiss = { showAdd = false },
            onSave = {
                onAddLivestock(it)
                showAdd = false
            }
        )
    }

    manageGroup?.let { group ->
        LivestockLifecycleEventDialog(
            group = group,
            inventory = inventory,
            onDismiss = { manageGroup = null },
            onSave = {
                onAddLifecycleEvent(it)
                manageGroup = null
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
                    Text("Livestock Lifecycle", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Track each livestock group from population through feed, health, mortality, production and sales.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { showAdd = true },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("+ Add Group") }
            }

            if (livestock.isEmpty()) {
                InfoCard(
                    "No livestock groups",
                    "Register a group first. A group gets a stable ID and population baseline for all later records."
                )
            } else {
                livestock.forEach { group ->
                    LivestockLifecycleCard(
                        group = group,
                        events = lifecycleEvents.filter { it.livestockId == group.groupId.ifBlank { group.name } },
                        onLogEvent = { manageGroup = group }
                    )
                }
            }
        }
    }
}

@Composable
private fun LivestockLifecycleCard(
    group: Livestock,
    events: List<LivestockLifecycleEvent>,
    onLogEvent: () -> Unit
) {
    val completed = events.map { it.stage }.map { normalizeLivestockStage(it) }.toSet()
    val currentCount = group.currentPopulation
    val initial = group.initialPopulation.coerceAtLeast(currentCount)

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCream)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(group.name, fontWeight = FontWeight.Bold)
                    Text(group.kind, color = AgriGreen, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Group ID: " + group.groupId.ifBlank { "Legacy group" },
                        color = AgriMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = AgriGreenSoft) {
                    Text(
                        group.status,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = AgriGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LifecycleMetric("Current", currentCount.toString(), Modifier.weight(1f))
                LifecycleMetric("Initial", initial.toString(), Modifier.weight(1f))
                LifecycleMetric("Events", events.size.toString(), Modifier.weight(1f))
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                LivestockLifecycleStages.forEach { stage ->
                    val done = completed.contains(stage)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (done) AgriGreenSoft else AgriCard
                    ) {
                        Text(
                            if (done) "✓ " + stage else stage,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            color = if (done) AgriGreen else AgriMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            val last = events.maxByOrNull { it.createdAt }
            if (last != null) {
                Text(
                    "Latest: " + normalizeLivestockStage(last.stage) +
                            " · " + last.date +
                            if (last.notes.isBlank()) "" else " · " + last.notes,
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLogEvent) { Text("Log Lifecycle Record") }
            }
        }
    }
}

@Composable
private fun LifecycleMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AgriText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddLivestockGroupDialog(
    onDismiss: () -> Unit,
    onSave: (Livestock) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("") }
    var population by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Livestock Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = kind,
                    onValueChange = { kind = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Species / kind") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = population,
                    onValueChange = { population = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Initial population") },
                    singleLine = true
                )
                Text("Group status", color = AgriGreen, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Active", "Quarantine", "Breeding", "For Sale", "Inactive").forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val count = population.toIntOrNull()
            TextButton(
                enabled = name.isNotBlank() && kind.isNotBlank() && count != null && count > 0,
                onClick = {
                    onSave(
                        Livestock(
                            name = name.trim(),
                            kind = kind.trim(),
                            count = count ?: 0,
                            status = status,
                            groupId = "LIV-" + System.currentTimeMillis(),
                            initialPopulation = count ?: 0,
                            currentPopulation = count ?: 0
                        )
                    )
                }
            ) { Text("Save Group") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LivestockLifecycleEventDialog(
    group: Livestock,
    inventory: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSave: (LivestockLifecycleEvent) -> Unit
) {
    val context = LocalContext.current
    var stage by remember(group.groupId) { mutableStateOf("Feed") }
    var date by remember(group.groupId) {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var notes by remember(group.groupId) { mutableStateOf("") }
    var inputName by remember(group.groupId) { mutableStateOf("") }
    var quantity by remember(group.groupId) { mutableStateOf("") }
    var unit by remember(group.groupId) { mutableStateOf("") }
    var amount by remember(group.groupId) { mutableStateOf("") }
    var showPicker by remember(group.groupId) { mutableStateOf(false) }

    if (showPicker) {
        val calendar = Calendar.getInstance()
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time
        } catch (_: Exception) {}
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).format(Calendar.getInstance().apply { set(year, month, day) }.time)
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
        title = { Text("Livestock Lifecycle Record", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        group.name + " · " + group.kind,
                        color = AgriGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LivestockLifecycleStages.forEach { option ->
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
                    ) { Text("Record date: " + date) }
                }
                item {
                    val inventoryStage = stage == "Feed" || stage == "Health"
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
                        label = {
                            Text(
                                when (stage) {
                                    "Feed" -> "Feed name"
                                    "Health" -> "Medicine / health action"
                                    "Production" -> "Product"
                                    "Sales" -> "Buyer / sales reference"
                                    else -> "Activity / source"
                                }
                            )
                        },
                        singleLine = true
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    when (stage) {
                                        "Mortality" -> "Deaths"
                                        "Population" -> "Population"
                                        else -> "Quantity"
                                    }
                                )
                            },
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
                    if (stage == "Sales" || stage == "Production") {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Amount (PHP)") },
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                when (stage) {
                                    "Health" -> "Health observation / treatment notes"
                                    "Mortality" -> "Mortality notes / reason"
                                    else -> "Notes"
                                }
                            )
                        },
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            val parsedQuantity = quantity.toDoubleOrNull() ?: 0.0
            val parsedAmount = amount.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = date.isNotBlank() &&
                        ((stage == "Population" && parsedQuantity >= 0.0) ||
                                stage != "Population"),
                onClick = {
                    onSave(
                        LivestockLifecycleEvent(
                            livestockId = group.groupId.ifBlank { group.name },
                            stage = stage,
                            date = date,
                            notes = notes.trim(),
                            inputName = inputName.trim(),
                            quantity = parsedQuantity,
                            unit = unit.trim(),
                            amount = parsedAmount
                        )
                    )
                }
            ) { Text("Save Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatInventoryNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value)

private fun normalizeLivestockStage(stage: String): String =
    LivestockLifecycleStages.firstOrNull { it.equals(stage, ignoreCase = true) } ?: stage
