package com.example.timecard.domain

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
    val appliedStreakMultiplier: Double = 1.0
)

object GamificationEngine {

    fun processTimecardSave(
        current: PlayerProfile,
        weekData: TimecardData,
        monthWeeks: List<TimecardData>,
        recentWeeks: List<TimecardData>,
        isMonday: Boolean,
        isBefore930: Boolean
    ): GamificationResult {
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
        val weekParts = weekData.weekStarting.split("-")
        for (dayIndex in 0..4) { // Mon-Fri only
            val dayKey = DAYS[dayIndex]
            val dayTotal = weekData.rows.sumOf { row ->
                row.getHours(dayKey).toDoubleOrNull() ?: 0.0
            }
            if (dayTotal <= 0) continue

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

            if (existing == null) {
                val baseCoin = (dayTotal * baseMultiplier).toInt()
                val totalCoin = (dayTotal * totalMultiplier).toInt()
                coinsGained += totalCoin
                streakBonusCoins += (totalCoin - baseCoin)
                coinLog[dayDateStr] = CoinLogEntry(savedAt = now, hoursLogged = dayTotal)
            } else if (dayTotal > existing.hoursLogged + 0.24) {
                val delta = dayTotal - existing.hoursLogged
                val baseCoin = (delta * baseMultiplier).toInt()
                val totalCoin = (delta * totalMultiplier).toInt()
                coinsGained += totalCoin
                streakBonusCoins += (totalCoin - baseCoin)
                coinLog[dayDateStr] = existing.copy(hoursLogged = dayTotal)
            }
        }

        // --- 2. Running stats (avoid double-counting per week) ---
        val isNewWeek = weekKey !in current.runningStats.processedWeeks

        var newStats = current.runningStats
        if (isNewWeek) {
            val shopHoursThisWeek = weekData.rows
                .filter { it.job.uppercase() == "SHOP" }
                .sumOf { row -> DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }

            val hasSaturday = weekData.rows.sumOf { row ->
                row.getHours("sat").toDoubleOrNull() ?: 0.0
            } > 0

            val deliveryJobsThisWeek = weekData.rows
                .filter { JobValidator.isDeliveryJob(it.job) }
                .map { it.job.uppercase() }
                .toSet()

            newStats = current.runningStats.copy(
                totalShopHours = current.runningStats.totalShopHours + shopHoursThisWeek,
                deliveryJobsSeen = (current.runningStats.deliveryJobsSeen + deliveryJobsThisWeek).distinct(),
                saturdayWeeksCount = current.runningStats.saturdayWeeksCount + (if (hasSaturday) 1 else 0),
                processedWeeks = current.runningStats.processedWeeks + weekKey
            )
        }

        // --- 3. Records ---
        val weekTotal = weekData.rows.sumOf { row ->
            DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
        }
        val busiestDay = DAYS.maxOfOrNull { day ->
            weekData.rows.sumOf { row -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
        } ?: 0.0
        val topJob = weekData.rows
            .associate { row -> row.job to DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }
            .filter { it.key.isNotBlank() }
            .maxByOrNull { it.value }?.key ?: current.records.favoriteJob

        var newRecords = current.records
        var recordMsg: String? = null
        if (weekTotal > 0 && weekTotal > newRecords.bestWeekHours) {
            newRecords = newRecords.copy(bestWeekHours = weekTotal)
            recordMsg = "New best week: ${String.format(java.util.Locale.US, "%.2f", weekTotal)} hrs! \uD83C\uDFC5"
            coinsGained += 25
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
            weekData.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
        }
        if (isPerfect && (alreadyAwarded["perfect_week_bonus"] ?: 0) == 0) {
            coinsGained += 30
            alreadyAwarded["perfect_week_bonus"] = 1
        }

        // --- 6. Assemble pre-badge profile ---
        val preBadge = current.copy(
            coins = current.coins + coinsGained,
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
            isBefore930 = isBefore930
        )

        // Filter repeatable badges against this week's bonus log to prevent re-awarding
        // on every save while conditions remain met. One-time badges are already protected
        // by the existingBadges count check inside BadgeEngine.award().
        val newBadgeMap = rawBadgeMap.toMutableMap()
        val repeatablePerWeek = setOf("perfect_week", "speed_logger", "overtime_warrior")
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
            badges = updatedBadges,
            weeklyBonusLog = weeklyBonusLog
        )

        return GamificationResult(
            profile = updatedProfile,
            pendingBadges = newBadgeMap.flatMap { (id, count) -> List(count) { id } },
            newRecordMessage = recordMsg,
            pendingConfetti = isPerfect,
            coinsGainedThisSave = coinsGained + badgeCoins,
            streakBonusCoins = streakBonusCoins,
            appliedStreakMultiplier = GamificationConfig.streakMultiplier(newStreaks.currentDaily)
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
        if (allWeeks.isEmpty()) {
            return currentProfile.copy(
                runningStats = currentProfile.runningStats.copy(backfillComplete = true)
            )
        }

        val now = Instant.now().toString()
        var coinsGained = 0
        val coinLog = mutableMapOf<String, CoinLogEntry>()
        val weeklyBonusLog = mutableMapOf<String, MutableMap<String, Int>>()
        var runStats = RunningStats(backfillComplete = true)
        var records = RecordData()
        val earnedBadges = mutableMapOf<String, Int>()
        val monthTotals = mutableMapOf<String, Double>()

        // --- Streaks from full history ---
        // Use computeStreaks (the same function as live saves) so that bestDaily/bestWeekly
        // are derived from the full historical record, not just set equal to currentDaily.
        val sortedDesc = allWeeks.sortedByDescending { it.weekStarting }
        // Backfill runs before current coinLog is computed, we can just use emptyMap
        // for historical streaks, or better yet, since it's just generating the initial
        // state, we don't know when they were "saved". Backfilling historical entries
        // doesn't penalize for "missed and then recorded" because backfill is just
        // processing a legacy log. We will pass emptyMap() to let it use strict logic,
        // or actually pass coinLog from currentProfile just in case it's an incremental backfill.
        val newStreaks = computeStreaks(StreakData(), sortedDesc, currentProfile.coinLog)

        // Iterate oldest-to-newest so the streak organically builds
        val sortedAsc = allWeeks.sortedBy { it.weekStarting }
        
        for (week in sortedAsc) {
            val weekParts = week.weekStarting.split("-")

            // Determine what the pseudo-streak was for this historical week
            // Note: Since backfill is just an initial state generation, we 
            // compute the final full streak logic below anyway. To prevent infinite 
            // loops looking back historically, we'll apply a simpler historical
            // scaling based on the final achieved streak for backwards-compatibility 
            // without complex state reconstruction loops.
            val historicalStreakScale = GamificationConfig.streakMultiplier(newStreaks.currentDaily)

            // --- Per-day coins at 85% ---
            for (dayIndex in 0..4) {
                val dayKey = DAYS[dayIndex]
                val dayTotal = week.rows.sumOf { row -> row.getHours(dayKey).toDoubleOrNull() ?: 0.0 }
                if (dayTotal <= 0) continue

                val dayCal = Calendar.getInstance()
                dayCal.set(weekParts[0].toInt(), weekParts[1].toInt() - 1, weekParts[2].toInt())
                dayCal.add(Calendar.DAY_OF_MONTH, dayIndex)
                val dayDateStr = String.format(java.util.Locale.US,
                    "%04d-%02d-%02d",
                    dayCal.get(Calendar.YEAR),
                    dayCal.get(Calendar.MONTH) + 1,
                    dayCal.get(Calendar.DAY_OF_MONTH)
                )
                if (dayDateStr !in coinLog) {
                    val finalVal = dayTotal * GamificationConfig.BACKFILL_RATE * historicalStreakScale
                    coinsGained += finalVal.toInt()
                    coinLog[dayDateStr] = CoinLogEntry(savedAt = now, hoursLogged = dayTotal)
                }
            }

            // --- Running stats ---
            val weekKey = week.weekStarting
            if (weekKey !in runStats.processedWeeks) {
                val shopH = week.rows.filter { it.job.uppercase() == "SHOP" }
                    .sumOf { row -> DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }
                val hasSat = week.rows.sumOf { row -> row.getHours("sat").toDoubleOrNull() ?: 0.0 } > 0
                val delivJobs = week.rows.filter { JobValidator.isDeliveryJob(it.job) }
                    .map { it.job.uppercase() }.toSet()
                runStats = runStats.copy(
                    totalShopHours = runStats.totalShopHours + shopH,
                    deliveryJobsSeen = (runStats.deliveryJobsSeen + delivJobs).distinct(),
                    saturdayWeeksCount = runStats.saturdayWeeksCount + (if (hasSat) 1 else 0),
                    processedWeeks = runStats.processedWeeks + weekKey
                )
            }

            // --- Records ---
            val weekTotal = week.rows.sumOf { row -> DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }
            val busiestDayVal = DAYS.maxOfOrNull { day ->
                week.rows.sumOf { row -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
            } ?: 0.0
            val topJob = week.rows
                .associate { row -> row.job to DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 } }
                .filter { it.key.isNotBlank() }.maxByOrNull { it.value }?.key ?: ""

            if (weekTotal > records.bestWeekHours) {
                records = records.copy(bestWeekHours = weekTotal)
                coinsGained += 25
            }
            if (busiestDayVal > records.busiestDay) records = records.copy(busiestDay = busiestDayVal)
            if (topJob.isNotBlank()) records = records.copy(favoriteJob = topJob)

            // Month total for Century
            val month = weekKey.substring(0, 7)
            monthTotals[month] = (monthTotals[month] ?: 0.0) + weekTotal

            // --- Repeatable badges per week ---
            val isPerfect = GamificationConfig.DAY_TARGETS.indices.all { i ->
                week.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
            }
            val weekBonuses = weeklyBonusLog.getOrPut(weekKey) { mutableMapOf() }
            if (isPerfect) {
                earnedBadges["perfect_week"] = (earnedBadges["perfect_week"] ?: 0) + 1
                coinsGained += 30
                weekBonuses["perfect_week_bonus"] = 1
                weekBonuses["perfect_week"] = 1
            }
            if (weekTotal >= 50.0) {
                earnedBadges["overtime_warrior"] = (earnedBadges["overtime_warrior"] ?: 0) + 1
                weekBonuses["overtime_warrior"] = 1
            }
            // Job hopper — per qualifying day
            var jobHopperCount = 0
            for (dayKey in DAYS.take(5)) {
                val jobsWithHours = week.rows.filter { row ->
                    val h = row.getHours(dayKey).toDoubleOrNull() ?: 0.0
                    h > 0 && row.job.isNotBlank() && row.job.uppercase() !in GamificationConfig.SPECIAL_JOBS
                }.map { it.job.uppercase() }.toSet()
                if (jobsWithHours.size >= 5) {
                    earnedBadges["job_hopper"] = (earnedBadges["job_hopper"] ?: 0) + 1
                    jobHopperCount++
                }
            }
            if (jobHopperCount > 0) weekBonuses["job_hopper"] = jobHopperCount
        }

        // --- Best month record ---
        val bestMonth = monthTotals.values.maxOrNull() ?: 0.0
        records = records.copy(bestMonthHours = bestMonth)

        // --- One-time milestone badges ---
        if (allWeeks.isNotEmpty()) earnedBadges["clock_puncher"] = 1
        if (runStats.totalShopHours >= 200.0) earnedBadges["shop_king"] = 1
        if (runStats.saturdayWeeksCount >= 10) earnedBadges["saturday_warrior"] = 1
        if (runStats.deliveryJobsSeen.size >= 10) earnedBadges["delivery_king"] = 1

        // Consistent — 4 consecutive complete weeks (check in most recent 4)
        if (sortedDesc.size >= 4 && sortedDesc.take(4).all { w ->
            GamificationConfig.DAY_TARGETS.indices.all { i ->
                w.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } >= GamificationConfig.DAY_TARGETS[i]
            }
        }) earnedBadges["consistent"] = 1

        // --- Badge coins ---
        val badgeCoins = earnedBadges.entries.sumOf { (id, count) ->
            (BadgeEngine.getDefinition(id)?.coinReward ?: 10) * count
        }
        val finalCoins = coinsGained + badgeCoins

        return PlayerProfile(
            displayName = currentProfile.displayName,
            accentColor = currentProfile.accentColor,
            grantedBadges = currentProfile.grantedBadges,
            inventory = currentProfile.inventory,
            coins = finalCoins,
            badges = earnedBadges,
            streaks = newStreaks,
            records = records,
            runningStats = runStats,
            coinLog = coinLog,
            weeklyBonusLog = weeklyBonusLog
        )
    }

