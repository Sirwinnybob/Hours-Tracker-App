package com.example.timecard.kkcnav

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KkcReturnNavigationTest {

    @Test
    fun destinationTapTargetsKkcMainActivityOnTopOfItsTask() {
        val request = KkcReturnNavigation.requestFor("supply")!!
        assertEquals("com.kkc.sheettracker", request.packageName)
        assertEquals("com.kkc.sheettracker.MainActivity", request.className)
        assertEquals(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP, request.flags)
        assertEquals("supply", request.destination)
    }

    @Test
    fun calculatorTapIsSentAsCalculator() {
        assertEquals("calculator", KkcReturnNavigation.requestFor("calculator")!!.destination)
    }

    @Test
    fun hoursTapDoesNothing() {
        assertNull(KkcReturnNavigation.requestFor("hours"))
    }

    @Test
    fun blankTapDoesNothing() {
        assertNull(KkcReturnNavigation.requestFor(""))
    }
}
