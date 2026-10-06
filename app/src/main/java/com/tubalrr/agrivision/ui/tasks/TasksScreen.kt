package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*


@Composable
internal fun TasksScreen(
    padding: PaddingValues,
    tasks: List<FarmTask>,
    inventory: List<InventoryItem>,
    onAddTask: (FarmTask) -> Unit,
    onToggleTask: (FarmTask) -> Unit
) {
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time) }
    var selectedDate by remember { mutableStateOf(todayKey) }
    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val lowStock = inventory.filter { it.status.equals("Low", ignoreCase = true) }
    val selectedTasks = tasks.filter { it.date == selectedDate || (selectedDate == todayKey && it.date.equals("Today", ignoreCase = true)) }
    val open = tasks.count { !it.done }

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onAdd = { record -> onAddTask(record); showDialog = false }
        )
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        try { calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate) ?: calendar.time } catch (_: Exception) { }
        DatePickerDialog(
            LocalContext.current,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).apply { setOnDismissListener { showDatePicker = false }; show() }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tasks & Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(open.toString() + " open task(s)", color = AgriMuted)
                }
                Button(onClick = { showDialog = true }, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)) { Text("+ Add") }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Farm Schedule", color = AgriGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        try { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)!!) }
                        catch (_: Exception) { selectedDate },
                        color = AgriText
                    )
                    Button(onClick = { showDatePicker = true }, shape = RoundedCornerShape(14.dp)) { Text("Select Date") }
                }
            }
        }

        item { SectionTitle("Active Alerts") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2DA))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlertRow(
                        title = if (selectedTasks.any { !it.done }) "Tasks need attention" else "No unfinished tasks",
                        detail = if (selectedTasks.any { !it.done }) selectedTasks.count { !it.done }.toString() + " task(s) scheduled for this date." else "This date is clear.",
                        warning = selectedTasks.any { !it.done }
                    )
                    if (lowStock.isNotEmpty()) {
                        AlertRow("Low inventory — " + lowStock.first().name, "Restock before the next farm activity.", true)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Scheduled Tasks")
                Text(selectedTasks.size.toString() + " task(s)", color = AgriGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        if (selectedTasks.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No tasks scheduled", fontWeight = FontWeight.Bold)
                        Text("Add a farm task for this date.", color = AgriMuted)
                    }
                }
            }
        }

        items(tasks.indices.toList().filter { index ->
            val date = tasks[index].date
            date == selectedDate || (selectedDate == todayKey && date.equals("Today", ignoreCase = true))
        }) { index ->
            val task = tasks[index]
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(if (task.done) AgriGreen else AgriSage), contentAlignment = Alignment.Center) {
                        Text(if (task.done) "✓" else "!", color = if (task.done) Color.White else AgriGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.category + " · " + if (task.done) "Completed" else "Upcoming", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onToggleTask(task.copy(done = !task.done)) }) {
                        Text(if (task.done) "Undo" else "Done")
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Farm Reminders", fontWeight = FontWeight.Bold, color = AgriGreen)
                    Spacer(Modifier.height(5.dp))
                    Text("Schedule feeding, watering, cleaning, vaccination, planting, harvesting, repairs and general farm work by date.", color = AgriText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun AlertRow(title: String, detail: String, warning: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                .background(if (warning) Color(0xFFFFA98F) else AgriGreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(if (warning) "!" else "✓", fontWeight = FontWeight.Bold, color = AgriGreen)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (FarmTask) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val calendar = Calendar.getInstance()
        try { calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time } catch (_: Exception) { }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                showPicker = false
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).apply { setOnDismissListener { showPicker = false }; show() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Farm Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Task") }, singleLine = true)
                OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Date: " + date) }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onAdd(FarmTask(title.trim(), category.trim(), date, false)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
