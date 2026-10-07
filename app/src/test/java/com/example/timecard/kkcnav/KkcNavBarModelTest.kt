package com.example.timecard.kkcnav

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class KkcNavBarModelTest {

    private val lightColors = KkcNavBarColors(
        primary = 0xFF1565C0.toInt(), onSurfaceVariant = 0xFF444444.toInt(), surfaceVariant = 0xFFE0E0E0.toInt(),
        frostedBase = 0xFFFFFFFF.toInt(), frostedContent = 0xFF000000.toInt(),
        boldGradient = listOf(0xFF1565C0.toInt(), 0xFFD8E6F7.toInt()),
        badgeContainer = 0xFFB3261E.toInt(), badgeContent = 0xFFFFFFFF.toInt()
    )

    private val darkColors = KkcNavBarColors(
        primary = 0xFF79B2FF.toInt(), onSurfaceVariant = 0xFFCAC4D0.toInt(), surfaceVariant = 0xFF203048.toInt(),
        frostedBase = 0xFF162438.toInt(), frostedContent = 0xFFFFFFFF.toInt(),
        boldGradient = listOf(0xFF79B2FF.toInt(), 0xFF204668.toInt()),
        badgeContainer = 0xFFF2B8B5.toInt(), badgeContent = 0xFF601410.toInt()
    )

    private val payload = KkcNavBarPayload(
        destinations = listOf("jobs", "hours", "timecard", "supply", "standards"),
        supplyCount = 4, safetyCount = 2,
        light = lightColors, dark = darkColors,
        frostedAlpha = 0.72f, frostedBlurDp = 14f, boldMode = false,
        indicatorCornerDp = 9f, animDisabled = false, blurDisabled = false, shadowsDisabled = false
    )

    @Test
    fun calculatorSlotSitsImmediatelyBeforeHours() {
        assertEquals(
            listOf("jobs", "calculator", "hours", "timecard", "supply", "standards"),
            kkcNavSlots(payload.destinations)
        )
    }

    @Test
    fun unknownRoutesFromANewerKkcAreDropped() {
        assertEquals(listOf("jobs", "calculator", "hours"), kkcNavSlots(listOf("jobs", "bogus", "hours")))
    }

    @Test
    fun labelsMatchKkcNavDestination() {
        // KEEP IN SYNC with KKC ui/components/AppScaffold.kt enum NavDestination labels.
        assertEquals(
            mapOf(
                "dashboard" to "Dashboard", "jobs" to "Jobs", "search" to "Search", "hours" to "Hours",
                "timecard" to "Timeclock", "supply" to "Supply", "settings" to "Settings", "standards" to "Library"
            ),
            KKC_NAV_LABELS
        )
        assertEquals("Calc", kkcNavLabel("calculator"))
    }

    @Test
    fun everySlotHasBothIconStates() {
        (KKC_NAV_LABELS.keys + "calculator").forEach { route ->
            kkcNavIcon(route, selected = true)
            kkcNavIcon(route, selected = false)
        }
    }

    @Test
    fun badgesOnlyOnSupplyAndLibrary() {
        assertEquals(4, kkcBadgeCount("supply", payload))
        assertEquals(2, kkcBadgeCount("standards", payload))
        assertEquals(0, kkcBadgeCount("jobs", payload))
        assertEquals(0, kkcBadgeCount("supply", payload.copy(supplyCount = 0)))
    }

    @Test
    fun normalTintsUsePrimaryAndOnSurfaceVariantOfTheActiveSet() {
        val light = kkcNavTints(payload.colorsFor(darkTheme = false), boldMode = false)
        assertEquals(Color(lightColors.primary), light.selected)
        assertEquals(Color(lightColors.onSurfaceVariant), light.unselected)

        val dark = kkcNavTints(payload.colorsFor(darkTheme = true), boldMode = false)
        assertEquals(Color(darkColors.primary), dark.selected)
        assertEquals(Color(darkColors.onSurfaceVariant), dark.unselected)
    }

    @Test
    fun boldTintsUseTheFrostedContentColorOfTheActiveSet() {
        val light = kkcNavTints(lightColors, boldMode = true)
        assertEquals(Color(lightColors.frostedContent), light.selected)
        assertEquals(Color(lightColors.frostedContent).copy(alpha = 0.8f), light.unselected)

        val dark = kkcNavTints(darkColors, boldMode = true)
        assertEquals(Color(darkColors.frostedContent), dark.selected)
        assertEquals(Color(darkColors.frostedContent).copy(alpha = 0.8f), dark.unselected)
    }

    @Test
    fun selectionFillIsSurfaceVariantNormallyAndTheGradientInBoldMode() {
        assertEquals(
            KkcNavSelectionFill.Solid(Color(lightColors.surfaceVariant)),
            kkcNavSelectionFill(lightColors, boldMode = false)
        )
        assertEquals(
            KkcNavSelectionFill.Solid(Color(darkColors.surfaceVariant)),
            kkcNavSelectionFill(darkColors, boldMode = false)
        )
        assertEquals(
            KkcNavSelectionFill.Gradient(listOf(Color(darkColors.boldGradient[0]), Color(darkColors.boldGradient[1]))),
            kkcNavSelectionFill(darkColors, boldMode = true)
        )
    }

    @Test
    fun singleStopGradientIsDuplicatedLikeKkcFlatGradient() {
        val flat = lightColors.copy(boldGradient = listOf(0xFF123456.toInt()))
        val fill = kkcNavSelectionFill(flat, boldMode = true) as KkcNavSelectionFill.Gradient
        assertEquals(listOf(Color(0xFF123456), Color(0xFF123456)), fill.stops)
    }

    @Test
    fun labelLineLimitMatchesKkcLowEndBranch() {
        // KKC: animated path = maxLines 1 + ellipsis; low-end path = unconstrained; Calc = unconstrained.
        assertEquals(1, kkcLabelMaxLines("jobs", animDisabled = false))
        assertEquals(Int.MAX_VALUE, kkcLabelMaxLines("jobs", animDisabled = true))
        assertEquals(Int.MAX_VALUE, kkcLabelMaxLines("calculator", animDisabled = false))
        assertEquals(Int.MAX_VALUE, kkcLabelMaxLines("calculator", animDisabled = true))
    }

    @Test
    fun frostedAlphaIsClampedLikeKkc() {
        assertEquals(0.5f, kkcFrostedFillAlpha(payload.copy(frostedAlpha = 0.3f)))
        assertEquals(0.95f, kkcFrostedFillAlpha(payload.copy(frostedAlpha = 0.99f)))
        assertEquals(0.72f, kkcFrostedFillAlpha(payload))
    }
}
