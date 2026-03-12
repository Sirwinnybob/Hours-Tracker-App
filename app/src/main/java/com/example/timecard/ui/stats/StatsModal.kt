package com.example.timecard.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.StatsBlue
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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

                        Box(
                            modifier = Modifier
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                                .background(
                                    if (isActive) colors.accent else colors.surface
                                )
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
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) Color.White else colors.textSecondary
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
                        "Job Breakdown",
                        fontSize = 15.sp,
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
                        "Hours by Job",
                        fontSize = 15.sp,
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(job, fontSize = 12.sp, color = colors.textPrimary, modifier = Modifier.width(60.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                    .background(colors.border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedWidth.value)
                                        .height(16.dp)
                                        .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                        .background(barColor)
                                )
                            }
                            Text(
                                String.format("%.2fh", hours),
                                fontSize = 11.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.width(50.dp).padding(start = 6.dp)
                            )
                            Text(
                                "${pct.toInt()}%",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Daily averages
                    Text(
                        "Daily Averages",
                        fontSize = 15.sp,
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
                                Text(DAY_LABELS[day] ?: day, fontSize = 11.sp, color = colors.textSecondary)
                                Text(
                                    String.format("%.2f", avg),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Job search
                    Text(
                        "Job Search",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.searchQuery = it },
                            placeholder = { Text("Enter job number...", color = colors.textSecondary) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = colors.textPrimary),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = colors.input,
                                unfocusedContainerColor = colors.input,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchJob() }),
                            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.searchJob() },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Text("Search")
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
                                    Text(label, fontSize = 13.sp, color = colors.textPrimary)
                                    Text(dayDetail, fontSize = 11.sp, color = colors.textSecondary)
                                }
                                Text(
                                    String.format("%.2fh", result.hours),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textTotal
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)))
                                .background(colors.hover)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total (${viewModel.searchResults.size} week${if (viewModel.searchResults.size != 1) "s" else ""})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                String.format("%.2fh", viewModel.searchTotalHours),
                                fontSize = 14.sp,
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

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)))
            .background(colors.hover)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 10.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = format(animatedValue.value.toDouble()),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
