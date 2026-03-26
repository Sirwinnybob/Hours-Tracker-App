package com.example.timecard.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── LCARS chrome dimensions ───────────────────────────────────────────────────
val LCARS_SIDEBAR_WIDTH: Dp  = 68.dp
val LCARS_TOP_BAR_HEIGHT: Dp = 40.dp
val LCARS_BOTTOM_BAR_HEIGHT: Dp = 30.dp
val LCARS_GAP: Dp = 6.dp

// Legacy alias kept so callers that haven't been updated yet still compile
val LCARS_ELBOW_HEIGHT: Dp = LCARS_TOP_BAR_HEIGHT

/**
 * Full-frame LCARS chrome wrapper.
 *
 * Renders an authentic LCARS interface frame around [content]:
 *   • Top horizontal orange bar with title (full width)
 *   • Left sidebar of coloured block towers
 *   • Bottom tan bar with status text (full width)
 *   • Black "content cutout" in the middle-right area
 *   • Concave quarter-circle elbows at each inner corner
 *
 * Usage: Replace the standard Box/Column root with LcarsFrame when isLcars is true.
 */
@Composable
fun LcarsFrame(
    title: String = "",
    statusText: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        LcarsTopBar(title = title)

        Spacer(Modifier.height(LCARS_GAP))

        // ── Middle: sidebar + content cutout ─────────────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LcarsSidebarColumn()

            Spacer(Modifier.width(LCARS_GAP))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                content()
            }
        }

        Spacer(Modifier.height(LCARS_GAP))

        // ── Bottom bar ────────────────────────────────────────────────────────
        LcarsBottomBar(statusText = statusText)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top bar: orange full-width bar.
//   Left LCARS_SIDEBAR_WIDTH = the elbow's horizontal arm (with concave cutout).
//   Right portion = title bar.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LcarsTopBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LCARS_TOP_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Elbow horizontal arm — orange with concave bite at bottom-right
        Canvas(
            modifier = Modifier
                .width(LCARS_SIDEBAR_WIDTH)
                .fillMaxHeight()
        ) {
            drawRect(LcarsOrange)
            // Black circle centered at bottom-right corner creates the concave arc
            drawCircle(
                color = Color.Black,
                radius = size.height * 1.15f,
                center = Offset(size.width, size.height)
            )
        }

        // Title bar — solid orange, full remaining width
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(LcarsOrange),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title.uppercase(),
                    fontFamily = AntonioFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 14.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sidebar column: coloured block towers separated by black gaps.
// The blocks use weight() so they scale naturally with screen height.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LcarsSidebarColumn() {
    Column(
        modifier = Modifier
            .width(LCARS_SIDEBAR_WIDTH)
            .fillMaxHeight()
    ) {
        // Tier 1 — orange cap
        Box(Modifier.fillMaxWidth().weight(2.5f).background(LcarsOrange))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().weight(0.8f).background(LcarsTan))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(0.7f).background(LcarsRed))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(0.7f).background(LcarsPurple))
        Spacer(Modifier.height(5.dp))
        // Tier 2 — blue block
        Box(Modifier.fillMaxWidth().weight(1.4f).background(LcarsBlueBell))
        Spacer(Modifier.height(8.dp))
        // Tier 3 — orange main section
        Box(Modifier.fillMaxWidth().weight(0.9f).background(LcarsOrange))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(0.6f).background(LcarsTan))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(3.5f).background(LcarsOrange))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(0.5f).background(LcarsMelrose))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(0.5f).background(LcarsRed))
        Spacer(Modifier.height(6.dp))
        // Tier 4 — lower accents
        Box(Modifier.fillMaxWidth().weight(0.9f).background(LcarsTan))
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().weight(1.3f).background(LcarsOrange))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom bar: tan full-width bar.
//   Left LCARS_SIDEBAR_WIDTH = elbow base arm (with concave bite at top-right).
//   Right portion = status text bar.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LcarsBottomBar(statusText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(LCARS_BOTTOM_BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Elbow base arm — tan with concave bite at top-right
        Canvas(
            modifier = Modifier
                .width(LCARS_SIDEBAR_WIDTH)
                .fillMaxHeight()
        ) {
            drawRect(LcarsTan)
            // Black circle centered at top-right corner
            drawCircle(
                color = Color.Black,
                radius = size.height * 1.15f,
                center = Offset(size.width, 0f)
            )
        }

        // Status bar — solid tan, full remaining width
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(LcarsTan),
            contentAlignment = Alignment.CenterStart
        ) {
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText.uppercase(),
                    fontFamily = AntonioFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.0.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}
