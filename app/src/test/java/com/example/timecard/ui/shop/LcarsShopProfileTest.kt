package com.example.timecard.ui.shop

import com.example.timecard.data.model.ShopItem
import com.example.timecard.testutil.SourceFiles
import com.example.timecard.ui.profile.lcarsInitials
import com.example.timecard.ui.profile.lcarsProfileReadouts
import com.example.timecard.ui.timesheet.lcars.LcarsClassic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** LCARS Classic Shop / Profile (2026-11-19 mockup): labels, readouts, shared shop rules and routing. */
class LcarsShopProfileTest {

    @Test
    fun `action pill words follow the item state without emoji`() {
        assertEquals("BUY", lcarsShopActionLabel(ShopItemState.CanBuy, isTheme = false, isCurrentTheme = false, limited = false))
        assertEquals("CLAIM", lcarsShopActionLabel(ShopItemState.CanBuy, isTheme = false, isCurrentTheme = false, limited = true))
        assertEquals("NEED 34", lcarsShopActionLabel(ShopItemState.Short(34), isTheme = false, isCurrentTheme = false, limited = false))
        assertEquals("OWNED", lcarsShopActionLabel(ShopItemState.Owned, isTheme = false, isCurrentTheme = false, limited = false))
        assertEquals("USE", lcarsShopActionLabel(ShopItemState.Owned, isTheme = true, isCurrentTheme = false, limited = false))
        assertEquals("IN USE", lcarsShopActionLabel(ShopItemState.Owned, isTheme = true, isCurrentTheme = true, limited = false))
        assertEquals("COMING SOON", lcarsShopActionLabel(ShopItemState.ComingSoon, false, false, false))
        assertEquals("SOLD OUT", lcarsShopActionLabel(ShopItemState.SoldOut, false, false, false))
        assertEquals("PENDING", lcarsShopActionLabel(ShopItemState.PendingApproval, false, false, true))
    }

    @Test
    fun `category pills use the mockup colors`() {
        assertEquals(LcarsClassic.Orange, lcarsShopCategoryColor(null))
        assertEquals(LcarsClassic.Butter, lcarsShopCategoryColor(ShopCategory.Actions))
        assertEquals(LcarsClassic.Violet, lcarsShopCategoryColor(ShopCategory.Rewards))
        assertEquals(LcarsClassic.Bluey, lcarsShopCategoryColor(ShopCategory.Upgrades))
        assertEquals(LcarsClassic.Mauve, lcarsShopCategoryColor(ShopCategory.Themes))
    }

    @Test
    fun `shared shop rules - theme key, try once, claim text`() {
        val lcars = ShopItem(id = "accent_lcars", category = "theme", price = 1000)
        val coffee = ShopItem(id = "reward_coffee", category = "reward", price = 500)
        assertEquals("lcars", shopThemeKey(lcars))
        assertNull(shopThemeKey(coffee))
        assertTrue(canTryTheme(lcars, "lcars", ShopItemState.Short(10), emptyList(), fullCatalog = false))
        assertFalse(canTryTheme(lcars, "lcars", ShopItemState.Short(10), listOf("accent_lcars"), fullCatalog = false))
        assertFalse(canTryTheme(lcars, "lcars", ShopItemState.Owned, emptyList(), fullCatalog = false))
        assertFalse(canTryTheme(lcars.copy(inShop = false), "lcars", ShopItemState.ComingSoon, emptyList(), fullCatalog = false))
        assertTrue(canTryTheme(lcars.copy(inShop = false), "lcars", ShopItemState.CanBuy, emptyList(), fullCatalog = true))
        assertFalse(canTryTheme(coffee, null, ShopItemState.CanBuy, emptyList(), fullCatalog = false))
        assertEquals("Claim submitted" to true, claimResultParts("✅ Claim submitted"))
        assertEquals("Sold out" to false, claimResultParts("❌ Sold out"))
    }

    @Test
    fun `profile readouts show each figure once`() {
        val r = lcarsProfileReadouts(66, 12, 31, 1, 1, 52.5, 11.0, "2041")
        assertEquals(listOf("KUSTOM KASH", "DAILY STREAK", "WEEKLY STREAK", "BEST WEEK", "BEST DAY", "FAVORITE JOB"), r.map { it.label })
        assertEquals(listOf("66", "12", "1", "52.5", "11.0", "2041"), r.map { it.value })
        assertEquals("BEST 31 DAYS", r[1].sub)
        assertEquals("BEST 1 WEEK", r[2].sub)
        assertEquals("—", lcarsProfileReadouts(0, 0, 0, 0, 0, 0.0, 0.0, "").last().value)
        assertEquals("WF", lcarsInitials("Winston  Ferguson"))
        assertEquals("S", lcarsInitials("shop"))
        assertEquals("?", lcarsInitials("  "))
    }

    @Test
    fun `lcars theme routes shop and settings to the classic layouts`() {
        val shop = SourceFiles.mainSource("com/example/timecard/ui/shop/ShopModal.kt").readText()
        assertTrue(shop.contains("if (LocalTimecardColors.current.isLcars)"))
        assertTrue(shop.contains("LcarsShopModal(onDismiss, shopViewModel, profileViewModel)"))
        val settings = SourceFiles.mainSource("com/example/timecard/ui/profile/SettingsModal.kt").readText()
        assertTrue(settings.contains("LcarsSettingsModal(profileViewModel, actualName, onDismiss, onNavigateToAlerts, onOpenShop)"))
        // Both shop layouts share purchase behavior; both settings layouts share the photo picker.
        listOf("StandardShop.kt", "LcarsShop.kt").forEach {
            val src = SourceFiles.mainSource("com/example/timecard/ui/shop/$it").readText()
            assertTrue(it, src.contains("completePurchase(") && src.contains("ShopOpenEffects(shopViewModel)"))
        }
        listOf("StandardSettings.kt", "LcarsSettings.kt").forEach {
            assertTrue(it, SourceFiles.mainSource("com/example/timecard/ui/profile/$it").readText().contains("rememberAvatarPicker(profileViewModel)"))
        }
        // No emoji in the new LCARS screens.
        listOf("ui/shop/LcarsShop.kt", "ui/profile/LcarsSettings.kt").forEach { path ->
            val src = SourceFiles.mainSource("com/example/timecard/$path").readText()
            assertFalse(path, src.codePoints().anyMatch { it in 0x1F300..0x1FAFF || it in 0x2600..0x27BF })
        }
    }

    @Test
    fun `frame shares the animations-off flag and a one-time entrance gate`() {
        val frame = SourceFiles.mainSource("com/example/timecard/ui/timesheet/lcars/LcarsClassicFrame.kt").readText()
        assertTrue(frame.contains("LocalLcarsEntranceGate provides gate, LocalLcarsAnimate provides animate"))
        val classic = SourceFiles.mainSource("com/example/timecard/ui/timesheet/lcars/LcarsClassic.kt").readText()
        assertTrue(classic.contains("rememberEntrance(LocalLcarsAnimate.current, 0, durationMs = 260)"))
    }
}
