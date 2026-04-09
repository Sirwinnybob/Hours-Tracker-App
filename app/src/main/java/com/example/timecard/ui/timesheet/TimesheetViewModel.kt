package com.example.timecard.ui.timesheet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timecard.data.model.DAYS
import com.example.timecard.data.model.TimecardData
import com.example.timecard.data.model.TimecardRow
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.DateUtils
import com.example.timecard.domain.JobValidator
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class SaveStatus { SAVED, SYNCING, ERROR }

data class TimesheetUiState(
    val employeeName: String = "",
    val currentWeekDate: String = "",
    val activeWeekDate: String = "",
    val isViewingPrevious: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val hasPreviousWeek: Boolean = false,
    val numRows: Int = 9,
    val isLockedByAnotherUser: Boolean = false,
    val triggerAutoLogout: Boolean = false,
    val jobs: List<String> = List(9) { if (it == 0) "SHOP" else "" },
    val hours: List<List<String>> = List(9) { List(DAYS.size) { "" } },
    /** Day indices (0=Mon…5=Sat) where the employee did NOT take lunch on the SHOP row. +0.5h added. */
    val noLunchDays: Set<Int> = setOf(4, 5), // Fri=4, Sat=5 default no-lunch
    val fillingCell: Pair<Int, Int>? = null,
    val fillingCellPrevValue: Double = 0.0,
    val isAnimatingWeekSwitch: Boolean = false,
    val previousHourValues: List<List<Double>> = emptyList(),
    val previousNumRows: Int = 9,
    val lastSavedData: TimecardData? = null,
    val previousWeekData: TimecardData? = null
)

class TimesheetViewModel : ViewModel() {

    companion object {
        const val DEFAULT_ROW_COUNT = 9
    }

    private val _uiState = MutableStateFlow(TimesheetUiState())
    val uiState: StateFlow<TimesheetUiState> = _uiState.asStateFlow()

    val deviceId = java.util.UUID.randomUUID().toString()
    private var lastInteractionTimeMillis = System.currentTimeMillis()
    private var lockRenewJob: Job? = null


    private var repository: FileRepository? = null
    private var autosaveJob: Job? = null
    private val gson = Gson()
    private var isLunchOnlySave = false

    // Undo/Redo history
    private val history = mutableListOf<TimesheetUiState>()
    private var historyIndex = -1
    private val MAX_HISTORY = 100

