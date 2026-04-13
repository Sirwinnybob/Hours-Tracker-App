package com.example.timecard.domain

import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.CoinLogEntry
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.RecordData
import com.example.timecard.data.model.RunningStats
import com.example.timecard.data.model.StreakData
import com.example.timecard.data.model.TimecardData
import java.time.Instant
import java.util.Calendar

data class GamificationResult(
    val profile: PlayerProfile,
    val pendingBadges: List<String>,
    val newRecordMessage: String?,
    val pendingConfetti: Boolean,
    val coinsGainedThisSave: Int,
    val streakBonusCoins: Int = 0,
    val appliedStreakMultiplier: Double = 1.0,
    val newEvents: List<ActivityEvent> = emptyList()
)

object GamificationEngine {

    fun processTimecardSave(
        current: PlayerProfile,
        employeeName: String,
        weekData: TimecardData,
        monthWeeks: List<TimecardData>,
        recentWeeks: List<TimecardData>,
        isMonday: Boolean,
        isBefore930: Boolean
    ): GamificationResult {
        // Pre-calculate daily totals and other row-based stats in a single pass
        val dailyTotals = mutableMapOf<String, Double>()
        val jobTotals = mutableMapOf<String, Double>()
        val dailyJobSets = DAYS.associateWith { mutableSetOf<String>() }
        var shopHoursThisWeek = 0.0
        var hasSaturdayHours = false

        for (row in weekData.rows) {
            val jobUpper = row.job.uppercase()
            val isShop = jobUpper == "SHOP"
            val isSpecial = jobUpper in GamificationConfig.SPECIAL_JOBS

            for (day in DAYS) {
                val hours = row.getHours(day).toDoubleOrNull() ?: 0.0
                if (hours > 0) {
                    dailyTotals[day] = (dailyTotals[day] ?: 0.0) + hours
                    if (row.job.isNotBlank()) {
                        jobTotals[row.job] = (jobTotals[row.job] ?: 0.0) + hours
                        if (!isSpecial) {
                            dailyJobSets[day]?.add(jobUpper)
                        }
                    }
                    if (isShop) shopHoursThisWeek += hours
                    if (day == "sat") hasSaturdayHours = true
                }
            }
        }
        val weekTotal = dailyTotals.values.sum()
        val jobHopperCount = dailyJobSets.filterKeys { it != "sat" }.count { it.value.size >= 5 }

        val now = Instant.now().toString()
        val weekKey = weekData.weekStarting
        val coinLog = current.coinLog.toMutableMap()
        var coinsGained = 0
        var streakBonusCoins = 0

        // Per-week bonus tracking — prevents repeatable awards from firing on every save
        val weeklyBonusLog = current.weeklyBonusLog.toMutableMap()
        val alreadyAwarded: MutableMap<String, Int> =
            weeklyBonusLog.getOrDefault(weekKey, emptyMap()).toMutableMap()

        // Calculate streaks upfront so we can use the daily streak as a coin multiplier multiplier
        val sortedWeeks = recentWeeks.sortedByDescending { it.weekStarting }
        val newStreaks = computeStreaks(current.streaks, sortedWeeks, current.coinLog)

        // --- 1. Coins: per-day timeliness & streaks ---
        val isCurrentWeek = weekData.weekStarting == DateUtils.getWeekStartingMonday()
        val dailyCoinCap = if (isCurrentWeek) 9.0 else 16.0
        val weekParts = weekData.weekStarting.split("-")
        for (dayIndex in 0..4) { // Mon-Fri only
            val dayKey = DAYS[dayIndex]
            val dayTotal = dailyTotals[dayKey] ?: 0.0

            val dayCal = Calendar.getInstance()
            dayCal.set(weekParts[0].toInt(), weekParts[1].toInt() - 1, weekParts[2].toInt())
            dayCal.add(Calendar.DAY_OF_MONTH, dayIndex)
            val dayDateStr = String.format(java.util.Locale.US,
                "%04d-%02d-%02d",
                dayCal.get(Calendar.YEAR),
                dayCal.get(Calendar.MONTH) + 1,
                dayCal.get(Calendar.DAY_OF_MONTH)
            )

            val existing = coinLog[dayDateStr]
            
            // Base timeliness constraint
            val baseMultiplier = GamificationConfig.coinMultiplier(dayDateStr)
            
            // Scale standard rate (1.0) exponentially by the current exact streak.
            val streakMult = GamificationConfig.streakMultiplier(newStreaks.currentDaily)
            val totalMultiplier = baseMultiplier * streakMult

            val cappedNewHours = dayTotal.coerceAtMost(dailyCoinCap)

            if (existing == null) {
                if (dayTotal > 0) {
                    val baseCoin = (cappedNewHours * baseMultiplier).toInt()
                    val totalCoin = (cappedNewHours * totalMultiplier).toInt()
                    coinsGained += totalCoin
                    streakBonusCoins += (totalCoin - baseCoin)
                    coinLog[dayDateStr] = CoinLogEntry(savedAt = now, hoursLogged = dayTotal, paidHours = cappedNewHours)
                }
            } else {
                // Determine previously paid hours. Fall back to hoursLogged if paidHours is not set (legacy data)
                // This correctly accounts for users who were previously over-awarded massive amounts of coins before the cap was implemented.
                val previouslyPaid = if (existing.paidHours > 0.0) existing.paidHours else existing.hoursLogged

                // Allow positive or negative adjustments based on the newly capped hours vs previously paid
                val delta = cappedNewHours - previouslyPaid

                // Only act if there's a meaningful change (either up, or down).
                if (kotlin.math.abs(delta) > 0.24) {
                    var baseCoin = (delta * baseMultiplier).toInt()
                    var totalCoin = (delta * totalMultiplier).toInt()
                    var finalPaidHours = cappedNewHours

                    // SECURE: Prevent negative coins while avoiding duplication loop.
                    // If this deletion would drop the player below 0 coins, we limit the refund
                    // to whatever coins they have left, and we only reduce their 'paidHours'
                    // by the exact amount they were able to refund.
                    val projectedBalance = current.coins + coinsGained + totalCoin
                    if (projectedBalance < 0) {
                        totalCoin = -(current.coins + coinsGained)
                        // Reverse math to find how many hours they actually refunded
                        val actualDelta = if (totalMultiplier > 0) totalCoin / totalMultiplier else 0.0
                        baseCoin = (actualDelta * baseMultiplier).toInt()
                        finalPaidHours = previouslyPaid + actualDelta
                    }

                    coinsGained += totalCoin
                    streakBonusCoins += (totalCoin - baseCoin)
                    coinLog[dayDateStr] = existing.copy(hoursLogged = dayTotal, paidHours = finalPaidHours)
                }
            }
        }

        // --- 2. Running stats (avoid double-counting per week) ---
        val isNewWeek = weekKey !in current.runningStats.processedWeeks

        var newStats = current.runningStats
        if (isNewWeek) {
            val deliveryJobsThisWeek = weekData.rows
                .filter { JobValidator.isDeliveryJob(it.job) }
                .map { it.job.uppercase() }
                .toSet()

            newStats = current.runningStats.copy(
                totalShopHours = current.runningStats.totalShopHours + shopHoursThisWeek,
                deliveryJobsSeen = (current.runningStats.deliveryJobsSeen + deliveryJobsThisWeek).distinct(),
                saturdayWeeksCount = current.runningStats.saturdayWeeksCount + (if (hasSaturdayHours) 1 else 0),
                processedWeeks = current.runningStats.processedWeeks + weekKey
            )
        }

        // --- 3. Records ---
        val busiestDay = DAYS.maxOfOrNull { day -> dailyTotals[day] ?: 0.0 } ?: 0.0
        val topJob = jobTotals.filter { it.key.isNotBlank() }
            .maxByOrNull { it.value }?.key ?: current.records.favoriteJob

        var newRecords = current.records
        var recordMsg: String? = null
        if (weekTotal > 0 && weekTotal > newRecords.bestWeekHours) {
            newRecords = newRecords.copy(bestWeekHours = weekTotal)
            if ((alreadyAwarded["best_week_bonus"] ?: 0) == 0) {
                recordMsg = "New best week: ${String.format(java.util.Locale.US, "%.2f", weekTotal)} hrs! \uD83C\uDFC5"
                coinsGained += 25
                alreadyAwarded["best_week_bonus"] = 1
            }
        }
        if (busiestDay > newRecords.busiestDay) newRecords = newRecords.copy(busiestDay = busiestDay)
        if (topJob.isNotBlank()) newRecords = newRecords.copy(favoriteJob = topJob)

        // --- 3b. Best Month record ---
        val monthTotal = monthWeeks.sumOf { w ->
            w.rows.sumOf { row -> DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }
        }
        if (monthTotal > newRecords.bestMonthHours) {
            newRecords = newRecords.copy(bestMonthHours = monthTotal)
            if (recordMsg == null) recordMsg = "New best month: ${String.format(java.util.Locale.US, "%.2f", monthTotal)} hrs! \uD83D\uDDD3\uFE0F"
        }

        // --- 5. Perfect Week Bonus ---
        val isPerfect = GamificationConfig.DAY_TARGETS.indices.all { i ->
            (dailyTotals[DAYS[i]] ?: 0.0) >= GamificationConfig.DAY_TARGETS[i]
        }
        if (isPerfect && (alreadyAwarded["perfect_week_bonus"] ?: 0) == 0) {
            coinsGained += 30
            alreadyAwarded["perfect_week_bonus"] = 1
        }

        // --- 6. Assemble pre-badge profile ---
        val preBadge = current.copy(
            coins = current.coins + coinsGained,
            allTimeCoinsEarned = current.allTimeCoinsEarned + coinsGained,
            streaks = newStreaks,
            records = newRecords,
            runningStats = newStats,
            coinLog = coinLog,
            weeklyBonusLog = weeklyBonusLog
        )

        // --- 7. Badges (Map<String,Int>: badge ID → count earned this save) ---
        val rawBadgeMap = BadgeEngine.checkBadges(
            existingBadges = current.badges,
            profile = preBadge,
            weekData = weekData,
            allWeekData = sortedWeeks,
            isMonday = isMonday,
            isBefore930 = isBefore930,
            dailyTotals = dailyTotals,
            jobHopperCount = jobHopperCount
        )

        // Filter repeatable badges against this week's bonus log to prevent re-awarding
        // on every save while conditions remain met. One-time badges are already protected
        // by the existingBadges count check inside BadgeEngine.award().
        val newBadgeMap = rawBadgeMap.toMutableMap()
        val repeatablePerWeek = setOf("perfect_week", "speed_logger", "overtime_warrior", "best_week_bonus")
        repeatablePerWeek.forEach { id ->
            if ((newBadgeMap[id] ?: 0) > 0 && (alreadyAwarded[id] ?: 0) > 0) {
                newBadgeMap.remove(id)
            }
        }
        // Job Hopper: award the delta between qualifying days now vs already awarded this week.
        // This allows legitimately earning more if new qualifying days are added mid-week.
        val jobHopperNow = rawBadgeMap["job_hopper"] ?: 0
        val jobHopperPrev = alreadyAwarded["job_hopper"] ?: 0
        val jobHopperDelta = maxOf(0, jobHopperNow - jobHopperPrev)
        if (jobHopperDelta == 0) newBadgeMap.remove("job_hopper")
        else newBadgeMap["job_hopper"] = jobHopperDelta

        // Record newly earned repeatable badges so they won't re-fire this week
        (repeatablePerWeek + "job_hopper").forEach { id ->
            val earned = newBadgeMap[id] ?: 0
            if (earned > 0) alreadyAwarded[id] = (alreadyAwarded[id] ?: 0) + earned
        }
        weeklyBonusLog[weekKey] = alreadyAwarded

        val badgeCoins = newBadgeMap.entries.sumOf { (id, count) ->
            (BadgeEngine.getDefinition(id)?.coinReward ?: 10) * count
        }

        val updatedBadges = current.badges.toMutableMap()
        newBadgeMap.forEach { (id, count) ->
            updatedBadges[id] = (updatedBadges[id] ?: 0) + count
        }

        val updatedProfile = preBadge.copy(
            coins = preBadge.coins + badgeCoins,
            allTimeCoinsEarned = preBadge.allTimeCoinsEarned + badgeCoins,
            badges = updatedBadges,
            weeklyBonusLog = weeklyBonusLog
        )

        // --- Build activity events ---
        val displayName = current.displayName?.takeIf { it.isNotBlank() } ?: employeeName
        val activityEvents = mutableListOf<ActivityEvent>()

        // Badges earned
        newBadgeMap.forEach { (id, count) ->
            val def = BadgeEngine.getDefinition(id)
            if (def != null) {
                repeat(count) {
                    activityEvents += ActivityEvent(
                        type = "badge_earned",
                        employeeName = employeeName,
                        displayName = displayName,
                        detail = def.name,
                        detailIcon = def.emoji,
                        timestamp = now
                    )
                }
            }
        }
        // Streak milestone (multiples of 5)
        val prevStreak = current.streaks.currentDaily
        val newStreak = newStreaks.currentDaily
        if (newStreak > prevStreak && newStreak > 0 && newStreak % 5 == 0) {
            activityEvents += ActivityEvent(
                type = "streak_milestone",
                employeeName = employeeName,
                displayName = displayName,
                detail = "$newStreak days",
                detailIcon = "🔥",
                timestamp = now
            )
        }
        // Record broken (best week)
        if (weekTotal > 0 && weekTotal > current.records.bestWeekHours) {
            activityEvents += ActivityEvent(
                type = "record_broken",
                employeeName = employeeName,
                displayName = displayName,
                detail = String.format(java.util.Locale.US, "%.2f hrs", weekTotal),
                detailIcon = "📈",
                timestamp = now
            )
        }
        // Big coin haul (≥ 100 coins this save)
        val totalCoinsThisSave = coinsGained + badgeCoins
        if (totalCoinsThisSave >= 100) {
            activityEvents += ActivityEvent(
                type = "coins_earned",
                employeeName = employeeName,
                displayName = displayName,
                detail = "$totalCoinsThisSave KK Coins",
                detailIcon = "🪙",
                timestamp = now
            )
        }

        return GamificationResult(
            profile = updatedProfile,
            pendingBadges = newBadgeMap.flatMap { (id, count) -> List(count) { id } },
            newRecordMessage = recordMsg,
            pendingConfetti = isPerfect,
            coinsGainedThisSave = coinsGained + badgeCoins,
            streakBonusCoins = streakBonusCoins,
            appliedStreakMultiplier = GamificationConfig.streakMultiplier(newStreaks.currentDaily),
            newEvents = activityEvents
        )
    }

