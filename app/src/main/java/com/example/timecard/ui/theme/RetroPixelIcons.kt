package com.example.timecard.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** The 16x16 pixel-art glyphs used by the Retro theme's sidebar buttons. */
enum class RetroIcon {
    Clock, Bars, Trophy, Target, Chart, Bag, Gear, Floppy, Door,
    // The KKC navbar glyphs, redrawn: tiles+ring, cabinet, magnifier, clock wedge, RTC card, forklift, binder, calculator
    Dashboard, Cabinet, Search, Calc, HoursClock, TimeCard, Forklift, Binder;

    companion object {
        /** Maps the emoji the nav buttons were originally written with (variation selector ignored). */
        fun fromEmoji(emoji: String): RetroIcon? = when (emoji.replace("️", "")) {
            "🕒" -> Clock
            "📊" -> Bars
            "🏆" -> Trophy
            "🎯" -> Target
            "📈" -> Chart
            "🛍" -> Bag
            "⚙" -> Gear
            "💾" -> Floppy
            "🚪" -> Door
            else -> null
        }

        /** The pixel icon for a KKC navbar destination route (Calc is the synthetic "calculator" slot). */
        fun forKkcRoute(route: String): RetroIcon? = when (route) {
            "dashboard" -> Dashboard
            "jobs" -> Cabinet
            "search" -> Search
            "calculator" -> Calc
            "hours" -> HoursClock
            "timecard" -> TimeCard
            "supply" -> Forklift
            "settings" -> Gear
            "standards" -> Binder
            else -> null
        }
    }
}

internal const val RETRO_ICON_GRID = 16

internal class Px(val color: Color, val x: Float, val y: Float, val w: Float, val h: Float)

private val Ink = Color(0xFF101040)
private val White = Color(0xFFFFFFFF)
private val Sky = Color(0xFF7FD0FF)
private val Blue = Color(0xFF2A5BFF)
private val Yellow = Color(0xFFFFE02A)
private val Orange = Color(0xFFFF9A1A)
private val Red = Color(0xFFE02828)
private val Pink = Color(0xFFFF5FD2)
private val Green = Color(0xFF2ED35A)
private val Silver = Color(0xFFC8CCD8)
private val DarkSilver = Color(0xFF7C8096)
private val Brown = Color(0xFF8A4B1E)

private fun r(c: Color, x: Int, y: Int, w: Int, h: Int) = Px(c, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat())

/** Pixel-snapped filled circle centred on the grid. */
private fun disc(c: Color, radius: Float, cx: Float = 8f, cy: Float = 8f): List<Px> =
    (0 until RETRO_ICON_GRID).mapNotNull { y ->
        val dy = y + 0.5f - cy
        val d = radius * radius - dy * dy
        if (d <= 0f) return@mapNotNull null
        val half = sqrt(d)
        val x0 = (cx - half).roundToInt().coerceAtLeast(0)
        val x1 = (cx + half).roundToInt().coerceAtMost(RETRO_ICON_GRID)
        if (x1 > x0) Px(c, x0.toFloat(), y.toFloat(), (x1 - x0).toFloat(), 1f) else null
    }

/** 1px Bresenham line, [thickness] pixels tall. */
private fun line(c: Color, x0: Int, y0: Int, x1: Int, y1: Int, thickness: Int = 1): List<Px> {
    val out = mutableListOf<Px>()
    var x = x0
    var y = y0
    val dx = abs(x1 - x0)
    val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1
    var err = dx + dy
    while (true) {
        out += r(c, x, y, 1, thickness)
        if (x == x1 && y == y1) break
        val e2 = 2 * err
        if (e2 >= dy) { err += dy; x += sx }
        if (e2 <= dx) { err += dx; y += sy }
    }
    return out
}

