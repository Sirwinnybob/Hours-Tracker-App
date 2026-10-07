package com.example.timecard.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchAccentCacheTest {

    @Test
    fun keyIgnoresCaseAndSurroundingWhitespace() {
        assertEquals(LaunchAccentCache.key("Winston"), LaunchAccentCache.key("  winston "))
        assertEquals("accent:1234", LaunchAccentCache.key("1234"))
    }
}
