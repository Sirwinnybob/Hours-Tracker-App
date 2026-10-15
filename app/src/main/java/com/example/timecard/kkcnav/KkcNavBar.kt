package com.example.timecard.kkcnav

// ═══════════════════════════════════════════════════════════════════════════════════════════
// KEEP IN SYNC — THIS IS A COPY OF KKCSHEETTRACKER'S BOTTOM NAVBAR
// Source of truth: C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\
//   ui/components/AppScaffold.kt   MorphingNavBar + MorphingNavIconRow (full state) → KkcNavBar.kt
//   ui/components/AppScaffold.kt   enum NavDestination labels / badge + tint rules → KkcNavBarModel.kt
//   ui/components/icons/NavIcons.kt + IconDsl.kt  → KkcNavIcons.kt + KkcIconDsl.kt (verbatim copies)
//   ui/theme/Type.kt  InterFontFamily + labelSmall → KkcNavTypography.kt
//   ui/theme/Spacing.kt  navBarHorizontal 24dp, floatingNavMinSideMargin 24dp, floatingNavBottomGap 12dp
//   navigation/KkcNavBarContract.kt + KkcNavBarPayloadBuilder.kt → KkcNavBarContract.kt
// Layout, glass and animation are KKC's. COLORS are picked by KkcNavBarStyle.kt (HT-only): standard HT
// theme → KKC's light/dark set for HT's effective mode; equipped HT theme → its colors; LCARS → KkcLcarsNav.kt.
// Any change to the KKC files above must be copied here in the same session; any contract
// change bumps VERSION in both repos. Full rules: Hours Tracker AGENTS.md "KKC navbar mirror".
// Fallback "← KKC" buttons (TimesheetScreen.kt, NameCard.kt) are intentionally kept.
// ═══════════════════════════════════════════════════════════════════════════════════════════

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * selected destination. Layout, glass and animation come from KKC via [payload]; colors come from
 * [style] (KkcNavBarStyle.kt): KKC's light or dark set for the standard Hours Tracker theme, or the
 * equipped Hours Tracker theme's colors. LCARS never uses this bar (see KkcLcarsNav.kt).
 */
@Composable
internal fun KkcNavBar(
    payload: KkcNavBarPayload,
    style: KkcNavBarStyle,
    hazeState: HazeState?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = style.colors
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
        val frostedBase = Color(colors.frostedBase)
        val fillAlpha = kkcFrostedFillAlpha(payload)
        val blurActive = hazeState != null && !payload.blurDisabled
        val hazeModifier = if (blurActive) {
            Modifier.hazeEffect(
                hazeState,
                style = HazeDefaults.style(
                    backgroundColor = frostedBase.copy(alpha = fillAlpha),
                    blurRadius = payload.frostedBlurDp.coerceAtLeast(1f).dp
                )
            )
        } else {
            Modifier.background(frostedBase)
        }
        // Mirror KKC's external shadow -> clip -> Haze layer; no Surface shadow through glass.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (payload.shadowsDisabled) 0.dp else 3.dp, barShape, clip = false)
                .clip(barShape)
                .then(hazeModifier)
        ) {
            KkcNavIconRow(payload, style, onNavigate, iconSize, hPad, vPad)
        }
    }
}

@Composable
private fun KkcNavIconRow(
    payload: KkcNavBarPayload,
    style: KkcNavBarStyle,
    onNavigate: (String) -> Unit,
    iconSize: Dp,
    hPad: Dp,
    vPad: Dp
) {
    val colors = style.colors
    val indicatorShape = remember(payload.indicatorCornerDp) { RoundedCornerShape(payload.indicatorCornerDp.dp) }
    val tints = kkcNavTints(style)
    val selectionFill = remember(colors, style.boldMode) { kkcNavSelectionFill(colors, style.boldMode) }
    fun Modifier.navSelectionBackground(active: Boolean): Modifier {
        if (!active) return this
        return when (val fill = selectionFill) {
            // KKC boldGradientColors always returns two stops (secondary falls back to primary).
            is KkcNavSelectionFill.Gradient ->
                background(brush = Brush.linearGradient(fill.stops), shape = indicatorShape, alpha = 0.55f)
            is KkcNavSelectionFill.Solid -> background(color = fill.color, shape = indicatorShape)
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
                                    containerColor = Color(colors.badgeContainer),
                                    contentColor = Color(colors.badgeContent)
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
