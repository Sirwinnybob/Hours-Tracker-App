package com.example.timecard.ui.timesheet.standard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.timecard.kkcnav.KkcNavBarColors
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.TimecardColors
import kotlin.math.abs

/**
 * Colors for the standard (non-LCARS, non-Retro) timesheet layout, which follows KKCSheetTracker's
 * look (kkc-layout-direction): white cards on a tinted canvas, a segmented week switch, tonal chips,
 * green/amber day status.
 *
 * Where they come from (TimecardApp decides, see [standardLookSource]):
 * - no Hours Tracker theme equipped and launched from KKC → KKC's own theme colors (navbar payload);
 * - an equipped Hours Tracker theme, or no payload → that Hours Tracker theme.
 */
@Immutable
data class StandardLook(
    val isDark: Boolean,
    val background: Brush,
    val header: Color,
    val surface: Color,
    /** Header rows, totals column, tile fill. */
    val head: Color,
    val line: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val empty: Color,
    /** Segmented-switch track, today outline, primary buttons. */
    val primary: Color,
    val onPrimary: Color,
    val segSelected: Color,
    val onSegSelected: Color,
    /** Tonal buttons/chips. */
    val soft: Color,
    val softText: Color,
    val today: Color,
    val todayHead: Color,
    val weekBar: Brush,
    val done: Color,
    val doneText: Color,
    val doneSoft: Color,
    val progress: Color,
    val progressText: Color,
    val warn: Color,
    val shadow: Color
)

/** Set by TimecardApp. Null → derive from the current Hours Tracker theme. */
val LocalStandardLook = staticCompositionLocalOf<StandardLook?> { null }

@Composable
fun currentStandardLook(): StandardLook =
    LocalStandardLook.current ?: standardLookFromTimecard(LocalTimecardColors.current)

private val NEAR_BLACK = Color(0xFF1A1F1C)
private val DARK_BASE = Color(0xFF121413)

private val LIGHT_STATUS = Triple(Color(0xFF2E7D32), Color(0xFF1F5E36), Color(0xFFD7EBD9))
private val DARK_STATUS = Triple(Color(0xFF66BB6A), Color(0xFF8FD694), Color(0x2E66BB6A))
private val LIGHT_PROGRESS = Color(0xFFE0A800) to Color(0xFF8A5A00)
private val DARK_PROGRESS = Color(0xFFFFB74D) to Color(0xFFFFCC80)

private fun onColor(c: Color): Color = if (c.luminance() > 0.45f) NEAR_BLACK else Color.White

private fun differs(a: Color, b: Color): Boolean =
    abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue) > 0.35f

