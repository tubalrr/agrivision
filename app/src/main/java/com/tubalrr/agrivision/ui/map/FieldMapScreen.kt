package com.tubalrr.agrivision

import android.content.pm.PackageManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import com.tubalrr.agrivision.domain.model.MapPoint
import kotlinx.coroutines.launch
import java.util.Locale

private enum class MapEditTarget {
    FARM,
    FIELD,
    INCIDENT
}

@Composable
internal fun FieldMapScreen(
    padding: PaddingValues,
    farm: FarmRecord,
    fields: List<FieldRecord>,
    crops: List<CropRecord>,
    incidents: List<FieldIncident>,
    farmViewModel: FarmViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(12.8797, 121.7740), 5.3f)
    }

    var showFields by remember { mutableStateOf(true) }
    var showCrops by remember { mutableStateOf(true) }
    var showIncidents by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf(MapEditTarget.FIELD) }
    var selectedFieldId by remember { mutableStateOf("") }
    var selectedIncidentId by remember { mutableStateOf("") }
    val draftPoints = remember { mutableStateListOf<MapPoint>() }

    LaunchedEffect(fields) {
        if (selectedFieldId.isBlank() && fields.isNotEmpty()) {
            selectedFieldId = fields.first().fieldId
        }
    }

    LaunchedEffect(incidents) {
        if (selectedIncidentId.isBlank() && incidents.isNotEmpty()) {
            selectedIncidentId = incidents.first().id
        }
    }

    val selectedField = fields.firstOrNull { it.fieldId == selectedFieldId }
    val selectedIncident = incidents.firstOrNull { it.id == selectedIncidentId }

    LaunchedEffect(editing, editTarget, selectedFieldId, selectedIncidentId, farm.boundaryPoints, fields, incidents) {
        if (!editing) return@LaunchedEffect
        val source = when (editTarget) {
            MapEditTarget.FARM -> farm.boundaryPoints
            MapEditTarget.FIELD -> selectedField?.boundaryPoints ?: emptyList()
            MapEditTarget.INCIDENT -> selectedIncident?.affectedAreaBoundary ?: emptyList()
        }
        draftPoints.clear()
        draftPoints.addAll(source)
    }

    val mapsKeyConfigured = remember(context) {
        runCatching {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            !appInfo.metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
        }.getOrDefault(false)
    }

    val mappedFieldCount = fields.count {
        (it.latitude != null && it.longitude != null) || it.boundaryPoints.size >= 3
    }
    val cropLayerCount = crops.count { it.fieldId.isNotBlank() }
    val activeIncidents = incidents.count { it.status != "Completed" }
    val mappedIncidentCount = incidents.count {
        (it.latitude != null && it.longitude != null) || it.affectedAreaBoundary.size >= 3
    }

    val allMappedPoints = remember(farm, fields, incidents) {
        buildList {
            addAll(farm.boundaryPoints)
            fields.forEach {
                addAll(it.boundaryPoints)
                if (it.latitude != null && it.longitude != null) {
                    add(MapPoint(it.latitude, it.longitude))
                }
            }
            incidents.forEach {
                addAll(it.affectedAreaBoundary)
                if (it.latitude != null && it.longitude != null) {
                    add(MapPoint(it.latitude, it.longitude))
                }
            }
        }
    }

    LaunchedEffect(allMappedPoints.firstOrNull()?.latitude, allMappedPoints.firstOrNull()?.longitude) {
        val first = allMappedPoints.firstOrNull() ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude, first.longitude), 15f),
            850
        )
    }

    val fitAll = {
        when {
            allMappedPoints.size >= 2 -> {
                val builder = LatLngBounds.Builder()
                allMappedPoints.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(builder.build(), 64),
                        700
                    )
                }
            }
            allMappedPoints.size == 1 -> {
                val point = allMappedPoints.first()
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(point.latitude, point.longitude),
                            16f
                        ),
                        600
                    )
                }
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(14.dp, 12.dp, 14.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Field Mapping",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AgriGreen
                )
                Text(
                    farm.farmName.ifBlank { "Farm map" },
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!mapsKeyConfigured) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Satellite tiles need a Maps API key", fontWeight = FontWeight.Bold, color = AgriGreen)
                        Text(
                            "The map and offline field data still work, but satellite tiles appear only when MAPS_API_KEY is configured in local.properties.",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                MapLayerChip("Fields", showFields) { showFields = it }
                MapLayerChip("Crops", showCrops) { showCrops = it }
                MapLayerChip("Incidents", showIncidents) { showIncidents = it }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().height(430.dp)) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                mapType = MapType.SATELLITE,
                                isMyLocationEnabled = false
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                myLocationButtonEnabled = false,
                                mapToolbarEnabled = false,
                                compassEnabled = true
                            ),
                            onMapLongClick = { point ->
                                if (editing) {
                                    draftPoints.add(MapPoint(point.latitude, point.longitude))
                                }
                            }
                        ) {
                            if (farm.boundaryPoints.size >= 3) {
                                Polygon(
                                    points = farm.boundaryPoints.toLatLng(),
                                    fillColor = AgriGreen.copy(alpha = 0.12f),
                                    strokeColor = AgriGreen,
                                    strokeWidth = 3f
                                )
                            }

                            if (showFields) {
                                fields.forEach { field ->
                                    if (field.boundaryPoints.size >= 3) {
                                        Polygon(
                                            points = field.boundaryPoints.toLatLng(),
                                            fillColor = AgriGreenSoft.copy(alpha = 0.28f),
                                            strokeColor = AgriGreen,
                                            strokeWidth = 2.5f
                                        )
                                    }
                                }
                            }

                            if (showCrops) {
                                fields.forEach { field ->
                                    if (field.boundaryPoints.size >= 3 && field.crop.isNotBlank()) {
                                        val crop = crops.firstOrNull { it.fieldId == field.fieldId }
                                        Polygon(
                                            points = field.boundaryPoints.toLatLng(),
                                            fillColor = cropStageFillColor(
                                                crop?.currentStatus ?: crop?.stage ?: field.currentStatus
                                            ),
                                            strokeColor = cropStageStrokeColor(
                                                crop?.currentStatus ?: crop?.stage ?: field.currentStatus
                                            ),
                                            strokeWidth = 1.5f
                                        )
                                    }
                                }
                            }

                            if (showFields || showCrops) {
                                fields.forEach { field ->
                                    val position = field.latitude?.let { lat ->
                                        field.longitude?.let { lon -> LatLng(lat, lon) }
                                    } ?: field.boundaryPoints.centroid()?.toLatLng()

                                    if (position != null) {
                                        val crop = crops.firstOrNull { it.fieldId == field.fieldId }
                                        Marker(
                                            state = rememberUpdatedMarkerState(position = position),
                                            title = field.name + if (field.fieldId.isBlank()) "" else " · " + field.fieldId,
                                            snippet = "Crop: " + (crop?.crop ?: field.crop).ifBlank { "Not recorded" } +
                                                    " · Stage: " + (crop?.currentStatus ?: field.currentStatus),
                                            onClick = { false }
                                        )
                                    }
                                }
                            }

                            if (showIncidents) {
                                incidents.forEach { incident ->
                                    val position = incident.latitude?.let { lat ->
                                        incident.longitude?.let { lon -> LatLng(lat, lon) }
                                    } ?: incident.affectedAreaBoundary.centroid()?.toLatLng()

                                    if (incident.affectedAreaBoundary.size >= 3) {
                                        Polygon(
                                            points = incident.affectedAreaBoundary.toLatLng(),
                                            fillColor = incidentSeverityFillColor(incident.severity),
                                            strokeColor = incidentSeverityStrokeColor(incident.severity),
                                            strokeWidth = 3f
                                        )
                                    }

                                    if (position != null) {
                                        Marker(
                                            state = rememberUpdatedMarkerState(position = position),
                                            title = "Incident · " + incident.type,
                                            snippet = incident.id +
                                                    " · Field: " + incident.fieldId.ifBlank { "—" } +
                                                    " · Severity: " + incident.severity +
                                                    " · " + incident.status,
                                            onClick = { false }
                                        )
                                    }
                                }
                            }

                            if (editing && draftPoints.isNotEmpty()) {
                                if (draftPoints.size >= 3) {
                                    Polygon(
                                        points = draftPoints.toLatLng(),
                                        fillColor = AgriGold.copy(alpha = 0.25f),
                                        strokeColor = AgriGold,
                                        strokeWidth = 4f
                                    )
                                } else {
                                    Polyline(
                                        points = draftPoints.toLatLng(),
                                        color = AgriGold,
                                        width = 4f
                                    )
                                }

                                draftPoints.forEachIndexed { index, point ->
                                    Marker(
                                        state = rememberUpdatedMarkerState(position = point.toLatLng()),
                                        title = "Point " + (index + 1),
                                        snippet = "%.6f, %.6f".format(
                                            Locale.US,
                                            point.latitude,
                                            point.longitude
                                        ),
                                        onClick = { false }
                                    )
                                }
                            }
                        }

                        Row(
                            Modifier.align(Alignment.TopEnd).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(onClick = fitAll, enabled = allMappedPoints.isNotEmpty()) {
                                Text("Fit all")
                            }
                            FilledTonalButton(onClick = { editing = !editing }) {
                                Text(if (editing) "Stop edit" else "Edit map")
                            }
                        }

                        if (editing) {
                            Card(
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.76f))
                            ) {
                                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "LONG-PRESS THE MAP TO ADD POINTS",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (editTarget == MapEditTarget.FIELD) {
                                            "1 point = field location • 3+ points = field boundary"
                                        } else {
                                            "Add 3+ points to draw the area boundary."
                                        },
                                        color = Color.White.copy(alpha = .8f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Map coverage", fontWeight = FontWeight.Bold, color = AgriGreen)
                        Text(
                            "Farm boundary " +
                                    if (farm.boundaryPoints.size >= 3) "mapped" else "not mapped" +
                                    " · " + mappedFieldCount + " field(s) mapped · " +
                                    cropLayerCount + " crop link(s) · " +
                                    mappedIncidentCount + " incident(s) mapped · " +
                                    activeIncidents + " active case(s)",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (editing) {
            item {
                MapEditorCard(
                    target = editTarget,
                    onTargetChange = {
                        editTarget = it
                        draftPoints.clear()
                    },
                    fields = fields,
                    selectedFieldId = selectedFieldId,
                    onFieldChange = {
                        selectedFieldId = it
                        draftPoints.clear()
                    },
                    incidents = incidents,
                    selectedIncidentId = selectedIncidentId,
                    onIncidentChange = {
                        selectedIncidentId = it
                        draftPoints.clear()
                    },
                    draftPoints = draftPoints,
                    selectedField = selectedField,
                    selectedIncident = selectedIncident,
                    onClear = { draftPoints.clear() },
                    onUndo = { if (draftPoints.isNotEmpty()) draftPoints.removeAt(draftPoints.lastIndex) },
                    onSave = {
                        when (editTarget) {
                            MapEditTarget.FARM -> {
                                if (draftPoints.size >= 3) {
                                    farmViewModel.saveFarmBoundary(draftPoints.toList())
                                }
                            }
                            MapEditTarget.FIELD -> {
                                selectedField?.let { field ->
                                    if (draftPoints.size == 1 || draftPoints.size >= 3) {
                                        farmViewModel.saveFieldMapping(field, draftPoints.toList())
                                    }
                                }
                            }
                            MapEditTarget.INCIDENT -> {
                                selectedIncident?.let { incident ->
                                    if (draftPoints.size >= 3) {
                                        farmViewModel.saveIncidentMapping(incident, draftPoints.toList())
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        item {
            Text(
                "Spatial relationship",
                color = AgriGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Farm → Fields → Crops → Incidents. Map geometry is stored in Room and can be backed up with the farm records.",
                color = AgriMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MapEditorCard(
    target: MapEditTarget,
    onTargetChange: (MapEditTarget) -> Unit,
    fields: List<FieldRecord>,
    selectedFieldId: String,
    onFieldChange: (String) -> Unit,
    incidents: List<FieldIncident>,
    selectedIncidentId: String,
    onIncidentChange: (String) -> Unit,
    draftPoints: SnapshotStateList<MapPoint>,
    selectedField: FieldRecord?,
    selectedIncident: FieldIncident?,
    onClear: () -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit
) {
    val requirements = when (target) {
        MapEditTarget.FARM -> "Farm boundary: 3+ points"
        MapEditTarget.FIELD -> "Field: 1 point or 3+ boundary points"
        MapEditTarget.INCIDENT -> "Affected area: 3+ points"
    }
    val saveEnabled = when (target) {
        MapEditTarget.FARM -> draftPoints.size >= 3
        MapEditTarget.FIELD -> draftPoints.size == 1 || draftPoints.size >= 3
        MapEditTarget.INCIDENT -> draftPoints.size >= 3
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Map editor", fontWeight = FontWeight.Bold, color = AgriGreen)
            Text(requirements, color = AgriMuted, style = MaterialTheme.typography.bodySmall)

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MapTargetChip("Farm", target == MapEditTarget.FARM) { onTargetChange(MapEditTarget.FARM) }
                MapTargetChip("Field", target == MapEditTarget.FIELD) { onTargetChange(MapEditTarget.FIELD) }
                MapTargetChip("Incident", target == MapEditTarget.INCIDENT) { onTargetChange(MapEditTarget.INCIDENT) }
            }

            when (target) {
                MapEditTarget.FARM -> {
                    Text(
                        draftPoints.size.toString() + " boundary point(s)",
                        color = AgriText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                MapEditTarget.FIELD -> {
                    SimpleSelector(
                        label = "Field",
                        value = selectedField?.let { it.name + " · " + it.fieldId } ?: "Select field",
                        items = fields.map { it.fieldId to (it.name + " · " + it.fieldId) },
                        selectedId = selectedFieldId,
                        onSelect = onFieldChange
                    )
                }
                MapEditTarget.INCIDENT -> {
                    SimpleSelector(
                        label = "Incident",
                        value = selectedIncident?.let { it.type + " · " + it.id } ?: "Select incident",
                        items = incidents.map { it.id to (it.type + " · " + it.id) },
                        selectedId = selectedIncidentId,
                        onSelect = onIncidentChange
                    )
                }
            }

            Text(
                draftPoints.size.toString() + " point(s) in draft",
                color = AgriGreen,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedButton(onClick = onUndo, enabled = draftPoints.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("Undo")
                }
                OutlinedButton(onClick = onClear, enabled = draftPoints.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
                Button(onClick = onSave, enabled = saveEnabled, modifier = Modifier.weight(1f)) {
                    Text("Save map")
                }
            }
        }
    }
}

@Composable
private fun SimpleSelector(
    label: String,
    value: String,
    items: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(value, modifier = Modifier.weight(1f))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { (id, title) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                title,
                                fontWeight = if (id == selectedId) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (items.isEmpty()) {
            Text("No records available.", color = AgriMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MapLayerChip(text: String, selected: Boolean, onSelected: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onSelected(!selected) },
        label = { Text(text) }
    )
}

@Composable
private fun MapTargetChip(text: String, selected: Boolean, onSelected: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(text) }
    )
}

private fun List<MapPoint>.toLatLng(): List<LatLng> =
    map { LatLng(it.latitude, it.longitude) }

private fun MapPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun List<MapPoint>.centroid(): MapPoint? =
    if (isEmpty()) null else MapPoint(
        latitude = map { it.latitude }.average(),
        longitude = map { it.longitude }.average()
    )

private fun cropStageFillColor(stage: String): Color =
    when (stage) {
        "Planting" -> Color(0xFFF4E4A6).copy(alpha = .45f)
        "Growing" -> Color(0xFF9ED8A2).copy(alpha = .45f)
        "Fertilization" -> Color(0xFFBFC9A8).copy(alpha = .45f)
        "Pest/Disease Monitoring" -> Color(0xFFE8C5A1).copy(alpha = .5f)
        "Harvest" -> Color(0xFFE8D37E).copy(alpha = .5f)
        else -> Color(0xFFC9D0C0).copy(alpha = .35f)
    }

private fun cropStageStrokeColor(stage: String): Color =
    when (stage) {
        "Planting" -> Color(0xFF9A7A15)
        "Growing" -> Color(0xFF28713C)
        "Fertilization" -> Color(0xFF64764A)
        "Pest/Disease Monitoring" -> Color(0xFF965D2D)
        "Harvest" -> Color(0xFF9A7C17)
        else -> AgriGreen
    }

private fun incidentSeverityFillColor(severity: String): Color =
    when (severity) {
        "Critical" -> Color(0xFFC94C4C).copy(alpha = .35f)
        "High" -> Color(0xFFE58947).copy(alpha = .35f)
        else -> Color(0xFFF0CC72).copy(alpha = .3f)
    }

private fun incidentSeverityStrokeColor(severity: String): Color =
    when (severity) {
        "Critical" -> Color(0xFF8C2525)
        "High" -> Color(0xFF9A4E1A)
        else -> Color(0xFF9A7718)
    }
