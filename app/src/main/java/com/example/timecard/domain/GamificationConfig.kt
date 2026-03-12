package com.example.timecard.domain

object GamificationConfig {
    const val BACKFILL_RATE = 0.85
    
    // Day targets for Perfect Week: Mon-Thu = 9h, Fri = 4h
    val DAY_TARGETS = listOf(9.0, 9.0, 9.0, 9.0, 4.0)

    // Targets for streak calculations, including Saturday (index 5)
    val STREAK_TARGETS = listOf(9.0, 9.0, 9.0, 9.0, 4.0, 4.0)

    // Jobs that do not count towards Job Hopper or are treated specially
    val SPECIAL_JOBS = setOf("SHOP", "HOLIDAY", "VACATION", "SICK", "PERSONAL", "DELIVERY")

    // Provides a base multiplier, can be used for holidays or other static dates
    fun coinMultiplier(dateStr: String): Double {
        return 1.0
    }

    // Provides an accelerating return based on consecutive days worked
    // Starts at 1.0x, grows slowly early on, ramps up for long streaks.
    // E.g., Streak 0 = 1.0x, Streak 5 = 1.25x, Streak 20 = 2.0x
    fun streakMultiplier(currentDailyStreak: Int): Double {
        if (currentDailyStreak <= 0) return 1.0
        // Cap the multiplier eventually so it doesn't break the economy completely
        val maxStreak = 100 // 100 days = max multiplier
        val cappedStreak = currentDailyStreak.coerceAtMost(maxStreak)
        val calculatedMultiplier = 1.0 + (cappedStreak * 0.025)
        return calculatedMultiplier.coerceAtMost(2.0)
    }
}
