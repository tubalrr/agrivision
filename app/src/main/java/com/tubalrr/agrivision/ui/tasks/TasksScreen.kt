package com.tubalrr.agrivision

import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Brush

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
    val tomorrowKey = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        )
    }
    var selectedDate by remember { mutableStateOf(todayKey) }
    var selectedFilter by remember { mutableStateOf("All") }
    var showDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val lowStock = inventory.filter {
        it.stock <= 0.0 || it.status.contains("low", ignoreCase = true)
    }
    val openTasks = tasks.filter { !it.done }
    val completedTasks = tasks.count { it.done }
    val overdueTasks = openTasks.count { isTaskOverdue(it.date, todayKey) }
    val dateTasks = tasks.filter {
        it.date == selectedDate || (selectedDate == todayKey && it.date.equals("Today", ignoreCase = true))
    }
    val selectedOpen = dateTasks.count { !it.done }
    val selectedCompleted = dateTasks.count { it.done }
    val visibleTasks = dateTasks.filter {
        when (selectedFilter) {
            "Open" -> !it.done
            "Completed" -> it.done
            else -> true
        }
    }.sortedWith(
        compareBy<FarmTask> { it.done }
            .thenBy { it.date }
            .thenBy { it.title.lowercase(Locale.US) }
    )

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onAdd = { record -> onAddTask(record); showDialog = false }
        )
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate) ?: calendar.time
        } catch (_: Exception) { }
        DatePickerDialog(
            LocalContext.current,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                selectedFilter = "All"
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).apply { setOnDismissListener { showDatePicker = false }; show() }
    }

    val formattedSelectedDate = try {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US).format(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)!!
        )
    } catch (_: Exception) { selectedDate }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A6442), AgriGreenDeep, Color(0xFF0A291B))
                        )
                    )
                    .border(1.dp, Color(0xFF377B55), RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.07f),
                    modifier = Modifier.size(120.dp).align(Alignment.CenterEnd)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Outlined.TaskAlt, null, tint = AgriGold, modifier = Modifier.size(17.dp))
                        Text(
                            "FARM TASK CENTER",
                            color = AgriGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Text(
                        "Make every farm day count.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )
                    Text(
                        "Keep feeding, fieldwork, health checks and harvest activities on track.",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(Icons.Outlined.EventAvailable, null, tint = Color(0xFFB9E85A), modifier = Modifier.size(17.dp))
                        Text(
                            formattedSelectedDate,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = { showDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB9E85A),
                            contentColor = Color(0xFF103B2A)
                        )
                    ) {
                        Icon(Icons.Outlined.AddTask, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Create farm task", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TaskMetricCard(
                        title = "Overdue",
                        value = overdueTasks.toString(),
                        detail = "past due",
                        icon = Icons.Outlined.WarningAmber,
                        accent = Color(0xFFFFE5DC),
                        tint = Color(0xFFAC4E2E),
                        modifier = Modifier.weight(1f)
                    )
                    TaskMetricCard(
                        title = "Open tasks",
                        value = openTasks.size.toString(),
                        detail = "not completed",
                        icon = Icons.Outlined.Schedule,
                        accent = Color(0xFFFFF0C7),
                        tint = Color(0xFF906B18),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TaskMetricCard(
                        title = "Completed",
                        value = completedTasks.toString(),
                        detail = "tasks finished",
                        icon = Icons.Outlined.TaskAlt,
                        accent = Color(0xFFDFF0E3),
                        tint = AgriGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TaskMetricCard(
                        title = "Stock alerts",
                        value = lowStock.size.toString(),
                        detail = "items to check",
                        icon = Icons.Outlined.Inventory2,
                        accent = if (lowStock.isEmpty()) Color(0xFFDFF0E3) else Color(0xFFFFE5DC),
                        tint = if (lowStock.isEmpty()) AgriGreen else Color(0xFFAC4E2E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, AgriLine, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(AgriGreenSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Event, null, tint = AgriGreen, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Schedule planner", color = AgriText, fontWeight = FontWeight.Bold)
                            Text("View tasks by date", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.EditCalendar, contentDescription = "Choose a date", tint = AgriGreen)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        FilterChip(
                            selected = selectedDate == todayKey,
                            onClick = { selectedDate = todayKey; selectedFilter = "All" },
                            label = { Text("Today") },
                            leadingIcon = { Icon(Icons.Outlined.Today, null, modifier = Modifier.size(17.dp)) }
                        )
                        FilterChip(
                            selected = selectedDate == tomorrowKey,
                            onClick = { selectedDate = tomorrowKey; selectedFilter = "All" },
                            label = { Text("Tomorrow") },
                            leadingIcon = { Icon(Icons.Outlined.NextPlan, null, modifier = Modifier.size(17.dp)) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { showDatePicker = true },
                            label = { Text("Choose date") },
                            leadingIcon = { Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(17.dp)) }
                        )
                    }
                    Text(
                        selectedTasksCountLabel(dateTasks.size, selectedOpen, selectedCompleted),
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Your task list")
                    Spacer(Modifier.weight(1f))
                    Text(visibleTasks.size.toString() + " shown", color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf("All", "Open", "Completed").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            leadingIcon = {
                                if (selectedFilter == filter) {
                                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        if (visibleTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AgriLine, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriCard)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            Modifier.size(64.dp).clip(RoundedCornerShape(21.dp)).background(AgriGreenSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (selectedFilter == "Completed") Icons.Outlined.TaskAlt else Icons.Outlined.EventAvailable,
                                null,
                                tint = AgriGreen,
                                modifier = Modifier.size(31.dp)
                            )
                        }
                        Text(
                            when (selectedFilter) {
                                "Open" -> "All caught up!"
                                "Completed" -> "Nothing completed yet"
                                else -> "No tasks scheduled"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            when (selectedFilter) {
                                "Open" -> "There are no unfinished tasks for this date."
                                "Completed" -> "Completed tasks will appear here."
                                else -> "Add a farm task or choose another date."
                            },
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (selectedFilter != "Completed") {
                            OutlinedButton(onClick = { showDialog = true }, shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add task")
                            }
                        }
                    }
                }
            }
        }

        items(visibleTasks) { task ->
            FarmTaskCard(task = task, onToggle = { onToggleTask(task.copy(done = !task.done)) })
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenDeep)
            ) {
                Row(
                    Modifier.padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.11f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.TipsAndUpdates, null, tint = AgriGold, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("A little planning goes a long way", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "Use recurring dates in your routine for feeding, watering, cleaning, vaccination, planting, harvesting and maintenance checks.",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskMetricCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, AgriLine, RoundedCornerShape(21.dp)),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = AgriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(value, color = AgriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(detail, color = AgriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FarmTaskCard(task: FarmTask, onToggle: () -> Unit) {
    val category = task.category.ifBlank { "General" }
    val dueLabel = taskDueLabel(task.date)
    val isOverdue = !task.done && dueLabel == "Overdue"
    val icon = when (category.lowercase(Locale.US)) {
        "feeding", "feed", "watering" -> Icons.Outlined.WaterDrop
        "health", "vaccination" -> Icons.Outlined.HealthAndSafety
        "planting", "crop", "harvest" -> Icons.Outlined.Spa
        "inventory", "stock" -> Icons.Outlined.Inventory2
        "maintenance", "repair" -> Icons.Outlined.Build
        "cleaning" -> Icons.Outlined.CleaningServices
        else -> Icons.Outlined.Assignment
    }
    Card(
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            if (isOverdue) Color(0xFFE8B6A5) else AgriLine,
            RoundedCornerShape(22.dp)
        ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(
                    when {
                        task.done -> AgriGreenSoft
                        isOverdue -> Color(0xFFFFE5DC)
                        else -> Color(0xFFE6EDF4)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (task.done) Icons.Outlined.TaskAlt else icon,
                    null,
                    tint = when {
                        task.done -> AgriGreen
                        isOverdue -> Color(0xFFAC4E2E)
                        else -> Color(0xFF456781)
                    },
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    task.title,
                    color = if (task.done) AgriMuted else AgriText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(7.dp), color = AgriCream) {
                        Text(
                            category,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            color = AgriMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = when {
                            task.done -> AgriGreenSoft
                            isOverdue -> Color(0xFFFFE5DC)
                            else -> Color(0xFFE6EDF4)
                        }
                    ) {
                        Text(
                            if (task.done) "Completed" else dueLabel,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            color = when {
                                task.done -> AgriGreen
                                isOverdue -> Color(0xFFAC4E2E)
                                else -> Color(0xFF456781)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(task.date, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(43.dp).clip(CircleShape).background(if (task.done) AgriGreenSoft else Color(0xFFB9E85A))
                ) {
                    Icon(
                        if (task.done) Icons.Outlined.Replay else Icons.Outlined.Check,
                        contentDescription = if (task.done) "Mark as open" else "Mark completed",
                        tint = AgriGreenDeep
                    )
                }
                Text(
                    if (task.done) "Undo" else "Done",
                    color = AgriMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun isTaskOverdue(date: String, todayKey: String): Boolean {
    if (date.isBlank() || date.equals("Today", ignoreCase = true) || date.equals("Tomorrow", ignoreCase = true)) return false
    val taskDate = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) }.getOrNull() ?: return false
    val today = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(todayKey) }.getOrNull() ?: return false
    return taskDate.before(today)
}

private fun taskDueLabel(date: String): String {
    if (date.equals("Today", ignoreCase = true)) return "Due today"
    if (date.equals("Tomorrow", ignoreCase = true)) return "Tomorrow"
    val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    if (date == key) return "Due today"
    return if (isTaskOverdue(date, key)) "Overdue" else date
}

private fun selectedTasksCountLabel(total: Int, open: Int, completed: Int): String =
    "$total scheduled · $open open · $completed completed"

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
                Text("Task category", color = AgriGreen, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("General", "Feeding", "Health", "Cleaning", "Planting", "Harvest", "Inventory", "Maintenance").forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) { Text("Date: " + date) }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onAdd(FarmTask(title.trim(), category.trim(), date, false)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
