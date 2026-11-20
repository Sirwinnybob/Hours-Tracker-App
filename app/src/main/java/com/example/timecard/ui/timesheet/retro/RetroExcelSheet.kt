package com.example.timecard.ui.timesheet.retro

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.timecard.domain.ActiveJob
import com.example.timecard.ui.theme.RetroIcon
import com.example.timecard.ui.theme.RetroPixelIcon
import com.example.timecard.ui.theme.rememberRetroMotion
import com.example.timecard.ui.timesheet.SaveStatus
import com.example.timecard.ui.timesheet.TimesheetUiState
import com.example.timecard.ui.timesheet.getDayTotal
import com.example.timecard.ui.timesheet.getGrandTotal
import com.example.timecard.ui.timesheet.standard.GridActions
import com.example.timecard.ui.timesheet.standard.JobPick
import com.example.timecard.ui.timesheet.standard.firstEmptyRow
import com.example.timecard.ui.timesheet.standard.formatHours
import com.example.timecard.ui.timesheet.standard.summarizeDays
import com.example.timecard.ui.timesheet.standard.todayIndexIn
import com.example.timecard.ui.timesheet.standard.visibleRowIndices
import com.example.timecard.ui.timesheet.standard.weekStartOf
import java.time.LocalDate
import java.util.Locale

/** The worksheet tabs at the bottom of the window. */
enum class XlSheet(val label: String) { ThisWeek("This Week"), LastWeek("Last Week"), Chart("Chart1") }

/** Quarter-hour value for a quick step from [current] (blank when it reaches 0), like the other layouts. */
internal fun xlStep(current: Double, delta: Double): String {
    val r = Math.round((current + delta).coerceAtLeast(0.0) * 4.0) / 4.0
    return if (r > 0) String.format(Locale.US, "%.2f", r) else ""
}

/**
 * Retro theme timesheet: an Excel 95 window (title bar, menus, toolbar, formula bar, worksheet,
 * embedded 3D chart, sheet tabs, status bar) on a teal desktop, inside the Retro homepage frame.
 * THIS WEEK / LAST WEEK are sheet tabs; Chart1 shows the week-comparison charts ([chartSheet]).
 */
