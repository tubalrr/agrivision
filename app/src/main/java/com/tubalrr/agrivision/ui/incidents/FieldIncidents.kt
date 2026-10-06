package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
internal fun FieldIncidentCard(
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
internal fun IncidentTimelineDialog(
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
internal fun IncidentReviewDialog(
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
internal fun AddFieldIncidentDialog(
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
