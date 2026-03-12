package com.example.timecard.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFrac: Float,    // 0..1 fraction of screen width
    val startYFrac: Float,    // 0..1 fraction of screen height (usually negative = above screen)
    val vxFrac: Float,        // horizontal velocity (fractions/sec)
    val vyFrac: Float,        // initial vertical velocity
    val color: Color,
    val sizePx: Float,
    val aspectRatio: Float,   // width/height of rectangle
    val rotation: Float,
    val rotationSpeed: Float  // degrees/sec
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFD4AF37), // gold
    Color(0xFF4ADE80), // green
    Color(0xFF60A5FA), // blue
    Color(0xFFF472B6), // pink
    Color(0xFFFBBF24), // amber
    Color(0xFFA78BFA), // violet
    Color(0xFFFB7185), // rose
)

private fun generateParticles(count: Int): List<ConfettiParticle> {
    val rng = Random.Default
    return List(count) {
        val angle = rng.nextFloat() * 360f
        val speed = 0.3f + rng.nextFloat() * 0.5f
        ConfettiParticle(
            startXFrac = rng.nextFloat(),
            startYFrac = -0.05f - rng.nextFloat() * 0.1f, // slightly above screen
            vxFrac = cos(Math.toRadians(angle.toDouble())).toFloat() * speed * 0.3f,
            vyFrac = speed * (0.4f + rng.nextFloat() * 0.3f), // always downward
            color = CONFETTI_COLORS[rng.nextInt(CONFETTI_COLORS.size)],
            sizePx = 12f + rng.nextFloat() * 14f,
            aspectRatio = 2f + rng.nextFloat() * 2f,
            rotation = rng.nextFloat() * 360f,
            rotationSpeed = (120f + rng.nextFloat() * 240f) * if (rng.nextBoolean()) 1f else -1f
        )
    }
}

@Composable
fun ConfettiBurst(
    trigger: Boolean,
    onDone: () -> Unit
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    val particles = remember { generateParticles(90) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(3200, easing = LinearEasing))
        onDone()
    }

    val t = progress.value
    val globalAlpha = if (t > 0.75f) 1f - ((t - 0.75f) / 0.25f) else 1f

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(97f)
    ) {
        val gravity = size.height * 0.8f // pixels/sec² normalised to animation duration
        particles.forEach { p ->
            val px = (p.startXFrac + p.vxFrac * t) * size.width
            val py = (p.startYFrac * size.height) + p.vyFrac * t * size.height + 0.5f * gravity * t * t
            val rot = p.rotation + p.rotationSpeed * t
            val w = p.sizePx * p.aspectRatio
            val h = p.sizePx

            if (py > size.height + h) return@forEach // off screen

            val alpha = (globalAlpha * 0.9f).coerceIn(0f, 1f)
            val color = p.color.copy(alpha = alpha)

            drawContext.canvas.save()
            drawContext.canvas.translate(px, py)
            drawContext.canvas.rotate(rot)
            drawRect(
                color = color,
                topLeft = Offset(-w / 2f, -h / 2f),
                size = Size(w, h)
            )
            drawContext.canvas.restore()
        }
    }
}
