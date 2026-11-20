package com.example.timecard.ui.timesheet.retro

import com.example.timecard.testutil.SourceFiles
import com.example.timecard.ui.timesheet.standard.summarizeDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Retro Excel 95 timesheet (2026-11-19 mockup A): sheet rows, cell references, formula bar, steps. */
class RetroExcelModelTest {

    private val days = summarizeDays(listOf(9.0, 6.0, 0.0, 0.0, 4.0, 0.0), null, null)

    @Test
    fun `sheet rows wrap the jobs with header, new-job, daily, fill and lunch rows`() {
        val rows = xlSheetRows(listOf(0, 2))
        assertEquals(
            listOf(XlRow.Header, XlRow.Job(0), XlRow.Job(2), XlRow.NewJob, XlRow.Daily, XlRow.Fill, XlRow.Lunch),
            rows
        )
        assertEquals("A", xlColumn(0))
        assertEquals("H", xlColumn(7))
        assertEquals("C4", xlRef(3, 2))
    }

    @Test
    fun `formula bar shows sums and shop fill like a spreadsheet`() {
        val rows = xlSheetRows(listOf(0, 1, 4))
        // Row total of the first job row (sheet row 1 = spreadsheet row 2).
        assertEquals("=SUM(B2:G2)", xlFormulaFor(rows, 1, XL_TOTAL_COL, "", days))
        // Daily total column B sums the job rows 2..4.
        val daily = rows.indexOf(XlRow.Daily)
        assertEquals("=SUM(B2:B4)", xlFormulaFor(rows, daily, 1, "", days))
        assertEquals("=SUM(B${daily + 1}:G${daily + 1})", xlFormulaFor(rows, daily, XL_TOTAL_COL, "", days))
        // Shop fill: Tue is 3.0 short, Mon is met (no fill), Sat never fills.
        val fill = rows.indexOf(XlRow.Fill)
        assertEquals("=FILL(\"SHOP\",3.0)", xlFormulaFor(rows, fill, 2, "", days))
        assertEquals("", xlFormulaFor(rows, fill, 1, "", days))
        assertEquals("", xlFormulaFor(rows, fill, 6, "", days))
        // Plain cells echo their text.
        assertEquals("711D", xlFormulaFor(rows, 2, XL_JOB_COL, "711D", days))
        assertEquals("8.5", xlFormulaFor(rows, 2, 3, "8.5", days))
    }

    @Test
    fun `cell text, status bar, quick steps and chart scale`() {
        assertEquals("8.5", xlHoursText("8.50"))
        assertEquals("-", xlHoursText("0"))
        assertEquals("", xlHoursText(""))
        assertEquals("abc", xlHoursText("abc"))
        assertEquals("Ready", xlStatusText(saving = false, error = false, editing = false))
        assertEquals("Enter", xlStatusText(saving = false, error = false, editing = true))
        assertEquals("Saving TIMECARD.XLS", xlStatusText(saving = true, error = false, editing = false))
        assertTrue(xlStatusText(saving = false, error = true, editing = false).contains("retry"))
        assertEquals("1.25", xlStep(1.0, 0.25))
        assertEquals("", xlStep(0.25, -0.25))
        assertEquals("", xlStep(0.0, -0.25))
        assertEquals("9.50", xlStep(8.0, 1.5))
        assertEquals(0.75f, xlBarShare(9.0), 0.0001f)
        assertEquals(1f, xlBarShare(14.0), 0.0001f)
        assertEquals(0f, xlBarShare(-1.0), 0.0001f)
    }

    @Test
    fun `retro timesheet uses the excel sheet and the shared job rules`() {
        val screen = SourceFiles.mainSource("com/example/timecard/ui/timesheet/TimesheetScreen.kt").readText()
        assertTrue(screen.contains("com.example.timecard.ui.timesheet.retro.RetroExcelTimesheet("))
        assertFalse(screen.contains("TimesheetGrid(uiState = uiState"))
        val picker = SourceFiles.mainSource("com/example/timecard/ui/timesheet/retro/RetroExcelAutoComplete.kt").readText()
        assertTrue(picker.contains("resolveJobPick(it, delivery, jobsOnCard)"))
        assertTrue(picker.contains("ActiveJobs.search(trimmed, activeJobs)"))
        // Motion follows the system animator setting like the rest of Retro.
        listOf("RetroExcelSheet.kt", "RetroExcelAutoComplete.kt").forEach {
            assertTrue(it, SourceFiles.mainSource("com/example/timecard/ui/timesheet/retro/$it").readText().contains("rememberRetroMotion()"))
        }
    }
}
