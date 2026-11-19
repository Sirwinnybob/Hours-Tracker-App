package com.example.timecard.ui.timesheet.lcars

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One coded side panel (thelcars sidebar panel): label bottom-right, tappable. */
data class LcarsPanel(
    val label: String,
    val color: Color,
    val height: Dp = 64.dp,
    val selected: Boolean = false,
    val onClick: (() -> Unit)? = null
)

private val MARGIN = 8.dp
private val SIDE_W = 148.dp
private val ELBOW_R = 90.dp
private val INNER_R = 40.dp
private val BAR_H = 20.dp
private val BAR_GAP = 6.dp
private val PANEL_GAP = 4.dp
private val HEADER_H = 120.dp
private val BAND_H = 52.dp
private val BOTTOM_ELBOW_H = 100.dp
private val SPINE_W = 92.dp
/** Bottom elbow arm before the KKC blocks: the inner curve plus a short straight run. */
private val ELBOW_ARM_W = INNER_R + 20.dp

/**
 * The LCARS Classic frame (portrait and landscape): a header elbow (title + actions), a split double bar,
 * the main elbow with coded side [panels], and a bottom elbow carrying [bottomBand] (KKC destinations)
 * or [bottomLabel]. Landscape KKC launches put the destinations on a [rightSpine] instead.
 * Everything sweeps / drops in once on first show unless [animate] is false (KKC animations off).
 */
@Composable
fun LcarsClassicFrame(
    title: String,
    headerCode: String,
    panels: List<LcarsPanel>,
    animate: Boolean,
    modifier: Modifier = Modifier,
    subtitle: @Composable RowScope.() -> Unit = {},
    headerActions: @Composable RowScope.() -> Unit = {},
    bottomBand: (@Composable RowScope.() -> Unit)? = null,
    bottomLabel: String = "",
    rightSpine: (@Composable ColumnScope.() -> Unit)? = null,
    entranceDelayMs: Int = 0,
    content: @Composable () -> Unit
) {
    val gate = androidx.compose.runtime.remember { android.os.SystemClock.uptimeMillis() + entranceDelayMs }
    androidx.compose.runtime.CompositionLocalProvider(LocalLcarsEntranceGate provides gate, LocalLcarsAnimate provides animate) {
        FrameBody(title, headerCode, panels, animate, modifier, subtitle, headerActions, bottomBand, bottomLabel, rightSpine, content)
    }
}

