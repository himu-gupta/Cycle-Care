package com.himu.cyclecare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.himu.cyclecare.domain.DailySymptomLog
import com.himu.cyclecare.domain.FlowLevel
import com.himu.cyclecare.domain.PeriodEntry
import java.time.LocalDate

@Entity(tableName = "period_entries", indices = [Index(value = ["startEpochDay"], unique = true)])
data class PeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long,
    val endEpochDay: Long?,
)

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val epochDay: Long,
    val flow: String,
    val pain: Int,
    val symptoms: String,
    val medicine: String,
    val notes: String,
)

fun PeriodEntity.toDomain() = PeriodEntry(
    id = id,
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = endEpochDay?.let(LocalDate::ofEpochDay),
)

fun PeriodEntry.toEntity() = PeriodEntity(id, startDate.toEpochDay(), endDate?.toEpochDay())

fun DailyLogEntity.toDomain() = DailySymptomLog(
    date = LocalDate.ofEpochDay(epochDay),
    flow = FlowLevel.entries.firstOrNull { it.name == flow } ?: FlowLevel.NONE,
    pain = pain,
    symptoms = symptoms.split('|').filter(String::isNotBlank).toSet(),
    medicine = medicine,
    notes = notes,
)

fun DailySymptomLog.toEntity() = DailyLogEntity(
    epochDay = date.toEpochDay(),
    flow = flow.name,
    pain = pain.coerceIn(0, 10),
    symptoms = symptoms.joinToString("|"),
    medicine = medicine,
    notes = notes,
)
