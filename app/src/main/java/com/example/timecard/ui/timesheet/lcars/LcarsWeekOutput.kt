package com.example.timecard.ui.timesheet.lcars

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.timesheet.standard.DayState
import com.example.timecard.ui.timesheet.standard.DaySummary
import com.example.timecard.ui.timesheet.standard.DayTargets
import com.example.timecard.ui.timesheet.standard.formatHours
import kotlinx.coroutines.delay

const val LCARS_TANK_SEGMENTS = 12

/** How many of a day's [LCARS_TANK_SEGMENTS] segments are lit (0 for an empty day, full for any Saturday hours). */
fun lcarsLitSegments(day: DaySummary): Int = when {
    day.target > 0 -> ((day.total / day.target) * LCARS_TANK_SEGMENTS).let { Math.round(it).toInt() }.coerceIn(0, LCARS_TANK_SEGMENTS)
    day.total > 0 -> LCARS_TANK_SEGMENTS
    else -> 0
}

internal fun lcarsDayColor(day: DaySummary): Color = when (day.state) {
    DayState.Met -> LcarsClassic.Creme
    DayState.Partial -> LcarsClassic.Orange
    DayState.NotStarted -> LcarsClassic.Lilac
    DayState.NoTarget -> if (day.total > 0) LcarsClassic.Orange else LcarsClassic.Dim
}

internal fun lcarsDayValueColor(day: DaySummary): Color = when (day.state) {
    DayState.Met -> LcarsClassic.Creme
    DayState.Partial -> LcarsClassic.Orange
    else -> if (day.total > 0) LcarsClassic.Orange else LcarsClassic.Muted
}

/**
 * WEEK OUTPUT: one level tank per day (segments fill toward the day's target, one by one, and refill
 * when the week changes), a scan line sweeping across, today's cap pulsing, and the week total.
 */
@Composable
internal fun LcarsWeekOutput(days: List<DaySummary>, weekTotal: Double, weekKey: String, animate: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "lcarsWeek")
    val pulse by infinite.animateFloat(1f, 0.45f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "todayPulse")
    val scan by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "scan")

    Row(modifier.fillMaxWidth().height(226.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.weight(6.6f)) {
            val tankWidth = (maxWidth - 12.dp * 5) / 6
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, day ->
                    DayTank(day, i, weekKey, animate, if (day.isToday && animate) pulse else 1f, Modifier.width(tankWidth))
                }
            }
            if (animate) {
                Box(
                    Modifier
                        .offset(x = (maxWidth + 30.dp) * scan - 30.dp, y = 28.dp)
                        .width(2.dp).height(152.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, LcarsClassic.Bluey, Color.Transparent)))
                )
            }
        }
        Column(Modifier.weight(2.2f).padding(bottom = 4.dp), horizontalAlignment = Alignment.End) {
            Text(formatHours(weekTotal), style = lcarsText(64.sp, LcarsClassic.Orange, FontWeight.Normal), maxLines = 1)
            Text("OF ${formatHours(DayTargets.WEEK_TARGET)} HOURS", style = lcarsText(16.sp, LcarsClassic.Creme, FontWeight.Normal))
            val left = (DayTargets.WEEK_TARGET - weekTotal).coerceAtLeast(0.0)
            Box(
                Modifier.padding(top = 10.dp).height(30.dp).clip(RoundedCornerShape(50))
                    .background(if (left > 0) LcarsClassic.Red else LcarsClassic.Creme).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (left > 0) "${formatHours(left)} TO TARGET" else "TARGET MET", style = lcarsText(14.sp, Color.Black))
            }
        }
    }
}

@Composable
private fun DayTank(day: DaySummary, index: Int, weekKey: String, animate: Boolean, capAlpha: Float, modifier: Modifier) {
    val color = lcarsDayColor(day)
    val lit = lcarsLitSegments(day)
    // Lit segments count up one at a time; restarts when the week (or the day's level) changes.
    val shown = remember(weekKey) { Animatable(if (animate) 0f else lit.toFloat()) }
    val entranceDelay = lcarsGateWaitMs().toInt()
    val firstFill = remember { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(weekKey, lit, animate) {
        if (!animate) { shown.snapTo(lit.toFloat()); return@LaunchedEffect }
        if (shown.value > lit) shown.snapTo(lit.toFloat())
        val wait = if (shown.value == 0f) (if (firstFill.value) entranceDelay + 550 else 150) + index * 60 else 0
        firstFill.value = false
        delay(wait.toLong())
        shown.animateTo(lit.toFloat(), tween(((lit - shown.value).coerceAtLeast(1f) * LcarsClassic.TANK_SEGMENT_MS).toInt(), easing = LinearEasing))
    }
    Column(
        modifier.semantics { contentDescription = "${day.label} ${formatHours(day.total)} of ${formatHours(day.target)} hours" },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(24.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(if (day.isToday) LcarsClassic.Bluey else color)
                .graphicsLayer { alpha = capAlpha },
            contentAlignment = Alignment.Center
        ) { Text(day.label.uppercase(), style = lcarsText(14.sp, Color.Black)) }
        Column(
            Modifier.fillMaxWidth().height(152.dp)
                .border(2.dp, color, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
        ) {
            for (k in LCARS_TANK_SEGMENTS - 1 downTo 0) {
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(2.dp))
                        .background(if (k < shown.value) color else LcarsClassic.Track)
                )
            }
        }
        Text(
            if (day.state == DayState.NoTarget && day.total == 0.0) "—" else formatHours(day.total),
            style = lcarsText(22.sp, lcarsDayValueColor(day), FontWeight.Normal),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
