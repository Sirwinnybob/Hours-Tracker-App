package com.example.timecard.ui.timesheet.retro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.ActiveJob
import com.example.timecard.domain.ActiveJobs
import com.example.timecard.domain.JobValidator
import com.example.timecard.ui.timesheet.TimesheetUiState
import com.example.timecard.ui.timesheet.getRowTotal
import com.example.timecard.ui.timesheet.standard.DayState
import com.example.timecard.ui.timesheet.standard.DaySummary
import com.example.timecard.ui.timesheet.standard.GridActions
import com.example.timecard.ui.timesheet.standard.formatHours

internal val XL_ROW_H = 36.dp
private val NUM_W = 30.dp
internal val XL_JOB_W = 170.dp
private val TOTAL_W = 64.dp

/** A cell address on the sheet (0-based sheet row and column). */
internal data class XlCell(val sheetRow: Int, val col: Int)

/**
 * The worksheet: column letters, row numbers, the yellow header row, one row per job, an empty row
 * to add a job, then DAILY TOTAL (double rule above), SHOP FILL links and LUNCH YES/NO cells.
 */
@Composable
internal fun XlGrid(
    uiState: TimesheetUiState,
    rows: List<XlRow>,
    days: List<DaySummary>,
    activeJobs: List<ActiveJob>,
    actions: GridActions,
    selected: XlCell?,
    lookupRow: Int?,
    flashRow: Int?,
    onSelect: (XlCell) -> Unit,
    onOpenLookup: (dataRow: Int, sheetRow: Int) -> Unit,
    onAddJob: () -> Unit,
    onHoursFocus: (sheetRow: Int, dataRow: Int, day: Int, focused: Boolean) -> Unit,
    lookup: @Composable (dataRow: Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Xl.White)) {
        // Column letters
        Row(Modifier.fillMaxWidth().height(24.dp)) {
            Heading("", Modifier.width(NUM_W).fillMaxHeight(), false)
            Heading(xlColumn(0), Modifier.width(XL_JOB_W).fillMaxHeight(), selected?.col == 0)
            for (c in 1..6) Heading(xlColumn(c), Modifier.weight(1f).fillMaxHeight(), selected?.col == c)
            Heading(xlColumn(7), Modifier.width(TOTAL_W).fillMaxHeight(), selected?.col == 7)
        }
        rows.forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().height(XL_ROW_H)) {
                Heading("${r + 1}", Modifier.width(NUM_W).fillMaxHeight(), selected?.sheetRow == r)
                val sel = { c: Int -> selected == XlCell(r, c) && lookupRow == null }
                val pick = { c: Int -> { onSelect(XlCell(r, c)) } }
                when (row) {
                    XlRow.Header -> {
                        Cell(Modifier.width(XL_JOB_W), sel(0), Xl.HeaderFill, pick(0)) { CellText("JOB", bold = true) }
                        days.forEachIndexed { i, d ->
                            Cell(Modifier.weight(1f), sel(i + 1), if (d.isToday) Xl.TodayHead else Xl.HeaderFill, pick(i + 1)) {
                                Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                    Text(d.label.uppercase(), style = xlText(13.sp, weight = FontWeight.Bold))
                                    if (d.date.isNotEmpty()) Text(d.date, style = xlText(10.sp))
                                }
                            }
                        }
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.HeaderFill, pick(7)) { CellText("TOTAL", bold = true, end = true) }
                    }
                    is XlRow.Job -> {
                        val dataRow = row.dataRow
                        val job = uiState.jobs.getOrElse(dataRow) { "" }
                        val fill = if (flashRow == dataRow) Xl.TodayHead else Xl.White
                        Box(Modifier.width(XL_JOB_W).fillMaxHeight()) {
                            Cell(Modifier.fillMaxSize(), sel(0), fill, { onSelect(XlCell(r, 0)); onOpenLookup(dataRow, r) }) {
                                JobCellText(job, ActiveJobs.find(job, activeJobs)?.name)
                            }
                            if (lookupRow == dataRow) lookup(dataRow)
                        }
                        days.forEachIndexed { d, summary ->
                            HoursCell(
                                value = uiState.hours.getOrNull(dataRow)?.getOrNull(d) ?: "",
                                selected = sel(d + 1),
                                fill = if (flashRow == dataRow) Xl.TodayHead else if (summary.isToday) Xl.TodayCell else Xl.White,
                                description = "${summary.label} hours, row ${dataRow + 1}",
                                onChange = { actions.onHoursChange(dataRow, d, it) },
                                onFocus = { focused ->
                                    if (focused) onSelect(XlCell(r, d + 1)) else actions.onSnapHours(dataRow, d)
                                    onHoursFocus(r, dataRow, d, focused)
                                }
                            )
                        }
                        val total = uiState.getRowTotal(dataRow)
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.White, pick(7)) { CellText(formatHours(total), bold = true, end = true, color = if (total > 0) Xl.Black else Xl.Muted) }
                    }
                    XlRow.NewJob -> {
                        Cell(Modifier.width(XL_JOB_W), sel(0), Xl.White, { onSelect(XlCell(r, 0)); onAddJob() }, label = "Add job") {
                            CellText("tap to add a job", color = Xl.Muted, italic = true)
                        }
                        for (c in 1..6) Cell(Modifier.weight(1f), sel(c), Xl.White, pick(c)) {}
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.White, pick(7)) {}
                    }
                    XlRow.Daily -> {
                        Cell(Modifier.width(XL_JOB_W), sel(0), Xl.White, pick(0), doubleTop = true) { CellText("DAILY TOTAL", bold = true) }
                        days.forEachIndexed { i, d ->
                            val text = if (d.state == DayState.NoTarget && d.total == 0.0) "-" else formatHours(d.total)
                            Cell(Modifier.weight(1f), sel(i + 1), Xl.White, pick(i + 1), doubleTop = true) {
                                CellText(text, bold = true, end = true, color = if (d.state == DayState.Met) Xl.Met else Xl.Black)
                            }
                        }
                        val grand = days.sumOf { it.total }
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.TotalFill, pick(7), doubleTop = true) { CellText(formatHours(grand), bold = true, end = true) }
                    }
                    XlRow.Fill -> {
                        Cell(Modifier.width(XL_JOB_W), sel(0), Xl.White, pick(0)) { CellText("SHOP FILL", italic = true) }
                        days.forEachIndexed { i, d ->
                            Cell(
                                Modifier.weight(1f), sel(i + 1), Xl.White,
                                if (d.canFillShop) ({ onSelect(XlCell(r, i + 1)); actions.onFillShop(d.index) }) else pick(i + 1),
                                label = if (d.canFillShop) "Fill ${d.label} with ${formatHours(d.shortBy)} Shop hours" else null
                            ) {
                                if (d.canFillShop) CellText("+${formatHours(d.shortBy)}", end = true, color = Xl.Link, underline = true)
                            }
                        }
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.White, pick(7)) {}
                    }
                    XlRow.Lunch -> {
                        Cell(Modifier.width(XL_JOB_W), sel(0), Xl.White, pick(0)) { CellText("LUNCH", italic = true) }
                        days.forEachIndexed { i, d ->
                            val taken = d.index !in uiState.noLunchDays
                            Cell(
                                Modifier.weight(1f), sel(i + 1), Xl.White,
                                { onSelect(XlCell(r, i + 1)); actions.onToggleNoLunch(d.index) },
                                label = "Toggle lunch for ${d.label}", role = Role.Switch
                            ) { CellText(if (taken) "YES" else "NO", end = true, color = if (taken) Xl.Black else Xl.Error) }
                        }
                        Cell(Modifier.width(TOTAL_W), sel(7), Xl.White, pick(7)) {}
                    }
                }
            }
        }
    }
}

