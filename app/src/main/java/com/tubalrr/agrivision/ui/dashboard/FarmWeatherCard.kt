package com.tubalrr.agrivision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tubalrr.agrivision.domain.model.FarmRecord
import com.tubalrr.agrivision.domain.model.FieldRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private data class FarmForecastDay(
    val date: String,
    val weatherCode: Int,
    val minimumTemperatureC: Double,
    val maximumTemperatureC: Double,
    val precipitationProbabilityPercent: Int,
    val precipitationSumMm: Double
)

private data class FarmWeather(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val humidityPercent: Int,
    val precipitationMm: Double,
    val windKmh: Double,
    val weatherCode: Int,
    val forecast: List<FarmForecastDay>,
    val fetchedAt: Long = System.currentTimeMillis()
)

private data class FarmCoordinates(val latitude: Double, val longitude: Double)

private fun weatherCodeLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mainly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Conditions unavailable"
}

private fun resolveFarmCoordinates(farm: FarmRecord, fields: List<FieldRecord>): FarmCoordinates? {
    val field = fields.firstOrNull {
        val lat = it.latitude
        val lon = it.longitude
        lat != null && lon != null &&
            lat.isFinite() && lon.isFinite() &&
            lat in -90.0..90.0 && lon in -180.0..180.0
    }
    if (field != null) {
        return FarmCoordinates(field.latitude!!, field.longitude!!)
    }

    // Use saved mapped boundaries only; never infer a location from a town name.
    val boundary = farm.boundaryPoints
    if (boundary.isNotEmpty()) {
        val lat = boundary.map { it.latitude }.average()
        val lon = boundary.map { it.longitude }.average()
        if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
            return FarmCoordinates(lat, lon)
        }
    }
    return null
}

private suspend fun loadFarmWeather(coordinates: FarmCoordinates): FarmWeather = withContext(Dispatchers.IO) {
    val lat = URLEncoder.encode(coordinates.latitude.toString(), "UTF-8")
    val lon = URLEncoder.encode(coordinates.longitude.toString(), "UTF-8")
    val query = "https://api.open-meteo.com/v1/forecast" +
        "?latitude=$lat&longitude=$lon" +
        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum" +
        "&forecast_days=5&timezone=auto"
    val connection = (URL(query).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "AgriVision-Android")
    }
    try {
        val status = connection.responseCode
        check(status in 200..299) { "Weather service returned HTTP $status" }
        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(response)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        val dates = daily?.optJSONArray("time")
        val codes = daily?.optJSONArray("weather_code")
        val maximums = daily?.optJSONArray("temperature_2m_max")
        val minimums = daily?.optJSONArray("temperature_2m_min")
        val probabilities = daily?.optJSONArray("precipitation_probability_max")
        val precipitation = daily?.optJSONArray("precipitation_sum")
        val forecast = if (
            dates != null && codes != null && maximums != null &&
            minimums != null && probabilities != null && precipitation != null
        ) {
            (0 until minOf(5, dates.length())).mapNotNull { index ->
                val date = dates.optString(index, "")
                val low = minimums.optDouble(index, Double.NaN)
                val high = maximums.optDouble(index, Double.NaN)
                if (date.isBlank() || !low.isFinite() || !high.isFinite()) null
                else FarmForecastDay(
                    date = date,
                    weatherCode = codes.optInt(index, -1),
                    minimumTemperatureC = low,
                    maximumTemperatureC = high,
                    precipitationProbabilityPercent = probabilities.optInt(index, -1),
                    precipitationSumMm = precipitation.optDouble(index, 0.0)
                )
            }
        } else emptyList()
        FarmWeather(
            temperatureC = current.getDouble("temperature_2m"),
            apparentTemperatureC = current.getDouble("apparent_temperature"),
            humidityPercent = current.getInt("relative_humidity_2m"),
            precipitationMm = current.getDouble("precipitation"),
            windKmh = current.getDouble("wind_speed_10m"),
            weatherCode = current.getInt("weather_code"),
            forecast = forecast
        )
    } finally {
        connection.disconnect()
    }
}

