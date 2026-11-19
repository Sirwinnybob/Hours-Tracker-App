package com.example.timecard.ui.timesheet.standard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.login.HeaderMetrics
import com.example.timecard.ui.theme.ThemeMode
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.ui.timesheet.SaveStatus

/**
 * Standard-layout header (KKC header pattern): avatar + name/week on the left; the week switch as a
 * segmented control; save status; icon actions; outlined Log out.
 *
 * The name itself is drawn by the login NameCard overlay flying into [onNameMeasured]'s position,
 * so this header only reserves (and measures) a transparent placeholder for it.
 */
@Composable
internal fun StandardHeader(
    look: StandardLook,
    nameForMeasure: String,
    weekLabel: String,
    isViewingPrevious: Boolean,
    canSwitchWeek: Boolean,
    onToggleWeek: () -> Unit,
    saveStatus: SaveStatus,
    onSave: () -> Unit,
    onStats: () -> Unit,
    onLeaderboard: (() -> Unit)?,
    onChallenges: (() -> Unit)?,
    onLogout: () -> Unit,
    themeState: ThemeState,
    showKkcBack: Boolean,
    onKkcBack: () -> Unit,
    avatar: @Composable () -> Unit,
    onNameMeasured: ((HeaderMetrics) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(3.dp, RoundedCornerShape(0.dp), ambientColor = look.shadow, spotColor = look.shadow)
            .background(look.header)
            .padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showKkcBack) {
            IconAction(HoursIcons.Back, "Back to KKC", look, onKkcBack, tonal = true)
        }
        avatar()
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Box(
                modifier = Modifier
                    .widthIn(min = 100.dp)
                    .height(24.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val sz = coords.size
                        onNameMeasured?.invoke(
                            HeaderMetrics(x = pos.x, centerY = pos.y + sz.height / 2f, widthPx = sz.width.toFloat(), heightPx = sz.height.toFloat())
                        )
                    }
            ) {
                Text(nameForMeasure, fontSize = 18.sp, maxLines = 1, softWrap = false, color = Color.Transparent)
            }
            Text(weekLabel, color = look.text2, fontSize = 13.sp, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        WeekSwitch(look, isViewingPrevious, canSwitchWeek, onToggleWeek)
        SaveStatusChip(look, saveStatus, onSave)
        IconAction(HoursIcons.Stats, "Stats", look, onStats, size = 42.dp)
        if (onLeaderboard != null) IconAction(HoursIcons.Leaderboard, "Leaderboard", look, onLeaderboard, size = 42.dp)
        if (onChallenges != null) IconAction(HoursIcons.Challenges, "Challenges", look, onChallenges, size = 42.dp)
        ThemeButton(look, themeState)
        PillButton("Log out", look, onLogout, kind = PillKind.Outlined, outlineColor = look.warn)
    }
}

/** This week / Last week segmented control. Fixed layout; only the highlight moves. */
@Composable
private fun WeekSwitch(look: StandardLook, isViewingPrevious: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(look.primary)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf("This week" to false, "Last week" to true).forEach { (label, isPrev) ->
            val selected = isPrev == isViewingPrevious
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) look.segSelected else Color.Transparent)
                    .clickable(enabled = enabled && !selected, role = Role.Tab, onClick = onToggle)
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = when {
                        selected -> look.onSegSelected
                        enabled -> look.onPrimary
                        else -> look.onPrimary.copy(alpha = 0.55f)
                    },
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

/** Autosave state. Tap saves now (and retries after an error). */
@Composable
private fun SaveStatusChip(look: StandardLook, status: SaveStatus, onSave: () -> Unit) {
    val (bg, fg) = when (status) {
        SaveStatus.SAVED -> look.doneSoft to look.doneText
        SaveStatus.SYNCING -> look.soft to look.softText
        SaveStatus.ERROR -> look.warn.copy(alpha = 0.15f) to look.warn
    }
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(PillShape)
            .background(bg)
            .clickable(role = Role.Button, onClickLabel = "Save now", onClick = onSave)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        AnimatedContent(targetState = status, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "saveStatus") { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                when (s) {
                    SaveStatus.SAVED -> Icon(HoursIcons.Check, null, tint = fg, modifier = Modifier.size(16.dp))
                    SaveStatus.SYNCING -> CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    SaveStatus.ERROR -> Icon(HoursIcons.Close, null, tint = fg, modifier = Modifier.size(16.dp))
                }
                Text(
                    when (s) {
                        SaveStatus.SAVED -> "Saved"
                        SaveStatus.SYNCING -> "Saving"
                        SaveStatus.ERROR -> "Retry"
                    },
                    color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Light/dark toggle (long press: OLED), same behavior as ThemeToggle without the emoji. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeButton(look: StandardLook, themeState: ThemeState) {
    val label = when (themeState.mode) {
        ThemeMode.Light -> "Switch to dark"
        ThemeMode.Dark -> "Switch to light"
        ThemeMode.Oled -> "Switch to light"
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = { themeState.toggleTheme() },
                onLongClick = { themeState.cycleDarkVariant() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(HoursIcons.Theme, contentDescription = label, tint = look.text, modifier = Modifier.size(22.dp))
    }
}

/** Avatar tile with the streak count badge (bottom-right), KKC-style rounded square. */
@Composable
internal fun AvatarTile(
    look: StandardLook,
    streak: Int,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(look.soft)
                .clickable(role = Role.Button, onClickLabel = "Profile and settings", onClick = onClick),
            contentAlignment = Alignment.Center
        ) { content() }
        if (streak >= 3) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(0.dp)
                    .height(18.dp)
                    .widthIn(min = 18.dp)
                    .clip(PillShape)
                    .background(Color(0xFFE65100))
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$streak", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