/** Gray column/row heading; darker and bold when its row or column holds the selection. */
@Composable
private fun Heading(text: String, modifier: Modifier, highlighted: Boolean) {
    Box(modifier.background(if (highlighted) Xl.SelectedHeading else Xl.Face).xlBevel(), contentAlignment = Alignment.Center) {
        Text(text, style = xlText(12.sp, weight = if (highlighted) FontWeight.Bold else FontWeight.Normal))
    }
}

/** A worksheet cell: gridlines right/bottom, optional double rule on top, thick border when selected. */
@Composable
private fun Cell(
    modifier: Modifier,
    selected: Boolean,
    fill: Color,
    onClick: () -> Unit,
    doubleTop: Boolean = false,
    label: String? = null,
    role: Role = Role.Button,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.fillMaxHeight()
            .background(fill)
            .clickable(role = role, onClickLabel = label, onClick = onClick)
            .xlCellChrome(selected, doubleTop)
            .padding(horizontal = 5.dp),
        content = content
    )
}

private fun Modifier.xlCellChrome(selected: Boolean, doubleTop: Boolean): Modifier = drawWithContent {
    drawContent()
    val g = 1.dp.toPx()
    drawLine(Xl.Gridline, Offset(size.width - g / 2, 0f), Offset(size.width - g / 2, size.height), g)
    drawLine(Xl.Gridline, Offset(0f, size.height - g / 2), Offset(size.width, size.height - g / 2), g)
    if (doubleTop) {
        drawLine(Xl.Black, Offset(0f, g / 2), Offset(size.width, g / 2), g)
        drawLine(Xl.Black, Offset(0f, g * 2.5f), Offset(size.width, g * 2.5f), g)
    }
    if (selected) {
        val s = 2.5.dp.toPx()
        drawRect(Xl.Black, topLeft = Offset(s / 2, s / 2), size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s), style = androidx.compose.ui.graphics.drawscope.Stroke(s))
        val h = 6.dp.toPx() // fill handle
        drawRect(Xl.Black, topLeft = Offset(size.width - h, size.height - h), size = androidx.compose.ui.geometry.Size(h, h))
    }
}

