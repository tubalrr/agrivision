package com.tubalrr.agrivision

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
internal fun DashboardScreen(
    padding: PaddingValues,
    livestock: List<Livestock>,
    crops: List<CropRecord>,
    production: List<ProductionRecord>,
    expenses: List<ExpenseRecord>,
    inventory: List<InventoryItem>,
    tasks: List<FarmTask>,
    fieldIncidents: List<FieldIncident>,
    incidentEvents: List<IncidentEvent>,
    assistance: List<AssistanceRecord>,
    farmer: com.tubalrr.agrivision.domain.model.FarmerRecord,
    farm: com.tubalrr.agrivision.domain.model.FarmRecord,
    fields: List<com.tubalrr.agrivision.domain.model.FieldRecord>,
    farmViewModel: FarmViewModel,
    onOpenTasks: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
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
    val activeIncidents = fieldIncidents.filter { it.status != "Completed" }
    val attentionReports = fieldIncidents.count { it.status != "Completed" && it.status != "Draft" }
    val queueCount = fieldIncidents.count { it.status == "Draft" } + openTasks + lowStock
    val urgentCases = activeIncidents.count {
        it.severity == "Critical" || it.severity == "High"
    }
    val missingEvidence = activeIncidents.count {
        it.evidenceUri.isBlank() && it.status != "Draft"
    }
    val assistancePending = activeIncidents.count { incident ->
        incident.status == "Assistance" &&
                assistance.none {
                    it.incidentId == incident.id && it.status == "Completed"
                }
    }
    val closureReady = fieldIncidents.count { incident ->
        incident.status == "Assistance" &&
                assistance.any {
                    it.incidentId == incident.id && it.status == "Completed"
                }
    }
    var showIncidentDialog by remember { mutableStateOf(false) }
    var reviewIncident by remember { mutableStateOf<FieldIncident?>(null) }
    var timelineIncident by remember { mutableStateOf<FieldIncident?>(null) }
    var resolutionIncident by remember { mutableStateOf<FieldIncident?>(null) }

    if (showIncidentDialog) {
        AddFieldIncidentDialog(
            farmerId = farmer.farmerId,
            farmerName = farmer.fullName,
            farmId = farm.farmId,
            farmName = farm.farmName,
            fields = fields,
            onDismiss = { showIncidentDialog = false },
            onSave = { incident ->
                farmViewModel.addFieldIncident(incident)
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
                farmViewModel.reviewFieldIncident(incident, updated)
                reviewIncident = null
            }
        )
    }
 
    resolutionIncident?.let { incident ->
        IncidentResolutionDialog(
            incident = incident,
            onDismiss = { resolutionIncident = null },
            onSave = { resolution, reviewer ->
                farmViewModel.completeFieldIncident(incident, resolution, reviewer)
                resolutionIncident = null
            }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "GOOD DAY, FARMER",
                        color = AgriGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        farm.farmName.ifBlank { "Your Farm" },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = AgriText
                    )
                    Text("Your farm, all in one view", color = AgriMuted)
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(AgriGreenSoft)
                        .border(1.dp, AgriLine, RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Yard,
                        contentDescription = "AgriVision",
                        tint = AgriGreen,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().border(1.dp, AgriGreen.copy(alpha = .18f), RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "FARM OPERATIONS",
                        color = AgriGold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (attentionReports == 0) "Your farm is ready for the next activity."
                        else "$attentionReports active field report(s) need attention.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Crops, livestock, incidents, finances and DA-ready records.",
                        color = Color.White.copy(alpha = .78f)
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Fields" to fields.size.toString(),
                            "Livestock" to totalAnimals.toString(),
                            "Open tasks" to openTasks.toString()
                        ).forEach { (label, value) ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = .08f))
                                    .padding(horizontal = 10.dp, vertical = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    value,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    label,
                                    color = Color.White.copy(alpha = .76f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
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
                    "$queueCount action(s)",
                    color = if (queueCount > 0) AgriWarning else AgriGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        action = onOpenTasks
                    )
                    HorizontalDivider(color = AgriLine)
                    ActionQueueRow(
                        title = "Inventory",
                        detail = if (lowStock == 0) "No low-stock alerts" else lowStock.toString() + " item(s) low",
                        action = onOpenInventory
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
                        shape = RoundedCornerShape(12.dp)
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
                        farmViewModel.updateFieldIncident(
                            incident.copy(status = next),
                            previousStatus = incident.status
                        )
                    },
                    onReview = { reviewIncident = incident },
                    onTimeline = { timelineIncident = incident },
                    onCreateAssistance = {
                        if (incident.status == "Assistance" && !assistance.any { it.incidentId == incident.id }) {
                            farmViewModel.requestAssistance(incident)
                        }
                    },
                    onResolve = { resolutionIncident = incident }
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
internal fun CaseTriageCard(
    urgentCases: Int,
    missingEvidence: Int,
    assistancePending: Int,
    closureReady: Int,
    cases: List<FieldIncident>,
    onReviewCase: (FieldIncident) -> Unit
) {
    val triageCases = cases
        .filter { it.status != "Completed" && (it.severity == "Critical" || it.severity == "High" || it.status == "Submitted" || it.status == "Under Review") }
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
internal fun TriageMetric(
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

internal fun activeTriageLabel(
    urgentCases: Int,
    assistancePending: Int
): String {
    val total = urgentCases + assistancePending
    return if (total == 0) "Clear" else total.toString() + " attention"
}

@Composable
internal fun ActionQueueRow(
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
internal fun DashboardKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    modifier: Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Card(
        modifier.border(1.dp, AgriLine, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                color = AgriText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    modifier: Modifier
) {
    Card(
        modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(9.dp))
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = AgriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
