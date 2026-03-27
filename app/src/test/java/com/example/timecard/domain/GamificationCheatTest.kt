package com.example.timecard.domain

import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.model.TimecardRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GamificationCheatTest {

    private val testWeekStarting = "2024-05-06" // A fixed Monday

    private fun createRow(job: String, mon: String = "", tue: String = "", wed: String = "", thu: String = "", fri: String = "", sat: String = ""): TimecardRow {
        return TimecardRow(job = job, mon = mon, tue = tue, wed = wed, thu = thu, fri = fri, sat = sat)
    }

    private fun createTimecard(weekStarting: String, rows: List<TimecardRow>): TimecardData {
        return TimecardData(
            weekStarting = weekStarting,
            employeeName = "Test Player",
            updatedAt = Instant.now().toString(),
            rows = rows
        )
    }

    private fun getBaseProfile(): PlayerProfile {
        return PlayerProfile(displayName = "Test Player", coins = 0)
    }

    // --- 1. Coin Accumulation Cheats ---

    @Test
    fun `cheat - log impossible hours to bypass daily cap`() {
        val initialProfile = getBaseProfile()
        val rows = listOf(createRow("Job A", mon = "40.0"))
        val tc = createTimecard(testWeekStarting, rows)

        val result = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val loggedHours = result.profile.coinLog.values.sumOf { it.hoursLogged }
        val paidHours = result.profile.coinLog.values.sumOf { it.paidHours }

        assertEquals("Logged hours should be 40.0", 40.0, loggedHours, 0.01)
        assertEquals("Paid hours should be capped at 16.0", 16.0, paidHours, 0.01)
    }

    @Test
    fun `cheat - infinite coin farming via save delete resave loop`() {
        val initialProfile = getBaseProfile()
        val rows8 = listOf(createRow("Job A", mon = "8.0"))
        val tc8 = createTimecard(testWeekStarting, rows8)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )
        val coinsAfterSave1 = result1.profile.coins

        val rows0 = listOf(createRow("Job A", mon = ""))
        val tc0 = createTimecard(testWeekStarting, rows0)

        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = tc0, monthWeeks = listOf(tc0), recentWeeks = listOf(tc0), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )
        val result3 = GamificationEngine.processTimecardSave(
            current = result2.profile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        assertEquals("Net coins should not increase through delete/resave loop", coinsAfterSave1, result3.profile.coins)
    }

    @Test
    fun `cheat - job hopping to trick base coins`() {
        val initialProfile = getBaseProfile()

        val rowsA = listOf(createRow("Job A", mon = "8.0"))
        val tcA = createTimecard(testWeekStarting, rowsA)
        val resultA = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tcA, monthWeeks = listOf(tcA), recentWeeks = listOf(tcA), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )
        val coinsA = resultA.profile.coins

        val rowsB = listOf(createRow("Job B", mon = "8.0"))
        val tcB = createTimecard(testWeekStarting, rowsB)
        val resultB = GamificationEngine.processTimecardSave(
            current = resultA.profile, weekData = tcB, monthWeeks = listOf(tcB), recentWeeks = listOf(tcB), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        assertEquals("Coins should not increase if total hours per day hasn't changed", coinsA, resultB.profile.coins)
    }

    // --- 2. Streak Manipulation Cheats ---

    @Test
    fun `cheat - future dating timecards to prebuild streak`() {
        val initialProfile = getBaseProfile()

        val futureWeekStarting = "2029-12-03"
        val rows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0"))
        val tc = createTimecard(futureWeekStarting, rows)

        val result = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val streak = result.profile.streaks
        assertEquals("Future dating should not inadvertently grant massive current daily streak", 0, streak.currentDaily)
    }

    @Test
    fun `cheat - backdating missed weeks to falsely revive broken streak`() {
        val initialProfile = getBaseProfile()

        val currentWeek = DateUtils.getWeekStartingMonday()
        val previousWeek = DateUtils.getPreviousMonday(currentWeek)
        val twoWeeksAgo = DateUtils.getPreviousMonday(previousWeek)

        val tc2WeeksAgo = createTimecard(twoWeeksAgo, listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0")))
        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc2WeeksAgo, monthWeeks = listOf(tc2WeeksAgo), recentWeeks = listOf(tc2WeeksAgo), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val tcCurrent = createTimecard(currentWeek, listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0")))
        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = tcCurrent, monthWeeks = listOf(tc2WeeksAgo, tcCurrent), recentWeeks = listOf(tcCurrent, tc2WeeksAgo), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val currentDayIdx = DateUtils.getCurrentDayIndex()
        val expectedStreakAfterMiss = if (currentDayIdx <= 4) currentDayIdx + 1 else 5
        assertEquals("Streak should break due to missed week", expectedStreakAfterMiss, result2.profile.streaks.currentDaily)

        val tcPrevious = createTimecard(previousWeek, listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0")))
        val result3 = GamificationEngine.processTimecardSave(
            current = result2.profile, weekData = tcPrevious, monthWeeks = listOf(tc2WeeksAgo, tcPrevious, tcCurrent), recentWeeks = listOf(tcCurrent, tcPrevious, tc2WeeksAgo), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        assertEquals("Streak should remain broken (low) despite backfilling the missed week", 1, result3.profile.streaks.currentDaily)
    }

    // --- 3. Badge Unlocking Cheats ---

    @Test
    fun `cheat - farming one time badges multiple times via save delete resave`() {
        val initialProfile = getBaseProfile()

        val rows = listOf(createRow("Job A", mon = "8.0"))
        val tc = createTimecard(testWeekStarting, rows)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val badges1 = result1.profile.badges
        assertEquals("Should receive clock puncher once", 1, badges1["clock_puncher"] ?: 0)

        val emptyTc = createTimecard(testWeekStarting, listOf(createRow("Job A", mon = "")))
        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = emptyTc, monthWeeks = listOf(emptyTc), recentWeeks = listOf(emptyTc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val result3 = GamificationEngine.processTimecardSave(
            current = result2.profile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val badges3 = result3.profile.badges
        assertEquals("Should not receive one-time badge again", 1, badges3["clock_puncher"] ?: 0)
    }

    @Test
    fun `cheat - farming repeatable badges (speed logger) infinitely`() {
        val initialProfile = getBaseProfile()

        val rows = listOf(createRow("Job A", mon = "8.0"))
        val tc = createTimecard(testWeekStarting, rows)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        assertEquals("Should receive speed_logger once", 1, result1.profile.badges["speed_logger"] ?: 0)

        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val finalCount = result2.profile.badges["speed_logger"] ?: 0
        assertEquals("System SECURE: Spam saving Monday morning DOES NOT farm Speed Logger infinitely.", 1, finalCount)
    }

    @Test
    fun `cheat - farming repeatable perfect week badge infinitely`() {
        val initialProfile = getBaseProfile()

        val rows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0"))
        val tc = createTimecard(testWeekStarting, rows)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        assertEquals("Should receive perfect_week once", 1, result1.profile.badges["perfect_week"] ?: 0)

        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = tc, monthWeeks = listOf(tc), recentWeeks = listOf(tc), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val finalCount = result2.profile.badges["perfect_week"] ?: 0
        assertEquals("System SECURE: perfect_week cannot be farmed infinitely.", 1, finalCount)
    }

    @Test
    fun `cheat - farming best week record coins by incremental saves`() {
        val initialProfile = getBaseProfile()

        val rows40 = listOf(createRow("Job A", mon = "10.0"))
        val tc40 = createTimecard(testWeekStarting, rows40)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc40, monthWeeks = listOf(tc40), recentWeeks = listOf(tc40), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )

        val expectedCoins = result1.profile.coins

        val rows41 = listOf(createRow("Job A", mon = "10.0", tue = "1.0"))
        val tc41 = createTimecard(testWeekStarting, rows41)

        val result2 = GamificationEngine.processTimecardSave(
            current = result1.profile, weekData = tc41, monthWeeks = listOf(tc41), recentWeeks = listOf(tc41), employeeName = "Test Player",
            isMonday = true, isBefore930 = true
        )


        println("Expected: ${expectedCoins + 1}, Actual: ${result2.profile.coins}")
        val actualCoins = result2.profile.coins
        assertEquals("Should not be able to farm 25 record coins multiple times in the same week by logging incrementally", expectedCoins + 1, actualCoins)
    }

    @Test
    fun `cheat - avoiding negative coins after spending and deleting hours`() {
        val initialProfile = getBaseProfile()
        val rows8 = listOf(createRow("Job A", mon = "8.0"))
        val tc8 = createTimecard(testWeekStarting, rows8)
        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), employeeName = "Test Player", isMonday = true, isBefore930 = true
        )

        val earnedCoins = result1.profile.coins

        // Simulate spending all coins
        val profileAfterSpend = result1.profile.copy(coins = 0)

        // Then they delete their hours
        val rows0 = listOf(createRow("Job A", mon = "0.0"))
        val tc0 = createTimecard(testWeekStarting, rows0)

        val result2 = GamificationEngine.processTimecardSave(
            current = profileAfterSpend, weekData = tc0, monthWeeks = listOf(tc0), recentWeeks = listOf(tc0), employeeName = "Test Player", isMonday = true, isBefore930 = true
        )

        assertEquals("Coins should NOT drop below zero when deleting hours. (It limits the refund)", 0, result2.profile.coins)

        // Now, if they try to re-log 8 hours to farm base coins again...
        val result3 = GamificationEngine.processTimecardSave(
            current = result2.profile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), employeeName = "Test Player", isMonday = true, isBefore930 = true
        )

        assertEquals("They should NOT get new coins when re-logging because they never paid back the refund debt in paidHours.", 0, result3.profile.coins)
    }
}
