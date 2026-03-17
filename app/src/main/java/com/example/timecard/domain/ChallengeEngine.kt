package com.example.timecard.domain

import com.example.timecard.data.model.Challenge
import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData

object ChallengeEngine {

    /** Returns a 0.0–1.0 progress value for the given challenge against the current week's timecard. */
    fun computeProgress(challenge: Challenge, timecard: TimecardData): Double {
        val weekdays = listOf("mon", "tue", "wed", "thu", "fri")
        val allDays  = DAYS

        return when (challenge.type) {
            "min_hours" -> {
                val total = allDays.sumOf { day ->
                    timecard.rows.sumOf { row -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
                }
                if (challenge.target <= 0) 0.0 else (total / challenge.target).coerceIn(0.0, 1.0)
            }
            "all_weekdays" -> {
                val daysWorked = weekdays.count { day ->
                    timecard.rows.sumOf { row -> row.getHours(day).toDoubleOrNull() ?: 0.0 } > 0.0
                }
                (daysWorked / 5.0).coerceIn(0.0, 1.0)
            }
            "saturday_logged" -> {
                val satHours = timecard.rows.sumOf { row -> row.getHours("sat").toDoubleOrNull() ?: 0.0 }
                if (satHours > 0.0) 1.0 else 0.0
            }
            "min_days" -> {
                val daysWorked = allDays.count { day ->
                    timecard.rows.sumOf { row -> row.getHours(day).toDoubleOrNull() ?: 0.0 } > 0.0
                }.toDouble()
                if (challenge.target <= 0) 0.0 else (daysWorked / challenge.target).coerceIn(0.0, 1.0)
            }
            else -> 0.0
        }
    }

    /**
     * Returns the subset of challenges that are newly completed this save.
     * A challenge is newly completed when:
     *   - progress == 1.0
     *   - its log key ("challengeId_weekStarting") is NOT already present in profile.challengeLog
     */
    fun detectCompletions(
        challenges: List<Challenge>,
        timecard: TimecardData,
        profile: PlayerProfile
    ): List<Challenge> {
        val weekStart = timecard.weekStarting
        return challenges.filter { challenge ->
            val logKey = "${challenge.id}_$weekStart"
            val alreadyDone = profile.challengeLog.containsKey(logKey)
            val complete = computeProgress(challenge, timecard) >= 1.0
            complete && !alreadyDone
        }
    }
}
