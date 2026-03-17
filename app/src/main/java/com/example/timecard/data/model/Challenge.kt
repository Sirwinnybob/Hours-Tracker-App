package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class Challenge(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String = "🎯",
    @SerializedName("type") val type: String,   // min_hours | all_weekdays | saturday_logged | min_days
    @SerializedName("target") val target: Double = 0.0,
    @SerializedName("reward") val reward: Int = 50
)

data class ChallengeCatalog(
    @SerializedName("challenges") val challenges: List<Challenge> = emptyList()
)
