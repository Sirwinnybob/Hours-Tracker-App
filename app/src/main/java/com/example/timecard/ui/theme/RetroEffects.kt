package com.example.timecard.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

// ── Shared motion plumbing ────────────────────────────────────────────────────

/** False when the system animator scale is 0 (accessibility "remove animations"). */
@Composable
fun rememberRetroMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}

/**
 * Looping 0..1 clock. Returned as State so draw-phase readers (Canvas, drawBehind,
 * graphicsLayer) animate without recomposing. Holds a still frame when motion is off.
 */
@Composable
fun rememberRetroClock(durationMs: Int): State<Float> {
    val motion = rememberRetroMotion()
    val transition = rememberInfiniteTransition(label = "retroClock")
    val running = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "clock"
    )
    val still = remember { mutableStateOf(0.3f) }
    return if (motion) running else still
}

@Composable
fun rememberRetroBlink(periodMs: Int = 1000): Boolean {
    val clock by rememberRetroClock(periodMs)
    return clock < 0.5f
}

/** Hue-cycling background drawn in the draw phase (no recomposition). */
fun Modifier.retroRainbowBackground(
    durationMs: Int = 2500,
    saturation: Float = 0.45f,
    value: Float = 1f
): Modifier = composed {
    val clock = rememberRetroClock(durationMs)
    drawBehind { drawRect(Color.hsv(clock.value * 360f, saturation, value)) }
}

// ── Drifting pixel clouds ─────────────────────────────────────────────────────

// (first column, column count) per pixel row of a chunky cloud
private val CloudRows = listOf(3 to 4, 1 to 8, 0 to 10, 0 to 10, 1 to 8)
private const val CLOUD_COLS = 10

private class CloudSpec(val yFrac: Float, val speed: Int, val offset: Float, val scale: Float)

private val Clouds = listOf(
    CloudSpec(0.04f, 1, 0.00f, 1.0f),
    CloudSpec(0.18f, 2, 0.55f, 0.7f),
    CloudSpec(0.33f, 1, 0.30f, 1.3f),
    CloudSpec(0.49f, 3, 0.80f, 0.6f),
    CloudSpec(0.63f, 2, 0.15f, 1.0f),
    CloudSpec(0.78f, 1, 0.65f, 1.4f),
    CloudSpec(0.90f, 3, 0.40f, 0.8f)
)

/** Sky-blue backdrop with pixel clouds drifting right at three parallax speeds. */
@Composable
fun RetroClouds(modifier: Modifier = Modifier, sky: Color = RetroSky) {
    val clock = rememberRetroClock(60_000)
    Canvas(modifier.fillMaxSize().background(sky).clipToBounds()) {
        val t = clock.value
        Clouds.forEach { cloud ->
            val unit = 4.dp.toPx() * cloud.scale
            val cloudWidth = unit * CLOUD_COLS
            val span = size.width + cloudWidth
            val x = ((t * cloud.speed + cloud.offset) % 1f) * span - cloudWidth
            val y = floor(size.height * cloud.yFrac / unit) * unit
            drawCloud(x, y, unit)
        }
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, unit: Float) {
    CloudRows.forEachIndexed { row, (firstCol, cols) ->
        val shade = if (row == CloudRows.lastIndex) Color(0xFFC9D6EA) else Color.White
        drawRect(shade, Offset(x + firstCol * unit, y + row * unit), Size(cols * unit, unit))
    }
}

// ── Rainbow / blink / sparkle text bits ───────────────────────────────────────

@Composable
fun RetroRainbowText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    durationMs: Int = 2800
) {
    val clock by rememberRetroClock(durationMs)
    Text(
        text = text,
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = 1.sp,
        color = Color.hsv(clock * 360f, 0.55f, 1f),
        maxLines = 1,
        modifier = modifier
    )
}

/** Twinkling four-point star; give each one a different [phase] (0..1) so they don't pulse together. */
@Composable
fun RetroSparkle(
    phase: Float,
    modifier: Modifier = Modifier,
    color: Color = RetroYellow,
    glyph: String = "✦",
    fontSize: TextUnit = 14.sp
) {
    val clock = rememberRetroClock(1600)
    Text(
        text = glyph,
        color = color,
        fontSize = fontSize,
        modifier = modifier.graphicsLayer {
            val pulse = sin(((clock.value + phase) % 1f) * 2f * PI.toFloat()) * 0.5f + 0.5f
            scaleX = 0.5f + pulse
            scaleY = 0.5f + pulse
            alpha = 0.25f + 0.75f * pulse
        }
    )
}

/** Spinning earth, the classic "page is loading" icon. */
@Composable
fun RetroGlobe(modifier: Modifier = Modifier, fontSize: TextUnit = 16.sp) {
    val frames = remember { listOf("🌍", "🌎", "🌏") }
    val clock by rememberRetroClock(1500)
    Text(frames[(clock * frames.size).toInt().coerceIn(0, frames.lastIndex)], fontSize = fontSize, modifier = modifier)
}

/** Scrolling yellow/black hazard tape with a blinking "UNDER CONSTRUCTION" caption. */
@Composable
fun RetroConstructionTape(modifier: Modifier = Modifier) {
    val clock = rememberRetroClock(900)
    val blink = rememberRetroBlink(1100)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .beveledBorder(isInset = true, borderWidth = 1.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black)
            val stripe = 8.dp.toPx()
            val shift = clock.value * stripe * 2f
            var x = -stripe * 2f + shift
            while (x < size.width) {
                drawRect(RetroYellow, Offset(x, 0f), Size(stripe, size.height))
                x += stripe * 2f
            }
        }
        if (blink) {
            Text(
                text = "UNDER CONSTRUCTION",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.Black,
                modifier = Modifier.background(RetroYellow).padding(horizontal = 3.dp)
            )
        }
    }
}

// ── Visitor counter with LED-gradient digits that actually tick ───────────────

private val DigitGradient = Brush.verticalGradient(
    listOf(Color(0xFF5B34F5), Color(0xFFFF4FD0), Color(0xFFFFE04A))
)

@Composable
fun RetroTickingCounter(modifier: Modifier = Modifier, start: Int = 1995) {
    val motion = rememberRetroMotion()
    var visitors by remember { mutableStateOf(start) }
    LaunchedEffect(motion) {
        while (motion) {
            delay(3500)
            visitors += 1
        }
    }
    Row(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)) {
        visitors.toString().padStart(6, '0').forEach { digit ->
            Box(
                modifier = Modifier
                    .size(width = 17.dp, height = 24.dp)
                    .background(DigitGradient)
                    .beveledBorder(isInset = true, borderWidth = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    fontFamily = VT323FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}
