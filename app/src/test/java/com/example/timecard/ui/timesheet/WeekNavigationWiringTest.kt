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
        // Standard 1 + LCARS 2 (01-THIS WEEK / 02-LAST WEEK side panels) + Retro 1.
        assertEquals(4, Regex("timesheetViewModel\\.togglePrevWeek\\(\\)").findAll(source).count())
        assertTrue(source.contains("LcarsPanel(\"01-THIS WEEK\""))
        assertTrue(source.contains("LcarsPanel(\"02-LAST WEEK\""))
        assertTrue(source.contains("if (!uiState.isViewingPrevious && canSwitchWeek) timesheetViewModel.togglePrevWeek()"))
        assertTrue(source.contains("PREV WEEK"))
        // Standard layout: This week / Last week segmented switch, gated only on load/save state.
        assertTrue(source.contains("onToggleWeek = { timesheetViewModel.togglePrevWeek() }"))
        assertTrue(source.contains("canSwitchWeek = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch"))
        val header = SourceFiles.mainSource("com/example/timecard/ui/timesheet/standard/StandardHeader.kt").readText()
        assertTrue(header.contains("\"This week\" to false, \"Last week\" to true"))
    }
}
