package com.example.timecard.ui.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.DateUtils
import com.example.timecard.domain.JobSearchResult
import com.example.timecard.domain.StatsCalculator
import com.example.timecard.domain.StatsResult
import com.example.timecard.ui.timesheet.TimesheetViewModel
import com.google.gson.Gson

import com.example.timecard.domain.StatsPeriod

class StatsViewModel : ViewModel() {

    var stats by mutableStateOf<StatsResult?>(null)
        private set
    var selectedPeriod by mutableStateOf<StatsPeriod>(StatsPeriod.ThisWeek)
        private set
    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<JobSearchResult>>(emptyList())
        private set
    var searchTotalHours by mutableStateOf(0.0)
        private set

    private var employeeName = ""
    private var repository: FileRepository? = null
    private var timesheetViewModel: TimesheetViewModel? = null
    private val gson = Gson()

    fun initialize(
        name: String,
        repo: FileRepository?,
        timesheetVm: TimesheetViewModel
    ) {
        employeeName = name
        repository = repo
        timesheetViewModel = timesheetVm
    }

    fun logout() {
        stats = null
        selectedPeriod = StatsPeriod.ThisWeek
        searchQuery = ""
        searchResults = emptyList()
        searchTotalHours = 0.0
        employeeName = ""
        repository = null
        timesheetViewModel = null
    }

    fun loadStats(period: StatsPeriod) {
        selectedPeriod = period
        viewModelScope.launch {
            val weeks = withContext(Dispatchers.IO) { loadStatsData(period) }
            stats = StatsCalculator.calculateStats(weeks)
        }
    }

    fun searchJob() {
        val query = searchQuery.trim()
        if (query.isEmpty()) return

        val tsVm = timesheetViewModel ?: return
        // Snapshot UI state on the calling (main) thread before switching to IO
        val activeWeekDate = tsVm.uiState.value.activeWeekDate
        val isViewingPrevious = tsVm.uiState.value.isViewingPrevious
        val currentData = if (!isViewingPrevious) tsVm.collectTimecardData() else null
        viewModelScope.launch {
            val (results, total) = withContext(Dispatchers.IO) {
                StatsCalculator.searchJob(
                    query = query,
                    employeeName = employeeName,
                    activeWeekDate = activeWeekDate,
                    isViewingPrevious = isViewingPrevious,
                    currentData = currentData,
                    loadFile = { name, date -> repository?.loadFile(name, date) }
                )
            }
            searchResults = results
            searchTotalHours = total
        }
    }

    private fun loadStatsData(period: StatsPeriod): List<TimecardData> {
        val tsVm = timesheetViewModel ?: return emptyList()
        val weeks = mutableListOf<TimecardData>()

        val activeDate = tsVm.uiState.value.currentWeekDate // To determine "This Month", we root on what the system thinks is today

        val validDates = mutableListOf<String>()

        when (period) {
            is StatsPeriod.ThisWeek -> {
                validDates.add(tsVm.uiState.value.activeWeekDate)
            }
            is StatsPeriod.LastWeek -> {
                validDates.add(DateUtils.getPreviousMonday(tsVm.uiState.value.activeWeekDate))
            }
            is StatsPeriod.TwoWeeks -> {
                validDates.add(tsVm.uiState.value.activeWeekDate)
                validDates.add(DateUtils.getPreviousMonday(tsVm.uiState.value.activeWeekDate))
            }
            is StatsPeriod.ThisMonth -> {
                val available = repository?.getAvailableDates(employeeName) ?: emptyList()
                validDates.addAll(available.filter { DateUtils.isInSameMonth(it, activeDate, monthOffset = 0) })
                val active = tsVm.uiState.value.activeWeekDate
                if (!validDates.contains(active) && DateUtils.isInSameMonth(active, activeDate, 0)) {
                    validDates.add(active)
                }
            }
            is StatsPeriod.LastMonth -> {
                val available = repository?.getAvailableDates(employeeName) ?: emptyList()
                validDates.addAll(available.filter { DateUtils.isInSameMonth(it, activeDate, monthOffset = 1) })
                val active = tsVm.uiState.value.activeWeekDate
                if (!validDates.contains(active) && DateUtils.isInSameMonth(active, activeDate, 1)) {
                    validDates.add(active)
                }
            }
            is StatsPeriod.AllTime -> {
                val available = repository?.getAvailableDates(employeeName) ?: emptyList()
                validDates.addAll(available)
                if (!validDates.contains(tsVm.uiState.value.activeWeekDate)) {
                    validDates.add(tsVm.uiState.value.activeWeekDate)
                }
            }
            is StatsPeriod.Custom -> {
                val available = repository?.getAvailableDates(employeeName) ?: emptyList()
                validDates.addAll(available.filter { DateUtils.isDateInRange(it, period.startDate, period.endDate) })
                val active = tsVm.uiState.value.activeWeekDate
                if (!validDates.contains(active) && DateUtils.isDateInRange(active, period.startDate, period.endDate)) {
                    validDates.add(active)
                }
            }
        }

        // Distinct them just in case
        val finalDatesToLoad = validDates.distinct()

        for (date in finalDatesToLoad) {
            // Optimization: if it's the date currently active in the viewmodel AND we aren't viewing past data, just pull from memory
            if (date == tsVm.uiState.value.activeWeekDate && !tsVm.uiState.value.isViewingPrevious) {
                weeks.add(tsVm.collectTimecardData())
            } else {
                val json = repository?.loadFile(employeeName, date)
                if (json != null) {
                    try { weeks.add(gson.fromJson(json, TimecardData::class.java)) } catch (_: Exception) {}
                }
            }
        }
        
        return weeks
    }
}
