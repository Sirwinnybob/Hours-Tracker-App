package com.example.timecard.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RetroPixelIconsTest {

    @Test
    fun everyIconHasGeometryInsideTheGrid() {
        RetroIcon.values().forEach { icon ->
            val rects = retroIconRects(icon)
            assertTrue("$icon has no pixels", rects.isNotEmpty())
            rects.forEach { px ->
                assertTrue("$icon rect starts outside grid: ${px.x},${px.y}", px.x >= 0f && px.y >= 0f)
                assertTrue("$icon rect has no size", px.w > 0f && px.h > 0f)
                assertTrue("$icon rect overflows grid: ${px.x}+${px.w}", px.x + px.w <= RETRO_ICON_GRID)
                assertTrue("$icon rect overflows grid: ${px.y}+${px.h}", px.y + px.h <= RETRO_ICON_GRID)
            }
        }
    }

    @Test
    fun navButtonEmojiMapToIcons() {
        assertEquals(RetroIcon.Clock, RetroIcon.fromEmoji("🕒"))
        assertEquals(RetroIcon.Bars, RetroIcon.fromEmoji("📊"))
        assertEquals(RetroIcon.Trophy, RetroIcon.fromEmoji("🏆"))
        assertEquals(RetroIcon.Target, RetroIcon.fromEmoji("🎯"))
        assertEquals(RetroIcon.Chart, RetroIcon.fromEmoji("📈"))
        assertEquals(RetroIcon.Floppy, RetroIcon.fromEmoji("💾"))
        assertEquals(RetroIcon.Door, RetroIcon.fromEmoji("🚪"))
    }

    @Test
    fun variationSelectorsAreIgnored() {
        assertEquals(RetroIcon.Bag, RetroIcon.fromEmoji("🛍️"))
        assertEquals(RetroIcon.Gear, RetroIcon.fromEmoji("⚙️"))
        assertNotNull(RetroIcon.fromEmoji("🛍"))
    }

    @Test
    fun everyKkcDestinationHasAPixelIcon() {
        listOf("dashboard", "jobs", "search", "calculator", "hours", "timecard", "supply", "settings", "standards")
            .forEach { route ->
                assertNotNull("no pixel icon for $route", RetroIcon.forKkcRoute(route))
            }
        assertNull(RetroIcon.forKkcRoute("unknown"))
    }

    @Test
    fun kkcDestinationsDrawTheirOwnSubjects() {
        // Same subjects as KKC's navbar icons, not look-alikes: jobs is the cabinet, supply the forklift.
        assertEquals(RetroIcon.Cabinet, RetroIcon.forKkcRoute("jobs"))
        assertEquals(RetroIcon.Forklift, RetroIcon.forKkcRoute("supply"))
        assertEquals(RetroIcon.Binder, RetroIcon.forKkcRoute("standards"))
        assertEquals(RetroIcon.TimeCard, RetroIcon.forKkcRoute("timecard"))
    }

    @Test
    fun unknownEmojiFallsBackToText() {
        assertNull(RetroIcon.fromEmoji("🦄"))
    }
}
