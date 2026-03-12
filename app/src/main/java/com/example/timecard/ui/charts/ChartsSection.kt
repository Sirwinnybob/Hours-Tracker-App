package com.example.timecard.ui.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.HourCalculator
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun ChartsSection(
    currentData: com.example.timecard.data.model.TimecardData,
    previousData: com.example.timecard.data.model.TimecardData?,
    expanded: Boolean,
    onToggle: () -> Unit,
    showToggle: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current

    Column(modifier = modifier) {
        // Toggle bar
        if (showToggle) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)))
                    .background(colors.surface)
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    "\uD83D\uDCCA View Charts",
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "\u25B2" else "\u25BC",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            // No verticalScroll here — parent already scrolls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val data = currentData
                val jobTotals = HourCalculator.getJobTotals(data.rows)
                val dailyTotals = HourCalculator.calcDailyTotals(data.rows)

                Text(
                    "Current Week",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textHeading,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (jobTotals.isNotEmpty()) {
                    PieChartCard(jobTotals = jobTotals)
                } else {
                    Text(
                        "No data yet",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                DailyTotalsBar(dailyTotals = dailyTotals)

                Spacer(modifier = Modifier.height(16.dp))

                // Previous week chart
                if (previousData != null) {
                    val prevJobTotals = HourCalculator.getJobTotals(previousData.rows)
                    val prevDailyTotals = HourCalculator.calcDailyTotals(previousData.rows)

                    Text(
                        "Previous Week",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textHeading,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    if (prevJobTotals.isNotEmpty()) {
                        PieChartCard(jobTotals = prevJobTotals)
                    }

                    DailyTotalsBar(dailyTotals = prevDailyTotals)
                }
            }
        }
    }
}

@Composable
private fun PieChartCard(jobTotals: Map<String, Double>) {
    val colors = LocalTimecardColors.current

    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .height(220.dp)
            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)))
            .background(colors.surface)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        PieChart(
            data = jobTotals,
            modifier = Modifier.fillMaxSize()
        )
    }
}
