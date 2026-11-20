package com.example.timecard.ui.retro95

import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.Challenge
import com.example.timecard.domain.StatsResult
import com.example.timecard.testutil.SourceFiles
import com.example.timecard.ui.alerts.AlertsViewModel
import com.example.timecard.ui.alerts.MailFolder
import com.example.timecard.ui.alerts.mailDate
import com.example.timecard.ui.alerts.mailFolderItems
import com.example.timecard.ui.alerts.mailFrom
import com.example.timecard.ui.alerts.mailSubject
import com.example.timecard.ui.challenges.ChallengeProgress
import com.example.timecard.ui.challenges.setupState
import com.example.timecard.ui.challenges.setupTotals
import com.example.timecard.ui.profile.BuddyTab
import com.example.timecard.ui.profile.LeaderboardEntry
import com.example.timecard.ui.profile.buddyOnline
import com.example.timecard.ui.profile.buddyOrder
import com.example.timecard.ui.profile.buddyValue
import com.example.timecard.ui.profile.feedLine
import com.example.timecard.ui.profile.kkcSpaceComments
import com.example.timecard.ui.profile.kkcSpaceHandle
import com.example.timecard.ui.profile.kkcSpaceMood
import com.example.timecard.ui.profile.kkcSpaceTop8
import com.example.timecard.ui.shop.ShopCategory
import com.example.timecard.ui.shop.ShopItemState
import com.example.timecard.ui.shop.martButtonLabel
import com.example.timecard.ui.shop.martPage
import com.example.timecard.ui.stats.monitorDayAverages
import com.example.timecard.ui.stats.monitorRangeValid
import com.example.timecard.ui.stats.monitorTopJobs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retro theme: every screen is its own 90s app (2026-11-20 mockups). Logic + routing. */
class RetroAppsTest {

    private fun entry(id: String, week: Double, month: Double = 0.0, streak: Int = 0, coins: Int = 0) = LeaderboardEntry(
        id = id, name = id, displayName = null, weekHours = week, monthHours = month, allTimeCoins = coins, currentStreak = streak,
        avatarBytes = null, bestDailyStreak = 0, currentWeeklyStreak = 0, bestWeeklyStreak = 0, bestWeekHours = 0.0, bestDayHours = 0.0,
        badges = emptyMap(), inventory = emptyList(), purchaseHistory = emptyList(), coins = coins
    )

    @Test
    fun `hours monitor averages days over weeks and ranks jobs`() {
        val stats = StatsResult(80.0, 10.0, mapOf("SHOP" to 10.0, "2041" to 60.0, "711" to 10.0, "X" to 0.0), mapOf("mon" to 18.0, "fri" to 8.0), 2)
        assertEquals(listOf(9.0, 0.0, 0.0, 0.0, 4.0, 0.0), monitorDayAverages(stats))
        assertEquals(List(6) { 0.0 }, monitorDayAverages(null))
        assertEquals(listOf("2041", "SHOP", "711"), monitorTopJobs(stats).map { it.first })
        assertTrue(monitorRangeValid("2026-11-01", "2026-11-19"))
        assertFalse(monitorRangeValid("2026-11-19", "2026-11-01"))
        assertFalse(monitorRangeValid("11/01/2026", "2026-11-19"))
    }

    @Test
    fun `messenger orders buddies per tab and splits online from offline`() {
        val list = listOf(entry("a", 30.0, 100.0, 2, 50), entry("b", 0.0, 150.0, 9, 900), entry("c", 36.0, 120.0, 0, 10))
        assertEquals(listOf("c", "a", "b"), buddyOrder(list, BuddyTab.Week).map { it.id })
        assertEquals(listOf("b", "c", "a"), buddyOrder(list, BuddyTab.Month).map { it.id })
        assertEquals(listOf("b", "a", "c"), buddyOrder(list, BuddyTab.Streak).map { it.id })
        assertEquals(listOf("b", "a", "c"), buddyOrder(list, BuddyTab.Coins).map { it.id })
        assertEquals("36.0 hrs", buddyValue(list[2], BuddyTab.Week))
        assertEquals("9 days", buddyValue(list[1], BuddyTab.Streak))
        assertEquals("900 KK", buddyValue(list[1], BuddyTab.Coins))
        assertFalse(buddyOnline(list[1]))
        assertEquals("earned the badge Speed Logger", feedLine(ActivityEvent("badge_earned", "x", "X", "Speed Logger", "", "")))
        assertEquals("hit a 5-day streak", feedLine(ActivityEvent("streak_milestone", "x", "X", "5 days", "", "")))
        assertEquals("hit a 1-day streak", feedLine(ActivityEvent("streak_milestone", "x", "X", "1 day", "", "")))
        // KKCspace Top 8: this week's hours, not me, nobody with zero.
        assertEquals(listOf("c"), kkcSpaceTop8(list, myId = "a").map { it.id })
    }

