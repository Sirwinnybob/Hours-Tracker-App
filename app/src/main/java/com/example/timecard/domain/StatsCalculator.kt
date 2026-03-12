package com.example.timecard.domain

import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.TimecardData

data class StatsResult(
    val totalHours: Double,
    val shopHours: Double,
    val jobMap: Map<String, Double>,
    val dailyTotals: Map<String, Double>,
    val weekCount: Int
)

data class JobSearchResult(
    val date: String,
    val hours: Double,
    val dailyBreakdown: Map<String, Double>
)

object StatsCalculator {

    fun calculateStats(weeks: List<TimecardData>): StatsResult {
        var totalHours = 0.0
        var shopHours = 0.0
        val jobMap = mutableMapOf<String, Double>()
        val dailyTotals = DAYS.associateWith { 0.0 }.toMutableMap()
        var weekCount = 0

        for (data in weeks) {
            weekCount++
            for (row in data.rows) {
                val job = row.job.trim()
                if (job.isEmpty()) continue
                val rowTotal = DAYS.sumOf { day ->
                    try {
                        row.getHours(day).toDouble()
                    } catch (e: Exception) {
                        0.0
                    }
                }
                totalHours += rowTotal
                if (job.uppercase() == "SHOP") shopHours += rowTotal
                jobMap[job] = (jobMap[job] ?: 0.0) + rowTotal
                DAYS.forEach { day ->
                    val v = try {
                        row.getHours(day).toDouble()
                    } catch (e: Exception) {
                        0.0
                    }
                    dailyTotals[day] = (dailyTotals[day] ?: 0.0) + v
                }
            }
        }

        return StatsResult(totalHours, shopHours, jobMap, dailyTotals, weekCount)
    }

    fun searchJob(
        query: String,
        employeeName: String,
        activeWeekDate: String,
        isViewingPrevious: Boolean,
        currentData: TimecardData?,
        loadFile: (String, String) -> String?
    ): Pair<List<JobSearchResult>, Double> {
        val results = mutableListOf<JobSearchResult>()
        var totalHours = 0.0

        for (i in 0 until 52) {
            val date = if (i == 0) activeWeekDate else DateUtils.getMondayNWeeksAgo(activeWeekDate, i)
            val data: TimecardData? = if (i == 0 && !isViewingPrevious) {
                currentData
            } else {
                val json = loadFile(employeeName, date) ?: continue
                try {
                    com.google.gson.Gson().fromJson(json, TimecardData::class.java)
                } catch (e: Exception) {
                    null
                }
            }

            if (data == null) continue

            var weekHours = 0.0
            val dailyBreakdown = mutableMapOf<String, Double>()

            for (row in data.rows) {
                val job = row.job.trim()
                if (job.equals(query, ignoreCase = true)) {
                    DAYS.forEach { day ->
                        val v = try {
                            row.getHours(day).toDouble()
                        } catch (e: Exception) {
                            0.0
                        }
                        weekHours += v
                        dailyBreakdown[day] = (dailyBreakdown[day] ?: 0.0) + v
                    }
                }
            }

            if (weekHours > 0) {
                results.add(JobSearchResult(date, weekHours, dailyBreakdown))
                totalHours += weekHours
            }
        }

        return Pair(results, totalHours)
    }
}
