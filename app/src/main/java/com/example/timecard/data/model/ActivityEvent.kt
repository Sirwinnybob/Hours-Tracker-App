package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class ActivityEvent(
    @SerializedName("type") val type: String,           // badge_earned | streak_milestone | record_broken | coins_earned
    @SerializedName("employeeName") val employeeName: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("detail") val detail: String,       // e.g. badge name, "10-day streak", "46.00 hrs"
    @SerializedName("detailIcon") val detailIcon: String,
    @SerializedName("timestamp") val timestamp: String  // ISO 8601
)

data class ActivityFeed(
    @SerializedName("events") val events: List<ActivityEvent> = emptyList()
)
