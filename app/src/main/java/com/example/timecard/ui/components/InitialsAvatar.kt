package com.example.timecard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.font.FontWeight

@Composable
fun InitialsAvatar(
    name: String,
    size: Dp,
    fontSize: TextUnit,
    bgColor: Color
) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
    val textColor = if (bgColor.luminance() > 0.5f) Color.Black else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
            .background(bgColor)
    ) {
        Text(initials, fontSize = fontSize, color = textColor, fontWeight = FontWeight.Bold)
    }
}
