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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

private val WaterSky = Color(0xFF0B1F33)
private val WaterDeep = Color(0xFF14566E)
private val WaterMid = Color(0xFF1E8FA3)
private val WaterFront = Color(0xFF52D6D0)
private val WaterFoam = Color(0xFFE8FFFB)
private const val BUBBLE_COUNT = 5

/**
 * 90s "aqua cam" screensaver panel: three stair-stepped, scrolling pixel waves with rising
 * bubbles. Chunky [pixel] steps keep it lo-fi. Freezes to a still frame when the system
 * animator scale is 0 (accessibility "remove animations").
 */
@Composable
fun RetroWater(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val animationsOn = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }

    val transition = rememberInfiniteTransition(label = "retroWater")
    val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )
    val animatedRise by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing), RepeatMode.Restart),
        label = "bubbleRise"
    )
    val phase = if (animationsOn) animatedPhase else 1.2f
    val rise = if (animationsOn) animatedRise else 0.3f

    Box(
        modifier = modifier
            .background(WaterSky)
            .beveledBorder(isInset = true, borderWidth = 1.dp)
            .clipToBounds()
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawWaveLayer(WaterDeep, baseFrac = 0.42f, ampFrac = 0.07f, waves = 1.0f, phase = phase, direction = 1f)
            drawWaveLayer(WaterMid, baseFrac = 0.54f, ampFrac = 0.08f, waves = 1.7f, phase = phase * 2f, direction = -1f)
            drawWaveLayer(WaterFront, baseFrac = 0.68f, ampFrac = 0.07f, waves = 2.4f, phase = phase, direction = 1f, foam = true)
            drawBubbles(rise)
        }
        Text(
            text = "~ AQUA CAM ~",
            fontFamily = VT323FontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = WaterFoam.copy(alpha = 0.85f),
            modifier = Modifier.padding(start = 4.dp, top = 1.dp)
        )
    }
}

private fun DrawScope.drawWaveLayer(
    color: Color,
    baseFrac: Float,
    ampFrac: Float,
    waves: Float,
    phase: Float,
    direction: Float,
    foam: Boolean = false
) {
    val pixel = 3.dp.toPx()
    val columns = (size.width / pixel).toInt() + 1
    val base = size.height * baseFrac
    val amp = size.height * ampFrac
    for (i in 0 until columns) {
        val x = i * pixel
        val angle = (x / size.width) * 2f * PI.toFloat() * waves + phase * direction
        val rawTop = base + amp * sin(angle)
        val top = floor(rawTop / pixel) * pixel
        drawRect(color, topLeft = Offset(x, top), size = Size(pixel, size.height - top))
        if (foam) drawRect(WaterFoam, topLeft = Offset(x, top), size = Size(pixel, pixel * 0.6f))
    }
}

private fun DrawScope.drawBubbles(rise: Float) {
    val pixel = 3.dp.toPx()
    for (i in 0 until BUBBLE_COUNT) {
        val offset = (rise + i / BUBBLE_COUNT.toFloat()) % 1f
        val y = size.height - offset * (size.height + pixel * 2f)
        val x = size.width * (0.12f + 0.19f * i) + sin(offset * 2f * PI.toFloat() * 2f + i) * pixel * 1.5f
        val snappedX = floor(x / pixel) * pixel
        val snappedY = floor(y / pixel) * pixel
        val radius = if (i % 2 == 0) pixel else pixel * 0.7f
        drawRect(
            color = WaterFoam.copy(alpha = 0.8f),
            topLeft = Offset(snappedX, snappedY),
            size = Size(radius * 1.6f, radius * 1.6f),
            style = Stroke(width = 1f)
        )
    }
}
