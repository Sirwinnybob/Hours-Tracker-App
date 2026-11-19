package com.example.timecard.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.timecard.kkcnav.KkcInterFontFamily
import com.example.timecard.kkcnav.KkcNavBarColors

// "No custom theme → follow KKCSheetTracker" for the WHOLE app (2026-11-19): when the profile has no
// Hours Tracker theme equipped and KKC sent its colors, the standard light/dark/OLED palette is re-tinted
// with KKC's theme so every screen and popup matches KKC. An equipped Shop theme is never touched.

private val DARK_BASE = Color(0xFF121413)

/** The standard Hours Tracker palette re-tinted with KKC's theme colors (one light/dark half of the payload). */
fun TimecardColors.followingKkc(kkc: KkcNavBarColors): TimecardColors {
    val primary = Color(kkc.primary)
    val stops = kkc.boldGradient.map { Color(it) }.ifEmpty { listOf(primary) }
    val dark = isDark
    val surface = if (dark) lerp(DARK_BASE, primary, 0.08f) else Color.White
    return TimecardColors(
        backgroundFrom = if (dark) Color.Black else lerp(Color.White, primary, 0.10f),
        backgroundTo = if (dark) lerp(Color.Black, primary, 0.08f) else Color.White,
        surface = surface,
        input = if (dark) lerp(DARK_BASE, primary, 0.14f) else Color.White,
        hover = lerp(surface, primary, if (dark) 0.25f else 0.14f),
        textPrimary = if (dark) Color(0xFFECEFED) else Color(0xFF1A1F1C),
        textHeading = if (dark) Color(0xFFF7FAF8) else Color(0xFF111512),
        textSecondary = if (dark) Color(0xFFA9B2AD) else lerp(Color(0xFF4A504C), primary, 0.25f),
        textTotal = if (dark) lerp(primary, Color.White, 0.35f) else lerp(primary, Color.Black, 0.35f),
        textGreen = textGreen,
        textOrange = textOrange,
        border = lerp(surface, primary, if (dark) 0.30f else 0.20f),
        landingGradientStart = landingGradientStart,
        landingGradientMid = landingGradientMid,
        landingGradientEnd = landingGradientEnd,
        tableHeader = primary,
        chartColors = (listOf(primary, stops.last()) + chartColors).distinct().take(chartColors.size.coerceAtLeast(2)),
        accent = primary,
        isRed = false,
        isDark = dark,
        isOled = isOled,
        isTerminal = false,
        isLcars = false,
        isRetro = false
    )
}

private fun on(c: Color) = if (c.luminance() > 0.45f) Color(0xFF1A1F1C) else Color.White

/** Material scheme re-tinted the same way, so stock Material controls (checkboxes, buttons, fields) match. */
fun ColorScheme.followingKkc(kkc: KkcNavBarColors, colors: TimecardColors): ColorScheme {
    val primary = Color(kkc.primary)
    val secondary = kkc.boldGradient.lastOrNull()?.let { Color(it) } ?: primary
    return copy(
        primary = primary,
        onPrimary = on(primary),
        primaryContainer = colors.hover,
        onPrimaryContainer = colors.textTotal,
        secondary = secondary,
        onSecondary = on(secondary),
        secondaryContainer = colors.hover,
        onSecondaryContainer = colors.textTotal,
        tertiary = primary,
        background = colors.backgroundTo,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.hover,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.border
    )
}

/** Standard-theme typography in KKC's font (Inter) — same sizes and weights as TimecardTypography. */
val KkcInterTypography: Typography by lazy {
    val t = TimecardTypography
    val f = KkcInterFontFamily
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = f),
        displayMedium = t.displayMedium.copy(fontFamily = f),
        displaySmall = t.displaySmall.copy(fontFamily = f),
        headlineLarge = t.headlineLarge.copy(fontFamily = f),
        headlineMedium = t.headlineMedium.copy(fontFamily = f),
        headlineSmall = t.headlineSmall.copy(fontFamily = f),
        titleLarge = t.titleLarge.copy(fontFamily = f),
        titleMedium = t.titleMedium.copy(fontFamily = f),
        titleSmall = t.titleSmall.copy(fontFamily = f),
        bodyLarge = t.bodyLarge.copy(fontFamily = f),
        bodyMedium = t.bodyMedium.copy(fontFamily = f),
        bodySmall = t.bodySmall.copy(fontFamily = f),
        labelLarge = t.labelLarge.copy(fontFamily = f),
        labelMedium = t.labelMedium.copy(fontFamily = f),
        labelSmall = t.labelSmall.copy(fontFamily = f)
    )
}