@Composable
private fun FrameBody(
    title: String,
    headerCode: String,
    panels: List<LcarsPanel>,
    animate: Boolean,
    modifier: Modifier,
    subtitle: @Composable RowScope.() -> Unit,
    headerActions: @Composable RowScope.() -> Unit,
    bottomBand: (@Composable RowScope.() -> Unit)?,
    bottomLabel: String,
    rightSpine: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable () -> Unit
) {
    val headerColor = LcarsClassic.Bluey
    val mainColor = panels.firstOrNull()?.color ?: LcarsClassic.Red
    val bottomColor = LcarsClassic.Almond

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val w = maxWidth
        val h = maxHeight
        val barStart = MARGIN + SIDE_W
        val bar1Top = MARGIN + HEADER_H - BAR_H
        val bar2Top = bar1Top + BAR_H + BAR_GAP
        val bandTop = h - MARGIN - BAND_H
        val spineW = if (rightSpine != null) SPINE_W + MARGIN else 0.dp

        // ── Header elbow ────────────────────────────────────────────────────────
        val headerIn = rememberEntrance(animate, 0)
        Box(
            Modifier.offset(MARGIN, MARGIN).size(SIDE_W, HEADER_H)
                .graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0f); scaleY = headerIn }
                .clip(RoundedCornerShape(bottomStart = ELBOW_R)).background(headerColor)
        ) {
            Text(headerCode, style = lcarsText(15.sp, Color.Black), modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp))
        }
        InnerCorner(barStart, bar1Top - INNER_R, headerColor, bottom = true, visible = headerIn)
        SplitBar(
            barStart, bar1Top, w - barStart - MARGIN, animate, baseDelay = 100,
            segments = listOf(210.dp to headerColor, 26.dp to LcarsClassic.Orange, 110.dp to LcarsClassic.Violet, Dp.Unspecified to LcarsClassic.Violet, 26.dp to LcarsClassic.Red)
        )

        val headerTextIn = rememberEntrance(animate, 200)
        Text(
            title.uppercase(),
            style = lcarsText(42.sp, LcarsClassic.Orange, FontWeight.Normal).copy(lineHeight = 44.sp),
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = MARGIN, end = 14.dp + spineW).graphicsLayer { alpha = headerTextIn }
        )
        Row(
            Modifier.offset(barStart + 30.dp, 18.dp).graphicsLayer { alpha = headerTextIn },
            verticalAlignment = Alignment.CenterVertically,
            content = subtitle
        )
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 62.dp, end = 14.dp + spineW).graphicsLayer { alpha = headerTextIn },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = headerActions
        )

        // ── Main elbow + side panels ────────────────────────────────────────────
        SplitBar(
            barStart, bar2Top, w - barStart - MARGIN, animate, baseDelay = 150,
            segments = listOf(210.dp to mainColor, 26.dp to LcarsClassic.Butter, 110.dp to LcarsClassic.Red, Dp.Unspecified to LcarsClassic.Violet, 26.dp to LcarsClassic.Butter),
            halfHeightIndex = 2
        )
        val mainIn = rememberEntrance(animate, 150)
        InnerCorner(barStart, bar2Top + BAR_H, mainColor, bottom = false, visible = mainIn)
        val columnH = bandTop - BOTTOM_ELBOW_H + BAND_H - bar2Top - PANEL_GAP
        // Short screens (landscape): shrink the panels evenly so they all fit above the bottom elbow.
        val needed = panels.fold(6.dp + 24.dp) { acc, p -> acc + p.height + PANEL_GAP }
        val fit = if (needed > columnH) (columnH / needed).coerceIn(0.6f, 1f) else 1f
        Column(
            Modifier.offset(MARGIN, bar2Top).width(SIDE_W).height(columnH),
            verticalArrangement = Arrangement.spacedBy(PANEL_GAP)
        ) {
            panels.forEachIndexed { i, p ->
                SidePanel(p.copy(height = p.height * fit), first = i == 0, animate = animate, delayMs = 200 + i * 50)
            }
            val fillIn = rememberEntrance(animate, 200 + panels.size * 50)
            Box(Modifier.fillMaxWidth().weight(1f).graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0f); scaleY = fillIn }.background(bottomColor))
        }

        // ── Bottom elbow + band ─────────────────────────────────────────────────
        val bottomIn = rememberEntrance(animate, 250)
        Box(
            Modifier.offset(MARGIN, h - MARGIN - BOTTOM_ELBOW_H).size(SIDE_W, BOTTOM_ELBOW_H)
                .graphicsLayer { transformOrigin = TransformOrigin(0.5f, 1f); scaleY = bottomIn }
                .clip(RoundedCornerShape(bottomStart = ELBOW_R)).background(bottomColor)
        )
        InnerCorner(barStart, bandTop - INNER_R, bottomColor, bottom = true, visible = bottomIn)
        Row(
            Modifier.offset(barStart, bandTop).width(w - barStart - MARGIN).height(BAND_H),
            horizontalArrangement = Arrangement.spacedBy(PANEL_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bottomBand != null) {
                // The elbow's arm runs on under the inner curve in its own color before the first
                // destination block, so the curve flows into the band (TNG frames never start a
                // button right at the curve).
                Box(
                    Modifier.width(ELBOW_ARM_W).height(BAND_H)
                        .graphicsLayer { transformOrigin = TransformOrigin(0f, 0.5f); scaleX = bottomIn }
                        .background(bottomColor)
                )
                bottomBand()
            } else {
                val bandIn = rememberEntrance(animate, 400)
                Box(
                    Modifier.weight(1f).height(BAND_H).graphicsLayer { transformOrigin = TransformOrigin(0f, 0.5f); scaleX = bandIn }
                        .clip(RoundedCornerShape(topEnd = BAND_H / 2, bottomEnd = BAND_H / 2)).background(bottomColor)
                ) {
                    Text(bottomLabel.uppercase(), style = lcarsText(14.sp, Color.Black), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 6.dp))
                }
            }
        }

        if (rightSpine != null) {
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = bar2Top + BAR_H + 12.dp, end = MARGIN, bottom = MARGIN + BAND_H + 12.dp).width(SPINE_W),
                content = rightSpine
            )
        }

        // ── Content ─────────────────────────────────────────────────────────────
        val contentIn = rememberEntrance(animate, 450)
        Box(
            Modifier.fillMaxSize()
                .padding(start = barStart + 26.dp, top = bar2Top + BAR_H + 14.dp, end = 16.dp + spineW, bottom = MARGIN + BAND_H + 12.dp)
                .graphicsLayer { alpha = contentIn }
        ) { content() }
    }
}

