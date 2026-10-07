package com.example.timecard.kkcnav

import androidx.compose.runtime.Immutable

/**
 * KEEP IN SYNC — byte-identical keys, VERSION and payload fields with Hours Tracker
 * C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\navigation\KkcNavBarContract.kt
 * Tests pin both sides to the same CANONICAL_KEYS list:
 *   KKC  app/src/test/java/com/kkc/sheettracker/navigation/KkcNavBarContractTest.kt
 *   HT   app/src/test/java/com/example/timecard/kkcnav/KkcNavBarContractTest.kt
 * Adding/removing/renaming a key or changing a value's type: update both files AND both tests,
 * and bump VERSION on both sides (an HT build that sees an unknown VERSION shows no bar, so old
 * and new builds stay safe in either order).
 */
object KkcNavBarContract {
    const val VERSION = 1

    const val EXTRA_VERSION = "extra_kkc_navbar_version"
    const val EXTRA_DESTINATIONS = "extra_kkc_navbar_destinations"
    const val EXTRA_SUPPLY_COUNT = "extra_kkc_navbar_supply_count"
    const val EXTRA_SAFETY_COUNT = "extra_kkc_navbar_safety_count"
    const val EXTRA_DARK = "extra_kkc_navbar_dark"
    const val EXTRA_PRIMARY = "extra_kkc_navbar_primary"
    const val EXTRA_ON_SURFACE_VARIANT = "extra_kkc_navbar_on_surface_variant"
    const val EXTRA_SURFACE_VARIANT = "extra_kkc_navbar_surface_variant"
    const val EXTRA_FROSTED_BASE = "extra_kkc_navbar_frosted_base"
    const val EXTRA_FROSTED_CONTENT = "extra_kkc_navbar_frosted_content"
    const val EXTRA_FROSTED_ALPHA = "extra_kkc_navbar_frosted_alpha"
    const val EXTRA_FROSTED_BLUR_DP = "extra_kkc_navbar_frosted_blur_dp"
    const val EXTRA_BOLD_MODE = "extra_kkc_navbar_bold_mode"
    const val EXTRA_BOLD_GRADIENT = "extra_kkc_navbar_bold_gradient"
    const val EXTRA_BADGE_CONTAINER = "extra_kkc_navbar_badge_container"
    const val EXTRA_BADGE_CONTENT = "extra_kkc_navbar_badge_content"
    const val EXTRA_INDICATOR_CORNER_DP = "extra_kkc_navbar_indicator_corner_dp"
    const val EXTRA_ANIM_DISABLED = "extra_kkc_navbar_anim_disabled"
    const val EXTRA_BLUR_DISABLED = "extra_kkc_navbar_blur_disabled"
    const val EXTRA_SHADOWS_DISABLED = "extra_kkc_navbar_shadows_disabled"

    val ALL_NAVBAR_KEYS = setOf(
        EXTRA_VERSION, EXTRA_DESTINATIONS, EXTRA_SUPPLY_COUNT, EXTRA_SAFETY_COUNT, EXTRA_DARK,
        EXTRA_PRIMARY, EXTRA_ON_SURFACE_VARIANT, EXTRA_SURFACE_VARIANT, EXTRA_FROSTED_BASE,
        EXTRA_FROSTED_CONTENT, EXTRA_FROSTED_ALPHA, EXTRA_FROSTED_BLUR_DP, EXTRA_BOLD_MODE,
        EXTRA_BOLD_GRADIENT, EXTRA_BADGE_CONTAINER, EXTRA_BADGE_CONTENT, EXTRA_INDICATOR_CORNER_DP,
        EXTRA_ANIM_DISABLED, EXTRA_BLUR_DISABLED, EXTRA_SHADOWS_DISABLED
    )

    // Return path (Hours Tracker → KKC)
    const val EXTRA_NAV_DESTINATION = "extra_kkc_nav_destination"
    const val DEST_CALCULATOR = "calculator"
    const val DEST_HOURS = "hours"
    const val KKC_PACKAGE = "com.kkc.sheettracker"
    const val KKC_ACTIVITY = "com.kkc.sheettracker.MainActivity"
}

/** Resolved KKC navbar look. Colors are ARGB ints. Same class exists in KKC. */
// Stable so Compose can skip/recapture correctly; equality is data-class equals.
@Immutable
data class KkcNavBarPayload(
    val destinations: List<String>,
    val supplyCount: Int,
    val safetyCount: Int,
    val dark: Boolean,
    val primary: Int,
    val onSurfaceVariant: Int,
    val surfaceVariant: Int,
    val frostedBase: Int,
    val frostedContent: Int,
    val frostedAlpha: Float,
    val frostedBlurDp: Float,
    val boldMode: Boolean,
    val boldGradient: List<Int>,
    val badgeContainer: Int,
    val badgeContent: Int,
    val indicatorCornerDp: Float,
    val animDisabled: Boolean,
    val blurDisabled: Boolean,
    val shadowsDisabled: Boolean
) {
    companion object {
        /**
         * Returns null (no bar, current behavior) unless the version matches and every required
         * value is present with the right type. [get] reads one intent extra by key.
         */
        fun parse(get: (String) -> Any?): KkcNavBarPayload? = with(KkcNavBarContract) {
            if (get(EXTRA_VERSION) as? Int != VERSION) return null
            val destinations = (get(EXTRA_DESTINATIONS) as? Array<*>)
                ?.mapNotNull { it as? String }
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            val gradient = (get(EXTRA_BOLD_GRADIENT) as? IntArray)?.toList()?.takeIf { it.isNotEmpty() }
                ?: return null
            fun int(key: String) = get(key) as? Int
            fun float(key: String) = get(key) as? Float
            fun bool(key: String) = get(key) as? Boolean
            KkcNavBarPayload(
                destinations = destinations,
                supplyCount = int(EXTRA_SUPPLY_COUNT) ?: 0,
                safetyCount = int(EXTRA_SAFETY_COUNT) ?: 0,
                dark = bool(EXTRA_DARK) ?: return null,
                primary = int(EXTRA_PRIMARY) ?: return null,
                onSurfaceVariant = int(EXTRA_ON_SURFACE_VARIANT) ?: return null,
                surfaceVariant = int(EXTRA_SURFACE_VARIANT) ?: return null,
                frostedBase = int(EXTRA_FROSTED_BASE) ?: return null,
                frostedContent = int(EXTRA_FROSTED_CONTENT) ?: return null,
                frostedAlpha = float(EXTRA_FROSTED_ALPHA) ?: return null,
                frostedBlurDp = float(EXTRA_FROSTED_BLUR_DP) ?: return null,
                boldMode = bool(EXTRA_BOLD_MODE) ?: return null,
                boldGradient = gradient,
                badgeContainer = int(EXTRA_BADGE_CONTAINER) ?: return null,
                badgeContent = int(EXTRA_BADGE_CONTENT) ?: return null,
                indicatorCornerDp = float(EXTRA_INDICATOR_CORNER_DP) ?: return null,
                animDisabled = bool(EXTRA_ANIM_DISABLED) ?: return null,
                blurDisabled = bool(EXTRA_BLUR_DISABLED) ?: return null,
                shadowsDisabled = bool(EXTRA_SHADOWS_DISABLED) ?: return null
            )
        }
    }
}
