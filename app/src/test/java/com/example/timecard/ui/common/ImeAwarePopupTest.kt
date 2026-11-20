package com.example.timecard.ui.common

import com.example.timecard.testutil.SourceFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Job pickers stay above the on-screen keyboard (LCARS ADD JOB was covered, 2026-11-20). */
class ImeAwarePopupTest {
    // Window 2800 px tall; keyboard 1100 px, so the usable bottom is 1700.
    @Test fun `stays under the anchor when it fits above the keyboard`() {
        assertEquals(600, imeAwarePopupY(anchorTop = 500, anchorBottom = 600, popupHeight = 900, windowHeight = 2800, imeHeight = 1100, overlayAnchor = false))
    }

    @Test fun `flips above the anchor when the keyboard would cover it`() {
        // Low row (ADD JOB): under it would end at 2700, behind the keyboard; above fits.
        assertEquals(500, imeAwarePopupY(anchorTop = 1400, anchorBottom = 1500, popupHeight = 900, windowHeight = 2800, imeHeight = 1100, overlayAnchor = false))
    }

    @Test fun `slides up to sit on the keyboard when neither side fits`() {
        assertEquals(500, imeAwarePopupY(anchorTop = 600, anchorBottom = 700, popupHeight = 1200, windowHeight = 2800, imeHeight = 1100, overlayAnchor = false))
        // Taller than the space left: pinned to the top, never off screen.
        assertEquals(0, imeAwarePopupY(anchorTop = 600, anchorBottom = 700, popupHeight = 2000, windowHeight = 2800, imeHeight = 1100, overlayAnchor = false))
    }

    @Test fun `in-cell editors overlay the anchor and slide up only when needed`() {
        assertEquals(900, imeAwarePopupY(anchorTop = 900, anchorBottom = 980, popupHeight = 600, windowHeight = 2800, imeHeight = 1100, overlayAnchor = true))
        assertEquals(1100, imeAwarePopupY(anchorTop = 1500, anchorBottom = 1580, popupHeight = 600, windowHeight = 2800, imeHeight = 1100, overlayAnchor = true))
        // No keyboard: the whole window is usable.
        assertEquals(1500, imeAwarePopupY(anchorTop = 1500, anchorBottom = 1580, popupHeight = 600, windowHeight = 2800, imeHeight = 0, overlayAnchor = true))
    }

    @Test fun `all three job pickers use the keyboard-aware placement`() {
        listOf(
            "ui/timesheet/lcars/LcarsJobLookup.kt" to "rememberImeAwarePopupPosition()",
            "ui/timesheet/standard/JobPickerPopup.kt" to "rememberImeAwarePopupPosition()",
            "ui/timesheet/retro/RetroExcelAutoComplete.kt" to "rememberImeAwarePopupPosition(overlayAnchor = true)"
        ).forEach { (path, call) ->
            assertTrue(path, SourceFiles.mainSource("com/example/timecard/$path").readText().contains(call))
        }
    }
}
