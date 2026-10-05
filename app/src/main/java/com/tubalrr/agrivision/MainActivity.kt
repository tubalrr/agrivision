package com.tubalrr.agrivision

import android.content.Context
import android.os.Bundle
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
data class SaleRecord(val product: String, val amount: Double, val date: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val farmPrefs = getSharedPreferences("agrivision_farm", Context.MODE_PRIVATE)

        val exportBackup = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) exportFarmBackup(this, uri, farmPrefs)
        }

        val importBackup = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) importFarmBackup(this, uri, farmPrefs)
        }

        setContent {
            AgriVisionApp(
                onExportBackup = { exportBackup.launch("agrivision-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json", "text/plain")) }
            )
        }
    }
}

@Composable
private fun AgriVisionApp(
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {}
) {
    var selected by remember { mutableStateOf(0) }
    val context = LocalContext.current

    val farmPrefs = remember {
        context.getSharedPreferences("agrivision_farm", Context.MODE_PRIVATE)
    }

    val livestock = remember {
        mutableStateListOf<Livestock>().apply {
            addAll(loadLivestock(farmPrefs))
        }
    }
    val crops = remember {
        mutableStateListOf<CropRecord>().apply {
            addAll(loadCrops(farmPrefs))
        }
    }
    val production = remember { mutableStateListOf<ProductionRecord>().apply { addAll(loadProduction(farmPrefs)) } }
    val expenses = remember { mutableStateListOf<ExpenseRecord>().apply { addAll(loadExpenses(farmPrefs)) } }
    val sales = remember { mutableStateListOf<SaleRecord>().apply { addAll(loadSales(farmPrefs)) } }
    val inventory = remember {
        mutableStateListOf<InventoryItem>().apply {
            addAll(loadInventory(farmPrefs))
        }
    }
    val equipment = remember {
        mutableStateListOf<EquipmentRecord>().apply {
            addAll(loadEquipment(farmPrefs))
        }
    }
    val tasks = remember {
        mutableStateListOf<FarmTask>().apply { addAll(loadTasks(farmPrefs)) }
    }

    val totalExpenses = expenses.sumOf { it.amount }
    val totalSales = sales.sumOf { it.amount }
    val netIncome = totalSales - totalExpenses
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
                    padding, livestock, crops, inventory, equipment, farmPrefs
                )
                2 -> ProductionFinanceScreen(
                    padding, production, expenses, sales, totalExpenses, totalSales, netIncome, totalAnimals, openTasks, farmPrefs
                )
                3 -> TasksScreen(padding, tasks, inventory, farmPrefs)
                else -> ProfileScreen(padding, onExportBackup, onImportBackup)
            }
        }
    }
}

@Composable
private fun AgriBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Farm" to Icons.Outlined.Yard,
        "Reports" to Icons.Outlined.Assessment,
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
    equipment: MutableList<EquipmentRecord>,
    farmPrefs: android.content.SharedPreferences
) {
    var category by remember { mutableStateOf("Livestock") }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddFarmRecordDialog(
            category = category,
            onDismiss = { showAddDialog = false },
            onAddLivestock = { record ->
                livestock.add(record)
                saveLivestock(farmPrefs, livestock)
                showAddDialog = false
            },
            onAddCrop = { record ->
                crops.add(record)
                saveCrops(farmPrefs, crops)
                showAddDialog = false
            },
            onAddInventory = { record ->
                inventory.add(record)
                saveInventory(farmPrefs, inventory)
                showAddDialog = false
            },
            onAddEquipment = { record ->
                equipment.add(record)
                saveEquipment(farmPrefs, equipment)
                showAddDialog = false
            }
        )
    }

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
                item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Livestock Groups")
                    TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            }
                items(livestock) { animal ->
                    FarmRecordCard(animal.name, animal.kind, animal.count.toString() + " heads · " + animal.status, Icons.Outlined.Pets)
                }
                item { AddHint("Add animal groups, feeding, health and mortality records.") }
            }
            "Crops" -> {
                item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Crop Records")
                    TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            }
                items(crops) { crop ->
                    FarmRecordCard(crop.name, crop.crop, crop.area + " · " + crop.stage, Icons.Outlined.LocalFlorist)
                }
                item { AddHint("Track planting, inputs, growth stage and harvest.") }
            }
            "Inventory" -> {
                item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Farm Inventory")
                    TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            }
                items(inventory) { item ->
                    FarmRecordCard(item.name, item.quantity, item.status, Icons.Outlined.Inventory2)
                }
                item { AddHint("Feeds, medicine, fertilizer, seeds, tools and supplies.") }
            }
            "Equipment" -> {
                item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Farm Equipment")
                    TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                }
            }
                items(equipment) { item ->
                    FarmRecordCard(item.name, item.status, item.note, Icons.Outlined.PrecisionManufacturing)
                }
                item { AddHint("Keep maintenance and repair history for every machine or tool.") }
            }
        }
    }
}

