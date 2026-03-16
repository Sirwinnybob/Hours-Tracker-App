package com.example.timecard.ui.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.repository.FileRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class LeaderboardEntry(
    val name: String,
    val displayName: String?,
    val weekHours: Double,
    val monthHours: Double,
    val allTimeCoins: Int,
    val currentStreak: Int
)

class LeaderboardViewModel : ViewModel() {

    var entries by mutableStateOf<List<LeaderboardEntry>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var lastLoadedWeek by mutableStateOf("")
        private set

    private val gson = Gson()

    fun load(
        employeeNames: List<String>,
        currentWeekDate: String,
        repository: FileRepository?
    ) {
        if (repository == null || isLoading) return
        viewModelScope.launch {
            isLoading = true
            try {
                val result = mutableListOf<LeaderboardEntry>()
                val currentMonth = currentWeekDate.substring(0, 7)

                for (empName in employeeNames) {
                    try {
                        // Load profile for level / displayName / XP
                        val profileJson = repository.loadGenericJSON(empName, "profile.json")
                        val profile = if (profileJson != null) {
                            try { gson.fromJson(profileJson, PlayerProfile::class.java) ?: PlayerProfile() }
                            catch (_: Exception) { PlayerProfile() }
                        } else PlayerProfile()

                        // Current week hours
                        val weekJson = repository.loadFile(empName, currentWeekDate)
                        val weekData = weekJson?.let {
                            try { gson.fromJson(it, TimecardData::class.java) } catch (_: Exception) { null }
                        }
                        val weekHours = weekData?.rows?.sumOf { row ->
                            DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
                        } ?: 0.0

                        // Month hours across all weeks that fall in the current month
                        val dates = repository.getAvailableDates(empName)
                        var monthHours = weekHours
                        for (date in dates) {
                            if (date == currentWeekDate) continue
                            if (!date.startsWith(currentMonth)) continue
                            val json = repository.loadFile(empName, date) ?: continue
                            try {
                                val data = gson.fromJson(json, TimecardData::class.java)
                                monthHours += data.rows.sumOf { row ->
                                    DAYS.sumOf { day -> row.getHours(day).toDoubleOrNull() ?: 0.0 }
                                }
                            } catch (_: Exception) {}
                        }

                        result.add(
                            LeaderboardEntry(
                                name = empName,
                                displayName = profile.displayName,
                                weekHours = weekHours,
                                monthHours = monthHours,
                                allTimeCoins = profile.coins,
                                currentStreak = profile.streaks.currentDaily
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("LeaderboardVM", "Error for $empName", e)
                    }
                }

                entries = result
                lastLoadedWeek = currentWeekDate
            } finally {
                isLoading = false
            }
        }
    }
}
