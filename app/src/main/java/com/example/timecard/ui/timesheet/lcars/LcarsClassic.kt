package com.example.timecard.ui.timesheet.lcars

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.AntonioFontFamily
import kotlinx.coroutines.delay

/**
 * LCARS "Classic" (TNG) palette and building blocks for the Hours Tracker LCARS theme, after
 * thelcars.com's Classic template (agreed in the 2026-11-19 mockup): thick elbows facing each other
 * across a split double bar, coded side panels, fat pills with their label bottom-right, text bars,
 * and level "tanks" for the days.
 */
object LcarsClassic {
    val Violet = Color(0xFFBAA4E5)
    val Almond = Color(0xFFD29B7F)
    val Creme = Color(0xFFFCC19F)
    val Barley = Color(0xFFEDB378)
    val Bluey = Color(0xFF8899FF)
    val Butter = Color(0xFFEA9C72)
    val Lilac = Color(0xFF8A72A7)
    val Orange = Color(0xFFEB943A)
    val Red = Color(0xFFCF4F4F)
    val Sienna = Color(0xFFC47D69)
    val Mauve = Color(0xFFC082A9)
    /** Off / disabled pill. */
    val Dim = Color(0xFF3A3450)
    val DimText = Color(0xFF6C6290)
    /** Unlit tank segment. */
    val Track = Color(0xFF17131F)
    /** Today's column wash. */
    val TodayWash = Color(0xFF1A1526)
    val Muted = Color(0xFF5A5070)

    /** Job pill colors, cycled per row (SHOP is always Bluey). */
    val RowColors = listOf(Butter, Violet, Mauve, Barley, Sienna, Almond)

    const val FRAME_ENTER_MS = 320
    const val TANK_SEGMENT_MS = 45
}

internal fun lcarsText(size: TextUnit, color: Color, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = AntonioFontFamily, fontSize = size, fontWeight = weight, color = color, letterSpacing = 0.6.sp)

/**
 * Uptime (ms) the LCARS entrance waits for. Set by the frame when KKC launched us, so the sweep-in
 * starts after the KKC → Hours window cross-fade instead of playing hidden behind it. A deadline, not a
 * delay: screens opened later (Shop, Profile…) don't wait again.
 */
val LocalLcarsEntranceGate = androidx.compose.runtime.compositionLocalOf { 0L }

/** False when KKC asked for animations off; set by the frame for everything inside it. */
val LocalLcarsAnimate = androidx.compose.runtime.compositionLocalOf { true }

/** Milliseconds still to wait for the entrance gate (0 once it has passed). */
@Composable
internal fun lcarsGateWaitMs(): Long {
    val gate = LocalLcarsEntranceGate.current
    return (gate - android.os.SystemClock.uptimeMillis()).coerceAtLeast(0L)
}

/** KKC → Hours Tracker window fade (android fade_in) the entrance waits out. */
const val LCARS_LAUNCH_FADE_MS = 520

/**
 * 0→1 entrance progress that starts after [delayMs] the first time it composes; always 1 when
 * [enabled] is false (KKC "animations off"), so nothing is ever left half-drawn.
 */
@Composable
internal fun rememberEntrance(enabled: Boolean, delayMs: Int, durationMs: Int = LcarsClassic.FRAME_ENTER_MS): Float {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    val gate = LocalLcarsEntranceGate.current
    LaunchedEffect(enabled) {
        if (!enabled) { progress.snapTo(1f); return@LaunchedEffect }
        delay((gate - android.os.SystemClock.uptimeMillis()).coerceAtLeast(0L) + delayMs)
        progress.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing))
    }
    return progress.value
}

