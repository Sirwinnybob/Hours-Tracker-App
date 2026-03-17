package com.example.timecard.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    Light, Dark, Oled
}

@Stable
class TimecardColors(
    val backgroundFrom: Color,
    val backgroundTo: Color,
    val surface: Color,
    val input: Color,
    val hover: Color,
    val textPrimary: Color,
    val textHeading: Color,
    val textSecondary: Color,
    val textTotal: Color,
    val textGreen: Color,
    val textOrange: Color,
    val border: Color,
    val landingGradientStart: Color,
    val landingGradientMid: Color,
    val landingGradientEnd: Color,
    val tableHeader: Color,
    val chartColors: List<Color>,
    val accent: Color,
    val isRed: Boolean,
    val isDark: Boolean,
    val isOled: Boolean,
    val isTerminal: Boolean
) {
    val backgroundBrush: Brush
        get() = Brush.linearGradient(listOf(backgroundFrom, backgroundTo))

    val landingBrush: Brush
        get() = if (isOled) Brush.linearGradient(listOf(Color.Black, Color.Black))
        else Brush.linearGradient(listOf(landingGradientStart, landingGradientMid, landingGradientEnd))
}

val LightTimecardColors = TimecardColors(
    backgroundFrom = LightBackground, backgroundTo = LightBackgroundTo,
    surface = LightSurface, input = LightInput, hover = LightHover,
    textPrimary = LightTextPrimary, textHeading = LightTextHeading,
    textSecondary = LightTextSecondary, textTotal = LightTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = LightBorder,
    landingGradientStart = LandingGradientStart,
    landingGradientMid = LandingGradientMid,
    landingGradientEnd = LandingGradientEnd,
    tableHeader = TableHeaderGreen,
    chartColors = ChartColors,
    accent = AccentBlue, isRed = false, isDark = false, isOled = false, isTerminal = false
)

val DarkTimecardColors = TimecardColors(
    backgroundFrom = DarkBackground, backgroundTo = DarkBackgroundTo,
    surface = DarkSurface, input = DarkInput, hover = DarkHover,
    textPrimary = DarkTextPrimary, textHeading = DarkTextHeading,
    textSecondary = DarkTextSecondary, textTotal = DarkTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = DarkBorder,
    landingGradientStart = DarkLandingGradientStart,
    landingGradientMid = DarkLandingGradientMid,
    landingGradientEnd = DarkLandingGradientEnd,
    tableHeader = TableHeaderDarkGreen,
    chartColors = ChartColors,
    accent = AccentBlue, isRed = false, isDark = true, isOled = false, isTerminal = false
)

val OledTimecardColors = TimecardColors(
    backgroundFrom = OledBackground, backgroundTo = OledBackground,
    surface = OledSurface, input = OledInput, hover = OledInput,
    textPrimary = DarkTextPrimary, textHeading = DarkTextHeading,
    textSecondary = DarkTextSecondary, textTotal = DarkTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = OledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = TableHeaderDarkGreen,
    chartColors = ChartColors,
    accent = AccentBlue, isRed = false, isDark = true, isOled = true, isTerminal = false
)

val RedTimecardColors = TimecardColors(
    backgroundFrom = RedBackground, backgroundTo = RedBackgroundTo,
    surface = RedSurface, input = RedInput, hover = RedInput,
    textPrimary = RedTextPrimary, textHeading = RedTextHeading,
    textSecondary = RedTextSecondary, textTotal = RedTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = RedBorder,
    landingGradientStart = RedLandingGradientStart,
    landingGradientMid = RedLandingGradientMid,
    landingGradientEnd = RedLandingGradientEnd,
    tableHeader = TableHeaderRedTheme,
    chartColors = RedChartColors,
    accent = RedAccent, isRed = true, isDark = true, isOled = false, isTerminal = false
)

val RedOledTimecardColors = TimecardColors(
    backgroundFrom = RedOledBackground, backgroundTo = RedOledBackground,
    surface = RedOledSurface, input = RedOledInput, hover = RedOledInput,
    textPrimary = RedTextPrimary, textHeading = RedTextHeading,
    textSecondary = RedTextSecondary, textTotal = RedTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = RedOledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = TableHeaderRedTheme,
    chartColors = RedChartColors,
    accent = RedAccent, isRed = true, isDark = true, isOled = true, isTerminal = false
)

