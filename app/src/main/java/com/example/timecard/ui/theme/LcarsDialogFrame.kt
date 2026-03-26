package com.example.timecard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * Wraps modal content in an LCARS-styled dialog frame:
 *
 *  [=== ORANGE HEADER BAR: title left, red ✕ pill right ===]
 *  [ 4dp black gap                                          ]
 *  [ Content (passed via lambda, full padding = 16dp)       ]
 *  [ 4dp black gap                                          ]
 *  [=== TAN FOOTER BAR ===]
 *
 * All corners are sharp (no rounding — LCARS is geometric).
 */
@Composable
fun LcarsDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // ── Orange header bar ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(LcarsOrange)
                .padding(horizontal = 12.dp),
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

        // ── Content area ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )

        Spacer(Modifier.height(4.dp))

        // ── Tan footer bar ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(LcarsTan)
        )
    }
}
