package com.example.timecard.ui.timesheet.lcars

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.timecard.domain.ActiveJob
import com.example.timecard.domain.ActiveJobs
import com.example.timecard.domain.JobValidator
import com.example.timecard.ui.timesheet.standard.JobPick
import com.example.timecard.ui.timesheet.standard.resolveJobPick

/**
 * LCARS job lookup under a job pill: same rules as the standard JobPickerPopup (number first, then
 * names; anything typed can be used "not on the floor"; Delivery can be set first or toggled on the
 * current job), drawn as a small TNG panel with a blinking cursor.
 */
@Composable
internal fun LcarsJobLookup(
    currentJob: String,
    activeJobs: List<ActiveJob>,
    jobsOnCard: Map<String, Int>,
    anchorHeightPx: Int,
    onPick: (JobPick) -> Unit,
    onDismiss: () -> Unit
) {
    val currentIsDelivery = JobValidator.isDeliveryJob(currentJob)
    val bare = if (currentIsDelivery) currentJob.trim().dropLast(1) else currentJob.trim()
    var query by remember { mutableStateOf(bare) }
    var delivery by remember { mutableStateOf(currentIsDelivery) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(1f, 0f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "blink")

    val trimmed = query.trim()
    val matches = ActiveJobs.search(trimmed, activeJobs)
    val showTyped = trimmed.isNotEmpty() && !ActiveJobs.isExactNumber(trimmed, activeJobs)
    val choose: (String) -> Unit = { onPick(resolveJobPick(it, delivery, jobsOnCard)) }
    val numbered = bare.isNotEmpty() && bare.all { it in '0'..'9' }

    Popup(offset = IntOffset(0, anchorHeightPx), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier.width(470.dp).padding(top = 6.dp).background(Color.Black).border(2.dp, LcarsClassic.Orange, RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LcarsTextBar("JOB LOOKUP", LcarsClassic.Orange, LcarsClassic.Orange) {
                LcarsPill("CLOSE", LcarsClassic.Red, onDismiss, Modifier.width(90.dp), height = 30.dp, shape = RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp))
            }
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NUMBER OR NAME", style = lcarsText(14.sp, LcarsClassic.Bluey))
                Box(Modifier.weight(1f)) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = lcarsText(34.sp, LcarsClassic.Creme, FontWeight.Normal),
                        cursorBrush = SolidColor(LcarsClassic.Orange.copy(alpha = blink)),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            when {
                                ActiveJobs.isExactNumber(trimmed, activeJobs) -> choose(trimmed)
                                matches.size == 1 -> choose(matches.first().number)
                                trimmed.isNotEmpty() -> choose(trimmed)
                                else -> onDismiss()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Job number or name" }
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(LcarsClassic.Bluey))
            matches.forEachIndexed { i, job ->
                val onCard = jobsOnCard.containsKey(job.number.uppercase())
                val enter = rememberEntrance(true, i * 60, durationMs = 180)
                Box(
                    Modifier.fillMaxWidth().height(50.dp).graphicsLayer { alpha = enter; translationX = (1f - enter) * 30f }
                        .clip(RoundedCornerShape(50)).background(if (i % 2 == 0) LcarsClassic.Butter else LcarsClassic.Violet)
                        .clickable(role = Role.Button) { choose(job.number) }
                ) {
                    Text(job.number, style = lcarsText(22.sp, Color.Black, FontWeight.Bold), modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp))
                    Text(
                        (job.name + if (onCard) " · ON CARD" else "").uppercase(), style = lcarsText(13.sp, Color.Black), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 7.dp, start = 90.dp)
                    )
                }
            }
            if (showTyped) {
                LcarsPill("USE ${trimmed.uppercase()} • NOT ON FLOOR", LcarsClassic.Red, { choose(trimmed) }, Modifier.fillMaxWidth(), height = 50.dp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val tag: @Composable (String, Color, Boolean, () -> Unit) -> Unit = { label, color, active, onClick ->
                    LcarsPill(label, if (active) LcarsClassic.Creme else color, onClick, Modifier.weight(1f), height = 40.dp, fontSize = 13.sp)
                }
                tag("SHOP", LcarsClassic.Bluey, currentJob.equals("SHOP", true)) { choose("SHOP") }
                tag("DELIVERY", LcarsClassic.Butter, delivery) {
                    if (trimmed == bare && numbered) onPick(resolveJobPick(bare, !delivery, jobsOnCard)) else delivery = !delivery
                }
                tag("PTO", LcarsClassic.Violet, currentJob.equals("PTO", true)) { choose("PTO") }
                tag("SICK", LcarsClassic.Violet, currentJob.equals("SICK", true)) { choose("SICK") }
                tag("HOLIDAY", LcarsClassic.Violet, currentJob.equals("HOLIDAY", true)) { choose("HOLIDAY") }
                if (currentJob.isNotBlank()) tag("CLEAR", LcarsClassic.Red, false) { onPick(JobPick.Entry("")) }
            }
        }
    }
}
