package com.himu.cyclecare.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.himu.cyclecare.CycleCareApplication
import com.himu.cyclecare.MainActivity
import com.himu.cyclecare.R
import com.himu.cyclecare.domain.CyclePrediction
import com.himu.cyclecare.domain.CyclePredictionEngine
import com.himu.cyclecare.domain.CycleSettings
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object NotificationSupport {
    const val CHANNEL_ID = "cycle_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cycle reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Period and cycle phase reminders"
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val title = inputData.getString(KEY_TITLE) ?: "Cycle reminder"
        val body = inputData.getString(KEY_BODY) ?: "Open Cycle Care to view your update."
        val openApp = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, NotificationSupport.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}

object ReminderScheduler {
    private const val PREFIX = "cycle-reminder-"

    fun schedule(context: Context, prediction: CyclePrediction, settings: CycleSettings) {
        val manager = WorkManager.getInstance(context)
        listOf("period-3", "period-1", "period-0", "ovulation", "premenstrual").forEach {
            manager.cancelUniqueWork(PREFIX + it)
        }
        if (settings.periodReminders) {
            enqueue(context, "period-3", prediction.nextPeriod.minusDays(3), settings, "Period expected in 3 days")
            enqueue(context, "period-1", prediction.nextPeriod.minusDays(1), settings, "Period expected tomorrow")
            enqueue(context, "period-0", prediction.nextPeriod, settings, "Period expected today")
        }
        if (settings.ovulationReminders) {
            enqueue(context, "ovulation", prediction.ovulationDate, settings, "Estimated ovulation day")
        }
        if (settings.premenstrualReminders) {
            enqueue(context, "premenstrual", prediction.premenstrualWindow.start, settings, "Premenstrual window begins")
        }
    }

    private fun enqueue(context: Context, key: String, date: LocalDate, settings: CycleSettings, detail: String) {
        val zone = ZoneId.systemDefault()
        val trigger = date.atTime(settings.reminderHour, 0).atZone(zone).toInstant()
        val delay = Duration.between(java.time.Instant.now(), trigger).toMillis()
        if (delay <= 0) return
        val body = if (settings.privateNotifications) "Open Cycle Care to view your update." else detail
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_TITLE, "Cycle reminder").putString(ReminderWorker.KEY_BODY, body).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(PREFIX + key, ExistingWorkPolicy.REPLACE, request)
    }
}

class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CycleCareApplication
        val settings = app.repository.settings.first()
        val prediction = CyclePredictionEngine.predict(app.repository.getPeriods(), settings)
        if (prediction != null) ReminderScheduler.schedule(applicationContext, prediction, settings)
        return Result.success()
    }

    companion object { const val NAME = "reschedule-cycle-reminders" }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            RescheduleWorker.NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
        )
    }
}
