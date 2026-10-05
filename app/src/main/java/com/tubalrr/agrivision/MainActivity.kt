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
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
                    padding = padding,
                    fields = fields,
                    inputs = inputs,
                    tasks = tasks,
                    onFieldLocationSelected = { location ->
                        if (fields.isNotEmpty()) {
                            fields[0] = fields[0].copy(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            fieldPrefs.edit()
                                .putString("latitude", location.latitude.toString())
                                .putString("longitude", location.longitude.toString())
                                .apply()
                        }
                    }
                )
                1 -> FieldsScreen(
                    padding = padding,
                    fields = fields,
                    onOpenMap = { selected = 0 }
                )
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
    onFieldLocationSelected: (Location) -> Unit
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
            SectionTitle("Field Overview", "Live · Satellite")
            Spacer(Modifier.height(10.dp))
            LiveFieldMap(
                field = fields.firstOrNull(),
                onFieldLocationSelected = onFieldLocationSelected
            )
        }

        item {
            SectionTitle("Today's Attention", "")
            Spacer(Modifier.height(8.dp))
            AttentionCard(tasks.count { !it.done }, inputs.size)
        }
    }
}

@Composable
private fun FieldOverviewCard(field: FarmField?) {
    val context = LocalContext.current
    val hasLocation = field?.latitude != null && field.longitude != null

    fun openSatellite() {
        val lat = field?.latitude ?: return
        val lng = field.longitude ?: return
        val uri = Uri.parse(
            "https://www.google.com/maps/@?api=1&map_action=map" +
                "&center=" + lat + "%2C" + lng +
                "&zoom=19&basemap=satellite"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDEE5CF))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(AgriGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        tint = AgriGreen,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        field?.name ?: "North Field",
                        fontWeight = FontWeight.Bold,
                        color = AgriText
                    )
                    Text(
                        (field?.crop ?: "Rice") + " · " + (field?.area ?: "1.0 ha"),
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    if (hasLocation) "LOCATED" else "NOT SET",
                    color = if (hasLocation) AgriGreen else AgriMuted,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFB7C69A)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        tint = AgriGreen,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (hasLocation) "Field location saved"
                        else "No field location yet",
                        color = AgriText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (hasLocation)
                            "%.6f, %.6f".format(field!!.latitude, field.longitude)
                        else
                            "Press Use My Location above",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Crop health", color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                    Text("92% Healthy", color = AgriGreen, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { if (hasLocation) openSatellite() },
                    enabled = hasLocation,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Map,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Satellite")
                }
            }
        }
    }
}

