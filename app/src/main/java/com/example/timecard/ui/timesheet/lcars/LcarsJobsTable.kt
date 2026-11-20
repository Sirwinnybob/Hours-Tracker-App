package com.example.timecard.ui.timesheet.lcars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.timecard.ui.timesheet.standard.JobPick
import com.example.timecard.ui.timesheet.standard.firstEmptyRow
import com.example.timecard.ui.timesheet.standard.formatHours
import com.example.timecard.ui.timesheet.standard.visibleRowIndices
import java.util.Locale

private const val JOB_W = 176
private const val TOTAL_W = 66

/** Job pill color: SHOP is always Bluey, other rows cycle the Classic row colors by position. */
fun lcarsRowColor(job: String, visibleIndex: Int): Color =
    if (job.trim().equals("SHOP", ignoreCase = true)) LcarsClassic.Bluey
    else LcarsClassic.RowColors[visibleIndex.mod(LcarsClassic.RowColors.size)]

/**
 * JOBS: job pills, hour cells, Daily total / Shop fill / Lunch rows, Add job / Undo / Redo, and the
 * LCARS job lookup. Same data rules as the standard grid (only rows with a job or hours show).
 */
@Composable
internal fun LcarsJobsTable(
    uiState: TimesheetUiState,
    days: List<DaySummary>,
    activeJobs: List<ActiveJob>,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: GridActions,
    animate: Boolean
) {
    var pendingRow by remember { mutableStateOf<Int?>(null) }
    var lookupRow by remember { mutableStateOf<Int?>(null) }
    var flashRow by remember { mutableStateOf<Int?>(null) }
    var focusedRow by remember { mutableIntStateOf(-1) }
    var focusedDay by remember { mutableIntStateOf(-1) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(flashRow) { if (flashRow != null) { kotlinx.coroutines.delay(1400); flashRow = null } }

    val rows = visibleRowIndices(uiState.jobs, uiState.hours, uiState.numRows, pendingRow)
    val closeLookup: () -> Unit = {
        val r = lookupRow
        lookupRow = null
        if (r != null && r == pendingRow && uiState.jobs.getOrElse(r) { "" }.isBlank()) pendingRow = null
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Header
        Row(Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(JOB_W.dp))
            days.forEach { d ->
                Box(
                    Modifier.weight(1f).height(26.dp)
                        .then(if (d.isToday) Modifier.clip(RoundedCornerShape(13.dp)).background(LcarsClassic.Bluey) else Modifier),
                    contentAlignment = Alignment.Center
                ) { Text(d.label.uppercase(), style = lcarsText(15.sp, if (d.isToday) Color.Black else LcarsClassic.Bluey)) }
            }
            Text("TOTAL", style = lcarsText(15.sp, LcarsClassic.Bluey), textAlign = TextAlign.End, modifier = Modifier.width(TOTAL_W.dp))
        }

        rows.forEachIndexed { vi, row ->
            val job = uiState.jobs.getOrElse(row) { "" }
            val rowIn = rememberEntrance(animate, 650 + vi * 70, durationMs = 260)
            Column(Modifier.graphicsLayer { alpha = rowIn; translationX = (1f - rowIn) * -40f }) {
                Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    JobPill(
                        job = job,
                        name = ActiveJobs.find(job, activeJobs)?.name,
                        color = lcarsRowColor(job, vi),
                        flashing = flashRow == row,
                        lookupOpen = lookupRow == row,
                        onOpen = { focusManager.clearFocus(); lookupRow = row },
                        lookup = { anchorPx ->
                            LcarsJobLookup(
                                currentJob = job,
                                activeJobs = activeJobs,
                                jobsOnCard = buildMap { uiState.jobs.forEachIndexed { i, j -> if (i != row && j.isNotBlank()) put(j.uppercase(), i) } },
                                anchorHeightPx = anchorPx,
                                onPick = { pick ->
                                    when (pick) {
                                        is JobPick.Entry -> { actions.onJobChange(row, pick.job); lookupRow = null; pendingRow = null }
                                        is JobPick.ExistingRow -> {
                                            lookupRow = null
                                            if (row == pendingRow && job.isBlank()) pendingRow = null
                                            flashRow = pick.rowIndex
                                        }
                                    }
                                },
                                onDismiss = closeLookup
                            )
                        }
                    )
                    days.forEachIndexed { d, summary ->
                        HourCell(
                            value = uiState.hours.getOrNull(row)?.getOrNull(d) ?: "",
                            today = summary.isToday,
                            description = "${summary.label} hours, row ${row + 1}",
                            onChange = { actions.onHoursChange(row, d, it) },
                            onFocus = { focused ->
                                if (focused) { focusedRow = row; focusedDay = d } else {
                                    actions.onSnapHours(row, d)
                                    if (focusedRow == row && focusedDay == d) { focusedRow = -1; focusedDay = -1 }
                                }
                            }
                        )
                    }
                    val total = uiState.getRowTotal(row)
                    Text(formatHours(total), style = lcarsText(24.sp, if (total > 0) LcarsClassic.Orange else LcarsClassic.Muted, FontWeight.Normal), textAlign = TextAlign.End, modifier = Modifier.width(TOTAL_W.dp))
                }
                AnimatedVisibility(visible = focusedRow == row && focusedDay >= 0, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    val d = focusedDay.coerceAtLeast(0)
                    val current = uiState.hours.getOrNull(row)?.getOrNull(d)?.toDoubleOrNull() ?: 0.0
                    QuickSteps(days.getOrNull(d)?.label ?: "", current, onSet = { actions.onHoursChange(row, d, it) }, onDone = { focusManager.clearFocus() })
                }
            }
        }

        // Daily total
        LabeledRow("DAILY TOTAL") {
            days.forEach { d ->
                Text(
                    if (d.state == DayState.NoTarget && d.total == 0.0) "—" else formatHours(d.total),
                    style = lcarsText(22.sp, lcarsDayValueColor(d), FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Shop fill
        LabeledRow("SHOP FILL") {
            days.forEach { d ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (d.canFillShop) {
                        LcarsPill("+${formatHours(d.shortBy)}", LcarsClassic.Sienna, { actions.onFillShop(d.index) }, Modifier.fillMaxWidth(), height = 32.dp, fontSize = 14.sp)
                    }
                }
            }
        }
        // Lunch
        LabeledRow("LUNCH") {
            days.forEach { d ->
                val taken = d.index !in uiState.noLunchDays
                Box(
                    Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(50))
                        .background(if (taken) LcarsClassic.Violet else Color(0xFF241F33))
                        .clickable(role = Role.Switch, onClickLabel = "Toggle lunch for ${d.label}") { actions.onToggleNoLunch(d.index) },
                    contentAlignment = Alignment.Center
                ) { Text(if (taken) "YES" else "NO", style = lcarsText(14.sp, if (taken) Color.Black else LcarsClassic.DimText)) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LcarsPill("ADD JOB", LcarsClassic.Orange, {
                focusManager.clearFocus()
                val target = firstEmptyRow(uiState.jobs, uiState.hours, uiState.numRows) ?: actions.onAddRow()
                pendingRow = target
                lookupRow = target
            }, Modifier.width(JOB_W.dp))
            Spacer(Modifier.weight(1f))
            LcarsPill("UNDO", LcarsClassic.Bluey, actions.onUndo, Modifier.width(120.dp), enabled = canUndo)
            LcarsPill("REDO", LcarsClassic.Bluey, actions.onRedo, Modifier.width(120.dp), enabled = canRedo)
        }
    }
}

@Composable
private fun LabeledRow(label: String, cells: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = lcarsText(16.sp, LcarsClassic.Barley), modifier = Modifier.width(JOB_W.dp))
        cells()
        Spacer(Modifier.width(TOTAL_W.dp))
    }
}

@Composable
private fun JobPill(
    job: String,
    name: String?,
    color: Color,
    flashing: Boolean,
    lookupOpen: Boolean,
    onOpen: () -> Unit,
    lookup: @Composable (Int) -> Unit
) {
    var heightPx by remember { mutableIntStateOf(0) }
    val delivery = JobValidator.isDeliveryJob(job)
    val invalid = job.isNotBlank() && !JobValidator.isValidJobEntry(job)
    val fill = when {
        lookupOpen || flashing -> LcarsClassic.Creme
        job.isBlank() -> LcarsClassic.Dim
        invalid -> LcarsClassic.Red
        else -> color
    }
    Box(Modifier.width(JOB_W.dp).fillMaxHeight()) {
        Box(
            Modifier.fillMaxWidth().fillMaxHeight()
                .onSizeChanged { heightPx = it.height }
                .clip(RoundedCornerShape(topStart = 27.dp, bottomStart = 27.dp))
                .background(fill)
                .clickable(role = Role.Button, onClickLabel = "Pick job", onClick = onOpen)
        ) {
            if (job.isBlank()) {
                Text("PICK A JOB", style = lcarsText(16.sp, LcarsClassic.DimText), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 6.dp))
            } else {
                Text(
                    if (delivery) job.dropLast(1) else job,
                    style = lcarsText(22.sp, Color.Black, FontWeight.Bold),
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 5.dp)
                )
                val sub = listOfNotNull(name, if (delivery) "DELIVERY" else null).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(
                        sub.uppercase(), style = lcarsText(11.sp, Color.Black), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 6.dp, start = 30.dp)
                    )
                }
            }
        }
        if (lookupOpen) lookup(heightPx)
    }
}

