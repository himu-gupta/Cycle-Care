package com.himu.cyclecare.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CyclePredictionEngine {
    fun predict(
        periods: List<PeriodEntry>,
        settings: CycleSettings,
        today: LocalDate = LocalDate.now(),
    ): CyclePrediction? {
        val starts = periods.map { it.startDate }.distinct().sorted()
        if (starts.isEmpty()) return null

        val recentLengths = starts.zipWithNext { first, second ->
            ChronoUnit.DAYS.between(first, second).toInt()
        }.filter { it in 15..60 }.takeLast(6)
        val learned = recentLengths.size >= 2
        val length = if (learned) median(recentLengths) else settings.cycleLength.coerceIn(21, 45)

        var cycleStart = starts.last()
        var nextPeriod = cycleStart.plusDays(length.toLong())
        while (nextPeriod <= today) {
            cycleStart = nextPeriod
            nextPeriod = cycleStart.plusDays(length.toLong())
        }
        val ovulation = nextPeriod.minusDays(14)
        return CyclePrediction(
            cycleStart = cycleStart,
            cycleLength = length,
            nextPeriod = nextPeriod,
            ovulationDate = ovulation,
            fertileWindow = ovulation.minusDays(5)..ovulation,
            premenstrualWindow = nextPeriod.minusDays(5)..nextPeriod.minusDays(1),
            periodLength = settings.periodLength.coerceIn(1, 10),
            learnedFromHistory = learned,
        )
    }

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle]
        else (sorted[middle - 1] + sorted[middle]) / 2
    }
}