/** A split double-bar row: fixed-width colored segments with 4dp gaps; one may be half height. */
@Composable
private fun SplitBar(
    x: Dp, y: Dp, width: Dp, animate: Boolean, baseDelay: Int,
    segments: List<Pair<Dp, Color>>,
    halfHeightIndex: Int = -1
) {
    Row(Modifier.offset(x, y).width(width).height(BAR_H), horizontalArrangement = Arrangement.spacedBy(PANEL_GAP)) {
        segments.forEachIndexed { i, (segW, color) ->
            val p = rememberEntrance(animate, baseDelay + i * 70, durationMs = 260)
            val sized = if (segW == Dp.Unspecified) Modifier.weight(1f) else Modifier.width(segW)
            Box(
                sized.height(if (i == halfHeightIndex) BAR_H / 2 else BAR_H)
                    .graphicsLayer { transformOrigin = TransformOrigin(0f, 0.5f); scaleX = p }
                    .background(color)
            )
        }
    }
}

/** The small inside curve where an elbow meets its bar. */
@Composable
private fun InnerCorner(x: Dp, y: Dp, color: Color, bottom: Boolean, visible: Float) {
    Box(Modifier.offset(x, y).size(INNER_R).graphicsLayer { alpha = visible }.background(color))
    Box(
        Modifier.offset(x, y).size(INNER_R).background(
            Color.Black,
            if (bottom) RoundedCornerShape(bottomStart = INNER_R) else RoundedCornerShape(topStart = INNER_R)
        )
    )
}

@Composable
private fun ColumnScope.SidePanel(p: LcarsPanel, first: Boolean, animate: Boolean, delayMs: Int) {
    val drop = rememberEntrance(animate, delayMs, durationMs = 240)
    val color by animateColorAsState(if (p.selected) LcarsClassic.Creme else p.color, tween(220), label = "lcarsPanel")
    Box(
        Modifier.fillMaxWidth().height(if (first) p.height + 6.dp else p.height)
            .graphicsLayer { transformOrigin = TransformOrigin(0.5f, 0f); scaleY = drop }
            .clip(if (first) RoundedCornerShape(topStart = ELBOW_R) else RoundedCornerShape(0.dp))
            .background(color)
            .then(if (p.onClick != null) Modifier.clickable(role = Role.Tab, onClickLabel = p.label, onClick = p.onClick) else Modifier)
            .semantics { selected = p.selected }
    ) {
        Text(p.label.uppercase(), style = lcarsText(15.sp, Color.Black), maxLines = 1, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 6.dp))
    }
}
