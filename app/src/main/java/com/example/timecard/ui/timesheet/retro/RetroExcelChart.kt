package com.example.timecard.ui.timesheet.retro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.timesheet.standard.DaySummary
import com.example.timecard.ui.timesheet.standard.DayTargets
import com.example.timecard.ui.timesheet.standard.formatHours
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Bar height as a 0..1 share of the value axis (12 h), clamped. */
internal fun xlBarShare(hours: Double): Float = (hours / Xl.CHART_MAX).coerceIn(0.0, 1.0).toFloat()

/**
 * Embedded chart object: Excel 95's 3D clustered columns, Hours vs Target per day, gray plot area,
 * black gridlines. Bars grow in one after another whenever the week changes (unless motion is off).
 */
@Composable
internal fun XlChart(days: List<DaySummary>, weekKey: String, animate: Boolean, modifier: Modifier = Modifier) {
    val grow = remember(weekKey) { days.indices.map { Animatable(if (animate) 0f else 1f) } }
    LaunchedEffect(weekKey, animate) {
        if (!animate) { grow.forEach { it.snapTo(1f) }; return@LaunchedEffect }
        grow.forEachIndexed { i, a ->
            launch { delay(150L + i * 110L); a.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
        }
    }
    val total = days.sumOf { it.total }
    Box(modifier.background(Xl.White).padding(start = 30.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)) {
        Column(
            Modifier.fillMaxSize().background(Xl.White).xlHairline().padding(horizontal = 10.dp, vertical = 6.dp)
                .semantics { contentDescription = "Chart of hours by day, week total ${formatHours(total)} of ${formatHours(DayTargets.WEEK_TARGET)}" }
        ) {
            Text(
                "Hours by Day — Week Total ${formatHours(total)} of ${formatHours(DayTargets.WEEK_TARGET)}",
                style = xlText(15.sp, weight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(22.dp).fillMaxHeight().padding(bottom = 20.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    listOf("12", "9", "6", "3", "0").forEach { Text(it, style = xlText(11.sp)) }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(Modifier.weight(1f).fillMaxWidth()) { drawPlot(days, grow.map { it.value }) }
                    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        days.forEach { Text(it.label, style = xlText(12.sp, weight = if (it.isToday) FontWeight.Bold else FontWeight.Normal), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                    }
                }
                Column(Modifier.align(Alignment.CenterVertically).xlHairline().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendKey(Xl.SeriesHours, "Hours")
                    LegendKey(Xl.SeriesTarget, "Target")
                }
            }
        }
    }
}

@Composable
private fun LegendKey(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(11.dp).background(color).xlHairline())
        Text(label, style = xlText(12.sp))
    }
}

private fun DrawScope.drawPlot(days: List<DaySummary>, grow: List<Float>) {
    val w = size.width
    val h = size.height
    drawRect(Xl.Face)
    val line = 1.dp.toPx()
    for (i in 0..4) {
        val y = h * i / 4f
        drawLine(Xl.Black, Offset(0f, y), Offset(w, y), line)
    }
    drawLine(Xl.Black, Offset(line / 2, 0f), Offset(line / 2, h), line)
    val slot = w / days.size
    val barW = (slot * 0.26f).coerceAtMost(26.dp.toPx())
    val depth = 5.dp.toPx()
    days.forEachIndexed { i, d ->
        val g = grow.getOrElse(i) { 1f }
        val cx = slot * i + slot / 2
        bar3d(cx - barW - 1.5f, barW, h * xlBarShare(d.total) * g, h, depth, Xl.SeriesHours, Xl.SeriesHoursSide)
        bar3d(cx + 1.5f, barW, h * xlBarShare(d.target) * g, h, depth, Xl.SeriesTarget, Xl.SeriesTargetSide)
    }
}

/** One 3D column: front face, darker side and top, black outline. */
private fun DrawScope.bar3d(left: Float, width: Float, height: Float, bottom: Float, depth: Float, front: Color, side: Color) {
    if (height <= 0.5f) return
    val top = bottom - height
    val outline = Stroke(1.dp.toPx())
    val sidePath = Path().apply {
        moveTo(left + width, top); lineTo(left + width + depth, top - depth); lineTo(left + width + depth, bottom - depth); lineTo(left + width, bottom); close()
    }
    val topPath = Path().apply {
        moveTo(left, top); lineTo(left + depth, top - depth); lineTo(left + width + depth, top - depth); lineTo(left + width, top); close()
    }
    drawPath(sidePath, side); drawPath(sidePath, Xl.Black, style = outline)
    drawPath(topPath, side.copy(alpha = 0.75f)); drawPath(topPath, Xl.Black, style = outline)
    drawRect(front, Offset(left, top), Size(width, height))
    drawRect(Xl.Black, Offset(left, top), Size(width, height), style = outline)
}
