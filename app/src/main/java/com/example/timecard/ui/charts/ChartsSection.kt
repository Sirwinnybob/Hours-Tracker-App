package com.example.timecard.ui.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Color

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
            val toggleShape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp))
            val toggleBg = if (colors.isLcars) Color.Black else colors.surface
            val toggleBorder = if (colors.isLcars) BorderStroke(1.dp, com.example.timecard.ui.theme.LcarsOrange.copy(alpha = 0.5f)) else null

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(toggleShape)
                    .background(toggleBg)
                    .then(if (toggleBorder != null) Modifier.border(toggleBorder, toggleShape) else Modifier)
                    .clickable { onToggle() }
            ) {
                if (colors.isLcars) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .fillMaxHeight()
                            .background(com.example.timecard.ui.theme.LcarsOrange)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (colors.isLcars) "VIEW CHARTS" else "\uD83D\uDCCA View Charts",
                        color = if (colors.isLcars) com.example.timecard.ui.theme.LcarsOrange else colors.textSecondary,
                        fontSize = 16.sp,
                        fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (expanded) "\u25B2" else "\u25BC",
                        color = if (colors.isLcars) com.example.timecard.ui.theme.LcarsOrange else colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
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
                    if (colors.isLcars) "CURRENT WEEK" else "Current Week",
                    fontSize = 14.sp,
                    fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = if (colors.isLcars) com.example.timecard.ui.theme.LcarsTan else colors.textHeading,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (jobTotals.isNotEmpty()) {
                    PieChartCard(jobTotals = jobTotals)
                } else {
                    Text(
                        if (colors.isLcars) "NO DATA YET" else "No data yet",
                        color = colors.textSecondary,
                        fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
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
                        if (colors.isLcars) "PREVIOUS WEEK" else "Previous Week",
                        fontSize = 14.sp,
                        fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isLcars) com.example.timecard.ui.theme.LcarsTan else colors.textHeading,
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

    val shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp))
    val bg = if (colors.isLcars) Color.Black else colors.surface
    val border = if (colors.isLcars) BorderStroke(1.dp, com.example.timecard.ui.theme.LcarsTan.copy(alpha = 0.5f)) else null

    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .height(220.dp)
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        PieChart(
            data = jobTotals,
            modifier = Modifier.fillMaxSize()
        )
    }
}