@Composable
internal fun RetroExcelTimesheet(
    uiState: TimesheetUiState,
    activeJobs: List<ActiveJob>,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: GridActions,
    sheet: XlSheet,
    canSwitchWeek: Boolean,
    onSelectSheet: (XlSheet) -> Unit,
    onSave: () -> Unit,
    chartSheet: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = rememberRetroMotion()
    val focusManager = LocalFocusManager.current
    var selected by remember { mutableStateOf<XlCell?>(null) }
    var pendingRow by remember { mutableStateOf<Int?>(null) }
    var lookupRow by remember { mutableStateOf<Int?>(null) }
    var flashRow by remember { mutableStateOf<Int?>(null) }
    var hoursFocus by remember { mutableStateOf<Pair<Int, Int>?>(null) } // dataRow to day
    LaunchedEffect(flashRow) { if (flashRow != null) { kotlinx.coroutines.delay(1400); flashRow = null } }
    LaunchedEffect(uiState.activeWeekDate) { selected = null; lookupRow = null; pendingRow = null }

    val weekStart = weekStartOf(uiState.activeWeekDate)
    val today = if (uiState.isViewingPrevious) null else todayIndexIn(weekStart, LocalDate.now())
    val days = summarizeDays((0 until 6).map { uiState.getDayTotal(it) }, weekStart, today)
    val rows = xlSheetRows(visibleRowIndices(uiState.jobs, uiState.hours, uiState.numRows, pendingRow))
    val closeLookup: () -> Unit = {
        val r = lookupRow
        lookupRow = null
        if (r != null && r == pendingRow && uiState.jobs.getOrElse(r) { "" }.isBlank()) pendingRow = null
    }
    val addJob: () -> Unit = {
        focusManager.clearFocus()
        val target = firstEmptyRow(uiState.jobs, uiState.hours, uiState.numRows) ?: actions.onAddRow()
        pendingRow = target
        lookupRow = target
    }
    val cellText = { cell: XlCell -> xlCellText(uiState, rows, days, cell) }
    val formula = selected?.let { xlFormulaFor(rows, it.sheetRow, it.col, cellText(it), days) } ?: ""

    Box(modifier.fillMaxSize().background(Xl.Desktop).padding(6.dp)) {
        Column(Modifier.fillMaxSize().background(Xl.Face).xlBevel(line = 1.5.dp).padding(3.dp)) {
            TitleBar()
            MenuBar()
            Toolbar(
                onSave = onSave,
                onAddJob = addJob,
                onUndo = actions.onUndo, onRedo = actions.onRedo, canUndo = canUndo, canRedo = canRedo,
                steps = hoursFocus?.let { (row, day) ->
                    val current = uiState.hours.getOrNull(row)?.getOrNull(day)?.toDoubleOrNull() ?: 0.0
                    ({ delta: Double -> actions.onHoursChange(row, day, xlStep(current, delta)) })
                },
                onDone = { focusManager.clearFocus() }
            )
            FormulaBar(selected?.let { xlRef(it.sheetRow, it.col) } ?: "A1", if (lookupRow != null) "" else formula, motion)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (sheet == XlSheet.Chart) {
                    Box(Modifier.fillMaxSize().background(Xl.White)) { chartSheet() }
                } else {
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    // The chart fills whatever the grid leaves (at least 300dp); the whole sheet scrolls.
                    val gridHeight = 24.dp + XL_ROW_H * rows.size
                    val chartHeight = (maxHeight - gridHeight).coerceAtLeast(300.dp)
                    Column(Modifier.fillMaxSize().background(Xl.White).verticalScroll(rememberScrollState())) {
                        XlGrid(
                            uiState = uiState, rows = rows, days = days, activeJobs = activeJobs, actions = actions,
                            selected = selected, lookupRow = lookupRow, flashRow = flashRow,
                            onSelect = { selected = it },
                            onOpenLookup = { dataRow, _ -> focusManager.clearFocus(); lookupRow = dataRow },
                            onAddJob = addJob,
                            onHoursFocus = { _, dataRow, day, focused ->
                                hoursFocus = if (focused) dataRow to day else hoursFocus.takeIf { it != dataRow to day }
                            },
                            lookup = { dataRow ->
                                val job = uiState.jobs.getOrElse(dataRow) { "" }
                                XlAutoComplete(
                                    currentJob = job,
                                    activeJobs = activeJobs,
                                    jobsOnCard = buildMap { uiState.jobs.forEachIndexed { i, j -> if (i != dataRow && j.isNotBlank()) put(j.uppercase(), i) } },
                                    onPick = { pick ->
                                        when (pick) {
                                            is JobPick.Entry -> { actions.onJobChange(dataRow, pick.job); lookupRow = null; pendingRow = null }
                                            is JobPick.ExistingRow -> {
                                                lookupRow = null
                                                if (dataRow == pendingRow && job.isBlank()) pendingRow = null
                                                flashRow = pick.rowIndex
                                            }
                                        }
                                    },
                                    onDismiss = closeLookup
                                )
                            }
                        )
                        XlChart(days, weekKey = uiState.activeWeekDate, animate = motion, modifier = Modifier.fillMaxWidth().height(chartHeight))
                    }
                    }
                }
            }

            SheetTabs(sheet, canSwitchWeek, onSelectSheet)
            StatusBar(uiState.saveStatus, lookupRow != null, uiState.getGrandTotal(), motion, onSave)
        }
    }
}

/** Text of a cell, for the formula bar. */
internal fun xlCellText(uiState: TimesheetUiState, rows: List<XlRow>, days: List<com.example.timecard.ui.timesheet.standard.DaySummary>, cell: XlCell): String {
    val day = days.getOrNull(cell.col - 1)
    return when (val row = rows.getOrNull(cell.sheetRow)) {
        XlRow.Header -> when (cell.col) { XL_JOB_COL -> "JOB"; XL_TOTAL_COL -> "TOTAL"; else -> day?.let { "${it.label.uppercase()} ${it.date}".trim() } ?: "" }
        is XlRow.Job -> when (cell.col) {
            XL_JOB_COL -> uiState.jobs.getOrElse(row.dataRow) { "" }
            XL_TOTAL_COL -> ""
            else -> uiState.hours.getOrNull(row.dataRow)?.getOrNull(cell.col - 1) ?: ""
        }
        XlRow.Daily -> if (cell.col == XL_JOB_COL) "DAILY TOTAL" else ""
        XlRow.Fill -> if (cell.col == XL_JOB_COL) "SHOP FILL" else ""
        XlRow.Lunch -> when {
            cell.col == XL_JOB_COL -> "LUNCH"
            day != null -> if (day.index in uiState.noLunchDays) "NO" else "YES"
            else -> ""
        }
        else -> ""
    }
}

