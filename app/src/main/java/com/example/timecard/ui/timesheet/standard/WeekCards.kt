package com.example.timecard.ui.timesheet.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "This week" card: week total against 40 h, the week bar, and one tile per day
 * (total + green/amber bar, today outlined). Lunch and SHOP fill live in the grid.
 */
@Composable
internal fun WeekStripCard(
    look: StandardLook,
    days: List<DaySummary>,
    weekTotal: Double,
    weekRange: String,
    isViewingPrevious: Boolean,
    shopLabel: String?,
    onShop: () -> Unit
) {
    SectionCard(look) {
        SectionTitle(look, if (isViewingPrevious) "Last week" else "This week", weekRange) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontFamily = HoursMono, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = look.text)) {
                        append(formatHours(weekTotal))
                    }
                    withStyle(SpanStyle(fontFamily = HoursMono, fontSize = 15.sp, color = look.text2)) { append(" / 40 h") }
                }
            )
            val left = (DayTargets.WEEK_TARGET - weekTotal).coerceAtLeast(0.0)
            Text(
                if (left > 0) "${formatHours(left)} left" else "Week met",
                color = if (left > 0) look.warn else look.doneText,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
        ProgressBar((weekTotal / DayTargets.WEEK_TARGET).toFloat(), look.weekBar, look.soft, height = 10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            days.forEach { day -> DayTile(look, day, Modifier.weight(1f)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Green = day met · amber = part done · lunch and Shop fill are in the grid below",
                color = look.text2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (shopLabel != null) {
                PillButton(shopLabel, look, onShop, kind = PillKind.Filled, icon = HoursIcons.Shop, height = 36.dp)
            }
        }
    }
}

@Composable
private fun DayTile(look: StandardLook, day: DaySummary, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (day.isToday) look.today else look.head)
            .border(if (day.isToday) 2.dp else 1.dp, if (day.isToday) look.primary else look.line, shape)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(day.label, color = look.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text(day.date, color = look.text2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Text(
            if (day.state == DayState.NoTarget && day.total == 0.0) "–" else formatHours(day.total),
            style = monoStyle(22, dayTotalColor(look, day))
        )
        ProgressBar(day.progress, SolidColor(if (day.state == DayState.Met) look.done else look.progress), look.line)
    }
}

internal fun dayTotalColor(look: StandardLook, day: DaySummary) = when (day.state) {
    DayState.Met -> look.doneText
    DayState.Partial -> look.progressText
    DayState.NotStarted, DayState.NoTarget -> if (day.total > 0) look.text else look.empty
}

/** One job's share of the week. */
data class JobShare(val job: String, val name: String?, val hours: Double)

/** "Where the week went": hours per job, biggest first. Hidden when nothing is logged (collapses). */
@Composable
internal fun WeekSplitCard(look: StandardLook, shares: List<JobShare>, onStats: () -> Unit) {
    if (shares.isEmpty()) return
    val total = shares.sumOf { it.hours }.coerceAtLeast(0.01)
    SectionCard(look) {
        SectionTitle(look, "Where the week went") {
            PillButton("Stats", look, onStats, kind = PillKind.Tonal, icon = HoursIcons.Stats, height = 36.dp)
        }
        shares.forEach { share ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
            Row(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(share.job, color = look.text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.width(64.dp))
                Text(
                    share.name ?: "",
                    color = look.text2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(250.dp)
                )
                ProgressBar((share.hours / total).toFloat(), SolidColor(look.primary), look.soft, Modifier.weight(1f), height = 10.dp)
                Text("${formatHours(share.hours)} h", style = monoStyle(15, look.text), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
