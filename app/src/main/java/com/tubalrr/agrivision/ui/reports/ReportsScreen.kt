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
internal fun CasePackageCard(
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
internal fun DataQualityCard(checks: List<Pair<String, Boolean>>) {
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
internal fun ReportSubmissionCard(
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
internal fun ReportSubmissionDialog(
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
internal fun RegistryReviewCard(status: String, notes: String) {
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
internal fun ReportSummaryGrid(items: List<Pair<String, String>>) {
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
internal fun ReportMetric(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun ReportStatCard(title: String, value: String, detail: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AgriGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
