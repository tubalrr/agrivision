package com.tubalrr.agrivision

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Yard

private val AgriCream = Color(0xFFF7F4E9)
private val AgriCard = Color(0xFFFFFCF5)
private val AgriGreen = Color(0xFF245B3A)
private val AgriGreenSoft = Color(0xFFDDE8C8)
private val AgriSage = Color(0xFFC9D4AD)
private val AgriGold = Color(0xFFCDBB8A)
private val AgriText = Color(0xFF183526)
private val AgriMuted = Color(0xFF7A806F)
private val AgriLine = Color(0xFFE5E2D6)
private val AgriWarning = Color(0xFFD18A27)

data class FarmAsset(val name: String, val type: String, val detail: String)
data class Livestock(val name: String, val kind: String, val count: Int, val status: String)
data class CropRecord(val name: String, val crop: String, val area: String, val stage: String)
data class ProductionRecord(val product: String, val quantity: String, val period: String)
data class ExpenseRecord(val category: String, val amount: Double, val note: String)
data class InventoryItem(val name: String, val quantity: String, val status: String)
data class EquipmentRecord(val name: String, val status: String, val note: String)
data class FarmTask(val title: String, val category: String, val date: String, val done: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgriVisionApp() }
    }
}

@Composable
private fun AgriVisionApp() {
    var selected by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val livestock = remember {
        mutableStateListOf(
            Livestock("Layer Batch 01", "Chickens", 279, "Healthy"),
            Livestock("Native Chickens", "Chickens", 24, "Monitor")
        )
    }
    val crops = remember {
        mutableStateListOf(
            CropRecord("North Plot", "Rice", "1.0 ha", "Growing"),
            CropRecord("Garden", "Vegetables", "0.15 ha", "Active")
        )
    }
    val production = remember {
        mutableStateListOf(
            ProductionRecord("Eggs", "186 pcs", "Today"),
            ProductionRecord("Vegetables", "12 kg", "This week")
        )
    }
    val expenses = remember {
        mutableStateListOf(
            ExpenseRecord("Feeds", 1730.0, "Layer feed"),
            ExpenseRecord("Farm supplies", 620.0, "General supplies"),
            ExpenseRecord("Medicine", 350.0, "Animal care")
        )
    }
    val inventory = remember {
        mutableStateListOf(
            InventoryItem("Layer Feed", "6 sacks", "Good"),
            InventoryItem("Medicine", "3 packs", "Good"),
            InventoryItem("Fertilizer", "2 bags", "Low")
        )
    }
    val equipment = remember {
        mutableStateListOf(
            EquipmentRecord("Water Pump", "Ready", "Last checked recently"),
            EquipmentRecord("Knapsack Sprayer", "Ready", "Good condition"),
            EquipmentRecord("Farm Tools", "Needs check", "Inspect before next use")
        )
    }
    val tasks = remember {
        mutableStateListOf(
            FarmTask("Feed layer chickens", "Livestock", "Today", false),
            FarmTask("Check water supply", "Farm", "Today", false),
            FarmTask("Inspect growing rice", "Crops", "Tomorrow", false),
            FarmTask("Record farm expenses", "Finance", "Today", false)
        )
    }

    val totalExpenses = expenses.sumOf { it.amount }
    val openTasks = tasks.count { !it.done }
    val totalAnimals = livestock.sumOf { it.count }

    val scheme = lightColorScheme(
        primary = AgriGreen,
        onPrimary = Color.White,
        background = AgriCream,
        surface = AgriCard,
        onBackground = AgriText,
        onSurface = AgriText,
        secondary = AgriGold,
        outline = AgriLine
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = AgriCream,
            bottomBar = { AgriBottomBar(selected) { selected = it } }
        ) { padding ->
            when (selected) {
                0 -> DashboardScreen(
                    padding, livestock, crops, production, expenses, inventory, tasks,
                    totalAnimals, totalExpenses, openTasks
                )
                1 -> FarmScreen(
                    padding, livestock, crops, inventory, equipment
                )
                2 -> ProductionFinanceScreen(
                    padding, production, expenses, totalExpenses
                )
                3 -> TasksScreen(padding, tasks)
                else -> ProfileScreen(padding, context)
            }
        }
    }
}

