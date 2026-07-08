package com.example.timecard.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.DAY_LABELS
import com.example.timecard.data.model.DAYS
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun DailyTotalsBar(
    dailyTotals: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(dailyTotals) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }
    
    val maxTotal = dailyTotals.values.maxOrNull() ?: 1.0

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        DAYS.forEachIndexed { index, day ->
            val total = dailyTotals[day] ?: 0.0
            val isGood = when (index) {
                in 0..3 -> total >= 9.0
                4 -> total >= 4.0
                else -> true
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (colors.isLcars) (DAY_LABELS[day] ?: day).uppercase() else (DAY_LABELS[day] ?: day),
                    fontSize = 12.sp,
                    fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
                    color = colors.textSecondary,
                    fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Medium
                )
                
                Text(
                    text = if (total > 0) String.format("%.2f", total) else "-",
                    fontSize = 14.sp,
                    fontFamily = if (colors.isLcars) com.example.timecard.ui.theme.AntonioFontFamily else null,
                    fontWeight = if (colors.isLcars) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (total > 0) {
                        if (isGood) colors.textGreen else colors.textOrange
                    } else colors.textSecondary
                )
            }
        }
    }
}
