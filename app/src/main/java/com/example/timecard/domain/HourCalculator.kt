package com.example.timecard.domain

import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.TimecardRow

object HourCalculator {

    fun calcRowTotal(row: TimecardRow): Double {
        return DAYS.sumOf { day ->
            val raw = try {
                row.getHours(day).toDouble()
            } catch (e: Exception) {
                0.0
            }
            Math.round(raw * 4.0) / 4.0
        }
    }

    fun calcGrandTotal(rows: List<TimecardRow>): Double {
        return rows.sumOf { calcRowTotal(it) }
    }

    fun calcDailyTotals(rows: List<TimecardRow>): Map<String, Double> {
        return DAYS.associateWith { day ->
            rows.sumOf { row ->
                val raw = try {
                    row.getHours(day).toDouble()
                } catch (e: Exception) {
                    0.0
                }
                Math.round(raw * 4.0) / 4.0
            }
        }
    }

    fun getJobTotals(rows: List<TimecardRow>): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        for (row in rows) {
            val job = row.job.trim()
            if (job.isEmpty()) continue
            val total = calcRowTotal(row)
            if (total > 0) {
                map[job] = (map[job] ?: 0.0) + total
            }
        }
        return map
    }
}
