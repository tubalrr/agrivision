package com.tubalrr.agrivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FarmField(val name: String, val crop: String, val area: String)
data class FarmInput(val name: String, val quantity: String, val unit: String)
data class FarmTask(val title: String, val date: String, val done: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgriVisionApp() }
    }
}

@Composable
private fun AgriVisionApp() {
    var selected by remember { mutableStateOf(0) }
    val fields = remember { mutableStateListOf(FarmField("Demo Field", "Rice", "1.0 ha")) }
    val inputs = remember { mutableStateListOf(
        FarmInput("Rice Seeds", "25", "kg"),
        FarmInput("Complete Fertilizer", "3", "bags")
    ) }
    val tasks = remember { mutableStateListOf(
        FarmTask("Inspect field", "Today", false),
        FarmTask("Record farm expenses", "Today", false)
    ) }
    val labels = listOf("Home", "Fields", "Inputs", "Tasks", "More")

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Text(label.take(1)) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                0 -> DashboardScreen(padding, fields.size, inputs.size, tasks.count { !it.done })
                1 -> FieldsScreen(padding, fields)
                2 -> InputsScreen(padding, inputs)
                3 -> TasksScreen(padding, tasks)
                else -> MoreScreen(padding)
            }
        }
    }
}

@Composable
private fun DashboardScreen(padding: PaddingValues, fieldCount: Int, inputCount: Int, taskCount: Int) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("AgriVision", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("See Your Farm. Know What To Do.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("Farm overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Fields", fieldCount.toString(), "registered")
                MetricCard("Open Tasks", taskCount.toString(), "needs attention")
                MetricCard("Inventory", inputCount.toString(), "tracked items")
                MetricCard("Harvest", "—", "add harvest records")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Farm attention", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(if (taskCount == 0) "All current tasks are complete." else "$taskCount task(s) need your attention.")
                }
            }
        }
    }
}

@Composable
private fun FieldsScreen(padding: PaddingValues, fields: MutableList<FarmField>) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader("Farm Fields", "Manage your crops and field areas.") }
            items(fields) { field ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(field.name, fontWeight = FontWeight.Bold)
                        Text(field.crop)
                        Text(field.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (showForm) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add field", fontWeight = FontWeight.Bold)
                            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Field name") })
                            OutlinedTextField(crop, { crop = it }, Modifier.fillMaxWidth(), label = { Text("Crop") })
                            OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Area") })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (name.isNotBlank()) {
                                        fields.add(FarmField(name.trim(), crop.ifBlank { "Unknown crop" }.trim(), area.ifBlank { "—" }.trim()))
                                        name = ""; crop = ""; area = ""; showForm = false
                                    }
                                }) { Text("Save") }
                                TextButton(onClick = { showForm = false }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputsScreen(padding: PaddingValues, inputs: MutableList<FarmInput>) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = { FloatingActionButton(onClick = { showForm = true }) { Text("+") } }
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader("Farm Inputs", "Track seeds, fertilizer, feeds and other supplies.") }
            items(inputs) { input ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column { Text(input.name, fontWeight = FontWeight.Bold); Text("Inventory") }
                        Text("${input.quantity} ${input.unit}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (showForm) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add inventory item", fontWeight = FontWeight.Bold)
                            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Item") })
                            OutlinedTextField(quantity, { quantity = it }, Modifier.fillMaxWidth(), label = { Text("Quantity") })
                            OutlinedTextField(unit, { unit = it }, Modifier.fillMaxWidth(), label = { Text("Unit") })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (name.isNotBlank()) {
                                        inputs.add(FarmInput(name.trim(), quantity.ifBlank { "0" }, unit.ifBlank { "unit" }))
                                        name = ""; quantity = ""; unit = ""; showForm = false
                                    }
                                }) { Text("Save") }
                                TextButton(onClick = { showForm = false }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksScreen(padding: PaddingValues, tasks: MutableList<FarmTask>) {
    Scaffold(modifier = Modifier.padding(padding)) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader("Farm Tasks", "Keep daily farm work visible and organized.") }
            items(tasks.indices.toList()) { index ->
                val task = tasks[index]
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold)
                            Text(task.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { tasks[index] = task.copy(done = !task.done) }) {
                            Text(if (task.done) "Done" else "Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("More", "AgriVision tools and farm settings.") }
        item { InfoCard("Reports", "Farm performance and expense reports will live here.") }
        item { InfoCard("Farm Map", "Map and field location records are planned for the next module.") }
        item { InfoCard("Backup & Restore", "Local farm data backup will be added before cloud sync.") }
        item { InfoCard("About AgriVision", "AgriVision — See Your Farm. Know What To Do.") }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
