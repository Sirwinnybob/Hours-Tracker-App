package com.example.timecard.domain

import com.example.timecard.data.model.CoinLogEntry
import com.example.timecard.data.model.StreakData
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.model.TimecardRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class StreakLogicTest {

    private val currentWeekMonday = DateUtils.getWeekStartingMonday()
    private val currentDayIndex = DateUtils.getCurrentDayIndex()

    private fun getPreviousWeek(week: String): String = DateUtils.getPreviousMonday(week)

    private fun createRow(job: String, mon: String = "", tue: String = "", wed: String = "", thu: String = "", fri: String = "", sat: String = ""): TimecardRow {
        return TimecardRow(job = job, mon = mon, tue = tue, wed = wed, thu = thu, fri = fri, sat = sat)
    }

    private fun createTimecard(weekStarting: String, rows: List<TimecardRow>): TimecardData {
        return TimecardData(
            weekStarting = weekStarting,
            employeeName = "Test",
            updatedAt = "",
            rows = rows
        )
    }

    @Test
    fun `empty weeks list returns initial streak data`() {
        val result = GamificationEngine.computeStreaks(StreakData(bestDaily = 5, bestWeekly = 2), emptyList())
        assertEquals(0, result.currentDaily)
        assertEquals(5, result.bestDaily)
        assertEquals(0, result.currentWeekly)
        assertEquals(2, result.bestWeekly)
    }

    @Test
    fun `normal daily streak progression across week boundaries`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1Rows = listOf(createRow("Job", "9.0", "9.0", "9.0", "9.0", "4.0", ""))
        val week1 = createTimecard(week1Date, week1Rows)

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)
        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks)

        val expectedDaysThisWeek = week2Target + 1
        assertEquals(5 + expectedDaysThisWeek, result.currentDaily)
        assertEquals(5 + expectedDaysThisWeek, result.bestDaily)

        val isWeek2Complete = week2Target >= 4
        assertEquals(if (isWeek2Complete) 2 else 1, result.currentWeekly)
        assertEquals(if (isWeek2Complete) 2 else 1, result.bestWeekly)
    }

    @Test
    fun `missed day breaks streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        // For week1, miss Thursday
        val week1 = createTimecard(week1Date, listOf(createRow("Job", "9.0", "9.0", "9.0", "", "4.0", "")))

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)
        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks)

        val expectedDaysThisWeek = week2Target + 1
        assertEquals(1 + expectedDaysThisWeek, result.currentDaily)

        val expectedBest = maxOf(3, 1 + expectedDaysThisWeek)
        assertEquals(expectedBest, result.bestDaily)

        val isWeek2Complete = week2Target >= 4
        assertEquals(if (isWeek2Complete) 1 else 0, result.currentWeekly)
    }

    @Test
    fun `excused absence preserves but does not increment streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1 = createTimecard(
            week1Date,
            listOf(
                createRow("Job", "9.0", "9.0", "9.0", "9.0", "", ""),
                createRow("PTO", "", "", "", "", "2.0", "") // Excused Friday, hours 2.0 < target 4.0
            )
        )

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)
        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks)

        val expectedDaysThisWeek = week2Target + 1
        // Mon-Thu (4) + Friday (+0) + expectedDaysThisWeek
        assertEquals(4 + expectedDaysThisWeek, result.currentDaily)
    }

    @Test
    fun `missing saturday does not break daily streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1 = createTimecard(
            week1Date,
            listOf(createRow("Job", "9.0", "9.0", "9.0", "9.0", "4.0", "")) // missing Saturday
        )

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)
        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks)

        val expectedDaysThisWeek = week2Target + 1
        assertEquals(5 + expectedDaysThisWeek, result.currentDaily)
    }

    @Test
    fun `today hours not entered yet does not break streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1 = createTimecard(
            week1Date,
            listOf(createRow("Job", "9.0", "9.0", "9.0", "9.0", "4.0", ""))
        )

        // Mon-(today-1) entered, today pending
        val week2Target = minOf(currentDayIndex - 1, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target && i >= 0) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)
        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks)

        val expectedDaysThisWeek = if (currentDayIndex == 0) 0 else week2Target + 1
        assertEquals(5 + expectedDaysThisWeek, result.currentDaily)
    }

    @Test
    fun `backfilled regular hours break current streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1 = createTimecard(
            week1Date,
            listOf(createRow("Job", "9.0", "9.0", "9.0", "9.0", "4.0", ""))
        )

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i <= week2Target) targets[i] else "" }
        val week2 = createTimecard(week2Date, listOf(createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])))

        val sortedWeeks = listOf(week2, week1)

        // Simulate that Monday of current week was backfilled today (saved in future of the day)
        val dateParts = week2Date.split("-")
        val dayDateStr = String.format(java.util.Locale.US,
            "%04d-%02d-%02d",
            dateParts[0].toInt(),
            dateParts[1].toInt(),
            dateParts[2].toInt()
        )
        val coinLog = mapOf(
            dayDateStr to CoinLogEntry(savedAt = "2099-03-12T10:00:00Z", hoursLogged = 9.0)
        )

        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks, coinLog)

        val expectedDaysThisWeek = if (week2Target == 0) 0 else week2Target // because index 0 was broken
        assertEquals(expectedDaysThisWeek, result.currentDaily)

        assertEquals(5, result.bestDaily)
    }

    @Test
    fun `backfilled excused absence does not break streak`() {
        val week2Date = currentWeekMonday
        val week1Date = getPreviousWeek(week2Date)

        val week1 = createTimecard(
            week1Date,
            listOf(createRow("Job", "9.0", "9.0", "9.0", "9.0", "4.0", ""))
        )

        val week2Target = minOf(currentDayIndex, 4)
        val targets = listOf("9.0", "9.0", "9.0", "9.0", "4.0")
        val week2Hours = Array(7) { i -> if (i in 1..week2Target) targets[i] else "" }
        // For PTO on Monday, use 8.0 < target 9.0, to hit the `isExcused` branch instead of regular increment branch
        val week2PtoHours = Array(7) { i -> if (i == 0) "8.0" else "" }
        val week2 = createTimecard(week2Date, listOf(
            createRow("PTO", week2PtoHours[0], week2PtoHours[1], week2PtoHours[2], week2PtoHours[3], week2PtoHours[4], week2PtoHours[5]),
            createRow("Job", week2Hours[0], week2Hours[1], week2Hours[2], week2Hours[3], week2Hours[4], week2Hours[5])
        ))

        val sortedWeeks = listOf(week2, week1)

        val dateParts = week2Date.split("-")
        val dayDateStr = String.format(java.util.Locale.US,
            "%04d-%02d-%02d",
            dateParts[0].toInt(),
            dateParts[1].toInt(),
            dateParts[2].toInt()
        )
        val coinLog = mapOf(
            dayDateStr to CoinLogEntry(savedAt = "2099-03-12T10:00:00Z", hoursLogged = 9.0)
        )

        val result = GamificationEngine.computeStreaks(StreakData(), sortedWeeks, coinLog)

        // PTO Monday doesn't increment but preserves. So streak = 5 (week1) + (week2Target - 0 PTO)
        val expectedDaysThisWeek = week2Target
        assertEquals(5 + expectedDaysThisWeek, result.currentDaily)
    }
}
