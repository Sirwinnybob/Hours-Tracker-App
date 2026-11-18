package com.example.timecard.kkcnav

// ═══════════════════════════════════════════════════════════════════════════════════════════
// KEEP IN SYNC — THIS IS A COPY OF KKCSHEETTRACKER'S LANDSCAPE NAV RAIL
// Source of truth: C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\
//   ui/components/LandscapeNavRail.kt  LandscapeNavRail + railGlass + RailIconColumn + RailItem → KkcNavRail
//   ui/components/LandscapeChrome.kt   useLandscapeChrome, rememberLandscapeChrome, RailIconColumnWidth 76dp,
//                                      RailEdgeMargin 6dp, LandscapeRailClearance → kkcUseLandscapeRail / Kkc* consts
//   ui/components/SideHeader.kt        SideHeaderClock → KkcRailClock
//   ui/components/BatteryIndicator.kt  BatteryIndicator → KkcRailBattery (KkcRailSystemItems.kt)
// Only the bare rail is mirrored: Hours Tracker has no navbar decorations, so KKC's tool column
// (search / CNC / pen / Add Item) never appears here. Hours is always the selected slot.
// COLORS come from KkcNavBarStyle.kt exactly like the portrait bar (KkcNavBar.kt). LCARS and Retro
// keep their own bottom bars in landscape too (HT-only styling, no rail).
// Settings sits at the rail's bottom like KKC's and returns "settings" to KKC.
// Full rules: Hours Tracker AGENTS.md "KKC navbar mirror".
// ═══════════════════════════════════════════════════════════════════════════════════════════

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/** KKC LandscapeNavRail RailSlotHeight: one fixed slot per destination, so icons never move. */
internal val KkcRailSlotHeight = 62.dp
/** KKC LandscapeNavRail RailCornerRadius. */
internal val KkcRailCornerRadius = 20.dp
/** KKC LandscapeChrome RailIconColumnWidth. */
internal val KkcRailIconColumnWidth = 76.dp
/** KKC LandscapeChrome RailEdgeMargin: gap between the glass and the right / top / bottom edges. */
internal val KkcRailEdgeMargin = 6.dp
/** KKC LandscapeChrome LandscapeRailClearance: room content leaves on its right for the rail. */
internal val KkcLandscapeRailClearance: Dp = KkcRailIconColumnWidth + KkcRailEdgeMargin + 6.dp

/** The rail's bottom item. KKC reaches Settings from there (not from the destination list). */
internal const val KKC_DEST_SETTINGS = "settings"

/**
 * HT-only (Retro / LCARS landscape bars): KKC's rail order plus Settings at the end, standing in for
 * the Settings item at the bottom of KKC's rail. The glass rail draws Settings as its own bottom item.
 */
internal fun kkcLandscapeSlots(destinations: List<String>): List<String> {
    val slots = kkcNavSlots(destinations)
    return if (KKC_DEST_SETTINGS in slots) slots else slots + KKC_DEST_SETTINGS
}

/** KKC LandscapeChrome.useLandscapeChrome: the chrome follows the window, never a pane or the keyboard. */
internal fun kkcUseLandscapeRail(width: Float, height: Float): Boolean =
    width.isFinite() && height.isFinite() && width > 0 && height > 0 && width > height

/** KKC LandscapeChrome.rememberLandscapeChrome: landscape orientation AND a window wider than tall. */
@Composable
internal fun rememberKkcLandscapeRail(): Boolean {
    val orientation = LocalConfiguration.current.orientation
    val size = LocalWindowInfo.current.containerSize
    return orientation == Configuration.ORIENTATION_LANDSCAPE &&
        kkcUseLandscapeRail(size.width.toFloat(), size.height.toFloat())
}

/**
 * Copy of KKC LandscapeNavRail with no decoration (the only state Hours Tracker can show): ONE thin
 * frosted rail on the right edge. Clock and battery on top, destinations in fixed 62dp slots
 * centered between, Settings at the bottom. Hours is the selected slot; its highlight is drawn
 * behind the slots like KKC's sliding highlight (it never moves here).
 */
@Composable
internal fun KkcNavRail(
    payload: KkcNavBarPayload,
    style: KkcNavBarStyle,
    hazeState: HazeState?,
    darkTheme: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = remember { RoundedCornerShape(KkcRailCornerRadius) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
            .padding(top = KkcRailEdgeMargin, bottom = KkcRailEdgeMargin, end = KkcRailEdgeMargin)
            .kkcRailGlass(payload, style, hazeState, shape)
    ) {
        KkcRailIconColumn(payload, style, darkTheme, onNavigate)
    }
}

