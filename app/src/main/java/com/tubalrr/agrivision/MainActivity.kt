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
            SectionTitle("Field Location", "Phone GPS")
            Spacer(Modifier.height(10.dp))
            LiveFieldMap(
                field = fields.firstOrNull(),
                onFieldLocationSelected = onFieldLocationSelected
            )
        }

        item {
            SectionTitle("Field Overview", "Live · Today")
            Spacer(Modifier.height(10.dp))
            FieldOverviewCard(field = fields.firstOrNull())
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
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationMessage by remember { mutableStateOf("Phone GPS not connected yet") }
    var loading by remember { mutableStateOf(false) }

    fun openGoogleMaps(latitude: Double, longitude: Double) {
        val uri = Uri.parse(
            "https://www.google.com/maps/@?api=1&map_action=map" +
                "&center=" + latitude + "%2C" + longitude +
                "&zoom=19&basemap=satellite"
        )
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun getPhoneLocation() {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            locationMessage = "Allow location permission to use phone GPS"
            loading = false
            return
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        val locationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        if (!locationEnabled) {
            locationMessage = "Turn on Location/GPS in phone settings"
            loading = false
            return
        }

        loading = true
        locationMessage = "Getting current phone location…"

        try {
            fun useLocation(location: Location, source: String) {
                loading = false
                currentLocation = location
                locationMessage = source + " · %.6f, %.6f".format(
                    location.latitude,
                    location.longitude
                )
                onFieldLocationSelected(location)
            }

            fun bestLastKnownLocation(): Location? {
                return locationManager.getProviders(true)
                    .mapNotNull { provider ->
                        locationManager.getLastKnownLocation(provider)
                    }
                    .minByOrNull { location -> location.accuracy.toDouble() }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val networkAvailable =
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                val gpsAvailable =
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                val firstProvider = when {
                    networkAvailable -> LocationManager.NETWORK_PROVIDER
                    gpsAvailable -> LocationManager.GPS_PROVIDER
                    else -> null
                }

                if (firstProvider == null) {
                    loading = false
                    locationMessage = "No location provider is available"
                    return
                }

                locationManager.getCurrentLocation(
                    firstProvider,
                    CancellationSignal(),
                    context.mainExecutor
                ) { location ->
                    if (location != null) {
                        useLocation(
                            location,
                            if (firstProvider == LocationManager.NETWORK_PROVIDER)
                                "Phone location"
                            else
                                "GPS locked"
                        )
                    } else {
                        val last = bestLastKnownLocation()
                        if (last != null) {
                            useLocation(last, "Last known location")
                        } else if (
                            firstProvider != LocationManager.GPS_PROVIDER &&
                            gpsAvailable
                        ) {
                            locationMessage = "Network location unavailable. Trying GPS…"
                            locationManager.getCurrentLocation(
                                LocationManager.GPS_PROVIDER,
                                CancellationSignal(),
                                context.mainExecutor
                            ) { gpsLocation ->
                                if (gpsLocation != null) {
                                    useLocation(gpsLocation, "GPS locked")
                                } else {
                                    loading = false
                                    locationMessage =
                                        "No location fix yet. Turn on Wi-Fi/mobile data or move near a window."
                                }
                            }
                        } else {
                            loading = false
                            locationMessage =
                                "No location fix yet. Turn on Wi-Fi/mobile data or move near a window."
                        }
                    }
                }
            } else {
                val last = bestLastKnownLocation()
                loading = false
                if (last != null) {
                    useLocation(last, "Phone location")
                } else {
                    locationMessage =
                        "No recent location. Turn on Wi-Fi/mobile data or move outdoors."
                }
            }
        } catch (_: SecurityException) {
            loading = false
            locationMessage = "Location permission is required"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted =
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) getPhoneLocation()
        else locationMessage = "Location permission was denied"
    }

    val savedLocation = remember(field?.latitude, field?.longitude) {
        if (field?.latitude != null && field.longitude != null) {
            Pair(field.latitude, field.longitude)
        } else null
    }

    Card(
        Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AgriGreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📍", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "PHONE GPS FIELD LOCATION",
                        color = AgriGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        locationMessage,
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fine || coarse) getPhoneLocation()
                            else permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        enabled = !loading,
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (loading) "Locating…" else "Use My Location")
                    }

                    OutlinedButton(
                        onClick = {
                            val location = currentLocation
                            val lat = location?.latitude ?: savedLocation?.first
                            val lng = location?.longitude ?: savedLocation?.second
                            if (lat != null && lng != null) {
                                openGoogleMaps(lat, lng)
                            } else {
                                locationMessage = "Get your phone location first"
                            }
                        },
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Open Satellite")
                    }
                }

                Text(
                    "Google Maps opens on the phone in satellite mode. No embedded Maps API key.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
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
