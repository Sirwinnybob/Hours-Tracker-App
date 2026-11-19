package com.example.timecard.ui.timesheet.standard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import java.util.Locale

private const val JOB_W = 3.2f
private const val DAY_W = 1f
private const val TOTAL_W = 1.1f
private val ERROR_RED = Color(0xFFC62828)

/** Everything the grid can change, all routed to TimesheetViewModel. */
internal class GridActions(
    val onJobChange: (row: Int, job: String) -> Unit,
    val onHoursChange: (row: Int, day: Int, value: String) -> Unit,
    val onSnapHours: (row: Int, day: Int) -> Unit,
    val onFillShop: (day: Int) -> Unit,
    val onToggleNoLunch: (day: Int) -> Unit,
    /** Appends a row and returns its index. */
    val onAddRow: () -> Int,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit
)

/**
 * The timesheet grid card: Job · Mon–Sat · Total, B-style rounded hour cells, Add job, Daily total
 * (with "+N Shop" fill) and Lunch rows. Only rows holding a job or hours are shown.
 */
@Composable
internal fun TimesheetGridCard(
    look: StandardLook,
    uiState: TimesheetUiState,
    days: List<DaySummary>,
    activeJobs: List<ActiveJob>,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: GridActions
) {
    var pendingRow by remember { mutableStateOf<Int?>(null) }
    var pickerRow by remember { mutableStateOf<Int?>(null) }
    var flashRow by remember { mutableStateOf<Int?>(null) }
    var focusedRow by remember { mutableIntStateOf(-1) }
    var focusedDay by remember { mutableIntStateOf(-1) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(flashRow) {
        if (flashRow != null) {
            kotlinx.coroutines.delay(1600)
            flashRow = null
        }
    }

    val rows = visibleRowIndices(uiState.jobs, uiState.hours, uiState.numRows, pendingRow)
    val closePicker: () -> Unit = {
        val row = pickerRow
        pickerRow = null
        if (row != null && row == pendingRow && uiState.jobs.getOrElse(row) { "" }.isBlank()) pendingRow = null
    }

    SectionCard(look) {
        SectionTitle(look, "Timesheet", "tap a job to change it, an hour to edit") {
            IconAction(HoursIcons.Undo, "Undo", look, actions.onUndo, tonal = true, enabled = canUndo)
            IconAction(HoursIcons.Redo, "Redo", look, actions.onRedo, tonal = true, enabled = canRedo)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, look.line, RoundedCornerShape(12.dp))
        ) {
            HeaderRow(look, days)
            rows.forEach { row ->
                val job = uiState.jobs.getOrElse(row) { "" }
                val jobsOnCard = buildMap {
                    uiState.jobs.forEachIndexed { i, j -> if (i != row && j.isNotBlank()) put(j.uppercase(), i) }
                }
                GridRow(
                    look = look,
                    rowIndex = row,
                    job = job,
                    jobName = ActiveJobs.find(job, activeJobs)?.name,
                    hours = uiState.hours.getOrNull(row).orEmpty(),
                    rowTotal = uiState.getRowTotal(row),
                    days = days,
                    highlighted = flashRow == row || focusedRow == row,
                    pickerOpen = pickerRow == row,
                    onOpenPicker = { focusManager.clearFocus(); pickerRow = row },
                    picker = { anchorPx ->
                        JobPickerPopup(
                            look = look,
                            currentJob = job,
                            activeJobs = activeJobs,
                            jobsOnCard = jobsOnCard,
                            anchorHeightPx = anchorPx,
                            onPick = { pick ->
                                when (pick) {
                                    is JobPick.Entry -> {
                                        actions.onJobChange(row, pick.job)
                                        pickerRow = null
                                        // Set: the job keeps the row visible. Cleared: hours (if any) keep it visible.
                                        pendingRow = null
                                    }
                                    is JobPick.ExistingRow -> {
                                        pickerRow = null
                                        if (row == pendingRow && job.isBlank()) pendingRow = null
                                        flashRow = pick.rowIndex
                                    }
                                }
                            },
                            onDismiss = closePicker
                        )
                    },
                    onHoursChange = { day, v -> actions.onHoursChange(row, day, v) },
                    onCellFocus = { day, focused ->
                        if (focused) {
                            focusedRow = row; focusedDay = day
                        } else {
                            actions.onSnapHours(row, day)
                            if (focusedRow == row && focusedDay == day) { focusedRow = -1; focusedDay = -1 }
                        }
                    }
                )
                AnimatedVisibility(visible = focusedRow == row && focusedDay >= 0, enter = expandVertically(), exit = shrinkVertically()) {
                    val day = focusedDay.coerceAtLeast(0)
                    val current = uiState.hours.getOrNull(row)?.getOrNull(day)?.toDoubleOrNull() ?: 0.0
                    QuickAddBar(
                        look = look,
                        dayLabel = days.getOrNull(day)?.label ?: "",
                        current = current,
                        onSet = { v -> actions.onHoursChange(row, day, v) },
                        onClose = { focusManager.clearFocus() }
                    )
                }
            }
            AddJobRow(look) {
                focusManager.clearFocus()
                val target = firstEmptyRow(uiState.jobs, uiState.hours, uiState.numRows) ?: actions.onAddRow()
                pendingRow = target
                pickerRow = target
            }
            DailyTotalRow(look, days, actions.onFillShop)
            LunchRow(look, days, uiState.noLunchDays, actions.onToggleNoLunch)
        }
    }
}

