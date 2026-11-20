package com.example.timecard.ui.timesheet.lcars

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.ActiveJob
import com.example.timecard.ui.timesheet.TimesheetUiState
import com.example.timecard.ui.timesheet.getDayTotal
import com.example.timecard.ui.timesheet.getGrandTotal
import com.example.timecard.ui.timesheet.standard.GridActions
import com.example.timecard.ui.timesheet.standard.summarizeDays
import com.example.timecard.ui.timesheet.standard.todayIndexIn
import com.example.timecard.ui.timesheet.standard.weekStartOf
import java.time.LocalDate
import java.time.temporal.IsoFields

/** ISO week number of the timesheet week ("47"), or "" when the week date isn't a date. */
fun lcarsWeekNumber(activeWeekDate: String): String =
    weekStartOf(activeWeekDate)?.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)?.toString() ?: ""

/** The LCARS timesheet tab: WEEK OUTPUT tanks, then JOBS. Scrolls as one column. */
@Composable
internal fun LcarsTimesheetBody(
    uiState: TimesheetUiState,
    activeJobs: List<ActiveJob>,
    canUndo: Boolean,
    canRedo: Boolean,
    actions: GridActions,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    if (uiState.isLockedByAnotherUser) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("TIMESHEET LOCKED", style = lcarsText(30.sp, LcarsClassic.Red, FontWeight.Normal))
            Spacer(Modifier.height(16.dp))
            Text(
                "IF YOU ARE SEEING THIS IT MEANS THE TIMECARD IS CURRENTLY BEING USED BY ANOTHER TABLET OR HAS BEEN MODIFIED WITHIN THE LAST 5 MINUTES.\n\n" +
                    "IF THIS LOCK PERSISTS TRY:\n1. WAITING 5 MINUTES\n2. SYNCING YOUR TABLET.\n3. RESTARTING THEN SYNCING YOUR TABLET.\n\nIF THIS LOCK PERSISTS TELL WINSTON AND HE WILL GET IT UNLOCKED FOR YOU.",
                style = lcarsText(16.sp, LcarsClassic.Violet, FontWeight.Normal), textAlign = TextAlign.Center
            )
        }
        return
    }
    val weekStart = weekStartOf(uiState.activeWeekDate)
    val today = if (uiState.isViewingPrevious) null else todayIndexIn(weekStart, LocalDate.now())
    val days = summarizeDays((0 until 6).map { uiState.getDayTotal(it) }, weekStart, today)
    // imePadding: the table ends at the keyboard, so a focused hour cell on a low row scrolls into view.
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        LcarsTextBar("WEEK OUTPUT", LcarsClassic.Sienna, LcarsClassic.Butter)
        Spacer(Modifier.height(14.dp))
        LcarsWeekOutput(days, uiState.getGrandTotal(), weekKey = uiState.activeWeekDate, animate = animate)
        Spacer(Modifier.height(22.dp))
        LcarsTextBar("JOBS", LcarsClassic.Lilac, LcarsClassic.Violet)
        Spacer(Modifier.height(12.dp))
        LcarsJobsTable(uiState, days, activeJobs, canUndo, canRedo, actions, animate)
        Spacer(Modifier.height(16.dp))
    }
}