@Composable
private fun TitleBar() {
    Row(
        Modifier.fillMaxWidth().height(30.dp).background(Brush.horizontalGradient(listOf(Xl.Navy, Xl.TitleEnd))).padding(start = 5.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ExcelIcon()
        Text("Microsoft Excel - TIMECARD.XLS", style = xlText(15.sp, Xl.White, FontWeight.Bold), modifier = Modifier.weight(1f))
        listOf("_", "□", "✕").forEach {
            Box(Modifier.size(width = 22.dp, height = 20.dp).background(Xl.Face).xlBevel(), contentAlignment = Alignment.Center) { Text(it, style = xlText(12.sp, weight = FontWeight.Bold)) }
        }
    }
}

/** The green "X" sheet icon (drawn, not a brand asset). */
@Composable
private fun ExcelIcon() {
    Canvas(Modifier.size(18.dp)) {
        drawRect(Xl.ExcelGreen)
        val p = size.width * 0.22f
        val s = 2.dp.toPx()
        drawLine(Xl.White, Offset(p, p), Offset(size.width - p, size.height - p), s)
        drawLine(Xl.White, Offset(size.width - p, p), Offset(p, size.height - p), s)
    }
}

@Composable
private fun MenuBar() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("File", "Edit", "View", "Insert", "Format", "Tools", "Data", "Window", "Help").forEach { m ->
            Text(buildAnnotatedString {
                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(m.first()) }
                append(m.drop(1))
            }, style = xlText(15.sp))
        }
    }
}

@Composable
private fun Toolbar(
    onSave: () -> Unit,
    onAddJob: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    steps: ((Double) -> Unit)?,
    onDone: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        ToolButton("Save", onClick = onSave) { RetroPixelIcon(RetroIcon.Floppy, size = 22.dp) }
        ToolButton("Add job", onClick = onAddJob) { Text("+", style = xlText(22.sp, Xl.ExcelGreen, FontWeight.Bold)) }
        ToolButton("Undo", enabled = canUndo, onClick = onUndo) { Text("↶", style = xlText(20.sp, if (canUndo) Xl.Black else Xl.Shadow, FontWeight.Bold)) }
        ToolButton("Redo", enabled = canRedo, onClick = onRedo) { Text("↷", style = xlText(20.sp, if (canRedo) Xl.Black else Xl.Shadow, FontWeight.Bold)) }
        Spacer(Modifier.width(6.dp))
        if (steps != null) {
            listOf(-0.25 to "−¼", 0.25 to "+¼", 0.5 to "+½", 1.0 to "+1", 1.5 to "+1½").forEach { (d, label) ->
                XlButton(label, false, Modifier.weight(1f), height = 32.dp) { steps(d) }
            }
            XlButton("Done", false, Modifier.weight(1f), height = 32.dp, onClick = onDone)
        } else {
            // Decorative font controls, like the real toolbar.
            Field(Modifier.width(100.dp), "Arial")
            Field(Modifier.width(48.dp), "10")
            listOf("B" to FontWeight.Bold, "I" to FontWeight.Bold, "U" to FontWeight.Bold).forEachIndexed { i, (l, w) ->
                Box(Modifier.size(30.dp).background(Xl.Face).xlBevel(), contentAlignment = Alignment.Center) {
                    Text(l, style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontWeight = w, fontSize = 16.sp, fontStyle = if (i == 1) FontStyle.Italic else FontStyle.Normal, textDecoration = if (i == 2) TextDecoration.Underline else null))
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolButton(label: String, enabled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 34.dp).background(Xl.Face).xlBevel()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Field(modifier: Modifier, text: String) {
    Row(modifier.height(28.dp).background(Xl.White).xlBevel(inset = true).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = xlText(14.sp), modifier = Modifier.weight(1f))
        Text("▼", style = xlText(10.sp))
    }
}

@Composable
private fun FormulaBar(name: String, formula: String, motion: Boolean) {
    val caret by rememberInfiniteTransition(label = "caret").animateFloat(
        1f, if (motion) 0f else 1f, infiniteRepeatable(tween(530, easing = LinearEasing), RepeatMode.Reverse), label = "caretA"
    )
    Row(
        Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Field(Modifier.width(76.dp), name)
        Box(Modifier.size(28.dp).background(Xl.Face).xlBevel(), contentAlignment = Alignment.Center) {
            Text("fx", style = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 15.sp))
        }
        Row(Modifier.weight(1f).fillMaxHeight().background(Xl.White).xlBevel(inset = true).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formula, style = xlText(15.sp), maxLines = 1)
            Text("|", style = xlText(15.sp), modifier = Modifier.graphicsLayer { alpha = if (caret > 0.5f) 1f else 0f })
        }
    }
}

