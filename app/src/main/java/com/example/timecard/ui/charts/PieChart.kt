package com.example.timecard.ui.charts

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.timecard.ui.theme.LocalTimecardColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current
    val chartColors = colors.chartColors

    val total = data.values.sum()
    if (total <= 0) return

    val entries = data.entries.toList()
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val radius = (size.minDimension / 2f) - 20.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        var startAngle = -90f

        entries.forEachIndexed { index, (job, hours) ->
            val sweepAngle = (hours / total * 360f).toFloat() * animationProgress.value
            val color = chartColors[index % chartColors.size]

            // Draw arc
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )

            // Draw label if slice is big enough
            if (sweepAngle > 15f) {
                val midAngle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                val labelRadius = radius * 0.65f
                val lx = center.x + labelRadius * cos(midAngle).toFloat()
                val ly = center.y + labelRadius * sin(midAngle).toFloat()

                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textAlign = Paint.Align.CENTER
                        textSize = 10.dp.toPx()
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(job, lx, ly - 4.dp.toPx(), paint)

                    paint.textSize = 9.dp.toPx()
                    paint.typeface = Typeface.DEFAULT
                    canvas.nativeCanvas.drawText(
                        String.format("%.2fh", hours),
                        lx, ly + 10.dp.toPx(), paint
                    )
                }
            }

            startAngle += sweepAngle
        }

        // Center circle for donut effect
        drawCircle(
            color = colors.surface,
            radius = radius * 0.3f,
            center = center
        )

        // Total in center
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                this.color = colors.textTotal.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 14.dp.toPx()
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawText(
                String.format("%.2f", total),
                center.x, center.y + 5.dp.toPx(), paint
            )
        }
    }
}
