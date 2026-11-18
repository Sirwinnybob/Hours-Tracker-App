package com.example.timecard.kkcnav

// ═══════════════════════════════════════════════════════════════════════════════════════════
// KEEP IN SYNC — COPIES OF THE SYSTEM ITEMS ON TOP OF KKCSHEETTRACKER'S LANDSCAPE RAIL
// Source of truth: C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\
//   ui/components/SideHeader.kt        SideHeaderClock  → KkcRailClock
//   ui/components/BatteryIndicator.kt  BatteryIndicator → KkcRailBattery (glyph copied verbatim)
//   ui/theme/KKCColors.kt              Light/DarkStatusColors.complete → kkcChargingColor
// Full rules: Hours Tracker AGENTS.md "KKC navbar mirror".
// ═══════════════════════════════════════════════════════════════════════════════════════════

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/** KKC SideHeaderClock: "1:35" over "PM". Ticks every 30 s. */
@Composable
internal fun KkcRailClock(color: Color) {
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val amPmFmt = remember { SimpleDateFormat("a", Locale.getDefault()) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = Date()
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFmt.format(now),
            style = KkcTitleMediumStyle,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        Text(
            text = amPmFmt.format(now),
            style = KkcNavLabelStyle,
            color = color.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

/**
 * KKC's default status `complete` color (the charging bolt). KKC themes can override it; the
 * payload doesn't carry it, so Hours Tracker uses KKC's built-in light/dark value.
 */
internal fun kkcChargingColor(darkTheme: Boolean): Color =
    if (darkTheme) Color(0xFF66BB6A) else Color(0xFF388E3C)

internal data class KkcBatteryStatus(val level: Int = 100, val isCharging: Boolean = false)

@Composable
private fun rememberKkcBatteryStatus(): State<KkcBatteryStatus> {
    val context = LocalContext.current
    val batteryState = remember { mutableStateOf(KkcBatteryStatus()) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let { parseKkcBatteryIntent(it, batteryState) }
            }
        }
        val initialIntent = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        initialIntent?.let { parseKkcBatteryIntent(it, batteryState) }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }
    return batteryState
}

private fun parseKkcBatteryIntent(intent: Intent, state: MutableState<KkcBatteryStatus>) {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    state.value = KkcBatteryStatus(level = pct, isCharging = isCharging)
}

/** KKC BatteryIndicator (showPercentage = true, contentColor = the rail's on-glass color). */
@Composable
internal fun KkcRailBattery(contentColor: Color, chargingColor: Color, modifier: Modifier = Modifier) {
    val batteryStatus by rememberKkcBatteryStatus()
    val level = batteryStatus.level
    val isCharging = batteryStatus.isCharging
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .padding(horizontal = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Battery $level%${if (isCharging) ", Charging" else ""}"
            }
    ) {
        if (isCharging) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = chargingColor,
                modifier = Modifier.size(16.dp)
            )
        }
        KkcBatteryGlyph(level = level, outlineColor = contentColor, fillColor = kkcBatteryFillColor(level))
    }
}

/** KKC batteryFillColor: hue sweeps red (0%) → amber → yellow → green (100%). */
internal fun kkcBatteryFillColor(level: Int): Color {
    val t = level.coerceIn(0, 100) / 100f
    val hue = 120f * t
    val saturation = 0.85f - 0.15f * t
    val value = 0.95f - 0.20f * t
    return Color.hsv(hue, saturation, value)
}

/** KKC BatteryGlyph: casing + cap, fill proportional to [level], percentage that flips color over the fill. */
@Composable
private fun KkcBatteryGlyph(level: Int, outlineColor: Color, fillColor: Color) {
    val textMeasurer = rememberTextMeasurer()
    val fillTextColor = if (fillColor.luminance() > 0.45f) Color.Black else Color.White
    val textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    val label = "$level%"

    Canvas(modifier = Modifier.size(width = 48.dp, height = 22.dp)) {
        val stroke = 2.dp.toPx()
        val capWidth = 3.5.dp.toPx()
        val capHeight = size.height * 0.46f
        val inset = 1.5.dp.toPx()
        val bodyWidth = size.width - capWidth
        val bodyCorner = 3.dp.toPx()

        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = CornerRadius(bodyCorner),
            style = Stroke(width = stroke)
        )

        val capCorner = CornerRadius(capWidth * 0.6f)
        val capPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = bodyWidth - stroke / 2f,
                    top = (size.height - capHeight) / 2f,
                    right = size.width,
                    bottom = (size.height + capHeight) / 2f,
                    topLeftCornerRadius = CornerRadius.Zero,
                    topRightCornerRadius = capCorner,
                    bottomRightCornerRadius = capCorner,
                    bottomLeftCornerRadius = CornerRadius.Zero
                )
            )
        }
        drawPath(capPath, color = outlineColor)

        val innerLeft = stroke + inset
        val innerTop = stroke + inset
        val innerWidth = bodyWidth - 2f * (stroke + inset)
        val innerHeight = size.height - 2f * (stroke + inset)
        val fillWidth = innerWidth * (level.coerceIn(0, 100) / 100f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(innerLeft, innerTop),
                size = Size(fillWidth, innerHeight),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }

        val layout = textMeasurer.measure(label, textStyle)
        val textTopLeft = Offset(
            x = (bodyWidth - layout.size.width) / 2f,
            y = (size.height - layout.size.height) / 2f
        )
        val split = innerLeft + fillWidth
        clipRect(left = split, top = 0f, right = size.width, bottom = size.height) {
            drawText(layout, color = outlineColor, topLeft = textTopLeft)
        }
        clipRect(left = 0f, top = 0f, right = split, bottom = size.height) {
            drawText(layout, color = fillTextColor, topLeft = textTopLeft)
        }
    }
}
