package com.himu.cyclecare.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PeriodEntry(
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

data class DailySymptomLog(
    val date: LocalDate,
    val flow: FlowLevel = FlowLevel.NONE,
    val pain: Int = 0,
    val symptoms: Set<String> = emptySet(),
    val medicine: String = "",
    val notes: String = "",
)

enum class FlowLevel { NONE, LIGHT, MEDIUM, HEAVY }

enum class CyclePhase(val label: String) {
    MENSTRUAL("Menstrual"),
    FOLLICULAR("Follicular"),
    OVULATION("Estimated ovulation"),
    LUTEAL("Luteal"),
}

data class CycleSettings(
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val reminderHour: Int = 9,
    val periodReminders: Boolean = true,
    val ovulationReminders: Boolean = false,
    val premenstrualReminders: Boolean = false,
    val privateNotifications: Boolean = true,
)

data class CyclePrediction(
    val cycleStart: LocalDate,
    val cycleLength: Int,
    val nextPeriod: LocalDate,
    val ovulationDate: LocalDate,
    val fertileWindow: ClosedRange<LocalDate>,
    val premenstrualWindow: ClosedRange<LocalDate>,
    val periodLength: Int,
    val learnedFromHistory: Boolean,
) {
    fun phaseOn(date: LocalDate): CyclePhase {
        val day = java.time.temporal.ChronoUnit.DAYS.between(cycleStart, date).toInt() + 1
        return when {
            day in 1..periodLength -> CyclePhase.MENSTRUAL
            date == ovulationDate -> CyclePhase.OVULATION
            date.isBefore(ovulationDate) -> CyclePhase.FOLLICULAR
            else -> CyclePhase.LUTEAL
        }
    }

    fun isPremenstrual(date: LocalDate): Boolean = date in premenstrualWindow

    fun daysOverdue(date: LocalDate): Long =
        ChronoUnit.DAYS.between(nextPeriod, date).coerceAtLeast(0)

    fun isOverdue(date: LocalDate): Boolean = date.isAfter(nextPeriod)
}
