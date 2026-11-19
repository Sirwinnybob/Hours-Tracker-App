package com.example.timecard.ui.timesheet.standard

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How far a day is toward its target, for the day tiles and the Daily total row. */
enum class DayState { Met, Partial, NotStarted, NoTarget }

data class DaySummary(
    val index: Int,
    val label: String,
    /** "Nov 16", or "" when the week start is unknown. */
    val date: String,
    val total: Double,
    val target: Double,
    val state: DayState,
    /** Hours still missing (0 when met or no target), rounded to the quarter hour. */
    val shortBy: Double,
    /** A short weekday (Mon–Fri) can be topped up with SHOP. Same rule as the old "+" in the footer. */
    val canFillShop: Boolean,
    val isToday: Boolean
) {
    /** 0..1 progress toward the target (0 for days without one). */
    val progress: Float get() = if (target > 0) (total / target).coerceIn(0.0, 1.0).toFloat() else 0f
}

object DayTargets {
    /** Same targets TimesheetViewModel.fillShopHours uses: 9 h Mon–Thu, 4 h Fri, none Sat. */
    fun target(dayIndex: Int): Double = when (dayIndex) {
        in 0..3 -> 9.0
        4 -> 4.0
        else -> 0.0
    }

    const val WEEK_TARGET = 40.0
}

private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** Monday of the timesheet week from `activeWeekDate` ("2026-11-16"), or null when it isn't a date. */
fun weekStartOf(activeWeekDate: String): LocalDate? =
    runCatching { LocalDate.parse(activeWeekDate.trim()) }.getOrNull()

/** Index (0=Mon…5=Sat) of [today] inside the week starting [weekStart], or null when outside it. */
fun todayIndexIn(weekStart: LocalDate?, today: LocalDate): Int? {
    if (weekStart == null) return null
    val offset = java.time.temporal.ChronoUnit.DAYS.between(weekStart, today).toInt()
    return offset.takeIf { it in 0..5 }
}

fun summarizeDays(dayTotals: List<Double>, weekStart: LocalDate?, todayIndex: Int?): List<DaySummary> =
    DAY_SHORT.indices.map { i ->
        val total = dayTotals.getOrElse(i) { 0.0 }
        val target = DayTargets.target(i)
        val state = when {
            target <= 0.0 -> DayState.NoTarget
            total >= target -> DayState.Met
            total > 0.0 -> DayState.Partial
            else -> DayState.NotStarted
        }
        val short = if (target > total) Math.round((target - total) * 4.0) / 4.0 else 0.0
        DaySummary(
            index = i,
            label = DAY_SHORT[i],
            date = weekStart?.plusDays(i.toLong())?.format(DATE_FMT) ?: "",
            total = total,
            target = target,
            state = state,
            shortBy = short,
            canFillShop = i < 5 && short > 0.0,
            isToday = todayIndex == i
        )
    }

/** "33.0" — one decimal, the way the cards show hours. */
fun formatHours(value: Double): String = String.format(Locale.US, "%.1f", value)