@Composable
private fun AddFarmRecordDialog(
    category: String,
    onDismiss: () -> Unit,
    onAddLivestock: (Livestock) -> Unit,
    onAddCrop: (CropRecord) -> Unit,
    onAddInventory: (InventoryItem) -> Unit,
    onAddEquipment: (EquipmentRecord) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    val title = when (category) {
        "Livestock" -> "Add Livestock"
        "Crops" -> "Add Crop"
        "Inventory" -> "Add Inventory"
        else -> "Add Equipment"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (category == "Crops") "Field / crop name" else "Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Animal type"
                                "Crops" -> "Crop type"
                                "Inventory" -> "Quantity"
                                else -> "Status"
                            }
                        )
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Quantity / heads"
                                "Crops" -> "Area"
                                "Inventory" -> "Stock status"
                                else -> "Maintenance note"
                            }
                        )
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Health status"
                                "Crops" -> "Growth stage"
                                "Inventory" -> "Item name / unit"
                                else -> "Equipment name / detail"
                            }
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    when (category) {
                        "Livestock" -> onAddLivestock(
                            Livestock(
                                name.trim(),
                                type.ifBlank { "Other" }.trim(),
                                value.toIntOrNull() ?: 0,
                                detail.ifBlank { "Healthy" }.trim()
                            )
                        )
                        "Crops" -> onAddCrop(
                            CropRecord(
                                name.trim(),
                                type.ifBlank { "Other" }.trim(),
                                value.ifBlank { "—" }.trim(),
                                detail.ifBlank { "Active" }.trim()
                            )
                        )
                        "Inventory" -> onAddInventory(
                            InventoryItem(
                                detail.ifBlank { name }.trim(),
                                type.ifBlank { "1" }.trim(),
                                value.ifBlank { "Good" }.trim()
                            )
                        )
                        else -> onAddEquipment(
                            EquipmentRecord(
                                name.trim(),
                                type.ifBlank { "Ready" }.trim(),
                                value.ifBlank { detail }.trim()
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ProductionFinanceScreen(
    padding: PaddingValues,
    production: MutableList<ProductionRecord>,
    expenses: MutableList<ExpenseRecord>,
    sales: MutableList<SaleRecord>,
    totalExpenses: Double,
    totalSales: Double,
    netIncome: Double,
    totalAnimals: Int,
    openTasks: Int,
    farmPrefs: android.content.SharedPreferences
) {
    var tab by remember { mutableStateOf("Production") }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddProductionFinanceDialog(
            tab = tab,
            onDismiss = { showDialog = false },
            onProduction = { record -> production.add(record); saveProduction(farmPrefs, production); showDialog = false },
            onExpense = { record -> expenses.add(record); saveExpenses(farmPrefs, expenses); showDialog = false },
            onSale = { record -> sales.add(record); saveSales(farmPrefs, sales); showDialog = false }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader("Farm Reports", "See your farm performance at a glance.") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreen)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Farm Performance", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReportMetric("Sales", "₱" + money(totalSales), Modifier.weight(1f))
                        ReportMetric("Expenses", "₱" + money(totalExpenses), Modifier.weight(1f))
                        ReportMetric("Net", "₱" + money(netIncome), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportStatCard("Animals", totalAnimals.toString(), "heads", Modifier.weight(1f))
                ReportStatCard("Tasks", openTasks.toString(), "open", Modifier.weight(1f))
                ReportStatCard("Production", production.size.toString(), "records", Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = tab == "Production", onClick = { tab = "Production" }, label = { Text("Production") }, leadingIcon = { Icon(Icons.Outlined.Assessment, null) })
                FilterChip(selected = tab == "Finance", onClick = { tab = "Finance" }, label = { Text("Finance") }, leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) })
            }
        }

        if (tab == "Production") {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SummaryCard("Recorded production", production.size.toString() + " records", "Eggs, meat, milk, harvests and other farm products.")
                    TextButton(onClick = { showDialog = true }) { Text("+ Add") }
                }
            }
            item { SectionTitle("Production Records") }
            items(production) { record -> FarmRecordCard(record.product, record.quantity, record.period, Icons.Outlined.Assessment) }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Sales", "₱" + money(totalSales), "Farm product sales.")
                    SummaryCard("Net", "₱" + money(netIncome), "Sales minus expenses.")
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Finance Records")
                    TextButton(onClick = { showDialog = true }) { Text("+ Add") }
                }
            }
            item { Text("Expenses", fontWeight = FontWeight.SemiBold) }
            items(expenses) { expense -> FarmRecordCard(expense.category, "₱" + money(expense.amount), expense.note, Icons.Outlined.ReceiptLong) }
            item { Text("Sales", fontWeight = FontWeight.SemiBold) }
            items(sales) { sale -> FarmRecordCard(sale.product, "₱" + money(sale.amount), sale.date, Icons.Outlined.MonetizationOn) }
        }
    }
}

