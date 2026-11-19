package com.example.timecard.ui.timesheet

import com.example.timecard.testutil.SourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekNavigationWiringTest {
    @Test
    fun `all three layouts offer week navigation even without a saved previous card`() {
        val source = SourceFiles.mainSource("com/example/timecard/ui/timesheet/TimesheetScreen.kt").readText()
        assertFalse(source.contains("if (uiState.hasPreviousWeek)"))
        assertEquals(3, Regex("timesheetViewModel\\.togglePrevWeek\\(\\)").findAll(source).count())
        assertTrue(source.contains("PREVIOUS WEEK"))
        assertTrue(source.contains("PREV WEEK"))
        // Standard layout: This week / Last week segmented switch, gated only on load/save state.
        assertTrue(source.contains("onToggleWeek = { timesheetViewModel.togglePrevWeek() }"))
        assertTrue(source.contains("canSwitchWeek = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch"))
        val header = SourceFiles.mainSource("com/example/timecard/ui/timesheet/standard/StandardHeader.kt").readText()
        assertTrue(header.contains("\"This week\" to false, \"Last week\" to true"))
    }
}
