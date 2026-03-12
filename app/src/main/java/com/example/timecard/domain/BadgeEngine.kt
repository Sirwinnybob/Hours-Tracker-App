package com.example.timecard.domain

import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData

data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val flavorText: String,
    val coinReward: Int = 50,
    val repeatable: Boolean = false,
    // For admin-defined custom badges:
    val trigger: String = "",       // trigger type key
    val threshold: Double = 0.0,    // numeric threshold
    val imagePath: String? = null   // optional server image, e.g. ".badge_images/clock_puncher.png"
)

object BadgeEngine {

    private val BUILT_IN_FALLBACK = listOf(
        BadgeDefinition("clock_puncher",    "Clock Puncher",      "First timecard saved",                          "⏰", "Welcome to the crew.",       coinReward = 10,  repeatable = false),
        BadgeDefinition("speed_logger",     "Speed Logger",       "Saved a timecard on Monday before 9:30 AM",     "⚡", "First one in.",               coinReward = 10,  repeatable = true),
        BadgeDefinition("job_hopper",       "Job Hopper",         "5+ different jobs in a single day",             "🤸", "Everywhere at once.",         coinReward = 15,  repeatable = true),
        BadgeDefinition("consistent",       "Consistent",         "4 complete weeks in a row",                     "📆", "Rain or shine.",              coinReward = 50,  repeatable = false),
        BadgeDefinition("perfect_week",     "Perfect Week",       "Hit target hours every day Mon-Fri",            "✅", "Not a minute wasted.",        coinReward = 0,   repeatable = true),
        BadgeDefinition("shop_king",        "Shop King",          "200+ total SHOP hours logged",                  "👑", "The shop runs through you.",  coinReward = 75,  repeatable = false),
        BadgeDefinition("overtime_warrior", "Overtime Warrior",   "50+ hours in one week",                         "💪", "You don't stop.",             coinReward = 20,  repeatable = true),
        BadgeDefinition("saturday_warrior", "Saturday Warrior",   "Logged Saturday hours 10+ times",               "🗓️", "Weekend? What weekend?",      coinReward = 50,  repeatable = false),
        BadgeDefinition("delivery_king",    "Delivery King",      "Logged hours on 10+ different delivery jobs",   "🚚", "Always moving.",              coinReward = 100, repeatable = false),
        BadgeDefinition("alert_responder",  "Alert Responder",    "Acknowledged 10+ alerts",                       "📬", "Stays in the loop.",          coinReward = 25,  repeatable = false),
    )

    // Loaded from server badges_config.json. Replaces all definitions when set.
    private var _loadedBadges: List<BadgeDefinition>? = null

    /** All badges: server-loaded definitions, or built-in fallback if not loaded yet. */
    val ALL_BADGES: List<BadgeDefinition>
        get() = _loadedBadges ?: BUILT_IN_FALLBACK

    /**
     * Called during app init with ALL badge definitions from the server's badges_config.json.
     * Replaces the entire in-memory badge list (built-ins + custom).
     */
    fun loadBadgesConfig(definitions: List<BadgeDefinition>) {
        _loadedBadges = definitions
    }

    fun getDefinition(id: String): BadgeDefinition? = ALL_BADGES.find { it.id == id }

    private fun isPerfectWeek(weekData: TimecardData): Boolean =
        GamificationConfig.DAY_TARGETS.indices.all { i ->
            weekData.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
        }