    fun computeStreaks(
        currentStreaks: StreakData,
        sortedWeeks: List<TimecardData>,
        coinLog: Map<String, CoinLogEntry> = emptyMap()
    ): StreakData {
        if (sortedWeeks.isEmpty()) {
            return StreakData(0, currentStreaks.bestDaily, 0, currentStreaks.bestWeekly)
        }
        
        val weekMap = sortedWeeks.associateBy { it.weekStarting }
        val thisWeekStr = DateUtils.getWeekStartingMonday()
        val todayIdx = DateUtils.getCurrentDayIndex()
        
        // --- 1. Current Streaks (Looking backwards from today) ---
        var currentDaily = 0
        var checkDate = thisWeekStr
        var checkDayIdx = minOf(todayIdx, 5)
        var broken = false
        val oldestWeek = sortedWeeks.last().weekStarting
        
        while (!broken) {
            val week = weekMap[checkDate]
            if (week != null) {
                val total = week.rows.sumOf { row -> row.getHours(DAYS[checkDayIdx]).toDoubleOrNull() ?: 0.0 }
                val isExcused = week.rows.any { row ->
                    val h = row.getHours(DAYS[checkDayIdx]).toDoubleOrNull() ?: 0.0
                    h > 0 && row.job.uppercase() in setOf("HOLIDAY", "VACATION", "SICK", "PERSONAL", "PTO")
                }

                // Check if the day was logged late (backfilled)
                val parts = checkDate.split("-")
                val isBackfilled = if (parts.size == 3 && checkDayIdx <= 4) {
                    val cal = Calendar.getInstance()
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    cal.add(Calendar.DAY_OF_MONTH, checkDayIdx)
                    val dayDateStr = String.format(java.util.Locale.US,
                        "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )

                    val entry = coinLog[dayDateStr]
                    if (entry != null && entry.savedAt.isNotBlank()) {
                        try {
                            val savedInstant = Instant.parse(entry.savedAt)
                            val savedDateStr = String.format(java.util.Locale.US,
                                "%04d-%02d-%02d",
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).year,
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).monthValue,
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                            )
                            // If the date it was saved is later than the date itself, it was backfilled
                            savedDateStr > dayDateStr
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }

                if (total >= GamificationConfig.STREAK_TARGETS[checkDayIdx]) {
                    if (isBackfilled && !isExcused) {
                        // Backfilled regular hours break the streak
                        broken = true
                    } else {
                        currentDaily++
                    }
                } else if (isExcused) {
                    // Excused absences (PTO/Holiday) preserve the streak, but don't add to it
                } else if (checkDate == thisWeekStr && checkDayIdx == todayIdx) {
                    // Today's hours not entered yet — don't break the streak
                } else if (checkDate == thisWeekStr && checkDayIdx > todayIdx) {
                    // Future day this week (shouldn't normally happen, but be safe)
                } else if (checkDayIdx == 5) {
                    // Saturday is optional! Missing it doesn't break a streak, it just doesn't add to it.
                } else {
                    broken = true
                }
            } else {
                if (checkDate == thisWeekStr && checkDayIdx == todayIdx) {
                    // Pending today, skip
                } else if (checkDayIdx == 5) {
                    // Missing Saturday is fine, continue looking backwards
                } else {
                    broken = true
                }
            }
            
            if (!broken) {
                checkDayIdx--
                if (checkDayIdx < 0) {
                    checkDayIdx = 5 // Wrap back to Saturday
                    checkDate = DateUtils.getPreviousMonday(checkDate)
                    if (checkDate < oldestWeek) break
                }
            }
        }
        
        var currentWeekly = 0
        var weekCheck = thisWeekStr
        broken = false
        
        while (!broken) {
            val week = weekMap[weekCheck]
            if (week != null) {
                val complete = (0..4).all { i ->
                    (week.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 }) > 0.0
                }
                if (complete) {
                    currentWeekly++
                } else if (weekCheck == thisWeekStr) {
                    // Pending week, skip
                } else {
                    broken = true
                }
            } else {
                if (weekCheck == thisWeekStr) {
                    // Pending week, skip
                } else {
                    broken = true
                }
            }
            if (!broken) {
                weekCheck = DateUtils.getPreviousMonday(weekCheck)
                if (weekCheck < oldestWeek) break
            }
        }

        // --- 2. Best Streaks (Historical Analysis) ---
        var bestDaily = currentStreaks.bestDaily
        var bestWeekly = currentStreaks.bestWeekly
        
        val sortedAsc = sortedWeeks.sortedBy { it.weekStarting }
        var tempDaily = 0
        var tempWeekly = 0
        var expectedNextWeek: String? = null
        
        for (week in sortedAsc) {
            if (expectedNextWeek != null && week.weekStarting != expectedNextWeek) {
                tempDaily = 0
                tempWeekly = 0
            }
            
            for (i in 0..5) {
                val total = week.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 }
                val isExcused = week.rows.any { row ->
                    val h = row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0
                    h > 0 && row.job.uppercase() in setOf("HOLIDAY", "VACATION", "SICK", "PERSONAL", "PTO")
                }

                val parts = week.weekStarting.split("-")
                val isBackfilled = if (parts.size == 3 && i <= 4) {
                    val cal = Calendar.getInstance()
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    cal.add(Calendar.DAY_OF_MONTH, i)
                    val dayDateStr = String.format(java.util.Locale.US,
                        "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                    )

                    val entry = coinLog[dayDateStr]
                    if (entry != null && entry.savedAt.isNotBlank()) {
                        try {
                            val savedInstant = Instant.parse(entry.savedAt)
                            val savedDateStr = String.format(java.util.Locale.US,
                                "%04d-%02d-%02d",
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).year,
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).monthValue,
                                savedInstant.atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                            )
                            savedDateStr > dayDateStr
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }

                if (total >= GamificationConfig.STREAK_TARGETS[i]) {
                    if (isBackfilled && !isExcused) {
                        tempDaily = 0 // Backfilled regular hours break historical streak too
                    } else {
                        tempDaily++
                        if (tempDaily > bestDaily) bestDaily = tempDaily
                    }
                } else if (isExcused) {
                    // Excused absence preserves tempDaily but doesn't increase it
                } else if (i == 5) {
                    // Saturday is optional. Missing it doesn't break daily streak
                } else {
                    tempDaily = 0
                }
            }
            
            val complete = (0..4).all { i ->
                (week.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 }) > 0.0
            }
            if (complete) {
                tempWeekly++
                if (tempWeekly > bestWeekly) bestWeekly = tempWeekly
            } else {
                tempWeekly = 0
            }
            
            expectedNextWeek = DateUtils.getMondayNWeeksAgo(week.weekStarting, -1)
        }
        
        bestDaily = maxOf(bestDaily, currentDaily)
        bestWeekly = maxOf(bestWeekly, currentWeekly)
        
        return StreakData(currentDaily, bestDaily, currentWeekly, bestWeekly)
    }

    fun runBackfill(currentProfile: PlayerProfile, allWeeks: List<TimecardData>): PlayerProfile {
        return currentProfile.copy(
            runningStats = currentProfile.runningStats.copy(backfillComplete = true)
        )
    }
}
