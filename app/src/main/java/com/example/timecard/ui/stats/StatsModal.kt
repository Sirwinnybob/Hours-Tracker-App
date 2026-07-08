package com.example.timecard.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.data.model.DAY_LABELS
import com.example.timecard.data.model.DAY_LABELS_SHORT
import com.example.timecard.data.model.DAYS
import com.example.timecard.domain.DateUtils
import com.example.timecard.domain.StatsPeriod
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsAnakiwa
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.StatsBlue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import com.example.timecard.ui.theme.StatsGreen
import com.example.timecard.ui.theme.StatsPurple
import com.example.timecard.ui.charts.PieChart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsModal(
    viewModel: StatsViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    val stats = viewModel.stats
    
    var showDatePicker by remember { mutableStateOf(false) }

    val modalContent = @Composable {
        Box(
            modifier = if (colors.isLcars) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                    .safeDrawingPadding()
            },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (colors.isLcars) Color.Black else colors.surface,
                        if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp))
                    )
            ) {
                if (colors.isLcars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(LcarsOrange).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "STATISTICS",
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.size(width = 52.dp, height = 26.dp)
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(50)))
                                .background(LcarsRed)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CLOSE", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (colors.isLcars) 16.dp else 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                if (!colors.isLcars) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "\uD83D\uDCCA Statistics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "\u2715",
                        fontSize = 20.sp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Period tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val periods = listOf(
                        StatsPeriod.ThisWeek,
                        StatsPeriod.LastWeek,
                        StatsPeriod.TwoWeeks,
                        StatsPeriod.ThisMonth,
                        StatsPeriod.LastMonth,
                        StatsPeriod.AllTime,
                        StatsPeriod.Custom("", "") // Dummy for button
                    )
                    
                    items(periods) { period ->
                        val isCustomBtn = period is StatsPeriod.Custom
                        val isActive = if (isCustomBtn) viewModel.selectedPeriod is StatsPeriod.Custom else viewModel.selectedPeriod == period
                        val label = if (isCustomBtn && isActive && viewModel.selectedPeriod is StatsPeriod.Custom) {
                            "Custom Range" // Could format dates here if preferred
                        } else {
                            period.label
                        }

                        val tabBg = if (colors.isLcars) {
                            if (isActive) LcarsAnakiwa else LcarsTan
                        } else {
                            if (isActive) colors.accent else colors.surface
                        }
                        val tabTextColor = if (colors.isLcars) {
                            Color.Black
                        } else {
                            if (isActive) Color.White else colors.textSecondary
                        }
                        val tabShape = if (colors.isLcars) RoundedCornerShape(50) else RoundedCornerShape(20.dp)
                        val tabFont = if (colors.isLcars) AntonioFontFamily else null
                        val tabLabel = if (colors.isLcars) label.uppercase() else label

                        Box(
                            modifier = Modifier
                                .clip(tabShape)
                                .background(tabBg)
                                .clickable {
                                    if (isCustomBtn) {
                                        showDatePicker = true
                                    } else {
                                        viewModel.loadStats(period)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                tabLabel,
                                fontSize = 12.sp,
                                fontFamily = tabFont,
                                fontWeight = if (colors.isLcars || isActive) FontWeight.Bold else FontWeight.Normal,
                                color = tabTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (showDatePicker) {
                    val dateRangePickerState = rememberDateRangePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                    val startMillis = dateRangePickerState.selectedStartDateMillis
                                    val endMillis = dateRangePickerState.selectedEndDateMillis
                                    if (startMillis != null && endMillis != null) {
                                        // Convert millis to YYYY-MM-DD
                                        val cal = java.util.Calendar.getInstance()
                                        cal.timeInMillis = startMillis
                                        val startStr = DateUtils.formatLocalDate(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                                        cal.timeInMillis = endMillis
                                        val endStr = DateUtils.formatLocalDate(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                                        
                                        viewModel.loadStats(StatsPeriod.Custom(startStr, endStr))
                                    }
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
                    }
                }

                if (stats == null || stats.weekCount == 0) {
                    Text(
                        "No data available for this period.",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(32.dp)
                    )
                } else {
                    // Summary cards
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            "Total Hours",
                            stats.totalHours,
                            StatsBlue,
                            Modifier.weight(1f)
                        ) { String.format("%.2f", it) }
                        
                        val shopPct = if (stats.totalHours > 0) (stats.shopHours / stats.totalHours * 100) else 0.0
                        StatCard(
                            "Shop %",
                            shopPct,
                            StatsPurple,
                            Modifier.weight(1f)
                        ) { "${it.toInt()}%" }
                        
                        StatCard(
                            "Jobs",
                            stats.jobMap.size.toDouble(),
                            StatsGreen,
                            Modifier.weight(1f)
                        ) { "${it.toInt()}" }
                        
                        val workDays = stats.weekCount * 6
                        val avgDaily = if (workDays > 0) stats.totalHours / workDays else 0.0
                        StatCard(
                            "Avg/Day",
                            avgDaily,
                            StatsBlue,
                            Modifier.weight(1f)
                        ) { String.format("%.2f", it) }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pie chart
                    Text(
                        text = if (colors.isLcars) "JOB BREAKDOWN" else "Job Breakdown",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PieChart(
                        data = stats.jobMap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Job bars - breakdown list
                    Text(
                        text = if (colors.isLcars) "HOURS BY JOB" else "Hours by Job",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sortedJobs = stats.jobMap.entries.sortedByDescending { it.value }
                    val maxHours = sortedJobs.firstOrNull()?.value ?: 1.0
                    val chartColors = colors.chartColors

                    sortedJobs.forEachIndexed { index, (job, hours) ->
                        val pct = if (stats.totalHours > 0) (hours / stats.totalHours * 100) else 0.0
                        val barPct = if (maxHours > 0) (hours / maxHours) else 0.0
                        val barColor = chartColors[index % chartColors.size]

                        // Animate Bar Width
                        val animatedWidth = animateFloatAsState(
                            targetValue = barPct.toFloat(),
                            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                            label = "barWidth"
                        )

                        val barShape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = if (colors.isLcars) job.uppercase() else job,
                                fontSize = 12.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textPrimary,
                                modifier = Modifier.width(60.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .clip(barShape)
                                    .background(if (colors.isLcars) Color(0xFF222222) else colors.border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedWidth.value)
                                        .height(16.dp)
                                        .clip(barShape)
                                        .background(barColor)
                                )
                            }
                            Text(
                                text = String.format("%.2fh", hours),
                                fontSize = 11.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textPrimary,
                                modifier = Modifier.width(50.dp).padding(start = 6.dp)
                            )
                            Text(
                                text = "${pct.toInt()}%",
                                fontSize = 11.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textSecondary,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Daily averages
                    Text(
                        text = if (colors.isLcars) "DAILY AVERAGES" else "Daily Averages",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DAYS.forEach { day ->
                            val avg = if (stats.weekCount > 0) {
                                (stats.dailyTotals[day] ?: 0.0) / stats.weekCount
                            } else 0.0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (colors.isLcars) (DAY_LABELS[day] ?: day).uppercase() else (DAY_LABELS[day] ?: day),
                                    fontSize = 11.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = String.format("%.2f", avg),
                                    fontSize = 13.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Job search
                    Text(
                        text = if (colors.isLcars) "JOB SEARCH" else "Job Search",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { 
                                Text(
                                    text = if (colors.isLcars) "ENTER JOB NUMBER..." else "Enter job number...", 
                                    color = colors.textSecondary,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null
                                ) 
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = colors.textPrimary,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (colors.isLcars) Color(0xFF151515) else colors.input,
                                unfocusedContainerColor = if (colors.isLcars) Color(0xFF111111) else colors.input,
                                focusedIndicatorColor = if (colors.isLcars) LcarsOrange else Color.Transparent,
                                unfocusedIndicatorColor = if (colors.isLcars) LcarsTan.copy(alpha = 0.5f) else Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchJob() }),
                            shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.searchJob() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (colors.isLcars) LcarsTan else colors.accent),
                            shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (colors.isLcars) "SEARCH" else "Search",
                                color = if (colors.isLcars) Color.Black else Color.White,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (viewModel.searchResults.isNotEmpty()) {
                        viewModel.searchResults.forEach { result ->
                            val label = DateUtils.formatWeekLabel(result.date)
                            val dayDetail = DAYS.mapNotNull { day ->
                                val v = result.dailyBreakdown[day] ?: 0.0
                                if (v > 0) "${DAY_LABELS_SHORT[day]}:$v" else null
                            }.joinToString(" ")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (colors.isLcars) label.uppercase() else label, 
                                        fontSize = 13.sp, 
                                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = if (colors.isLcars) dayDetail.uppercase() else dayDetail, 
                                        fontSize = 11.sp, 
                                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = String.format("%.2fh", result.hours),
                                    fontSize = 13.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textTotal
                                )
                            }
                        }

                        val totalBoxShape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(totalBoxShape)
                                .background(if (colors.isLcars) Color.Black else colors.hover)
                                .then(if (colors.isLcars) Modifier.border(1.dp, LcarsOrange.copy(alpha = 0.5f), totalBoxShape) else Modifier)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (colors.isLcars) "TOTAL (${viewModel.searchResults.size} WEEK${if (viewModel.searchResults.size != 1) "S" else ""})" else "Total (${viewModel.searchResults.size} week${if (viewModel.searchResults.size != 1) "s" else ""})",
                                fontSize = 13.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = String.format("%.2fh", viewModel.searchTotalHours),
                                fontSize = 14.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textTotal
                            )
                        }
                    } else if (viewModel.searchQuery.isNotBlank() && viewModel.searchResults.isEmpty()) {
                        Text(
                            "No entries found",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (!colors.isLcars) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                } // end inner Column

                if (colors.isLcars) {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(LcarsTan))
                }
            }
        }
    }

    if (colors.isLcars) {
        modalContent()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            modalContent()
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    targetValue: Double,
    valueColor: Color,
    modifier: Modifier = Modifier,
    format: (Double) -> String
) {
    val colors = LocalTimecardColors.current
    val animatedValue = remember { Animatable(0f) }
    
    // Animate to new target value when it changes
    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val cardShape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp))
    val cardBackground = if (colors.isLcars) Color.Black else colors.hover
    val cardBorder = if (colors.isLcars) BorderStroke(1.dp, valueColor.copy(alpha = 0.5f)) else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(cardShape)
            .background(cardBackground)
            .then(if (cardBorder != null) Modifier.border(cardBorder, cardShape) else Modifier)
            .padding(12.dp)
    ) {
        Text(
            text = if (colors.isLcars) label.uppercase() else label,
            fontSize = 10.sp,
            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
            color = colors.textSecondary,
            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = format(animatedValue.value.toDouble()),
            fontSize = 20.sp,
            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
