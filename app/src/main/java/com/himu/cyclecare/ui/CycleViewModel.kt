package com.himu.cyclecare.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.himu.cyclecare.CycleCareApplication
import com.himu.cyclecare.domain.CyclePrediction
import com.himu.cyclecare.domain.CyclePredictionEngine
import com.himu.cyclecare.domain.CycleSettings
import com.himu.cyclecare.domain.DailySymptomLog
import com.himu.cyclecare.domain.PeriodEntry
import com.himu.cyclecare.notifications.RescheduleWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CycleUiState(
    val periods: List<PeriodEntry> = emptyList(),
    val logs: List<DailySymptomLog> = emptyList(),
    val settings: CycleSettings = CycleSettings(),
    val prediction: CyclePrediction? = null,
)

class CycleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CycleCareApplication).repository

    val state = combine(repository.periods, repository.logs, repository.settings) { periods, logs, settings ->
        CycleUiState(periods, logs, settings, CyclePredictionEngine.predict(periods, settings))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CycleUiState())

    fun addPeriod(entry: PeriodEntry) = viewModelScope.launch {
        repository.addPeriod(entry)
        reschedule()
    }

    fun completeOnboarding(startDate: LocalDate, periodLength: Int) = viewModelScope.launch {
        val settings = state.value.settings.copy(periodLength = periodLength)
        repository.updateSettings(settings)
        repository.addPeriod(
            PeriodEntry(
                startDate = startDate,
                endDate = startDate.plusDays(periodLength.toLong() - 1),
            ),
        )
        reschedule()
    }

    fun deletePeriod(entry: PeriodEntry) = viewModelScope.launch {
        repository.deletePeriod(entry)
        reschedule()
    }

    fun saveLog(log: DailySymptomLog) = viewModelScope.launch { repository.saveLog(log) }

    fun updateSettings(settings: CycleSettings) = viewModelScope.launch {
        repository.updateSettings(settings)
        reschedule()
    }

    private fun reschedule() {
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            RescheduleWorker.NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
        )
    }
}