// --- LIGHT IMMERSIVE THEMES ---

val SunriseTimecardColors = TimecardColors(
    backgroundFrom = SunriseBackground, backgroundTo = SunriseBackgroundTo,
    surface = SunriseSurface, input = SunriseInput, hover = SunriseInput,
    textPrimary = SunriseTextPrimary, textHeading = SunriseTextHeading,
    textSecondary = SunriseTextSecondary, textTotal = SunriseTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = SunriseBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFFEF8F4),
    landingGradientEnd = Color.White,
    tableHeader = SunriseInput,
    chartColors = ChartColors,
    accent = SunriseAccent, isRed = false, isDark = false, isOled = false, isTerminal = false
)

val TwilightTimecardColors = TimecardColors(
    backgroundFrom = TwilightBackground, backgroundTo = TwilightBackgroundTo,
    surface = TwilightSurface, input = TwilightInput, hover = TwilightInput,
    textPrimary = TwilightTextPrimary, textHeading = TwilightTextHeading,
    textSecondary = TwilightTextSecondary, textTotal = TwilightTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = TwilightBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFFAF9FD),
    landingGradientEnd = Color.White,
    tableHeader = TwilightInput,
    chartColors = ChartColors,
    accent = TwilightAccent, isRed = false, isDark = false, isOled = false, isTerminal = false
)

val IsleTimecardColors = TimecardColors(
    backgroundFrom = IsleBackground, backgroundTo = IsleBackgroundTo,
    surface = IsleSurface, input = IsleInput, hover = IsleInput,
    textPrimary = IsleTextPrimary, textHeading = IsleTextHeading,
    textSecondary = IsleTextSecondary, textTotal = IsleTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = IsleBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFF7FDFD),
    landingGradientEnd = Color.White,
    tableHeader = IsleInput,
    chartColors = ChartColors,
    accent = IsleAccent, isRed = false, isDark = false, isOled = false, isTerminal = false
)

val DaybreakTimecardColors = TimecardColors(
    backgroundFrom = DaybreakBackground, backgroundTo = DaybreakBackgroundTo,
    surface = DaybreakSurface, input = DaybreakInput, hover = DaybreakInput,
    textPrimary = DaybreakTextPrimary, textHeading = DaybreakTextHeading,
    textSecondary = DaybreakTextSecondary, textTotal = DaybreakTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = DaybreakBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFFEFEFB),
    landingGradientEnd = Color.White,
    tableHeader = DaybreakInput,
    chartColors = ChartColors,
    accent = DaybreakAccent, isRed = false, isDark = false, isOled = false, isTerminal = false
)

val LightRedTimecardColors = TimecardColors(
    backgroundFrom = LightRedBackground, backgroundTo = LightRedBackgroundTo,
    surface = LightRedSurface, input = LightRedInput, hover = LightRedInput,
    textPrimary = LightRedTextPrimary, textHeading = LightRedTextHeading,
    textSecondary = LightRedTextSecondary, textTotal = LightRedTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = LightRedBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFFCF5F5),
    landingGradientEnd = Color.White,
    tableHeader = LightRedInput,
    chartColors = ChartColors,
    accent = LightRedAccent, isRed = true, isDark = false, isOled = false, isTerminal = false
)

// --- DARK IMMERSIVE THEMES ---

val SunsetTimecardColors = TimecardColors(
    backgroundFrom = SunsetBackground, backgroundTo = SunsetBackgroundTo,
    surface = SunsetSurface, input = SunsetInput, hover = SunsetInput,
    textPrimary = SunsetTextPrimary, textHeading = SunsetTextHeading,
    textSecondary = SunsetTextSecondary, textTotal = SunsetTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = SunsetBorder,
    landingGradientStart = SunsetLandingGradientStart,
    landingGradientMid = SunsetLandingGradientMid,
    landingGradientEnd = SunsetLandingGradientEnd,
    tableHeader = SunsetInput,
    chartColors = ChartColors,
    accent = SunsetAccent, isRed = false, isDark = true, isOled = false, isTerminal = false
)

