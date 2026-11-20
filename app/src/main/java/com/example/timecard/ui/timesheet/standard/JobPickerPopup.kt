package com.example.timecard.ui.timesheet.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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

/** What the operator chose in the job picker. */
sealed interface JobPick {
    /** An entry to store as the row's job: an active job number, a typed number, SHOP, or a tag word. */
    data class Entry(val job: String) : JobPick
    /** The job is already on another row: go there instead of adding a duplicate. */
    data class ExistingRow(val rowIndex: Int) : JobPick
}

internal fun resolveJobPick(entry: String, delivery: Boolean, jobsOnCard: Map<String, Int>): JobPick {
    val job = if (delivery && isNumberedJob(entry)) "${entry}D" else entry
    val existing = jobsOnCard[job.uppercase()]
    return if (existing == null) JobPick.Entry(job) else JobPick.ExistingRow(existing)
}

private fun isNumberedJob(entry: String): Boolean =
    entry.isNotEmpty() && entry.all { it in '0'..'9' }

/**
 * Small popup that opens under a row's job cell. One field: a number matches active Ready Jobs by
 * number first, words match names; anything else can still be used as typed ("not on the floor").
 * Delivery can be set before choosing a job or toggled on the current numbered job.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JobPickerPopup(
    look: StandardLook,
    currentJob: String,
    activeJobs: List<ActiveJob>,
    /** Job entry (uppercase) → row index, for every other row that already has a job. */
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

    val matches = ActiveJobs.search(query, activeJobs)
    val trimmed = query.trim()
    val showTyped = trimmed.isNotEmpty() && !ActiveJobs.isExactNumber(trimmed, activeJobs)
    val choose: (String) -> Unit = { entry ->
        onPick(resolveJobPick(entry, delivery, jobsOnCard))
    }

    // Under the job cell, but moved above the keyboard when it would cover the picker.
    Popup(
        popupPositionProvider = com.example.timecard.ui.common.rememberImeAwarePopupPosition(),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .padding(top = 4.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(look.surface)
                .border(1.dp, look.line, RoundedCornerShape(14.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, look.primary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(HoursIcons.Search, null, tint = look.text2, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Job number or name", color = look.empty, fontSize = 17.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = monoStyle(18, look.text),
                        cursorBrush = SolidColor(look.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            when {
                                ActiveJobs.isExactNumber(trimmed, activeJobs) -> choose(trimmed)
                                matches.size == 1 -> choose(matches.first().number)
                                trimmed.isNotEmpty() -> choose(trimmed)
                                else -> onDismiss()
                            }
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .semantics { contentDescription = "Job number or name" }
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(PillShape)
                            .background(look.head)
                            .clickable(role = Role.Button, onClickLabel = "Clear") { query = "" },
                        contentAlignment = Alignment.Center
                    ) { Icon(HoursIcons.Close, "Clear", tint = look.text2, modifier = Modifier.size(16.dp)) }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagChip(look, "SHOP", currentJob.equals("SHOP", true)) { choose("SHOP") }
                TagChip(look, "Delivery", delivery, HoursIcons.Delivery) {
                    if (trimmed == bare && isNumberedJob(bare)) {
                        onPick(resolveJobPick(bare, !delivery, jobsOnCard))
                    } else {
                        delivery = !delivery
                    }
                }
                if (currentJob.isNotBlank()) {
                    TagChip(look, "Clear job", false, HoursIcons.Close) { onPick(JobPick.Entry("")) }
                }
            }

            // Active-job matches
            matches.forEach { job ->
                val onCard = resolveJobPick(job.number, delivery, jobsOnCard) is JobPick.ExistingRow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { choose(job.number) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(job.number, color = look.text, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(56.dp), maxLines = 1)
                    Text(job.name, color = look.text2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (onCard) Text("on card", color = look.doneText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Not on the floor
            if (showTyped) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, look.warn, RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { choose(trimmed) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(HoursIcons.Plus, null, tint = look.text, modifier = Modifier.size(18.dp))
                    Text("Use “${trimmed.uppercase()}”", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("not on the floor", color = look.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    look: StandardLook,
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(PillShape)
            .background(if (active) look.primary else look.soft)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (active) selected = true }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        val fg = if (active) look.onPrimary else look.softText
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TextStyle())
    }
}
