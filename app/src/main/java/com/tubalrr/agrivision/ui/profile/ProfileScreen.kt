package com.tubalrr.agrivision

import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FarmerRecord
import com.tubalrr.agrivision.domain.model.FieldRecord

import android.app.DatePickerDialog
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
internal fun ProfileScreen(
    padding: PaddingValues,
    profile: FarmerProfile,
    farmer: FarmerRecord,
    farm: FarmRecord,
    fields: List<FieldRecord>,
    onProfileSaved: (FarmerProfile) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    backupStatus: String,
    assistance: List<AssistanceRecord>,
    onAddAssistance: (AssistanceRecord) -> Unit,
    onUpdateAssistance: (AssistanceRecord) -> Unit,
    onAddField: (FieldRecord) -> Unit,
    onUpdateField: (FieldRecord) -> Unit
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
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
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
                colors = CardDefaults.cardColors(containerColor = AgriGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        shape = RoundedCornerShape(12.dp),
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
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenSoft),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Registry Identity", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text("Farmer ID: " + farmer.farmerId.ifBlank { "Not assigned" }, style = MaterialTheme.typography.bodySmall)
                    Text("Farm ID: " + farm.farmId, style = MaterialTheme.typography.bodySmall)
                    Text("Registered fields: " + fields.size, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            FieldRegistrySection(
                farm = farm,
                fields = fields,
                onAdd = onAddField,
                onUpdate = onUpdateField
            )
        }

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
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AgriCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Backup & Restore", fontWeight = FontWeight.Bold)
                    Text(
                        "Export AgriVision Backup creates a versioned Room-data snapshot. Import restores that snapshot into Room.",
                        color = AgriMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onExportBackup, shape = RoundedCornerShape(12.dp)) { Text("Export AgriVision Backup") }
                        OutlinedButton(onClick = onImportBackup, shape = RoundedCornerShape(12.dp)) { Text("Import AgriVision Backup") }
                    }
                    if (backupStatus.isNotBlank()) {
                        Text(backupStatus, color = AgriGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                onAdd = onAddAssistance,
                onUpdate = onUpdateAssistance
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
internal fun AssistanceSection(
    assistance: List<AssistanceRecord>,
    onAdd: (AssistanceRecord) -> Unit,
    onUpdate: (AssistanceRecord) -> Unit
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
                onUpdate(updated)
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
internal fun AssistanceWorkflowDialog(
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
internal fun AddAssistanceDialog(
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
internal fun RegistryInfoCard(title: String, value: String, modifier: Modifier = Modifier) {
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
internal fun FarmerRegistryDialog(
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
