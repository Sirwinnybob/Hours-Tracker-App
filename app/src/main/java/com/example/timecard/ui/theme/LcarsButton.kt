package com.example.timecard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LcarsButtonShape {
    Rect, LeftCap, RightCap, Pill
}

@Composable
fun LcarsButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = LcarsOrange,
    code: String? = null,
    shapeType: LcarsButtonShape = LcarsButtonShape.Rect,
    textColor: Color = Color.Black,
    enabled: Boolean = true
) {
    val shape = when (shapeType) {
        LcarsButtonShape.Rect -> RectangleShape
        LcarsButtonShape.LeftCap -> RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100)
        LcarsButtonShape.RightCap -> RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100)
        LcarsButtonShape.Pill -> RoundedCornerShape(50)
    }

    val paddingValues = when (shapeType) {
        LcarsButtonShape.LeftCap -> Modifier.padding(end = 12.dp, start = 8.dp)
        LcarsButtonShape.RightCap -> Modifier.padding(start = 12.dp, end = 8.dp)
        else -> Modifier.padding(horizontal = 8.dp)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(paddingValues),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (shapeType) {
                LcarsButtonShape.LeftCap -> Arrangement.End
                LcarsButtonShape.RightCap -> Arrangement.Start
                else -> Arrangement.Center
            }
        ) {
            if (code != null) {
                Text(
                    text = code,
                    fontFamily = AntonioFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = label.uppercase(),
                fontFamily = AntonioFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = textColor,
                textAlign = when (shapeType) {
                    LcarsButtonShape.LeftCap -> TextAlign.End
                    LcarsButtonShape.RightCap -> TextAlign.Start
                    else -> TextAlign.Center
                }
            )
        }
    }
}
