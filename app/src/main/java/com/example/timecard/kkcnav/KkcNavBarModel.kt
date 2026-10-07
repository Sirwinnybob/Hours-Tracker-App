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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** KKC NavDestination route → label (AppScaffold.kt enum NavDestination). */
internal val KKC_NAV_LABELS: Map<String, String> = linkedMapOf(
    "dashboard" to "Dashboard",
    "jobs" to "Jobs",
    "search" to "Search",
    "hours" to "Hours",
    "timecard" to "Timeclock",
    "supply" to "Supply",
    "settings" to "Settings",
    "standards" to "Library"
)

/** KKC draws the Calc slot immediately before HOURS (MorphingNavIconRow). Unknown routes are dropped. */
internal fun kkcNavSlots(destinations: List<String>): List<String> = buildList {
    destinations.filter { it in KKC_NAV_LABELS }.forEach { route ->
        if (route == KkcNavBarContract.DEST_HOURS) add(KkcNavBarContract.DEST_CALCULATOR)
        add(route)
    }
}

internal fun kkcNavLabel(route: String): String =
    if (route == KkcNavBarContract.DEST_CALCULATOR) "Calc" else KKC_NAV_LABELS.getValue(route)

internal fun kkcNavIcon(route: String, selected: Boolean): ImageVector = when (route) {
    "dashboard" -> if (selected) NavDashboardSelected else NavDashboardUnselected
    "jobs" -> if (selected) NavJobsSelected else NavJobsUnselected
    "search" -> if (selected) NavSearchSelected else NavSearchUnselected
    "hours" -> if (selected) NavHoursSelected else NavHoursUnselected
    "timecard" -> if (selected) NavTimeclockSelected else NavTimeclockUnselected
    "supply" -> if (selected) NavSupplySelected else NavSupplyUnselected
    "settings" -> if (selected) NavSettingsSelected else NavSettingsUnselected
    "standards" -> if (selected) NavLibrarySelected else NavLibraryUnselected
    "calculator" -> if (selected) NavCalculatorSelected else NavCalculatorUnselected
    else -> error("Unknown KKC nav route: $route")
}

/** MorphingNavIconRow: SUPPLY → supply count, STANDARDS (Library) → safety count, else none. */
internal fun kkcBadgeCount(route: String, payload: KkcNavBarPayload): Int = when (route) {
    "supply" -> payload.supplyCount
    "standards" -> payload.safetyCount
    else -> 0
}

internal data class KkcNavTints(val selected: Color, val unselected: Color)

/**
 * MorphingNavIconRow tint rules: bold mode uses the contrast-picked on-glass color (picked against
 * Hours Tracker's canvas — see KkcNavBarStyle.kt), otherwise primary / onSurfaceVariant.
 */
internal fun kkcNavTints(style: KkcNavBarStyle): KkcNavTints =
    if (style.boldMode) {
        KkcNavTints(selected = style.onGlass, unselected = style.onGlass.copy(alpha = 0.8f))
    } else {
        KkcNavTints(selected = Color(style.colors.primary), unselected = Color(style.colors.onSurfaceVariant))
    }

/** Fill behind the selected slot (MorphingNavIconRow navSelectionBackground), minus the 0.55 gradient alpha. */
internal sealed interface KkcNavSelectionFill {
    data class Solid(val color: Color) : KkcNavSelectionFill
    data class Gradient(val stops: List<Color>) : KkcNavSelectionFill
}

/** Bold mode: the theme gradient (KKC always has two stops; a single stop is doubled). Else surfaceVariant. */
internal fun kkcNavSelectionFill(colors: KkcNavBarColors, boldMode: Boolean): KkcNavSelectionFill =
    if (boldMode) {
        val stops = colors.boldGradient.map { Color(it) }
        KkcNavSelectionFill.Gradient(if (stops.size == 1) stops + stops else stops)
    } else {
        KkcNavSelectionFill.Solid(Color(colors.surfaceVariant))
    }

/**
 * MorphingNavIconRow label line limit. KKC draws destination labels with maxLines = 1 + ellipsis
 * only in the animated (AnimatedVisibility) branch; the low-end (animationsDisabled) branch and the
 * Calc label have no limit.
 */
internal fun kkcLabelMaxLines(route: String, animDisabled: Boolean): Int =
    if (route == KkcNavBarContract.DEST_CALCULATOR || animDisabled) Int.MAX_VALUE else 1

/** MorphingNavBar: frostedTokens.backgroundAlpha.coerceIn(0.5f, 0.95f). */
internal fun kkcFrostedFillAlpha(payload: KkcNavBarPayload): Float = payload.frostedAlpha.coerceIn(0.5f, 0.95f)
