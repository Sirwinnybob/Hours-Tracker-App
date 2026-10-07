package com.example.timecard.kkcnav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KkcNavBarContractTest {

    /** Exactly what KKC's KkcNavBarPayload.toExtras() produces for its test sample (contract v2). */
    private fun kkcExtras(): MutableMap<String, Any> = mutableMapOf(
        "extra_kkc_navbar_version" to 2,
        "extra_kkc_navbar_destinations" to arrayOf("jobs", "hours", "timecard", "supply", "standards"),
        "extra_kkc_navbar_supply_count" to 3,
        "extra_kkc_navbar_safety_count" to 1,
        "extra_kkc_navbar_frosted_alpha" to 0.72f,
        "extra_kkc_navbar_frosted_blur_dp" to 14f,
        "extra_kkc_navbar_bold_mode" to true,
        "extra_kkc_navbar_indicator_corner_dp" to 9f,
        "extra_kkc_navbar_anim_disabled" to false,
        "extra_kkc_navbar_blur_disabled" to false,
        "extra_kkc_navbar_shadows_disabled" to true,
        "extra_kkc_navbar_primary_light" to 0xFF112233.toInt(),
        "extra_kkc_navbar_primary_dark" to 0xFF998877.toInt(),
        "extra_kkc_navbar_on_surface_variant_light" to 0xFF445566.toInt(),
        "extra_kkc_navbar_on_surface_variant_dark" to 0xFF665544.toInt(),
        "extra_kkc_navbar_surface_variant_light" to 0xFF778899.toInt(),
        "extra_kkc_navbar_surface_variant_dark" to 0xFF332211.toInt(),
        "extra_kkc_navbar_frosted_base_light" to 0xFFAABBCC.toInt(),
        "extra_kkc_navbar_frosted_base_dark" to 0xFF0C0D0E.toInt(),
        "extra_kkc_navbar_frosted_content_light" to 0xFF000000.toInt(),
        "extra_kkc_navbar_frosted_content_dark" to 0xFFFFFFFF.toInt(),
        "extra_kkc_navbar_bold_gradient_light" to intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
        "extra_kkc_navbar_bold_gradient_dark" to intArrayOf(0xFF00FF00.toInt(), 0xFFFFFF00.toInt()),
        "extra_kkc_navbar_badge_container_light" to 0xFFB3261E.toInt(),
        "extra_kkc_navbar_badge_container_dark" to 0xFFF2B8B5.toInt(),
        "extra_kkc_navbar_badge_content_light" to 0xFFFFFFFF.toInt(),
        "extra_kkc_navbar_badge_content_dark" to 0xFF601410.toInt()
    )

    @Test
    fun parsesTheKkcSample() {
        val payload = KkcNavBarPayload.parse { kkcExtras()[it] }
        assertEquals(
            KkcNavBarPayload(
                destinations = listOf("jobs", "hours", "timecard", "supply", "standards"),
                supplyCount = 3,
                safetyCount = 1,
                light = KkcNavBarColors(
                    primary = 0xFF112233.toInt(),
                    onSurfaceVariant = 0xFF445566.toInt(),
                    surfaceVariant = 0xFF778899.toInt(),
                    frostedBase = 0xFFAABBCC.toInt(),
                    frostedContent = 0xFF000000.toInt(),
                    boldGradient = listOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
                    badgeContainer = 0xFFB3261E.toInt(),
                    badgeContent = 0xFFFFFFFF.toInt()
                ),
                dark = KkcNavBarColors(
                    primary = 0xFF998877.toInt(),
                    onSurfaceVariant = 0xFF665544.toInt(),
                    surfaceVariant = 0xFF332211.toInt(),
                    frostedBase = 0xFF0C0D0E.toInt(),
                    frostedContent = 0xFFFFFFFF.toInt(),
                    boldGradient = listOf(0xFF00FF00.toInt(), 0xFFFFFF00.toInt()),
                    badgeContainer = 0xFFF2B8B5.toInt(),
                    badgeContent = 0xFF601410.toInt()
                ),
                frostedAlpha = 0.72f,
                frostedBlurDp = 14f,
                boldMode = true,
                indicatorCornerDp = 9f,
                animDisabled = false,
                blurDisabled = false,
                shadowsDisabled = true
            ),
            payload
        )
    }

    @Test
    fun colorsForPicksTheMatchingSet() {
        val payload = KkcNavBarPayload.parse { kkcExtras()[it] }!!
        assertSame(payload.light, payload.colorsFor(darkTheme = false))
        assertSame(payload.dark, payload.colorsFor(darkTheme = true))
    }

    @Test
    fun keySetMatchesKkc() {
        assertEquals(CANONICAL_KEYS, kkcExtras().keys)
        assertEquals(CANONICAL_KEYS, KkcNavBarContract.ALL_NAVBAR_KEYS)
        assertEquals(27, KkcNavBarContract.ALL_NAVBAR_KEYS.size)
    }

    @Test
    fun versionIsTwo() {
        assertEquals(2, KkcNavBarContract.VERSION)
    }

    @Test
    fun oldKkcWithoutExtrasShowsNoBar() {
        assertNull(KkcNavBarPayload.parse { null })
    }

    @Test
    fun versionOneFromAnOlderKkcShowsNoBar() {
        val extras = kkcExtras().apply { put("extra_kkc_navbar_version", 1) }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun unsupportedFutureVersionShowsNoBar() {
        val extras = kkcExtras().apply { put("extra_kkc_navbar_version", 3) }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun missingRequiredColorShowsNoBar() {
        val extras = kkcExtras().apply { remove("extra_kkc_navbar_primary_light") }
        assertNull(KkcNavBarPayload.parse { extras[it] })
    }

    @Test
    fun missingADarkOnlyColorShowsNoBar() {
        listOf(
            "extra_kkc_navbar_primary_dark",
            "extra_kkc_navbar_frosted_content_dark",
            "extra_kkc_navbar_badge_content_dark",
            "extra_kkc_navbar_bold_gradient_dark"
        ).forEach { key ->
            val extras = kkcExtras().apply { remove(key) }
            assertNull("expected null without $key", KkcNavBarPayload.parse { extras[it] })
        }
    }

    @Test
    fun emptyGradientForEitherThemeShowsNoBar() {
        listOf("extra_kkc_navbar_bold_gradient_light", "extra_kkc_navbar_bold_gradient_dark").forEach { key ->
            val extras = kkcExtras().apply { put(key, intArrayOf()) }
            assertNull("expected null with empty $key", KkcNavBarPayload.parse { extras[it] })
        }
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
            "extra_kkc_navbar_frosted_alpha",
            "extra_kkc_navbar_frosted_blur_dp",
            "extra_kkc_navbar_bold_mode",
            "extra_kkc_navbar_indicator_corner_dp",
            "extra_kkc_navbar_anim_disabled",
            "extra_kkc_navbar_blur_disabled",
            "extra_kkc_navbar_shadows_disabled",
            "extra_kkc_navbar_primary_light",
            "extra_kkc_navbar_primary_dark",
            "extra_kkc_navbar_on_surface_variant_light",
            "extra_kkc_navbar_on_surface_variant_dark",
            "extra_kkc_navbar_surface_variant_light",
            "extra_kkc_navbar_surface_variant_dark",
            "extra_kkc_navbar_frosted_base_light",
            "extra_kkc_navbar_frosted_base_dark",
            "extra_kkc_navbar_frosted_content_light",
            "extra_kkc_navbar_frosted_content_dark",
            "extra_kkc_navbar_bold_gradient_light",
            "extra_kkc_navbar_bold_gradient_dark",
            "extra_kkc_navbar_badge_container_light",
            "extra_kkc_navbar_badge_container_dark",
            "extra_kkc_navbar_badge_content_light",
            "extra_kkc_navbar_badge_content_dark"
        )
    }
}
