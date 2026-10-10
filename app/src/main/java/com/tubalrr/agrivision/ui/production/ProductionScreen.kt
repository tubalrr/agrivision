package com.tubalrr.agrivision

import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Brush

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
internal fun ProductionFinanceScreen(
    padding: PaddingValues,
    production: List<ProductionRecord>,
    crops: List<CropRecord>,
    livestock: List<Livestock>,
    expenses: List<ExpenseRecord>,
    sales: List<SaleRecord>,
    totalExpenses: Double,
    totalSales: Double,
    netIncome: Double,
    totalAnimals: Int,
    openTasks: Int,
    farmerProfile: FarmerProfile,
    assistance: List<AssistanceRecord>,
    fieldIncidents: List<FieldIncident>,
    incidentEvents: List<IncidentEvent>,
    onExportCasePackage: () -> Unit,
    onExportDaReport: (String, DaReportFormat) -> Unit,
    exportStatus: String,
    reportSubmission: ReportSubmission,
    onProduction: (ProductionRecord) -> Unit,
    feedLogs: List<com.tubalrr.agrivision.data.local.FeedLogRecord>,
    onFeedLog: (com.tubalrr.agrivision.data.local.FeedLogRecord) -> Unit,
    onExpense: (ExpenseRecord) -> Unit,
    onSale: (SaleRecord) -> Unit,
    onSubmissionSaved: (ReportSubmission) -> Unit
) {
    var tab by remember { mutableStateOf("Production") }
    var showDialog by remember { mutableStateOf(false) }
    var showFeedDialog by remember { mutableStateOf(false) }
    var selectedDaReport by remember { mutableStateOf(DaReportExporter.reportTypes.first()) }
    var selectedDaFormat by remember { mutableStateOf(DaReportFormat.PDF) }
    var daReportCategory by remember { mutableStateOf("All reports") }
    var reportMenuExpanded by remember { mutableStateOf(false) }
    val reportGroups = listOf(
        "All reports" to DaReportExporter.reportTypes,
        "Registry" to listOf("Farmer Profile", "Farm Registry", "Field Summary"),
        "Production" to listOf("Crop Production", "Harvest", "Crop Lifecycle", "Livestock Inventory", "Livestock Lifecycle", "Feed Logs"),
        "Finance & Inputs" to listOf("Farm Inputs", "Inventory Transactions", "Expenses", "Sales"),
        "Incidents & DA" to listOf("Incidents", "Incident Audit", "Assistance")
    )
    val availableReportTypes = reportGroups.firstOrNull { it.first == daReportCategory }?.second
        ?: DaReportExporter.reportTypes

    if (showDialog) {
        AddProductionFinanceDialog(
            tab = tab,
            crops = crops,
            livestock = livestock,
            onDismiss = { showDialog = false },
            onProduction = { record -> onProduction(record); showDialog = false },
            onExpense = { record -> onExpense(record); showDialog = false },
            onSale = { record -> onSale(record); showDialog = false }
        )
    }

    if (showFeedDialog) {
        FeedLogDialog(
            livestock = livestock,
            onDismiss = { showFeedDialog = false },
            onSave = { record -> onFeedLog(record); showFeedDialog = false }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF174B32), AgriGreenDeep, Color(0xFF0A281B))
                        )
                    )
                    .border(1.dp, Color(0xFF2E7550), RoundedCornerShape(28.dp))
                    .padding(21.dp)
            ) {
                Icon(
                    Icons.Outlined.Assessment,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.07f),
                    modifier = Modifier.size(116.dp).align(Alignment.CenterEnd)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Icon(Icons.Outlined.Insights, null, tint = AgriGold, modifier = Modifier.size(17.dp))
                        Text(
                            "FARM REPORT CENTER",
                            color = AgriGold,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Text(
                        "Your farm.\nClearly understood.",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )
                    Text(
                        "Review performance, check record readiness and export organized farm reports.",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Storage, null, tint = Color(0xFFB9E85A), modifier = Modifier.size(14.dp))
                            Text(
                                "Based on your saved farm records",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Farm performance", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Net result", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = (if (netIncome >= 0.0) Color(0xFFB9E85A) else Color(0xFFFFD4C7)).copy(alpha = 0.18f)
                        ) {
                            Text(
                                if (netIncome >= 0.0) "Positive" else "Negative",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = if (netIncome >= 0.0) Color(0xFFD9F4A0) else Color(0xFFFFD4C7),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        "₱" + money(netIncome),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.TrendingUp, null, tint = Color(0xFFB9E85A), modifier = Modifier.size(18.dp))
                            Text("Sales", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelSmall)
                            Text("₱" + money(totalSales), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, null, tint = Color(0xFFFFD4C7), modifier = Modifier.size(18.dp))
                            Text("Expenses", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelSmall)
                            Text("₱" + money(totalExpenses), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReportStatCard("Livestock", totalAnimals.toString(), "total heads", Modifier.weight(1f))
                    ReportStatCard("Open tasks", openTasks.toString(), "need action", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ReportStatCard("Production", production.size.toString(), "records logged", Modifier.weight(1f))
                    ReportStatCard("Active incidents", fieldIncidents.count { it.status != "Completed" }.toString(), "cases in progress", Modifier.weight(1f))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == "Production", onClick = { tab = "Production" }, label = { Text("Production") }, leadingIcon = { Icon(Icons.Outlined.Assessment, null) }, shape = RoundedCornerShape(12.dp))
                FilterChip(selected = tab == "Feed Logs", onClick = { tab = "Feed Logs" }, label = { Text("Feed Logs") }, leadingIcon = { Icon(Icons.Outlined.Restaurant, null) }, shape = RoundedCornerShape(12.dp))
                FilterChip(selected = tab == "Finance", onClick = { tab = "Finance" }, label = { Text("Finance") }, leadingIcon = { Icon(Icons.Outlined.MonetizationOn, null) }, shape = RoundedCornerShape(12.dp))
                FilterChip(selected = tab == "DA Report", onClick = { tab = "DA Report" }, label = { Text("DA Report") }, leadingIcon = { Icon(Icons.Outlined.Assessment, null) }, shape = RoundedCornerShape(12.dp))
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

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("DA-Ready Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Export individual official-style reports directly from Room data.",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Report category", fontWeight = FontWeight.SemiBold)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            reportGroups.forEach { (group, reports) ->
                                FilterChip(
                                    selected = daReportCategory == group,
                                    onClick = {
                                        daReportCategory = group
                                        if (selectedDaReport !in reports) selectedDaReport = reports.first()
                                    },
                                    label = { Text(group, maxLines = 1) }
                                )
                            }
                        }
                        Text(
                            availableReportTypes.size.toString() + " report types available",
                            color = AgriMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { reportMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Outlined.Description, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(selectedDaReport, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(
                                expanded = reportMenuExpanded,
                                onDismissRequest = { reportMenuExpanded = false }
                            ) {
                                availableReportTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                type,
                                                fontWeight = if (selectedDaReport == type) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (selectedDaReport == type) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                                null
                                            )
                                        },
                                        onClick = {
                                            selectedDaReport = type
                                            reportMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text("Format", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DaReportFormat.values().forEach { format ->
                                FilterChip(
                                    selected = selectedDaFormat == format,
                                    onClick = { selectedDaFormat = format },
                                    label = { Text(format.extension.uppercase()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(containerColor = AgriCream)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AgriGreenSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (selectedDaFormat) {
                                            DaReportFormat.PDF -> Icons.Outlined.PictureAsPdf
                                            DaReportFormat.CSV -> Icons.Outlined.TableChart
                                            DaReportFormat.JSON -> Icons.Outlined.DataObject
                                        },
                                        null,
                                        tint = AgriGreen,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(selectedDaReport, color = AgriText, fontWeight = FontWeight.Bold)
                                    Text(
                                        DaReportExporter.fileName(selectedDaReport, selectedDaFormat),
                                        color = AgriMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        if (exportStatus.isNotBlank()) {
                            Text(
                                exportStatus,
                                color = if (exportStatus.contains("failed", ignoreCase = true)) MaterialTheme.colorScheme.error else AgriGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = { onExportDaReport(selectedDaReport, selectedDaFormat) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export " + selectedDaReport)
                        }
                    }
                }
            }
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
        } else if (tab == "Feed Logs") {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreen)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("FEED MANAGEMENT", color = AgriGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Track livestock feed consumption.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Room-backed records are included in backup and DA reports.", color = Color.White.copy(alpha = .78f))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Feed History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(feedLogs.size.toString() + " records", color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { showFeedDialog = true }) { Text("+ Add Feed") }
                }
            }
            if (feedLogs.isEmpty()) {
                item { InfoCard("No feed records yet", "Add feed consumption for a livestock group to start tracking.") }
            } else {
                items(feedLogs.sortedByDescending { it.createdAt }) { log ->
                    FarmRecordCard(
                        log.feedName.ifBlank { "Feed record" },
                        String.format(Locale.US, "%.2f %s", log.quantityKg, log.unit),
                        listOf(log.date, log.livestockId.ifBlank { "Unlinked livestock" }, log.notes).filter { it.isNotBlank() }.joinToString(" · "),
                        Icons.Outlined.Restaurant
                    )
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
            items(production) { record ->
                val sourceLabel = when (record.sourceType) {
                    "Crop" -> "Crop " + record.sourceId
                    "Livestock" -> "Livestock " + record.sourceId
                    else -> "Legacy / unlinked"
                }
                val areaLabel = if (record.areaHectares > 0.0) {
                    String.format(Locale.US, "%.2f ha", record.areaHectares)
                } else {
                    "Area not recorded"
                }
                FarmRecordCard(
                    record.product,
                    record.quantity + " · " + areaLabel,
                    record.period + " · " + sourceLabel + " · " + record.productionType,
                    Icons.Outlined.Assessment
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Income", "₱" + money(totalSales), "Crop, livestock and other income.")
                    SummaryCard("Expenses", "₱" + money(totalExpenses), "Operating farm costs.")
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Finance Records")
                    TextButton(onClick = { showDialog = true }) { Text("+ Add") }
                }
            }
            item { Text("Income by category", fontWeight = FontWeight.SemiBold) }
            item {
                FinanceCategorySummary(
                    FinancialCategories.incomeCategories.map { category ->
                        category to sales.filter { it.incomeCategory.equals(category, ignoreCase = true) }.sumOf { it.amount }
                    }
                )
            }
            item { Text("Expenses by category", fontWeight = FontWeight.SemiBold) }
            item {
                FinanceCategorySummary(
                    FinancialCategories.expenseCategories.map { category ->
                        category to expenses.filter { it.category.equals(category, ignoreCase = true) }.sumOf { it.amount }
                    }
                )
            }
            item { Text("Expense records", fontWeight = FontWeight.SemiBold) }
            items(expenses) { expense ->
                val detail = listOf(expense.date, expense.note).filter { it.isNotBlank() }.joinToString(" · ")
                FarmRecordCard(expense.category, "₱" + money(expense.amount), detail, Icons.Outlined.ReceiptLong)
            }
            item { Text("Income records", fontWeight = FontWeight.SemiBold) }
            items(sales) { sale ->
                FarmRecordCard(
                    sale.product,
                    "₱" + money(sale.amount),
                    sale.incomeCategory + " · " + sale.date,
                    Icons.Outlined.MonetizationOn
                )
            }
        }
    }
}

@Composable
private fun FinanceCategorySummary(rows: List<Pair<String, Double>>) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { (category, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category, fontWeight = FontWeight.Medium)
                    Text("₱" + money(value), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun AddProductionFinanceDialog(
    tab: String,
    crops: List<CropRecord>,
    livestock: List<Livestock>,
    onDismiss: () -> Unit,
    onProduction: (ProductionRecord) -> Unit,
    onExpense: (ExpenseRecord) -> Unit,
    onSale: (SaleRecord) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("Today") }
    var note by remember { mutableStateOf("") }
    var financeType by remember { mutableStateOf("Expense") }
    var incomeCategory by remember { mutableStateOf(FinancialCategories.incomeCategories.first()) }
    var expenseCategory by remember { mutableStateOf(FinancialCategories.expenseCategories.first()) }
    var productionType by remember { mutableStateOf("Harvest") }
    var sourceType by remember { mutableStateOf("Crop") }
    var sourceId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (tab == "Production") "Record Farm Production" else "Add Finance Record",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tab == "Production") {
                    Text("Production source", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = sourceType == "Crop",
                            onClick = {
                                sourceType = "Crop"
                                sourceId = crops.firstOrNull()?.cropId.orEmpty()
                            },
                            label = { Text("Crop") }
                        )
                        FilterChip(
                            selected = sourceType == "Livestock",
                            onClick = {
                                sourceType = "Livestock"
                                sourceId = livestock.firstOrNull()?.groupId.orEmpty()
                            },
                            label = { Text("Livestock") }
                        )
                    }

                    if (sourceType == "Crop") {
                        if (crops.isEmpty()) {
                            Text(
                                "Register a crop cycle first in Farm → Crops.",
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                crops.forEach { crop ->
                                    FilterChip(
                                        selected = sourceId == crop.cropId,
                                        onClick = { sourceId = crop.cropId },
                                        label = {
                                            Text(
                                                crop.name + " · " + crop.crop,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        if (livestock.isEmpty()) {
                            Text(
                                "Register a livestock group first in Farm → Livestock.",
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                livestock.forEach { group ->
                                    FilterChip(
                                        selected = sourceId == group.groupId,
                                        onClick = { sourceId = group.groupId },
                                        label = {
                                            Text(
                                                group.name + " · " + group.kind,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Product / commodity") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Harvest quantity") },
                        supportingText = { Text("Example: 4200 kg") },
                        singleLine = true
                    )
                    Text("Production type", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Harvest", "Milk", "Eggs", "Meat", "Other").forEach { option ->
                            FilterChip(
                                selected = productionType == option,
                                onClick = { productionType = option },
                                label = { Text(option) }
                            )
                        }
                    }
                } else {
                    Text("Record type", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = financeType == "Income",
                            onClick = { financeType = "Income" },
                            label = { Text("Income") }
                        )
                        FilterChip(
                            selected = financeType == "Expense",
                            onClick = { financeType = "Expense" },
                            label = { Text("Expense") }
                        )
                    }

                    val categories = if (financeType == "Income") {
                        FinancialCategories.incomeCategories
                    } else {
                        FinancialCategories.expenseCategories
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = if (financeType == "Income") {
                                    incomeCategory == category
                                } else {
                                    expenseCategory == category
                                },
                                onClick = {
                                    if (financeType == "Income") {
                                        incomeCategory = category
                                    } else {
                                        expenseCategory = category
                                    }
                                },
                                label = { Text(category) }
                            )
                        }
                    }

                    if (financeType == "Income") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Product / income source") },
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Expense description") },
                            supportingText = { Text("Example: 2 farm workers · 3 days") },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (₱)") },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = period,
                    onValueChange = { period = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (tab == "Production") "Production date / period" else "Date") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val sourceExists = if (sourceType == "Crop") {
                crops.any { it.cropId == sourceId }
            } else {
                livestock.any { it.groupId == sourceId }
            }
            val financialAmount = amount.toDoubleOrNull()
            val financeValid = financialAmount != null && financialAmount > 0.0
            TextButton(
                enabled = if (tab == "Production") {
                    name.isNotBlank() &&
                            quantity.isNotBlank() &&
                            quantity.contains(" ") &&
                            sourceExists
                } else {
                    financeValid && if (financeType == "Income") name.isNotBlank() else note.isNotBlank()
                },
                onClick = {
                    if (tab == "Production") {
                        val sourceArea = if (sourceType == "Crop") {
                            crops.firstOrNull { it.cropId == sourceId }?.area?.toDoubleOrNull() ?: 0.0
                        } else {
                            0.0
                        }
                        onProduction(
                            ProductionRecord(
                                product = name.trim(),
                                quantity = quantity.trim(),
                                period = period.trim(),
                                sourceType = sourceType,
                                sourceId = sourceId,
                                fieldId = if (sourceType == "Crop") {
                                    crops.firstOrNull { it.cropId == sourceId }?.fieldId.orEmpty()
                                } else "",
                                areaHectares = sourceArea,
                                productionType = productionType
                            )
                        )
                    } else {
                        val value = financialAmount ?: return@TextButton
                        if (financeType == "Income") {
                            onSale(
                                SaleRecord(
                                    product = name.trim(),
                                    amount = value,
                                    date = period.trim(),
                                    incomeCategory = incomeCategory
                                )
                            )
                        } else {
                            onExpense(
                                ExpenseRecord(
                                    category = expenseCategory,
                                    amount = value,
                                    note = note.trim(),
                                    date = period.trim()
                                )
                            )
                        }
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
private fun FeedLogDialog(
    livestock: List<Livestock>,
    onDismiss: () -> Unit,
    onSave: (com.tubalrr.agrivision.data.local.FeedLogRecord) -> Unit
) {
    var livestockId by remember { mutableStateOf(livestock.firstOrNull()?.groupId.orEmpty()) }
    var feedName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Feed Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(livestockId, { livestockId = it }, label = { Text("Livestock / Group ID") }, singleLine = true)
                OutlinedTextField(feedName, { feedName = it }, label = { Text("Feed name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(date, { date = it }, label = { Text("Date") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                enabled = feedName.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                onClick = {
                    onSave(
                        com.tubalrr.agrivision.data.local.FeedLogRecord(
                            livestockId = livestockId.trim(),
                            date = date.trim(),
                            feedName = feedName.trim(),
                            quantityKg = quantity.toDoubleOrNull() ?: 0.0,
                            unit = unit.trim().ifBlank { "kg" },
                            notes = notes.trim()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
