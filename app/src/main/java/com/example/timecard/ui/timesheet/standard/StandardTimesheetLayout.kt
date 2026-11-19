package com.example.timecard.ui.timesheet.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.ActiveJob
import com.example.timecard.domain.ActiveJobs
import com.example.timecard.ui.timesheet.TimesheetUiState
import com.example.timecard.ui.timesheet.getDayTotal
import com.example.timecard.ui.timesheet.getGrandTotal
import com.example.timecard.ui.timesheet.getRowTotal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The standard theme's timesheet body (KKC look, "A + B" from the 2026-11-19 mockups):
 * week card with day tiles → timesheet grid → where the week went. Scrolls as one column.
 * The header is separate ([StandardHeader]) so it stays put.
 */
@Composable
internal fun StandardTimesheetBody(
    look: StandardLook,
    uiState: TimesheetUiState,
    activeJobs: List<ActiveJob>,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: GridActions,
    shopLabel: String?,
    onShop: () -> Unit,
    onStats: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val weekStart = weekStartOf(uiState.activeWeekDate)
    val todayIndex = if (uiState.isViewingPrevious) null else todayIndexIn(weekStart, LocalDate.now())
    val days = summarizeDays((0 until 6).map { uiState.getDayTotal(it) }, weekStart, todayIndex)
    val shares = jobShares(uiState, activeJobs)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "week") {
            Box(Modifier.widthIn(max = 1100.dp)) {
                WeekStripCard(look, days, uiState.getGrandTotal(), weekRangeLabel(weekStart), uiState.isViewingPrevious, shopLabel, onShop)
            }
        }
        item(key = "grid") {
            Box(Modifier.widthIn(max = 1100.dp)) {
                TimesheetGridCard(look, uiState, days, activeJobs, canUndo, canRedo, actions)
            }
        }
        item(key = "split") {
            Box(Modifier.widthIn(max = 1100.dp)) { WeekSplitCard(look, shares, onStats) }
        }
    }
}

/** Shown instead of the body when another tablet holds the timecard (same text as before, no emoji). */
@Composable
internal fun StandardLockedNotice(look: StandardLook, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Timesheet locked", color = look.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "If you are seeing this it means the timecard is currently being used by another tablet or has been modified within the last 5 minutes.\n\nIf this lock persists try:\n1. Waiting 5 minutes\n2. Syncing your tablet.\n3. Restarting then syncing your tablet.\n\nIf this lock persists tell Winston and he will get it unlocked for you.",
            color = look.text2, fontSize = 16.sp, textAlign = TextAlign.Center
        )
    }
}

/** Active jobs on the floor, read once per repository from the Ready Jobs root (IO thread). */
@Composable
internal fun rememberActiveJobs(repository: FileRepository?): List<ActiveJob> {
    val jobs by produceState(initialValue = emptyList<ActiveJob>(), repository) {
        value = withContext(Dispatchers.IO) {
            runCatching { ActiveJobs.fromFolderNames(repository?.listReadyJobFolders().orEmpty()) }.getOrDefault(emptyList())
        }
    }
    return jobs
}

/** Hours per job this week (same job on two rows adds up), biggest first. */
internal fun jobShares(uiState: TimesheetUiState, activeJobs: List<ActiveJob>): List<JobShare> {
    val totals = linkedMapOf<String, Double>()
    for (i in 0 until uiState.numRows) {
        val job = uiState.jobs.getOrElse(i) { "" }.trim().uppercase()
        val hours = uiState.getRowTotal(i)
        if (job.isEmpty() || hours <= 0.0) continue
        totals[job] = (totals[job] ?: 0.0) + hours
    }
    return totals.entries
        .sortedByDescending { it.value }
        .map { (job, hours) -> JobShare(job, ActiveJobs.find(job, activeJobs)?.name, hours) }
}

private val RANGE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

internal fun weekRangeLabel(weekStart: LocalDate?): String =
    if (weekStart == null) "" else "${weekStart.format(RANGE_FMT)} – ${weekStart.plusDays(5).format(RANGE_FMT)}"

internal fun weekOfLabel(activeWeekDate: String): String {
    val start = weekStartOf(activeWeekDate) ?: return "Week of $activeWeekDate"
    return "Week of ${start.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
}
