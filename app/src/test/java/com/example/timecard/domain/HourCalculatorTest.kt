package com.example.timecard.domain

import com.example.timecard.data.model.TimecardRow
import org.junit.Assert.assertEquals
import org.junit.Test

class HourCalculatorTest {

    @Test
    fun testCalcRowTotal_validInput_returnsCorrectSum() {
        // Given
        val row = TimecardRow(
            mon = "1.5",
            tue = "2.0",
            wed = "0.5",
            thu = "1.25",
            fri = "3.0",
            sat = "0.0"
        )

        // When
        val total = HourCalculator.calcRowTotal(row)

        // Then
        // 1.5 + 2.0 + 0.5 + 1.25 + 3.0 + 0.0 = 8.25
        assertEquals(8.25, total, 0.001)
    }

    @Test
    fun testCalcRowTotal_invalidInput_returnsZeroForInvalidDays() {
        // Given
        val row = TimecardRow(
            mon = "invalid", // Should be 0.0
            tue = "", // Should be 0.0
            wed = "  ", // Should be 0.0
            thu = "1.5", // Should be 1.5
            fri = "abc", // Should be 0.0
            sat = "2.5" // Should be 2.5
        )

        // When
        val total = HourCalculator.calcRowTotal(row)

        // Then
        // 0.0 + 0.0 + 0.0 + 1.5 + 0.0 + 2.5 = 4.0
        assertEquals(4.0, total, 0.001)
    }

    @Test
    fun testCalcRowTotal_allInvalidInput_returnsZero() {
        // Given
        val row = TimecardRow(
            mon = "x",
            tue = "y",
            wed = "z",
            thu = "a",
            fri = "b",
            sat = "c"
        )

        // When
        val total = HourCalculator.calcRowTotal(row)

        // Then
        assertEquals(0.0, total, 0.001)
    }
}
