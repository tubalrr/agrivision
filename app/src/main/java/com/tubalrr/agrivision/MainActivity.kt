package com.tubalrr.agrivision

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person

private val AgriCream = Color(0xFFF7F4E9)
private val AgriCard = Color(0xFFFFFCF5)
private val AgriGreen = Color(0xFF245B3A)
private val AgriGreenSoft = Color(0xFFDDE8C8)
private val AgriSage = Color(0xFFC9D4AD)
private val AgriGold = Color(0xFFCDBB8A)
private val AgriText = Color(0xFF183526)
private val AgriMuted = Color(0xFF7A806F)
private val AgriLine = Color(0xFFE5E2D6)

data class FarmField(
    val name: String,
    val crop: String,
    val area: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
data class FarmInput(val name: String, val quantity: String, val unit: String)
data class FarmTask(val title: String, val date: String, val done: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgriVisionApp() }
    }
}

@Composable
private fun AgriVisionApp() {
    var selected by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val fieldPrefs = remember {
        context.getSharedPreferences("agrivision_field", Context.MODE_PRIVATE)
    }
    val savedLatitude = fieldPrefs.getString("latitude", null)?.toDoubleOrNull()
    val savedLongitude = fieldPrefs.getString("longitude", null)?.toDoubleOrNull()
    val savedBoundary = fieldPrefs.getString("boundary", null)
        ?.split(";")
        ?.mapNotNull { pair ->
            val parts = pair.split(",")
            if (parts.size == 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            } else null
        }
        ?: emptyList()
    val fieldBoundary = remember { mutableStateListOf<LatLng>().apply { addAll(savedBoundary) } }
    val fields = remember {
        mutableStateListOf(
            FarmField(
                "North Field",
                "Rice",
                "1.0 ha",
                latitude = savedLatitude,
                longitude = savedLongitude
            )
        )
    }
    val inputs = remember {
        mutableStateListOf(
            FarmInput("Rice Seeds", "25", "kg"),
            FarmInput("Complete Fertilizer", "3", "bags")
        )
    }
    val tasks = remember {
        mutableStateListOf(
            FarmTask("Inspect field", "Today", false),
            FarmTask("Record farm expenses", "Today", false)
        )
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
                    padding,
                    fields,
                    inputs,
                    tasks,
                    fieldBoundary,
                    onFieldLocationSelected = { point ->
                        if (fields.isNotEmpty()) {
                            fields[0] = fields[0].copy(
                                latitude = point.latitude,
                                longitude = point.longitude
                            )
                            fieldPrefs.edit()
                                .putString("latitude", point.latitude.toString())
                                .putString("longitude", point.longitude.toString())
                                .apply()
                        }
                    },
                    onFieldBoundaryChanged = { points ->
                        fieldBoundary.clear()
                        fieldBoundary.addAll(points)
                        val encoded = points.joinToString(";") { it.latitude.toString() + "," + it.longitude.toString() }
                        fieldPrefs.edit().putString("boundary", encoded).apply()
                    }
                )
                1 -> FieldsScreen(padding, fields)
                2 -> InsightsScreen(padding, fields)
                3 -> TasksScreen(padding, tasks)
                else -> MoreScreen(padding)
            }
        }
    }
}