@Composable
private fun ReportMetric(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ReportStatCard(title: String, value: String, detail: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AgriGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddProductionFinanceDialog(
    tab: String,
    onDismiss: () -> Unit,
    onProduction: (ProductionRecord) -> Unit,
    onExpense: (ExpenseRecord) -> Unit,
    onSale: (SaleRecord) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Today") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tab == "Production") "Add Production" else "Add Finance Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(if (tab == "Production") "Product" else "Type / category") }, singleLine = true)
                if (tab == "Production") {
                    OutlinedTextField(quantity, { quantity = it }, Modifier.fillMaxWidth(), label = { Text("Quantity") }, singleLine = true)
                } else {
                    OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Amount (₱)") }, singleLine = true)
                }
                OutlinedTextField(period, { period = it }, Modifier.fillMaxWidth(), label = { Text("Date / period") }, singleLine = true)
                if (tab == "Finance") Text("For a sale, prefix the product with SALE:", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && (tab == "Production" && quantity.isNotBlank() || tab == "Finance" && amount.toDoubleOrNull() != null),
                onClick = {
                    if (tab == "Production") {
                        onProduction(ProductionRecord(name.trim(), quantity.trim(), period.trim()))
                    } else {
                        val value = amount.toDouble()
                        if (name.trim().startsWith("SALE:", ignoreCase = true)) {
                            onSale(SaleRecord(name.trim().substringAfter(":").trim(), value, period.trim()))
                        } else {
                            onExpense(ExpenseRecord(name.trim(), value, period.trim()))
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TasksScreen(
    padding: PaddingValues,
    tasks: MutableList<FarmTask>,
    inventory: List<InventoryItem>,
    farmPrefs: android.content.SharedPreferences
) {
    val open = tasks.count { !it.done }
    val lowStock = inventory.filter { it.status.equals("Low", ignoreCase = true) }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddTaskDialog(
            onDismiss = { showDialog = false },
            onAdd = {
                tasks.add(it)
                saveTasks(farmPrefs, tasks)
                showDialog = false
            }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tasks & Alerts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Today · " + open + " open task(s)", color = AgriMuted)
                }
                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) { Text("+ Add") }
            }
        }

        item {
            SectionTitle("Active Alerts")
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2DA))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AlertRow(
                        title = if (open > 0) "Farm work needs attention" else "All farm tasks are clear",
                        detail = if (open > 0) open.toString() + " task(s) still open today." else "No unfinished tasks.",
                        warning = open > 0
                    )
                    if (lowStock.isNotEmpty()) {
                        AlertRow(
                            title = "Low inventory — " + lowStock.first().name,
                            detail = "Restock before the next farm activity.",
                            warning = true
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Today's Farm Tasks")
                Text(open.toString() + " open", color = AgriGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        items(tasks.indices.toList()) { index ->
            val task = tasks[index]
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(58.dp)) {
                        Text(task.date, color = AgriGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Box(
                            Modifier.padding(top = 7.dp).size(12.dp).clip(CircleShape)
                                .background(if (task.done) AgriGreen else AgriSage)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold)
                        Text(task.category + " · " + if (task.done) "Completed" else "Upcoming", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        tasks[index] = task.copy(done = !task.done)
                        saveTasks(farmPrefs, tasks)
                    }) {
                        Text(if (task.done) "Undo" else "Done")
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Farm Reminders", fontWeight = FontWeight.Bold, color = AgriGreen)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Use tasks for feeding, watering, cleaning, vaccination, planting, harvesting, repairs and general farm work.",
                        color = AgriText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(title: String, detail: String, warning: Boolean) {
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
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (FarmTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var date by remember { mutableStateOf("Today") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Farm Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Date / time") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onAdd(FarmTask(title.trim(), category.trim(), date.trim(), false)) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProfileScreen(
    padding: PaddingValues,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("Profile & Settings", "Personal farm setup.") }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("My Farm", color = AgriGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Personal farm manager • offline-first", color = AgriMuted)
                }
            }
        }

        item {
            Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup & Restore", fontWeight = FontWeight.Bold)
                    Text("Save all your farm records to a JSON backup file, or restore them later on this device.", color = AgriMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExportBackup, shape = RoundedCornerShape(14.dp)) {
                            Text("Export")
                        }
                        OutlinedButton(onClick = onImportBackup, shape = RoundedCornerShape(14.dp)) {
                            Text("Restore")
                        }
                    }
                }
            }
        }

        item { InfoCard("Privacy", "AgriVision does not require GPS, live maps, or a farm location.") }
        item { InfoCard("Reports", "Daily, weekly and monthly farm performance.") }
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


// ---------- Local farm storage ----------

private fun exportFarmBackup(context: Context, uri: Uri, prefs: android.content.SharedPreferences) {
    val backup = JSONObject().apply {
        put("version", 1)
        put("app", "AgriVision")
        put("livestock", JSONArray(prefs.getString("livestock", "[]")))
        put("crops", JSONArray(prefs.getString("crops", "[]")))
        put("inventory", JSONArray(prefs.getString("inventory", "[]")))
        put("equipment", JSONArray(prefs.getString("equipment", "[]")))
        put("production", JSONArray(prefs.getString("production", "[]")))
        put("expenses", JSONArray(prefs.getString("expenses", "[]")))
        put("sales", JSONArray(prefs.getString("sales", "[]")))
        put("tasks", JSONArray(prefs.getString("tasks", "[]")))
    }
    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(backup.toString(2).toByteArray(Charsets.UTF_8))
    }
}

private fun importFarmBackup(context: Context, uri: Uri, prefs: android.content.SharedPreferences) {
    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: return
    val backup = JSONObject(json)
    val edit = prefs.edit()
    val keys = listOf("livestock", "crops", "inventory", "equipment", "production", "expenses", "sales", "tasks")
    keys.forEach { key ->
        if (backup.has(key)) edit.putString(key, backup.getJSONArray(key).toString())
    }
    edit.apply()
}

private fun loadLivestock(prefs: android.content.SharedPreferences): List<Livestock> {
    val raw = prefs.getString("livestock", null) ?: return listOf(
        Livestock("Layer Batch 01", "Chickens", 279, "Healthy"),
        Livestock("Native Chickens", "Chickens", 24, "Monitor")
    )
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        Livestock(o.getString("name"), o.getString("kind"), o.getInt("count"), o.getString("status"))
    }
}

private fun loadCrops(prefs: android.content.SharedPreferences): List<CropRecord> {
    val raw = prefs.getString("crops", null) ?: return listOf(
        CropRecord("North Plot", "Rice", "1.0 ha", "Growing"),
        CropRecord("Garden", "Vegetables", "0.15 ha", "Active")
    )
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        CropRecord(o.getString("name"), o.getString("crop"), o.getString("area"), o.getString("stage"))
    }
}

private fun loadInventory(prefs: android.content.SharedPreferences): List<InventoryItem> {
    val raw = prefs.getString("inventory", null) ?: return listOf(
        InventoryItem("Layer Feed", "6 sacks", "Good"),
        InventoryItem("Medicine", "3 packs", "Good"),
        InventoryItem("Fertilizer", "2 bags", "Low")
    )
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        InventoryItem(o.getString("name"), o.getString("quantity"), o.getString("status"))
    }
}

