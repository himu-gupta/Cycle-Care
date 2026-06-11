package com.himu.cyclecare

import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.himu.cyclecare.data.CycleDatabase
import com.himu.cyclecare.data.CycleRepository
import com.himu.cyclecare.data.SettingsStore
import com.himu.cyclecare.notifications.NotificationSupport
import com.himu.cyclecare.notifications.RescheduleWorker

class CycleCareApplication : Application() {
    lateinit var repository: CycleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = CycleDatabase.create(this)
        repository = CycleRepository(database.cycleDao(), SettingsStore(this))
        NotificationSupport.createChannel(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            RescheduleWorker.NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
        )
    }
}
