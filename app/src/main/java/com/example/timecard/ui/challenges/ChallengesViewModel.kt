package com.example.timecard.ui.challenges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.Challenge
import com.example.timecard.data.model.PlayerProfile
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.ChallengeEngine
import com.google.gson.Gson
import kotlinx.coroutines.launch

data class ChallengeProgress(
    val challenge: Challenge,
    val progress: Double,       // 0.0–1.0
    val isComplete: Boolean
)

class ChallengesViewModel : ViewModel() {

    var challengeProgress by mutableStateOf<List<ChallengeProgress>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val gson = Gson()

    fun load(
        employeeName: String,
        weekDate: String,
        profile: PlayerProfile,
        repository: FileRepository?
    ) {
        if (repository == null || isLoading) return
        viewModelScope.launch {
            isLoading = true
            try {
                val challenges = repository.loadChallenges()
                if (challenges.isEmpty()) {
                    challengeProgress = emptyList()
                    return@launch
                }

                val weekJson = repository.loadFile(employeeName, weekDate)
                val timecard = weekJson?.let {
                    try { gson.fromJson(it, TimecardData::class.java) } catch (_: Exception) { null }
                }

                challengeProgress = challenges.map { ch ->
                    val prog = if (timecard != null) ChallengeEngine.computeProgress(ch, timecard) else 0.0
                    val logKey = "${ch.id}_$weekDate"
                    val done = profile.challengeLog.containsKey(logKey) || prog >= 1.0
                    ChallengeProgress(ch, prog, done)
                }
            } finally {
                isLoading = false
            }
        }
    }
}