@Composable
private fun RowScope.HourCell(value: String, today: Boolean, description: String, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.weight(1f).fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) Color(0xFF2A2140) else if (today) LcarsClassic.TodayWash else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty() && !focused) Text("—", style = lcarsText(24.sp, LcarsClassic.Dim, FontWeight.Normal))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = lcarsText(24.sp, LcarsClassic.Creme, FontWeight.Normal).copy(textAlign = TextAlign.Center),
            cursorBrush = SolidColor(LcarsClassic.Orange),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            // The whole cell is the tap target, not just the one text line in its middle.
            decorationBox = { inner -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() } },
            modifier = Modifier.fillMaxSize().semantics { contentDescription = description }
                .onFocusChanged { if (it.isFocused != focused) { focused = it.isFocused; onFocus(it.isFocused) } }
        )
    }
}

/** −0.25 / +0.25 / +0.5 / +1 / +1.5 for the focused cell (same steps as the other layouts). */
@Composable
private fun QuickSteps(dayLabel: String, current: Double, onSet: (String) -> Unit, onDone: () -> Unit) {
    fun fmt(v: Double): String { val r = Math.round(v.coerceAtLeast(0.0) * 4.0) / 4.0; return if (r > 0) String.format(Locale.US, "%.2f", r) else "" }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${dayLabel.uppercase()} ${String.format(Locale.US, "%.2f", current)}", style = lcarsText(20.sp, LcarsClassic.Orange, FontWeight.Normal), modifier = Modifier.width(JOB_W.dp))
        LcarsPill("−0.25", LcarsClassic.Red, { onSet(fmt(current - 0.25)) }, Modifier.weight(1f), height = 44.dp)
        listOf(0.25, 0.5, 1.0, 1.5).forEach { step ->
            val label = "+" + if (step % 1.0 == 0.0) step.toInt().toString() else step.toString()
            LcarsPill(label, LcarsClassic.Butter, { onSet(fmt(current + step)) }, Modifier.weight(1f), height = 44.dp)
        }
        LcarsPill("DONE", LcarsClassic.Bluey, onDone, Modifier.weight(1f), height = 44.dp)
    }
}
