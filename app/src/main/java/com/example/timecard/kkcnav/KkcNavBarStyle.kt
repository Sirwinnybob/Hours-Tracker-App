package com.example.timecard.kkcnav

// Hours Tracker–only styling for the mirrored KKC navbar. NOT a copy of KKCSheetTracker code:
// KKC has no equivalent, so nothing here needs to be kept in sync with KKC. It decides WHERE the
// bar's colors come from (KKC's theme vs Hours Tracker's equipped theme vs the LCARS band) and
// picks the on-glass text color against Hours Tracker's own canvas.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.example.timecard.ui.theme.accentColorFor

/** Where the mirrored bar's colors come from. */
internal enum class KkcNavBarSource { KkcTheme, HoursTrackerTheme, Lcars }

/**
 * Standard Hours Tracker theme (no immersive theme equipped) → KKC's theme colors.
 * Any equipped immersive theme → that Hours Tracker theme's colors. LCARS → the LCARS band.
 * [accentKey] is the same key TimecardApp passes to TimecardTheme.
 */
internal fun kkcNavBarSource(accentKey: String?): KkcNavBarSource = when {
    accentKey == "lcars" -> KkcNavBarSource.Lcars
    accentKey != null && accentColorFor(accentKey) != null -> KkcNavBarSource.HoursTrackerTheme
    else -> KkcNavBarSource.KkcTheme
}

/** Resolved look of the glass bar: the color set, whether KKC bold-mode rules apply, and the on-glass color. */
internal data class KkcNavBarStyle(
    val colors: KkcNavBarColors,
    val boldMode: Boolean,
    val onGlass: Color
)

/**
 * Black or white for content on the glass, judged against what is actually behind it in Hours
 * Tracker (the glass tint at its fill alpha over HT's [canvas]). Same 0.179 threshold as KKC's
 * kkcFrostedContentColor, but KKC judges against KKC's own background, which is wrong here.
 */
internal fun kkcOnGlassColor(frostedBase: Color, fillAlpha: Float, canvas: Color): Color {
    val effective = frostedBase.copy(alpha = fillAlpha).compositeOver(canvas)
    return if (effective.luminance() > 0.179f) Color.Black else Color.White
}

/** An equipped Hours Tracker theme expressed as a navbar color set. */
internal fun kkcNavColorsFromHt(
    accent: Color,
    textSecondary: Color,
    pill: Color,
    surface: Color,
    error: Color,
    onError: Color
): KkcNavBarColors = KkcNavBarColors(
    primary = accent.toArgb(),
    onSurfaceVariant = textSecondary.toArgb(),
    surfaceVariant = pill.toArgb(),
    frostedBase = surface.toArgb(),
    // Unused for HT themes (they never use bold mode); kept so the set is complete.
    frostedContent = textSecondary.toArgb(),
    boldGradient = listOf(accent.toArgb(), accent.toArgb()),
    badgeContainer = error.toArgb(),
    badgeContent = onError.toArgb()
)

/**
 * KKC theme: the payload set matching Hours Tracker's effective light/dark, with KKC's bold mode.
 * Hours Tracker theme: [htColors], never bold. The on-glass color is always picked against [canvas].
 */
internal fun kkcNavBarStyle(
    payload: KkcNavBarPayload,
    source: KkcNavBarSource,
    darkTheme: Boolean,
    htColors: KkcNavBarColors,
    canvas: Color
): KkcNavBarStyle {
    val useKkc = source == KkcNavBarSource.KkcTheme
    val colors = if (useKkc) payload.colorsFor(darkTheme) else htColors
    return KkcNavBarStyle(
        colors = colors,
        boldMode = useKkc && payload.boldMode,
        onGlass = kkcOnGlassColor(Color(colors.frostedBase), kkcFrostedFillAlpha(payload), canvas)
    )
}
