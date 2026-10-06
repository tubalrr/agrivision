package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.tubalrr.agrivision.domain.model.FieldRecord
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
    onCreateAssistance: () -> Unit,
    onResolve: () -> Unit
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
                        listOf(
                            incident.commodity,
                            "Field " + incident.fieldId.ifBlank { "not linked" },
                            incident.affectedArea,
                            incident.date
                        ).filter { it.isNotBlank() }.joinToString(" · "),
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

            Text(
                "Farmer: " + incident.farmerId.ifBlank { "not linked" } +
                        " · Farm: " + incident.farmId.ifBlank { "not linked" },
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )

            if (incident.latitude != null && incident.longitude != null) {
                Text(
                    "Coordinates: %.5f, %.5f".format(Locale.US, incident.latitude, incident.longitude),
                    color = AgriMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (incident.reviewer.isNotBlank()) {
                Text(
                    "Reviewer: " + incident.reviewer,
                    color = AgriMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (incident.reviewNotes.isNotBlank()) {
                Text(
                    "Review note: " + incident.reviewNotes,
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (incident.resolution.isNotBlank()) {
                Text(
                    "Resolution: " + incident.resolution,
                    color = AgriGreen,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
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
                        TextButton(onClick = onReview) { Text("Start Review") }
                    }
                    "Under Review" -> {
                        TextButton(onClick = onReview) { Text("Review") }
                    }
                    "Verified" -> {
                        if (!hasAssistanceRequest) {
                            TextButton(onClick = onCreateAssistance) { Text("Request Assistance") }
                        }
                    }
                    "Assistance" -> {
                        if (assistanceCompleted) {
                            TextButton(onClick = onResolve) { Text("Mark Completed") }
                        } else {
                            Text(
                                "Awaiting assistance completion",
                                color = AgriWarning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    "Completed" -> {
                        Text("Case completed", color = AgriGreen, style = MaterialTheme.typography.bodySmall)
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
                    Text(
                        "Farmer " + incident.farmerId.ifBlank { "—" } +
                                " · Farm " + incident.farmId.ifBlank { "—" } +
                                " · Field " + incident.fieldId.ifBlank { "—" },
                        color = AgriMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (events.isEmpty()) {
                    item { Text("No status history recorded.", color = AgriMuted) }
                } else {
                    items(events) { event ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(
                                    if (event.status == "Verified" || event.status == "Assistance" || event.status == "Completed") AgriGreen else AgriGreenSoft
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (event.status == "Verified" || event.status == "Assistance" || event.status == "Completed") "✓" else "•",
                                    color = if (event.status == "Verified" || event.status == "Assistance" || event.status == "Completed") Color.White else AgriGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                val transition = if (event.fromStatus.isBlank()) {
                                    event.status
                                } else {
                                    event.fromStatus + " → " + event.status
                                }
                                Text(transition, fontWeight = FontWeight.Bold)
                                Text(
                                    formatter.format(java.util.Date(event.timestamp)),
                                    color = AgriMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                if (event.actor.isNotBlank()) {
                                    Text("Actor: " + event.actor, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                                }
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
    val initialStatus = when (incident.status) {
        "Submitted" -> "Under Review"
        "Under Review" -> "Verified"
        else -> incident.status
    }
    var status by remember(incident.id) { mutableStateOf(initialStatus) }
    var reviewer by remember(incident.id) { mutableStateOf(incident.reviewer) }
    var notes by remember(incident.id) { mutableStateOf(incident.reviewNotes) }

    val options = when (incident.status) {
        "Submitted" -> listOf("Under Review")
        "Under Review" -> listOf("Verified", "Returned")
        else -> emptyList()
    }

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
                    OutlinedTextField(
                        value = reviewer,
                        onValueChange = { reviewer = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reviewer / validating officer") },
                        singleLine = true
                    )
                }
                if (options.isNotEmpty()) {
                    item {
                        Text("Workflow step", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            options.forEach { option ->
                                FilterChip(
                                    selected = status == option,
                                    onClick = { status = option },
                                    label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
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
                        "Workflow is enforced in the data layer: Submitted → Under Review → Verified. Returned reports must be submitted again.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = reviewer.isNotBlank() && status != incident.status,
                onClick = {
                    onSave(
                        incident.copy(
                            status = status,
                            reviewer = reviewer.trim(),
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
internal fun IncidentResolutionDialog(
    incident: FieldIncident,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var resolution by remember(incident.id) { mutableStateOf(incident.resolution) }
    var reviewer by remember(incident.id) { mutableStateOf(incident.reviewer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Incident", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    incident.id + " · Assistance completed",
                    color = AgriGreen,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = reviewer,
                    onValueChange = { reviewer = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reviewer / closing officer") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = resolution,
                    onValueChange = { resolution = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Resolution / outcome") },
                    supportingText = { Text("Example: Inputs distributed and field restored.") },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = reviewer.isNotBlank() && resolution.isNotBlank(),
                onClick = { onSave(resolution.trim(), reviewer.trim()) }
            ) { Text("Complete Case") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun AddFieldIncidentDialog(
    farmerId: String,
    farmerName: String,
    farmId: String,
    farmName: String,
    fields: List<FieldRecord>,
    onDismiss: () -> Unit,
    onSave: (FieldIncident) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Pest / Disease") }
    var fieldId by remember { mutableStateOf(fields.firstOrNull()?.fieldId.orEmpty()) }
    var commodity by remember { mutableStateOf(fields.firstOrNull()?.crop.orEmpty()) }
    var affectedArea by remember {
        mutableStateOf(
            fields.firstOrNull()?.areaHectares?.let { String.format(Locale.US, "%.2f ha", it) }.orEmpty()
        )
    }
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time))
    }
    var severity by remember { mutableStateOf("Moderate") }
    var description by remember { mutableStateOf("") }
    var evidenceUri by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
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
                modifier = Modifier.heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Text("Reporter / farmer", color = AgriGreen, fontWeight = FontWeight.Bold)
                            Text(
                                farmerName.ifBlank { "Farmer not registered" } + " · " + farmerId.ifBlank { "No farmer ID" },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                farmName.ifBlank { "Farm not registered" } + " · " + farmId.ifBlank { "No farm ID" },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                item {
                    Text("Field", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    if (fields.isEmpty()) {
                        Text(
                            "Register a field first in Farm → Fields.",
                            color = AgriWarning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fields.forEach { field ->
                                FilterChip(
                                    selected = fieldId == field.fieldId,
                                    onClick = {
                                        fieldId = field.fieldId
                                        if (commodity.isBlank() || commodity == fields.firstOrNull()?.crop) {
                                            commodity = field.crop
                                        }
                                        affectedArea = String.format(Locale.US, "%.2f ha", field.areaHectares)
                                        latitudeText = field.latitude?.toString().orEmpty()
                                        longitudeText = field.longitude?.toString().orEmpty()
                                    },
                                    label = { Text(field.name + " · " + field.fieldId) }
                                )
                            }
                        }
                    }
                }
                item {
                    Text("Issue type", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                        value = commodity,
                        onValueChange = { commodity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Commodity / crop / animal") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = affectedArea,
                        onValueChange = { affectedArea = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Affected area / quantity") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Date: " + date) }
                }
                item {
                    Text("Severity", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Latitude (optional)") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Longitude (optional)") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
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
                        "Coordinates are entered explicitly; the app does not request GPS permission.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = fields.isNotEmpty() &&
                        fieldId.isNotBlank() &&
                        commodity.isNotBlank() &&
                        description.isNotBlank(),
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
                            evidenceUri = evidenceUri,
                            farmerId = farmerId,
                            farmId = farmId,
                            fieldId = fieldId,
                            latitude = latitudeText.toDoubleOrNull(),
                            longitude = longitudeText.toDoubleOrNull()
                        )
                    )
                }
            ) { Text("Save Draft") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
