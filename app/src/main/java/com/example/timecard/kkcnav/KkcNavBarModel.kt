package com.example.timecard.kkcnav

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

/** MorphingNavIconRow tint rules: bold mode uses the contrast-picked frosted content color. */
internal fun kkcNavTints(payload: KkcNavBarPayload): KkcNavTints =
    if (payload.boldMode) {
        val onGlass = Color(payload.frostedContent)
        KkcNavTints(selected = onGlass, unselected = onGlass.copy(alpha = 0.8f))
    } else {
        KkcNavTints(selected = Color(payload.primary), unselected = Color(payload.onSurfaceVariant))
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
