package com.tubalrr.agrivision

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tubalrr.agrivision.data.local.AgriDatabase
import com.tubalrr.agrivision.data.local.FarmRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Locale

internal class FarmReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        return try {
            val dao = AgriDatabase.getInstance(applicationContext).farmDao()
            val today = LocalDate.now()
            val tasks = dao.observeTasks(FarmRepository.DEFAULT_FARM_ID).first()
            val crops = dao.observeCrops(FarmRepository.DEFAULT_FARM_ID).first()
            val inventory = dao.observeInventory(FarmRepository.DEFAULT_FARM_ID).first()
            val incidents = dao.observeFieldIncidents(FarmRepository.DEFAULT_FARM_ID).first()

            val alertPrefs = applicationContext.getSharedPreferences("agrivision_preferences", Context.MODE_PRIVATE)
            val legacyReminders = alertPrefs.getBoolean("farm_reminders_enabled", false)
            val notifyWatering = alertPrefs.getBoolean("notify_watering", false)
            val notifyFertilizing = alertPrefs.getBoolean("notify_fertilizing", false)
            val notifySpraying = alertPrefs.getBoolean("notify_spraying", false)
            val notifyHarvest = alertPrefs.getBoolean("notify_harvest", false)
            val notifyWeather = alertPrefs.getBoolean("notify_weather", false)

            val dueTasks = tasks.count { task ->
                if (task.done || parseDate(task.date)?.let { it.isAfter(today) } != false) return@count false
                if (legacyReminders) return@count true
                val category = (task.category + " " + task.title).lowercase(Locale.US)
                (notifyWatering && (category.contains("water") || category.contains("dilig"))) ||
                    (notifyFertilizing && (category.contains("fertiliz") || category.contains("pataba"))) ||
                    (notifySpraying && (category.contains("spray") || category.contains("pest"))) ||
                    (notifyHarvest && (category.contains("harvest") || category.contains("ani")))
            }
            val harvestSoon = if (legacyReminders || notifyHarvest) crops.count {
                parseDate(it.expectedHarvest)?.let { d -> !d.isBefore(today) && !d.isAfter(today.plusDays(3)) } == true
            } else 0
            val stockAlerts = if (legacyReminders) inventory.count {
                it.quantity <= 0.0 || it.status.contains("low", true) || it.status.contains("out of stock", true) ||
                    parseDate(it.expiryDate)?.let { d -> !d.isAfter(today.plusDays(30)) } == true
            } else 0

            val caseAlerts = if (legacyReminders) incidents.count { incident ->
                incident.status != "Completed" && incident.status != "Draft" &&
                    (incident.severity.equals("Critical", true) ||
                        incident.severity.equals("High", true) ||
                        incident.status.equals("Submitted", true) ||
                        incident.status.equals("Under Review", true))
            } else 0

            val weatherAlert = if (notifyWeather) runCatching {
                val cache = AgriExtrasDatabase.getInstance(applicationContext).extrasDao()
                    .getWeatherCache(FarmRepository.DEFAULT_FARM_ID)
                val daily = cache?.payloadJson?.let { org.json.JSONObject(it).optJSONObject("daily") }
                val rain = daily?.optJSONArray("precipitation_sum")
                val wind = daily?.optJSONArray("wind_speed_10m_max")
                val heavyRain = rain != null && (0 until rain.length()).any { rain.optDouble(it, 0.0) >= 50.0 }
                val strongWind = wind != null && (0 until wind.length()).any { wind.optDouble(it, 0.0) >= 60.0 }
                val lowRain = rain != null && (0 until rain.length()).sumOf { rain.optDouble(it, 0.0) } < 5.0
                heavyRain || strongWind || lowRain
            }.getOrDefault(false) else false

            val parts = buildList {
                if (dueTasks > 0) add("$dueTasks task(s) due/overdue")
                if (harvestSoon > 0) add("$harvestSoon harvest(s) within 3 days")
                if (stockAlerts > 0) add("$stockAlerts stock/expiry alert(s)")
                if (caseAlerts > 0) add("$caseAlerts active case(s) need review")
                if (weatherAlert) add("Weather risk indicator in cached 7-day forecast; check PAGASA advisories")
            }
            if (parts.isEmpty()) {
                cancelReminder()
            } else {
                showReminder(parts.joinToString(" · "))
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    @SuppressLint("MissingPermission")
    private fun cancelReminder() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            NotificationManagerCompat.from(applicationContext).cancel(1601)
        } catch (_: SecurityException) {
            // The user may revoke notification permission between the check and the call.
        }
    }

    @SuppressLint("MissingPermission")
    private fun showReminder(text: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel("farm_reminders", "Farm reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pending = launch?.let { PendingIntent.getActivity(applicationContext, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(applicationContext, "farm_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AgriVision farm reminder")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply { if (pending != null) setContentIntent(pending) }
            .build()
        NotificationManagerCompat.from(applicationContext).notify(1601, notification)
    }

    private fun parseDate(value: String): LocalDate? {
        val raw = value.trim()
        if (raw.isBlank()) return null
        return when (raw.lowercase()) {
            "today" -> LocalDate.now()
            "tomorrow" -> LocalDate.now().plusDays(1)
            "yesterday" -> LocalDate.now().minusDays(1)
            else -> runCatching { LocalDate.parse(raw) }.getOrNull()
        }
    }
}