    @Test
    fun `setup wizard and mail wording`() {
        fun cp(reward: Int, p: Double, done: Boolean) = ChallengeProgress(Challenge("id$reward", "T", "D", type = "min_hours", reward = reward), p, done)
        assertEquals("Installed", setupState(1.0, true))
        assertEquals("Installing... 80%", setupState(0.8, false))
        assertEquals("Not started", setupState(0.0, false))
        assertEquals(50 to 75, setupTotals(listOf(cp(50, 1.0, true), cp(75, 0.4, false))))

        val a = AlertsViewModel.MergedAlert("1", "Delivery moved to Friday\nLoad Thursday.", "2026-11-17T09:05:00", null, false, null, null)
        val b = a.copy(id = "2", acknowledged = true, sentBy = "Cameron")
        assertEquals("Delivery moved to Friday", mailSubject(a.message))
        assertEquals("(no subject)", mailSubject("  "))
        assertEquals(48, mailSubject("x".repeat(80)).length)
        assertEquals("Office", mailFrom(a))
        assertEquals("Cameron", mailFrom(b))
        assertEquals("2026-11-17 09:05", mailDate(a.sentAt))
        assertEquals(listOf("1"), mailFolderItems(listOf(a, b), MailFolder.NeedsReply).map { it.id })
        assertEquals(listOf("2"), mailFolderItems(listOf(a, b), MailFolder.Acknowledged).map { it.id })
    }

    @Test
    fun `kk mart and kkcspace wording`() {
        assertEquals("BUY NOW!", martButtonLabel(ShopItemState.CanBuy, false, false, false))
        assertEquals("CLAIM IT!", martButtonLabel(ShopItemState.CanBuy, false, false, true))
        assertEquals("NEED 429 MORE", martButtonLabel(ShopItemState.Short(429), false, false, false))
        assertEquals("USE IT", martButtonLabel(ShopItemState.Owned, true, false, false))
        assertEquals("IN USE", martButtonLabel(ShopItemState.Owned, true, true, false))
        assertEquals("OWNED", martButtonLabel(ShopItemState.Owned, false, false, false))
        assertEquals("index.html", martPage(null))
        assertEquals("theme.html", martPage(ShopCategory.Themes))
        assertEquals("winstonferguson", kkcSpaceHandle("Winston Ferguson"))
        assertEquals("me", kkcSpaceHandle("!!"))
        assertTrue(kkcSpaceMood(3).startsWith("on fire"))
        assertTrue(kkcSpaceMood(0).startsWith("caffeinated"))
        val comments = kkcSpaceComments(mapOf("speed_logger" to 2, "nope" to 1, "perfect_week" to 0))
        assertEquals(listOf("speed_logger"), comments.map { it.badge.id })
        assertTrue(comments.first().text.contains("(x2)"))
    }

    @Test
    fun `retro routes every screen to its own app`() {
        mapOf(
            "ui/stats/StatsModal.kt" to "RetroHoursMonitor(viewModel, onDismiss)",
            "ui/profile/LeaderboardModal.kt" to "RetroMessenger(viewModel, myId, myDisplayName, feedEmployeeNames, onFeedTabSelected, onDismiss)",
            "ui/challenges/ChallengesModal.kt" to "RetroChallengesSetup(viewModel, onLoad, onDismiss)",
            "ui/alerts/PastAlertsModal.kt" to "RetroMail(viewModel, onDismiss)",
            "ui/shop/ShopModal.kt" to "RetroKkMart(onDismiss, shopViewModel, profileViewModel)",
            "ui/profile/SettingsModal.kt" to "RetroKkcSpace(profileViewModel, actualName, friends, myId, onDismiss, onNavigateToAlerts, onOpenShop)"
        ).forEach { (path, call) ->
            val src = SourceFiles.mainSource("com/example/timecard/$path").readText()
            assertTrue(path, src.contains("if (LocalTimecardColors.current.isRetro)") && src.contains(call))
        }
        val screen = SourceFiles.mainSource("com/example/timecard/ui/timesheet/TimesheetScreen.kt").readText()
        assertTrue(screen.contains("friends = leaderboardViewModel?.entries ?: emptyList()"))
        // Shop rules stay shared; the profile reuses the shared photo picker.
        val mart = SourceFiles.mainSource("com/example/timecard/ui/shop/RetroKkMart.kt").readText()
        assertTrue(mart.contains("completePurchase(") && mart.contains("ShopOpenEffects(shopViewModel)"))
        assertTrue(SourceFiles.mainSource("com/example/timecard/ui/profile/RetroKkcSpace.kt").readText().contains("rememberAvatarPicker(profileViewModel)"))
    }

    @Test
    fun `fading confirms act on the latest item, not the first one shown`() {
        // remember { x } inside a fading dialog kept the first value: reopening a BUY confirm
        // during the fade-out could show and buy the previous item.
        listOf("ui/shop/LcarsShop.kt", "ui/shop/RetroKkMart.kt", "ui/profile/LcarsSettings.kt", "ui/profile/RetroMessenger.kt").forEach { path ->
            val src = SourceFiles.mainSource("com/example/timecard/$path").readText()
            assertTrue(path, src.contains("rememberLatestNonNull("))
            assertFalse(path, Regex("""remember \{ (pendingPurchase|pending|selectedBadge|info) \}""").containsMatchIn(src))
        }
    }
}