internal fun retroIconRects(icon: RetroIcon): List<Px> = when (icon) {
    RetroIcon.Clock ->
        disc(Ink, 8f) + disc(White, 7f) +
            listOf(r(Red, 7, 3, 2, 5), r(Red, 7, 7, 4, 2), r(Ink, 7, 7, 2, 2))

    RetroIcon.Bars -> listOf(
        r(Ink, 0, 0, 1, 15), r(Ink, 0, 14, 16, 1),
        r(Red, 2, 10, 3, 4), r(Yellow, 6, 6, 3, 8), r(Green, 10, 2, 3, 12),
        r(White, 2, 10, 1, 4), r(White, 6, 6, 1, 8), r(White, 10, 2, 1, 12)
    )

    RetroIcon.Trophy -> listOf(
        r(Orange, 2, 2, 2, 1), r(Orange, 2, 2, 1, 3), r(Orange, 2, 4, 2, 1),
        r(Orange, 12, 2, 2, 1), r(Orange, 13, 2, 1, 3), r(Orange, 12, 4, 2, 1),
        r(Yellow, 4, 1, 8, 6), r(Yellow, 5, 7, 6, 1), r(Yellow, 6, 8, 4, 1),
        r(White, 5, 2, 1, 4),
        r(Orange, 7, 9, 2, 3), r(Blue, 4, 12, 8, 2), r(Ink, 3, 14, 10, 1)
    )

    RetroIcon.Target ->
        disc(Ink, 8f) + disc(Red, 7f) + disc(White, 5f) + disc(Red, 3.2f) + disc(Yellow, 1.6f) +
            line(Brown, 14, 1, 9, 6) + listOf(r(Pink, 13, 0, 3, 2))

    RetroIcon.Chart ->
        listOf(r(Ink, 0, 0, 16, 16), r(Sky, 1, 1, 14, 14), r(White, 1, 5, 14, 1), r(White, 1, 9, 14, 1)) +
            line(Green, 2, 12, 5, 8, 2) + line(Green, 5, 8, 8, 10, 2) +
            line(Green, 8, 10, 11, 5, 2) + line(Green, 11, 5, 14, 2, 2) +
            listOf(r(Red, 4, 7, 2, 2), r(Red, 10, 4, 2, 2), r(Red, 13, 1, 2, 2))

    RetroIcon.Bag -> listOf(
        r(Ink, 5, 0, 6, 1), r(Ink, 4, 1, 1, 4), r(Ink, 11, 1, 1, 4),
        r(Ink, 2, 4, 12, 12), r(Pink, 3, 5, 10, 10), r(White, 4, 6, 1, 7),
        r(Ink, 6, 8, 4, 4), r(Yellow, 7, 9, 2, 2)
    )

    RetroIcon.Gear ->
        listOf(
            r(DarkSilver, 7, 0, 2, 3), r(DarkSilver, 7, 13, 2, 3),
            r(DarkSilver, 0, 7, 3, 2), r(DarkSilver, 13, 7, 3, 2),
            r(DarkSilver, 2, 2, 3, 3), r(DarkSilver, 11, 2, 3, 3),
            r(DarkSilver, 2, 11, 3, 3), r(DarkSilver, 11, 11, 3, 3)
        ) + disc(DarkSilver, 6f) + disc(Silver, 5f) + disc(Ink, 2.2f) + listOf(r(White, 4, 4, 2, 1))

    RetroIcon.Floppy -> listOf(
        r(Ink, 0, 0, 16, 16), r(Blue, 1, 1, 14, 14),
        r(Silver, 4, 1, 8, 5), r(Ink, 9, 2, 2, 3),
        r(White, 3, 8, 10, 7), r(Sky, 4, 10, 8, 1), r(Sky, 4, 12, 8, 1)
    )

    // Dashboard: 2x2 tiles, bottom-right tile is a 3/4 progress ring
    RetroIcon.Dashboard ->
        listOf(
            r(Ink, 0, 0, 7, 7), r(Pink, 1, 1, 5, 5), r(White, 1, 1, 2, 1),
            r(Ink, 9, 0, 7, 7), r(Orange, 10, 1, 5, 5), r(White, 10, 1, 2, 1),
            r(Ink, 0, 9, 7, 7), r(Sky, 1, 10, 5, 5), r(White, 1, 10, 2, 1)
        ) + disc(Ink, 3.6f, 12f, 12f) + disc(Green, 2.8f, 12f, 12f) +
            listOf(r(White, 9, 9, 3, 3)) + disc(White, 1.3f, 12f, 12f)

    // Jobs: base cabinet front, a drawer over a pair of doors
    RetroIcon.Cabinet -> listOf(
        r(Ink, 1, 1, 14, 14), r(Orange, 2, 2, 12, 12), r(White, 3, 3, 1, 3),
        r(Ink, 2, 7, 12, 1), r(Yellow, 6, 4, 4, 1),
        r(Ink, 7, 8, 2, 6), r(Yellow, 5, 10, 1, 3), r(Yellow, 10, 10, 1, 3),
        r(Brown, 2, 13, 12, 1)
    )

    // Search: magnifier with a lens glint
    RetroIcon.Search ->
        disc(Ink, 6.5f, 6.5f, 6.5f) + disc(Sky, 5.2f, 6.5f, 6.5f) +
            listOf(r(White, 3, 3, 3, 1), r(White, 3, 4, 1, 2)) +
            line(Ink, 10, 10, 13, 13, 3) + line(Brown, 10, 10, 13, 13, 2)

    // Calculator: display over operator keys (+ - x =)
    RetroIcon.Calc -> listOf(
        r(Ink, 2, 0, 12, 16), r(Blue, 3, 1, 10, 14), r(Green, 4, 2, 8, 3), r(White, 4, 2, 3, 1),
        r(Yellow, 4, 7, 3, 3), r(Pink, 9, 7, 3, 3), r(Sky, 4, 11, 3, 3), r(Orange, 9, 11, 3, 3),
        // + - x =
        r(Ink, 5, 7, 1, 3), r(Ink, 4, 8, 3, 1),
        r(Ink, 9, 8, 3, 1),
        r(Ink, 4, 11, 1, 1), r(Ink, 6, 11, 1, 1), r(Ink, 5, 12, 1, 1), r(Ink, 4, 13, 1, 1), r(Ink, 6, 13, 1, 1),
        r(Ink, 9, 11, 3, 1), r(Ink, 9, 13, 3, 1)
    )

    // Hours: clock with the 15-minute wedge filled
    RetroIcon.HoursClock ->
        disc(Ink, 8f) + disc(White, 7f) +
            listOf(
                r(Color(0xFF7A3CF0), 8, 2, 5, 6), r(Color(0xFF7A3CF0), 8, 1, 3, 1), r(Color(0xFF7A3CF0), 13, 3, 1, 4),
                r(Ink, 8, 2, 1, 6), r(Ink, 8, 8, 4, 1), r(Ink, 7, 7, 2, 2)
            )

    // Timeclock: RTC card with a swipe strip, numpad and action keys
    RetroIcon.TimeCard -> listOf(
        r(Ink, 1, 0, 14, 16), r(Silver, 2, 1, 12, 14), r(Ink, 3, 2, 10, 2), r(Yellow, 4, 3, 3, 1),
        r(Blue, 3, 5, 2, 2), r(Blue, 6, 5, 2, 2), r(Blue, 3, 8, 2, 2), r(Blue, 6, 8, 2, 2),
        r(Blue, 3, 11, 2, 2), r(Blue, 6, 11, 2, 2),
        r(Green, 10, 5, 3, 2), r(Yellow, 10, 8, 3, 2), r(Red, 10, 11, 3, 2)
    )

    // Supply: forklift carrying a load
    RetroIcon.Forklift ->
        listOf(
            r(Ink, 0, 12, 6, 1),
            r(Ink, 0, 5, 6, 7), r(Brown, 1, 6, 4, 5), r(Orange, 1, 8, 4, 1),
            r(Ink, 6, 2, 2, 11), r(Silver, 6, 3, 1, 9),
            r(Ink, 8, 8, 8, 5), r(Yellow, 9, 9, 6, 3),
            r(Ink, 10, 3, 5, 1), r(Ink, 10, 3, 1, 5), r(Ink, 14, 3, 1, 5), r(Red, 11, 6, 2, 2)
        ) + disc(Ink, 2.3f, 10f, 14f) + disc(Silver, 1f, 10f, 14f) +
            disc(Ink, 2.3f, 14f, 14f) + disc(Silver, 1f, 14f, 14f)

    // Library: binder with lines of text on the cover
    RetroIcon.Binder -> listOf(
        r(Ink, 2, 0, 12, 16), r(Blue, 3, 1, 2, 14), r(Green, 5, 1, 8, 14),
        r(White, 6, 4, 6, 1), r(White, 6, 6, 6, 1), r(White, 6, 8, 6, 1), r(White, 6, 10, 3, 1),
        r(Yellow, 6, 13, 2, 1)
    )

    RetroIcon.Door -> listOf(
        r(Ink, 2, 0, 9, 16), r(Brown, 3, 1, 7, 14), r(Orange, 4, 2, 2, 12), r(Yellow, 8, 8, 1, 2),
        r(Red, 10, 7, 5, 2), r(Red, 13, 5, 1, 6), r(Red, 14, 6, 1, 4)
    )
}

/** Draws a [RetroIcon] crisp at [size]; when [bounce] is true it bobs up and down. */
@Composable
fun RetroPixelIcon(
    icon: RetroIcon,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    bounce: Boolean = false
) {
    val clock = rememberRetroClock(700)
    val rects = androidx.compose.runtime.remember(icon) { retroIconRects(icon) }
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                if (bounce) translationY = -sin(clock.value * 2f * PI.toFloat()).coerceAtLeast(0f) * 3.dp.toPx()
            }
    ) {
        val unit = this.size.width / RETRO_ICON_GRID
        rects.forEach { px ->
            drawRect(px.color, Offset(px.x * unit, px.y * unit), Size(px.w * unit, px.h * unit))
        }
    }
}
