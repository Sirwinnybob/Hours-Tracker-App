package com.example.timecard.ui.timesheet.retro

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.timecard.ui.timesheet.standard.DaySummary
import com.example.timecard.ui.timesheet.standard.formatHours

/**
 * Retro theme timesheet as an Excel 95 window (2026-11-19 mockup "Retro A"). This file holds the
 * palette, the Win95 bevel, and the pure sheet model (rows, cell references, formula-bar text).
 */
object Xl {
    val Face = Color(0xFFC0C0C0)
    val Light = Color(0xFFDFDFDF)
    val Shadow = Color(0xFF808080)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Navy = Color(0xFF000080)
    val TitleEnd = Color(0xFF1084D0)
    val Desktop = Color(0xFF008080)
    val HeaderFill = Color(0xFFFFFFCC)
    val TodayHead = Color(0xFFFFFF00)
    val TodayCell = Color(0xFFFFFFE8)
    val Gridline = Color(0xFFC0C0C0)
    val SelectedHeading = Color(0xFFA0A0A0)
    val Link = Color(0xFF0000FF)
    val Met = Color(0xFF008000)
    val TotalFill = Color(0xFFCCFFCC)
    val Delivery = Color(0xFF800080)
    val Error = Color(0xFFC00000)
    val Muted = Color(0xFFA0A0A0)
    val ExcelGreen = Color(0xFF107C41)
    val SeriesHours = Color(0xFF9999FF)
    val SeriesHoursSide = Color(0xFF6666CC)
    val SeriesTarget = Color(0xFF993366)
    val SeriesTargetSide = Color(0xFF662244)

    /** Chart value axis top (hours). */
    const val CHART_MAX = 12.0
}

/** Arial-like UI/cell text (Android's sans), the way Excel 95 drew its grid. */
internal fun xlText(size: TextUnit, color: Color = Xl.Black, weight: FontWeight = FontWeight.Normal, italic: Boolean = false) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size, color = color, fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)

/** Classic two-line Win95 bevel (white/black outside, light/dark gray inside); [inset] for sunken fields. */
internal fun Modifier.xlBevel(inset: Boolean = false, line: Dp = 1.dp): Modifier = drawBehind {
    val s = line.toPx()
    val w = size.width
    val h = size.height
    val outerTL = if (inset) Xl.Shadow else Xl.White
    val outerBR = if (inset) Xl.White else Xl.Black
    val innerTL = if (inset) Xl.Black else Xl.Light
    val innerBR = if (inset) Xl.Light else Xl.Shadow
    fun frame(i: Float, tl: Color, br: Color) {
        drawLine(tl, Offset(i, i + s / 2), Offset(w - i, i + s / 2), s)
        drawLine(tl, Offset(i + s / 2, i), Offset(i + s / 2, h - i), s)
        drawLine(br, Offset(i, h - i - s / 2), Offset(w - i, h - i - s / 2), s)
        drawLine(br, Offset(w - i - s / 2, i), Offset(w - i - s / 2, h - i), s)
    }
    frame(0f, outerTL, outerBR)
    frame(s, innerTL, innerBR)
}

/** One sheet row. Spreadsheet row numbers are the list position + 1. */
internal sealed interface XlRow {
    data object Header : XlRow
    data class Job(val dataRow: Int) : XlRow
    /** Empty row under the jobs: tapping its job cell adds a job. */
    data object NewJob : XlRow
    data object Daily : XlRow
    data object Fill : XlRow
    data object Lunch : XlRow
}

/** Sheet columns: A = job, B..G = Mon..Sat, H = total. */
internal const val XL_JOB_COL = 0
internal const val XL_TOTAL_COL = 7

internal fun xlSheetRows(visibleDataRows: List<Int>): List<XlRow> =
    listOf(XlRow.Header) + visibleDataRows.map { XlRow.Job(it) } + listOf(XlRow.NewJob, XlRow.Daily, XlRow.Fill, XlRow.Lunch)

/** Column letter for a 0-based column (A..Z). */
internal fun xlColumn(col: Int): String = ('A' + col).toString()

/** "C4" for 0-based sheet row 3, column 2. */
internal fun xlRef(sheetRow: Int, col: Int): String = xlColumn(col) + (sheetRow + 1)

/**
 * What the formula bar shows for a cell: =SUM(...) for row totals and the Daily total row,
 * =FILL("SHOP",n) for a Shop fill link, otherwise the cell's own text.
 */
internal fun xlFormulaFor(rows: List<XlRow>, sheetRow: Int, col: Int, cellText: String, days: List<DaySummary>): String {
    val row = rows.getOrNull(sheetRow) ?: return cellText
    val jobRows = rows.indices.filter { rows[it] is XlRow.Job }
    return when (row) {
        is XlRow.Job -> if (col == XL_TOTAL_COL) "=SUM(${xlRef(sheetRow, 1)}:${xlRef(sheetRow, 6)})" else cellText
        XlRow.Daily -> when {
            col == XL_TOTAL_COL -> "=SUM(${xlRef(sheetRow, 1)}:${xlRef(sheetRow, 6)})"
            col in 1..6 && jobRows.isNotEmpty() -> "=SUM(${xlRef(jobRows.first(), col)}:${xlRef(jobRows.last(), col)})"
            else -> cellText
        }
        XlRow.Fill -> {
            val d = days.getOrNull(col - 1)
            if (col in 1..6 && d != null && d.canFillShop) "=FILL(\"SHOP\",${formatHours(d.shortBy)})" else cellText
        }
        else -> cellText
    }
}

/** Hours as Excel shows them in a cell: one decimal, blank for empty, "-" for an explicit 0. */
internal fun xlHoursText(raw: String): String {
    val v = raw.trim().toDoubleOrNull() ?: return raw.trim()
    return if (v == 0.0) "-" else formatHours(v)
}

/** Status bar text for the save state. */
internal fun xlStatusText(saving: Boolean, error: Boolean, editing: Boolean): String = when {
    error -> "Cannot save TIMECARD.XLS - tap Save to retry"
    saving -> "Saving TIMECARD.XLS"
    editing -> "Enter"
    else -> "Ready"
}
