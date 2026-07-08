package com.example.timecard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── LCARS chrome dimensions ───────────────────────────────────────────────────
val LCARS_SIDEBAR_WIDTH: Dp  = 108.dp
val LCARS_TOP_BAR_HEIGHT: Dp = 40.dp
val LCARS_BOTTOM_BAR_HEIGHT: Dp = 30.dp
val LCARS_GAP: Dp = 6.dp

// Legacy alias kept so callers compile
val LCARS_ELBOW_HEIGHT: Dp = LCARS_TOP_BAR_HEIGHT

/**
 * Full-frame LCARS chrome wrapper.
 *
 * Renders an authentic, contiguous LCARS interface frame around [content]:
 *   • Left spine: top-left elbow, vertical sidebar buttons, bottom-left elbow.
 *   • Right side: top header, content cutout, bottom footer.
 *   • Gaps only separate the right-side components and the left-side spine,
 *     ensuring the left spine remains a single continuous piece.
 */
@Composable
fun LcarsFrame(
    modifier: Modifier = Modifier,
    headerContent: @Composable RowScope.() -> Unit = {},
    footerContent: @Composable RowScope.() -> Unit = {},
    sidebarContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Left Column: Unbroken Spine ──────────────────────────────────────
        Column(
            modifier = Modifier
                .width(LCARS_SIDEBAR_WIDTH) // 108.dp
                .fillMaxHeight()
        ) {
            // Top Left Elbow (w = 80.dp, r = 28.dp)
            LcarsTopLeftElbow(
                color = LcarsOrange,
                w = 80.dp,
                r = 28.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Spacer(Modifier.height(LCARS_GAP))

            // Sidebar stack (Buttons are 80.dp wide)
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(LCARS_GAP)
            ) {
                sidebarContent()
            }

            Spacer(Modifier.height(LCARS_GAP))

            // Bottom Left Elbow (w = 80.dp, r = 28.dp)
            LcarsBottomLeftElbow(
                color = LcarsTan,
                w = 80.dp,
                r = 28.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }

        Spacer(modifier = Modifier.width(LCARS_GAP))

        // ── Right Column: Content + Headers ──────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp) // aligns with horizontal arm of Top Left Elbow
                    .background(LcarsOrange),
                verticalAlignment = Alignment.CenterVertically,
                content = headerContent
            )

            Spacer(Modifier.height(LCARS_GAP))

            // Middle Content Cutout
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                content()
            }

            Spacer(Modifier.height(LCARS_GAP))

            // Footer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp) // aligns with horizontal arm of Bottom Left Elbow
                    .background(LcarsTan)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = footerContent
            )
        }
    }
}

@Composable
fun LcarsTopLeftElbow(
    color: Color,
    modifier: Modifier = Modifier,
    w: Dp = 80.dp,
    h: Dp = 28.dp,
    r: Dp = 28.dp,
    rOuter: Dp = 20.dp
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val wPx = w.toPx()
        val hPx = h.toPx()
        val rPx = r.toPx()
        val rOuterPx = rOuter.toPx()
        val W = size.width
        val H = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, H)
            lineTo(0f, rOuterPx)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 0f,
                    top = 0f,
                    right = rOuterPx * 2f,
                    bottom = rOuterPx * 2f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(W, 0f)
            lineTo(W, hPx)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = wPx,
                    top = hPx,
                    right = wPx + rPx * 2f,
                    bottom = hPx + rPx * 2f
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(wPx, H)
            lineTo(0f, H)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun LcarsBottomLeftElbow(
    color: Color,
    modifier: Modifier = Modifier,
    w: Dp = 80.dp,
    h: Dp = 20.dp,
    r: Dp = 28.dp,
    rOuter: Dp = 20.dp
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val wPx = w.toPx()
        val hPx = h.toPx()
        val rPx = r.toPx()
        val rOuterPx = rOuter.toPx()
        val W = size.width
        val H = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(wPx, 0f)
            lineTo(wPx, H - hPx - rPx)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = wPx,
                    top = H - hPx - rPx * 2f,
                    right = wPx + rPx * 2f,
                    bottom = H - hPx
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -90f,
                forceMoveTo = false
            )
            lineTo(W, H - hPx)
            lineTo(W, H)
            lineTo(rOuterPx, H)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 0f,
                    top = H - rOuterPx * 2f,
                    right = rOuterPx * 2f,
                    bottom = H
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, 0f)
            close()
        }
        drawPath(path = path, color = color)
    }
}
