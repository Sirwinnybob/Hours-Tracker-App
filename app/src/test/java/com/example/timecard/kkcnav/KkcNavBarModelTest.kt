package com.example.timecard.kkcnav

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class KkcNavBarModelTest {

    private val payload = KkcNavBarPayload(
        destinations = listOf("jobs", "hours", "timecard", "supply", "standards"),
        supplyCount = 4, safetyCount = 2, dark = false,
        primary = 0xFF1565C0.toInt(), onSurfaceVariant = 0xFF444444.toInt(), surfaceVariant = 0xFFE0E0E0.toInt(),
        frostedBase = 0xFFFFFFFF.toInt(), frostedContent = 0xFF000000.toInt(),
        frostedAlpha = 0.72f, frostedBlurDp = 14f, boldMode = false,
        boldGradient = listOf(0xFF1565C0.toInt()),
        badgeContainer = 0xFFB3261E.toInt(), badgeContent = 0xFFFFFFFF.toInt(),
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
    fun normalTintsUsePrimaryAndOnSurfaceVariant() {
        val tints = kkcNavTints(payload)
        assertEquals(Color(payload.primary), tints.selected)
        assertEquals(Color(payload.onSurfaceVariant), tints.unselected)
    }

    @Test
    fun boldTintsUseTheFrostedContentColor() {
        val tints = kkcNavTints(payload.copy(boldMode = true))
        assertEquals(Color(payload.frostedContent), tints.selected)
        assertEquals(Color(payload.frostedContent).copy(alpha = 0.8f), tints.unselected)
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