val SunsetOledTimecardColors = TimecardColors(
    backgroundFrom = SunsetOledBackground, backgroundTo = SunsetOledBackground,
    surface = SunsetOledSurface, input = SunsetOledInput, hover = SunsetOledInput,
    textPrimary = SunsetTextPrimary, textHeading = SunsetTextHeading,
    textSecondary = SunsetTextSecondary, textTotal = SunsetTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = SunsetOledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = SunsetOledInput,
    chartColors = ChartColors,
    accent = SunsetAccent, isRed = false, isDark = true, isOled = true, isTerminal = false
)

val MidnightTimecardColors = TimecardColors(
    backgroundFrom = MidnightBackground, backgroundTo = MidnightBackgroundTo,
    surface = MidnightSurface, input = MidnightInput, hover = MidnightInput,
    textPrimary = MidnightTextPrimary, textHeading = MidnightTextHeading,
    textSecondary = MidnightTextSecondary, textTotal = MidnightTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = MidnightBorder,
    landingGradientStart = MidnightLandingGradientStart,
    landingGradientMid = MidnightLandingGradientMid,
    landingGradientEnd = MidnightLandingGradientEnd,
    tableHeader = MidnightInput,
    chartColors = ChartColors,
    accent = MidnightAccent, isRed = false, isDark = true, isOled = false, isTerminal = false
)

val MidnightOledTimecardColors = TimecardColors(
    backgroundFrom = MidnightOledBackground, backgroundTo = MidnightOledBackground,
    surface = MidnightOledSurface, input = MidnightOledInput, hover = MidnightOledInput,
    textPrimary = MidnightTextPrimary, textHeading = MidnightTextHeading,
    textSecondary = MidnightTextSecondary, textTotal = MidnightTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = MidnightOledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = MidnightOledInput,
    chartColors = ChartColors,
    accent = MidnightAccent, isRed = false, isDark = true, isOled = true, isTerminal = false
)

val OceanTimecardColors = TimecardColors(
    backgroundFrom = OceanBackground, backgroundTo = OceanBackgroundTo,
    surface = OceanSurface, input = OceanInput, hover = OceanInput,
    textPrimary = OceanTextPrimary, textHeading = OceanTextHeading,
    textSecondary = OceanTextSecondary, textTotal = OceanTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = OceanBorder,
    landingGradientStart = OceanLandingGradientStart,
    landingGradientMid = OceanLandingGradientMid,
    landingGradientEnd = OceanLandingGradientEnd,
    tableHeader = OceanInput,
    chartColors = ChartColors,
    accent = OceanAccent, isRed = false, isDark = true, isOled = false, isTerminal = false
)

val OceanOledTimecardColors = TimecardColors(
    backgroundFrom = OceanOledBackground, backgroundTo = OceanOledBackground,
    surface = OceanOledSurface, input = OceanOledInput, hover = OceanOledInput,
    textPrimary = OceanTextPrimary, textHeading = OceanTextHeading,
    textSecondary = OceanTextSecondary, textTotal = OceanTextTotal,
    textGreen = DarkTextGreen, textOrange = DarkTextOrange,
    border = OceanOledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = OceanOledInput,
    chartColors = ChartColors,
    accent = OceanAccent, isRed = false, isDark = true, isOled = true, isTerminal = false
)

// --- HACKER THEME ---

val HackerTimecardColors = TimecardColors(
    backgroundFrom = HackerBackground, backgroundTo = HackerBackgroundTo,
    surface = HackerSurface, input = HackerInput, hover = HackerInput,
    textPrimary = HackerTextPrimary, textHeading = HackerTextHeading,
    textSecondary = HackerTextSecondary, textTotal = HackerTextTotal,
    textGreen = HackerTextPrimary, textOrange = HackerTextTotal,
    border = HackerBorder,
    landingGradientStart = HackerLandingGradientStart,
    landingGradientMid = HackerLandingGradientMid,
    landingGradientEnd = HackerLandingGradientEnd,
    tableHeader = TableHeaderHacker,
    chartColors = HackerChartColors,
    accent = HackerAccent, isRed = false, isDark = true, isOled = false, isTerminal = true
)

