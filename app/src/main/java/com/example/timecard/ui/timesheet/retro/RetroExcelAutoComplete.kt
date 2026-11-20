package com.example.timecard.ui.timesheet.retro

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.timecard.ui.theme.rememberRetroMotion
import com.example.timecard.ui.timesheet.standard.JobPick
import com.example.timecard.ui.timesheet.standard.resolveJobPick

/**
 * Excel AutoComplete over the job cell: you type in the cell (marching-ants border) and a list box of
 * matching active Ready Jobs drops under it. Same rules as the other pickers (resolveJobPick): number
 * first, then names; anything typed can be used "not on the floor"; Delivery set first or toggled on
 * the current job; SHOP / PTO / SICK / HOLIDAY / Clear as Win95 buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun XlAutoComplete(
    currentJob: String,
    activeJobs: List<ActiveJob>,
    jobsOnCard: Map<String, Int>,
    onPick: (JobPick) -> Unit,
    onDismiss: () -> Unit
) {
    val currentIsDelivery = JobValidator.isDeliveryJob(currentJob)
    val bare = if (currentIsDelivery) currentJob.trim().dropLast(1) else currentJob.trim()
    var query by remember { mutableStateOf(bare) }
    var delivery by remember { mutableStateOf(currentIsDelivery) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val trimmed = query.trim()
    val matches = ActiveJobs.search(trimmed, activeJobs)
    val showTyped = trimmed.isNotEmpty() && !ActiveJobs.isExactNumber(trimmed, activeJobs)
    val choose: (String) -> Unit = { onPick(resolveJobPick(it, delivery, jobsOnCard)) }
    val numbered = bare.isNotEmpty() && bare.all { it in '0'..'9' }

    val motion = rememberRetroMotion()
    val phase by rememberInfiniteTransition(label = "ants").animateFloat(
        0f, if (motion) 16f else 0f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Restart), label = "antsPhase"
    )

    Popup(offset = IntOffset(0, 0), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(Modifier.width(360.dp)) {
            // In-cell edit with the marching-ants border
            Box(
                Modifier.width(XL_JOB_W).height(XL_ROW_H).background(Xl.White)
                    .drawWithContent {
                        drawContent()
                        val s = 2.dp.toPx()
                        drawRect(
                            Xl.Black, topLeft = Offset(s / 2, s / 2),
                            size = androidx.compose.ui.geometry.Size(size.width - s, size.height - s),
                            style = Stroke(s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), phase))
                        )
                    }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = xlText(15.sp),
                    cursorBrush = SolidColor(Xl.Black),
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
            // Drop-down list box
            Column(
                Modifier.fillMaxWidth().shadow(4.dp).background(Xl.White).border(1.dp, Xl.Black)
            ) {
                matches.forEachIndexed { i, job ->
                    val onCard = jobsOnCard.containsKey(job.number.uppercase())
                    val first = i == 0
                    Row(
                        Modifier.fillMaxWidth().height(34.dp).background(if (first) Xl.Navy else Xl.White)
                            .clickable(role = Role.Button) { choose(job.number) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val fg = if (first) Xl.White else Xl.Black
                        Text(job.number, style = xlText(15.sp, fg, FontWeight.Bold), modifier = Modifier.width(52.dp))
                        Text(job.name + if (onCard) "  (on card)" else "", style = xlText(15.sp, fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (showTyped) {
                    Box(
                        Modifier.fillMaxWidth().height(34.dp).clickable(role = Role.Button) { choose(trimmed) }.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) { Text("Use \"${trimmed.uppercase()}\" (not on the floor)", style = xlText(14.sp, Xl.Navy, italic = true)) }
                }
                if (matches.isEmpty() && !showTyped) {
                    Box(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Type a job number or name", style = xlText(14.sp, Xl.Shadow, italic = true))
                    }
                }
                // Footer: Delivery check box + quick entries
                Column(Modifier.fillMaxWidth().background(Xl.Face).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.clickable(role = Role.Checkbox, onClickLabel = "Delivery") {
                            if (trimmed == bare && numbered) onPick(resolveJobPick(bare, !delivery, jobsOnCard)) else delivery = !delivery
                        }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(18.dp).background(Xl.White).xlBevel(inset = true), contentAlignment = Alignment.Center) {
                            if (delivery) Text("✓", style = xlText(14.sp, weight = FontWeight.Bold))
                        }
                        Text("Delivery", style = xlText(15.sp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        XlButton("SHOP", currentJob.equals("SHOP", true)) { choose("SHOP") }
                        XlButton("PTO", currentJob.equals("PTO", true)) { choose("PTO") }
                        XlButton("SICK", currentJob.equals("SICK", true)) { choose("SICK") }
                        XlButton("HOLIDAY", currentJob.equals("HOLIDAY", true)) { choose("HOLIDAY") }
                        if (currentJob.isNotBlank()) XlButton("Clear", false) { onPick(JobPick.Entry("")) }
                        XlButton("Cancel", false, onClick = onDismiss)
                    }
                }
            }
        }
    }
}

/** Small Win95 push button (sunken when [pressed]). */
@Composable
internal fun XlButton(label: String, pressed: Boolean, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 32.dp, onClick: () -> Unit) {
    Box(
        modifier.height(height).background(Xl.Face).xlBevel(inset = pressed)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = xlText(14.sp)) }
}
