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
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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

class MainActivity : ComponentActivity() {
    private val farmViewModel: FarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exportBackup = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) farmViewModel.exportBackup(uri)
        }

        val importBackup = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) farmViewModel.importBackup(uri)
        }

        val exportCasePackage = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) farmViewModel.exportCasePackage(uri)
        }

        setContent {
            AgriVisionApp(
                onExportBackup = { exportBackup.launch("agrivision-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json", "text/plain")) },
                onExportCasePackage = { exportCasePackage.launch("agrivision-da-case-package.json") }
            )
        }
    }
}

@Composable
private fun AgriVisionApp(
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onExportCasePackage: () -> Unit = {},
    farmViewModel: FarmViewModel = viewModel()
) {
    var selected by remember { mutableStateOf(0) }

    val livestock by farmViewModel.livestock.collectAsStateWithLifecycle()
    val crops by farmViewModel.crops.collectAsStateWithLifecycle()
    val production by farmViewModel.production.collectAsStateWithLifecycle()
    val expenses by farmViewModel.expenses.collectAsStateWithLifecycle()
    val sales by farmViewModel.sales.collectAsStateWithLifecycle()
    val inventory by farmViewModel.inventory.collectAsStateWithLifecycle()
    val equipment by farmViewModel.equipment.collectAsStateWithLifecycle()
    val tasks by farmViewModel.tasks.collectAsStateWithLifecycle()
    val assistance by farmViewModel.assistance.collectAsStateWithLifecycle()
    val fieldIncidents by farmViewModel.fieldIncidents.collectAsStateWithLifecycle()
    val incidentEvents by farmViewModel.incidentEvents.collectAsStateWithLifecycle()
    val farmerProfile by farmViewModel.profile.collectAsStateWithLifecycle()
    val reportSubmission by farmViewModel.reportSubmission.collectAsStateWithLifecycle()
    val totalSales by farmViewModel.totalSales.collectAsStateWithLifecycle()
    val totalExpenses by farmViewModel.totalExpenses.collectAsStateWithLifecycle()
    val productionCount by farmViewModel.productionCount.collectAsStateWithLifecycle()
    val totalLivestock by farmViewModel.totalLivestock.collectAsStateWithLifecycle()
    val netIncome = totalSales - totalExpenses
    val openTasks = tasks.count { !it.done }

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
                    padding = padding,
                    livestock = livestock,
                    crops = crops,
                    production = production,
                    expenses = expenses,
                    inventory = inventory,
                    tasks = tasks,
                    fieldIncidents = fieldIncidents,
                    incidentEvents = incidentEvents,
                    assistance = assistance,
                    farmViewModel = farmViewModel,
                    totalAnimals = totalLivestock,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    openTasks = openTasks
                )
                1 -> FarmScreen(
                    padding = padding,
                    livestock = livestock,
                    crops = crops,
                    inventory = inventory,
                    equipment = equipment,
                    onAddLivestock = farmViewModel::addLivestock,
                    onAddCrop = farmViewModel::addCrop,
                    onAddInventory = farmViewModel::addInventory,
                    onAddEquipment = farmViewModel::addEquipment
                )
                2 -> ProductionFinanceScreen(
                    padding = padding,
                    production = production,
                    expenses = expenses,
                    sales = sales,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    totalAnimals = totalLivestock,
                    openTasks = openTasks,
                    farmerProfile = farmerProfile,
                    assistance = assistance,
                    fieldIncidents = fieldIncidents,
                    incidentEvents = incidentEvents,
                    onExportCasePackage = onExportCasePackage,
                    reportSubmission = reportSubmission,
                    onProduction = farmViewModel::addProduction,
                    onExpense = farmViewModel::addExpense,
                    onSale = farmViewModel::addSale,
                    onSubmissionSaved = farmViewModel::saveReportSubmission
                )
                3 -> TasksScreen(
                    padding = padding,
                    tasks = tasks,
                    inventory = inventory,
                    onAddTask = farmViewModel::addTask,
                    onToggleTask = farmViewModel::toggleTask
                )
                else -> ProfileScreen(
                    padding = padding,
                    profile = farmerProfile,
                    onProfileSaved = farmViewModel::saveProfile,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    assistance = assistance,
                    onAddAssistance = farmViewModel::addAssistance,
                    onUpdateAssistance = farmViewModel::updateAssistance
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
    fieldIncidents: MutableList<FieldIncident>,
    incidentEvents: MutableList<IncidentEvent>,
    assistance: MutableList<AssistanceRecord>,
    farmPrefs: android.content.SharedPreferences,
    totalAnimals: Int,
    totalExpenses: Double,
    totalSales: Double,
    netIncome: Double,
    openTasks: Int
) {
    val lowStock = inventory.count { it.status.equals("Low", ignoreCase = true) }
    val completedTasks = tasks.count { it.done }
    val totalTasks = tasks.size
    val submittedIncidents = fieldIncidents.count { it.status == "Submitted" }
    val reviewIncidents = fieldIncidents.count { it.status == "Under Review" }
    val verifiedIncidents = fieldIncidents.count { it.status == "Verified" }
    val activeIncidents = fieldIncidents.filter { it.status != "Resolved" }
    val urgentCases = activeIncidents.count {
        it.severity == "Critical" || it.severity == "High"
    }
    val missingEvidence = activeIncidents.count {
        it.evidenceUri.isBlank() && it.status != "Draft"
    }
    val assistancePending = activeIncidents.count { incident ->
        incident.status == "Verified" &&
                assistance.none {
                    it.incidentId == incident.id && it.status == "Completed"
                }
    }
    val closureReady = fieldIncidents.count { incident ->
        incident.status == "Verified" &&
                assistance.any {
                    it.incidentId == incident.id && it.status == "Completed"
                }
    }
    var showIncidentDialog by remember { mutableStateOf(false) }
    var reviewIncident by remember { mutableStateOf<FieldIncident?>(null) }
    var timelineIncident by remember { mutableStateOf<FieldIncident?>(null) }

    if (showIncidentDialog) {
        AddFieldIncidentDialog(
            onDismiss = { showIncidentDialog = false },
            onSave = { incident ->
                fieldIncidents.add(incident)
                incidentEvents.add(IncidentEvent(incident.id, "Draft", "Report created"))
                saveFieldIncidents(farmPrefs, fieldIncidents)
                saveIncidentEvents(farmPrefs, incidentEvents)
                showIncidentDialog = false
            }
        )
    }

    timelineIncident?.let { incident ->
        IncidentTimelineDialog(
            incident = incident,
            events = incidentEvents.filter { it.incidentId == incident.id }.sortedByDescending { it.timestamp },
            onDismiss = { timelineIncident = null }
        )
    }

    reviewIncident?.let { incident ->
        IncidentReviewDialog(
            incident = incident,
            onDismiss = { reviewIncident = null },
            onSave = { updated ->
                val index = fieldIncidents.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    fieldIncidents[index] = updated
                    if (updated.status != incident.status || updated.reviewNotes != incident.reviewNotes) {
                        incidentEvents.add(
                            IncidentEvent(
                                incidentId = updated.id,
                                status = updated.status,
                                note = updated.reviewNotes.ifBlank { "Reviewer updated case status" }
                            )
                        )
                        saveIncidentEvents(farmPrefs, incidentEvents)
                    }
                    saveFieldIncidents(farmPrefs, fieldIncidents)
                }
                reviewIncident = null
            }
        )
    }

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
                    Text(
                        "AgriVision",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AgriGreen
                    )
                    Text(
                        "Agriculture Operations Platform",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(AgriGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AV", color = AgriGreen, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "Operations Center",
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "From field event to government report.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Capture what is happening on the farm, attach evidence, then prepare it for validation and assistance workflows.",
                        color = Color.White.copy(alpha = .84f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DashboardMiniStat("Incidents", fieldIncidents.size.toString(), Modifier.weight(1f))
                        DashboardMiniStat("Submitted", submittedIncidents.toString(), Modifier.weight(1f))
                        DashboardMiniStat("For review", reviewIncidents.toString(), Modifier.weight(1f))
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
                SectionTitle("Action Queue")
                Text(
                    (fieldIncidents.count { it.status == "Draft" } + openTasks).toString() + " action(s)",
                    color = if (openTasks > 0 || fieldIncidents.any { it.status == "Draft" }) AgriWarning else AgriGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionQueueRow(
                        title = "Field incidents",
                        detail = fieldIncidents.count { it.status == "Draft" }.toString() + " draft report(s)",
                        action = { showIncidentDialog = true }
                    )
                    HorizontalDivider(color = AgriLine)
                    ActionQueueRow(
                        title = "Farm tasks",
                        detail = openTasks.toString() + " open task(s)",
                        action = {}
                    )
                    HorizontalDivider(color = AgriLine)
                    ActionQueueRow(
                        title = "Inventory",
                        detail = if (lowStock == 0) "No low-stock alerts" else lowStock.toString() + " item(s) low",
                        action = {}
                    )
                }
            }
        }

        item {
            CaseTriageCard(
                urgentCases = urgentCases,
                missingEvidence = missingEvidence,
                assistancePending = assistancePending,
                closureReady = closureReady,
                cases = fieldIncidents,
                onReviewCase = { incident ->
                    reviewIncident = incident
                }
            )
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1D6))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Report a field incident", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Pest, disease, flooding, drought, crop damage or animal health events can be logged with date, severity and photo evidence.",
                        color = AgriText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { showIncidentDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("+ New Field Report")
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
                SectionTitle("Recent Field Reports")
                Text(
                    fieldIncidents.size.toString() + " total",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (fieldIncidents.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriCard)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("No field incidents recorded", fontWeight = FontWeight.Bold)
                        Text(
                            "Create the first report when an agricultural issue occurs.",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(fieldIncidents.takeLast(5).asReversed()) { incident ->
                FieldIncidentCard(
                    incident = incident,
                    hasAssistanceRequest = assistance.any { it.incidentId == incident.id },
                    assistanceCompleted = assistance.any {
                        it.incidentId == incident.id && it.status == "Completed"
                    },
                    onStatusChange = { next ->
                        val index = fieldIncidents.indexOfFirst { it.id == incident.id }
                        if (index >= 0) {
                            fieldIncidents[index] = incident.copy(status = next)
                            incidentEvents.add(
                                IncidentEvent(
                                    incidentId = incident.id,
                                    status = next,
                                    note = when (next) {
                                        "Submitted" -> "Farmer submitted field report"
                                        "Resolved" -> "Case closed after closure gate"
                                        else -> "Case status updated"
                                    }
                                )
                            )
                            saveFieldIncidents(farmPrefs, fieldIncidents)
                            saveIncidentEvents(farmPrefs, incidentEvents)
                        }
                    },
                    onReview = { reviewIncident = incident },
                    onTimeline = { timelineIncident = incident },
                    onCreateAssistance = {
                        if (!assistance.any { it.incidentId == incident.id }) {
                            assistance.add(
                                AssistanceRecord(
                                    program = "Field Incident Assistance",
                                    assistanceType = incident.type,
                                    dateReceived = "",
                                    quantity = incident.affectedArea,
                                    status = "Applied",
                                    source = "Linked to " + incident.id,
                                    incidentId = incident.id,
                                    requestId = "DAR-" + System.currentTimeMillis()
                                )
                            )
                            saveAssistance(farmPrefs, assistance)
                            incidentEvents.add(
                                IncidentEvent(
                                    incidentId = incident.id,
                                    status = "Assistance Requested",
                                    note = "Assistance request linked to verified incident"
                                )
                            )
                            saveIncidentEvents(farmPrefs, incidentEvents)
                        }
                    }
                )
            }
        }

        item { SectionTitle("Farm Operations") }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardKpiCard(
                    "Sales",
                    "₱" + money(totalSales),
                    Icons.Outlined.MonetizationOn,
                    AgriGreenSoft,
                    Modifier.weight(1f)
                )
                DashboardKpiCard(
                    "Expenses",
                    "₱" + money(totalExpenses),
                    Icons.Outlined.ReceiptLong,
                    Color(0xFFE9DFC7),
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardKpiCard(
                    "Net Income",
                    "₱" + money(netIncome),
                    Icons.Outlined.Assessment,
                    Color(0xFFE1EEDB),
                    Modifier.weight(1f)
                )
                DashboardKpiCard(
                    "Production",
                    production.size.toString(),
                    Icons.Outlined.LocalFlorist,
                    Color(0xFFECE8D9),
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Farm Health",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (lowStock == 0 && openTasks == 0) "Healthy" else "Needs attention",
                            color = if (lowStock == 0 && openTasks == 0) AgriGreen else AgriWarning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    StatusRow("Livestock", totalAnimals.toString() + " heads", Icons.Outlined.Pets, AgriGreen)
                    StatusRow("Crops", crops.size.toString() + " records", Icons.Outlined.LocalFlorist, AgriGreen)
                    StatusRow(
                        "Inventory",
                        if (lowStock == 0) "All stocked" else lowStock.toString() + " low",
                        Icons.Outlined.Inventory2,
                        if (lowStock > 0) AgriWarning else AgriGreen
                    )
                    StatusRow(
                        "Tasks",
                        completedTasks.toString() + "/" + totalTasks + " completed",
                        Icons.Outlined.Checklist,
                        if (openTasks > 0) AgriWarning else AgriGreen
                    )
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Workflow status", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "FARMER → FIELD EVENT → EVIDENCE → VALIDATION → ASSISTANCE → OUTCOME",
                        color = AgriText,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CaseTriageCard(
    urgentCases: Int,
    missingEvidence: Int,
    assistancePending: Int,
    closureReady: Int,
    cases: List<FieldIncident>,
    onReviewCase: (FieldIncident) -> Unit
) {
    val triageCases = cases
        .filter { it.status != "Resolved" && (it.severity == "Critical" || it.severity == "High" || it.status == "Submitted" || it.status == "Under Review") }
        .take(3)

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Case Triage", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Surface cases that need the next operational action.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    activeTriageLabel(urgentCases, assistancePending),
                    color = if (urgentCases > 0 || assistancePending > 0) AgriWarning else AgriGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriageMetric("Urgent", urgentCases, Modifier.weight(1f))
                TriageMetric("Evidence gaps", missingEvidence, Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TriageMetric("Assistance pending", assistancePending, Modifier.weight(1f))
                TriageMetric("Ready to close", closureReady, Modifier.weight(1f))
            }

            if (triageCases.isEmpty()) {
                Text(
                    "No active cases require triage right now.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                triageCases.forEach { incident ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(incident.id, fontWeight = FontWeight.Bold)
                            Text(
                                incident.type + " · " + incident.severity + " · " + incident.status,
                                color = AgriMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        TextButton(onClick = { onReviewCase(incident) }) {
                            Text(if (incident.status == "Submitted") "Review" else "Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriageMetric(
    label: String,
    value: Int,
    modifier: Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value.toString(), color = AgriGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun activeTriageLabel(
    urgentCases: Int,
    assistancePending: Int
): String {
    val total = urgentCases + assistancePending
    return if (total == 0) "Clear" else total.toString() + " attention"
}

@Composable
private fun ActionQueueRow(
    title: String,
    detail: String,
    action: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = action) {
            Text(if (title == "Field incidents") "Report" else "Open")
        }
    }
}

@Composable
private fun FieldIncidentCard(
    incident: FieldIncident,
    hasAssistanceRequest: Boolean,
    assistanceCompleted: Boolean,
    onStatusChange: (String) -> Unit,
    onReview: () -> Unit,
    onTimeline: () -> Unit,
    onCreateAssistance: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(incident.type, fontWeight = FontWeight.Bold)
                    Text(
                        incident.id,
                        color = AgriGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        listOf(incident.commodity, incident.affectedArea, incident.date)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusBadge(incident.status)
            }

            Text(
                incident.description.ifBlank { "No description provided." },
                color = AgriText,
                style = MaterialTheme.typography.bodySmall
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Severity: " + incident.severity,
                    color = if (incident.severity == "Critical" || incident.severity == "High") AgriWarning else AgriGreen,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (incident.evidenceUri.isNotBlank()) {
                    Text("Photo evidence", color = AgriGreen, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (incident.reviewNotes.isNotBlank()) {
                Text(
                    "Review note: " + incident.reviewNotes,
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTimeline) { Text("Timeline") }
                when (incident.status) {
                    "Draft", "Returned" -> {
                        TextButton(onClick = { onStatusChange("Submitted") }) {
                            Text("Submit Report")
                        }
                    }
                    "Submitted" -> {
                        TextButton(onClick = onReview) {
                            Text("Start Review")
                        }
                    }
                    "Under Review" -> {
                        TextButton(onClick = onReview) {
                            Text("Review")
                        }
                    }
                    "Verified" -> {
                        if (!hasAssistanceRequest) {
                            TextButton(onClick = onCreateAssistance) {
                                Text("Request Assistance")
                            }
                        } else if (assistanceCompleted) {
                            Text("Assistance completed", color = AgriGreen, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(
                                "Awaiting assistance completion",
                                color = AgriWarning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!hasAssistanceRequest || assistanceCompleted) {
                            TextButton(onClick = { onStatusChange("Resolved") }) {
                                Text("Mark Resolved")
                            }
                        }
                    }
                    "Resolved" -> {
                        Text("Case closed", color = AgriGreen, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentTimelineDialog(
    incident: FieldIncident,
    events: List<IncidentEvent>,
    onDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Case Timeline", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(incident.id, color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        incident.type + " · " + incident.commodity,
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (events.isEmpty()) {
                    item { Text("No events recorded yet.", color = AgriMuted) }
                } else {
                    items(events) { event ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(
                                    if (event.status == "Verified" || event.status == "Resolved") AgriGreen else AgriGreenSoft
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (event.status == "Verified" || event.status == "Resolved") "✓" else "•",
                                    color = if (event.status == "Verified" || event.status == "Resolved") Color.White else AgriGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(event.status, fontWeight = FontWeight.Bold)
                                Text(
                                    formatter.format(java.util.Date(event.timestamp)),
                                    color = AgriMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (event.note.isNotBlank()) {
                                    Text(event.note, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun IncidentReviewDialog(
    incident: FieldIncident,
    onDismiss: () -> Unit,
    onSave: (FieldIncident) -> Unit
) {
    var status by remember(incident.id) {
        mutableStateOf(
            when (incident.status) {
                "Submitted" -> "Under Review"
                else -> incident.status
            }
        )
    }
    var notes by remember(incident.id) { mutableStateOf(incident.reviewNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incident Review", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 390.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(incident.id, color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        incident.type + " · " + incident.commodity,
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text("Validation status", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Under Review", "Verified", "Returned").forEach { option ->
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
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reviewer notes") },
                        minLines = 3
                    )
                }
                item {
                    Text(
                        "Verified reports can be converted into a linked assistance request.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        incident.copy(
                            status = status,
                            reviewNotes = notes.trim()
                        )
                    )
                }
            ) { Text("Save Review") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddFieldIncidentDialog(
    onDismiss: () -> Unit,
    onSave: (FieldIncident) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Pest / Disease") }
    var commodity by remember { mutableStateOf("") }
    var affectedArea by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var severity by remember { mutableStateOf("Moderate") }
    var description by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        evidenceUri = uri?.toString().orEmpty()
    }

    if (showPicker) {
        val calendar = Calendar.getInstance()
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time
        } catch (_: Exception) { }
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
        title = { Text("New Field Incident", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Issue type", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Pest / Disease", "Flood", "Drought", "Crop Damage", "Animal Health", "Other").forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        commodity,
                        { commodity = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Commodity / crop / animal") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        affectedArea,
                        { affectedArea = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Affected area / quantity") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Date: " + date)
                    }
                }
                item {
                    Text("Severity", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Low", "Moderate", "High", "Critical").forEach { option ->
                            FilterChip(
                                selected = severity == option,
                                onClick = { severity = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        description,
                        { description = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("What happened?") },
                        minLines = 3
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (evidenceUri.isBlank()) "Attach photo evidence" else "Photo evidence attached")
                    }
                }
                item {
                    Text(
                        "The photo is stored as a local file reference. Official DA submission still requires a connected and authorized backend.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = commodity.isNotBlank() && description.isNotBlank(),
                onClick = {
                    onSave(
                        FieldIncident(
                            id = "INC-" + System.currentTimeMillis(),
                            type = type,
                            commodity = commodity.trim(),
                            affectedArea = affectedArea.trim(),
                            date = date,
                            severity = severity,
                            description = description.trim(),
                            status = "Draft",
                            evidenceUri = evidenceUri
                        )
                    )
                }
            ) { Text("Save Draft") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
    fieldIncidents: List<FieldIncident>,
    incidentEvents: List<IncidentEvent>,
    onExportCasePackage: () -> Unit,
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
            item { RegistryReviewCard(farmerProfile.registryStatus, farmerProfile.reviewNotes) }
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
            item {
                val verifiedCases = fieldIncidents.count { it.status == "Verified" || it.status == "Resolved" }
                val hasRegistry = farmerProfile.farmerName.isNotBlank() &&
                        farmerProfile.farmerId.isNotBlank() &&
                        farmerProfile.farmName.isNotBlank() &&
                        listOf(farmerProfile.barangay, farmerProfile.municipality, farmerProfile.province).all { it.isNotBlank() }
                CasePackageCard(
                    verifiedCases = verifiedCases,
                    assistanceCount = assistance.size,
                    auditEventCount = incidentEvents.size,
                    ready = hasRegistry && verifiedCases > 0,
                    onExport = onExportCasePackage
                )
            }
            item { SummaryCard("Assistance Received", assistance.size.toString() + " records", "Seeds, fertilizer, livestock, equipment and other agricultural support.") }
            item { SectionTitle("Assistance Records") }

            item { SectionTitle("DA Data Quality") }
            item {
                val checks = listOf(
                    "Farmer name" to farmerProfile.farmerName.isNotBlank(),
                    "Farmer ID / reference" to farmerProfile.farmerId.isNotBlank(),
                    "Farm name" to farmerProfile.farmName.isNotBlank(),
                    "Farm location" to listOf(farmerProfile.barangay, farmerProfile.municipality, farmerProfile.province).all { it.isNotBlank() },
                    "Farm size" to farmerProfile.farmSize.isNotBlank(),
                    "Commodities" to farmerProfile.commodities.isNotBlank(),
                    "Agricultural records" to (totalAnimals > 0 || production.isNotEmpty() || assistance.isNotEmpty())
                )
                DataQualityCard(checks)
            }
            item { SectionTitle("DA Submission Gate") }
            item {
                val checks = listOf(
                    farmerProfile.farmerName.isNotBlank(),
                    farmerProfile.farmerId.isNotBlank(),
                    farmerProfile.farmName.isNotBlank(),
                    listOf(farmerProfile.barangay, farmerProfile.municipality, farmerProfile.province).all { it.isNotBlank() },
                    farmerProfile.farmSize.isNotBlank(),
                    farmerProfile.commodities.isNotBlank(),
                    totalAnimals > 0 || production.isNotEmpty() || assistance.isNotEmpty()
                )
                val ready = checks.all { it }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (ready) AgriGreenSoft else Color(0xFFFFF1D6))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (ready) "Report can be marked Ready for Submission" else "Complete the missing registry data first", fontWeight = FontWeight.Bold)
                        Text(if (ready) "All required data checks passed." else "Open Farmer & Farm Registry and complete the fields marked as missing.", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
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
private fun CasePackageCard(
    verifiedCases: Int,
    assistanceCount: Int,
    auditEventCount: Int,
    ready: Boolean,
    onExport: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ready) AgriGreenSoft else Color(0xFFFFF1D6)
        )
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("DA Case Package", color = AgriGreen, fontWeight = FontWeight.Bold)
            Text(
                "Create a structured offline package containing registry, verified field cases, assistance records and audit history.",
                color = AgriText,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportStatCard("Verified cases", verifiedCases.toString(), "", Modifier.weight(1f))
                ReportStatCard("Assistance", assistanceCount.toString(), "", Modifier.weight(1f))
                ReportStatCard("Audit events", auditEventCount.toString(), "", Modifier.weight(1f))
            }
            Text(
                if (ready)
                    "Package is ready for export."
                else
                    "Add complete registry information and at least one verified field case first.",
                color = if (ready) AgriGreen else AgriWarning,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onExport,
                enabled = ready,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Export DA Case Package")
            }
            Text(
                "Export creates a local JSON package only; it does not transmit data to the DA.",
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DataQualityCard(checks: List<Pair<String, Boolean>>) {
    val complete = checks.count { it.second }
    val missing = checks.filterNot { it.second }.map { it.first }
    val ready = missing.isEmpty()
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (ready) AgriGreenSoft else Color(0xFFFFF1D6))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Report readiness", fontWeight = FontWeight.Bold)
                    Text(complete.toString() + "/" + checks.size + " required areas complete", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (ready) "READY" else "NEEDS INFO", color = if (ready) AgriGreen else AgriWarning, fontWeight = FontWeight.Bold)
                
            }
            checks.forEach { (label, ok) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (ok) "✓" else "!", color = if (ok) AgriGreen else AgriWarning, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(label, modifier = Modifier.weight(1f))
                    Text(if (ok) "Complete" else "Missing", color = if (ok) AgriGreen else AgriWarning, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!ready) {
                Text("Complete the missing fields in Farmer & Farm Registry before marking the report ready for submission.", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
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
private fun RegistryReviewCard(status: String, notes: String) {
    val color = when (status) {
        "Verified" -> AgriGreen
        "Returned" -> AgriWarning
        else -> AgriMuted
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("DA Registry Review", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", color = AgriMuted, modifier = Modifier.weight(1f))
                Text(status, color = color, fontWeight = FontWeight.Bold)
            }
            Text(if (notes.isBlank()) "No review notes recorded." else "Review notes: " + notes, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
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
    farmPrefs: android.content.SharedPreferences,
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
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("DA Farmer Registry", color = AgriGreen, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Registry status: " + if (profile.farmerName.isNotBlank() && profile.farmerId.isNotBlank()) "Registered" else "Incomplete", color = AgriText, fontWeight = FontWeight.SemiBold)
                    Text("Use this record as the farmer identity reference for future DA workflows.", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
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
                farmPrefs = farmPrefs,
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
    assistance: MutableList<AssistanceRecord>,
    farmPrefs: android.content.SharedPreferences,
    onAdd: (AssistanceRecord) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var manageRecord by remember { mutableStateOf<AssistanceRecord?>(null) }
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

    manageRecord?.let { record ->
        AssistanceWorkflowDialog(
            record = record,
            onDismiss = { manageRecord = null },
            onSave = { updated ->
                val index = assistance.indexOfFirst {
                    it.requestId == record.requestId && record.requestId.isNotBlank()
                            || it == record
                }
                if (index >= 0) {
                    assistance[index] = updated
                    saveAssistance(farmPrefs, assistance)
                }
                manageRecord = null
            }
        )
    }

    val statuses = listOf("All", "Applied", "Approved", "Distributed", "Completed")
    val filtered = if (filter == "All") assistance
    else assistance.filter {
        val normalized = if (it.status == "Received") "Distributed" else it.status
        normalized.equals(filter, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("DA Assistance Management", fontWeight = FontWeight.Bold, color = AgriGreen)
                        Text(
                            "Track an intervention from application through distribution and outcome.",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(onClick = { showAdd = true }, shape = RoundedCornerShape(14.dp)) {
                        Text("+ Add")
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
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
                if (assistance.isEmpty()) "No assistance requests" else "No records in $filter",
                if (assistance.isEmpty())
                    "Create an intervention record or link one from a verified field incident."
                else
                    "There are no assistance records with this workflow status."
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
                                if (record.requestId.isNotBlank()) {
                                    Text(
                                        record.requestId,
                                        color = AgriMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            StatusBadge(if (record.status == "Received") "Distributed" else record.status)
                        }

                        Text(
                            listOf(record.dateReceived, record.quantity)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (record.incidentId.isNotBlank()) {
                            Text("Linked incident: " + record.incidentId, color = AgriGreen, style = MaterialTheme.typography.bodySmall)
                        }
                        if (record.distributionDetails.isNotBlank()) {
                            Text("Distribution: " + record.distributionDetails, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (record.outcome.isNotBlank()) {
                            Text("Outcome: " + record.outcome, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (record.source.isNotBlank()) {
                            Text("Source / office: " + record.source, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { manageRecord = record }) {
                                Text("Manage Workflow")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistanceWorkflowDialog(
    record: AssistanceRecord,
    onDismiss: () -> Unit,
    onSave: (AssistanceRecord) -> Unit
) {
    val context = LocalContext.current
    var status by remember(record.requestId) {
        mutableStateOf(if (record.status == "Received") "Distributed" else record.status.ifBlank { "Applied" })
    }
    var requestedDate by remember(record.requestId) { mutableStateOf(record.dateReceived) }
    var approvedDate by remember(record.requestId) { mutableStateOf(record.approvedDate) }
    var distributedDate by remember(record.requestId) { mutableStateOf(record.distributedDate) }
    var completedDate by remember(record.requestId) { mutableStateOf(record.completedDate) }
    var details by remember(record.requestId) { mutableStateOf(record.distributionDetails) }
    var outcome by remember(record.requestId) { mutableStateOf(record.outcome) }
    var notes by remember(record.requestId) { mutableStateOf(record.reviewNotes) }
    var pickerTarget by remember { mutableStateOf("") }

    if (pickerTarget.isNotBlank()) {
        val calendar = Calendar.getInstance()
        val current = when (pickerTarget) {
            "requested" -> requestedDate
            "approved" -> approvedDate
            "distributed" -> distributedDate
            else -> completedDate
        }
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current) ?: calendar.time
        } catch (_: Exception) { }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                when (pickerTarget) {
                    "requested" -> requestedDate = value
                    "approved" -> approvedDate = value
                    "distributed" -> distributedDate = value
                    "completed" -> completedDate = value
                }
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
        title = { Text("Assistance Workflow", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    Text(
                        record.requestId.ifBlank { "Assistance record" },
                        color = AgriGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        record.program + " · " + record.assistanceType,
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text("Workflow status", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Applied", "Approved", "Distributed", "Completed").forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item { OutlinedButton(onClick = { pickerTarget = "requested" }, Modifier.fillMaxWidth()) { Text("Applied date: " + requestedDate.ifBlank { "Set date" }) } }
                item { OutlinedButton(onClick = { pickerTarget = "approved" }, Modifier.fillMaxWidth()) { Text("Approved date: " + approvedDate.ifBlank { "Set date" }) } }
                item { OutlinedButton(onClick = { pickerTarget = "distributed" }, Modifier.fillMaxWidth()) { Text("Distributed date: " + distributedDate.ifBlank { "Set date" }) } }
                item { OutlinedButton(onClick = { pickerTarget = "completed" }, Modifier.fillMaxWidth()) { Text("Completed date: " + completedDate.ifBlank { "Set date" }) } }
                item {
                    OutlinedTextField(
                        details,
                        { details = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Distribution details") },
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        outcome,
                        { outcome = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Intervention outcome") },
                        minLines = 2
                    )
                }
                item {
                    OutlinedTextField(
                        notes,
                        { notes = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Management notes") },
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
                    val approved = if (status == "Approved" && approvedDate.isBlank()) today else approvedDate.trim()
                    val distributed = if (status == "Distributed" && distributedDate.isBlank()) today else distributedDate.trim()
                    val completed = if (status == "Completed" && completedDate.isBlank()) today else completedDate.trim()

                    onSave(
                        record.copy(
                            status = status,
                            dateReceived = requestedDate.trim(),
                            approvedDate = approved,
                            distributedDate = distributed,
                            completedDate = completed,
                            distributionDetails = details.trim(),
                            outcome = outcome.trim(),
                            reviewNotes = notes.trim()
                        )
                    )
                }
            ) { Text("Save Workflow") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
                    onSave(
                        AssistanceRecord(
                            program = program.trim(),
                            assistanceType = type.trim(),
                            dateReceived = date,
                            quantity = quantity.trim(),
                            status = status.trim(),
                            source = source.trim(),
                            requestId = "DAR-" + System.currentTimeMillis()
                        )
                    )
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

private fun exportDaCasePackage(
    context: Context,
    uri: Uri,
    prefs: android.content.SharedPreferences
) {
    val packageJson = JSONObject().apply {
        put("packageVersion", 1)
        put("app", "AgriVision")
        put("packageType", "DA Case Package")
        put("generatedAt", System.currentTimeMillis())

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
            put("registryStatus", prefs.getString("registryStatus", "For Review"))
            put("reviewNotes", prefs.getString("reviewNotes", ""))
        })

        put("fieldIncidents", JSONArray(prefs.getString("fieldIncidents", "[]")))
        put("incidentEvents", JSONArray(prefs.getString("incidentEvents", "[]")))
        put("assistance", JSONArray(prefs.getString("assistance", "[]")))

        put("operationalRecords", JSONObject().apply {
            put("livestock", JSONArray(prefs.getString("livestock", "[]")))
            put("crops", JSONArray(prefs.getString("crops", "[]")))
            put("production", JSONArray(prefs.getString("production", "[]")))
        })
    }

    context.contentResolver.openOutputStream(uri)?.use { output ->
        output.write(packageJson.toString(2).toByteArray(Charsets.UTF_8))
    }
}

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
        put("fieldIncidents", JSONArray(prefs.getString("fieldIncidents", "[]")))
        put("incidentEvents", JSONArray(prefs.getString("incidentEvents", "[]")))
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
            put("registryStatus", prefs.getString("registryStatus", "For Review"))
            put("reviewNotes", prefs.getString("reviewNotes", ""))
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
    val keys = listOf("livestock", "crops", "inventory", "equipment", "production", "expenses", "sales", "tasks", "assistance", "fieldIncidents", "incidentEvents")
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
        edit.putString("registryStatus", p.optString("registryStatus", "For Review"))
        edit.putString("reviewNotes", p.optString("reviewNotes"))
    }
    edit.apply()
}

private fun loadIncidentEvents(prefs: android.content.SharedPreferences): List<IncidentEvent> {
    val raw = prefs.getString("incidentEvents", "[]") ?: "[]"
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        IncidentEvent(
            incidentId = o.optString("incidentId"),
            status = o.optString("status"),
            note = o.optString("note"),
            timestamp = o.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

private fun saveIncidentEvents(
    prefs: android.content.SharedPreferences,
    list: List<IncidentEvent>
) {
    val a = JSONArray()
    list.forEach { event ->
        a.put(JSONObject().apply {
            put("incidentId", event.incidentId)
            put("status", event.status)
            put("note", event.note)
            put("timestamp", event.timestamp)
        })
    }
    prefs.edit().putString("incidentEvents", a.toString()).apply()
}

private fun loadFieldIncidents(prefs: android.content.SharedPreferences): List<FieldIncident> {
    val raw = prefs.getString("fieldIncidents", "[]") ?: "[]"
    val a = JSONArray(raw)
    return List(a.length()) { i ->
        val o = a.getJSONObject(i)
        FieldIncident(
            id = o.optString("id"),
            type = o.optString("type", "Other"),
            commodity = o.optString("commodity"),
            affectedArea = o.optString("affectedArea"),
            date = o.optString("date"),
            severity = o.optString("severity", "Moderate"),
            description = o.optString("description"),
            status = o.optString("status", "Draft"),
            evidenceUri = o.optString("evidenceUri"),
            reviewNotes = o.optString("reviewNotes")
        )
    }
}

private fun saveFieldIncidents(prefs: android.content.SharedPreferences, list: List<FieldIncident>) {
    val a = JSONArray()
    list.forEach { incident ->
        a.put(JSONObject().apply {
            put("id", incident.id)
            put("type", incident.type)
            put("commodity", incident.commodity)
            put("affectedArea", incident.affectedArea)
            put("date", incident.date)
            put("severity", incident.severity)
            put("description", incident.description)
            put("status", incident.status)
            put("evidenceUri", incident.evidenceUri)
            put("reviewNotes", incident.reviewNotes)
        })
    }
    prefs.edit().putString("fieldIncidents", a.toString()).apply()
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
            o.optString("source"),
            o.optString("incidentId"),
            o.optString("requestId"),
            o.optString("approvedDate"),
            o.optString("distributedDate"),
            o.optString("completedDate"),
            o.optString("distributionDetails"),
            o.optString("outcome"),
            o.optString("reviewNotes")
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
            put("incidentId", it.incidentId)
            put("requestId", it.requestId)
            put("approvedDate", it.approvedDate)
            put("distributedDate", it.distributedDate)
            put("completedDate", it.completedDate)
            put("distributionDetails", it.distributionDetails)
            put("outcome", it.outcome)
            put("reviewNotes", it.reviewNotes)
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
        prefs.getString("commodities", "") ?: "",
        prefs.getString("registryStatus", "For Review") ?: "For Review",
        prefs.getString("reviewNotes", "") ?: ""
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
        .putString("registryStatus", profile.registryStatus)
        .putString("reviewNotes", profile.reviewNotes)
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
