package com.example.timecard.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.ThemeMode
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.ui.theme.timecardShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThemeToggle(
    themeState: ThemeState,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    val colors = LocalTimecardColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Theme toggle (light/dark + long press for OLED)
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(size)
                .clip(CircleShape)
                .background(colors.hover)
                .combinedClickable(
                    onClick = { themeState.toggleTheme() },
                    onLongClick = { themeState.cycleDarkVariant() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (themeState.mode) {
                    ThemeMode.Light -> "\uD83C\uDF19" // Moon
                    ThemeMode.Dark -> "\u2600\uFE0F" // Sun
                    ThemeMode.Oled -> "\uD83D\uDCA1" // Bulb
                },
                fontSize = 18.sp
            )
        }
    }
}