@Composable
private fun AgriBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Farm" to Icons.Outlined.Yard,
        "Production" to Icons.Outlined.Assessment,
        "Tasks" to Icons.Outlined.Checklist,
        "Profile" to Icons.Outlined.Person
    )
    NavigationBar(
        containerColor = AgriCard,
        tonalElevation = 0.dp,
        modifier = Modifier.height(78.dp)
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelected(index) },
                icon = {
                    Icon(
                        item.second,
                        contentDescription = item.first,
                        tint = if (selected == index) AgriGreen else AgriMuted,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        item.first,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = AgriGreen,
                    unselectedTextColor = AgriMuted,
                    indicatorColor = AgriGreenSoft
                )
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    padding: PaddingValues,
    livestock: List<Livestock>,
    crops: List<CropRecord>,
    production: List<ProductionRecord>,
    expenses: List<ExpenseRecord>,
    inventory: List<InventoryItem>,
    tasks: List<FarmTask>,
    totalAnimals: Int,
    totalExpenses: Double,
    openTasks: Int
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AgriVision", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Personal Farm Management", color = AgriMuted)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(AgriGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AV", color = AgriGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Good morning, Farmer", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Here's your farm at a glance.", color = Color.White.copy(alpha = .82f))
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardMiniStat("Animals", totalAnimals.toString(), Modifier.weight(1f))
                        DashboardMiniStat("Fields", crops.size.toString(), Modifier.weight(1f))
                        DashboardMiniStat("Tasks", openTasks.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionTitle("Farm Analytics") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Production", production.size.toString(), "records", Icons.Outlined.Assessment, AgriGreenSoft, Modifier.weight(1f))
                StatCard("Expenses", "₱" + money(totalExpenses), "recorded", Icons.Outlined.MonetizationOn, Color(0xFFE9DFC7), Modifier.weight(1f))
            }
        }

        item { SectionTitle("Farm Status") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatusRow("Livestock", livestock.size.toString() + " groups", Icons.Outlined.Pets, AgriGreen)
                    StatusRow("Crops", crops.size.toString() + " records", Icons.Outlined.LocalFlorist, AgriGreen)
                    StatusRow("Inventory", inventory.count { it.status == "Low" }.toString() + " low stock", Icons.Outlined.Inventory2, if (inventory.any { it.status == "Low" }) AgriWarning else AgriGreen)
                    StatusRow("Tasks", openTasks.toString() + " open", Icons.Outlined.Checklist, if (openTasks > 0) AgriWarning else AgriGreen)
                }
            }
        }

        item { SectionTitle("Production Today") }
        items(production.take(3)) { record ->
            SimpleRecordCard(record.product, record.quantity, record.period, Icons.Outlined.Assessment)
        }

        item { SectionTitle("Today's Attention") }
        item {
            AttentionCard(
                if (openTasks == 0) "Everything is up to date." else openTasks.toString() + " farm task(s) need attention.",
                inventory.firstOrNull { it.status == "Low" }?.name?.let { "Low stock: " + it } ?: "No inventory alerts."
            )
        }
    }
}

@Composable
private fun FarmScreen(
    padding: PaddingValues,
    livestock: MutableList<Livestock>,
    crops: MutableList<CropRecord>,
    inventory: MutableList<InventoryItem>,
    equipment: MutableList<EquipmentRecord>
) {
    var category by remember { mutableStateOf("Livestock") }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("My Farm", "Everything on your farm, not just crops.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmCategory("Livestock", Icons.Outlined.Pets, category == "Livestock") { category = "Livestock" }
                FarmCategory("Crops", Icons.Outlined.LocalFlorist, category == "Crops") { category = "Crops" }
                FarmCategory("Inventory", Icons.Outlined.Inventory2, category == "Inventory") { category = "Inventory" }
                FarmCategory("Equipment", Icons.Outlined.PrecisionManufacturing, category == "Equipment") { category = "Equipment" }
            }
        }

        when (category) {
            "Livestock" -> {
                item { SectionTitle("Livestock Groups") }
                items(livestock) { animal ->
                    FarmRecordCard(animal.name, animal.kind, animal.count.toString() + " heads · " + animal.status, Icons.Outlined.Pets)
                }
                item { AddHint("Add animal groups, feeding, health and mortality records.") }
            }
            "Crops" -> {
                item { SectionTitle("Crop Records") }
                items(crops) { crop ->
                    FarmRecordCard(crop.name, crop.crop, crop.area + " · " + crop.stage, Icons.Outlined.LocalFlorist)
                }
                item { AddHint("Track planting, inputs, growth stage and harvest.") }
            }
            "Inventory" -> {
                item { SectionTitle("Farm Inventory") }
                items(inventory) { item ->
                    FarmRecordCard(item.name, item.quantity, item.status, Icons.Outlined.Inventory2)
                }
                item { AddHint("Feeds, medicine, fertilizer, seeds, tools and supplies.") }
            }
            "Equipment" -> {
                item { SectionTitle("Farm Equipment") }
                items(equipment) { item ->
                    FarmRecordCard(item.name, item.status, item.note, Icons.Outlined.PrecisionManufacturing)
                }
                item { AddHint("Keep maintenance and repair history for every machine or tool.") }
            }
        }
    }
}