@Composable
internal fun FarmWeatherCard(
    farm: FarmRecord,
    fields: List<FieldRecord>
) {
    val coordinates = remember(farm.boundaryPoints, fields) { resolveFarmCoordinates(farm, fields) }
    val weatherLocale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<FarmWeather?>(null) }
    var weatherForCoordinates by remember { mutableStateOf<FarmCoordinates?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Keep a saved forecast tied to the location it was fetched for.
    val currentWeather = weather.takeIf { weatherForCoordinates == coordinates }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Cloud, contentDescription = null, tint = AgriGreen, modifier = Modifier.size(26.dp))
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Farm Weather", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Optional live data • saved farm records stay on this device",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = AgriMuted, modifier = Modifier.size(22.dp))
                }
            }

            if (coordinates == null) {
                Text(
                    "Add a GPS point to a field or map the farm boundary to request a local forecast.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (currentWeather == null) {
                Text(
                    error ?: "Get current conditions for the saved farm location.",
                    color = error?.let { AgriWarning } ?: AgriMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                weather = loadFarmWeather(coordinates)
                                weatherForCoordinates = coordinates
                            } catch (_: Exception) {
                                error = "Weather unavailable. Check your connection and try again."
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (loading) "Loading weather…" else "Get farm weather")
                }
            } else {
                val current = currentWeather!!
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${"%.1f".format(Locale.US, current.temperatureC)}°C",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AgriGreen
                        )
                        Text(weatherCodeLabel(current.weatherCode), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Feels like ${"%.1f".format(Locale.US, current.apparentTemperatureC)}°C",
                            color = AgriMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(
                        Modifier.background(AgriSurfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("Humidity  ${current.humidityPercent}%", color = AgriText, style = MaterialTheme.typography.bodySmall)
                        Text("Rain  ${"%.1f".format(Locale.US, current.precipitationMm)} mm", color = AgriText, style = MaterialTheme.typography.bodySmall)
                        Text("Wind  ${"%.1f".format(Locale.US, current.windKmh)} km/h", color = AgriText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "Updated ${SimpleDateFormat("MMM d, HH:mm", weatherLocale).format(Date(current.fetchedAt))} • Source: Open-Meteo",
                    color = AgriMuted,
                    style = MaterialTheme.typography.labelSmall
                )
                if (current.forecast.isNotEmpty()) {
                    Text("5-day forecast", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        current.forecast.forEach { day ->
                            Column(
                                Modifier.width(112.dp)
                                    .background(AgriSurfaceVariant, RoundedCornerShape(15.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val dayLabel = runCatching {
                                    LocalDate.parse(day.date).format(DateTimeFormatter.ofPattern("EEE", weatherLocale))
                                }.getOrDefault(day.date)
                                Text(dayLabel, fontWeight = FontWeight.Bold, color = AgriText)
                                Text(weatherCodeLabel(day.weatherCode), color = AgriMuted, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${"%.0f".format(Locale.US, day.maximumTemperatureC)}° / ${"%.0f".format(Locale.US, day.minimumTemperatureC)}°",
                                    fontWeight = FontWeight.SemiBold,
                                    color = AgriText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (day.precipitationProbabilityPercent in 0..100) {
                                    Text("Rain ${day.precipitationProbabilityPercent}%", color = AgriGreen, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    "${"%.1f".format(Locale.US, day.precipitationSumMm)} mm",
                                    color = AgriMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    val rainyDay = current.forecast.firstOrNull {
                        it.precipitationProbabilityPercent >= 70 || it.precipitationSumMm >= 8.0
                    }
                    if (rainyDay != null) {
                        Text(
                            "Planning note: rain is forecast on ${rainyDay.date}. Check local conditions before scheduling weather-sensitive field work.",
                            color = AgriWarning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                weather = loadFarmWeather(coordinates)
                                weatherForCoordinates = coordinates
                            } catch (_: Exception) {
                                error = "Weather refresh failed. Check your connection and try again."
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled = !loading,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Refresh")
                }
                error?.let {
                    Text(it, color = AgriWarning, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
