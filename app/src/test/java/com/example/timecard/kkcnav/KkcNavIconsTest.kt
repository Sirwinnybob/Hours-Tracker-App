package com.example.timecard.kkcnav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KkcNavIconsTest {

    private val pairs = listOf(
        NavDashboardSelected to NavDashboardUnselected,
        NavJobsSelected to NavJobsUnselected,
        NavSearchSelected to NavSearchUnselected,
        NavHoursSelected to NavHoursUnselected,
        NavTimeclockSelected to NavTimeclockUnselected,
        NavSupplySelected to NavSupplyUnselected,
        NavLibrarySelected to NavLibraryUnselected,
        NavSettingsSelected to NavSettingsUnselected,
        NavCalculatorSelected to NavCalculatorUnselected
    )

    @Test
    fun selectedAndUnselectedDiffer() {
        pairs.forEach { (s, u) -> assertNotEquals(s.name, u.name) }
    }

    @Test
    fun allIconsAreTheKkcNavFamilyOnA24UnitViewport() {
        pairs.flatMap { listOf(it.first, it.second) }.forEach {
            assertTrue(it.name, it.name.startsWith("Nav"))
            assertEquals(it.name, 24f, it.viewportWidth)
            assertEquals(it.name, 24f, it.viewportHeight)
        }
    }

    @Test
    fun labelStyleMatchesKkcLabelSmall() {
        assertEquals(11f, KkcNavLabelStyle.fontSize.value)
        assertEquals(16f, KkcNavLabelStyle.lineHeight.value)
        assertEquals(0.5f, KkcNavLabelStyle.letterSpacing.value)
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, KkcNavLabelStyle.fontWeight)
    }
}
