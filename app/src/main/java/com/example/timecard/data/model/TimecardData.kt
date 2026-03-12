package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class TimecardRow(
    @SerializedName("job") val job: String = "",
    @SerializedName("delivery") val delivery: Boolean = false,
    @SerializedName("mon") val mon: String = "",
    @SerializedName("tue") val tue: String = "",
    @SerializedName("wed") val wed: String = "",
    @SerializedName("thu") val thu: String = "",
    @SerializedName("fri") val fri: String = "",
    @SerializedName("sat") val sat: String = ""
) {
    fun getHours(day: String): String = when (day) {
        "mon" -> mon; "tue" -> tue; "wed" -> wed
        "thu" -> thu; "fri" -> fri; "sat" -> sat
        else -> ""
    }

    fun withHours(day: String, value: String): TimecardRow = when (day) {
        "mon" -> copy(mon = value); "tue" -> copy(tue = value)
        "wed" -> copy(wed = value); "thu" -> copy(thu = value)
        "fri" -> copy(fri = value); "sat" -> copy(sat = value)
        else -> this
    }
}

data class TimecardData(
    @SerializedName("employeeName") val employeeName: String,
    @SerializedName("weekStarting") val weekStarting: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("rows") val rows: List<TimecardRow>
)

val DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat")
val DAY_LABELS = mapOf(
    "mon" to "Mon", "tue" to "Tue", "wed" to "Wed",
    "thu" to "Thu", "fri" to "Fri", "sat" to "Sat"
)
val DAY_LABELS_SHORT = mapOf(
    "mon" to "M", "tue" to "T", "wed" to "W",
    "thu" to "Th", "fri" to "F", "sat" to "S"
)
