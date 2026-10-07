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
        // The bar follows Hours Tracker's own light/dark mode, live.
        assertTrue(src.contains("darkTheme = themeState.mode != ThemeMode.Light"))
        val call = src.substring(src.indexOf("KkcNavBar("))
        assertTrue(call.substringBefore("onNavigate").contains("darkTheme = "))
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