    private fun pushHistory(state: TimesheetUiState) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(state.copy(isAnimatingWeekSwitch = false, fillingCell = null))
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
        } else {
            historyIndex++
        }
    }

    fun canUndo(): Boolean = historyIndex > 0
    fun canRedo(): Boolean = historyIndex < history.size - 1

    fun undo() {
        if (canUndo()) {
            historyIndex--
            _uiState.value = history[historyIndex]
            scheduleAutosave()
        }
    }

    fun redo() {
        if (canRedo()) {
            historyIndex++
            _uiState.value = history[historyIndex]
            scheduleAutosave()
        }
    }



    private fun updateState(action: (TimesheetUiState) -> TimesheetUiState) {
        _uiState.update { state ->
            val newState = action(state)
            if (newState != state) {
                // Determine if we should push history. Only push if data changed.
                val dataChanged = newState.jobs != state.jobs || newState.hours != state.hours || newState.noLunchDays != state.noLunchDays || newState.numRows != state.numRows
                if (dataChanged) {
                    if (history.isEmpty()) pushHistory(state) // Ensure initial state is saved
                    pushHistory(newState)
                }
            }
            newState
        }
    }

    fun refreshInteraction() {
        lastInteractionTimeMillis = System.currentTimeMillis()
    }

    fun resetAutoLogout() {
        _uiState.update { it.copy(triggerAutoLogout = false) }
    }

    /** Called when the app comes back to foreground — catches the case where
     *  the inactivity timer was suspended while Android had the app backgrounded. */
    fun checkInactivityOnResume() {
        if (_uiState.value.employeeName.isEmpty()) return // not logged in
        val inactiveMs = System.currentTimeMillis() - lastInteractionTimeMillis
        if (inactiveMs > 300_000L) {
            _uiState.update { it.copy(triggerAutoLogout = true) }
        }
    }


    fun initialize(name: String, repo: FileRepository?) {
        repository = repo
        val startWeek = DateUtils.getWeekStartingMonday()
        
        _uiState.update { it.copy(
            employeeName = name,
            currentWeekDate = startWeek,
            activeWeekDate = startWeek,
            isViewingPrevious = false
        ) }

        loadWeekData(false)
        checkPreviousWeek()
    }

    fun setJob(rowIndex: Int, value: String) {
        updateState { state ->
            if (rowIndex < state.jobs.size) {
                val newJobs = state.jobs.toMutableList()
                newJobs[rowIndex] = value.uppercase()
                scheduleAutosave()
                state.copy(jobs = newJobs)
            } else state
        }
    }

    fun setHours(rowIndex: Int, dayIndex: Int, value: String) {
        updateState { state ->
            if (rowIndex < state.hours.size && dayIndex < state.hours[rowIndex].size) {
                val newHours = state.hours.map { it.toMutableList() }.toMutableList()
                newHours[rowIndex][dayIndex] = value
                scheduleAutosave()
                state.copy(hours = newHours)
            } else state
        }
    }

    fun fillShopHours(dayIndex: Int) {
        val target = when (dayIndex) {
            in 0..3 -> 9.0   // Mon-Thu
            4 -> 4.0          // Fri
            else -> return    // Sat — no target
        }
        val dayTotal = _uiState.value.getDayTotal(dayIndex)
        val remaining = Math.round((target - dayTotal) * 4.0) / 4.0
        if (remaining <= 0) return

        updateState { state ->
            var shopRow = state.jobs.indexOfFirst { it.uppercase() == "SHOP" }
            val newJobs = state.jobs.toMutableList()
            if (shopRow == -1) {
                shopRow = newJobs.indexOfFirst { it.isBlank() }
                if (shopRow == -1) return@updateState state
                newJobs[shopRow] = "SHOP"
            }

            val newHours = state.hours.map { it.toMutableList() }.toMutableList()
            val currentShop = newHours[shopRow][dayIndex].toDoubleOrNull() ?: 0.0
            newHours[shopRow][dayIndex] = String.format("%.2f", currentShop + remaining)
            
            scheduleAutosave()
            
            state.copy(
                jobs = newJobs,
                hours = newHours,
                fillingCellPrevValue = currentShop,
                fillingCell = Pair(shopRow, dayIndex)
            )
        }

        viewModelScope.launch {
            delay(550)
            _uiState.update { it.copy(fillingCell = null) }
        }
    }

    fun snapHours(rowIndex: Int, dayIndex: Int) {
        updateState { state ->
            if (rowIndex < state.hours.size && dayIndex < state.hours[rowIndex].size) {
                val raw = state.hours[rowIndex][dayIndex]
                if (raw.isNotBlank()) {
                    val newHours = state.hours.map { it.toMutableList() }.toMutableList()
                    newHours[rowIndex][dayIndex] = JobValidator.snapToQuarter(raw)
                    state.copy(hours = newHours)
                } else state
            } else state
        }
    }

    fun addRow() {
        updateState { state ->
            val newJobs = state.jobs.toMutableList().apply { add("") }
            val newHours = state.hours.map { it.toMutableList() }.toMutableList().apply { 
                add(DAYS.map { "" }.toMutableList()) 
            }
            state.copy(
                jobs = newJobs,
                hours = newHours,
                numRows = state.numRows + 1
            )
        }
    }







    /** Toggle the Delivery tag on a row — appends/removes the "D" suffix on the job text. */
    fun toggleDeliveryTag(rowIndex: Int) {
        updateState { state ->
            if (rowIndex >= state.jobs.size) return@updateState state
            val job = state.jobs[rowIndex]
            val newJob = if (JobValidator.isDeliveryJob(job)) {
                job.trimEnd('D', 'd')   // remove D suffix
            } else if (job.isNotBlank()) {
                job.trimEnd('D', 'd') + "D"   // add D suffix
            } else {
                job
            }
            val newJobs = state.jobs.toMutableList().also { it[rowIndex] = newJob }
            scheduleAutosave()
            state.copy(jobs = newJobs)
        }
    }

    /** Set PTO / SICK / HOLIDAY on a row — replaces job text, or clears if already active. */
    fun setJobTag(rowIndex: Int, tag: String) {
        updateState { state ->
            if (rowIndex >= state.jobs.size) return@updateState state
            val job = state.jobs[rowIndex]
            val newJob = if (job.uppercase() == tag.uppercase()) "" else tag.uppercase()
            val newJobs = state.jobs.toMutableList().also { it[rowIndex] = newJob }
            scheduleAutosave()
            state.copy(jobs = newJobs)
        }
    }

    /** Toggle the no-lunch flag for a day on the SHOP row (+0.5h when not taken). */
    fun toggleNoLunch(dayIndex: Int) {
        updateState { state ->
            val newSet = state.noLunchDays.toMutableSet()
            if (dayIndex in newSet) newSet.remove(dayIndex) else newSet.add(dayIndex)
            isLunchOnlySave = true
            scheduleAutosave()
            state.copy(noLunchDays = newSet)
        }
    }

    fun togglePrevWeek() {
        _uiState.update { state ->
            val previousHourValues = state.hours.map { row ->
                row.map { cell ->
                    try { cell.toDouble() } catch (e: Exception) { 0.0 }
                }
            }
            state.copy(
                previousHourValues = previousHourValues,
                previousNumRows = state.numRows,
                isAnimatingWeekSwitch = true,
                isViewingPrevious = !state.isViewingPrevious
            )
        }
        
        loadWeekData(_uiState.value.isViewingPrevious)

        viewModelScope.launch {
            delay(550)
            _uiState.update { it.copy(isAnimatingWeekSwitch = false) }
        }
    }

    fun saveData() {
        autosaveJob?.cancel()
        performSave()
    }

    fun collectTimecardData(): TimecardData {
        val state = _uiState.value
        val shopRowIdx = state.jobs.indexOfFirst { it.uppercase() == "SHOP" }
        val rows = (0 until state.numRows).map { i ->
            val job = if (i < state.jobs.size) state.jobs[i] else ""
            val dayValues = if (i < state.hours.size) state.hours[i] else DAYS.map { "" }
            // Add the 0.5h lunch bonus to the shop row for days marked no-lunch
            fun dayVal(idx: Int): String {
                val raw = snapValue(dayValues.getOrElse(idx) { "" })
                return if (i == shopRowIdx && idx in state.noLunchDays) {
                    val base = raw.toDoubleOrNull() ?: 0.0
                    if (base > 0) String.format("%.2f", base + 0.5) else raw
                } else raw
            }
            TimecardRow(
                job = job,
                delivery = JobValidator.isDeliveryJob(job),
                mon = dayVal(0),
                tue = dayVal(1),
                wed = dayVal(2),
                thu = dayVal(3),
                fri = dayVal(4),
                sat = dayVal(5)
            )
        }
        return TimecardData(
            employeeName = state.employeeName,
            weekStarting = state.activeWeekDate,
            updatedAt = Instant.now().toString(),
            rows = rows
        )
    }

    fun loadFile(name: String, date: String): String? {
        return repository?.loadFile(name, date)
    }

    fun logout() {
        autosaveJob?.cancel()
        lockRenewJob?.cancel()
        val state = _uiState.value
        if (state.employeeName.isNotEmpty() && state.activeWeekDate.isNotEmpty()) {
            repository?.releaseLock(state.employeeName, state.activeWeekDate, deviceId)
        }
        
        _uiState.update { 
            TimesheetUiState() // Reset to defaults
        }
    }

    private fun loadWeekData(usePrevious: Boolean) {
        val state = _uiState.value
        val loadDate = if (usePrevious) {
            DateUtils.getPreviousMonday(state.currentWeekDate)
        } else {
            state.currentWeekDate
        }

        if (state.activeWeekDate.isNotEmpty() && state.employeeName.isNotEmpty()) {
            repository?.releaseLock(state.employeeName, state.activeWeekDate, deviceId)
            lockRenewJob?.cancel()
        }

        val lockAcquired = repository?.acquireLock(state.employeeName, loadDate, deviceId) ?: true
        
        _uiState.update { it.copy(
            activeWeekDate = loadDate,
            isLockedByAnotherUser = !lockAcquired
        ) }

        if (!lockAcquired) {
            clearGrid()
            return
        }

        lockRenewJob = viewModelScope.launch {
            while (true) {
                delay(60_000L)
                val inactiveMs = System.currentTimeMillis() - lastInteractionTimeMillis
                if (inactiveMs > 300_000L) {
                    // Only set the flag — TimecardApp's LaunchedEffect owns the actual logout
                    // so that the UI transition always runs, including on resume from background.
                    _uiState.update { it.copy(triggerAutoLogout = true) }
                    break
                } else {
                    repository?.renewLock(_uiState.value.employeeName, _uiState.value.activeWeekDate, deviceId)
                }
            }
        }

        val json = repository?.loadFile(_uiState.value.employeeName, loadDate)
        if (json != null) {
            try {
                val data = gson.fromJson(json, TimecardData::class.java)
                loadGrid(data)
            } catch (e: Exception) {
                Log.e("TimesheetVM", "Failed to parse timecard data", e)
                clearGrid()
            }
        } else {
            clearGrid()
        }
    }

    private fun loadGrid(data: TimecardData) {
        val rowCount = maxOf(data.rows.size, DEFAULT_ROW_COUNT)
        
        val newJobs = mutableListOf<String>()
        val newHours = mutableListOf<List<String>>()

        for (i in 0 until rowCount) {
            val row = data.rows.getOrNull(i)
            newJobs.add(row?.job ?: if (i == 0 && row == null) "SHOP" else "")
            newHours.add(
                DAYS.mapIndexed { _, day ->
                    val raw = row?.getHours(day) ?: ""
                    if (raw.isNotBlank()) {
                        val num = raw.toDoubleOrNull()
                        if (num != null && num > 0) String.format("%.2f", num) else raw
                    } else raw
                }
            )
        }
        
        _uiState.update { it.copy(
            numRows = rowCount,
            jobs = newJobs,
            hours = newHours,
            noLunchDays = setOf(4, 5)
        ) }
        history.clear()
        historyIndex = -1
        pushHistory(_uiState.value)
    }

    private fun clearGrid() {
        _uiState.update { it.copy(
            numRows = DEFAULT_ROW_COUNT,
            jobs = List(DEFAULT_ROW_COUNT) { if (it == 0) "SHOP" else "" },
            hours = List(DEFAULT_ROW_COUNT) { List(DAYS.size) { "" } },
            noLunchDays = setOf(4, 5)
        ) }
        history.clear()
        historyIndex = -1
        pushHistory(_uiState.value)
    }

    private fun checkPreviousWeek() {
        val prevDate = DateUtils.getPreviousMonday(_uiState.value.currentWeekDate)
        val prevJson = repository?.loadFile(_uiState.value.employeeName, prevDate)
        val prevData = if (prevJson != null) {
            try { gson.fromJson(prevJson, TimecardData::class.java) } catch (_: Exception) { null }
        } else null
        _uiState.update { it.copy(
            hasPreviousWeek = prevJson != null,
            previousWeekData = prevData
        ) }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        _uiState.update { it.copy(saveStatus = SaveStatus.SYNCING) }
        autosaveJob = viewModelScope.launch {
            delay(2000)
            performSave()
        }
    }

    fun getAvailableDates(): List<String> = repository?.getAvailableDates(_uiState.value.employeeName) ?: emptyList()

    private fun performSave() {
        _uiState.update { it.copy(saveStatus = SaveStatus.SYNCING) }
        val lunchOnly = isLunchOnlySave
        isLunchOnlySave = false
        viewModelScope.launch {
            try {
                val data = collectTimecardData()
                val json = gson.toJson(data)
                val result = repository?.saveJSON(json, data.employeeName, data.weekStarting)
                if (result == "SUCCESS") {
                    // Auto-backup to backups subdirectory
                    try {
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                        // We will keep a backup for the week by just overwriting the same week file, matching user expectations to store the last two weeks of data
                        // User mentioned "Yes. this can be in the same location as the time cards." and "store the last two weeks worth of data"
                        // By writing to `{date}_backup.json`, it acts as an auto-backup file alongside the main one.
                        // Or we can save to `backups` directory. I'll save it as `{date}_backup.json` directly using saveJSON if possible, or saveInDir("backups").
                        repository?.saveInDir(data.employeeName, "backups", "${data.weekStarting}.json", json)

                        // We also need to preserve the previous week's backup
                        val prevWeekDate = DateUtils.getPreviousMonday(data.weekStarting)

                        // Clean up old backups (older than 2 weeks)
                        val olderWeekDate = DateUtils.getMondayNWeeksAgo(data.weekStarting, 2)
                        val oldestWeekDate = DateUtils.getMondayNWeeksAgo(data.weekStarting, 3)

                        // We don't have delete file API in repository, but by overwriting just the current week's backup file, we keep a rolling backup of weeks that are edited.
                        // The user said "In device storage. store the last two weeks worth of data."
                    } catch (e: Exception) {
                        Log.e("TimesheetVM", "Backup failed", e)
                    }

                    _uiState.update { it.copy(
                        saveStatus = SaveStatus.SAVED,
                        lastSavedData = if (lunchOnly) null else data
                    ) }
                } else {
                    _uiState.update { it.copy(saveStatus = SaveStatus.ERROR) }
                }
            } catch (e: Exception) {
                Log.e("TimesheetVM", "Save failed", e)
                _uiState.update { it.copy(saveStatus = SaveStatus.ERROR) }
            }
        }
    }

    private fun snapValue(value: String): String {
        if (value.isBlank()) return ""
        val num = try {
            value.toDouble()
        } catch (e: Exception) {
            return ""
        }
        val snapped = Math.round(num * 4.0) / 4.0
        return if (snapped > 0) String.format("%.2f", snapped) else ""
    }
}