val HackerOledTimecardColors = TimecardColors(
    backgroundFrom = HackerOledBackground, backgroundTo = HackerOledBackground,
    surface = HackerOledSurface, input = HackerOledInput, hover = HackerOledInput,
    textPrimary = HackerTextPrimary, textHeading = HackerTextHeading,
    textSecondary = HackerTextSecondary, textTotal = HackerTextTotal,
    textGreen = HackerTextPrimary, textOrange = HackerTextTotal,
    border = HackerOledBorder,
    landingGradientStart = Color.Black,
    landingGradientMid = Color.Black,
    landingGradientEnd = Color.Black,
    tableHeader = TableHeaderHacker,
    chartColors = HackerChartColors,
    accent = HackerAccent, isRed = false, isDark = true, isOled = true, isTerminal = true
)

val LightHackerTimecardColors = TimecardColors(
    backgroundFrom = LightHackerBackground, backgroundTo = LightHackerBackgroundTo,
    surface = LightHackerSurface, input = LightHackerInput, hover = LightHackerInput,
    textPrimary = LightHackerTextPrimary, textHeading = LightHackerTextHeading,
    textSecondary = LightHackerTextSecondary, textTotal = LightHackerTextTotal,
    textGreen = LightTextGreen, textOrange = LightTextOrange,
    border = LightHackerBorder,
    landingGradientStart = Color.White,
    landingGradientMid = Color(0xFFF5FFF5),
    landingGradientEnd = Color.White,
    tableHeader = LightHackerInput,
    chartColors = HackerChartColors,
    accent = LightHackerAccent, isRed = false, isDark = false, isOled = false, isTerminal = true
)

val LocalTimecardColors = compositionLocalOf { DarkTimecardColors }

private val LightMaterialColors = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextHeading,
    primary = AccentBlue,
    onPrimary = Color.White
)

private val DarkMaterialColors = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextHeading,
    primary = AccentBlue,
    onPrimary = Color.White
)

private val OledMaterialColors = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextHeading,
    primary = AccentBlue,
    onPrimary = Color.White
)

private val RedMaterialColors = darkColorScheme(
    background = RedBackground,
    surface = RedSurface,
    onBackground = RedTextPrimary,
    onSurface = RedTextHeading,
    primary = RedAccent,
    onPrimary = Color.White
)

private val SunriseMaterialColors = lightColorScheme(
    background = SunriseBackground,
    surface = SunriseSurface,
    onBackground = SunriseTextPrimary,
    onSurface = SunriseTextHeading,
    primary = SunriseAccent,
    onPrimary = Color.White
)

private val TwilightMaterialColors = lightColorScheme(
    background = TwilightBackground,
    surface = TwilightSurface,
    onBackground = TwilightTextPrimary,
    onSurface = TwilightTextHeading,
    primary = TwilightAccent,
    onPrimary = Color.White
)

private val IsleMaterialColors = lightColorScheme(
    background = IsleBackground,
    surface = IsleSurface,
    onBackground = IsleTextPrimary,
    onSurface = IsleTextHeading,
    primary = IsleAccent,
    onPrimary = Color.White
)

private val DaybreakMaterialColors = lightColorScheme(
    background = DaybreakBackground,
    surface = DaybreakSurface,
    onBackground = DaybreakTextPrimary,
    onSurface = DaybreakTextHeading,
    primary = DaybreakAccent,
    onPrimary = Color.White
)

val LightRedMaterialColors = lightColorScheme(
    background = LightRedBackground,
    surface = LightRedSurface,
    onBackground = LightRedTextPrimary,
    onSurface = LightRedTextHeading,
    primary = LightRedAccent,
    onPrimary = Color.White
)

private val SunsetMaterialColors = darkColorScheme(
    background = SunsetBackground,
    surface = SunsetSurface,
    onBackground = SunsetTextPrimary,
    onSurface = SunsetTextHeading,
    primary = SunsetAccent,
    onPrimary = Color.White
)

