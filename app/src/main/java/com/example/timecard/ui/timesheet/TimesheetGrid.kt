package com.example.timecard.ui.timesheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.DAY_LABELS
import com.example.timecard.data.model.DAYS
import com.example.timecard.domain.JobValidator
import com.example.timecard.ui.components.AnimatedCounter
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.LcarsAnakiwa
import com.example.timecard.ui.theme.LcarsBlueBell
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsPurple
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
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
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    chartsContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current
    val focusManager = LocalFocusManager.current

    // Track which day cell is focused for quick-add bar
    var focusedRow by remember { mutableIntStateOf(-1) }
    var focusedDay by remember { mutableIntStateOf(-1) }
    
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        // Sticky header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (colors.isLcars) LcarsOrange else colors.tableHeader)
                .height(ROW_HEIGHT)
        ) {
            HeaderCell("Job", JOB_WEIGHT)
            DAYS.forEachIndexed { dayIndex, day ->
                if (colors.isLcars && dayIndex > 0) Spacer(Modifier.width(2.dp).height(ROW_HEIGHT).background(Color.Black))
                Box(
                    modifier = Modifier
                        .weight(DAY_WEIGHT)
                        .height(ROW_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (colors.isLcars) (DAY_LABELS[day] ?: day).uppercase() else (DAY_LABELS[day] ?: day),
                        color = if (colors.isLcars) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        letterSpacing = if (colors.isLcars) 0.5.sp else 0.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (colors.isLcars) Spacer(Modifier.width(2.dp).height(ROW_HEIGHT).background(Color.Black))
            HeaderCell("Total", TOTAL_WEIGHT)
        }
        if (colors.isLcars) Spacer(Modifier.fillMaxWidth().height(4.dp).background(Color.Black))

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
                        .background(if (colors.isLcars) Color.Black else colors.hover.copy(alpha = 0.2f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (colors.isLcars) "  + ADD JOB" else "  + Add Job",
                        color = if (colors.isLcars) LcarsTan else colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        letterSpacing = if (colors.isLcars) 1.sp else 0.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { onAddRow() }
                            .padding(end = 16.dp, top = 8.dp, bottom = 8.dp) // extra padding for click area
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "UNDO",
                            color = if (canUndo) (if (colors.isLcars) LcarsOrange else colors.accent) else colors.textSecondary.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            modifier = Modifier
                                .clickable(enabled = canUndo) { onUndo() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "REDO",
                            color = if (canRedo) (if (colors.isLcars) LcarsOrange else colors.accent) else colors.textSecondary.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            modifier = Modifier
                                .clickable(enabled = canRedo) { onRedo() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Footer: Daily totals + Grand total
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (colors.isLcars) Color.Black else colors.surface)
                        .height(ROW_HEIGHT)
                        .then(if (!colors.isLcars) Modifier.border(0.5.dp, colors.border) else Modifier)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(JOB_WEIGHT)
                            .height(ROW_HEIGHT)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (colors.isLcars) "DAILY" else "Daily",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            letterSpacing = if (colors.isLcars) 1.sp else 0.sp,
                            color = if (colors.isLcars) LcarsTan else colors.textSecondary
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
                        val lcarsZebra = if (dayIndex % 2 == 0) Color.Transparent else LcarsOrange.copy(alpha = 0.04f)
                        if (colors.isLcars) Spacer(Modifier.width(2.dp).height(ROW_HEIGHT).background(Color.Black))
                        Box(
                            modifier = Modifier
                                .weight(DAY_WEIGHT)
                                .height(ROW_HEIGHT)
                                .border(0.5.dp, colors.border)
                                .background(if (colors.isLcars) lcarsZebra else zebraColor)
                                .then(if (canFill) Modifier.clickable { onFillShopHours(dayIndex) } else Modifier)
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
                                Text("+", modifier = Modifier.align(Alignment.TopEnd), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textGreen)
                            }
                        }
                    }

                    if (colors.isLcars) Spacer(Modifier.width(2.dp).height(ROW_HEIGHT).background(Color.Black))
                    Box(
                        modifier = Modifier
                            .weight(TOTAL_WEIGHT)
                            .height(ROW_HEIGHT)
                            .border(0.5.dp, colors.border)
                            .background(if (colors.isLcars) LcarsOrange.copy(alpha = 0.2f) else colors.accent.copy(alpha = 0.1f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedCounter(targetValue = uiState.getGrandTotal(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textTotal)
                    }
                }
            }

            // Lunch row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(if (colors.isLcars) Color.Black else colors.surface)
                        .border(0.5.dp, colors.border)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(JOB_WEIGHT)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (colors.isLcars) "LUNCH" else "Lunch",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            letterSpacing = if (colors.isLcars) 1.sp else 0.sp,
                            color = if (colors.isLcars) LcarsTan else colors.textSecondary
                        )
                    }
                    DAYS.forEachIndexed { dayIndex, _ ->
                        val lunchTaken = dayIndex !in uiState.noLunchDays
                        val zebraColor = if (dayIndex % 2 == 0) ZEBRA_EVEN else ZEBRA_ODD
                        val lcarsZebra = if (dayIndex % 2 == 0) Color.Transparent else LcarsOrange.copy(alpha = 0.04f)
                        if (colors.isLcars) Spacer(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                        Box(
                            modifier = Modifier
                                .weight(DAY_WEIGHT)
                                .fillMaxHeight()
                                .border(0.5.dp, colors.border)
                                .background(if (colors.isLcars) lcarsZebra else zebraColor)
                                .clickable { onToggleNoLunch(dayIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Checkbox(
                                checked = lunchTaken,
                                onCheckedChange = { onToggleNoLunch(dayIndex) },
                                modifier = Modifier.scale(0.65f),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (colors.isLcars) LcarsOrange else colors.accent,
                                    uncheckedColor = if (colors.isLcars) LcarsTan else colors.textSecondary
                                )
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(TOTAL_WEIGHT).fillMaxHeight())
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

    val uppercaseTransformation = remember {
        VisualTransformation { text ->
            TransformedText(
                text = AnnotatedString(text.text.uppercase()),
                offsetMapping = OffsetMapping.Identity
            )
        }
    }

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
                .background(if (colors.isLcars) Color.Black else rowColor)
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
                            isDelivery -> if (colors.isLcars) LcarsOrange.copy(alpha = 0.12f) else Color(0x33DD6B20)
                            else -> if (colors.isLcars) Color.Transparent else Color.Transparent
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
                        fontFamily = if (colors.isLcars) AntonioFontFamily else androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Medium
                    ),
                    visualTransformation = if (colors.isLcars) uppercaseTransformation else VisualTransformation.None,
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

            // Day cells
            DAYS.forEachIndexed { dayIndex, _ ->
                val cellValue = uiState.hours.getOrNull(rowIndex)
                    ?.getOrElse(dayIndex) { "" } ?: ""
                val zebraColor = if (dayIndex % 2 == 0) ZEBRA_EVEN else ZEBRA_ODD
                val lcarsZebra = if (dayIndex % 2 == 0) Color.Transparent else LcarsOrange.copy(alpha = 0.04f)

                // Black gap between cells in LCARS mode
                if (colors.isLcars) Spacer(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))

                Box(
                    modifier = Modifier
                        .weight(DAY_WEIGHT)
                        .height(ROW_HEIGHT)
                        .border(0.5.dp, colors.border)
                        .background(if (colors.isLcars) lcarsZebra else zebraColor)
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
                            fontFamily = if (colors.isLcars) AntonioFontFamily else androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal
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
                }
            }

            // Total cell
            if (colors.isLcars) Spacer(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            Box(
                modifier = Modifier
                    .weight(TOTAL_WEIGHT)
                    .height(ROW_HEIGHT)
                    .border(0.5.dp, colors.border)
                    .background(if (colors.isLcars) LcarsOrange.copy(alpha = 0.15f) else colors.hover.copy(alpha = 0.3f))
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
                    .background(if (colors.isLcars) Color.Black else colors.accent.copy(alpha = 0.06f))
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                JobTagChip(label = if (colors.isLcars) "DELIVERY" else "🚚 Delivery", active = isDelivery, activeColor = if (colors.isLcars) LcarsOrange else Color(0xFFDD6B20), colors = colors) { onDeliveryTag(rowIndex) }
                JobTagChip(label = if (colors.isLcars) "PTO" else "🏖 PTO", active = job.uppercase() == "PTO", activeColor = if (colors.isLcars) LcarsBlueBell else colors.accent, colors = colors) { onJobTag(rowIndex, "PTO") }
                JobTagChip(label = if (colors.isLcars) "SICK" else "🤒 Sick", active = job.uppercase() == "SICK", activeColor = if (colors.isLcars) LcarsRed else Color(0xFFE53935), colors = colors) { onJobTag(rowIndex, "SICK") }
                JobTagChip(label = if (colors.isLcars) "HOLIDAY" else "🎉 Holiday", active = job.uppercase() == "HOLIDAY", activeColor = if (colors.isLcars) LcarsPurple else Color(0xFF7B1FA2), colors = colors) { onJobTag(rowIndex, "HOLIDAY") }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (colors.isLcars) LcarsOrange.copy(alpha = 0.2f) else colors.hover).clickable { onFocusClear() }, contentAlignment = Alignment.Center) {
                    Text("✕", color = if (colors.isLcars) LcarsOrange else colors.textSecondary, fontSize = 12.sp)
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
                    .background(if (colors.isLcars) Color.Black else colors.accent.copy(alpha = 0.08f))
                    .border(0.5.dp, colors.border)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (colors.isLcars) "${dayLabel.uppercase()}: ${String.format("%.2f", currentVal)}" else "$dayLabel: ${String.format("%.2f", currentVal)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    letterSpacing = if (colors.isLcars) 0.8.sp else 0.sp,
                    color = if (colors.isLcars) LcarsOrange else colors.textHeading,
                    modifier = Modifier.weight(1f)
                )
                QuickAddChip("-0.25", if (colors.isLcars) LcarsRed else null, colors.textOrange) {
                    val rounded = Math.round(maxOf(0.0, currentVal - 0.25) * 4.0) / 4.0
                    onHoursChange(rowIndex, activeDay, if (rounded > 0) String.format("%.2f", rounded) else "")
                }
                QuickAddChip("+0.25", if (colors.isLcars) LcarsOrange else null, colors.accent) {
                    val newVal = Math.round((currentVal + 0.25) * 4.0) / 4.0
                    onHoursChange(rowIndex, activeDay, String.format("%.2f", newVal))
                }
                QuickAddChip("+0.5", if (colors.isLcars) LcarsOrange else null, colors.accent) {
                    val newVal = Math.round((currentVal + 0.5) * 4.0) / 4.0
                    onHoursChange(rowIndex, activeDay, String.format("%.2f", newVal))
                }
                QuickAddChip("+1", if (colors.isLcars) LcarsTan else null, colors.accent) {
                    val newVal = Math.round((currentVal + 1.0) * 4.0) / 4.0
                    onHoursChange(rowIndex, activeDay, String.format("%.2f", newVal))
                }
                QuickAddChip("+1.5", if (colors.isLcars) LcarsTan else null, colors.accent) {
                    val newVal = Math.round((currentVal + 1.5) * 4.0) / 4.0
                    onHoursChange(rowIndex, activeDay, String.format("%.2f", newVal))
                }
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(if (colors.isLcars) LcarsOrange.copy(alpha = 0.2f) else colors.hover).clickable { onFocusClear() }, contentAlignment = Alignment.Center) {
                    Text("\u2715", color = if (colors.isLcars) LcarsOrange else colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    val colors = LocalTimecardColors.current
    Box(
        modifier = Modifier
            .weight(weight)
            .height(ROW_HEIGHT)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (colors.isLcars) text.uppercase() else text,
            color = if (colors.isLcars) Color.Black else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
            letterSpacing = if (colors.isLcars) 0.8.sp else 0.sp,
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
    val isLcars = colors.isLcars
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isLcars && active -> activeColor
                isLcars -> Color.Black
                active -> activeColor.copy(alpha = 0.18f)
                else -> colors.input
            }
        ),
        border = when {
            isLcars && active -> null
            isLcars -> androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.4f))
            active -> androidx.compose.foundation.BorderStroke(1.5.dp, activeColor)
            else -> androidx.compose.foundation.BorderStroke(1.dp, colors.border)
        },
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.height(36.dp)
    ) {
        Text(
            text = if (isLcars) label.uppercase() else label,
            color = when {
                isLcars && active -> Color.Black
                isLcars -> activeColor
                active -> activeColor
                else -> colors.textSecondary
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isLcars) AntonioFontFamily else androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
            letterSpacing = if (isLcars) 0.5.sp else 0.sp
        )
    }
}

@Composable
private fun QuickAddChip(label: String, lcarsBg: Color?, textColor: Color, onClick: () -> Unit) {
    val colors = LocalTimecardColors.current
    val bg = if (colors.isLcars && lcarsBg != null) lcarsBg else colors.input
    val lblColor = if (colors.isLcars && lcarsBg != null) Color.Black else textColor
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        border = if (colors.isLcars) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier.height(40.dp)
    ) {
        Text(
            text = label,
            color = lblColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (colors.isLcars) AntonioFontFamily else androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
            letterSpacing = if (colors.isLcars) 0.5.sp else 0.sp
        )
    }
}
