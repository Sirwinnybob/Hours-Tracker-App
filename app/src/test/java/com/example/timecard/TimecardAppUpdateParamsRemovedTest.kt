package com.example.timecard

import com.example.timecard.testutil.SourceFiles
import org.junit.Assert.assertFalse
import org.junit.Test

class TimecardAppUpdateParamsRemovedTest {

    @Test
    fun deadUpdateDialogParamsAreRemoved() {
        val source = SourceFiles.mainSource("com/example/timecard/TimecardApp.kt").readText()

        assertFalse("TimecardApp must not accept pendingUpdate", source.contains("pendingUpdate: java.io.File?"))
        assertFalse("TimecardApp must not accept onInstallUpdate", source.contains("onInstallUpdate: () -> Unit"))
        assertFalse("TimecardApp must not render the update AlertDialog", source.contains("Update dialog — shown when a newer APK is found"))
    }
}