    private fun computeBackfillStreaks(sortedDesc: List<TimecardData>): StreakData {
        val cal = Calendar.getInstance()
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)
        val todayIdx = if (todayDow == Calendar.SUNDAY) 6 else todayDow - Calendar.MONDAY

        var daily = 0
        outer@ for (i in sortedDesc.indices) {
            val week = sortedDesc[i]
            val maxDay = if (i == 0) minOf(todayIdx, 4) else 4
            for (dayIdx in maxDay downTo 0) {
                val total = week.rows.sumOf { row -> row.getHours(DAYS[dayIdx]).toDoubleOrNull() ?: 0.0 }
                val isExcused = week.rows.any { row ->
                    val h = row.getHours(DAYS[dayIdx]).toDoubleOrNull() ?: 0.0
                    h > 0 && row.job.uppercase() in setOf("HOLIDAY", "VACATION", "SICK", "PERSONAL", "PTO")
                }

                if (total >= GamificationConfig.DAY_TARGETS[dayIdx]) {
                    daily++
                } else if (isExcused) {
                    // Excused absence preserves streak
                } else if (i == 0 && dayIdx == todayIdx) {
                    // Today's hours aren't entered yet — skip without breaking streak
                } else {
                    break@outer
                }
            }
        }
        var weekly = 0
        for (week in sortedDesc) {
            val complete = (0..4).all { i ->
                week.rows.sumOf { row -> row.getHours(DAYS[i]).toDoubleOrNull() ?: 0.0 } > 0
            }
            if (complete) weekly++ else break
        }
        return StreakData(currentDaily = daily, bestDaily = daily, currentWeekly = weekly, bestWeekly = weekly)
    }
}
