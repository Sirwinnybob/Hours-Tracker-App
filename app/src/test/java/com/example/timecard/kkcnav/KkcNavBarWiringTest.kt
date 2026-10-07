package com.example.timecard.kkcnav

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KkcNavBarWiringTest {

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
    fun mainActivityParsesThePayloadOnlyWhenLaunchedByKkc() {
        val src = read("java/com/example/timecard/MainActivity.kt")
        assertTrue(src.contains("if (launchedByKkc) KkcNavBarPayload.parse"))
        assertTrue(src.contains("kkcNavBar = kkcNavBar"))
    }

    @Test
    fun timecardAppIsTheHazeSourceAndDrawsTheBarOverlay() {
        val src = read("java/com/example/timecard/TimecardApp.kt")
        assertTrue(src.contains("hazeSource(kkcHazeState)"))
        assertTrue(src.contains("KkcNavBar("))
        assertTrue(src.contains("KkcReturnNavigation.navigate("))
        // Theme source: standard HT theme → KKC colors; equipped HT theme → HT colors; LCARS → band.
        assertTrue(src.contains("kkcNavBarSource(accentKey)"))
        // Light/dark is HT's EFFECTIVE mode (immersive themes can force it), not just the toggle.
        assertTrue(src.contains("darkTheme = colors.isDark"))
        // The call site (not the import): "KkcNavBar(" followed by a line break (LF or CRLF).
        val callStart = Regex("KkcNavBar\\(\\s*\\r?\\n").find(src)?.range?.first ?: -1
        assertTrue("KkcNavBar call site not found", callStart >= 0)
        assertTrue(src.substring(callStart).substringBefore("onNavigate").contains("style = "))
    }

    @Test
    fun lcarsUsesTheFrameBandOnTheTimesheetAndAStandaloneBandOnLogin() {
        val app = read("java/com/example/timecard/TimecardApp.kt")
        assertTrue(app.contains("KkcLcarsNavBand("))
        assertTrue(app.contains("kkcNavBar = if (kkcNavSource == KkcNavBarSource.Lcars && kkcShownLcars) kkcNavBar else null"))
        // Theme is chosen in the Settings dialog (its own window): the navbar transition waits for focus
        // so it isn't played hidden behind the dialog.
        assertTrue(app.contains("LocalWindowInfo.current.isWindowFocused"))
        val timesheet = read("java/com/example/timecard/ui/timesheet/TimesheetScreen.kt")
        assertTrue(timesheet.contains("KkcLcarsNavButtons("))
        assertTrue(timesheet.contains("footerHeight = if (kkcNavBar != null) KKC_LCARS_NAV_HEIGHT"))
    }

    @Test
    fun lcarsBlocksShowTheKkcNavIcons() {
        val lcars = read("java/com/example/timecard/kkcnav/KkcLcarsNav.kt")
        assertTrue(lcars.contains("kkcNavIcon(route, selected)"))
    }

    @Test
    fun barFloatsOverFullHeightContentWithNoBottomInset() {
        val src = read("java/com/example/timecard/TimecardApp.kt")
        assertFalse(src.contains("padding(bottom = kkcNavBarInset)"))
        assertFalse(src.contains("kkcNavBarInset"))
        assertFalse(src.contains("kkcNavBarHeightPx"))
        assertFalse(src.contains("onSizeChanged"))
    }

    @Test
    fun manifestCanSeeKkcPackage() {
        val manifest = read("AndroidManifest.xml")
        assertTrue(manifest.contains("<package android:name=\"com.kkc.sheettracker\" />"))
    }

    @Test
    fun doubleTapGuard() {
        val src = read("java/com/example/timecard/kkcnav/KkcReturnNavigation.kt")
        assertTrue(src.contains("if (activity.isFinishing) return"))
    }

    @Test
    fun kkcFallbackButtonsStillFinish() {
        assertTrue(read("java/com/example/timecard/ui/timesheet/TimesheetScreen.kt").contains("Text(\"← KKC\""))
        assertTrue(read("java/com/example/timecard/ui/login/NameCard.kt").contains("onClick = { activity?.finish() }"))
    }
}