private fun loadEquipment(prefs: android.content.SharedPreferences): List<EquipmentRecord> {
    val raw = prefs.getString("equipment", null) ?: return listOf(
        EquipmentRecord("Water Pump", "Ready", "Last checked recently"),
        EquipmentRecord("Knapsack Sprayer", "Ready", "Good condition"),
        EquipmentRecord("Farm Tools", "Needs check", "Inspect before next use")
    )
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        EquipmentRecord(o.getString("name"), o.getString("status"), o.getString("note"))
    }
}

private fun saveLivestock(prefs: android.content.SharedPreferences, list: List<Livestock>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().apply {
        put("name", it.name); put("kind", it.kind); put("count", it.count); put("status", it.status)
    }) }
    prefs.edit().putString("livestock", a.toString()).apply()
}

private fun saveCrops(prefs: android.content.SharedPreferences, list: List<CropRecord>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().apply {
        put("name", it.name); put("crop", it.crop); put("area", it.area); put("stage", it.stage)
    }) }
    prefs.edit().putString("crops", a.toString()).apply()
}

private fun saveInventory(prefs: android.content.SharedPreferences, list: List<InventoryItem>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().apply {
        put("name", it.name); put("quantity", it.quantity); put("status", it.status)
    }) }
    prefs.edit().putString("inventory", a.toString()).apply()
}

