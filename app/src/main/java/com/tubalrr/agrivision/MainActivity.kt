package com.tubalrr.agrivision

import android.content.Context
import android.os.Bundle
import android.net.Uri
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
data class AssistanceRecord(
    val program: String,
    val assistanceType: String,
    val dateReceived: String,
    val quantity: String,
    val status: String,
    val source: String
)
data class ReportSubmission(
    val status: String,
    val submittedDate: String,
    val referenceNo: String
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
    val commodities: String
)

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
    var farmerProfile by remember { mutableStateOf(loadFarmerProfile(farmPrefs)) }
    var reportSubmission by remember { mutableStateOf(loadReportSubmission(farmPrefs)) }
    val assistance = remember {
        mutableStateListOf<AssistanceRecord>().apply { addAll(loadAssistance(farmPrefs)) }
    }

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
                    totalAnimals, totalExpenses, totalSales, netIncome, openTasks
                )
                1 -> FarmScreen(
                    padding, livestock, crops, inventory, equipment, farmPrefs
                )
                2 -> ProductionFinanceScreen(
                    padding = padding,
                    production = production,
                    expenses = expenses,
                    sales = sales,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    totalAnimals = totalAnimals,
                    openTasks = openTasks,
                    farmPrefs = farmPrefs,
                    farmerProfile = farmerProfile,
                    assistance = assistance,
                    reportSubmission = reportSubmission,
                    onSubmissionSaved = {
                        reportSubmission = it
                        saveReportSubmission(farmPrefs, it)
                    }
                )
                3 -> TasksScreen(padding, tasks, inventory, farmPrefs)
                else -> ProfileScreen(
                    padding = padding,
                    profile = farmerProfile,
                    onProfileSaved = { farmerProfile = it; saveFarmerProfile(farmPrefs, it) },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    assistance = assistance,
                    onAddAssistance = {
                        assistance.add(it)
                        saveAssistance(farmPrefs, assistance)
                    }
                )
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
    totalSales: Double,
    netIncome: Double,
    openTasks: Int
) {
    val lowStock = inventory.count { it.status.equals("Low", ignoreCase = true) }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AgriVision", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AgriGreen)
                    Text("Your personal farm dashboard", color = AgriMuted)
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(AgriGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AV", color = AgriGreen, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Good morning, Farmer", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Everything important about your farm, in one place.", color = Color.White.copy(alpha = .82f))
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardMiniStat("Animals", totalAnimals.toString(), Modifier.weight(1f))
                        DashboardMiniStat("Crops", crops.size.toString(), Modifier.weight(1f))
                        DashboardMiniStat("Tasks", openTasks.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionTitle("Farm Overview") }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardKpiCard("Sales", "₱" + money(totalSales), Icons.Outlined.MonetizationOn, AgriGreenSoft, Modifier.weight(1f))
                DashboardKpiCard("Expenses", "₱" + money(totalExpenses), Icons.Outlined.ReceiptLong, Color(0xFFE9DFC7), Modifier.weight(1f))
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardKpiCard("Net Income", "₱" + money(netIncome), Icons.Outlined.Assessment, Color(0xFFE1EEDB), Modifier.weight(1f))
                DashboardKpiCard("Production", production.size.toString(), Icons.Outlined.LocalFlorist, Color(0xFFECE8D9), Modifier.weight(1f))
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Farm Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Live", color = AgriGreen, fontWeight = FontWeight.SemiBold)
                    }
                    StatusRow("Livestock", livestock.size.toString() + " groups", Icons.Outlined.Pets, AgriGreen)
                    StatusRow("Crops", crops.size.toString() + " records", Icons.Outlined.LocalFlorist, AgriGreen)
                    StatusRow("Inventory", lowStock.toString() + " low stock", Icons.Outlined.Inventory2, if (lowStock > 0) AgriWarning else AgriGreen)
                    StatusRow("Tasks", openTasks.toString() + " open", Icons.Outlined.Checklist, if (openTasks > 0) AgriWarning else AgriGreen)
                }
            }
        }

        item { SectionTitle("Today's Attention") }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (openTasks > 0 || lowStock > 0) Color(0xFFFFE2DA) else AgriGreenSoft
                )
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (openTasks > 0) "$openTasks farm task(s) need attention."
                        else "Your farm tasks are up to date.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (lowStock > 0) "$lowStock inventory item(s) are low."
                        else "No inventory alerts right now.",
                        color = AgriMuted
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Recent Production")
                Text(production.size.toString() + " records", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        items(production.take(3)) { record ->
            SimpleRecordCard(record.product, record.quantity, record.period, Icons.Outlined.Assessment)
        }
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    modifier: Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(9.dp))
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = AgriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    farmPrefs: android.content.SharedPreferences,
    farmerProfile: FarmerProfile,
    assistance: List<AssistanceRecord>,
    reportSubmission: ReportSubmission,
    onSubmissionSaved: (ReportSubmission) -> Unit
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
                FilterChip(selected = tab == "DA Report", onClick = { tab = "DA Report" }, label = { Text("DA Report") }, leadingIcon = { Icon(Icons.Outlined.Assessment, null) })
            }
        }

        if (tab == "DA Report") {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreen)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Agricultural Report", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge)
                        Text(farmerProfile.farmerName.ifBlank { "Farmer not registered" }, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(farmerProfile.farmName.ifBlank { "Farm not registered" }, color = Color.White.copy(alpha = .85f))
                    }
                }
            }
            item { SectionTitle("Farmer & Farm Summary") }
            item {
                ReportSummaryGrid(listOf(
                    "Farmer ID" to farmerProfile.farmerId.ifBlank { "—" },
                    "Farm Area" to farmerProfile.farmSize.ifBlank { "—" },
                    "Location" to listOf(farmerProfile.barangay, farmerProfile.municipality, farmerProfile.province).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "—" },
                    "Commodities" to farmerProfile.commodities.ifBlank { "—" }
                ))
            }
            item { SectionTitle("Agricultural Records") }
            item {
                ReportSummaryGrid(listOf(
                    "Livestock" to (totalAnimals.toString() + " heads"),
                    "Production" to (production.size.toString() + " records"),
                    "Assistance" to (assistance.size.toString() + " records"),
                    "Open Tasks" to openTasks.toString()
                ))
            }
            item { SummaryCard("Assistance Received", assistance.size.toString() + " records", "Seeds, fertilizer, livestock, equipment and other agricultural support.") }
            item { SectionTitle("Assistance Records") }

            item { SectionTitle("DA Report Submission") }

            item {
                ReportSubmissionCard(
                    submission = reportSubmission,
                    onSave = onSubmissionSaved
                )
            }


            if (assistance.isEmpty()) {
                item { InfoCard("No assistance recorded", "Add agricultural assistance from the Farmer & Farm Registry section.") }
            } else {
                items(assistance.takeLast(10).asReversed()) { record ->
                    FarmRecordCard(record.program, record.assistanceType, listOf(record.dateReceived, record.quantity, record.status).filter { it.isNotBlank() }.joinToString(" · "), Icons.Outlined.Inventory2)
                }
            }
        } else if (tab == "Production") {
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
private fun ReportSubmissionCard(
    submission: ReportSubmission,
    onSave: (ReportSubmission) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        ReportSubmissionDialog(
            submission = submission,
            onDismiss = { showDialog = false },
            onSave = {
                onSave(it)
                showDialog = false
            }
        )
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Submission Status", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        submission.status,
                        color = AgriGreen,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(submission.status)
            }

            if (submission.submittedDate.isNotBlank()) {
                Text("Date: " + submission.submittedDate, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (submission.referenceNo.isNotBlank()) {
                Text("Reference: " + submission.referenceNo, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(onClick = { showDialog = true }, shape = RoundedCornerShape(14.dp)) {
                Text("Update Submission")
            }
        }
    }
}

@Composable
private fun ReportSubmissionDialog(
    submission: ReportSubmission,
    onDismiss: () -> Unit,
    onSave: (ReportSubmission) -> Unit
) {
    var status by remember { mutableStateOf(submission.status) }
    var date by remember { mutableStateOf(submission.submittedDate) }
    var reference by remember { mutableStateOf(submission.referenceNo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("DA Report Submission", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text("Status", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("Draft", "Ready for Submission", "Submitted").forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        date,
                        { date = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Submission date") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        reference,
                        { reference = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Reference / tracking number") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(ReportSubmission(status, date.trim(), reference.trim()))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReportSummaryGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (title, value) ->
                    ReportStatCard(title, value, "", Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
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
            onAdd = { record -> tasks.add(record); saveTasks(farmPrefs, tasks); showDialog = false }
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
                    TextButton(onClick = { tasks[index] = task.copy(done = !task.done); saveTasks(farmPrefs, tasks) }) {
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

@Composable
private fun ProfileScreen(
    padding: PaddingValues,
    profile: FarmerProfile,
    onProfileSaved: (FarmerProfile) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    assistance: MutableList<AssistanceRecord>,
    onAddAssistance: (AssistanceRecord) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }

    if (showEdit) {
        FarmerRegistryDialog(
            profile = profile,
            onDismiss = { showEdit = false },
            onSave = {
                onProfileSaved(it)
                showEdit = false
            }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                "Farmer & Farm Registry",
                "DA-ready farm information for organized agricultural records."
            )
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Farmer Profile", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        profile.farmerName.ifBlank { "Farmer not registered" },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (profile.farmerId.isBlank()) "No farmer reference ID yet"
                        else "Farmer ID: " + profile.farmerId,
                        color = Color.White.copy(alpha = .82f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showEdit = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = AgriGreen
                        )
                    ) { Text(if (profile.farmerName.isBlank()) "Register Farmer" else "Edit Profile") }
                }
            }
        }

        item { SectionTitle("Farm Registry") }

        item {
            RegistryInfoCard("Farm Name", profile.farmName.ifBlank { "Not registered" })
        }

        item {
            RegistryInfoCard(
                "Location",
                listOf(profile.barangay, profile.municipality, profile.province)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .ifBlank { "Barangay / Municipality / Province not registered" }
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RegistryInfoCard("Farm Size", profile.farmSize.ifBlank { "Not set" }, Modifier.weight(1f))
                RegistryInfoCard("Land Tenure", profile.landTenure.ifBlank { "Not set" }, Modifier.weight(1f))
            }
        }

        item {
            RegistryInfoCard(
                "Commodities",
                profile.commodities.ifBlank { "Add crops, livestock, fisheries or other commodities." }
            )
        }

        item {
            RegistryInfoCard(
                "Contact",
                profile.contact.ifBlank { "No contact information saved." }
            )
        }

        item { SectionTitle("Data Management") }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup & Restore", fontWeight = FontWeight.Bold)
                    Text(
                        "Export your farmer, farm and operational records as a local JSON backup.",
                        color = AgriMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExportBackup, shape = RoundedCornerShape(14.dp)) { Text("Export") }
                        OutlinedButton(onClick = onImportBackup, shape = RoundedCornerShape(14.dp)) { Text("Restore") }
                    }
                }
            }
        }

        item {
            SectionTitle("Agricultural Assistance")
        }

        item {
            AssistanceSection(
                assistance = assistance,
                onAdd = onAddAssistance
            )
        }

        item {
            InfoCard(
                "Privacy",
                "Farmer information stays on this device unless you intentionally export or share a backup."
            )
        }
    }
}

@Composable
private fun AssistanceSection(
    assistance: List<AssistanceRecord>,
    onAdd: (AssistanceRecord) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("All") }

    if (showAdd) {
        AddAssistanceDialog(
            onDismiss = { showAdd = false },
            onSave = {
                onAdd(it)
                showAdd = false
            }
        )
    }

    val statuses = listOf("All", "Applied", "Approved", "Received", "Completed")
    val filtered = if (filter == "All") assistance
    else assistance.filter { it.status.equals(filter, ignoreCase = true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("DA / Agriculture Programs", fontWeight = FontWeight.Bold, color = AgriGreen)
                        Text(
                            "Track applications and assistance from application to completion.",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(14.dp)) {
                        Text("+ Add")
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statuses.forEach { status ->
                        FilterChip(
                            selected = filter == status,
                            onClick = { filter = status },
                            label = { Text(status, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            InfoCard(
                if (assistance.isEmpty()) "No assistance records" else "No records in $filter",
                if (assistance.isEmpty())
                    "Add an agricultural program or assistance record."
                else
                    "There are no assistance records with this status."
            )
        } else {
            filtered.asReversed().forEach { record ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriCard)
                ) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(record.program, fontWeight = FontWeight.Bold)
                                Text(record.assistanceType, color = AgriGreen, fontWeight = FontWeight.SemiBold)
                            }
                            StatusBadge(record.status)
                        }
                        Text(
                            listOf(record.dateReceived, record.quantity)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (record.source.isNotBlank()) {
                            Text("Source: " + record.source, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val normalized = status.trim().lowercase()
    val background = when (normalized) {
        "completed" -> AgriGreenSoft
        "received" -> Color(0xFFE1EEDB)
        "approved" -> Color(0xFFE9DFC7)
        else -> Color(0xFFF0EBDD)
    }
    Text(
        status.ifBlank { "Unknown" },
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = AgriGreen,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AddAssistanceDialog(
    onDismiss: () -> Unit,
    onSave: (AssistanceRecord) -> Unit
) {
    val context = LocalContext.current
    var program by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Seeds") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var quantity by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Applied") }
    var source by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        val calendar = Calendar.getInstance()
        try { calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time } catch (_: Exception) {}
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
        ).apply { setOnDismissListener { showPicker = false }; show() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Agricultural Assistance", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(program, { program = it }, Modifier.fillMaxWidth(), label = { Text("Program / assistance") }, singleLine = true) }
                item { OutlinedTextField(type, { type = it }, Modifier.fillMaxWidth(), label = { Text("Type") }, singleLine = true) }
                item { OutlinedButton(onClick = { showPicker = true }, Modifier.fillMaxWidth()) { Text("Date received: " + date) } }
                item { OutlinedTextField(quantity, { quantity = it }, Modifier.fillMaxWidth(), label = { Text("Quantity / amount") }, singleLine = true) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Status", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Applied", "Approved", "Received", "Completed").forEach { option ->
                                FilterChip(
                                    selected = status == option,
                                    onClick = { status = option },
                                    label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
                item { OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth(), label = { Text("Source / office") }, singleLine = true) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = program.isNotBlank(),
                onClick = {
                    onSave(AssistanceRecord(program.trim(), type.trim(), date, quantity.trim(), status.trim(), source.trim()))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RegistryInfoCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FarmerRegistryDialog(
    profile: FarmerProfile,
    onDismiss: () -> Unit,
    onSave: (FarmerProfile) -> Unit
) {
    var farmerName by remember { mutableStateOf(profile.farmerName) }
    var farmerId by remember { mutableStateOf(profile.farmerId) }
    var contact by remember { mutableStateOf(profile.contact) }
    var province by remember { mutableStateOf(profile.province) }
    var municipality by remember { mutableStateOf(profile.municipality) }
    var barangay by remember { mutableStateOf(profile.barangay) }
    var farmName by remember { mutableStateOf(profile.farmName) }
    var farmSize by remember { mutableStateOf(profile.farmSize) }
    var landTenure by remember { mutableStateOf(profile.landTenure) }
    var commodities by remember { mutableStateOf(profile.commodities) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Farmer & Farm Registry", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Farmer Information", color = AgriGreen, fontWeight = FontWeight.Bold) }
                item { OutlinedTextField(farmerName, { farmerName = it }, Modifier.fillMaxWidth(), label = { Text("Farmer name") }, singleLine = true) }
                item { OutlinedTextField(farmerId, { farmerId = it }, Modifier.fillMaxWidth(), label = { Text("Farmer ID / reference no.") }, singleLine = true) }
                item { OutlinedTextField(contact, { contact = it }, Modifier.fillMaxWidth(), label = { Text("Contact number") }, singleLine = true) }

                item { Text("Farm Information", color = AgriGreen, fontWeight = FontWeight.Bold) }
                item { OutlinedTextField(farmName, { farmName = it }, Modifier.fillMaxWidth(), label = { Text("Farm name") }, singleLine = true) }
                item { OutlinedTextField(province, { province = it }, Modifier.fillMaxWidth(), label = { Text("Province") }, singleLine = true) }
                item { OutlinedTextField(municipality, { municipality = it }, Modifier.fillMaxWidth(), label = { Text("Municipality / City") }, singleLine = true) }
                item { OutlinedTextField(barangay, { barangay = it }, Modifier.fillMaxWidth(), label = { Text("Barangay") }, singleLine = true) }
                item { OutlinedTextField(farmSize, { farmSize = it }, Modifier.fillMaxWidth(), label = { Text("Farm area / size") }, singleLine = true) }
                item { OutlinedTextField(landTenure, { landTenure = it }, Modifier.fillMaxWidth(), label = { Text("Land tenure / status") }, singleLine = true) }
                item { OutlinedTextField(commodities, { commodities = it }, Modifier.fillMaxWidth(), label = { Text("Commodities") }, minLines = 2) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = farmerName.isNotBlank(),
                onClick = {
                    onSave(
                        FarmerProfile(
                            farmerName.trim(), farmerId.trim(), contact.trim(),
                            province.trim(), municipality.trim(), barangay.trim(),
                            farmName.trim(), farmSize.trim(), landTenure.trim(), commodities.trim()
                        )
                    )
                }
            ) { Text("Save Registry") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
        put("assistance", JSONArray(prefs.getString("assistance", "[]")))
        put("reportSubmission", JSONObject().apply {
            put("status", prefs.getString("reportStatus", "Draft"))
            put("submittedDate", prefs.getString("reportSubmittedDate", ""))
            put("referenceNo", prefs.getString("reportReferenceNo", ""))
        })
        put("farmerProfile", JSONObject().apply {
            put("farmerName", prefs.getString("farmerName", ""))
            put("farmerId", prefs.getString("farmerId", ""))
            put("contact", prefs.getString("contact", ""))
            put("province", prefs.getString("province", ""))
            put("municipality", prefs.getString("municipality", ""))
            put("barangay", prefs.getString("barangay", ""))
            put("farmName", prefs.getString("farmName", ""))
            put("farmSize", prefs.getString("farmSize", ""))
            put("landTenure", prefs.getString("landTenure", ""))
            put("commodities", prefs.getString("commodities", ""))
        })
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
    val keys = listOf("livestock", "crops", "inventory", "equipment", "production", "expenses", "sales", "tasks", "assistance")
    keys.forEach { key ->
        if (backup.has(key)) edit.putString(key, backup.getJSONArray(key).toString())
    }
    if (backup.has("reportSubmission")) {
        val r = backup.getJSONObject("reportSubmission")
        edit.putString("reportStatus", r.optString("status", "Draft"))
        edit.putString("reportSubmittedDate", r.optString("submittedDate"))
        edit.putString("reportReferenceNo", r.optString("referenceNo"))
    }
    if (backup.has("farmerProfile")) {
        val p = backup.getJSONObject("farmerProfile")
        edit.putString("farmerName", p.optString("farmerName"))
        edit.putString("farmerId", p.optString("farmerId"))
        edit.putString("contact", p.optString("contact"))
        edit.putString("province", p.optString("province"))
        edit.putString("municipality", p.optString("municipality"))
        edit.putString("barangay", p.optString("barangay"))
        edit.putString("farmName", p.optString("farmName"))
        edit.putString("farmSize", p.optString("farmSize"))
        edit.putString("landTenure", p.optString("landTenure"))
        edit.putString("commodities", p.optString("commodities"))
    }
    edit.apply()
}

private fun loadAssistance(prefs: android.content.SharedPreferences): List<AssistanceRecord> {
    val raw = prefs.getString("assistance", "[]") ?: "[]"
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        AssistanceRecord(
            o.optString("program"),
            o.optString("assistanceType"),
            o.optString("dateReceived"),
            o.optString("quantity"),
            o.optString("status"),
            o.optString("source")
        )
    }
}

private fun saveAssistance(prefs: android.content.SharedPreferences, list: List<AssistanceRecord>) {
    val a = JSONArray()
    list.forEach {
        a.put(JSONObject().apply {
            put("program", it.program)
            put("assistanceType", it.assistanceType)
            put("dateReceived", it.dateReceived)
            put("quantity", it.quantity)
            put("status", it.status)
            put("source", it.source)
        })
    }
    prefs.edit().putString("assistance", a.toString()).apply()
}

private fun loadReportSubmission(prefs: android.content.SharedPreferences): ReportSubmission {
    return ReportSubmission(
        prefs.getString("reportStatus", "Draft") ?: "Draft",
        prefs.getString("reportSubmittedDate", "") ?: "",
        prefs.getString("reportReferenceNo", "") ?: ""
    )
}

private fun saveReportSubmission(prefs: android.content.SharedPreferences, submission: ReportSubmission) {
    prefs.edit()
        .putString("reportStatus", submission.status)
        .putString("reportSubmittedDate", submission.submittedDate)
        .putString("reportReferenceNo", submission.referenceNo)
        .apply()
}

private fun loadFarmerProfile(prefs: android.content.SharedPreferences): FarmerProfile {
    return FarmerProfile(
        prefs.getString("farmerName", "") ?: "",
        prefs.getString("farmerId", "") ?: "",
        prefs.getString("contact", "") ?: "",
        prefs.getString("province", "") ?: "",
        prefs.getString("municipality", "") ?: "",
        prefs.getString("barangay", "") ?: "",
        prefs.getString("farmName", "") ?: "",
        prefs.getString("farmSize", "") ?: "",
        prefs.getString("landTenure", "") ?: "",
        prefs.getString("commodities", "") ?: ""
    )
}

private fun saveFarmerProfile(prefs: android.content.SharedPreferences, profile: FarmerProfile) {
    prefs.edit()
        .putString("farmerName", profile.farmerName)
        .putString("farmerId", profile.farmerId)
        .putString("contact", profile.contact)
        .putString("province", profile.province)
        .putString("municipality", profile.municipality)
        .putString("barangay", profile.barangay)
        .putString("farmName", profile.farmName)
        .putString("farmSize", profile.farmSize)
        .putString("landTenure", profile.landTenure)
        .putString("commodities", profile.commodities)
        .apply()
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