@Composable
private fun HeaderRow(look: StandardLook, days: List<DaySummary>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(look.head)
    ) {
        HeadCell("Job", JOB_W, look, Alignment.CenterStart)
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(DAY_W)
                    .fillMaxHeight()
                    .background(if (day.isToday) look.todayHead else Color.Transparent),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (day.isToday) Box(Modifier.fillMaxWidth().height(3.dp).background(look.primary))
                Text(day.label, color = look.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (day.date.isNotEmpty()) Text(day.date, color = look.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        HeadCell("Total", TOTAL_W, look, Alignment.Center)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
}

@Composable
private fun RowScope.HeadCell(text: String, weight: Float, look: StandardLook, align: Alignment) {
    Box(Modifier.weight(weight).fillMaxHeight().padding(horizontal = 12.dp), contentAlignment = align) {
        Text(text, color = look.text2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GridRow(
    look: StandardLook,
    rowIndex: Int,
    job: String,
    jobName: String?,
    hours: List<String>,
    rowTotal: Double,
    days: List<DaySummary>,
    highlighted: Boolean,
    pickerOpen: Boolean,
    onOpenPicker: () -> Unit,
    picker: @Composable (anchorPx: Int) -> Unit,
    onHoursChange: (day: Int, value: String) -> Unit,
    onCellFocus: (day: Int, focused: Boolean) -> Unit
) {
    val rowBg by animateColorAsState(if (highlighted) look.today else Color.Transparent, label = "rowHighlight")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
            .background(rowBg)
    ) {
        JobCell(look, job, jobName, pickerOpen, onOpenPicker, picker, Modifier.weight(JOB_W))
        days.forEachIndexed { day, summary ->
            HourCell(
                look = look,
                value = hours.getOrElse(day) { "" },
                isToday = summary.isToday,
                description = "${summary.label} hours, row ${rowIndex + 1}",
                onValueChange = { onHoursChange(day, it) },
                onFocus = { onCellFocus(day, it) },
                modifier = Modifier.weight(DAY_W)
            )
        }
        Box(
            modifier = Modifier.weight(TOTAL_W).fillMaxHeight().background(look.head),
            contentAlignment = Alignment.Center
        ) {
            Text(formatHours(rowTotal), style = monoStyle(17, if (rowTotal > 0) look.softText else look.empty))
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
}

@Composable
private fun JobCell(
    look: StandardLook,
    job: String,
    jobName: String?,
    pickerOpen: Boolean,
    onOpen: () -> Unit,
    picker: @Composable (anchorPx: Int) -> Unit,
    modifier: Modifier
) {
    var heightPx by remember { mutableIntStateOf(0) }
    val invalid = job.isNotBlank() && !JobValidator.isValidJobEntry(job)
    val delivery = JobValidator.isDeliveryJob(job)
    val shape = RoundedCornerShape(10.dp)
    Box(modifier = modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .onSizeChanged { heightPx = it.height }
                .clip(shape)
                .background(if (pickerOpen) look.today else Color.Transparent)
                .border(
                    width = if (pickerOpen) 2.dp else 1.dp,
                    color = when {
                        pickerOpen -> look.primary
                        invalid -> ERROR_RED
                        else -> look.line
                    },
                    shape = shape
                )
                .clickable(role = Role.Button, onClickLabel = "Pick job", onClick = onOpen)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
        ) {
            if (job.isBlank()) {
                Text("Pick a job", color = look.empty, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (delivery) job.dropLast(1) else job,
                        color = if (invalid) ERROR_RED else look.text,
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1
                    )
                    if (jobName != null) {
                        Text(jobName, color = look.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (delivery) Chip("Delivery", Color(0xFFCCDDF1), Color(0xFF154B8A), icon = HoursIcons.Delivery)
            }
        }
        if (pickerOpen) picker(heightPx)
    }
}

@Composable
private fun HourCell(
    look: StandardLook,
    value: String,
    isToday: Boolean,
    description: String,
    onValueChange: (String) -> Unit,
    onFocus: (Boolean) -> Unit,
    modifier: Modifier
) {
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val shape = CellShape
    Box(modifier = modifier.fillMaxHeight().padding(horizontal = 3.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(if (isToday) look.today else look.head)
                .border(
                    width = if (focused || isToday) 2.dp else 1.dp,
                    color = if (focused || isToday) look.primary else look.line,
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (value.isEmpty() && !focused) Text("–", color = look.empty, fontSize = 15.sp, fontFamily = HoursMono)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = monoStyle(15, look.text).copy(textAlign = TextAlign.Center),
                cursorBrush = SolidColor(look.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .semantics { contentDescription = description }
                    .onFocusChanged {
                        if (it.isFocused != focused) {
                            focused = it.isFocused
                            onFocus(it.isFocused)
                        }
                    }
            )
        }
    }
}

/** −0.25 / +0.25 / +0.5 / +1 / +1.5 for the focused cell (same steps as before). */
@Composable
private fun QuickAddBar(look: StandardLook, dayLabel: String, current: Double, onSet: (String) -> Unit, onClose: () -> Unit) {
    fun fmt(v: Double): String {
        val r = Math.round(v.coerceAtLeast(0.0) * 4.0) / 4.0
        return if (r > 0) String.format(Locale.US, "%.2f", r) else ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(look.today)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("$dayLabel  ${String.format(Locale.US, "%.2f", current)}", style = monoStyle(16, look.text), modifier = Modifier.weight(1f))
        PillButton("−0.25", look, { onSet(fmt(current - 0.25)) }, kind = PillKind.Outlined, outlineColor = look.warn, height = 44.dp)
        listOf(0.25, 0.5, 1.0, 1.5).forEach { step ->
            val label = "+" + if (step % 1.0 == 0.0) step.toInt().toString() else step.toString()
            PillButton(label, look, { onSet(fmt(current + step)) }, kind = PillKind.Tonal, height = 44.dp)
        }
        IconAction(HoursIcons.Close, "Done editing", look, onClose, tonal = true)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
}

@Composable
private fun AddJobRow(look: StandardLook, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        PillButton("Add job", look, onClick, kind = PillKind.Dashed, icon = HoursIcons.Plus, height = 44.dp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
}

@Composable
private fun DailyTotalRow(look: StandardLook, days: List<DaySummary>, onFill: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 76.dp).background(look.head)
    ) {
        Column(Modifier.weight(JOB_W).fillMaxHeight().padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
            Text("Daily total", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("9 h Mon–Thu · 4 h Fri", color = look.text2, fontSize = 12.sp)
        }
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(DAY_W)
                    .fillMaxHeight()
                    .background(if (day.isToday) look.today else Color.Transparent)
                    .padding(horizontal = 3.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
            ) {
                Text(
                    if (day.state == DayState.NoTarget && day.total == 0.0) "–" else formatHours(day.total),
                    style = monoStyle(16, dayTotalColor(look, day))
                )
                if (day.canFillShop) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(PillShape)
                            .background(look.soft)
                            .clickable(role = Role.Button, onClickLabel = "Fill ${day.label} with SHOP") { onFill(day.index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${formatHours(day.shortBy)}", color = look.softText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        Box(Modifier.weight(TOTAL_W))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
}

@Composable
private fun LunchRow(look: StandardLook, days: List<DaySummary>, noLunchDays: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp)) {
        Row(
            Modifier.weight(JOB_W).fillMaxHeight().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(HoursIcons.Lunch, null, tint = look.text2, modifier = Modifier.size(20.dp))
            Text("Lunch", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        days.forEach { day ->
            val taken = day.index !in noLunchDays
            Box(
                modifier = Modifier
                    .weight(DAY_W)
                    .fillMaxHeight()
                    .background(if (day.isToday) look.today else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .clip(PillShape)
                        .background(if (taken) look.doneSoft else Color.Transparent)
                        .border(if (taken) 0.dp else 1.5.dp, if (taken) Color.Transparent else look.line, PillShape)
                        .clickable(role = Role.Switch, onClickLabel = "Toggle lunch for ${day.label}") { onToggle(day.index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (taken) "Yes" else "No", color = if (taken) look.doneText else look.text2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(Modifier.weight(TOTAL_W))
    }
}
