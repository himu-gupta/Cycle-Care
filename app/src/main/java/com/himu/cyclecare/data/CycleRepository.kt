package com.himu.cyclecare.data

import com.himu.cyclecare.domain.CycleSettings
import com.himu.cyclecare.domain.DailySymptomLog
import com.himu.cyclecare.domain.PeriodEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleRepository(
    private val dao: CycleDao,
    private val settingsStore: SettingsStore,
) {
    val periods: Flow<List<PeriodEntry>> = dao.observePeriods().map { rows -> rows.map(PeriodEntity::toDomain) }
    val logs: Flow<List<DailySymptomLog>> = dao.observeLogs().map { rows -> rows.map(DailyLogEntity::toDomain) }
    val settings: Flow<CycleSettings> = settingsStore.settings

    suspend fun addPeriod(entry: PeriodEntry) = dao.insertPeriod(entry.toEntity())
    suspend fun deletePeriod(entry: PeriodEntry) = dao.deletePeriod(entry.toEntity())
    suspend fun saveLog(log: DailySymptomLog) = dao.upsertLog(log.toEntity())
    suspend fun updateSettings(settings: CycleSettings) = settingsStore.update(settings)
    suspend fun getPeriods(): List<PeriodEntry> = dao.getPeriods().map(PeriodEntity::toDomain)
}
