package com.example.timecard.ui.shop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.timecard.data.model.ShopItem
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.kkcnav.KkcNavBarColors
import com.example.timecard.ui.common.FunIcons
import com.example.timecard.ui.profile.themeChoices
import com.example.timecard.ui.theme.DarkTimecardColors
import com.example.timecard.ui.theme.LightTimecardColors
import com.example.timecard.ui.theme.followingKkc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardShopLogicTest {

    private fun item(id: String, price: Int = 100, category: String = "", inShop: Boolean = true, quantity: Int? = null) =
        ShopItem(id = id, title = id, price = price, category = category, inShop = inShop, quantity = quantity)

    @Test
    fun `categories follow the old grouping`() {
        assertEquals(ShopCategory.Themes, shopCategoryOf(item("accent_ocean", category = "theme")))
        assertEquals(ShopCategory.Themes, shopCategoryOf(item("accent_x", category = "accent")))
        assertEquals(ShopCategory.Themes, shopCategoryOf(item("accent_y")))
        assertEquals(ShopCategory.Upgrades, shopCategoryOf(item("feature_display_name")))
        assertEquals(ShopCategory.Actions, shopCategoryOf(item("consumable_send_note", category = "consumable")))
        assertEquals(ShopCategory.Rewards, shopCategoryOf(item("reward_coffee", category = "reward")))
    }

    @Test
    fun `item state priority matches the old card`() {
        val inv = listOf("feature_display_name", "consumable_send_note")
        assertEquals(ShopItemState.ComingSoon, shopItemState(item("reward_x", inShop = false), inv, 999, false, fullCatalog = false))
        // Winston sees the full catalog, so a pool item is buyable for him.
        assertEquals(ShopItemState.CanBuy, shopItemState(item("reward_x", inShop = false), inv, 999, false, fullCatalog = true))
        assertEquals(ShopItemState.Owned, shopItemState(item("feature_display_name"), inv, 0, false, false))
        // Consumables are never "owned", even if a copy sits in the inventory.
        assertEquals(ShopItemState.CanBuy, shopItemState(item("consumable_send_note", price = 20), inv, 66, false, false))
        assertEquals(ShopItemState.SoldOut, shopItemState(item("reward_l", quantity = 0), inv, 999, false, false))
        assertEquals(ShopItemState.PendingApproval, shopItemState(item("reward_l", quantity = 2), inv, 999, true, false))
        assertEquals(ShopItemState.Short(434), shopItemState(item("reward_coffee", price = 500), inv, 66, false, false))
    }

    @Test
    fun `settings theme list starts with Match KKC and only shows owned themes`() {
        val choices = themeChoices(listOf("accent_isle", "accent_retro", "feature_display_name"))
        assertEquals(listOf(null, "teal", "retro"), choices.map { it.key })
        assertEquals("Match KKC", choices.first().name)
        assertNull(choices.first().swatch)
        assertEquals(listOf("Isle", "Retro"), choices.drop(1).map { it.name })
    }

    @Test
    fun `every catalog item and built-in badge has its own fun icon`() {
        val catalog = listOf(
            "feature_custom_avatar", "feature_display_name", "consumable_send_note", "consumable_send_anonymous_note",
            "reward_coffee", "reward_lunch", "reward_early_out"
        )
        val fallbackShop = FunIcons.shopItem("something_new")
        catalog.forEach { assertNotSame("$it uses the fallback", fallbackShop.vector, FunIcons.shopItem(it).vector) }
        val fallbackBadge = FunIcons.badge("custom_admin_badge")
        BadgeEngine.ALL_BADGES.filter { it.trigger.isEmpty() }.forEach {
            assertNotSame("${it.id} uses the fallback", fallbackBadge.vector, FunIcons.badge(it.id).vector)
        }
    }

    @Test
    fun `whole-app KKC follow re-tints the standard palette and keeps light or dark`() {
        val ducks = KkcNavBarColors(
            primary = 0xFF007030.toInt(), onSurfaceVariant = 0xFF444444.toInt(), surfaceVariant = 0xFFE0E0E0.toInt(),
            frostedBase = 0xFFC4CF8E.toInt(), frostedContent = 0xFF000000.toInt(),
            boldGradient = listOf(0xFF0B752F.toInt(), 0xFFFEE11A.toInt()),
            badgeContainer = 0xFFB3261E.toInt(), badgeContent = 0xFFFFFFFF.toInt()
        )
        val light = LightTimecardColors.followingKkc(ducks)
        assertEquals(Color(0xFF007030).toArgb(), light.accent.toArgb())
        assertEquals(Color(0xFF007030).toArgb(), light.tableHeader.toArgb())
        assertFalse(light.isDark)
        assertFalse(light.isLcars || light.isRetro || light.isTerminal)
        assertEquals(Color.White.toArgb(), light.surface.toArgb())
        val dark = DarkTimecardColors.followingKkc(ducks)
        assertTrue(dark.isDark)
        assertEquals(Color(0xFF007030).toArgb(), dark.accent.toArgb())
    }
}
