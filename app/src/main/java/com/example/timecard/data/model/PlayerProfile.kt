package com.example.timecard.data.model

import com.google.gson.annotations.SerializedName

data class CoinLogEntry(
    @SerializedName("savedAt") val savedAt: String = "",
    @SerializedName("hoursLogged") val hoursLogged: Double = 0.0
)

data class StreakData(
    @SerializedName("currentDaily") val currentDaily: Int = 0,
    @SerializedName("bestDaily") val bestDaily: Int = 0,
    @SerializedName("currentWeekly") val currentWeekly: Int = 0,
    @SerializedName("bestWeekly") val bestWeekly: Int = 0
)

data class RecordData(
    @SerializedName("bestWeekHours") val bestWeekHours: Double = 0.0,
    @SerializedName("bestMonthHours") val bestMonthHours: Double = 0.0,
    @SerializedName("busiestDay") val busiestDay: Double = 0.0,
    @SerializedName("favoriteJob") val favoriteJob: String = ""
)

data class RunningStats(
    @SerializedName("totalShopHours") val totalShopHours: Double = 0.0,
    @SerializedName("deliveryJobsSeen") val deliveryJobsSeen: List<String> = emptyList(),
    @SerializedName("saturdayWeeksCount") val saturdayWeeksCount: Int = 0,
    @SerializedName("alertsAcknowledged") val alertsAcknowledged: Int = 0,
    @SerializedName("processedWeeks") val processedWeeks: List<String> = emptyList(),
    /** Set to true after one-time historical backfill completes. */
    @SerializedName("backfillComplete") val backfillComplete: Boolean = false
)

data class PurchaseRecord(
    @SerializedName("itemId")      val itemId: String = "",
    @SerializedName("itemTitle")   val itemTitle: String = "",
    @SerializedName("price")       val price: Int = 0,
    @SerializedName("purchasedAt") val purchasedAt: String = ""
)

data class PlayerProfile(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("coins") val coins: Int = 0,
    @SerializedName("accentColor") val accentColor: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("badges") val badges: Map<String, Int> = emptyMap(),
    @SerializedName("streaks") val streaks: StreakData = StreakData(),
    @SerializedName("records") val records: RecordData = RecordData(),
    @SerializedName("runningStats") val runningStats: RunningStats = RunningStats(),
    @SerializedName("grantedBadges") val grantedBadges: List<String> = emptyList(),
    @SerializedName("coinLog") val coinLog: Map<String, CoinLogEntry> = emptyMap(),
    @SerializedName("inventory") val inventory: List<String> = emptyList(),
    // Tracks which bonuses/badges were already awarded per week (weekStarting → bonusId → count).
    // Prevents repeatable awards from firing on every save while conditions remain met.
    @SerializedName("weeklyBonusLog") val weeklyBonusLog: Map<String, Map<String, Int>> = emptyMap(),
    /** IDs of special shop items this user has already seen (banner won't re-trigger). */
    @SerializedName("seenSpecialItems") val seenSpecialItems: List<String> = emptyList(),
    /** Full purchase history for this employee. */
    @SerializedName("purchaseHistory") val purchaseHistory: List<PurchaseRecord> = emptyList()
)
