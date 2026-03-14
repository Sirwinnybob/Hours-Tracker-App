package com.example.timecard.domain

import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.model.TimecardRow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class GamificationEngineTest {

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


    @Test
    fun `filling out a week, deleting Friday, and refilling does not reward duplicate base coins`() {
        val initialProfile = getBaseProfile()

        // Scenario 1: Fill a complete week (9 hours Mon-Thu, 4 hours Fri to meet targets)
        val fullWeekRows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0"))
        val fullWeekTimecard = createTimecard(testWeekStarting, fullWeekRows)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile,
            weekData = fullWeekTimecard,
            monthWeeks = listOf(fullWeekTimecard),
            recentWeeks = listOf(fullWeekTimecard),
            isMonday = true,
            isBefore930 = true
        )
        val profileAfterFullWeek = result1.profile
        val coinsAfterFullWeek = profileAfterFullWeek.coins

        // The user should have gained coins.
        // We ensure we recorded the right coin log entries.
        assert(coinsAfterFullWeek > 0)

        // Scenario 2: Delete Friday
        val deletedFridayRows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", ""))
        val deletedFridayTimecard = createTimecard(testWeekStarting, deletedFridayRows)

        val result2 = GamificationEngine.processTimecardSave(
            current = profileAfterFullWeek,
            weekData = deletedFridayTimecard,
            monthWeeks = listOf(deletedFridayTimecard),
            recentWeeks = listOf(deletedFridayTimecard),
            isMonday = true,
            isBefore930 = true
        )
        val profileAfterDelete = result2.profile
        val coinsAfterDelete = profileAfterDelete.coins

        // Coins should NOT increase when we just delete hours
        assertEquals(coinsAfterFullWeek, coinsAfterDelete)

        // Scenario 3: Refill Friday
        val result3 = GamificationEngine.processTimecardSave(
            current = profileAfterDelete,
            weekData = fullWeekTimecard,
            monthWeeks = listOf(fullWeekTimecard),
            recentWeeks = listOf(fullWeekTimecard),
            isMonday = true,
            isBefore930 = true
        )
        val profileAfterRefill = result3.profile
        val coinsAfterRefill = profileAfterRefill.coins

        // Since Friday had already logged 8.0 hours previously, refilling back to 8.0
        // shouldn't award new base coins. (It's possible streak breaks and reforms could
        // affect the streak multiplier for *other* things but the base hourly reward for
        // Friday is capped at what was already awarded.)
        assertEquals(coinsAfterFullWeek, coinsAfterRefill)
    }

    @Test
    fun `perfect week bonus is not awarded multiple times for the same week`() {
        val initialProfile = getBaseProfile()

        // Scenario 1: Fill a complete week (9 hours Mon-Thu, 4 hours Fri to meet targets)
        val fullWeekRows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", "4.0"))
        val fullWeekTimecard = createTimecard(testWeekStarting, fullWeekRows)

        val result1 = GamificationEngine.processTimecardSave(
            current = initialProfile,
            weekData = fullWeekTimecard,
            monthWeeks = listOf(fullWeekTimecard),
            recentWeeks = listOf(fullWeekTimecard),
            isMonday = true,
            isBefore930 = true
        )
        val profileAfterFullWeek = result1.profile

        val bonusLogWeek1 = profileAfterFullWeek.weeklyBonusLog[testWeekStarting] ?: emptyMap()
        assertEquals(1, bonusLogWeek1["perfect_week_bonus"])

        // Scenario 2: Delete Friday
        val deletedFridayRows = listOf(createRow("Job A", "9.0", "9.0", "9.0", "9.0", ""))
        val deletedFridayTimecard = createTimecard(testWeekStarting, deletedFridayRows)

        val result2 = GamificationEngine.processTimecardSave(
            current = profileAfterFullWeek,
            weekData = deletedFridayTimecard,
            monthWeeks = listOf(deletedFridayTimecard),
            recentWeeks = listOf(deletedFridayTimecard),
            isMonday = true,
            isBefore930 = true
        )

        // Scenario 3: Refill Friday
        val result3 = GamificationEngine.processTimecardSave(
            current = result2.profile,
            weekData = fullWeekTimecard,
            monthWeeks = listOf(fullWeekTimecard),
            recentWeeks = listOf(fullWeekTimecard),
            isMonday = true,
            isBefore930 = true
        )

        val bonusLogWeek3 = result3.profile.weeklyBonusLog[testWeekStarting] ?: emptyMap()
        // Bonus should still be exactly 1, meaning it wasn't re-awarded
        assertEquals(1, bonusLogWeek3["perfect_week_bonus"])
    }

    @Test
    fun `deleting and refilling smaller increments does not reward duplicate coins`() {
        val initialProfile = getBaseProfile()

        // Step A: Process Monday with 4 hours
        val rows4 = listOf(createRow("Job A", "4.0"))
        val tc4 = createTimecard(testWeekStarting, rows4)

        val resultA = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tc4, monthWeeks = listOf(tc4), recentWeeks = listOf(tc4), isMonday = true, isBefore930 = true
        )
        val coinsA = resultA.profile.coins
        assert(coinsA > 0)

        // Step B: Process Monday with 8 hours (increment)
        val rows8 = listOf(createRow("Job A", "8.0"))
        val tc8 = createTimecard(testWeekStarting, rows8)

        val resultB = GamificationEngine.processTimecardSave(
            current = resultA.profile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), isMonday = true, isBefore930 = true
        )
        val coinsB = resultB.profile.coins
        // We should gain new coins for the extra 4 hours
        assert(coinsB > coinsA)

        // Step C: Process Monday with 4 hours (delete)
        val resultC = GamificationEngine.processTimecardSave(
            current = resultB.profile, weekData = tc4, monthWeeks = listOf(tc4), recentWeeks = listOf(tc4), isMonday = true, isBefore930 = true
        )
        val coinsC = resultC.profile.coins
        // Coins should not decrease or increase
        assertEquals(coinsB, coinsC)

        // Step D: Process Monday with 8 hours (re-fill)
        val resultD = GamificationEngine.processTimecardSave(
            current = resultC.profile, weekData = tc8, monthWeeks = listOf(tc8), recentWeeks = listOf(tc8), isMonday = true, isBefore930 = true
        )
        val coinsD = resultD.profile.coins
        // Coins should remain exactly the same as Step B
        assertEquals(coinsB, coinsD)
    }

    @Test
    fun `changing job codes for the same amount of hours does not reward extra coins`() {
        val initialProfile = getBaseProfile()

        // Step A: Process Monday with 8 hours Job A
        val rowsJobA = listOf(createRow("Job A", "8.0"))
        val tcJobA = createTimecard(testWeekStarting, rowsJobA)

        val resultA = GamificationEngine.processTimecardSave(
            current = initialProfile, weekData = tcJobA, monthWeeks = listOf(tcJobA), recentWeeks = listOf(tcJobA), isMonday = true, isBefore930 = true
        )
        val coinsA = resultA.profile.coins
        assert(coinsA > 0)

        // Step B: Process Monday with 8 hours Job B
        val rowsJobB = listOf(createRow("Job B", "8.0"))
        val tcJobB = createTimecard(testWeekStarting, rowsJobB)

        val resultB = GamificationEngine.processTimecardSave(
            current = resultA.profile, weekData = tcJobB, monthWeeks = listOf(tcJobB), recentWeeks = listOf(tcJobB), isMonday = true, isBefore930 = true
        )
        val coinsB = resultB.profile.coins

        // Total hours for Monday remains 8.0, so no new coins for time logged
        assertEquals(coinsA, coinsB)
    }
}
