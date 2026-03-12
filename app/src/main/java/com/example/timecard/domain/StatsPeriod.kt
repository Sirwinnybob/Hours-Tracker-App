package com.example.timecard.domain

sealed class StatsPeriod {
    data object ThisWeek : StatsPeriod()
    data object LastWeek : StatsPeriod()
    data object TwoWeeks : StatsPeriod()
    data object ThisMonth : StatsPeriod()
    data object LastMonth : StatsPeriod()
    data object AllTime : StatsPeriod()
    data class Custom(val startDate: String, val endDate: String) : StatsPeriod()

    // Human-readable labels for the UI
    val label: String
        get() = when (this) {
            is ThisWeek -> "This Week"
            is LastWeek -> "Last Week"
            is TwoWeeks -> "2 Weeks"
            is ThisMonth -> "This Month"
            is LastMonth -> "Last Month"
            is AllTime -> "All Time"
            is Custom -> "Custom Range"
        }
}
