package com.example.timecard.ui.timesheet.lcars

import com.example.timecard.ui.timesheet.standard.summarizeDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LcarsClassicLogicTest {

    @Test
    fun `tank segments follow each day's target`() {
        val days = summarizeDays(listOf(9.0, 4.5, 0.0, 12.0, 2.0, 3.0), LocalDate.of(2026, 11, 16), 3)
        // 12 segments: full when met, half at 4.5 of 9, none when empty, capped at full when over target.
        assertEquals(listOf(12, 6, 0, 12, 6, 12), days.map { lcarsLitSegments(it) })
        val empty = summarizeDays(List(6) { 0.0 }, null, null)
        assertEquals(List(6) { 0 }, empty.map { lcarsLitSegments(it) })
    }

    @Test
    fun `SHOP pill is always bluey and other rows cycle the classic colors`() {
        assertEquals(LcarsClassic.Bluey, lcarsRowColor("shop", 4))
        assertEquals(LcarsClassic.RowColors[0], lcarsRowColor("725", 0))
        assertEquals(LcarsClassic.RowColors[1], lcarsRowColor("591", 1))
        assertEquals(LcarsClassic.RowColors[0], lcarsRowColor("679", LcarsClassic.RowColors.size))
    }

    @Test
    fun `title week number is the ISO week of the timesheet week`() {
        assertEquals("47", lcarsWeekNumber("2026-11-16"))
        // ISO week 1 of 2026 starts Monday Dec 29, 2025 (Jan 1 is a Thursday).
        assertEquals("1", lcarsWeekNumber("2025-12-29"))
        assertEquals("2", lcarsWeekNumber("2026-01-05"))
        assertEquals("", lcarsWeekNumber("W12"))
    }
}
