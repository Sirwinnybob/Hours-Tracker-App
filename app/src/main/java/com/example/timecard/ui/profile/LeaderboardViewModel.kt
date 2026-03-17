package com.example.timecard.ui.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.PurchaseRecord
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
    val currentStreak: Int,
    val avatarBytes: ByteArray?,
    val bestDailyStreak: Int,
    val currentWeeklyStreak: Int,
    val bestWeeklyStreak: Int,
    val bestWeekHours: Double,
    val bestDayHours: Double,
    val badges: Map<String, Int>,
    val inventory: List<String>,
    val purchaseHistory: List<PurchaseRecord>,
    val coins: Int
)

class LeaderboardViewModel : ViewModel() {

    var entries by mutableStateOf<List<LeaderboardEntry>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var lastLoadedWeek by mutableStateOf("")
        private set

    var feedEvents by mutableStateOf<List<ActivityEvent>>(emptyList())
        private set
    var isFeedLoading by mutableStateOf(false)
        private set

    fun loadFeed(employeeNames: List<String>, repository: FileRepository?) {
        if (repository == null || isFeedLoading) return
        viewModelScope.launch {
            isFeedLoading = true
            try {
                val allEvents = mutableListOf<ActivityEvent>()
                for (empName in employeeNames) {
                    try {
                        allEvents += repository.loadEmployeeActivityEvents(empName)
                    } catch (_: Exception) {}
                }
                feedEvents = allEvents.sortedByDescending { it.timestamp }.take(100)
            } finally {
                isFeedLoading = false
            }
        }
    }

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

                        // Load avatar (try wildcard first, then explicit extensions)
                        val avatarBytes = repository.loadEmployeeBinaryFile(empName, ".avatar.jpg")
                            ?: repository.loadEmployeeBinaryFile(empName, ".avatar.png")
                            ?: repository.loadEmployeeBinaryFile(empName, ".avatar.jpeg")

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
                                allTimeCoins = profile.allTimeCoinsEarned,
                                currentStreak = profile.streaks.currentDaily,
                                avatarBytes = avatarBytes,
                                bestDailyStreak = profile.streaks.bestDaily,
                                currentWeeklyStreak = profile.streaks.currentWeekly,
                                bestWeeklyStreak = profile.streaks.bestWeekly,
                                bestWeekHours = profile.records.bestWeekHours,
                                bestDayHours = profile.records.busiestDay,
                                badges = profile.badges,
                                inventory = profile.inventory,
                                purchaseHistory = profile.purchaseHistory,
                                coins = profile.coins
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