/** KKC railGlass: external shadow, clip, full-opacity Haze fill (frosted rule), gaps swallow taps. */
private fun Modifier.kkcRailGlass(
    payload: KkcNavBarPayload,
    style: KkcNavBarStyle,
    hazeState: HazeState?,
    shape: Shape
): Modifier {
    val frostedBase = Color(style.colors.frostedBase)
    val fill = if (hazeState != null && !payload.blurDisabled) {
        Modifier.hazeEffect(
            hazeState,
            style = HazeDefaults.style(
                backgroundColor = frostedBase.copy(alpha = kkcFrostedFillAlpha(payload)),
                blurRadius = payload.frostedBlurDp.coerceAtLeast(1f).dp
            )
        )
    } else {
        Modifier.background(frostedBase)
    }
    return this
        .shadow(if (payload.shadowsDisabled) 0.dp else 3.dp, shape, clip = false)
        .clip(shape)
        .then(fill)
        .clickable(interactionSource = null, indication = null, onClick = {})
}

@Composable
private fun KkcRailIconColumn(
    payload: KkcNavBarPayload,
    style: KkcNavBarStyle,
    darkTheme: Boolean,
    onNavigate: (String) -> Unit
) {
    val colors = style.colors
    val slots = remember(payload.destinations) { kkcNavSlots(payload.destinations) }
    val selectedIndex = slots.indexOf(KkcNavBarContract.DEST_HOURS)
    val slotPx = with(LocalDensity.current) { KkcRailSlotHeight.toPx() }
    val tints = kkcNavTints(style)
    val indicatorShape = remember(payload.indicatorCornerDp) { RoundedCornerShape(payload.indicatorCornerDp.dp) }
    val selectionFill = remember(colors, style.boldMode) { kkcNavSelectionFill(colors, style.boldMode) }
    fun Modifier.selectionFill(): Modifier = when (val fill = selectionFill) {
        is KkcNavSelectionFill.Gradient -> background(Brush.linearGradient(fill.stops), indicatorShape, alpha = 0.55f)
        is KkcNavSelectionFill.Solid -> background(fill.color, indicatorShape)
    }

    Column(
        modifier = Modifier
            .width(KkcRailIconColumnWidth)
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KkcRailClock(color = style.onGlass)
        KkcRailBattery(
            contentColor = style.onGlass,
            chargingColor = kkcChargingColor(darkTheme),
            modifier = Modifier.padding(top = 4.dp)
        )
        Box(
            modifier = Modifier
                .width(KkcRailIconColumnWidth)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                Box {
                    if (selectedIndex >= 0) {
                        Box(
                            Modifier
                                .graphicsLayer { translationY = selectedIndex * slotPx }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                                .width(KkcRailIconColumnWidth - 10.dp)
                                .height(KkcRailSlotHeight - 6.dp)
                                .selectionFill()
                        )
                    }
                    Column {
                        slots.forEach { route ->
                            // Hours is the screen being shown; the calculator is never open in Hours Tracker.
                            val selected = route == KkcNavBarContract.DEST_HOURS
                            val label = kkcNavLabel(route)
                            val badge = kkcBadgeCount(route, payload)
                            KkcRailItem(
                                icon = { tint ->
                                    val icon = @Composable {
                                        Icon(
                                            kkcNavIcon(route, selected),
                                            contentDescription = if (route == KkcNavBarContract.DEST_CALCULATOR) "Calculator" else label,
                                            tint = tint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    if (badge > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = Color(colors.badgeContainer),
                                                    contentColor = Color(colors.badgeContent)
                                                ) { Text(badge.toString(), style = KkcNavLabelStyle) }
                                            }
                                        ) { icon() }
                                    } else {
                                        icon()
                                    }
                                },
                                label = label,
                                tint = if (selected) tints.selected else tints.unselected,
                                indicatorShape = indicatorShape,
                                onClick = { onNavigate(route) }
                            )
                        }
                    }
                }
            }
        }
        // KKC draws a dot here when KKC has pending app updates; Hours Tracker doesn't know that.
        KkcRailItem(
            icon = { tint ->
                Icon(NavSettingsSelected, contentDescription = "Settings", tint = tint, modifier = Modifier.size(24.dp))
            },
            label = "Settings",
            tint = tints.unselected,
            indicatorShape = indicatorShape,
            onClick = { onNavigate(KKC_DEST_SETTINGS) }
        )
    }
}

/** KKC RailItem: fixed 76x62 slot, icon over a one-line SemiBold label (weight never changes). */
@Composable
private fun KkcRailItem(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    tint: Color,
    indicatorShape: Shape,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(KkcRailIconColumnWidth)
            .height(KkcRailSlotHeight)
            .padding(horizontal = 5.dp, vertical = 3.dp)
            .clip(indicatorShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            icon(tint)
            Text(
                text = label,
                style = KkcNavLabelStyle,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