    /**
     * Returns a map of badge ID → count newly earned this save.
     * One-time badges: only awarded if not previously earned (existingBadges count == 0).
     * Repeatable badges: awarded once per qualifying event (multiple per save if applicable).
     *
     * @param existingBadges current badge counts from profile.badges
     * @param profile        current player profile (pre-save state with updated runningStats)
     * @param weekData       the week just saved
     * @param allWeekData    recent weeks (current + up to 7 previous), sorted newest first
     * @param isMonday       whether today is Monday
     * @param isBefore930    whether the current time is before 9:30 AM
     */
    fun checkBadges(
        existingBadges: Map<String, Int>,
        profile: PlayerProfile,
        weekData: TimecardData,
        allWeekData: List<TimecardData>,
        isMonday: Boolean,
        isBefore930: Boolean
    ): Map<String, Int> {
        val earned = mutableMapOf<String, Int>()

        fun award(id: String, count: Int = 1) {
            val def = getDefinition(id) ?: return
            if (!def.repeatable && (existingBadges[id] ?: 0) > 0) return
            earned[id] = (earned[id] ?: 0) + count
        }

        val weekTotal = weekData.rows.sumOf { row ->
            DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
        }
// Clock Puncher — first ever save (one-time)
        award("clock_puncher")

        // Speed Logger — Monday before 9:30 AM (repeatable)
        if (isMonday && isBefore930) award("speed_logger")

        // Job Hopper — 5+ different numeric jobs in a single day (repeatable, per qualifying day)
        var jobHopperCount = 0
        for (dayKey in DAYS.take(5)) {
            val jobsWithHours = weekData.rows
                .filter { row ->
                    val h = row.getHours(dayKey).toDoubleOrNull() ?: 0.0
                    h > 0 && row.job.isNotBlank() && row.job.uppercase() !in GamificationConfig.SPECIAL_JOBS
                }
                .map { it.job.uppercase() }
                .toSet()
            if (jobsWithHours.size >= 5) jobHopperCount++
        }
        if (jobHopperCount > 0) award("job_hopper", jobHopperCount)

        // Perfect Week — all Mon-Fri hit targets (repeatable)
        if (isPerfectWeek(weekData)) award("perfect_week")

        // Overtime Warrior — 50+ hours this week (repeatable)
        if (weekTotal >= 50.0) award("overtime_warrior")

        // Shop King — 200+ total SHOP hours (one-time)
        if (profile.runningStats.totalShopHours >= 200.0) award("shop_king")

        // Saturday Warrior — 10+ Saturdays with hours (one-time)
        if (profile.runningStats.saturdayWeeksCount >= 10) award("saturday_warrior")

// Delivery King — 10+ distinct delivery jobs (one-time)
        if (profile.runningStats.deliveryJobsSeen.size >= 10) award("delivery_king")

        // Alert Responder — 10+ alerts acknowledged (one-time)
        if (profile.runningStats.alertsAcknowledged >= 10) award("alert_responder")

        // Consistent — 4 consecutive complete weeks (one-time)
        if (allWeekData.size >= 4) {
            val allComplete = allWeekData.take(4).all { w ->
                GamificationConfig.DAY_TARGETS.indices.all { i ->
                    w.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
                }
            }
            if (allComplete) award("consistent")
        }

        // Server-defined badges with triggers (any badge not in the built-in ID set)
        val builtInIds = setOf(
            "clock_puncher", "speed_logger", "job_hopper", "consistent", "perfect_week",
            "shop_king", "overtime_warrior", "saturday_warrior", "delivery_king", "alert_responder"
        )
        val thisMonth = weekData.weekStarting.substring(0, 7)
        val monthTotal = allWeekData
            .filter { it.weekStarting.substring(0, 7) == thisMonth }
            .sumOf { w -> w.rows.sumOf { row -> DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } } }

        ALL_BADGES.filter { it.trigger.isNotEmpty() && it.id !in builtInIds }.forEach { def ->
            val count = checkCustomTrigger(
                def, profile, weekData, allWeekData, weekTotal, monthTotal, isMonday, isBefore930
            )
            if (count > 0) award(def.id, count)
        }

        return earned
    }

    private fun checkCustomTrigger(
        def: BadgeDefinition,
        profile: PlayerProfile,
        weekData: TimecardData,
        allWeekData: List<TimecardData>,
        weekTotal: Double,
        monthTotal: Double,
        isMonday: Boolean,
        isBefore930: Boolean
    ): Int = when (def.trigger) {
        "first_save"                  -> 1
        "monday_morning"              -> if (isMonday && isBefore930) 1 else 0
        "week_hours_over"             -> if (weekTotal >= def.threshold) 1 else 0
        "perfect_week"                -> if (isPerfectWeek(weekData)) 1 else 0
        "total_shop_hours_over"       -> if (profile.runningStats.totalShopHours >= def.threshold) 1 else 0
        "saturday_count_over"         -> if (profile.runningStats.saturdayWeeksCount >= def.threshold) 1 else 0
        "total_alerts_over"           -> if (profile.runningStats.alertsAcknowledged >= def.threshold) 1 else 0
        "month_hours_over"            -> if (monthTotal >= def.threshold) 1 else 0
        "distinct_delivery_jobs_over" -> if (profile.runningStats.deliveryJobsSeen.size >= def.threshold) 1 else 0
        "consecutive_complete_weeks"  -> {
            val n = def.threshold.toInt()
            if (allWeekData.size >= n && allWeekData.take(n).all { w ->
                GamificationConfig.DAY_TARGETS.indices.all { i ->
                    w.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
                }
            }) 1 else 0
        }
        else -> 0
    }
}
