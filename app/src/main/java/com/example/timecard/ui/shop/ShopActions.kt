package com.example.timecard.ui.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.timecard.data.model.ShopItem
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.theme.ACCENT_UNLOCKS

/**
 * Shop behavior shared by every themed shop layout (standard StandardShop.kt, LCARS LcarsShop.kt):
 * what a theme item maps to, what confirming a purchase does, the try-a-theme countdown, and the
 * send-note follow-up. Layouts only decide how things look.
 */

/** Accent key a theme item applies (profile.accentColor), or null when the item is not a theme. */
internal fun shopThemeKey(item: ShopItem): String? =
    if (shopCategoryOf(item) == ShopCategory.Themes) ACCENT_UNLOCKS.find { it.third.equals(item.id.trim(), true) }?.first else null

/** A theme can be tried for 30 s once, when it is not owned and is actually for sale. */
internal fun canTryTheme(item: ShopItem, themeKey: String?, state: ShopItemState, triedThemes: List<String>, fullCatalog: Boolean): Boolean =
    themeKey != null && state != ShopItemState.Owned && !triedThemes.contains(item.id) && (item.inShop || fullCatalog)

/** Winston's PIN sees the full, unfiltered catalog (as before). */
internal fun isFullCatalog(profileViewModel: ProfileViewModel): Boolean = profileViewModel.employeeName == "389"

/**
 * Confirmed purchase: notes go on to the send-note dialog ([onSendNote] with anonymous), limited items
 * become a claim for admin approval, everything else is bought (a bought theme is applied right away).
 */
internal fun completePurchase(item: ShopItem, shopViewModel: ShopViewModel, profileViewModel: ProfileViewModel, onSendNote: (anonymous: Boolean) -> Unit) {
    when {
        item.id == "consumable_send_note" -> onSendNote(false)
        item.id == "consumable_send_anonymous_note" -> onSendNote(true)
        item.quantity != null -> shopViewModel.purchaseLimitedItem(item.id)
        else -> {
            shopViewModel.purchaseItem(item.id)
            ACCENT_UNLOCKS.find { it.third == item.id }?.first?.let { profileViewModel.setAccentColor(it) }
        }
    }
}

/** Reloads the catalog (marking specials seen) when the shop opens and hides a claim result after 4 s. */
@Composable
internal fun ShopOpenEffects(shopViewModel: ShopViewModel) {
    val claimResult = shopViewModel.limitedClaimResult
    LaunchedEffect(Unit) { shopViewModel.reloadAndMarkSeen() }
    LaunchedEffect(claimResult) {
        if (claimResult != null) {
            kotlinx.coroutines.delay(4000)
            shopViewModel.dismissLimitedClaimResult()
        }
    }
}

/** Seconds left on the running 30 s theme preview (ticks every 500 ms while one runs). */
@Composable
internal fun rememberPreviewSecondsLeft(shopViewModel: ShopViewModel): Int {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val expiresAt = shopViewModel.previewExpiresAtMs
    LaunchedEffect(expiresAt) {
        while (expiresAt != null && System.currentTimeMillis() < expiresAt) {
            kotlinx.coroutines.delay(500)
            nowMs = System.currentTimeMillis()
        }
        nowMs = System.currentTimeMillis()
    }
    return (((expiresAt ?: 0L) - nowMs) / 1000).coerceAtLeast(0).toInt()
}

/** Claim result text without its leading emoji/symbols; ok = the claim went through. */
internal fun claimResultParts(raw: String): Pair<String, Boolean> =
    raw.replace(Regex("^[^\\p{L}\\p{N}]+"), "") to raw.trimStart().startsWith("✅")

/** Pick-a-recipient-and-write dialog after buying a note; sends and charges [ShopViewModel]. */
@Composable
internal fun ShopSendNote(shopViewModel: ShopViewModel, anonymous: Boolean, onClose: () -> Unit) {
    val id = if (anonymous) "consumable_send_anonymous_note" else "consumable_send_note"
    val cost = shopViewModel.items.find { it.id == id }?.price ?: if (anonymous) 60 else 30
    SendNoteDialog(
        recipients = shopViewModel.recipients,
        cost = cost,
        isAnonymousMode = anonymous,
        onDismiss = onClose,
        onSend = { folder, msg ->
            if (anonymous) shopViewModel.sendAnonymousNote(folder, msg, cost) else shopViewModel.sendNote(folder, msg, cost)
            onClose()
        }
    )
}
