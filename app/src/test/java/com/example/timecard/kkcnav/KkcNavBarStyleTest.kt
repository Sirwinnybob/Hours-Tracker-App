package com.example.timecard.kkcnav

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.timecard.ui.theme.LcarsBlueBell
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsPurple
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsYellow
import org.junit.Assert.assertEquals
import org.junit.Test

class KkcNavBarStyleTest {

    private val kkcLight = KkcNavBarColors(
        primary = 0xFF1565C0.toInt(), onSurfaceVariant = 0xFF444444.toInt(), surfaceVariant = 0xFFE0E0E0.toInt(),
        frostedBase = 0xFFFFFFFF.toInt(), frostedContent = 0xFF000000.toInt(),
        boldGradient = listOf(0xFF2E7D32.toInt(), 0xFF81C784.toInt()),
        badgeContainer = 0xFFB3261E.toInt(), badgeContent = 0xFFFFFFFF.toInt()
    )

    private val kkcDark = KkcNavBarColors(
        primary = 0xFF79B2FF.toInt(), onSurfaceVariant = 0xFFCAC4D0.toInt(), surfaceVariant = 0xFF203048.toInt(),
        // A mid-luminance bold glow, like the green theme on the tablet.
        frostedBase = 0xFF4CAF50.toInt(), frostedContent = 0xFF000000.toInt(),
        boldGradient = listOf(0xFF2E7D32.toInt(), 0xFF81C784.toInt()),
        badgeContainer = 0xFFF2B8B5.toInt(), badgeContent = 0xFF601410.toInt()
    )

    private val payload = KkcNavBarPayload(
        destinations = listOf("jobs", "hours", "timecard", "supply", "standards"),
        supplyCount = 0, safetyCount = 0,
        light = kkcLight, dark = kkcDark,
        frostedAlpha = 0.72f, frostedBlurDp = 14f, boldMode = true,
        indicatorCornerDp = 9f, animDisabled = false, blurDisabled = false, shadowsDisabled = false
    )

    private val htColors = kkcNavColorsFromHt(
        accent = Color(0xFFFF8C00), textSecondary = Color(0xFF8A6A40), pill = Color(0xFFFFE0B2),
        surface = Color(0xFFFFF8F0), error = Color(0xFFB3261E), onError = Color.White
    )

    @Test
    fun standardThemeUsesKkcColors() {
        assertEquals(KkcNavBarSource.KkcTheme, kkcNavBarSource(null))
        assertEquals(KkcNavBarSource.KkcTheme, kkcNavBarSource("unknown-key"))
    }

    @Test
    fun equippedHoursTrackerThemeUsesHoursTrackerColors() {
        listOf("orange", "purple", "teal", "gold", "red", "hacker", "sunset", "midnight", "ocean", "starwars").forEach {
            assertEquals(it, KkcNavBarSource.HoursTrackerTheme, kkcNavBarSource(it))
        }
    }

    @Test
    fun lcarsGetsItsOwnBand() {
        assertEquals(KkcNavBarSource.Lcars, kkcNavBarSource("lcars"))
    }

    @Test
    fun onGlassIsWhiteOverADarkCanvasAndBlackOverALightOne() {
        val glow = Color(0xFF4CAF50)
        // The tablet bug: bold green glass over Hours Tracker's dark canvas must get light text.
        assertEquals(Color.White, kkcOnGlassColor(glow, 0.72f, canvas = Color(0xFF0F1420)))
        assertEquals(Color.Black, kkcOnGlassColor(glow, 0.72f, canvas = Color(0xFFF0F4F8)))
    }

    @Test
    fun kkcThemeStyleUsesTheSetMatchingHoursTrackersMode() {
        val dark = kkcNavBarStyle(payload, KkcNavBarSource.KkcTheme, darkTheme = true, htColors, canvas = Color(0xFF0F1420))
        assertEquals(kkcDark, dark.colors)
        assertEquals(true, dark.boldMode)
        assertEquals(Color.White, dark.onGlass)

        val light = kkcNavBarStyle(payload, KkcNavBarSource.KkcTheme, darkTheme = false, htColors, canvas = Color(0xFFF0F4F8))
        assertEquals(kkcLight, light.colors)
        assertEquals(Color.Black, light.onGlass)
    }

    @Test
    fun hoursTrackerThemeStyleIgnoresKkcColorsAndBoldMode() {
        val style = kkcNavBarStyle(payload, KkcNavBarSource.HoursTrackerTheme, darkTheme = false, htColors, canvas = Color(0xFFFFF8F0))
        assertEquals(htColors, style.colors)
        assertEquals(false, style.boldMode)
    }

    @Test
    fun hoursTrackerColorsMapAccentSecondaryPillSurfaceAndError() {
        assertEquals(Color(0xFFFF8C00).toArgbInt(), htColors.primary)
        assertEquals(Color(0xFF8A6A40).toArgbInt(), htColors.onSurfaceVariant)
        assertEquals(Color(0xFFFFE0B2).toArgbInt(), htColors.surfaceVariant)
        assertEquals(Color(0xFFFFF8F0).toArgbInt(), htColors.frostedBase)
        assertEquals(Color(0xFFB3261E).toArgbInt(), htColors.badgeContainer)
        assertEquals(Color.White.toArgbInt(), htColors.badgeContent)
    }

    @Test
    fun tintsUseOnGlassInBoldModeAndThemeColorsOtherwise() {
        val bold = kkcNavTints(KkcNavBarStyle(kkcDark, boldMode = true, onGlass = Color.White))
        assertEquals(Color.White, bold.selected)
        assertEquals(Color.White.copy(alpha = 0.8f), bold.unselected)

        val normal = kkcNavTints(KkcNavBarStyle(kkcDark, boldMode = false, onGlass = Color.White))
        assertEquals(Color(kkcDark.primary), normal.selected)
        assertEquals(Color(kkcDark.onSurfaceVariant), normal.unselected)
    }

    @Test
    fun lcarsBlocksCyclePaletteAndHighlightHours() {
        assertEquals(LcarsOrange, kkcLcarsBlockColor(index = 0, selected = false))
        assertEquals(LcarsTan, kkcLcarsBlockColor(index = 1, selected = false))
        assertEquals(LcarsPurple, kkcLcarsBlockColor(index = 2, selected = false))
        assertEquals(LcarsBlueBell, kkcLcarsBlockColor(index = 3, selected = false))
        assertEquals(LcarsOrange, kkcLcarsBlockColor(index = 4, selected = false))
        assertEquals(LcarsYellow, kkcLcarsBlockColor(index = 2, selected = true))
    }

    @Test
    fun lcarsLabelsAreUppercase() {
        assertEquals("LIBRARY", kkcLcarsLabel("standards"))
        assertEquals("CALC", kkcLcarsLabel("calculator"))
    }

    private fun Color.toArgbInt(): Int = toArgb()
}