/** KKC theme colors (one light/dark half of the navbar payload) as a standard look. */
fun standardLookFromKkc(colors: KkcNavBarColors, dark: Boolean): StandardLook {
    val primary = Color(colors.primary)
    val stops = colors.boldGradient.map { Color(it) }.ifEmpty { listOf(primary) }
    val accentEnd = stops.last()
    val weekBar = if (stops.size > 1) Brush.horizontalGradient(stops) else Brush.horizontalGradient(listOf(primary, primary))
    val (done, doneText, doneSoft) = if (dark) DARK_STATUS else LIGHT_STATUS
    val (prog, progText) = if (dark) DARK_PROGRESS else LIGHT_PROGRESS
    return if (!dark) {
        val surface = Color.White
        val segSelected = if (differs(accentEnd, primary)) accentEnd else surface
        StandardLook(
            isDark = false,
            background = Brush.verticalGradient(listOf(lerp(surface, primary, 0.10f), surface)),
            header = lerp(surface, primary, 0.04f),
            surface = surface,
            head = lerp(surface, primary, 0.06f),
            line = lerp(Color(0xFFE6E8E7), primary, 0.10f),
            border = lerp(surface, primary, 0.45f),
            text = NEAR_BLACK,
            text2 = lerp(Color(0xFF4A504C), primary, 0.25f),
            empty = Color(0xFFA9B1AC),
            primary = primary,
            onPrimary = onColor(primary),
            segSelected = segSelected,
            onSegSelected = onColor(segSelected),
            soft = lerp(surface, primary, 0.14f),
            softText = lerp(primary, Color.Black, 0.35f),
            today = lerp(surface, primary, 0.07f),
            todayHead = lerp(surface, primary, 0.15f),
            weekBar = weekBar,
            done = done, doneText = doneText, doneSoft = doneSoft,
            progress = prog, progressText = progText,
            warn = Color(0xFFB45309),
            shadow = Color(0x1A10200F)
        )
    } else {
        val surface = lerp(DARK_BASE, primary, 0.08f)
        val segSelected = if (differs(accentEnd, primary)) accentEnd else lerp(primary, Color.White, 0.55f)
        StandardLook(
            isDark = true,
            background = Brush.verticalGradient(listOf(Color.Black, lerp(Color.Black, primary, 0.08f))),
            header = lerp(DARK_BASE, primary, 0.10f),
            surface = surface,
            head = lerp(DARK_BASE, primary, 0.14f),
            line = lerp(DARK_BASE, primary, 0.22f),
            border = lerp(DARK_BASE, primary, 0.42f),
            text = Color(0xFFECEFED),
            text2 = Color(0xFFA9B2AD),
            empty = Color(0xFF5F6B66),
            primary = primary,
            onPrimary = onColor(primary),
            segSelected = segSelected,
            onSegSelected = onColor(segSelected),
            soft = lerp(DARK_BASE, primary, 0.25f),
            softText = lerp(primary, Color.White, 0.35f),
            today = lerp(DARK_BASE, primary, 0.16f),
            todayHead = lerp(DARK_BASE, primary, 0.26f),
            weekBar = weekBar,
            done = done, doneText = doneText, doneSoft = doneSoft,
            progress = prog, progressText = progText,
            warn = Color(0xFFFFB74D),
            shadow = Color(0x59000000)
        )
    }
}

/** An Hours Tracker theme (standard light/dark/OLED or an equipped Shop theme) as a standard look. */
fun standardLookFromTimecard(c: TimecardColors): StandardLook {
    val accent = c.accent
    val (done, doneText, doneSoft) = if (c.isDark) DARK_STATUS else LIGHT_STATUS
    val (prog, progText) = if (c.isDark) DARK_PROGRESS else LIGHT_PROGRESS
    val segSelected = if (c.isDark) c.textTotal else c.surface
    return StandardLook(
        isDark = c.isDark,
        background = c.backgroundBrush,
        header = c.surface,
        surface = c.surface,
        head = lerp(c.surface, accent, if (c.isDark) 0.10f else 0.05f),
        line = lerp(c.surface, c.border, 0.7f),
        border = c.border,
        text = c.textPrimary,
        text2 = c.textSecondary,
        empty = lerp(c.textSecondary, c.surface, 0.45f),
        primary = accent,
        onPrimary = onColor(accent),
        segSelected = segSelected,
        onSegSelected = onColor(segSelected),
        soft = lerp(c.surface, accent, if (c.isDark) 0.25f else 0.14f),
        softText = c.textTotal,
        today = lerp(c.surface, accent, if (c.isDark) 0.16f else 0.08f),
        todayHead = lerp(c.surface, accent, if (c.isDark) 0.26f else 0.16f),
        weekBar = Brush.horizontalGradient(listOf(accent, lerp(accent, Color.White, 0.4f))),
        done = done, doneText = doneText, doneSoft = doneSoft,
        progress = prog, progressText = progText,
        warn = c.textOrange,
        shadow = if (c.isDark) Color(0x59000000) else Color(0x1A101830)
    )
}

/**
 * Which colors the standard layout uses. Mirrors the navbar rule (kkcnav/KkcNavBarStyle.kt):
 * KKC's theme only when no Hours Tracker theme is equipped and KKC sent its colors.
 */
fun standardLookSource(followKkcTheme: Boolean, kkcColors: KkcNavBarColors?, darkTheme: Boolean, ht: TimecardColors): StandardLook =
    if (followKkcTheme && kkcColors != null) standardLookFromKkc(kkcColors, darkTheme) else standardLookFromTimecard(ht)