@Composable
private fun AgriBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Fields" to Icons.Outlined.Map,
        "Insights" to Icons.Outlined.Assessment,
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
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected == index) AgriGreen
                                else AgriGreenSoft.copy(alpha = 0.55f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.second,
                            contentDescription = item.first,
                            tint = if (selected == index) Color.White else AgriGreen,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                },
                label = {
                    Text(
                        item.first,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = AgriGreen,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = AgriGreen,
                    unselectedTextColor = AgriMuted
                )
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    padding: PaddingValues,
    fields: List<FarmField>,
    inputs: List<FarmInput>,
    tasks: List<FarmTask>,
    fieldBoundary: List<LatLng>,
    onFieldLocationSelected: (LatLng) -> Unit,
    onFieldBoundaryChanged: (List<LatLng>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_agrivision_logo),
                        contentDescription = "AgriVision logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "AgriVision",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Farm Management",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AgriCard)
                            .border(1.dp, AgriLine, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = AgriGreen
                        )
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AgriGreenSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "F",
                            fontWeight = FontWeight.Bold,
                            color = AgriGreen
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text("Good morning, Farmer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Your farm is healthy today · Updated just now", color = AgriMuted)
            }
        }

        item {
            SectionTitle("Farm Analytics", "Info")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { StatCard("Total Area", "1.0 ha", "+0.0 ha", "🌿", AgriGreenSoft) }
                item { StatCard("Est. Yield", "—", "Add harvest", "🌾", Color(0xFFE9DFC7)) }
                item { StatCard("Revenue", "₱0", "No sales yet", "₱", Color(0xFFD9E4C1)) }
            }
        }

        item {
            SectionTitle("Crop Health Monitoring", "View Details ›")
            Spacer(Modifier.height(10.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AgriCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    HealthRing(85)
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CropHealthRow("Rice", "92% Healthy", "North Field", AgriGreen)
                        CropHealthRow("Rice", "78% Fair", "Demo Field", Color(0xFFE9A72C))
                    }
                }
            }
        }

        item {
            SectionTitle("Live Google Field Map", "Satellite · Long-press to pin")
            Spacer(Modifier.height(10.dp))
            LiveFieldMap(
                field = fields.firstOrNull(),
                boundary = fieldBoundary,
                onFieldLocationSelected = onFieldLocationSelected,
                onFieldBoundaryChanged = onFieldBoundaryChanged
            )
        }

        item {
            SectionTitle("Field Overview", "Live · Today")
            Spacer(Modifier.height(10.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDEE5CF))
            ) {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(185.dp).padding(10.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFB7C69A))
                    ) {
                        Text("FIELD MAP", Modifier.align(Alignment.TopStart).padding(14.dp), color = AgriGreen, fontWeight = FontWeight.Bold)
                        Box(
                            Modifier.align(Alignment.Center).size(110.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFECF0DF))
                                .border(2.dp, AgriGreen, RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("●", color = AgriGreen)
                                Text("North Field", fontWeight = FontWeight.Bold, color = AgriText)
                                Text("Rice · 1.0 ha", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NDVI — Good vegetation", color = AgriGreen, fontWeight = FontWeight.SemiBold)
                        Text("Healthy", color = AgriGreen)
                    }
                }
            }
        }

        item {
            SectionTitle("Today's Attention", "")
            Spacer(Modifier.height(8.dp))
            AttentionCard(tasks.count { !it.done }, inputs.size)
        }
    }
}

