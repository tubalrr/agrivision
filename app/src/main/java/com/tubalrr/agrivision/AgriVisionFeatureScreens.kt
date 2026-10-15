package com.tubalrr.agrivision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
internal fun rememberInternetAvailable(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    var online by remember { mutableStateOf(hasFeatureInternet(manager)) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = hasFeatureInternet(manager) }
            override fun onLost(network: Network) { online = hasFeatureInternet(manager) }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return online
}

private fun hasFeatureInternet(manager: ConnectivityManager): Boolean {
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
internal fun OfflineBadge(language: String) {
    Surface(shape = RoundedCornerShape(50), color = if (AgriIsDark) AgriSurfaceVariant else MaterialTheme.colorScheme.errorContainer) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
            Text("Offline", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun FeatureScreenHeader(title: String, subtitle: String, language: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(featureText(language, "‹ Higit pang tools", "‹ More tools"))
        }
        Text(title, color = AgriText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        if (subtitle.isNotBlank()) Text(subtitle, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun MoreHubScreen(
    padding: PaddingValues,
    farm: FarmRecord,
    language: String,
    online: Boolean,
    onOpenReports: () -> Unit,
    onOpenScan: () -> Unit,
    onOpenPrices: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenHelp: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(featureText(language, "DAGDAG NA MGA TOOL", "MORE TOOLS"), color = AgriGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    Text(featureText(language, "Mga tool para sa sakahan", "Farm tools"), color = AgriText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(farm.farmName.ifBlank { featureText(language, "Walang pangalan ng sakahan", "Farm not named yet") }, color = AgriMuted)
                }
                if (!online) OfflineBadge(language)
            }
        }
        item { FeatureActionCard(featureText(language, "Pest at Disease Scan", "Pest & Disease Scan"), featureText(language, "Mag-upload o kumuha ng larawan ng dahon.", "Upload or take a leaf photo."), Icons.Outlined.CameraAlt, onOpenScan) }
        item { FeatureActionCard("Market Prices", featureText(language, "Presyo ng palay, mais, at gulay.", "Palay, corn, and vegetable prices."), Icons.Outlined.CurrencyExchange, onOpenPrices) }
        item { FeatureActionCard(featureText(language, "Panahon sa Sakahan", "Farm Weather"), featureText(language, "Kasalukuyang panahon, 7-day forecast, at risk indicators.", "Current conditions, 7-day forecast, and risk indicators."), Icons.Outlined.Cloud, onOpenWeather) }
        item { FeatureActionCard("Planting Calendar & Soil Guide", featureText(language, "Mga petsa ng pagtatanim, pagpapataba, at pag-aani.", "Planting, fertilizer, and harvest recommendations."), Icons.Outlined.CalendarMonth, onOpenCalendar) }
        item { FeatureActionCard("Reports", featureText(language, "Production, finance, farm reports, at exports.", "Production, finance, farm reports, and exports."), Icons.Outlined.Assessment, onOpenReports) }
        item { FeatureActionCard("Help / About / Settings", featureText(language, "FAQ, privacy, terms, wika, at appearance.", "FAQ, privacy, terms, language, and appearance."), Icons.Outlined.HelpOutline, onOpenHelp) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Offline-first", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(featureText(language, "Nananatili sa device ang farm, task, at report records. Naka-cache rin sa Room ang scan history, forecast, at sample market prices.", "Farm, task, and report records remain on-device. Scan history, weather forecasts, and sample market prices are cached in Room."), color = AgriText, style = MaterialTheme.typography.bodySmall)
                    Text(featureText(language, "Wala pang cloud sync server; mare-refresh ang forecast kapag bumalik ang internet.", "No cloud sync server is connected yet; the forecast refreshes when internet returns."), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FeatureActionCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = AgriGreenSoft) {
                Box(Modifier.padding(11.dp)) { Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = AgriGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun interface MarketPriceProvider {
    suspend fun load(): List<MarketPriceEntity>
}

internal object MockMarketPriceProvider : MarketPriceProvider {
    override suspend fun load(): List<MarketPriceEntity> = mockMarketPrices()
}

private fun mockMarketPrices(): List<MarketPriceEntity> {
    val now = System.currentTimeMillis()
    return listOf(
        MarketPriceEntity("palay-dry", "Palay (dry)", "Grains", 19.50, "DOWN", -1.2, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("corn-yellow", "Yellow corn", "Grains", 16.80, "UP", 2.4, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("corn-white", "White corn", "Grains", 18.00, "STABLE", 0.0, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("tomato", "Tomato", "Vegetables", 44.00, "UP", 5.6, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("onion", "Red onion", "Vegetables", 92.00, "DOWN", -3.2, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("cabbage", "Cabbage", "Vegetables", 37.00, "DOWN", -1.7, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("eggplant", "Eggplant", "Vegetables", 41.00, "UP", 3.5, source = "MOCK DATA", updatedAt = now),
        MarketPriceEntity("bitter-gourd", "Bitter gourd", "Vegetables", 54.00, "STABLE", 0.0, source = "MOCK DATA", updatedAt = now)
    )
}

@Composable
internal fun MarketPricesScreen(
    padding: PaddingValues,
    farm: FarmRecord,
    language: String,
    onBack: () -> Unit,
    provider: MarketPriceProvider = MockMarketPriceProvider
) {
    val context = LocalContext.current
    val dao = remember(context) { AgriExtrasDatabase.getInstance(context).extrasDao() }
    val prices by dao.observeMarketPrices().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val online = rememberInternetAvailable()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(dao, provider) {
        if (dao.countMarketPrices() == 0) dao.saveMarketPrices(provider.load())
    }
    val visible = remember(prices, query, category) {
        prices.filter { (category == "All" || it.category == category) && (query.isBlank() || it.commodity.contains(query.trim(), ignoreCase = true)) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureScreenHeader(featureText(language, "Presyo sa Pamilihan", "Market Prices"), featureText(language, "Sample prices per kilogram; hindi pa real-time market quotes.", "Sample prices per kilogram; these are not live market quotes."), language, onBack) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Farm: " + farm.farmName.ifBlank { farm.farmId }, color = AgriMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                if (!online) OfflineBadge(language)
            }
        }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text(featureText(language, "Maghanap ng produkto", "Search commodity")) }, singleLine = true) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("All", "Grains", "Vegetables").forEach { option ->
                    FilterChip(selected = category == option, onClick = { category = option }, label = { Text(when(option) { "All" -> featureText(language, "Lahat", "All"); "Grains" -> featureText(language, "Butil", "Grains"); else -> featureText(language, "Gulay", "Vegetables") }) })
                }
                OutlinedButton(enabled = !isRefreshing, onClick = {
                    scope.launch { isRefreshing = true; try { dao.clearMarketPrices(); dao.saveMarketPrices(provider.load()) } finally { isRefreshing = false } }
                }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    if (isRefreshing) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(featureText(language, "I-refresh sample", "Refresh sample"))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = if (AgriIsDark) AgriSurfaceVariant else AgriGoldSoft)) {
                Text(featureText(language, "Demo data lang ito. Palitan ng opisyal na API/source bago gamitin sa pagbebenta o pagbili.", "Demo data only. Replace with an official API/source before using it for trading decisions."), Modifier.padding(12.dp), color = AgriText, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (visible.isEmpty()) item { InfoCard(featureText(language, "Walang tugmang produkto", "No matching commodities"), featureText(language, "Baguhin ang search o filter.", "Change the search or filter.")) }
        else items(visible, key = { it.priceId }) { row ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(row.commodity, color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium); Text(row.category, color = AgriMuted, style = MaterialTheme.typography.labelSmall) }
                        Text("₱" + String.format(Locale.US, "%.2f", row.pricePerKg) + "/" + row.unit, color = AgriGreen, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        val trendLabel = when (row.trend) { "UP" -> "↑ " + String.format(Locale.US, "%.1f%%", row.changePercent); "DOWN" -> "↓ " + String.format(Locale.US, "%.1f%%", kotlin.math.abs(row.changePercent)); else -> featureText(language, "Walang pagbabago", "Stable") }
                        Text(trendLabel, color = when(row.trend) { "UP" -> AgriGreen; "DOWN" -> AgriDanger; else -> AgriMuted }, fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(row.updatedAt)), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Source: " + row.source, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private data class FeatureCoordinates(val latitude: Double, val longitude: Double)

private fun resolveFeatureCoordinates(farm: FarmRecord, fields: List<FieldRecord>): FeatureCoordinates? {
    fields.firstOrNull { f ->
        val lat = f.latitude; val lon = f.longitude
        lat != null && lon != null && lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
    }?.let { return FeatureCoordinates(it.latitude!!, it.longitude!!) }
    val points = farm.boundaryPoints
    if (points.isEmpty()) return null
    val lat = points.map { it.latitude }.average()
    val lon = points.map { it.longitude }.average()
    return if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) FeatureCoordinates(lat, lon) else null
}

private data class FeatureForecastDay(val date: String, val code: Int, val low: Double, val high: Double, val rainChance: Int, val rainMm: Double, val maxWindKmh: Double)
private data class FeatureWeatherSnapshot(val temperature: Double, val feelsLike: Double, val humidity: Int, val rainNowMm: Double, val windNowKmh: Double, val code: Int, val days: List<FeatureForecastDay>, val fetchedAt: Long)

private fun parseFeatureWeather(jsonText: String, fetchedAt: Long): FeatureWeatherSnapshot {
    val root = JSONObject(jsonText)
    val current = root.getJSONObject("current")
    val daily = root.getJSONObject("daily")
    val dates = daily.getJSONArray("time")
    val codes = daily.getJSONArray("weather_code")
    val highs = daily.getJSONArray("temperature_2m_max")
    val lows = daily.getJSONArray("temperature_2m_min")
    val probabilities = daily.getJSONArray("precipitation_probability_max")
    val rain = daily.getJSONArray("precipitation_sum")
    val winds = daily.optJSONArray("wind_speed_10m_max")
    val days = (0 until minOf(7, dates.length())).map { i ->
        FeatureForecastDay(dates.getString(i), codes.optInt(i, -1), lows.optDouble(i, 0.0), highs.optDouble(i, 0.0), probabilities.optInt(i, -1), rain.optDouble(i, 0.0), winds?.optDouble(i, 0.0) ?: 0.0)
    }
    return FeatureWeatherSnapshot(current.optDouble("temperature_2m", 0.0), current.optDouble("apparent_temperature", 0.0), current.optInt("relative_humidity_2m", 0), current.optDouble("precipitation", 0.0), current.optDouble("wind_speed_10m", 0.0), current.optInt("weather_code", -1), days, fetchedAt)
}

private suspend fun fetchFeatureWeather(coordinates: FeatureCoordinates): String = withContext(Dispatchers.IO) {
    val query = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=" + URLEncoder.encode(coordinates.latitude.toString(), "UTF-8") +
        "&longitude=" + URLEncoder.encode(coordinates.longitude.toString(), "UTF-8") +
        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,wind_speed_10m_max" +
        "&forecast_days=7&timezone=auto"
    val connection = (URL(query).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"; connectTimeout = 9000; readTimeout = 9000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "AgriVision-Android")
    }
    try {
        check(connection.responseCode in 200..299) { "Weather service unavailable" }
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally { connection.disconnect() }
}

private fun featureWeatherCodeLabel(code: Int, language: String): String = when (code) {
    0 -> featureText(language, "Maaliwalas", "Clear sky")
    1 -> featureText(language, "Kadalasang maaliwalas", "Mainly clear")
    2 -> featureText(language, "Bahagyang maulap", "Partly cloudy")
    3 -> featureText(language, "Maulap", "Overcast")
    45, 48 -> featureText(language, "Makapal na hamog", "Fog")
    51, 53, 55 -> featureText(language, "Ambon", "Drizzle")
    61, 63, 65, 80, 81, 82 -> featureText(language, "Ulan", "Rain")
    95, 96, 99 -> featureText(language, "Kulog at kidlat", "Thunderstorm")
    else -> featureText(language, "Iba pang kondisyon", "Variable conditions")
}

private fun featureWeatherWarnings(snapshot: FeatureWeatherSnapshot, language: String): List<Pair<String, String>> {
    val warnings = mutableListOf<Pair<String, String>>()
    if (snapshot.windNowKmh >= 60.0 || snapshot.days.any { it.maxWindKmh >= 60.0 }) {
        warnings += featureText(language, "Malakas na hangin / posibleng bagyo", "Strong-wind / possible storm conditions") to
            featureText(language, "May forecast na hangin na 60 km/h o higit pa. Hindi ito opisyal na typhoon bulletin; tingnan ang PAGASA advisories.", "Forecast wind reaches 60 km/h or more. This is not an official typhoon bulletin; check PAGASA advisories.")
    }
    val wet = snapshot.days.firstOrNull { it.rainMm >= 50.0 || (it.rainChance >= 85 && it.rainMm >= 20.0) }
    if (wet != null) {
        warnings += featureText(language, "Babala sa malakas na ulan", "Heavy-rain planning warning") to
            featureText(language, "May mataas na ulan na forecast sa ", "High rainfall is forecast on ") + wet.date + ". " +
            featureText(language, "Suriin ang drainage at lokal na babala.", "Check drainage and official local advisories.")
    }
    val rainTotal = snapshot.days.sumOf { it.rainMm }
    val maxHeat = snapshot.days.maxOfOrNull { it.high } ?: snapshot.temperature
    if (rainTotal < 5.0 && maxHeat >= 32.0) {
        warnings += featureText(language, "Posibleng dry spell", "Possible dry-spell conditions") to
            featureText(language, "Mas mababa sa 5 mm ang forecast na ulan sa 7 araw at mainit. Hindi ito opisyal na drought declaration.", "Forecast rainfall is under 5 mm over 7 days with high temperatures. This is not an official drought declaration.")
    }
    return warnings
}

@Composable
internal fun WeatherScreen(padding: PaddingValues, farm: FarmRecord, fields: List<FieldRecord>, language: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember(context) { AgriExtrasDatabase.getInstance(context).extrasDao() }
    val coordinates = remember(farm.boundaryPoints, fields) { resolveFeatureCoordinates(farm, fields) }
    val online = rememberInternetAvailable()
    val scope = rememberCoroutineScope()
    var cache by remember { mutableStateOf<WeatherCacheEntity?>(null) }
    var snapshot by remember { mutableStateOf<FeatureWeatherSnapshot?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    suspend fun refreshWeather() {
        if (!online) {
            error = featureText(language, "Offline ka. Ipinapakita ang huling naka-cache na forecast.", "You are offline. Showing the last cached forecast.")
            return
        }
        if (coordinates == null) {
            error = featureText(language, "Mag-save muna ng GPS point sa field o farm boundary.", "Save a GPS point to a field or farm boundary first.")
            return
        }
        loading = true
        error = ""
        try {
            val raw = fetchFeatureWeather(coordinates)
            val fetchedAt = System.currentTimeMillis()
            val parsed = parseFeatureWeather(raw, fetchedAt)
            require(parsed.days.size == 7) { "Incomplete 7-day forecast" }
            val row = WeatherCacheEntity(farm.farmId, coordinates.latitude, coordinates.longitude, raw, fetchedAt)
            dao.saveWeatherCache(row)
            cache = row
            snapshot = parsed
        } catch (_: Exception) {
            error = featureText(language, "Hindi makuha ang weather. Subukan ulit kapag may internet.", "Weather unavailable. Try again when internet is available.")
        } finally { loading = false }
    }

    LaunchedEffect(farm.farmId, coordinates, online) {
        val saved = withContext(Dispatchers.IO) { dao.getWeatherCache(farm.farmId) }
        if (saved != null) {
            cache = saved
            val matches = coordinates == null || (kotlin.math.abs(saved.latitude - coordinates.latitude) < 0.001 && kotlin.math.abs(saved.longitude - coordinates.longitude) < 0.001)
            if (matches) snapshot = runCatching { parseFeatureWeather(saved.payloadJson, saved.fetchedAt) }.getOrNull()
        }
        if (online && coordinates != null) refreshWeather()
        else if (!online && saved != null) error = featureText(language, "Offline mode: huling naka-cache na forecast.", "Offline mode: last cached forecast.")
    }

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureScreenHeader(featureText(language, "Panahon sa Sakahan", "Farm Weather"), featureText(language, "7-day forecast para sa naka-save na lokasyon ng farm.", "7-day forecast for the saved farm location."), language, onBack) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(farm.farmName.ifBlank { farm.farmId }, fontWeight = FontWeight.Bold)
                    Text(if (coordinates == null) featureText(language, "GPS point ay hindi pa naka-set", "GPS point not set yet") else String.format(Locale.US, "%.4f, %.4f", coordinates.latitude, coordinates.longitude), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (!online) OfflineBadge(language)
                OutlinedButton(enabled = !loading, onClick = { scope.launch { refreshWeather() } }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (coordinates == null && snapshot == null) {
            item { InfoCard(featureText(language, "Kailangan ang lokasyon", "Location required"), featureText(language, "Ilagay ang GPS coordinates sa Map → Fields o mag-save ng farm boundary. Hindi hihinuha ng app ang lokasyon mula sa text address.", "Enter GPS coordinates under Map → Fields or save a farm boundary. The app will not infer a location from the address text.")) }
        }
        if (error.isNotBlank()) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = if (AgriIsDark) AgriSurfaceVariant else AgriGoldSoft)) {
                Text(error, Modifier.padding(12.dp), color = AgriText, style = MaterialTheme.typography.bodySmall)
            }
        }
        val value = snapshot
        if (value != null) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(String.format(Locale.US, "%.1f", value.temperature) + "°C", style = MaterialTheme.typography.headlineLarge, color = AgriGreen, fontWeight = FontWeight.ExtraBold)
                        Text(featureWeatherCodeLabel(value.code, language), color = AgriText, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            WeatherMetric(featureText(language, "Parang init", "Feels like"), String.format(Locale.US, "%.1f", value.feelsLike) + "°", Modifier.weight(1f))
                            WeatherMetric("Humidity", value.humidity.toString() + "%", Modifier.weight(1f))
                            WeatherMetric(featureText(language, "Hangin", "Wind"), String.format(Locale.US, "%.1f", value.windNowKmh) + " km/h", Modifier.weight(1f))
                        }
                        Text(featureText(language, "Ulan ngayon: ", "Rain now: ") + String.format(Locale.US, "%.1f mm", value.rainNowMm), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                        Text(featureText(language, "Na-update: ", "Updated: ") + SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(value.fetchedAt)) + " · Open-Meteo", color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item { Text("7-Day Forecast", color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }
            items(value.days, key = { it.date }) { day ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.width(56.dp)) {
                            Text(runCatching { LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE", Locale.US)) }.getOrDefault(day.date), color = AgriText, fontWeight = FontWeight.Bold)
                            Text(day.date.takeLast(5), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(featureWeatherCodeLabel(day.code, language), color = AgriText, style = MaterialTheme.typography.bodyMedium)
                            Text(featureText(language, "Ulan ", "Rain ") + (if (day.rainChance in 0..100) day.rainChance.toString() + "%" else "—") + " · " + String.format(Locale.US, "%.1f mm", day.rainMm), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(day.high.toInt().toString() + "° / " + day.low.toInt().toString() + "°", color = AgriGreen, fontWeight = FontWeight.ExtraBold)
                            Text(String.format(Locale.US, "%.0f km/h", day.maxWindKmh), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            item { Text(featureText(language, "Mga risk indicator", "Risk indicators"), color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }
            val warnings = featureWeatherWarnings(value, language)
            if (warnings.isEmpty()) item { InfoCard(featureText(language, "Walang risk threshold na na-detect", "No heuristic risk threshold triggered"), featureText(language, "Tingnan pa rin ang opisyal na PAGASA bulletins at aktuwal na kondisyon sa bukid.", "Continue checking official PAGASA bulletins and actual farm conditions.")) }
            else items(warnings) { pair ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (AgriIsDark) AgriSurfaceVariant else AgriGoldSoft)) {
                    Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Outlined.WarningAmber, null, tint = AgriWarning)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(pair.first, color = AgriText, fontWeight = FontWeight.Bold)
                            Text(pair.second, color = AgriText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item { Text(featureText(language, "Ang thresholds ay planning indicators lang, hindi official weather warnings. Sundin ang PAGASA.", "Risk thresholds are planning indicators, not official warnings. Follow PAGASA advisories."), color = AgriMuted, style = MaterialTheme.typography.labelSmall) }
        }
        if (snapshot == null && loading) item { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        if (cache != null && !online) item { Text(featureText(language, "Ipinakita mula sa local Room cache. Walang ibang farm data na ipinapadala sa weather provider.", "Loaded from the local Room cache. No other farm data is sent to the weather provider."), color = AgriMuted, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun WeatherMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier.background(AgriSurfaceVariant, RoundedCornerShape(12.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
        Text(value, color = AgriText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

internal data class LeafDetectionResult(val diagnosis: String, val confidence: Int, val severity: String, val treatment: String)

internal fun interface LeafDiseaseDetector {
    suspend fun analyze(context: Context, photoUri: Uri, language: String): LeafDetectionResult
}

internal object MockLeafDiseaseDetector : LeafDiseaseDetector {
    override suspend fun analyze(context: Context, photoUri: Uri, language: String): LeafDetectionResult =
        mockDetectLeaf(photoUri.toString(), language)
}

private fun mockDetectLeaf(imageKey: String, language: String): LeafDetectionResult {
    val results = if (language == "en") listOf(
        LeafDetectionResult("Possible fungal leaf spot", 86, "Moderate", "Isolate affected plants when feasible, remove heavily infected leaves, improve airflow, and consult a local agriculturist before applying fungicide."),
        LeafDetectionResult("Possible leaf blight", 81, "High", "Check lesion spread, avoid wetting foliage overnight, clean tools, and confirm the cause before choosing treatment."),
        LeafDetectionResult("Possible bacterial leaf spot", 79, "Moderate", "Avoid handling wet foliage, remove severely affected material where appropriate, and ask an agricultural technician to confirm the cause.")
    ) else listOf(
        LeafDetectionResult("Posibleng fungal leaf spot", 86, "Katamtaman", "Ihiwalay ang apektadong halaman kung maaari, alisin ang matinding infected leaves, panatilihing maaliwalas ang tanim, at kumonsulta sa agriculturist bago gumamit ng fungicide."),
        LeafDetectionResult("Posibleng leaf blight", 81, "Mataas", "Suriin ang pagkalat ng lesions, linisin ang tools, at kumpirmahin ang sanhi bago pumili ng treatment."),
        LeafDetectionResult("Posibleng bacterial leaf spot", 79, "Katamtaman", "Iwasan ang paghawak kapag basa ang dahon at humingi ng diagnosis sa agricultural technician; maaaring magkapareho ang hitsura ng iba't ibang sakit.")
    )
    return results[Math.floorMod(imageKey.hashCode(), results.size)]
}

private suspend fun copyPhotoToPrivateStorage(context: Context, source: Uri): Uri? = withContext(Dispatchers.IO) {
    val input = context.contentResolver.openInputStream(source) ?: return@withContext null
    val dir = File(context.filesDir, "leaf-scans")
    if (!dir.exists() && !dir.mkdirs()) return@withContext null
    val file = File(dir, "leaf-" + UUID.randomUUID() + ".jpg")
    try {
        input.use { src ->
            FileOutputStream(file).use { dest ->
                val buffer = ByteArray(8 * 1024)
                var total = 0L
                while (true) {
                    val count = src.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 12L * 1024L * 1024L) { "Image is larger than the 12 MB scan limit." }
                    dest.write(buffer, 0, count)
                }
            }
        }
        Uri.fromFile(file)
    } catch (_: Exception) { file.delete(); null }
}

private suspend fun saveBitmapToPrivateStorage(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "leaf-scans")
    if (!dir.exists() && !dir.mkdirs()) return@withContext null
    val file = File(dir, "leaf-" + UUID.randomUUID() + ".jpg")
    try {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(file)
    } catch (_: Exception) { file.delete(); null }
}

private suspend fun decodePrivatePhoto(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val path = uri.path
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (uri.scheme.equals("file", true)) {
            BitmapFactory.decodeFile(path, bounds)
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 1280 || bounds.outHeight / sampleSize > 1280) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        if (uri.scheme.equals("file", true)) BitmapFactory.decodeFile(path, options)
        else context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

@Composable
internal fun PestDetectionScreen(
    padding: PaddingValues,
    farm: FarmRecord,
    crops: List<CropRecord>,
    language: String,
    onBack: () -> Unit,
    detector: LeafDiseaseDetector = MockLeafDiseaseDetector
) {
    val context = LocalContext.current
    val dao = remember(context) { AgriExtrasDatabase.getInstance(context).extrasDao() }
    val scope = rememberCoroutineScope()
    val history by dao.observeScanHistory(farm.farmId).collectAsState(initial = emptyList())
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<LeafDetectionResult?>(null) }
    var selectedCropId by remember { mutableStateOf(crops.firstOrNull()?.cropId.orEmpty()) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var savedScanId by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) scope.launch {
            val stored = saveBitmapToPrivateStorage(context, bitmap)
            if (stored != null) {
                imageUri = stored; imageBitmap = bitmap; result = null; savedScanId = null; message = ""
            } else message = featureText(language, "Hindi na-save ang larawan.", "Unable to save the photo.")
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else message = featureText(language, "Kailangan ang camera permission para kumuha ng larawan.", "Camera permission is needed to take a photo.")
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selected ->
        if (selected != null) scope.launch {
            val stored = copyPhotoToPrivateStorage(context, selected)
            if (stored != null) {
                imageUri = stored; imageBitmap = decodePrivatePhoto(context, stored); result = null; savedScanId = null; message = ""
            } else message = featureText(language, "Hindi mabasa ang napiling larawan.", "Could not read the selected photo.")
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureScreenHeader("Pest & Disease Scan", featureText(language, "Mock detector muna; hindi pa ito trained ML diagnosis.", "Mock detector for now; this is not a trained ML diagnosis."), language, onBack) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(modifier = Modifier.weight(1f), onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null)
                            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) { Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(featureText(language, "Kumuha", "Camera")) }
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = { galleryLauncher.launch("image/*") }) { Icon(Icons.Outlined.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text(featureText(language, "Mag-upload", "Upload")) }
                    }
                    if (imageBitmap != null) Image(bitmap = imageBitmap!!.asImageBitmap(), contentDescription = featureText(language, "Larawan ng dahon", "Leaf photo"), modifier = Modifier.fillMaxWidth().height(220.dp).border(1.dp, AgriLine, RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    else Box(Modifier.fillMaxWidth().height(150.dp).background(AgriSurfaceVariant, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Outlined.Spa, null, tint = AgriGreen, modifier = Modifier.size(34.dp))
                            Text(featureText(language, "Kumuha o pumili ng larawan ng dahon", "Take or choose a leaf photo"), color = AgriMuted)
                        }
                    }
                    Text(featureText(language, "Iugnay sa crop (optional)", "Link to crop (optional)"), color = AgriText, fontWeight = FontWeight.Bold)
                    if (crops.isEmpty()) Text(featureText(language, "Wala pang naka-register na crop. Maaari pa ring i-save sa farm.", "No crops registered yet. You can still save the scan to the farm."), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        crops.forEach { crop -> FilterChip(selected = selectedCropId == crop.cropId, onClick = { selectedCropId = crop.cropId }, label = { Text(crop.name + " · " + crop.crop, maxLines = 1) }) }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = imageUri != null && !scanning,
                        onClick = {
                            scope.launch {
                                val photo = imageUri
                                if (photo == null) {
                                    message = featureText(language, "Pumili muna ng larawan.", "Choose a photo first.")
                                    return@launch
                                }
                                scanning = true
                                result = null
                                savedScanId = null
                                message = ""
                                try {
                                    delay(250)
                                    result = detector.analyze(context, photo, language)
                                } catch (_: Exception) {
                                    message = featureText(language, "Hindi masuri ang larawan. Subukan muli.", "The image could not be analyzed. Try again.")
                                } finally {
                                    scanning = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (scanning) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text(featureText(language, "Suriin ang dahon", "Analyze leaf"))
                    }
                    if (message.isNotBlank()) Text(message, color = AgriWarning, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        val detection = result
        if (detection != null) item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(featureText(language, "Posibleng resulta", "Possible result"), color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(detection.diagnosis, color = AgriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Mock confidence: " + detection.confidence + "%", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(featureText(language, "Antas: ", "Severity: ") + detection.severity, color = AgriMuted)
                    Text(featureText(language, "Rekomendasyon", "Recommended next steps"), color = AgriText, fontWeight = FontWeight.Bold)
                    Text(detection.treatment, color = AgriText, style = MaterialTheme.typography.bodyMedium)
                    Text(featureText(language, "Hindi ito tunay na AI diagnosis. Kumpirmahin sa agriculturist bago mag-apply ng pesticide/fungicide.", "This is not a real AI diagnosis. Confirm with an agriculturist before applying pesticide/fungicide."), color = AgriWarning, style = MaterialTheme.typography.bodySmall)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = savedScanId == null,
                        onClick = {
                            scope.launch {
                                val crop = crops.firstOrNull { it.cropId == selectedCropId }
                                val id = UUID.randomUUID().toString()
                                dao.saveScan(ScanHistoryEntity(
                                    scanId = id, farmId = farm.farmId, farmName = farm.farmName,
                                    cropId = crop?.cropId.orEmpty(),
                                    cropName = if (crop == null) "" else crop.name + " · " + crop.crop,
                                    imagePath = imageUri?.toString().orEmpty(),
                                    diagnosis = detection.diagnosis, confidencePercent = detection.confidence,
                                    severity = detection.severity, treatment = detection.treatment
                                ))
                                savedScanId = id
                                message = featureText(language, "Na-save sa scan history ng farm.", "Saved to farm scan history.")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (savedScanId != null) featureText(language, "Na-save na", "Saved") else "Save to Farm") }
                    if (savedScanId != null) Text(message, color = AgriGreen, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text(featureText(language, "Scan History", "Scan History") + " · " + history.size, color = AgriText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
        if (history.isEmpty()) item { InfoCard(featureText(language, "Wala pang scan", "No scans yet"), featureText(language, "Ang scan na ise-save mo rito ay nakatali sa kasalukuyang farm.", "Saved scans here are attached to the current farm.")) }
        else items(history.take(10), key = { it.scanId }) { scan ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(scan.diagnosis, color = AgriText, fontWeight = FontWeight.Bold)
                    Text(listOf(scan.farmName.ifBlank { scan.farmId }, scan.cropName, scan.confidencePercent.toString() + "% · " + scan.severity).filter { it.isNotBlank() }.joinToString(" · "), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(Date(scan.createdAt)), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private data class CropGuide(val name: String, val fertilizeDays: Int, val harvestDays: Int, val advice: String)

private fun cropGuide(cropName: String, soil: String, language: String): CropGuide {
    val key = cropName.lowercase(Locale.US)
    val days = when {
        key.contains("rice") || key.contains("palay") || key.contains("palay") -> 110
        key.contains("corn") || key.contains("mais") -> 85
        key.contains("tomato") || key.contains("kamatis") -> 90
        key.contains("eggplant") || key.contains("talong") -> 85
        key.contains("cabbage") -> 75
        else -> 75
    }
    val soilFil = when (soil) {
        "Clay" -> "Mabigat ang clay; siguraduhin ang drainage at iwasang mag-overwater."
        "Sandy" -> "Mabilis mag-drain ang sandy soil; hatiin ang pataba at bantayan ang moisture."
        "Silty" -> "Panatilihin ang soil structure at iwasan ang compaction."
        else -> "Ang loam ay karaniwang may balance ng drainage at water retention."
    }
    val soilEn = when (soil) {
        "Clay" -> "Clay holds water; check drainage and avoid overwatering."
        "Sandy" -> "Sandy soil drains quickly; split fertilizer applications and monitor moisture."
        "Silty" -> "Protect soil structure and avoid compaction."
        else -> "Loam often balances drainage and water retention."
    }
    val fertilizerFil = when {
        key.contains("rice") || key.contains("palay") -> "Gumamit ng compost at balanced nutrients; hatiin ang nitrogen application ayon sa soil test at lokal na rekomendasyon."
        key.contains("corn") || key.contains("mais") -> "Ilagay ang basal fertilizer ayon sa soil test; nakadepende sa variety at target yield ang timing."
        else -> "Gumamit ng hinog na compost at crop-appropriate balanced fertilizer batay sa soil test."
    }
    val fertilizerEn = when {
        key.contains("rice") || key.contains("palay") -> "Use compost and balanced nutrients; split nitrogen applications according to soil testing and local recommendations."
        key.contains("corn") || key.contains("mais") -> "Apply basal fertilizer according to a soil test; timing depends on variety and target yield."
        else -> "Use mature compost and crop-appropriate balanced fertilizer based on a soil test."
    }
    return CropGuide(cropName, 14, days, (if (language == "en") fertilizerEn + " " + soilEn else fertilizerFil + " " + soilFil))
}

@Composable
internal fun PlantingCalendarScreen(padding: PaddingValues, farm: FarmRecord, crops: List<CropRecord>, language: String, onAddTask: (FarmTask) -> Unit, onBack: () -> Unit) {
    var soil by remember { mutableStateOf("Loam") }
    var suggestedCrop by remember { mutableStateOf("Palay") }
    var alreadyAdded by remember { mutableStateOf(setOf<String>()) }
    val today = remember { LocalDate.now() }
    val guides = if (crops.isNotEmpty()) crops.map { crop ->
        val guide = cropGuide(crop.crop.ifBlank { crop.name }, soil, language)
        guide.copy(name = guide.name + " · " + crop.name)
    } else listOf(cropGuide(suggestedCrop, soil, language))

    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureScreenHeader("Planting Calendar & Soil Guide", featureText(language, "General guide lang; i-confirm ang variety at soil test.", "General guidance; confirm local variety and soil test."), language, onBack) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(featureText(language, "Uri ng lupa", "Soil type"), color = AgriText, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Loam", "Clay", "Sandy", "Silty").forEach { option ->
                            FilterChip(selected = soil == option, onClick = { soil = option; alreadyAdded = emptySet() }, label = { Text(if (language == "en") option else when(option) { "Clay" -> "Luwad"; "Sandy" -> "Mabuhangin"; else -> option }) })
                        }
                    }
                    if (crops.isEmpty()) {
                        Text(featureText(language, "Pumili ng crop para sa sample calendar", "Choose a crop for the sample calendar"), color = AgriMuted)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Palay", "Mais", "Kamatis", "Talong").forEach { option -> FilterChip(selected = suggestedCrop == option, onClick = { suggestedCrop = option }, label = { Text(option) }) }
                        }
                    } else Text(featureText(language, "Mga naka-register na crop ang ginagamit sa schedule.", "Registered crops are used for the schedule."), color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    Text(featureText(language, "Farm: ", "Farm: ") + farm.farmName.ifBlank { farm.farmId }, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(guides, key = { it.name }) { guide ->
            val crop = crops.firstOrNull { guide.name.endsWith(it.name) }
            val plantingDate = crop?.plantingDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
            val fertilizerDate = plantingDate.plusDays(guide.fertilizeDays.toLong())
            val harvestDate = crop?.expectedHarvest?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: plantingDate.plusDays(guide.harvestDays.toLong())
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(guide.name, color = AgriText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    CalendarDateRow(featureText(language, "Pagtatanim / start", "Planting / start"), plantingDate.toString())
                    CalendarDateRow(featureText(language, "Pataba (guide)", "Fertilizer timing (guide)"), fertilizerDate.toString())
                    CalendarDateRow(featureText(language, "Tinatayang harvest", "Estimated harvest"), harvestDate.toString())
                    Text(guide.advice, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !alreadyAdded.contains(guide.name),
                        onClick = {
                            val cropName = crop?.name ?: guide.name
                            onAddTask(FarmTask(featureText(language, "Pataba: ", "Fertilize: ") + cropName, "Fertilizing", fertilizerDate.toString(), false))
                            onAddTask(FarmTask(featureText(language, "Suriin ang peste: ", "Pest check: ") + cropName, "Spraying", fertilizerDate.plusDays(7).toString(), false))
                            onAddTask(FarmTask(featureText(language, "Aniin: ", "Harvest: ") + cropName, "Harvest", harvestDate.toString(), false))
                            alreadyAdded = alreadyAdded + guide.name
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (alreadyAdded.contains(guide.name)) featureText(language, "Naidagdag na sa Tasks", "Added to Tasks") else featureText(language, "Add to Tasks", "Add to Tasks")) }
                }
            }
        }
        item { Text(featureText(language, "Paalala: generic planning dates lang ang mga ito. Iangkop sa variety, lokasyon, season, soil test, at payo ng DA/local agriculturist.", "Note: these are generic planning dates. Adapt to variety, location, season, soil test, and DA/local agriculturist guidance."), color = AgriMuted, style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun CalendarDateRow(label: String, date: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AgriMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(date, color = AgriGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun HelpSettingsScreen(padding: PaddingValues, language: String, isDark: Boolean, onLanguageChange: (String) -> Unit, onThemeChange: (Boolean) -> Unit, onBack: () -> Unit) {
    var openItem by remember { mutableStateOf("") }
    val helpItems = listOf(
        Triple("FAQ", "FAQ", featureText(language, "• Ang weather ay nangangailangan ng saved GPS point.\n• Ang scan at market price ay mock data pa.\n• Nananatili sa device ang farm records; walang cloud sync server na konektado.", "• Weather needs a saved GPS point.\n• Scan and market prices are mock data for now.\n• Farm records stay on-device; no cloud sync server is connected.")),
        Triple("About", featureText(language, "Tungkol sa AgriVision", "About AgriVision"), "AgriVision — See Your Farm. Know What To Do."),
        Triple("Contact", "Contact", featureText(language, "Maglagay ng opisyal na support email/phone ng project owner bago i-release sa publiko.", "Add the project owner's official support email/phone before public release.")),
        Triple("Privacy", "Privacy Policy", featureText(language, "Ang farm records, scan history, at account prototype ay lokal na naka-save sa device. Ang weather request ay nagpapadala lamang ng farm GPS coordinates sa Open-Meteo kapag online at nagre-refresh. Walang server-side account verification o cloud sync.", "Farm records, scan history, and prototype accounts are stored locally. Weather requests send only farm GPS coordinates to Open-Meteo when refreshing. There is no server-side account verification or cloud sync.")),
        Triple("Terms", "Terms", featureText(language, "Ang mock scan results, sample market prices, generic crop dates, at heuristic weather indicators ay pang-demo at planning lamang, hindi professional diagnosis, trading quote, o official weather warning.", "Mock scan results, sample market prices, generic crop dates, and heuristic weather indicators are for demonstration and planning only—not professional diagnosis, trading quotes, or official weather warnings."))
    )
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 14.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FeatureScreenHeader("Help & Settings", featureText(language, "FAQ, impormasyon ng app, privacy, at terms.", "FAQ, app information, privacy, and terms."), language, onBack) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(featureText(language, "Wika", "Language"), fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = language == "fil", onClick = { onLanguageChange("fil") }, label = { Text("Filipino") })
                        FilterChip(selected = language == "en", onClick = { onLanguageChange("en") }, label = { Text("English") })
                    }
                    HorizontalDivider(color = AgriLine)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("Dark mode", fontWeight = FontWeight.Bold); Text(featureText(language, "Palitan ang appearance", "Change app appearance"), color = AgriMuted, style = MaterialTheme.typography.bodySmall) }
                        Switch(checked = isDark, onCheckedChange = onThemeChange)
                    }
                }
            }
        }
        items(helpItems) { row ->
            Card(Modifier.fillMaxWidth().clickable { openItem = if (openItem == row.first) "" else row.first }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(when (row.first) { "Contact" -> Icons.Outlined.ContactSupport; "About" -> Icons.Outlined.Info; else -> Icons.Outlined.HelpOutline }, null, tint = AgriGreen)
                        Text(row.second, color = AgriText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (openItem == row.first) "−" else "+", color = AgriGreen, fontWeight = FontWeight.Bold)
                    }
                    if (openItem == row.first) Text(row.third, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text(featureText(language, "Bago mag-release: palitan ang local demo authentication ng verified auth provider at kumpirmahin ang contact/privacy terms.", "Before release: replace local demo authentication with a verified auth provider and finalize contact/privacy terms."), color = AgriMuted, style = MaterialTheme.typography.labelSmall) }
    }
}
