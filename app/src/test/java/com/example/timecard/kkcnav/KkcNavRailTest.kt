package com.example.timecard.kkcnav

import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KkcNavRailTest {

    private fun read(relative: String): String {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            val candidate = File(dir, "app/src/main/$relative")
            if (candidate.exists()) return candidate.readText()
            val direct = File(dir, "src/main/$relative")
            if (direct.exists()) return direct.readText()
            dir = dir.parentFile ?: return@repeat
        }
        error("Unable to locate $relative")
    }

    @Test
    fun landscapeRuleMatchesKkcUseLandscapeChrome() {
        assertTrue(kkcUseLandscapeRail(1920f, 1200f))
        assertFalse(kkcUseLandscapeRail(1200f, 1920f))
        assertFalse(kkcUseLandscapeRail(1200f, 1200f))
        assertFalse(kkcUseLandscapeRail(0f, 0f))
        assertFalse(kkcUseLandscapeRail(Float.NaN, 100f))
        assertFalse(kkcUseLandscapeRail(Float.POSITIVE_INFINITY, 100f))
    }

    @Test
    fun railSizesMatchKkcLandscapeChrome() {
        assertEquals(62.dp, KkcRailSlotHeight)
        assertEquals(20.dp, KkcRailCornerRadius)
        assertEquals(76.dp, KkcRailIconColumnWidth)
        assertEquals(6.dp, KkcRailEdgeMargin)
        // KKC LandscapeRailClearance = RailIconColumnWidth + RailEdgeMargin + 6dp.
        assertEquals(88.dp, KkcLandscapeRailClearance)
    }

    @Test
    fun settingsAtTheBottomReturnsSettingsToKkc() {
        assertEquals("settings", KKC_DEST_SETTINGS)
        val request = KkcReturnNavigation.requestFor(KKC_DEST_SETTINGS)
        assertEquals("settings", request?.destination)
    }

    @Test
    fun batteryFillSweepsRedToGreenLikeKkc() {
        assertEquals(kkcBatteryFillColor(-5), kkcBatteryFillColor(0))
        assertEquals(kkcBatteryFillColor(150), kkcBatteryFillColor(100))
    }

    @Test
    fun timecardAppSwapsTheBarForTheRailInLandscape() {
        val app = read("java/com/example/timecard/TimecardApp.kt")
        assertTrue(app.contains("val kkcLandscapeRail = kkcNavBar != null && rememberKkcLandscapeRail()"))
        assertTrue(app.contains("KkcNavRail("))
        assertTrue(app.contains("Modifier.align(if (kkcLandscapeRail) Alignment.CenterEnd else Alignment.BottomCenter)"))
        // The rail replaces only the glass bar: LCARS / Retro keep their own bars, gated as before.
        assertTrue(app.contains("visible = !kkcShownLcars && !kkcRetractingNow && !kkcIsRetro"))
        assertTrue(app.contains("kkcLandscapeNav = kkcLandscapeRail,"))
    }

    @Test
    fun timesheetLeavesRoomOnTheRightButPaintsTheBackgroundUnderTheRail() {
        val timesheet = read("java/com/example/timecard/ui/timesheet/TimesheetScreen.kt")
        val padded = Regex(
            // Standard layout paints its KKC-look background (StandardLook) under the rail, then pads.
            "\\.background\\(look\\.background\\)\\s*\\.statusBarsPadding\\(\\)\\s*" +
                "\\.padding\\(end = if \\(kkcLandscapeNav\\) com\\.example\\.timecard\\.kkcnav\\.KkcLandscapeRailClearance else 0\\.dp\\)"
        )
        assertTrue(padded.containsMatchIn(timesheet))
    }

    @Test
    fun landscapeSlotsAddSettingsOnceAtTheEnd() {
        val dests = listOf("dashboard", "jobs", "hours", "timecard", "supply", "standards")
        assertEquals(
            listOf("dashboard", "jobs", "calculator", "hours", "timecard", "supply", "standards", "settings"),
            kkcLandscapeSlots(dests)
        )
        assertEquals(1, kkcLandscapeSlots(dests + "settings").count { it == "settings" })
        assertEquals(kkcNavSlots(dests), kkcLcarsSlots(dests, vertical = false))
        assertEquals(kkcLandscapeSlots(dests), kkcLcarsSlots(dests, vertical = true))
    }

    @Test
    fun lcarsMovesTheBlocksIntoARightSpineInLandscape() {
        val timesheet = read("java/com/example/timecard/ui/timesheet/TimesheetScreen.kt")
        assertTrue(timesheet.contains("= if (kkcNavBar != null && kkcLandscapeNav)"))
        assertTrue(timesheet.contains("anim = kkcLcarsAnim, vertical = true"))
        assertTrue(timesheet.contains("rightSpine = rightSpineContent"))
        val frame = read("java/com/example/timecard/ui/timesheet/lcars/LcarsClassicFrame.kt")
        assertTrue(frame.contains("if (rightSpine != null)"))
        val app = read("java/com/example/timecard/TimecardApp.kt")
        assertTrue(app.contains("vertical = kkcLandscapeRail"))
    }

    @Test
    fun retroDocksTheTaskbarOnTheRightInLandscape() {
        val timesheet = read("java/com/example/timecard/ui/timesheet/TimesheetScreen.kt")
        assertTrue(timesheet.contains("sideBar = kkcRetroNavBar?.takeIf { kkcLandscapeNav }?.let"))
        assertTrue(timesheet.contains("KkcRetroNavRail(payload = payload, onNavigate = onKkcNavigate)"))
        val frame = read("java/com/example/timecard/ui/theme/RetroChrome.kt")
        assertTrue(frame.contains("sideBar?.invoke()"))
        val app = read("java/com/example/timecard/TimecardApp.kt")
        assertTrue(app.contains("KkcRetroNavRail("))
        val retro = read("java/com/example/timecard/kkcnav/KkcRetroNav.kt")
        assertTrue(retro.contains("kkcLandscapeSlots(payload.destinations)"))
    }

    @Test
    fun railUsesTheFrostedPatternAndHtStyling() {
        val rail = read("java/com/example/timecard/kkcnav/KkcNavRail.kt")
        // External shadow, clip, then Haze (never Surface shadowElevation over translucent glass).
        val glass = rail.substringAfter("private fun Modifier.kkcRailGlass(")
        val shadow = glass.indexOf(".shadow(")
        val clip = glass.indexOf(".clip(shape)")
        val fill = glass.indexOf(".then(fill)")
        assertTrue(shadow in 0 until clip && clip < fill)
        assertFalse(rail.contains("shadowElevation"))
        // Colors come from KkcNavBarStyle like the portrait bar.
        assertTrue(rail.contains("kkcNavTints(style)"))
        assertTrue(rail.contains("kkcNavSelectionFill(colors, style.boldMode)"))
        assertTrue(rail.contains("kkcNavSlots(payload.destinations)"))
        assertTrue(rail.contains("kkcBadgeCount(route, payload)"))
        assertTrue(rail.contains("onNavigate(KKC_DEST_SETTINGS)"))
    }
}
