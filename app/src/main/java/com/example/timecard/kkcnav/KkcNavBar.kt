package com.example.timecard.kkcnav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

// Animation spec copied verbatim from KKC AppScaffold.kt (NavSpringDp). The mirrored bar only
// ever shows KKC's full state, so these targets never change today; they are kept so any future
// morph moves exactly like KKC's, and snap() in low-end mode matches KKC.
private val KkcNavSpringDp = spring<Dp>(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)

/**
 * Copy of KKCSheetTracker AppScaffold.kt MorphingNavBar + MorphingNavIconRow in the FULL state
 * (labels shown, not minimized, no decorations / extended controls). Hours is always the
 * selected destination. Colors come only from [payload], never from Hours Tracker's theme.
 */
@Composable
fun KkcNavBar(
    payload: KkcNavBarPayload,
    hazeState: HazeState?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navSpec = if (!payload.animDisabled) KkcNavSpringDp else snap()
    val iconSize by animateDpAsState(22.dp, navSpec, label = "kkcNavIconSize")
    val hPad by animateDpAsState(14.dp, navSpec, label = "kkcNavItemHPad")
    val vPad by animateDpAsState(8.dp, navSpec, label = "kkcNavItemVPad")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        val barShape = remember { RoundedCornerShape(20.dp) }
        val frostedBase = Color(payload.frostedBase)
        val fillAlpha = kkcFrostedFillAlpha(payload)
        val blurActive = hazeState != null && !payload.blurDisabled
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = barShape,
            color = if (blurActive) Color.Transparent else frostedBase.copy(alpha = fillAlpha),
            shadowElevation = if (payload.shadowsDisabled) 0.dp else 3.dp,
            tonalElevation = 0.dp
        ) {
            val hazeModifier = if (blurActive) {
                Modifier.hazeEffect(
                    hazeState,
                    style = HazeDefaults.style(
                        backgroundColor = frostedBase.copy(alpha = fillAlpha),
                        blurRadius = payload.frostedBlurDp.coerceAtLeast(1f).dp
                    )
                )
            } else {
                Modifier
            }
            Box(modifier = Modifier.fillMaxWidth().then(hazeModifier)) {
                KkcNavIconRow(payload, onNavigate, iconSize, hPad, vPad)
            }
        }
    }
}

@Composable
private fun KkcNavIconRow(
    payload: KkcNavBarPayload,
    onNavigate: (String) -> Unit,
    iconSize: Dp,
    hPad: Dp,
    vPad: Dp
) {
    val indicatorShape = remember(payload.indicatorCornerDp) { RoundedCornerShape(payload.indicatorCornerDp.dp) }
    val tints = kkcNavTints(payload)
    val boldBrush = remember(payload.boldGradient) {
        val stops = payload.boldGradient.map { Color(it) }
        // KKC boldGradientColors always returns two stops (secondary falls back to primary).
        Brush.linearGradient(if (stops.size == 1) stops + stops else stops)
    }
    fun Modifier.navSelectionBackground(active: Boolean): Modifier {
        if (!active) return this
        return if (payload.boldMode) {
            background(brush = boldBrush, shape = indicatorShape, alpha = 0.55f)
        } else {
            background(color = Color(payload.surfaceVariant), shape = indicatorShape)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        kkcNavSlots(payload.destinations).forEach { route ->
            val isCalculator = route == KkcNavBarContract.DEST_CALCULATOR
            // Hours is the destination being shown; the calculator is never open in Hours Tracker.
            val selected = route == KkcNavBarContract.DEST_HOURS
            val tint = if (selected) tints.selected else tints.unselected
            val label = kkcNavLabel(route)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .clip(indicatorShape)
                        .navSelectionBackground(selected)
                        .clickable { onNavigate(route) }
                        .padding(horizontal = hPad, vertical = vPad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val iconContent = @Composable {
                        Icon(
                            kkcNavIcon(route, selected),
                            contentDescription = if (isCalculator) "Calculator" else label,
                            tint = tint,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    val badgeCount = kkcBadgeCount(route, payload)
                    if (badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(payload.badgeContainer),
                                    contentColor = Color(payload.badgeContent)
                                ) { Text(badgeCount.toString(), style = KkcNavLabelStyle) }
                            }
                        ) { iconContent() }
                    } else {
                        iconContent()
                    }
                    // Mirrors KKC MorphingNavIconRow: maxLines = 1 + ellipsis only in the animated
                    // branch; the low-end (animationsDisabled) branch and the Calc label are unconstrained.
                    val labelMaxLines = kkcLabelMaxLines(route, payload.animDisabled)
                    Text(
                        text = label,
                        style = KkcNavLabelStyle,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = tint,
                        maxLines = labelMaxLines,
                        overflow = if (labelMaxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
                    )
                }
            }
        }
    }
}
