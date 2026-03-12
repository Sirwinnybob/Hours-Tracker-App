package com.example.timecard.domain

import java.util.Calendar
import java.util.Locale

object DateUtils {

    fun formatLocalDate(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun getWeekStartingMonday(): String {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        return formatLocalDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getPreviousMonday(currentWeekDate: String): String {
        val cal = parseDate(currentWeekDate)
        cal.add(Calendar.DAY_OF_MONTH, -7)
        return formatLocalDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getMondayNWeeksAgo(currentWeekDate: String, n: Int): String {
        val cal = parseDate(currentWeekDate)
        cal.add(Calendar.DAY_OF_MONTH, -(7 * n))
        return formatLocalDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getCurrentDayIndex(): Int {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to 0=Mon, 5=Sat
        return if (dayOfWeek == Calendar.SUNDAY) 5 else dayOfWeek - Calendar.MONDAY
    }

    fun formatWeekLabel(dateStr: String): String {
        val cal = parseDate(dateStr)
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val month = months[cal.get(Calendar.MONTH)]
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val year = cal.get(Calendar.YEAR)
        return "$month $day, $year"
    }

    /**
     * Checks if a targetDate falls within the requested month,
     * offset from the current active date (e.g. 0 = this month, 1 = last month)
     */
    fun isInSameMonth(targetDate: String, activeDate: String, monthOffset: Int): Boolean {
        val targetCal = parseDate(targetDate)
        val activeCal = parseDate(activeDate)
        
        activeCal.add(Calendar.MONTH, -monthOffset)
        
        return targetCal.get(Calendar.YEAR) == activeCal.get(Calendar.YEAR) &&
               targetCal.get(Calendar.MONTH) == activeCal.get(Calendar.MONTH)
    }

    /**
     * Checks if a targetDate is >= startDate and <= endDate
     */
    fun isDateInRange(targetDate: String, startDate: String, endDate: String): Boolean {
        val target = parseDate(targetDate).timeInMillis
        val start = parseDate(startDate).timeInMillis
        val end = parseDate(endDate).timeInMillis
        
        return target in start..end
    }

    private fun parseDate(dateStr: String): Calendar {
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) throw Exception("Invalid format")
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, parts[0].toInt())
            cal.set(Calendar.MONTH, parts[1].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            return cal
        } catch (e: Exception) {
            return Calendar.getInstance()
        }
    }
}