@Composable
private fun LiveFieldMap(
    field: FarmField?,
    onFieldLocationSelected: (Location) -> Unit
) {
    val context = LocalContext.current
    var pendingLocation by remember { mutableStateOf<Location?>(null) }
    var locationMessage by remember { mutableStateOf("Farm location not set") }
    var settingLocation by remember { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val savedLat = field?.latitude
    val savedLng = field?.longitude

    fun selectLocation(latitude: Double, longitude: Double) {
        val location = Location("map").apply {
            this.latitude = latitude
            this.longitude = longitude
            accuracy = 1f
        }
        pendingLocation = location
        settingLocation = true
        locationMessage = "Location selected · tap Save Farm Location"
    }

    fun saveLocation() {
        val location = pendingLocation ?: return
        settingLocation = false
        pendingLocation = null
        locationMessage = "Farm location saved · %.6f, %.6f".format(
            location.latitude, location.longitude
        )
        onFieldLocationSelected(location)
        webViewRef?.evaluateJavascript(
            "setFarmLocation(" + location.latitude + ", " + location.longitude + ", true);",
            null
        )
    }

    val mapBridge = remember {
        object {
            @JavascriptInterface
            fun selectLocation(latitude: Double, longitude: Double) {
                context.mainExecutor.execute {
                    selectLocation(latitude, longitude)
                }
            }
        }
    }

    LaunchedEffect(savedLat, savedLng, mapReady) {
        if (mapReady && savedLat != null && savedLng != null) {
            locationMessage = "Saved farm location"
            webViewRef?.evaluateJavascript(
                "setFarmLocation($savedLat, $savedLng, true);",
                null
            )
        }
    }

    val html = remember {
        """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
          <style>
            html, body, #map { height:100%; width:100%; margin:0; padding:0; }
            body { overflow:hidden; font-family:Arial,sans-serif; }
            .leaflet-control-attribution { font-size:9px; }
            .leaflet-control-layers { font-size:12px; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script>
            const map = L.map('map', {
              center: [12.8797, 121.7740],
              zoom: 6,
              zoomControl: true,
              attributionControl: true
            });

            const satellite = L.tileLayer(
              'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
              { maxZoom: 19, attribution: 'Tiles © Esri' }
            ).addTo(map);

            const streets = L.tileLayer(
              'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
              { maxZoom: 19, attribution: '© OpenStreetMap contributors' }
            );

            L.control.layers({
              'Satellite': satellite,
              'Map': streets
            }, null, { position:'topright', collapsed:true }).addTo(map);

            let marker = null;
            let selectionEnabled = false;

            function setSelectionMode(enabled) {
              selectionEnabled = enabled;
              document.body.style.cursor = enabled ? 'crosshair' : 'default';
            }

            function setFarmLocation(lat, lng, saved) {
              const point = [lat, lng];
              if (marker) marker.setLatLng(point);
              else marker = L.marker(point).addTo(map);
              marker.bindPopup(saved ? 'Saved Farm Location' : 'Selected Farm Location');
              map.setView(point, 18, { animate:true });
              marker.openPopup();
            }

            function choosePoint(e) {
              if (selectionEnabled && window.AndroidMap) {
                window.AndroidMap.selectLocation(e.latlng.lat, e.latlng.lng);
                setFarmLocation(e.latlng.lat, e.latlng.lng, false);
              }
            }

            map.on('click', choosePoint);
            map.on('contextmenu', choosePoint);

            window.setSelectionMode = setSelectionMode;
            window.setFarmLocation = setFarmLocation;
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        Modifier.fillMaxWidth().height(350.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp)),
                factory = {
                    WebView(context).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                mapReady = true
                                view?.evaluateJavascript("setSelectionMode($settingLocation);", null)
                                if (savedLat != null && savedLng != null) {
                                    view?.evaluateJavascript(
                                        "setFarmLocation($savedLat, $savedLng, true);", null
                                    )
                                }
                            }
                        }
                        addJavascriptInterface(mapBridge, "AndroidMap")
                        loadDataWithBaseURL(
                            "https://agrivision.local/",
                            html, "text/html", "UTF-8", null
                        )
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (mapReady) {
                        webView.evaluateJavascript("setSelectionMode($settingLocation);", null)
                    }
                }
            )

            Row(
                Modifier.align(Alignment.TopStart).padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xEEFFFDF4))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Map, contentDescription = null, tint = AgriGreen, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(field?.name ?: "Farm Field", fontWeight = FontWeight.Bold, color = AgriText, style = MaterialTheme.typography.labelLarge)
                    Text(locationMessage, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                Modifier.align(Alignment.BottomStart).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (pendingLocation != null) {
                            saveLocation()
                        } else {
                            settingLocation = true
                            locationMessage = "Tap or long-press the exact farm location"
                            webViewRef?.evaluateJavascript("setSelectionMode(true);", null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
                ) {
                    Text(if (pendingLocation != null) "Save Farm Location" else "Set Farm Location")
                }

                OutlinedButton(
                    onClick = {
                        if (savedLat != null && savedLng != null) {
                            webViewRef?.evaluateJavascript(
                                "setFarmLocation($savedLat, $savedLng, true);", null
                            )
                        }
                    },
                    enabled = savedLat != null && savedLng != null,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xEEFFFDF4))
                ) {
                    Text("Farm")
                }
            }

            Text(
                if (settingLocation)
                    "SELECT MODE · Tap or long-press the exact farm location"
                else
                    "Satellite · Drag · Pinch · Zoom",
                Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEEFFFDF4))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                color = AgriText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
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
private fun FieldsScreen(
    padding: PaddingValues,
    fields: MutableList<FarmField>,
    onOpenMap: () -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.padding(padding),
        containerColor = AgriCream,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showForm = true },
                containerColor = AgriGreen,
                contentColor = Color.White
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("Your Fields", "Manage crops, areas and field records.") }
            items(fields) { field ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AgriCard)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AgriGreenSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (field.latitude != null && field.longitude != null) "📍" else "🌾",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(field.name, fontWeight = FontWeight.Bold)
                                Text(field.crop)
                                Text(
                                    field.area,
                                    color = AgriMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (field.latitude != null && field.longitude != null)
                                            AgriGreenSoft
                                        else
                                            AgriSage
                                    )
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    if (field.latitude != null && field.longitude != null) "Mapped" else "Not mapped",
                                    color = AgriGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        HorizontalDivider(color = AgriLine)

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Satellite location", color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    if (field.latitude != null && field.longitude != null)
                                        "%.5f, %.5f".format(field.latitude, field.longitude)
                                    else
                                        "Add the real field location",
                                    color = AgriText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenMap,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Open Map")
                            }
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
