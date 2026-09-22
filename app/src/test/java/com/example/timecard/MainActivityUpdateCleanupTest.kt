package com.example.timecard

import com.example.timecard.testutil.SourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityUpdateCleanupTest {

    @Test
    fun selfUpdateScanAndDialogWiringIsRemoved() {
        val source = SourceFiles.mainSource("com/example/timecard/MainActivity.kt").readText()

        assertFalse("MainActivity.kt must not call updateManager.checkForUpdates()", source.contains("updateManager.checkForUpdates()"))
        assertFalse("MainActivity.kt must not pass pendingUpdate to TimecardApp", source.contains("pendingUpdate = updateManager.pendingUpdateApk"))
        assertFalse("MainActivity.kt must not pass onInstallUpdate to TimecardApp", source.contains("onInstallUpdate = { updateManager.installPendingUpdate() }"))
        assertTrue("MainActivity.kt must still wire onReinstallLatest as a manual fallback", source.contains("onReinstallLatest = { updateManager.reinstallLatest() }"))
    }
}