@Composable
private fun LiveFieldMap(
    field: FarmField?,
    boundary: List<LatLng>,
    onFieldLocationSelected: (LatLng) -> Unit,
    onFieldBoundaryChanged: (List<LatLng>) -> Unit
) {
    val defaultCenter = LatLng(12.8797, 121.7740)
    val fieldLocation = remember(field?.latitude, field?.longitude) {
        if (field?.latitude != null && field.longitude != null) {
            LatLng(field.latitude, field.longitude)
        } else {
            null
        }
    }
    var drawingBoundary by remember { mutableStateOf(false) }
    var showFieldDetails by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            fieldLocation ?: defaultCenter,
            if (fieldLocation != null) 19f else 5.5f
        )
    }

    LaunchedEffect(fieldLocation) {
        if (fieldLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(fieldLocation, 19f),
                700
            )
        }
    }

    Card(
        Modifier.fillMaxWidth().height(340.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE5E8D8))
    ) {
        Box(Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.SATELLITE),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    tiltGesturesEnabled = true
                ),
                onMapClick = { point ->
                    if (drawingBoundary) {
                        onFieldBoundaryChanged(boundary + point)
                        if (fieldLocation == null) {
                            onFieldLocationSelected(point)
                        }
                    }
                },
                onMapLongClick = { point ->
                    if (!drawingBoundary) {
                        onFieldLocationSelected(point)
                    }
                }
            ) {
                if (boundary.size >= 3) {
                    Polygon(
                        points = boundary,
                        clickable = true,
                        fillColor = Color(0x664F7D45),
                        strokeColor = AgriGreen,
                        strokeWidth = 4f,
                        onClick = { showFieldDetails = true }
                    )
                }

                if (drawingBoundary) {
                    boundary.forEachIndexed { index, point ->
                        Marker(
                            state = rememberUpdatedMarkerState(position = point),
                            title = "Boundary point " + (index + 1)
                        )
                    }
                } else if (fieldLocation != null && boundary.size < 3) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = fieldLocation),
                        title = field?.name ?: "Farm Field",
                        snippet = (field?.crop ?: "Crop") + " · " + (field?.area ?: "")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xEFFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(9.dp).clip(CircleShape).background(AgriGreen)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "SATELLITE FIELD",
                        fontWeight = FontWeight.Bold,
                        color = AgriText,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        when {
                            boundary.size >= 3 -> "Field boundary saved · " + boundary.size + " points"
                            drawingBoundary -> "Tap each corner of the real field"
                            fieldLocation != null -> "Field pinned · ready to trace boundary"
                            else -> "Long-press the field to set its location"
                        },
                        color = AgriMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (drawingBoundary) {
                    Button(
                        onClick = {
                            drawingBoundary = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("Finish")
                    }
                } else {
                    Button(
                        onClick = {
                            drawingBoundary = true
                            onFieldBoundaryChanged(emptyList())
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text("Trace Field")
                    }
                }
            }

            if (boundary.size >= 3 && !drawingBoundary) {
                Text(
                    "Real field boundary · " + "%.2f".format(calculateApproxAreaHa(boundary)) + " ha",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEFFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = AgriText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showFieldDetails && field != null && boundary.size >= 3) {
                AlertDialog(
                    onDismissRequest = { showFieldDetails = false },
                    confirmButton = {
                        TextButton(onClick = { showFieldDetails = false }) {
                            Text("Close")
                        }
                    },
                    title = {
                        Text(field.name, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FieldDetailRow("Crop", field.crop)
                            FieldDetailRow("Mapped Area", "%.2f ha".format(calculateApproxAreaHa(boundary)))
                            FieldDetailRow("Recorded Area", field.area)
                            FieldDetailRow(
                                "Center",
                                if (fieldLocation != null)
                                    "%.6f, %.6f".format(fieldLocation.latitude, fieldLocation.longitude)
                                else "Not set"
                            )
                            FieldDetailRow("Health", "92% Healthy")
                            FieldDetailRow("Map", "Google Satellite")
                        }
                    },
                    shape = RoundedCornerShape(26.dp),
                    containerColor = AgriCard
                )
            }

            Text(
                if (drawingBoundary) "Tap corners → Finish"
                else "Google Satellite · Drag · Pinch · Rotate",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9FFFDF4))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                color = AgriText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FieldDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AgriMuted)
        Spacer(Modifier.width(16.dp))
        Text(value, color = AgriText, fontWeight = FontWeight.SemiBold)
    }
}

private fun calculateApproxAreaHa(points: List<LatLng>): Double {
    if (points.size < 3) return 0.0
    val avgLat = points.map { it.latitude }.average()
    val latMeters = 111_320.0
    val lonMeters = 111_320.0 * kotlin.math.cos(Math.toRadians(avgLat))
    var areaM2 = 0.0
    for (i in points.indices) {
        val a = points[i]
        val b = points[(i + 1) % points.size]
        val ax = a.longitude * lonMeters
        val ay = a.latitude * latMeters
        val bx = b.longitude * lonMeters
        val by = b.latitude * latMeters
        areaM2 += ax * by - bx * ay
    }
    return kotlin.math.abs(areaM2) / 2.0 / 10_000.0
}

