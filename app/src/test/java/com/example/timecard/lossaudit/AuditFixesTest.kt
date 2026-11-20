package com.example.timecard.lossaudit

import com.example.timecard.data.model.LimitedPurchaseClaim
import com.example.timecard.data.model.RunningStats
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.model.TimecardRow
import com.example.timecard.domain.DateUtils
import com.example.timecard.domain.GamificationEngine
import com.example.timecard.domain.applyGrantChanges
import com.example.timecard.testutil.SourceFiles
import com.example.timecard.ui.shop.claimBelongsToCurrentUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fixes from the 2026-11-20 audit of older code (profile, timecard, shop, alerts, stats, grants). */
@OptIn(ExperimentalCoroutinesApi::class)
class AuditFixesTest {
    private val week = DateUtils.getWeekStartingMonday()

    @Before fun setUp() = Dispatchers.setMain(Dispatchers.Unconfined)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `undo back to the loaded state keeps week switching available`() {
        val share = FakeShare()
        share.files["101/$week"] = card("101", week, """{"job":"SHOP","mon":"8.00"}""")
        val model = newModel(share)
        model.setHours(0, 1, "4")
        model.undo()
        assertTrue("snapshot's stale isDataLoaded must not come back", model.uiState.value.isDataLoaded)
        assertEquals("", model.uiState.value.hours[0][1])
        assertFalse("nothing left to undo after one edit", model.canUndo())
    }

    @Test fun `first edit after loading is recorded once`() {
        val share = FakeShare()
        val model = newModel(share)
        model.setJob(1, "123")
        model.undo()
        assertEquals("one undo reverts the first edit", "", model.uiState.value.jobs[1])
    }

    @Test fun `grant revoke takes back exactly what the grant paid`() {
        val granted = applyGrantChanges(emptyMap(), emptyMap(), listOf("hero"), emptyList()) { 250 }
        assertEquals(250, granted.coinDelta)
        assertEquals(1, granted.badges["hero"])
        val revoked = applyGrantChanges(granted.badges, granted.grantCoins, emptyList(), listOf("hero")) { error("not used") }
        assertEquals(-250, revoked.coinDelta)
        assertFalse(revoked.badges.containsKey("hero"))
        // A revoke the app never paid (badge earned before it was granted, or a legacy grant)
        // leaves the earned badge and coins alone.
        val untouched = applyGrantChanges(mapOf("speed_logger" to 1), emptyMap(), emptyList(), listOf("speed_logger")) { 0 }
        assertEquals(0, untouched.coinDelta)
        assertEquals(1, untouched.badges["speed_logger"])
    }

    @Test fun `running stats follow every save of the week`() {
        fun week(shop: String, sat: String = "", job: String = "SHOP") =
            TimecardData(employeeName = "x", weekStarting = "2026-11-16", updatedAt = "", rows = listOf(TimecardRow(job = job, mon = shop, sat = sat)))
        var stats = RunningStats()
        stats = GamificationEngine.updateRunningStats(stats, "2026-11-16", 9.0, false, week("9"))
        stats = GamificationEngine.updateRunningStats(stats, "2026-11-16", 18.0, true, week("18", "2"))
        assertEquals("second save adds its extra shop hours", 18.0, stats.totalShopHours, 0.001)
        assertEquals("Saturday logged after the first save counts", 1, stats.saturdayWeeksCount)
        stats = GamificationEngine.updateRunningStats(stats, "2026-11-16", 18.0, false, week("18"))
        assertEquals("Saturday removed again", 0, stats.saturdayWeeksCount)
        assertEquals(18.0, stats.totalShopHours, 0.001)
        // Weeks counted before per-week tracking keep their old count.
        val legacy = RunningStats(totalShopHours = 5.0, processedWeeks = listOf("2026-11-09"))
        assertEquals(5.0, GamificationEngine.updateRunningStats(legacy, "2026-11-09", 30.0, false, week("30")).totalShopHours, 0.001)
    }

    @Test fun `limited claims resolve only for the employee who made them`() {
        val claim = LimitedPurchaseClaim(claimId = "c", itemId = "i", employeeName = "101")
        assertTrue(claimBelongsToCurrentUser(claim, "101", "101"))
        assertFalse("another employee logged in", claimBelongsToCurrentUser(claim, "202", "202"))
        assertFalse("profile not loaded yet", claimBelongsToCurrentUser(claim, "101", null))
        assertFalse("nobody logged in", claimBelongsToCurrentUser(claim, "", null))
    }

    @Test fun `profile and alert guards are in place`() {
        val profile = SourceFiles.mainSource("com/example/timecard/ui/profile/ProfileViewModel.kt").readText()
        assertTrue(profile.contains("if (employeeName.isEmpty() || loadedProfileFor != employeeName || profileLoadFailed)"))
        assertTrue(profile.contains("repo?.employeeFileExists(name, \"profile.json\")"))
        assertTrue(profile.contains("profileReady.await()"))
        val alerts = SourceFiles.mainSource("com/example/timecard/ui/alerts/AlertsViewModel.kt").readText()
        assertTrue(alerts.contains("if (isAcknowledging) return"))
        assertTrue(alerts.contains("if (saved == AckSave.NEW) onAcknowledged?.invoke()"))
        val shop = SourceFiles.mainSource("com/example/timecard/ui/shop/ShopViewModel.kt").readText()
        assertTrue("note charged only after delivery", shop.contains("if (result == \"SUCCESS\" && vm.employeeName == myName)"))
        val stats = SourceFiles.mainSource("com/example/timecard/ui/stats/StatsModal.kt").readText()
        assertTrue(stats.contains("java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(\"UTC\"))"))
    }
}