private fun saveEquipment(prefs: android.content.SharedPreferences, list: List<EquipmentRecord>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().apply {
        put("name", it.name); put("status", it.status); put("note", it.note)
    }) }
    prefs.edit().putString("equipment", a.toString()).apply()
}

private fun loadProduction(prefs: android.content.SharedPreferences): List<ProductionRecord> {
    val raw = prefs.getString("production", null) ?: return listOf(ProductionRecord("Eggs", "186 pcs", "Today"), ProductionRecord("Vegetables", "12 kg", "This week"))
    val a = JSONArray(raw); return List(a.length()) { i -> val o = a.getJSONObject(i); ProductionRecord(o.getString("product"), o.getString("quantity"), o.getString("period")) }
}
private fun loadExpenses(prefs: android.content.SharedPreferences): List<ExpenseRecord> {
    val raw = prefs.getString("expenses", null) ?: return listOf(ExpenseRecord("Feeds", 1730.0, "Layer feed"), ExpenseRecord("Farm supplies", 620.0, "General supplies"), ExpenseRecord("Medicine", 350.0, "Animal care"))
    val a = JSONArray(raw); return List(a.length()) { i -> val o = a.getJSONObject(i); ExpenseRecord(o.getString("category"), o.getDouble("amount"), o.getString("note")) }
}
private fun loadSales(prefs: android.content.SharedPreferences): List<SaleRecord> {
    val a = JSONArray(prefs.getString("sales", "[]")); return List(a.length()) { i -> val o = a.getJSONObject(i); SaleRecord(o.getString("product"), o.getDouble("amount"), o.getString("date")) }
}
private fun saveProduction(prefs: android.content.SharedPreferences, list: List<ProductionRecord>) {
    val a=JSONArray(); list.forEach { a.put(JSONObject().apply { put("product",it.product); put("quantity",it.quantity); put("period",it.period) }) }; prefs.edit().putString("production",a.toString()).apply()
}
private fun saveExpenses(prefs: android.content.SharedPreferences, list: List<ExpenseRecord>) {
    val a=JSONArray(); list.forEach { a.put(JSONObject().apply { put("category",it.category); put("amount",it.amount); put("note",it.note) }) }; prefs.edit().putString("expenses",a.toString()).apply()
}
private fun saveSales(prefs: android.content.SharedPreferences, list: List<SaleRecord>) {
    val a=JSONArray(); list.forEach { a.put(JSONObject().apply { put("product",it.product); put("amount",it.amount); put("date",it.date) }) }; prefs.edit().putString("sales",a.toString()).apply()
}

private fun loadTasks(prefs: android.content.SharedPreferences): List<FarmTask> {
    val raw = prefs.getString("tasks", null) ?: return listOf(
        FarmTask("Feed layer chickens", "Livestock", "Today", false),
        FarmTask("Check water supply", "Farm", "Today", false),
        FarmTask("Inspect growing rice", "Crops", "Tomorrow", false),
        FarmTask("Record farm expenses", "Finance", "Today", false)
    )
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        FarmTask(o.getString("title"), o.getString("category"), o.getString("date"), o.getBoolean("done"))
    }
}

private fun saveTasks(prefs: android.content.SharedPreferences, list: List<FarmTask>) {
    val a = JSONArray()
    list.forEach { task ->
        a.put(JSONObject().apply {
            put("title", task.title)
            put("category", task.category)
            put("date", task.date)
            put("done", task.done)
        })
    }
    prefs.edit().putString("tasks", a.toString()).apply()
}

private fun money(value: Double): String {
    return String.format(java.util.Locale.US, "%,.0f", value)
}