@Composable
private fun StatCard(title: String, value: String, change: String, icon: String, background: Color) {
    Card(
        Modifier.width(150.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(title, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(change, color = AgriGreen, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HealthRing(percent: Int) {
    Box(Modifier.size(128.dp).clip(CircleShape).background(AgriGreenSoft), contentAlignment = Alignment.Center) {
        Box(Modifier.size(102.dp).clip(CircleShape).background(AgriCard), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(percent.toString() + "%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AgriGreen)
                Text("Good", color = AgriMuted)
            }
        }
    }
}

@Composable
private fun CropHealthRow(crop: String, health: String, field: String, dot: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(crop, fontWeight = FontWeight.Bold)
            Text(health, style = MaterialTheme.typography.bodySmall)
            Text(field, style = MaterialTheme.typography.bodySmall, color = AgriMuted)
        }
    }
}

@Composable
private fun AttentionCard(openTasks: Int, inventory: Int) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Farm status", fontWeight = FontWeight.Bold)
                Text(
                    if (openTasks == 0) "Everything looks good."
                    else openTasks.toString() + " task(s) need attention.",
                    color = AgriMuted
                )
            }
            Text("●", color = AgriGreen, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun FieldsScreen(padding: PaddingValues, fields: MutableList<FarmField>) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.padding(padding),
        containerColor = AgriCream,
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }, containerColor = AgriGreen, contentColor = Color.White) { Text("+") }
        }
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("Your Fields", "Manage crops, areas and field records.") }
            items(fields) { field ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(AgriGreenSoft), contentAlignment = Alignment.Center) { Text("🌾") }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(field.name, fontWeight = FontWeight.Bold)
                            Text(field.crop)
                            Text(field.area, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (showForm) {
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add field", fontWeight = FontWeight.Bold)
                            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Field name") })
                            OutlinedTextField(crop, { crop = it }, Modifier.fillMaxWidth(), label = { Text("Crop") })
                            OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Area") })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (name.isNotBlank()) {
                                        fields.add(FarmField(name.trim(), crop.ifBlank { "Unknown crop" }.trim(), area.ifBlank { "—" }.trim()))
                                        name = ""; crop = ""; area = ""; showForm = false
                                    }
                                }) { Text("Save Field") }
                                TextButton(onClick = { showForm = false }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsScreen(padding: PaddingValues, fields: List<FarmField>) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader("Farm Insights", "Simple signals to help you decide what to do next.") }
        item { InsightCard("Crop health", "85%", "Overall field condition is good.", "●") }
        item { InsightCard("Field coverage", fields.size.toString(), "registered field(s)", "▣") }
        item { InsightCard("Yield tracking", "Ready", "Add your harvest records to unlock trends.", "↗") }
        item { InsightCard("Smart alerts", "Coming next", "AgriVision will flag low stock and unusual farm activity.", "!") }
    }
}

@Composable
private fun InsightCard(title: String, value: String, description: String, icon: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(AgriGreenSoft), contentAlignment = Alignment.Center) {
                Text(icon, color = AgriGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TasksScreen(padding: PaddingValues, tasks: MutableList<FarmTask>) {
    Scaffold(modifier = Modifier.padding(padding), containerColor = AgriCream) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ScreenHeader("Farm Tasks", "Your daily farm work at a glance.") }
            items(tasks.indices.toList()) { index ->
                val task = tasks[index]
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(if (task.done) AgriGreen else AgriSage), contentAlignment = Alignment.Center) {
                            Text(if (task.done) "✓" else "!", color = if (task.done) Color.White else AgriGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold)
                            Text(task.date, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { tasks[index] = task.copy(done = !task.done) }) {
                            Text(if (task.done) "Done" else "Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader("Profile", "Farm settings and AgriVision tools.") }
        item { InfoCard("My Farm", "Farm profile, owner details and default units.") }
        item { InfoCard("Reports", "Daily, weekly and monthly farm performance.") }
        item { InfoCard("Backup & Restore", "Keep a local backup of your farm records.") }
        item { InfoCard("AgriVision", "See Your Farm. Know What To Do.") }
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
private fun SectionTitle(title: String, action: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        when {
            action == "Info" -> {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Information",
                    tint = AgriGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            action.isNotBlank() -> {
                Text(
                    action,
                    color = AgriGreen,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
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
