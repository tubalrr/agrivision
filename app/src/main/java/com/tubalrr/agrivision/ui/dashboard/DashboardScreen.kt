package com.tubalrr.agrivision

import androidx.compose.ui.unit.sp

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.foundation.clickable

import androidx.compose.foundation.Image

import androidx.compose.foundation.Canvas

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.FastOutSlowInEasing

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
import java.text.ParsePosition
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*


@Composable
internal fun DashboardScreen(
    padding: PaddingValues,
    livestock: List<Livestock>,
    crops: List<CropRecord>,
    production: List<ProductionRecord>,
    sales: List<SaleRecord>,
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_agrivision_logo),
                        contentDescription = "AgriVision logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .border(1.dp, AgriLine, RoundedCornerShape(17.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "GOOD DAY, FARMER",
                            color = AgriGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            farm.farmName.ifBlank { "Your Farm" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AgriText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Calendar.getInstance().time),
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (queueCount > 0) Color(0xFFFFE6D9) else AgriGreenSoft)
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            if (queueCount > 0) Icons.Outlined.NotificationsActive else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (queueCount > 0) AgriDanger else AgriGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (queueCount > 0) "$queueCount alerts" else "All clear",
                            color = if (queueCount > 0) AgriDanger else AgriGreen,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            val heroTransition = rememberInfiniteTransition(label = "farm-hero")
            val sunScale by heroTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1700, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "hero-sun-scale"
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(298.dp)
                    .border(1.dp, AgriGreen.copy(alpha = 0.18f), RoundedCornerShape(30.dp)),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF174A32), Color(0xFF0D2F20), Color(0xFF102E22))
                            )
                        )
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val sunCenter = Offset(w * 0.83f, h * 0.27f)
                        drawCircle(Color(0xFFFFD66B).copy(alpha = 0.12f), w * 0.16f, sunCenter)
                        drawCircle(Color(0xFFFFD66B), w * 0.067f * sunScale, sunCenter)

                        val distantHills = Path().apply {
                            moveTo(0f, h * 0.65f)
                            cubicTo(w * 0.18f, h * 0.48f, w * 0.34f, h * 0.61f, w * 0.48f, h * 0.56f)
                            cubicTo(w * 0.68f, h * 0.43f, w * 0.82f, h * 0.62f, w, h * 0.49f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(distantHills, Color(0xFF35684A).copy(alpha = 0.8f))

                        val fieldBase = Path().apply {
                            moveTo(0f, h * 0.76f)
                            cubicTo(w * 0.35f, h * 0.65f, w * 0.62f, h * 0.72f, w, h * 0.61f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(fieldBase, Color(0xFF1B5837))

                        val fieldGlow = Path().apply {
                            moveTo(w * 0.28f, h)
                            cubicTo(w * 0.48f, h * 0.77f, w * 0.76f, h * 0.74f, w, h * 0.70f)
                            lineTo(w, h * 0.76f)
                            cubicTo(w * 0.74f, h * 0.83f, w * 0.49f, h * 0.87f, w * 0.37f, h)
                            close()
                        }
                        drawPath(fieldGlow, Color(0xFF2C7442))
                        for (i in 0..4) {
                            val t = i / 4f
                            val line = Path().apply {
                                moveTo(w * (0.30f - t * 0.15f), h * (0.79f + t * 0.045f))
                                cubicTo(
                                    w * (0.50f - t * 0.10f), h * (0.72f + t * 0.06f),
                                    w * (0.72f + t * 0.07f), h * (0.79f + t * 0.035f),
                                    w, h * (0.68f + t * 0.06f)
                                )
                            }
                            drawPath(
                                path = line,
                                color = if (i % 2 == 0) Color(0xFF91C95B).copy(alpha = 0.85f) else Color(0xFFD6B85D).copy(alpha = 0.68f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(21.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Spa, null, tint = Color(0xFFBDEB7A), modifier = Modifier.size(14.dp))
                                    Text(
                                        "FARM OVERVIEW",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.0.sp
                                    )
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Outlined.WbSunny,
                                contentDescription = "Sunrise",
                                tint = Color(0xFFFFD66B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            if (attentionReports == 0) "Grow with\nconfidence." else "$attentionReports field report(s) need attention.",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 31.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.82f)
                        )
                        Text(
                            "Your farm activity, all in one clear view.",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("Fields", fields.size.toString(), Icons.Outlined.Map),
                                Triple("Livestock", totalAnimals.toString(), Icons.Outlined.Pets),
                                Triple("Open tasks", openTasks.toString(), Icons.Outlined.Checklist)
                            ).forEach { (label, value, icon) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF082719).copy(alpha = 0.68f))
                                        .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color(0xFFBDEB7A), modifier = Modifier.size(17.dp))
                                    Text(
                                        value,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        label,
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Quick Actions")
                    Text("Jump right in", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DashboardQuickAction(
                        title = "Report",
                        subtitle = "Field issue",
                        icon = Icons.Outlined.AddCircleOutline,
                        accent = Color(0xFFFFE8D8),
                        iconTint = Color(0xFFB45C2D),
                        modifier = Modifier.weight(1f),
                        onClick = { showIncidentDialog = true }
                    )
                    DashboardQuickAction(
                        title = "Tasks",
                        subtitle = "$openTasks open",
                        icon = Icons.Outlined.Checklist,
                        accent = Color(0xFFDFF1E3),
                        iconTint = AgriGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenTasks
                    )
                    DashboardQuickAction(
                        title = "Stock",
                        subtitle = if (lowStock == 0) "All stocked" else "$lowStock low",
                        icon = Icons.Outlined.Inventory2,
                        accent = Color(0xFFFFF0C7),
                        iconTint = Color(0xFF96701B),
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInventory
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
            SectionTitle("Farm Analytics")
        }

        item {
            CashFlowAnalyticsCard(
                sales = sales,
                expenses = expenses
            )
        }

        item {
            ProductionTrendCard(production = production)
        }

        item {
            FarmAchievementsCard(
                achievements = buildFarmAchievements(
                    fields = fields,
                    crops = crops,
                    livestock = livestock,
                    production = production,
                    tasks = tasks,
                    sales = sales,
                    expenses = expenses,
                    inventory = inventory,
                    incidents = fieldIncidents,
                    assistance = assistance
                )
            )
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

private data class DashboardMonth(
    val key: String,
    val label: String,
    val sales: Double,
    val expenses: Double,
    val productionCount: Int
)

private fun parseDashboardDate(value: String): Date? {
    val cleaned = value.trim()
    if (cleaned.isBlank()) return null

    val calendar = Calendar.getInstance()
    when (cleaned.lowercase(Locale.US)) {
        "today" -> return calendar.time
        "yesterday" -> {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            return calendar.time
        }
        "tomorrow" -> {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            return calendar.time
        }
    }

    val patterns = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "M/d/yyyy",
        "dd/MM/yyyy",
        "d/M/yyyy",
        "MMM d, yyyy",
        "MMMM d, yyyy",
        "d MMM yyyy",
        "MMM yyyy",
        "MMMM yyyy"
    )
    for (pattern in patterns) {
        val formatter = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val parsed = formatter.parse(cleaned, position)
        if (parsed != null && position.index == cleaned.length) return parsed
    }
    return null
}

private fun dashboardMonths(
    sales: List<SaleRecord>,
    expenses: List<ExpenseRecord>,
    production: List<ProductionRecord>
): List<DashboardMonth> {
    val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val labelFormat = SimpleDateFormat("MMM", Locale.US)
    val datedSales = sales.mapNotNull { record ->
        parseDashboardDate(record.date)?.let { keyFormat.format(it) to record }
    }
    val datedExpenses = expenses.mapNotNull { record ->
        parseDashboardDate(record.date)?.let { keyFormat.format(it) to record }
    }
    val datedProduction = production.mapNotNull { record ->
        parseDashboardDate(record.period)?.let { keyFormat.format(it) to record }
    }
    val current = Calendar.getInstance()
    return (5 downTo 0).map { offset ->
        val month = Calendar.getInstance().apply {
            set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -offset)
        }
        val date = month.time
        val key = keyFormat.format(date)
        DashboardMonth(
            key = key,
            label = labelFormat.format(date),
            sales = datedSales.filter { it.first == key }.sumOf { it.second.amount },
            expenses = datedExpenses.filter { it.first == key }.sumOf { it.second.amount },
            productionCount = datedProduction.count { it.first == key }
        )
    }
}

@Composable
private fun CashFlowAnalyticsCard(
    sales: List<SaleRecord>,
    expenses: List<ExpenseRecord>
) {
    val months = remember(sales, expenses) { dashboardMonths(sales, expenses, emptyList()) }
    val datedSalesCount = remember(sales) { sales.count { parseDashboardDate(it.date) != null } }
    val datedExpensesCount = remember(expenses) { expenses.count { parseDashboardDate(it.date) != null } }
    val hasDatedFinance = datedSalesCount + datedExpensesCount > 0
    val currentMonth = months.lastOrNull()
    val maxValue = (months.maxOfOrNull { maxOf(it.sales, it.expenses) } ?: 0.0).coerceAtLeast(1.0)
    val monthSales = currentMonth?.sales ?: 0.0
    val monthExpenses = currentMonth?.expenses ?: 0.0
    val net = monthSales - monthExpenses

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Cash flow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Monthly sales vs expenses · last 6 months", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = AgriGreenSoft, shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = AgriGreen,
                        modifier = Modifier.padding(10.dp).size(21.dp)
                    )
                }
            }

            if (!hasDatedFinance) {
                Column(
                    Modifier.fillMaxWidth().background(AgriGreenSoft, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Your trend starts with dated records", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Add sales and expense entries with dates to populate the six-month chart. No sample figures are shown.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Canvas(Modifier.fillMaxWidth().height(142.dp)) {
                    val chartHeight = size.height - 6.dp.toPx()
                    val step = size.width / months.size
                    val barWidth = (step * .23f).coerceAtLeast(4.dp.toPx())
                    val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    for (grid in 0..4) {
                        val y = chartHeight * grid / 4f
                        drawLine(
                            color = AgriLine,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    months.forEachIndexed { index, month ->
                        val groupCenter = step * (index + .5f)
                        val salesHeight = ((month.sales / maxValue).toFloat() * (chartHeight - 4.dp.toPx())).coerceAtLeast(0f)
                        val expenseHeight = ((month.expenses / maxValue).toFloat() * (chartHeight - 4.dp.toPx())).coerceAtLeast(0f)
                        drawRoundRect(
                            color = AgriGreen,
                            topLeft = Offset(groupCenter - barWidth - 2.dp.toPx(), chartHeight - salesHeight),
                            size = Size(barWidth, salesHeight),
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            color = AgriDanger.copy(alpha = .82f),
                            topLeft = Offset(groupCenter + 2.dp.toPx(), chartHeight - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = radius
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    months.forEach { month ->
                        Text(
                            month.label,
                            modifier = Modifier.weight(1f),
                            color = AgriMuted,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DashboardChartLegend(color = AgriGreen, label = "Sales")
                    DashboardChartLegend(color = AgriDanger, label = "Expenses")
                }
                HorizontalDivider(color = AgriLine)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardAnalyticsMetric("This month · Sales", "₱" + money(monthSales), Modifier.weight(1f))
                    DashboardAnalyticsMetric("This month · Expenses", "₱" + money(monthExpenses), Modifier.weight(1f))
                }
                Surface(
                    color = if (net >= 0) AgriGreenSoft else Color(0xFFFFE8E3),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (net >= 0) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = if (net >= 0) AgriGreen else AgriDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Dated net cash flow", style = MaterialTheme.typography.labelSmall, color = AgriMuted)
                            Text(
                                (if (net < 0) "−₱" else "₱") + money(kotlin.math.abs(net)),
                                color = if (net >= 0) AgriGreen else AgriDanger,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionTrendCard(production: List<ProductionRecord>) {
    val months = remember(production) { dashboardMonths(emptyList(), emptyList(), production) }
    val datedCount = remember(production) { production.count { parseDashboardDate(it.period) != null } }
    val recentCount = months.sumOf { it.productionCount }
    val peak = (months.maxOfOrNull { it.productionCount } ?: 0).coerceAtLeast(1)

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Production activity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Dated production logs · last 6 months", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = AgriGoldSoft, shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        Icons.Outlined.Eco,
                        contentDescription = null,
                        tint = AgriGreen,
                        modifier = Modifier.padding(10.dp).size(21.dp)
                    )
                }
            }
            if (datedCount == 0) {
                Column(
                    Modifier.fillMaxWidth().background(AgriGreenSoft, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Production trends will appear here", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        if (production.isEmpty()) "No production records have been added yet."
                        else "Existing production entries don't have a recognized date yet. Use a date such as 2026-10-10 to chart them.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                    val chartHeight = size.height - 4.dp.toPx()
                    val step = size.width / months.size
                    val barWidth = (step * .4f).coerceAtLeast(5.dp.toPx())
                    val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    for (grid in 0..3) {
                        val y = chartHeight * grid / 3f
                        drawLine(
                            color = AgriLine,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    months.forEachIndexed { index, month ->
                        val barHeight = (month.productionCount.toFloat() / peak) * (chartHeight - 4.dp.toPx())
                        val left = step * (index + .5f) - barWidth / 2f
                        drawRoundRect(
                            color = if (index == months.lastIndex) AgriGold else AgriGreen.copy(alpha = .72f),
                            topLeft = Offset(left, chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    months.forEach { month ->
                        Text(
                            month.label,
                            modifier = Modifier.weight(1f),
                            color = AgriMuted,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("$recentCount dated production record(s)", fontWeight = FontWeight.Bold, color = AgriGreen)
                        Text("From the last six calendar months", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Total logs: " + production.size, color = AgriText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DashboardChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DashboardAnalyticsMetric(
    title: String,
    value: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCream)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private data class FarmAchievement(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val progress: Int,
    val target: Int,
    val points: Int
) {
    val unlocked: Boolean get() = progress >= target
}

private fun buildFarmAchievements(
    fields: List<com.tubalrr.agrivision.domain.model.FieldRecord>,
    crops: List<CropRecord>,
    livestock: List<Livestock>,
    production: List<ProductionRecord>,
    tasks: List<FarmTask>,
    sales: List<SaleRecord>,
    expenses: List<ExpenseRecord>,
    inventory: List<InventoryItem>,
    incidents: List<FieldIncident>,
    assistance: List<AssistanceRecord>
): List<FarmAchievement> {
    val completedTasks = tasks.count { it.done }
    val financeReady = sales.isNotEmpty() && expenses.isNotEmpty()
    return listOf(
        FarmAchievement("First Field", "Register your first farm field.", Icons.Outlined.Map, fields.size, 1, 40),
        FarmAchievement("Field Explorer", "Register three or more fields.", Icons.Outlined.Explore, fields.size, 3, 80),
        FarmAchievement("Crop Tracker", "Add your first crop record.", Icons.Outlined.LocalFlorist, crops.size, 1, 40),
        FarmAchievement("Livestock Keeper", "Register a livestock group.", Icons.Outlined.Pets, livestock.size, 1, 40),
        FarmAchievement("First Production", "Record your first production entry.", Icons.Outlined.Assessment, production.size, 1, 50),
        FarmAchievement("Task Finisher", "Complete five farm tasks.", Icons.Outlined.Checklist, completedTasks, 5, 80),
        FarmAchievement("Financial Steward", "Record at least one sale and one expense.", Icons.Outlined.MonetizationOn, if (financeReady) 1 else 0, 1, 75),
        FarmAchievement("Stock Keeper", "Add an item to inventory.", Icons.Outlined.Inventory2, inventory.size, 1, 40),
        FarmAchievement("Incident Reporter", "Keep your first field incident on record.", Icons.Outlined.NotificationsActive, incidents.size, 1, 50),
        FarmAchievement("Assistance Tracker", "Record an agricultural assistance request.", Icons.Outlined.VerifiedUser, assistance.size, 1, 50),
        FarmAchievement("Farm Planner", "Create five farm tasks.", Icons.Outlined.EventNote, tasks.size, 5, 60)
    )
}

@Composable
private fun FarmAchievementsCard(achievements: List<FarmAchievement>) {
    val earned = achievements.filter { it.unlocked }
    val xp = earned.sumOf { it.points }
    val next = achievements.firstOrNull { !it.unlocked }
    val allUnlocked = next == null
    val levelSize = 150
    val level = xp / levelSize + 1
    val progress = if (allUnlocked) 1f else (xp % levelSize).toFloat() / levelSize

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AgriGreenDeep),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF103B2A), Color(0xFF176B45), Color(0xFF245841))
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(AgriGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = AgriGreenDeep, modifier = Modifier.size(27.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Farm Achievements", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text("Every record helps your farm story grow.", color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(13.dp)) {
                    Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEVEL", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                        Text(level.toString(), color = AgriGold, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$xp XP", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text("${earned.size} of ${achievements.size} badges unlocked", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
                }
                Text(if (allUnlocked) "Milestones complete" else "Next level: ${level + 1}", color = AgriGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = AgriGold,
                trackColor = Color.White.copy(alpha = .16f)
            )

            Text(
                when {
                    allUnlocked -> "All current badges unlocked! Keep adding real farm records as your operation grows."
                    next != null -> "NEXT BADGE · ${next.title} · ${next.progress.coerceAtMost(next.target)}/${next.target}"
                    else -> "Keep up the great work!"
                },
                color = Color.White.copy(alpha = .9f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                achievements.forEach { badge ->
                    Card(
                        Modifier.width(150.dp).height(142.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.unlocked) Color(0xFFE7F3E7) else Color.White.copy(alpha = .08f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (badge.unlocked) AgriGold.copy(alpha = .9f) else Color.White.copy(alpha = .12f)
                        )
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (badge.unlocked) AgriGoldSoft else Color.White.copy(alpha = .1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (badge.unlocked) Icons.Outlined.EmojiEvents else badge.icon,
                                    contentDescription = null,
                                    tint = if (badge.unlocked) AgriGreen else Color.White.copy(alpha = .7f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                badge.title,
                                color = if (badge.unlocked) AgriText else Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                badge.description,
                                color = if (badge.unlocked) AgriMuted else Color.White.copy(alpha = .72f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (badge.unlocked) "+${badge.points} XP" else "${badge.progress.coerceAtMost(badge.target)}/${badge.target} progress",
                                color = if (badge.unlocked) AgriGreen else AgriGold,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Text(
                "XP and badges are calculated from your saved farm records on this device; they are motivational milestones, not an official farm rating.",
                color = Color.White.copy(alpha = .64f),
                style = MaterialTheme.typography.labelSmall
            )
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


@Composable
private fun DashboardQuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(126.dp)
            .clickable(onClick = onClick)
            .border(1.dp, AgriLine, RoundedCornerShape(21.dp)),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(39.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AgriText,
                maxLines = 1
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AgriMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
