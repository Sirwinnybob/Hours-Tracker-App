package com.example.timecard.kkcnav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KkcNavBarContractTest {

    /** Exactly what KKC's KkcNavBarPayload.toExtras() produces for its test sample. */
    private fun kkcExtras(): MutableMap<String, Any> = mutableMapOf(
        "extra_kkc_navbar_version" to 1,
        "extra_kkc_navbar_destinations" to arrayOf("jobs", "hours", "timecard", "supply", "standards"),
        "extra_kkc_navbar_supply_count" to 3,
        "extra_kkc_navbar_safety_count" to 1,
        "extra_kkc_navbar_dark" to true,
        "extra_kkc_navbar_primary" to 0xFF112233.toInt(),
        "extra_kkc_navbar_on_surface_variant" to 0xFF445566.toInt(),
        "extra_kkc_navbar_surface_variant" to 0xFF778899.toInt(),
        "extra_kkc_navbar_frosted_base" to 0xFFAABBCC.toInt(),
        "extra_kkc_navbar_frosted_content" to 0xFF000000.toInt(),
        "extra_kkc_navbar_frosted_alpha" to 0.72f,
        "extra_kkc_navbar_frosted_blur_dp" to 14f,
        "extra_kkc_navbar_bold_mode" to true,
        "extra_kkc_navbar_bold_gradient" to intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
        "extra_kkc_navbar_badge_container" to 0xFFB3261E.toInt(),
        "extra_kkc_navbar_badge_content" to 0xFFFFFFFF.toInt(),
        "extra_kkc_navbar_indicator_corner_dp" to 9f,
        "extra_kkc_navbar_anim_disabled" to false,
        "extra_kkc_navbar_blur_disabled" to false,
        "extra_kkc_navbar_shadows_disabled" to true
    )

    @Test
    fun parsesTheKkcSample() {
        val payload = KkcNavBarPayload.parse { kkcExtras()[it] }
        assertEquals(
            KkcNavBarPayload(
                destinations = listOf("jobs", "hours", "timecard", "supply", "standards"),
                supplyCount = 3,
                safetyCount = 1,
                dark = true,
                primary = 0xFF112233.toInt(),
                onSurfaceVariant = 0xFF445566.toInt(),
                surfaceVariant = 0xFF778899.toInt(),
                frostedBase = 0xFFAABBCC.toInt(),
                frostedContent = 0xFF000000.toInt(),
                frostedAlpha = 0.72f,
                frostedBlurDp = 14f,
                boldMode = true,
                boldGradient = listOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
                badgeContainer = 0xFFB3261E.toInt(),
                badgeContent = 0xFFFFFFFF.toInt(),
                indicatorCornerDp = 9f,
                animDisabled = false,
                blurDisabled = false,
                shadowsDisabled = true
            ),
            payload
        )
    }

    @Test
    fun keySetMatchesKkc() {
        assertEquals(CANONICAL_KEYS, kkcExtras().keys)
        assertEquals(CANONICAL_KEYS, KkcNavBarContract.ALL_NAVBAR_KEYS)
    }

    @Test
    fun oldKkcWithoutExtrasShowsNoBar() {
        assertNull(KkcNavBarPayload.parse { null })
    }

    @Test
    fun unsupportedVersionShowsNoBar() {
        val extras = kkcExtras().apply { put("extra_kkc_navbar_version", 2) }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun missingRequiredColorShowsNoBar() {
        val extras = kkcExtras().apply { remove("extra_kkc_navbar_primary") }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun emptyDestinationsShowsNoBar() {
        val extras = kkcExtras().apply { put("extra_kkc_navbar_destinations", emptyArray<String>()) }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun missingBadgeCountsDefaultToZero() {
        val extras = kkcExtras().apply {
            remove("extra_kkc_navbar_supply_count")
            remove("extra_kkc_navbar_safety_count")
        }
        val payload = KkcNavBarPayload.parse { extras[it] }!!
        assertEquals(0, payload.supplyCount)
        assertEquals(0, payload.safetyCount)
    }

    companion object {
        // KEEP IN SYNC with KKCSheetTracker
        // app/src/test/java/com/kkc/sheettracker/navigation/KkcNavBarContractTest.kt CANONICAL_KEYS.
        val CANONICAL_KEYS = setOf(
            "extra_kkc_navbar_version",
            "extra_kkc_navbar_destinations",
            "extra_kkc_navbar_supply_count",
            "extra_kkc_navbar_safety_count",
            "extra_kkc_navbar_dark",
            "extra_kkc_navbar_primary",
            "extra_kkc_navbar_on_surface_variant",
            "extra_kkc_navbar_surface_variant",
            "extra_kkc_navbar_frosted_base",
            "extra_kkc_navbar_frosted_content",
            "extra_kkc_navbar_frosted_alpha",
            "extra_kkc_navbar_frosted_blur_dp",
            "extra_kkc_navbar_bold_mode",
            "extra_kkc_navbar_bold_gradient",
            "extra_kkc_navbar_badge_container",
            "extra_kkc_navbar_badge_content",
            "extra_kkc_navbar_indicator_corner_dp",
            "extra_kkc_navbar_anim_disabled",
            "extra_kkc_navbar_blur_disabled",
            "extra_kkc_navbar_shadows_disabled"
        )
    }
}