/** Sheet tabs with the four scroll arrows and a trapezoid tab per sheet; the front tab is white. */
@Composable
private fun SheetTabs(sheet: XlSheet, canSwitchWeek: Boolean, onSelect: (XlSheet) -> Unit) {
    val tabShape = remember {
        GenericShape { size, _ ->
            val slant = size.height * 0.45f
            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width - slant, size.height); lineTo(slant, size.height); close()
        }
    }
    Row(Modifier.fillMaxWidth().height(40.dp).background(Xl.Face), verticalAlignment = Alignment.Top) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            listOf("◀◀", "◀", "▶", "▶▶").forEach {
                Box(Modifier.size(width = 22.dp, height = 22.dp).background(Xl.Face).xlBevel(), contentAlignment = Alignment.Center) { Text(it, style = xlText(8.sp)) }
            }
        }
        XlSheet.entries.forEachIndexed { i, s ->
            val front = s == sheet
            val enabled = front || s == XlSheet.Chart || canSwitchWeek
            Box(
                Modifier.offset(x = (-12 * i).dp).zIndex(if (front) 2f else 1f - i * 0.1f)
                    .height(if (front) 34.dp else 30.dp).width(132.dp)
                    .clip(tabShape).background(Xl.Black).padding(start = 1.dp, end = 1.dp, bottom = 1.dp)
                    .clip(tabShape).background(if (front) Xl.White else Xl.Face)
                    .clickable(enabled = enabled && !front, role = Role.Tab, onClickLabel = s.label) { onSelect(s) },
                contentAlignment = Alignment.Center
            ) {
                Text(s.label, style = xlText(15.sp, if (enabled) Xl.Black else Xl.Shadow, if (front) FontWeight.Bold else FontWeight.Normal))
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.padding(6.dp).size(width = 150.dp, height = 22.dp).background(Color(0xFFDCDCDC)).xlBevel(inset = true)) {
            Box(Modifier.padding(start = 24.dp).size(width = 40.dp, height = 22.dp).background(Xl.Face).xlBevel())
        }
    }
}

@Composable
private fun StatusBar(saveStatus: SaveStatus, editing: Boolean, total: Double, motion: Boolean, onRetry: () -> Unit) {
    val saving = saveStatus == SaveStatus.SYNCING
    val error = saveStatus == SaveStatus.ERROR
    val fill by rememberInfiniteTransition(label = "saveBlocks").animateFloat(
        0f, if (motion) 1f else 1f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "saveFill"
    )
    Row(Modifier.fillMaxWidth().height(30.dp).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.weight(1f).fillMaxHeight().xlBevel(inset = true).clickable(enabled = error, role = Role.Button, onClickLabel = "Retry save", onClick = onRetry).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(xlStatusText(saving, error, editing), style = xlText(14.sp, if (error) Xl.Error else Xl.Black))
            if (saving) {
                val lit = if (motion) (fill * 12).toInt() else 12
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(12) { Box(Modifier.size(width = 8.dp, height = 14.dp).background(if (it < lit) Xl.Navy else Color.Transparent)) }
                }
            }
        }
        StatusCell(Modifier.width(130.dp), "Sum=${formatHours(total)}")
        StatusCell(Modifier.width(52.dp), "NUM")
    }
}

@Composable
private fun RowScope.StatusCell(modifier: Modifier, text: String) {
    Box(modifier.fillMaxHeight().xlBevel(inset = true).padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) { Text(text, style = xlText(14.sp)) }
}
