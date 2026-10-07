package com.example.timecard.kkcnav

import androidx.compose.runtime.Immutable

/**
 * KEEP IN SYNC — byte-identical keys, VERSION and payload fields with KKCSheetTracker
 * C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\navigation\KkcNavBarContract.kt
 * Tests pin both sides to the same CANONICAL_KEYS list:
 *   KKC  app/src/test/java/com/kkc/sheettracker/navigation/KkcNavBarContractTest.kt
 *   HT   app/src/test/java/com/example/timecard/kkcnav/KkcNavBarContractTest.kt
 * Adding/removing/renaming a key or changing a value's type: update both files AND both tests,
 * and bump VERSION on both sides (an HT build that sees an unknown VERSION shows no bar, so old
 * and new builds stay safe in either order).
 *
 * v2: the bar follows HOURS TRACKER's light/dark mode, so KKC sends BOTH resolved color sets
 * (`_light` / `_dark` suffix keys; no single "dark" flag) and HT picks one at render time
 * via [KkcNavBarPayload.colorsFor]. Theme-independent values are sent once.
 */
object KkcNavBarContract {
    const val VERSION = 2

    // Theme-independent
    const val EXTRA_VERSION = "extra_kkc_navbar_version"
    const val EXTRA_DESTINATIONS = "extra_kkc_navbar_destinations"
    const val EXTRA_SUPPLY_COUNT = "extra_kkc_navbar_supply_count"
    const val EXTRA_SAFETY_COUNT = "extra_kkc_navbar_safety_count"
    const val EXTRA_FROSTED_ALPHA = "extra_kkc_navbar_frosted_alpha"
    const val EXTRA_FROSTED_BLUR_DP = "extra_kkc_navbar_frosted_blur_dp"
    const val EXTRA_BOLD_MODE = "extra_kkc_navbar_bold_mode"
    const val EXTRA_INDICATOR_CORNER_DP = "extra_kkc_navbar_indicator_corner_dp"
    const val EXTRA_ANIM_DISABLED = "extra_kkc_navbar_anim_disabled"
    const val EXTRA_BLUR_DISABLED = "extra_kkc_navbar_blur_disabled"
    const val EXTRA_SHADOWS_DISABLED = "extra_kkc_navbar_shadows_disabled"

    // Per-theme colors (light + dark sets)
    const val EXTRA_PRIMARY_LIGHT = "extra_kkc_navbar_primary_light"
    const val EXTRA_PRIMARY_DARK = "extra_kkc_navbar_primary_dark"
    const val EXTRA_ON_SURFACE_VARIANT_LIGHT = "extra_kkc_navbar_on_surface_variant_light"
    const val EXTRA_ON_SURFACE_VARIANT_DARK = "extra_kkc_navbar_on_surface_variant_dark"
    const val EXTRA_SURFACE_VARIANT_LIGHT = "extra_kkc_navbar_surface_variant_light"
    const val EXTRA_SURFACE_VARIANT_DARK = "extra_kkc_navbar_surface_variant_dark"
    const val EXTRA_FROSTED_BASE_LIGHT = "extra_kkc_navbar_frosted_base_light"
    const val EXTRA_FROSTED_BASE_DARK = "extra_kkc_navbar_frosted_base_dark"
    const val EXTRA_FROSTED_CONTENT_LIGHT = "extra_kkc_navbar_frosted_content_light"
    const val EXTRA_FROSTED_CONTENT_DARK = "extra_kkc_navbar_frosted_content_dark"
    const val EXTRA_BOLD_GRADIENT_LIGHT = "extra_kkc_navbar_bold_gradient_light"
    const val EXTRA_BOLD_GRADIENT_DARK = "extra_kkc_navbar_bold_gradient_dark"
    const val EXTRA_BADGE_CONTAINER_LIGHT = "extra_kkc_navbar_badge_container_light"
    const val EXTRA_BADGE_CONTAINER_DARK = "extra_kkc_navbar_badge_container_dark"
    const val EXTRA_BADGE_CONTENT_LIGHT = "extra_kkc_navbar_badge_content_light"
    const val EXTRA_BADGE_CONTENT_DARK = "extra_kkc_navbar_badge_content_dark"

    val ALL_NAVBAR_KEYS = setOf(
        EXTRA_VERSION, EXTRA_DESTINATIONS, EXTRA_SUPPLY_COUNT, EXTRA_SAFETY_COUNT,
        EXTRA_FROSTED_ALPHA, EXTRA_FROSTED_BLUR_DP, EXTRA_BOLD_MODE, EXTRA_INDICATOR_CORNER_DP,
        EXTRA_ANIM_DISABLED, EXTRA_BLUR_DISABLED, EXTRA_SHADOWS_DISABLED,
        EXTRA_PRIMARY_LIGHT, EXTRA_PRIMARY_DARK,
        EXTRA_ON_SURFACE_VARIANT_LIGHT, EXTRA_ON_SURFACE_VARIANT_DARK,
        EXTRA_SURFACE_VARIANT_LIGHT, EXTRA_SURFACE_VARIANT_DARK,
        EXTRA_FROSTED_BASE_LIGHT, EXTRA_FROSTED_BASE_DARK,
        EXTRA_FROSTED_CONTENT_LIGHT, EXTRA_FROSTED_CONTENT_DARK,
        EXTRA_BOLD_GRADIENT_LIGHT, EXTRA_BOLD_GRADIENT_DARK,
        EXTRA_BADGE_CONTAINER_LIGHT, EXTRA_BADGE_CONTAINER_DARK,
        EXTRA_BADGE_CONTENT_LIGHT, EXTRA_BADGE_CONTENT_DARK
    )

