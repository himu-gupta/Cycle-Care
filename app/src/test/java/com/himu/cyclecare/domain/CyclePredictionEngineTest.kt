package com.himu.cyclecare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CyclePredictionEngineTest {
    @Test
    fun defaultsTo28DayCycleAndDay14Ovulation() {
        val start = LocalDate.of(2026, 5, 1)
        val prediction = CyclePredictionEngine.predict(
            periods = listOf(PeriodEntry(startDate = start)),
            settings = CycleSettings(),
        )

        assertNotNull(prediction)
        assertEquals(LocalDate.of(2026, 5, 29), prediction!!.nextPeriod)
        assertEquals(LocalDate.of(2026, 5, 15), prediction.ovulationDate)
        assertFalse(prediction.learnedFromHistory)
    }

    @Test
    fun learnsMedianAfterTwoCompletedIntervals() {
        val prediction = CyclePredictionEngine.predict(
            periods = listOf(
                PeriodEntry(startDate = LocalDate.of(2026, 1, 1)),
                PeriodEntry(startDate = LocalDate.of(2026, 1, 30)),
                PeriodEntry(startDate = LocalDate.of(2026, 2, 26)),
                PeriodEntry(startDate = LocalDate.of(2026, 3, 26)),
            ),
            settings = CycleSettings(),
        )!!

        assertEquals(28, prediction.cycleLength)
        assertTrue(prediction.learnedFromHistory)
    }

    @Test
    fun phaseBoundariesAndPremenstrualOverlayAreStable() {
        val prediction = CyclePredictionEngine.predict(
            periods = listOf(PeriodEntry(startDate = LocalDate.of(2026, 5, 1))),
            settings = CycleSettings(periodLength = 5),
        )!!

        assertEquals(CyclePhase.MENSTRUAL, prediction.phaseOn(LocalDate.of(2026, 5, 5)))
        assertEquals(CyclePhase.FOLLICULAR, prediction.phaseOn(LocalDate.of(2026, 5, 14)))
        assertEquals(CyclePhase.OVULATION, prediction.phaseOn(LocalDate.of(2026, 5, 15)))
        assertEquals(CyclePhase.LUTEAL, prediction.phaseOn(LocalDate.of(2026, 5, 16)))
        assertTrue(prediction.isPremenstrual(LocalDate.of(2026, 5, 24)))
        assertEquals(CyclePhase.LUTEAL, prediction.phaseOn(LocalDate.of(2026, 5, 24)))
    }

    @Test
    fun keepsExpectedDateWhenPeriodIsOverdue() {
        val prediction = CyclePredictionEngine.predict(
            periods = listOf(PeriodEntry(startDate = LocalDate.of(2026, 1, 1))),
            settings = CycleSettings(),
        )!!

        assertEquals(LocalDate.of(2026, 1, 29), prediction.nextPeriod)
        assertEquals(3, prediction.daysOverdue(LocalDate.of(2026, 2, 1)))
        assertTrue(prediction.isOverdue(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun handlesLeapDayAndYearBoundary() {
        val leapPrediction = CyclePredictionEngine.predict(
            periods = listOf(PeriodEntry(startDate = LocalDate.of(2024, 2, 10))),
            settings = CycleSettings(),
        )!!
        val yearPrediction = CyclePredictionEngine.predict(
            periods = listOf(PeriodEntry(startDate = LocalDate.of(2025, 12, 20))),
            settings = CycleSettings(),
        )!!

        assertEquals(LocalDate.of(2024, 3, 9), leapPrediction.nextPeriod)
        assertEquals(LocalDate.of(2026, 1, 17), yearPrediction.nextPeriod)
    }
}