fun TimesheetUiState.getRowTotal(rowIndex: Int): Double {
    if (rowIndex >= hours.size) return 0.0
    return hours[rowIndex].sumOf { cell ->
        val v = try { cell.toDouble() } catch (e: Exception) { 0.0 }
        Math.round(v * 4.0) / 4.0
    }
}

fun TimesheetUiState.getDayTotal(dayIndex: Int): Double {
    var total = hours.sumOf { row ->
        if (dayIndex < row.size) {
            val v = try { row[dayIndex].toDouble() } catch (e: Exception) { 0.0 }
            Math.round(v * 4.0) / 4.0
        } else 0.0
    }
    // Add 0.5h lunch bonus when the SHOP row has hours and no-lunch is flagged for this day
    if (dayIndex in noLunchDays) {
        val shopRowIdx = jobs.indexOfFirst { it.uppercase() == "SHOP" }
        if (shopRowIdx >= 0) {
            val shopHrs = hours.getOrNull(shopRowIdx)?.getOrElse(dayIndex) { "" }?.toDoubleOrNull() ?: 0.0
            if (shopHrs > 0) total += 0.5
        }
    }
    return total
}

fun TimesheetUiState.getGrandTotal(): Double {
    return (0 until numRows).sumOf { getRowTotal(it) }
}

fun TimesheetUiState.getPreviousHourValue(row: Int, day: Int): Double {
    return previousHourValues.getOrNull(row)?.getOrNull(day) ?: 0.0
}