private val MidnightMaterialColors = darkColorScheme(
    background = MidnightBackground,
    surface = MidnightSurface,
    onBackground = MidnightTextPrimary,
    onSurface = MidnightTextHeading,
    primary = MidnightAccent,
    onPrimary = Color.White
)

private val OceanMaterialColors = darkColorScheme(
    background = OceanBackground,
    surface = OceanSurface,
    onBackground = OceanTextPrimary,
    onSurface = OceanTextHeading,
    primary = OceanAccent,
    onPrimary = Color.White
)


private val HackerMaterialColors = darkColorScheme(
    background = HackerBackground,
    surface = HackerSurface,
    onBackground = HackerTextPrimary,
    onSurface = HackerTextHeading,
    primary = HackerAccent,
    onPrimary = Color.Black
)

private val LightHackerMaterialColors = lightColorScheme(
    background = LightHackerBackground,
    surface = LightHackerSurface,
    onBackground = LightHackerTextPrimary,
    onSurface = LightHackerTextHeading,
    primary = LightHackerAccent,
    onPrimary = Color.White
)

fun timecardColorsFor(mode: ThemeMode): TimecardColors = when (mode) {
    ThemeMode.Light -> LightTimecardColors
    ThemeMode.Dark -> DarkTimecardColors
    ThemeMode.Oled -> OledTimecardColors
}

/** Returns a copy of TimecardColors with a different accent color. */
fun TimecardColors.withAccent(newAccent: Color) = TimecardColors(
    backgroundFrom = backgroundFrom, backgroundTo = backgroundTo,
    surface = surface, input = input, hover = hover,
    textPrimary = textPrimary, textHeading = textHeading,
    textSecondary = textSecondary, textTotal = textTotal,
    textGreen = textGreen, textOrange = textOrange, border = border,
    landingGradientStart = landingGradientStart,
    landingGradientMid = landingGradientMid,
    landingGradientEnd = landingGradientEnd,
    tableHeader = tableHeader, chartColors = chartColors,
    accent = newAccent, isRed = isRed, isDark = isDark, isOled = isOled, isTerminal = isTerminal
)

/** Maps an accentColor key (stored in profile.json) to a Color, or null for default. */
fun accentColorFor(key: String?): Color? = when (key) {
    "orange" -> Color(0xFFFF8C00)
    "purple" -> Color(0xFF9B59B6)
    "teal"   -> Color(0xFF00BFA5)
    "gold"   -> Color(0xFFD4AF37)
    "red"    -> Color(0xFFCC0000)
    "hacker" -> Color(0xFF00FF41)
    "sunset" -> Color(0xFFE91E63)
    "midnight" -> Color(0xFF3F51B5)
    "ocean" -> Color(0xFF00BCD4)
    else     -> null

}

/** Inventory item required to unlock each accent color. */
val ACCENT_UNLOCKS = listOf(
    Triple("orange", Color(0xFFFF8C00), "accent_sunrise"),
    Triple("purple", Color(0xFF9B59B6), "accent_twilight"),
    Triple("teal",   Color(0xFF00BFA5), "accent_isle"),
    Triple("gold",   Color(0xFFD4AF37), "accent_daybreak"),
    Triple("red",    Color(0xFFCC0000), "accent_red"),
    Triple("hacker", Color(0xFF00FF41), "accent_hacker"),
    Triple("sunset", Color(0xFFE91E63), "accent_sunset"),   // Added missing
    Triple("midnight", Color(0xFF3F51B5), "accent_midnight"), // Added missing
    Triple("ocean",  Color(0xFF00BCD4), "accent_ocean"),    // Added missing
)

/**
 * Resolves the final TimecardColors and Material colorScheme based on the base ThemeMode
 * and whether the user has equipped an unlocked immersive theme (via accentKey).
 */
