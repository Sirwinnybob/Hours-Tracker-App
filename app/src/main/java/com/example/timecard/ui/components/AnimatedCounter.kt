package com.example.timecard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

@Composable
fun AnimatedCounter(
    targetValue: Double,
    modifier: Modifier = Modifier,
    initialValue: Double = targetValue,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    showZero: Boolean = true
) {
    val animatedValue = remember { Animatable(initialValue.toFloat()) }

    LaunchedEffect(targetValue) {
        animatedValue.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    val displayValue = animatedValue.value.toDouble()
    val text = if (!showZero && displayValue < 0.005) "" else String.format("%.2f", displayValue)

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign,
        fontFamily = com.example.timecard.ui.theme.JetBrainsMonoFontFamily
    )
}
