package com.tubalrr.agrivision

import android.Manifest
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

            val dueTasks = tasks.count { !it.done && parseDate(it.date)?.let { d -> !d.isAfter(today) } == true }
            val harvestSoon = crops.count { parseDate(it.expectedHarvest)?.let { d -> !d.isBefore(today) && !d.isAfter(today.plusDays(3)) } == true }
            val stockAlerts = inventory.count {
                it.quantity <= 0.0 || it.status.contains("low", true) || it.status.contains("out of stock", true) ||
                    parseDate(it.expiryDate)?.let { d -> !d.isAfter(today.plusDays(30)) } == true
            }

            val parts = buildList {
                if (dueTasks > 0) add("$dueTasks task(s) due/overdue")
                if (harvestSoon > 0) add("$harvestSoon harvest(s) within 3 days")
                if (stockAlerts > 0) add("$stockAlerts stock/expiry alert(s)")
            }
            if (parts.isEmpty()) {
                NotificationManagerCompat.from(applicationContext).cancel(1601)
            } else {
                showReminder(parts.joinToString(" · "))
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

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
