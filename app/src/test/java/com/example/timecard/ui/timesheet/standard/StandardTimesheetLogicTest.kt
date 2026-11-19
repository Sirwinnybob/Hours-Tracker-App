package com.example.timecard.ui.timesheet.standard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.timecard.domain.ActiveJob
import com.example.timecard.kkcnav.KkcNavBarColors
import com.example.timecard.ui.theme.DarkTimecardColors
import com.example.timecard.ui.theme.LightTimecardColors
import com.example.timecard.ui.theme.SunriseTimecardColors
import com.example.timecard.ui.timesheet.TimesheetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StandardTimesheetLogicTest {

    // ── Day summary ──────────────────────────────────────────────────────────────

    @Test
    fun `day states and shop fill follow the 9-9-9-9-4 targets`() {
        val start = LocalDate.of(2026, 11, 16)
        val days = summarizeDays(listOf(9.0, 9.0, 9.0, 6.0, 0.0, 0.0), start, todayIndex = 3)
        assertEquals(listOf(DayState.Met, DayState.Met, DayState.Met, DayState.Partial, DayState.NotStarted, DayState.NoTarget), days.map { it.state })
        assertEquals(listOf("Nov 16", "Nov 17", "Nov 18", "Nov 19", "Nov 20", "Nov 21"), days.map { it.date })
        assertEquals(3.0, days[3].shortBy, 0.0)
        assertEquals(4.0, days[4].shortBy, 0.0)
        assertEquals(listOf(false, false, false, true, true, false), days.map { it.canFillShop })
        assertEquals(listOf(false, false, false, true, false, false), days.map { it.isToday })
        assertEquals(6f / 9f, days[3].progress, 0.0001f)
        assertEquals(0f, days[5].progress, 0f)
    }

    @Test
    fun `over target is met and never offers a fill`() {
        val day = summarizeDays(listOf(10.5, 0.0, 0.0, 0.0, 0.0, 3.0), null, null)
        assertEquals(DayState.Met, day[0].state)
        assertEquals(0.0, day[0].shortBy, 0.0)
        assertFalse(day[0].canFillShop)
        assertEquals("", day[0].date)
        // Saturday hours are shown but Saturday has no target.
        assertEquals(DayState.NoTarget, day[5].state)
    }

    @Test
    fun `today index only inside the viewed week`() {
        val start = LocalDate.of(2026, 11, 16)
        assertEquals(3, todayIndexIn(start, LocalDate.of(2026, 11, 19)))
        assertNull(todayIndexIn(start, LocalDate.of(2026, 11, 22)))  // Sunday
        assertNull(todayIndexIn(start, LocalDate.of(2026, 11, 13)))
        assertNull(todayIndexIn(null, LocalDate.of(2026, 11, 19)))
        assertNull(weekStartOf("W12"))
        assertEquals("Week of Nov 16", weekOfLabel("2026-11-16"))
        assertEquals("Mon Nov 16 – Sat Nov 21", weekRangeLabel(start))
    }

    // ── Visible rows ─────────────────────────────────────────────────────────────

    @Test
    fun `grid shows rows with a job or hours plus the pending row`() {
        val jobs = listOf("SHOP", "725", "", "", "")
        val hours = listOf(
            listOf("1", "", "", "", "", ""),
            listOf("", "", "", "", "", ""),
            listOf("", "2.00", "", "", "", ""),   // hours without a job must stay visible
            listOf("", "", "", "", "", ""),
            listOf("", "", "", "", "", "")
        )
        assertEquals(listOf(0, 1, 2), visibleRowIndices(jobs, hours, 5, pendingRow = null))
        assertEquals(listOf(0, 1, 2, 4), visibleRowIndices(jobs, hours, 5, pendingRow = 4))
        assertEquals(3, firstEmptyRow(jobs, hours, 5))
        assertNull(firstEmptyRow(listOf("SHOP"), listOf(listOf("", "", "", "", "", "")), 1))
    }

    // ── Job shares ───────────────────────────────────────────────────────────────

    @Test
    fun `week split adds the same job across rows and names active jobs`() {
        val state = TimesheetUiState(
            numRows = 4,
            jobs = listOf("SHOP", "725", "591", "725"),
            hours = listOf(
                listOf("1", "0.5", "", "", "", ""),
                listOf("6", "5", "", "", "", ""),
                listOf("2", "", "", "", "", ""),
                listOf("", "", "3", "", "", "")
            )
        )
        val shares = jobShares(state, listOf(ActiveJob("725", "WIECHERT 34 CHURCHVIEW")))
        assertEquals(listOf("725", "591", "SHOP"), shares.map { it.job })
        assertEquals(14.0, shares[0].hours, 0.0)
        assertEquals("WIECHERT 34 CHURCHVIEW", shares[0].name)
        assertNull(shares[1].name)
    }

    // ── Theme rule ───────────────────────────────────────────────────────────────

    private val ducksLight = KkcNavBarColors(
        primary = 0xFF007030.toInt(), onSurfaceVariant = 0xFF444444.toInt(), surfaceVariant = 0xFFE0E0E0.toInt(),
        frostedBase = 0xFFC4CF8E.toInt(), frostedContent = 0xFF000000.toInt(),
        boldGradient = listOf(0xFF0B752F.toInt(), 0xFFFEE11A.toInt()),
        badgeContainer = 0xFFB3261E.toInt(), badgeContent = 0xFFFFFFFF.toInt()
    )

    @Test
    fun `no Hours Tracker theme follows KKC, an equipped theme or no payload keeps Hours Tracker colors`() {
        val kkc = standardLookSource(followKkcTheme = true, kkcColors = ducksLight, darkTheme = false, ht = LightTimecardColors)
        assertEquals(Color(0xFF007030).toArgb(), kkc.primary.toArgb())
        // KKC's second gradient stop (Ducks yellow) highlights the selected segment.
        assertEquals(Color(0xFFFEE11A).toArgb(), kkc.segSelected.toArgb())

        val equipped = standardLookSource(followKkcTheme = false, kkcColors = ducksLight, darkTheme = false, ht = SunriseTimecardColors)
        assertEquals(SunriseTimecardColors.accent.toArgb(), equipped.primary.toArgb())

        val noPayload = standardLookSource(followKkcTheme = true, kkcColors = null, darkTheme = true, ht = DarkTimecardColors)
        assertEquals(DarkTimecardColors.accent.toArgb(), noPayload.primary.toArgb())
        assertTrue(noPayload.isDark)
    }

    @Test
    fun `single-color KKC theme still gets a visible selected segment`() {
        val flat = ducksLight.copy(boldGradient = listOf(0xFF1E5FAF.toInt(), 0xFF1E5FAF.toInt()), primary = 0xFF1E5FAF.toInt())
        val light = standardLookFromKkc(flat, dark = false)
        assertEquals(Color.White.toArgb(), light.segSelected.toArgb())
        val dark = standardLookFromKkc(flat, dark = true)
        assertTrue(dark.isDark)
        assertFalse(dark.segSelected.toArgb() == dark.primary.toArgb())
    }
}