@Composable
private fun BoxScope.CellText(
    text: String,
    bold: Boolean = false,
    end: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    color: Color = Xl.Black
) {
    Text(
        text,
        style = xlText(15.sp, color, if (bold) FontWeight.Bold else FontWeight.Normal, italic).copy(textDecoration = if (underline) TextDecoration.Underline else null),
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        textAlign = if (end) TextAlign.End else TextAlign.Start,
        modifier = Modifier.fillMaxWidth().align(if (end) Alignment.CenterEnd else Alignment.CenterStart)
    )
}

/** Job cell: number (bold for SHOP), the active job's name after it, [DELIVERY] in purple, red if invalid. */
@Composable
private fun BoxScope.JobCellText(job: String, name: String?) {
    val delivery = JobValidator.isDeliveryJob(job)
    val invalid = job.isNotBlank() && !JobValidator.isValidJobEntry(job)
    val number = if (delivery) job.trim().dropLast(1) else job.trim()
    val text = buildString {
        append(number)
        if (!name.isNullOrBlank()) append("  ").append(name)
        if (delivery) append("  [DELIVERY]")
    }
    CellText(
        if (job.isBlank()) "" else text,
        bold = number.equals("SHOP", true),
        color = when { invalid -> Xl.Error; delivery -> Xl.Delivery; else -> Xl.Black }
    )
}

/** Editable hours cell: shows "8.5" like Excel, raw text while typing; snaps on leaving. */
@Composable
private fun RowScope.HoursCell(value: String, selected: Boolean, fill: Color, description: String, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.weight(1f).fillMaxHeight().background(fill).xlCellChrome(selected || focused, false).padding(horizontal = 5.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (!focused) {
            val shown = xlHoursText(value)
            Text(shown, style = xlText(15.sp, if (shown == "-") Xl.Muted else Xl.Black), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = xlText(15.sp, if (focused) Xl.Black else Color.Transparent).copy(textAlign = TextAlign.End),
            cursorBrush = SolidColor(Xl.Black),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            // The whole cell is the tap target, not just the one text line in its middle.
            decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) { inner() } },
            modifier = Modifier.fillMaxSize().semantics { contentDescription = description }
                .onFocusChanged { if (it.isFocused != focused) { focused = it.isFocused; onFocus(it.isFocused) } }
        )
    }
}

/** Gridline-only drawBehind for plain boxes outside the grid (kept for the chart object frame). */
internal fun Modifier.xlHairline(color: Color = Xl.Black): Modifier = drawBehind {
    val g = 1.dp.toPx()
    drawRect(color, style = androidx.compose.ui.graphics.drawscope.Stroke(g))
}