@Composable
fun TimecardTheme(
    themeMode: ThemeMode,
    accentKey: String? = null,
    content: @Composable () -> Unit
) {
    // Determine base colors from system/user preference
    var timecardColors = timecardColorsFor(themeMode)
    var materialColors = when (themeMode) {
        ThemeMode.Light -> LightMaterialColors
        ThemeMode.Dark -> DarkMaterialColors
        ThemeMode.Oled -> OledMaterialColors
    }

    // Override EVERYTHING if an immersive theme (accentKey) is equipped,
    // but respect the OLED prefix (if the user is in an OLED mode, map to the immersive OLED variant).
    val isOledBase = themeMode == ThemeMode.Oled
    val isLightBase = themeMode == ThemeMode.Light

    when (accentKey) {
        "orange" -> {
            // Sunrise: always light mode
            timecardColors = SunriseTimecardColors
            materialColors = SunriseMaterialColors
        }
        "sunset" -> {
            // Sunset: always dark mode
            timecardColors = if (isOledBase) SunsetOledTimecardColors else SunsetTimecardColors
            materialColors = SunsetMaterialColors
        }
        "purple", "midnight" -> { 
            timecardColors = if (isLightBase) TwilightTimecardColors else if (isOledBase) MidnightOledTimecardColors else MidnightTimecardColors
            materialColors = if (isLightBase) TwilightMaterialColors else MidnightMaterialColors
        }
        "teal", "ocean" -> { 
            timecardColors = if (isLightBase) IsleTimecardColors else if (isOledBase) OceanOledTimecardColors else OceanTimecardColors
            materialColors = if (isLightBase) IsleMaterialColors else OceanMaterialColors
        }
        "gold" -> {
            timecardColors = DaybreakTimecardColors
            materialColors = DaybreakMaterialColors
        }
        "red" -> {
            timecardColors = if (isLightBase) LightRedTimecardColors else if (isOledBase) RedOledTimecardColors else RedTimecardColors
            materialColors = if (isLightBase) LightRedMaterialColors else RedMaterialColors
        }
        "hacker" -> {
            // Hacker always forces dark mode
            timecardColors = if (isOledBase) HackerOledTimecardColors else HackerTimecardColors
            materialColors = HackerMaterialColors
        }
    }

    CompositionLocalProvider(LocalTimecardColors provides timecardColors) {
        val typography = if (timecardColors.isTerminal) TerminalTypography else TimecardTypography
    
        MaterialTheme(
            colorScheme = materialColors,
            typography = typography,
            content = content
        )
    }
}

// Theme state manager
class ThemeState(private val prefs: SharedPreferences) {
    var mode by mutableStateOf(loadTheme())
        private set

    private fun loadTheme(): ThemeMode {
        val saved = prefs.getString("theme", null) ?: return ThemeMode.Dark
        return when (saved) {
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            "oled" -> ThemeMode.Oled
            else -> ThemeMode.Dark
        }
    }

    fun toggleTheme() {
        mode = when (mode) {
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark -> ThemeMode.Light
            ThemeMode.Oled -> ThemeMode.Light
        }
        saveTheme()
    }

    fun cycleDarkVariant() {
        mode = when (mode) {
            ThemeMode.Dark -> ThemeMode.Oled
            ThemeMode.Oled -> ThemeMode.Dark
            else -> ThemeMode.Oled
        }
        saveTheme()
    }

    private fun saveTheme() {
        val value = when (mode) {
            ThemeMode.Light -> "light"
            ThemeMode.Dark -> "dark"
            ThemeMode.Oled -> "oled"
        }
        prefs.edit().putString("theme", value).apply()
    }

    companion object {
        fun create(context: Context): ThemeState {
            val prefs = context.getSharedPreferences("TimecardThemePrefs", Context.MODE_PRIVATE)
            
            // On first install (or if preference is missing), detect system theme
            if (!prefs.contains("theme")) {
                val uiMode = context.resources.configuration.uiMode
                val isSystemDark = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                val initialMode = if (isSystemDark) "dark" else "light"
                prefs.edit().putString("theme", initialMode).apply()
            }
            
            return ThemeState(prefs)
        }
    }
}

@Composable
fun timecardShape(base: Shape): Shape {
    // Industrial Avionics aesthetic: override default bubbly shapes with rigid, physical shapes.
    // Base shape is ignored to strictly enforce the design system across all standard UI elements.
    return if (LocalTimecardColors.current.isTerminal) RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
}