@Composable
private fun ProductionFinanceScreen(
    padding: PaddingValues,
    production: MutableList<ProductionRecord>,
    expenses: MutableList<ExpenseRecord>,
    totalExpenses: Double
) {
    var tab by remember { mutableStateOf("Production") }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Production & Finance", "Track what the farm produces and what it costs.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = tab == "Production",
                    onClick = { tab = "Production" },
                    label = { Text("Production") },
                    leadingIcon = { Icon(Icons.Outlined.Assessment, null) }
                )
                FilterChip(
                    selected = tab == "Finance",
                    onClick = { tab = "Finance" },
                    label = { Text("Finance") },
                    leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) }
                )
            }
        }

        if (tab == "Production") {
            item { SummaryCard("Recorded production", production.size.toString() + " records", "Eggs, meat, milk, harvests and other farm products.") }
            item { SectionTitle("Production Records") }
            items(production) { record ->
                FarmRecordCard(record.product, record.quantity, record.period, Icons.Outlined.Assessment)
            }
            item { AddHint("Next upgrade: daily production entry and monthly production charts.") }
        } else {
            item { SummaryCard("Total expenses", "₱" + money(totalExpenses), "Track feeds, supplies, medicine, labor and other costs.") }
            item { SectionTitle("Expense Records") }
            items(expenses) { expense ->
                FarmRecordCard(expense.category, "₱" + money(expense.amount), expense.note, Icons.Outlined.ReceiptLong)
            }
            item { AddHint("Next upgrade: sales, profit, budget and expense categories.") }
        }
    }
}

@Composable
private fun TasksScreen(padding: PaddingValues, tasks: MutableList<FarmTask>) {
    val open = tasks.count { !it.done }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Farm Tasks", open.toString() + " task(s) still open.") }
        items(tasks.indices.toList()) { index ->
            val task = tasks[index]
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(if (task.done) AgriGreen else AgriSage),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (task.done) "✓" else "!", color = if (task.done) Color.White else AgriGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.category + " · " + task.date, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { tasks[index] = task.copy(done = !task.done) }) {
                        Text(if (task.done) "Undo" else "Done")
                    }
                }
            }
        }
        item { AddHint("Tasks cover livestock, crops, equipment, inventory, finance and general farm work.") }
    }
}

@Composable
private fun ProfileScreen(padding: PaddingValues, context: Context) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Profile & Settings", "Personal farm setup.") }
        item { InfoCard("My Farm", "Farm name, owner details, default units and preferences.") }
        item { InfoCard("Reports", "Daily, weekly and monthly farm performance.") }
        item { InfoCard("Backup & Restore", "Keep a local backup of your farm records.") }
        item { InfoCard("Privacy", "AgriVision no longer requires GPS or a live farm map.") }
        item { InfoCard("AgriVision", "See Your Farm. Know What To Do.") }
    }
}

@Composable
private fun FarmCategory(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(90.dp).height(86.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) AgriGreenSoft else AgriCard)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FarmRecordCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(AgriGreenSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(value, color = AgriGreen, fontWeight = FontWeight.SemiBold)
                Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SimpleRecordCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FarmRecordCard(title, value, detail, icon)
}

@Composable
private fun StatusRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = background)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(25.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail, color = AgriGreen, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardMiniStat(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SummaryCard(title: String, value: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, color = AgriMuted)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AgriGreen)
            Spacer(Modifier.height(4.dp))
            Text(body, color = AgriText)
        }
    }
}

@Composable
private fun AttentionCard(primary: String, secondary: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(primary, fontWeight = FontWeight.Bold)
            Text(secondary, color = AgriMuted)
        }
    }
}

@Composable
private fun AddHint(text: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EBDD))
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Settings, null, tint = AgriGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = AgriMuted)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = AgriMuted)
        }
    }
}

private fun money(value: Double): String {
    return String.format(java.util.Locale.US, "%,.0f", value)
}
