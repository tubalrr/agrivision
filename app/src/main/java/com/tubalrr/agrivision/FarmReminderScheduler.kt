package com.tubalrr.agrivision

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object FarmReminderScheduler {
    private const val UNIQUE_NAME = "agrivision_daily_farm_reminders"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<FarmReminderWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NAME)
    }
}