/** Section header: rounded cap, title, long bar (thelcars "text bar"). */
@Composable
internal fun LcarsTextBar(title: String, capColor: Color, barColor: Color, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(24.dp).height(30.dp).clip(RoundedCornerShape(topStart = 15.dp, bottomStart = 15.dp)).background(capColor))
        Text(title.uppercase(), style = lcarsText(26.sp, LcarsClassic.Barley, FontWeight.Normal), modifier = Modifier.padding(horizontal = 6.dp))
        Box(Modifier.weight(1f).height(30.dp).background(barColor))
        if (trailing != null) trailing()
    }
}

/**
 * Header for LCARS screens shown inside the frame (Stats, Board, Targets, Alerts…): rounded cap,
 * big thin title, long bar, and a CLOSE pill with a rounded right end — a Classic text bar.
 * Slides in from the left once when the screen opens.
 */
@Composable
fun LcarsScreenHeader(title: String, onClose: () -> Unit, color: Color = LcarsClassic.Orange) {
    val enter = rememberEntrance(LocalLcarsAnimate.current, 0, durationMs = 260)
    Row(
        Modifier.fillMaxWidth().height(44.dp).graphicsLayer { alpha = enter; translationX = (1f - enter) * -60f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.width(28.dp).height(36.dp).clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)).background(color))
        Text(title.uppercase(), style = lcarsText(32.sp, color, FontWeight.Normal), maxLines = 1, modifier = Modifier.padding(horizontal = 4.dp))
        Box(Modifier.weight(1f).height(36.dp).background(color))
        LcarsPill(
            "CLOSE", LcarsClassic.Red, onClose,
            Modifier.width(96.dp), height = 36.dp,
            shape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
        )
    }
}

/** "• SAVED" in the header; blinks while saving, red on error; tap saves now (or retries). */
@Composable
fun LcarsSaveStatus(status: com.example.timecard.ui.timesheet.SaveStatus, animate: Boolean, onSave: () -> Unit) {
    val syncing = status == com.example.timecard.ui.timesheet.SaveStatus.SYNCING
    val blink = if (syncing && animate) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "saveBlink")
        t.animateFloat(1f, 0.2f, androidx.compose.animation.core.infiniteRepeatable(tween(450), androidx.compose.animation.core.RepeatMode.Reverse), label = "saveBlinkA").value
    } else 1f
    val (label, color) = when (status) {
        com.example.timecard.ui.timesheet.SaveStatus.SAVED -> "SAVED" to LcarsClassic.Creme
        com.example.timecard.ui.timesheet.SaveStatus.SYNCING -> "SAVING" to LcarsClassic.Orange
        com.example.timecard.ui.timesheet.SaveStatus.ERROR -> "SAVE ERROR · RETRY" to LcarsClassic.Red
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(" • ", style = lcarsText(14.sp, LcarsClassic.Bluey))
        Text(
            label,
            style = lcarsText(14.sp, color),
            modifier = Modifier
                .graphicsLayer { alpha = blink }
                .clickable(enabled = !syncing, role = Role.Button, onClickLabel = "Save now", onClick = onSave)
                .padding(vertical = 10.dp, horizontal = 4.dp)
        )
    }
}

/** Fat LCARS pill with its label bottom-right (thelcars buttons). */
@Composable
internal fun LcarsPill(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    shape: Shape = RoundedCornerShape(50),
    textColor: Color = Color.Black,
    enabled: Boolean = true,
    fontSize: TextUnit = 15.sp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(if (enabled) color else LcarsClassic.Dim)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
    ) {
        Text(
            label.uppercase(),
            style = lcarsText(fontSize, if (enabled) textColor else LcarsClassic.DimText),
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 6.dp)
        )
    }
}

/** Slides in from the left and fades up once, [delayMs] after the screen shows (staggered rows). */
@Composable
internal fun Modifier.lcarsSlideIn(delayMs: Int, durationMs: Int = 240): Modifier {
    val p = rememberEntrance(LocalLcarsAnimate.current, delayMs, durationMs)
    return graphicsLayer { alpha = p; translationX = (1f - p) * -48f }
}