    // Return path (Hours Tracker → KKC)
    const val EXTRA_NAV_DESTINATION = "extra_kkc_nav_destination"
    const val DEST_CALCULATOR = "calculator"
    const val DEST_HOURS = "hours"
    const val KKC_PACKAGE = "com.kkc.sheettracker"
    const val KKC_ACTIVITY = "com.kkc.sheettracker.MainActivity"
}

/** One theme's resolved navbar colors (ARGB ints). Same class exists in KKC. */
@Immutable
data class KkcNavBarColors(
    val primary: Int,
    val onSurfaceVariant: Int,
    val surfaceVariant: Int,
    val frostedBase: Int,
    val frostedContent: Int,
    val boldGradient: List<Int>,
    val badgeContainer: Int,
    val badgeContent: Int
)

/** Resolved KKC navbar look. Colors are ARGB ints. Same class exists in KKC. */
// Stable so Compose can skip/recapture correctly; equality is data-class equals.
@Immutable
data class KkcNavBarPayload(
    val destinations: List<String>,
    val supplyCount: Int,
    val safetyCount: Int,
    val light: KkcNavBarColors,
    val dark: KkcNavBarColors,
    val frostedAlpha: Float,
    val frostedBlurDp: Float,
    val boldMode: Boolean,
    val indicatorCornerDp: Float,
    val animDisabled: Boolean,
    val blurDisabled: Boolean,
    val shadowsDisabled: Boolean
) {
    fun colorsFor(darkTheme: Boolean): KkcNavBarColors = if (darkTheme) dark else light

    companion object {
        /**
         * Returns null (no bar, current behavior) unless the version matches and every required
         * value is present with the right type, for BOTH themes. [get] reads one intent extra by key.
         */
        fun parse(get: (String) -> Any?): KkcNavBarPayload? = with(KkcNavBarContract) {
            if (get(EXTRA_VERSION) as? Int != VERSION) return null
            val destinations = (get(EXTRA_DESTINATIONS) as? Array<*>)
                ?.mapNotNull { it as? String }
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            fun int(key: String) = get(key) as? Int
            fun float(key: String) = get(key) as? Float
            fun bool(key: String) = get(key) as? Boolean
            fun gradient(key: String) = (get(key) as? IntArray)?.toList()?.takeIf { it.isNotEmpty() }
            val light = KkcNavBarColors(
                primary = int(EXTRA_PRIMARY_LIGHT) ?: return null,
                onSurfaceVariant = int(EXTRA_ON_SURFACE_VARIANT_LIGHT) ?: return null,
                surfaceVariant = int(EXTRA_SURFACE_VARIANT_LIGHT) ?: return null,
                frostedBase = int(EXTRA_FROSTED_BASE_LIGHT) ?: return null,
                frostedContent = int(EXTRA_FROSTED_CONTENT_LIGHT) ?: return null,
                boldGradient = gradient(EXTRA_BOLD_GRADIENT_LIGHT) ?: return null,
                badgeContainer = int(EXTRA_BADGE_CONTAINER_LIGHT) ?: return null,
                badgeContent = int(EXTRA_BADGE_CONTENT_LIGHT) ?: return null
            )
            val dark = KkcNavBarColors(
                primary = int(EXTRA_PRIMARY_DARK) ?: return null,
                onSurfaceVariant = int(EXTRA_ON_SURFACE_VARIANT_DARK) ?: return null,
                surfaceVariant = int(EXTRA_SURFACE_VARIANT_DARK) ?: return null,
                frostedBase = int(EXTRA_FROSTED_BASE_DARK) ?: return null,
                frostedContent = int(EXTRA_FROSTED_CONTENT_DARK) ?: return null,
                boldGradient = gradient(EXTRA_BOLD_GRADIENT_DARK) ?: return null,
                badgeContainer = int(EXTRA_BADGE_CONTAINER_DARK) ?: return null,
                badgeContent = int(EXTRA_BADGE_CONTENT_DARK) ?: return null
            )
            KkcNavBarPayload(
                destinations = destinations,
                supplyCount = int(EXTRA_SUPPLY_COUNT) ?: 0,
                safetyCount = int(EXTRA_SAFETY_COUNT) ?: 0,
                light = light,
                dark = dark,
                frostedAlpha = float(EXTRA_FROSTED_ALPHA) ?: return null,
                frostedBlurDp = float(EXTRA_FROSTED_BLUR_DP) ?: return null,
                boldMode = bool(EXTRA_BOLD_MODE) ?: return null,
                indicatorCornerDp = float(EXTRA_INDICATOR_CORNER_DP) ?: return null,
                animDisabled = bool(EXTRA_ANIM_DISABLED) ?: return null,
                blurDisabled = bool(EXTRA_BLUR_DISABLED) ?: return null,
                shadowsDisabled = bool(EXTRA_SHADOWS_DISABLED) ?: return null
            )
        }
    }
}
