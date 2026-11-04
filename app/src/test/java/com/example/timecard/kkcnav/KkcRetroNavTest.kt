package com.example.timecard.kkcnav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Calendar

class KkcRetroNavTest {

    private fun millisAt(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 4, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun clockUsesTwelveHourTimeWithMeridiem() {
        assertEquals("1:23 PM", kkcRetroClockText(millisAt(13, 23)))
        assertEquals("12:05 AM", kkcRetroClockText(millisAt(0, 5)))
    }

    @Test
    fun everyKkcDestinationGetsItsOwnIconColor() {
        val routes = kkcNavSlots(KKC_NAV_LABELS.keys.toList())
        val colors = routes.map { kkcRetroIconColor(it) }
        assertEquals("each destination should be a distinct color", routes.size, colors.toSet().size)
    }

    @Test
    fun dateUnderTheClockIsWeekdayAndShortDate() {
        assertEquals("Wed 11/4", kkcRetroDateText(millisAt(13, 23)))
    }

    @Test
    fun clockColonBlinksOffWithoutShiftingTheText() {
        val on = kkcRetroClockText(millisAt(9, 41), colonVisible = true)
        val off = kkcRetroClockText(millisAt(9, 41), colonVisible = false)
        assertFalse(off.contains(':'))
        assertEquals(on.length, off.length)
        assertEquals("9 41 AM", off)
    }
}
