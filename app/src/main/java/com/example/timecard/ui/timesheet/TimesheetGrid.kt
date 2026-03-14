package com.example.timecard.ui.timesheet

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.DAY_LABELS
import com.example.timecard.data.model.DAYS
import com.example.timecard.domain.JobValidator
import com.example.timecard.ui.components.AnimatedCounter
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.LocalTimecardColors
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer

// Weight ratios for columns
private const val JOB_WEIGHT = 1.4f
private const val DAY_WEIGHT = 1f
private const val TOTAL_WEIGHT = 1.1f
private val ROW_HEIGHT = 48.dp

// Zebra column colors
private val ZEBRA_EVEN = Color.Transparent
private val ZEBRA_ODD = Color(0x08000000)
private val ROW_ZEBRA = Color(0xFF2196F3).copy(alpha = 0.05f)

@Composable
fun TimesheetGrid(
    uiState: TimesheetUiState,
    onJobChange: (Int, String) -> Unit,
    onHoursChange: (Int, Int, String) -> Unit,
    onFillShopHours: (Int) -> Unit,
    onSnapHours: (Int, Int) -> Unit,
    onAddRow: () -> Unit,
    onDeliveryTag: (Int) -> Unit,
    onJobTag: (Int, String) -> Unit,
    onToggleNoLunch: (Int) -> Unit,
    chartsContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current
    val focusManager = LocalFocusManager.current

    // Track which day cell is focused for quick-add bar
    var focusedRow by remember { mutableIntStateOf(-1) }
    var focusedDay by remember { mutableIntStateOf(-1) }
    
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        // Sticky header row - kept outside LazyColumn for simplicity/stickiness without experimental APIs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.tableHeader)
                .height(ROW_HEIGHT)
        ) {
            HeaderCell("Job", JOB_WEIGHT)
            DAYS.forEachIndexed { _, day ->
                HeaderCell(DAY_LABELS[day] ?: day, DAY_WEIGHT)
            }
            HeaderCell("Total", TOTAL_WEIGHT)
        }

        // LazyColumn for content
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f)
        ) {
            // Data rows (show max of current and previous during animation for smooth expand/collapse)
            val displayRows = if (uiState.isAnimatingWeekSwitch) {
                maxOf(uiState.numRows, uiState.previousNumRows)
            } else {
                uiState.numRows
            }
            items(
                count = displayRows,
                key = { it } // Use index as key since data is list-based
            ) { rowIndex ->
                // For expanding rows: start hidden, then reveal after composition
                // For collapsing rows: start visible, then hide
                val isNewRow = rowIndex >= uiState.previousNumRows && uiState.isAnimatingWeekSwitch
                var rowVisible by remember(uiState.isAnimatingWeekSwitch, rowIndex) {
                    mutableStateOf(
                        if (isNewRow) false // New rows start hidden so they can animate in
                        else rowIndex < uiState.numRows
                    )
                }
                LaunchedEffect(isNewRow) {
                    if (isNewRow) {
                        rowVisible = true // Flip to true to trigger expandVertically
                    }
                }
                AnimatedVisibility(
                    visible = rowVisible,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    TimesheetRowItem(
                        rowIndex = rowIndex,
                        uiState = uiState,
                        onJobChange = onJobChange,
                        onHoursChange = onHoursChange,
                        onSnapHours = onSnapHours,
                        isFocused = focusedRow == rowIndex,
                        activeDay = if (focusedRow == rowIndex) focusedDay else -1,
                        onFocusChange = { row, day ->
                            focusedRow = row
                            focusedDay = day
                        },
                        onFocusClear = {
                            focusedRow = -1
                            focusedDay = -1
                            focusManager.clearFocus()
                        },
                        onDeliveryTag = onDeliveryTag,
                        onJobTag = onJobTag,
                        onToggleNoLunch = onToggleNoLunch
                    )
                }
            }

            // Add Job row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .clickable { onAddRow() }
                        .background(colors.hover.copy(alpha = 0.2f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "  + Add Job",
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Footer: Daily totals + Grand total
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .height(ROW_HEIGHT)
                        .border(0.5.dp, colors.border)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(JOB_WEIGHT)
                            .height(ROW_HEIGHT)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Daily",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }

                    DAYS.forEachIndexed { dayIndex, _ ->
                        val dayTotal = uiState.getDayTotal(dayIndex)
                        val target = when (dayIndex) {
                            in 0..3 -> 9.0
                            4 -> 4.0
                            else -> 0.0
                        }
                        val isGood = if (target > 0) dayTotal >= target else true
                        val canFill = dayIndex < 5 && dayTotal < target
                        val zebraColor = if (dayIndex % 2 == 0) ZEBRA_EVEN else ZEBRA_ODD
                        Box(
                            modifier = Modifier
                                .weight(DAY_WEIGHT)
                                .height(ROW_HEIGHT)
                                .border(0.5.dp, colors.border)
                                .background(zebraColor)
                                .then(
                                    if (canFill) Modifier.clickable { onFillShopHours(dayIndex) }
                                    else Modifier
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedCounter(
                                targetValue = dayTotal,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayTotal > 0) {
                                    if (isGood) colors.textGreen else colors.textOrange
                                } else colors.textSecondary
                            )
                            if (canFill) {
                                Text(
                                    "+",
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textGreen
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(TOTAL_WEIGHT)
                            .height(ROW_HEIGHT)
                            .border(0.5.dp, colors.border)
                            .background(colors.accent.copy(alpha = 0.1f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedCounter(
                            targetValue = uiState.getGrandTotal(),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textTotal
                        )
                    }
                }
            }

            // Charts section
            if (chartsContent != null) {
                item {
                    chartsContent()
                }
            }
        }
    }
    // (End of LazyColumn, function closes here)
}

@Composable
private fun TimesheetRowItem(
    rowIndex: Int,
    uiState: TimesheetUiState,
    onJobChange: (Int, String) -> Unit,
    onHoursChange: (Int, Int, String) -> Unit,
    onSnapHours: (Int, Int) -> Unit,
    isFocused: Boolean,
    activeDay: Int,
    onFocusChange: (Int, Int) -> Unit,
    onFocusClear: () -> Unit,
    onDeliveryTag: (Int) -> Unit,
    onJobTag: (Int, String) -> Unit,
    onToggleNoLunch: (Int) -> Unit
) {
    val colors = LocalTimecardColors.current
    val focusManager = LocalFocusManager.current
    val job = uiState.jobs.getOrElse(rowIndex) { "" }
    val isDelivery = JobValidator.isDeliveryJob(job)
    val isInvalidJob = job.isNotBlank() && !JobValidator.isValidJobEntry(job)
    val isShopRow = job.uppercase() == "SHOP"

    val targetColor = if (isFocused) {
        colors.accent.copy(alpha = 0.1f)
    } else {
        if (rowIndex % 2 != 0) ROW_ZEBRA else Color.Transparent
    }
    val rowColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "rowColor"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowColor)
                .height(IntrinsicSize.Min)
        ) {
            // Job cell
            Box(
                modifier = Modifier
                    .weight(JOB_WEIGHT)
                    .height(ROW_HEIGHT)
                    .border(0.5.dp, colors.border)
                    .background(
                        when {
                            isInvalidJob -> Color(0x33E53935)
                            isDelivery -> Color(0x33DD6B20)
                            else -> Color.Transparent
                        }
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = job,
                    onValueChange = { onJobChange(rowIndex, it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                // Highlight row; activeDay=-1 suppresses quick-add bar
                                onFocusChange(rowIndex, -1)
                            }
                        }
                )
                // Delivery label — top-left corner badge
                if (isDelivery) {
                    Text(
                        text = "Delivery",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDD6B20),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 2.dp, top = 1.dp)
                    )
                }
            }

            // Day cells with zebra columns
            DAYS.forEachIndexed { dayIndex, _ ->
                val cellValue = uiState.hours.getOrNull(rowIndex)
                    ?.getOrElse(dayIndex) { "" } ?: ""
                val zebraColor = if (dayIndex % 2 == 0) ZEBRA_EVEN else ZEBRA_ODD

                Box(
                    modifier = Modifier
                        .weight(DAY_WEIGHT)
                        .height(ROW_HEIGHT)
                        .border(0.5.dp, colors.border)
                        .background(zebraColor)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = cellValue,
                        onValueChange = {
                            onHoursChange(rowIndex, dayIndex, it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                focusManager.moveFocus(FocusDirection.Next)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val isFilling = uiState.fillingCell?.let {
                                    it.first == rowIndex && it.second == dayIndex
                                } == true
                                alpha = if (uiState.isAnimatingWeekSwitch || isFilling) 0f else 1f
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onFocusChange(rowIndex, dayIndex)
                                } else {
                                    onSnapHours(rowIndex, dayIndex)
                                }
                            }
                    )
                    // Rolling number overlay during week switch
                    if (uiState.isAnimatingWeekSwitch) {
                        val prevValue = uiState.getPreviousHourValue(rowIndex, dayIndex)
                        val newValue = cellValue.toDoubleOrNull() ?: 0.0
                        if (prevValue > 0 || newValue > 0) {
                            AnimatedCounter(
                                targetValue = newValue,
                                initialValue = prevValue,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                showZero = false
                            )
                        }
                    }
                    // Rolling number overlay during SHOP fill
                    val filling = uiState.fillingCell
                    if (filling != null && filling.first == rowIndex && filling.second == dayIndex) {
                        AnimatedCounter(
                            targetValue = cellValue.toDoubleOrNull() ?: 0.0,
                            initialValue = uiState.fillingCellPrevValue,
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            showZero = false
                        )
                    }
                    // Lunch toggle — top-right corner of SHOP day cells
                    if (isShopRow) {
                        val lunchTaken = dayIndex !in uiState.noLunchDays
                        Text(
                            text = if (lunchTaken) "L" else "L̶",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lunchTaken) colors.accent.copy(alpha = 0.7f)
                                    else colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 2.dp, top = 1.dp)
                                .clickable { onToggleNoLunch(dayIndex) }
                        )
                    }
                }
            }

            // Total cell
            Box(
                modifier = Modifier
                    .weight(TOTAL_WEIGHT)
                    .height(ROW_HEIGHT)
                    .border(0.5.dp, colors.border)
                    .background(colors.hover.copy(alpha = 0.3f))
                    .padding(4.dp),
                    contentAlignment = Alignment.Center
            ) {
                val rowTotal = uiState.getRowTotal(rowIndex)
                AnimatedCounter(
                    targetValue = rowTotal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textTotal,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Job tag bar — appears when the job cell is focused (activeDay == -1)
        AnimatedVisibility(
            visible = isFocused && activeDay == -1,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.06f))
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                JobTagChip(
                    label = "🚚 Delivery",
                    active = isDelivery,
                    activeColor = Color(0xFFDD6B20),
                    colors = colors
                ) { onDeliveryTag(rowIndex) }
                JobTagChip(
                    label = "🏖 PTO",
                    active = job.uppercase() == "PTO",
                    activeColor = colors.accent,
                    colors = colors
                ) { onJobTag(rowIndex, "PTO") }
                JobTagChip(
                    label = "🤒 Sick",
                    active = job.uppercase() == "SICK",
                    activeColor = Color(0xFFE53935),
                    colors = colors
                ) { onJobTag(rowIndex, "SICK") }
                JobTagChip(
                    label = "🎉 Holiday",
                    active = job.uppercase() == "HOLIDAY",
                    activeColor = Color(0xFF7B1FA2),
                    colors = colors
                ) { onJobTag(rowIndex, "HOLIDAY") }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                // Close button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.hover)
                        .clickable { /* just dismiss by tapping elsewhere */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }

        // Quick-add bar below the focused row — animated expand/collapse
        AnimatedVisibility(
            visible = isFocused && activeDay >= 0,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val currentVal = uiState.hours.getOrNull(rowIndex)
                ?.getOrElse(activeDay.coerceAtLeast(0)) { "" }?.toDoubleOrNull() ?: 0.0
            val dayLabel = DAY_LABELS[DAYS.getOrElse(activeDay.coerceAtLeast(0)) { "" }] ?: ""

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.accent.copy(alpha = 0.08f))
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$dayLabel: ${String.format("%.2f", currentVal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textHeading,
                    modifier = Modifier.weight(1f)
                )
                QuickAddChip("-0.25", colors.textOrange) {
                    val newVal = maxOf(0.0, currentVal - 0.25)
                    val rounded = Math.round(newVal * 100.0) / 100.0
                    onHoursChange(rowIndex, activeDay, if (rounded > 0) rounded.toString() else "")
                }
                QuickAddChip("+0.25", colors.accent) {
                    val newVal = Math.round((currentVal + 0.25) * 100.0) / 100.0
                    onHoursChange(rowIndex, activeDay, newVal.toString())
                }
                QuickAddChip("+0.5", colors.accent) {
                    val newVal = Math.round((currentVal + 0.5) * 100.0) / 100.0
                    onHoursChange(rowIndex, activeDay, newVal.toString())
                }
                QuickAddChip("+1", colors.accent) {
                    val newVal = Math.round((currentVal + 1.0) * 100.0) / 100.0
                    onHoursChange(rowIndex, activeDay, newVal.toString())
                }
                QuickAddChip("+1.5", colors.accent) {
                    val newVal = Math.round((currentVal + 1.5) * 100.0) / 100.0
                    onHoursChange(rowIndex, activeDay, newVal.toString())
                }
                // Close button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.hover)
                        .clickable { onFocusClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("\u2715", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(ROW_HEIGHT)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JobTagChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    colors: com.example.timecard.ui.theme.TimecardColors,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) activeColor.copy(alpha = 0.18f) else colors.input
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) activeColor else colors.border
        ),
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = label,
            color = if (active) activeColor else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily
        )
    }
}

@Composable
private fun QuickAddChip(label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalTimecardColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.input),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.height(40.dp)
    ) {
        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily)
    }
}
