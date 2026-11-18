package com.example.timecard.ui.alerts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.timecard.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertColorsTest {
    @Test fun alertContentIsReadableAcrossPalettes() {
        val palettes = listOf(LightTimecardColors, DarkTimecardColors, OledTimecardColors,
            RedTimecardColors, RedOledTimecardColors, SunriseTimecardColors, TwilightTimecardColors,
            IsleTimecardColors, DaybreakTimecardColors, LightRedTimecardColors, SunsetTimecardColors,
            SunsetOledTimecardColors, MidnightTimecardColors, MidnightOledTimecardColors,
            OceanTimecardColors, OceanOledTimecardColors, HackerTimecardColors, HackerOledTimecardColors,
            LightHackerTimecardColors, LcarsTimecardColors, StarWarsTimecardColors, RetroTimecardColors)
        palettes.forEach { theme ->
            val palette = alertColors(theme)
            listOf(palette.text, palette.secondary, palette.link).forEach { foreground ->
                assertTrue("Unreadable alert text $foreground on ${palette.background}", contrast(foreground, palette.background) >= 4.5f)
            }
            assertTrue("Unreadable button", contrast(palette.buttonText, palette.button) >= 4.5f)
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + .05f) / (dark + .05f)
    }
}
