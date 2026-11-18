package com.example.timecard.ui.alerts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.TimecardColors

internal data class AlertColors(
    val background: Color, val text: Color, val secondary: Color,
    val link: Color, val button: Color, val buttonText: Color
)

private fun readableOn(preferred: Color, background: Color): Color {
    fun contrast(color: Color): Float {
        val a = color.luminance()
        val b = background.luminance()
        return (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
    }
    return if (contrast(preferred) >= 4.5f) preferred
    else if (contrast(Color.Black) >= contrast(Color.White)) Color.Black else Color.White
}

internal fun alertColors(theme: TimecardColors): AlertColors {
    val background = if (theme.isLcars) Color.Black else theme.surface
    val button = if (theme.isLcars) LcarsOrange else if (theme.isRed) Color(0xFFCC0000) else AccentBlue
    return AlertColors(
        background = background,
        text = readableOn(theme.textPrimary, background),
        secondary = readableOn(theme.textSecondary, background),
        link = readableOn(theme.accent, background),
        button = button,
        buttonText = readableOn(if (theme.isLcars) Color.Black else Color.White, button)
    )
}
