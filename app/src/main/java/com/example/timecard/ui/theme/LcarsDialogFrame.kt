package com.example.timecard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wraps modal content in an LCARS-styled dialog frame using a contiguous left bracket:
 *
 *  [ [=== ORANGE HEADER BAR: title left, red ✕ pill right ===] ]
 *  [ [ 4dp black gap                                          ] ]
 *  [ [ Content (passed via lambda, full padding = 16dp)       ] ]
 *  [ [ 4dp black gap                                          ] ]
 *  [ [=== TAN FOOTER BAR ===]                                 ]
 *
 * The left bracket is drawn as a single continuous piece using custom path rendering,
 * curving into the header and footer bars for a premium Star Trek dashboard look.
 */
@Composable
fun LcarsDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color.Black)
    ) {
        // Left Column: Bracket (width = 20.dp)
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val H = size.height
            val W = size.width
            val w = 10.dp.toPx()
            val h_top = 40.dp.toPx()
            val h_bottom = 24.dp.toPx()
            val r_concave = 10.dp.toPx()
            val rOuter = 12.dp.toPx()

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, H)
                lineTo(0f, rOuter)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(0f, 0f, rOuter * 2f, rOuter * 2f),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(W, 0f)
                lineTo(W, h_top)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(W - r_concave * 2f, h_top, W, h_top + r_concave * 2f),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(w, H - h_bottom - r_concave)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(W - r_concave * 2f, H - h_bottom - r_concave * 2f, W, H - h_bottom),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = false
                )
                lineTo(W, H)
                lineTo(rOuter, H)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(0f, H - rOuter * 2f, rOuter * 2f, H),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = path, color = LcarsOrange)
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Right Column: Header, Content, Footer
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Header Bar (Orange)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(LcarsOrange)
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontFamily = AntonioFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // Red close pill
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LcarsRed)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE",
                        fontFamily = AntonioFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp, end = 16.dp),
                content = content
            )

            Spacer(Modifier.height(4.dp))

            // Footer Bar (Tan)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(LcarsTan)
            )
        }
    }
}
