package com.example.timecard.ui.shop

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.ShopItem
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.common.FunIcons
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.timesheet.lcars.LcarsClassic
import com.example.timecard.ui.timesheet.lcars.LcarsPill
import com.example.timecard.ui.timesheet.lcars.LcarsScreenHeader
import com.example.timecard.ui.timesheet.lcars.LcarsTextBar
import com.example.timecard.ui.timesheet.lcars.lcarsSlideIn
import com.example.timecard.ui.timesheet.lcars.lcarsText

/** Section bar color per shop category (featured is red). */
internal fun lcarsShopCategoryColor(cat: ShopCategory?): Color = when (cat) {
    null -> LcarsClassic.Orange
    ShopCategory.Actions -> LcarsClassic.Butter
    ShopCategory.Rewards -> LcarsClassic.Violet
    ShopCategory.Upgrades -> LcarsClassic.Bluey
    ShopCategory.Themes -> LcarsClassic.Mauve
}

/** Action pill label for an item (LCARS words, no emoji). */
internal fun lcarsShopActionLabel(state: ShopItemState, isTheme: Boolean, isCurrentTheme: Boolean, limited: Boolean): String = when (state) {
    ShopItemState.Owned -> if (!isTheme) "OWNED" else if (isCurrentTheme) "IN USE" else "USE"
    ShopItemState.ComingSoon -> "COMING SOON"
    ShopItemState.SoldOut -> "SOLD OUT"
    ShopItemState.PendingApproval -> "PENDING"
    is ShopItemState.Short -> "NEED ${state.need}"
    ShopItemState.CanBuy -> if (limited) "CLAIM" else "BUY"
}

/**
 * Shop in the LCARS Classic look (2026-11-19 mockup): header text bar, big KK readout with the original
 * coin, category pills, text-bar sections and one row per item — colored block with the fun icon in
 * black, title, price, action pill. Buying confirms in an LCARS panel over the list. Behavior is shared
 * with the standard shop through ShopActions.kt.
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
internal fun LcarsShopModal(onDismiss: () -> Unit, shopViewModel: ShopViewModel, profileViewModel: ProfileViewModel) {
    val items = shopViewModel.items
    val coins = shopViewModel.userCoins
    val profile = profileViewModel.profile
    val fullCatalog = isFullCatalog(profileViewModel)
    val claimResult = shopViewModel.limitedClaimResult
    var tab by remember { mutableStateOf<ShopCategory?>(null) }
    var pendingPurchase by remember { mutableStateOf<ShopItem?>(null) }
    var sendNote by remember { mutableStateOf<Boolean?>(null) }

    ShopOpenEffects(shopViewModel)
    val previewSecondsLeft = rememberPreviewSecondsLeft(shopViewModel)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LcarsScreenHeader("SHOP", onClose = onDismiss)
            Wallet(coins, Modifier.lcarsSlideIn(80))
            CategoryPills(items, tab, Modifier.lcarsSlideIn(140)) { tab = it }
            if (claimResult != null) {
                val (message, ok) = claimResultParts(claimResult)
                Text(
                    message.uppercase(),
                    style = lcarsText(16.sp, if (ok) LcarsClassic.Creme else LcarsClassic.Red),
                    modifier = Modifier.fillMaxWidth().border(2.dp, if (ok) LcarsClassic.Bluey else LcarsClassic.Red, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                var row = 0
                val section: @Composable (String, Color, List<ShopItem>) -> Unit = { title, color, sectionItems ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LcarsTextBar(title, color, color, Modifier.lcarsSlideIn(180 + row * 40))
                        sectionItems.forEachIndexed { i, item ->
                            key(tab, item.id) {
                                val state = shopItemState(item, profile.inventory, coins, shopViewModel.pendingLimitedClaims.containsKey(item.id), fullCatalog)
                                val themeKey = shopThemeKey(item)
                                ItemRow(
                                    item = item,
                                    state = state,
                                    blockColor = LcarsClassic.RowColors[i % LcarsClassic.RowColors.size],
                                    imageBytes = shopViewModel.itemImages[item.id],
                                    themeKey = themeKey,
                                    isCurrentTheme = themeKey != null && profile.accentColor == themeKey,
                                    previewActive = shopViewModel.previewItemId == item.id,
                                    previewSecondsLeft = previewSecondsLeft,
                                    canTry = canTryTheme(item, themeKey, state, profile.triedThemes, fullCatalog),
                                    onTry = { if (themeKey != null) shopViewModel.tryTheme(item.id, themeKey) },
                                    onAction = {
                                        when {
                                            state == ShopItemState.Owned && themeKey != null -> profileViewModel.setAccentColor(themeKey)
                                            state == ShopItemState.CanBuy && (item.inShop || fullCatalog) -> pendingPurchase = item
                                        }
                                    },
                                    modifier = Modifier.lcarsSlideIn(220 + (row++).coerceAtMost(12) * 45)
                                )
                            }
                        }
                    }
                }
                val featured = items.filter { it.isSpecial }
                if (featured.isNotEmpty() && tab == null) section("FEATURED", LcarsClassic.Red, featured)
                ShopCategory.entries.filter { tab == null || tab == it }.forEach { cat ->
                    val catItems = items.filter { !it.isSpecial && shopCategoryOf(it) == cat }
                    if (catItems.isNotEmpty()) section(cat.title, lcarsShopCategoryColor(cat), catItems)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        AnimatedVisibility(pendingPurchase != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            // Keep showing the item while the panel fades out after pendingPurchase is cleared.
            val shown = remember { pendingPurchase } ?: return@AnimatedVisibility
            Box(Modifier.fillMaxSize()) {
                ConfirmPanel(
                    item = shown,
                    coins = coins,
                    onCancel = { pendingPurchase = null },
                    onConfirm = {
                        pendingPurchase = null
                        completePurchase(shown, shopViewModel, profileViewModel) { sendNote = it }
                    },
                    panelModifier = Modifier.align(Alignment.Center).animateEnterExit(enter = scaleIn(initialScale = 0.92f))
                )
            }
        }
    }

    sendNote?.let { anonymous -> ShopSendNote(shopViewModel, anonymous) { sendNote = null } }
}

@Composable
private fun Wallet(coins: Int, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        CoinIcon(size = 52.dp)
        Column {
            Text("$coins", style = lcarsText(52.sp, LcarsClassic.Orange, FontWeight.Normal).copy(lineHeight = 52.sp))
            Text("KUSTOM KASH", style = lcarsText(15.sp, LcarsClassic.Creme))
        }
    }
}

@Composable
private fun CategoryPills(items: List<ShopItem>, selected: ShopCategory?, modifier: Modifier, onSelect: (ShopCategory?) -> Unit) {
    val tabs = listOf<ShopCategory?>(null) + ShopCategory.entries
    Row(modifier.fillMaxWidth().height(50.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tabs.forEachIndexed { i, cat ->
            val isSel = cat == selected
            val count = items.count { cat == null || (!it.isSpecial && shopCategoryOf(it) == cat) }
            val shape = when (i) {
                0 -> RoundedCornerShape(topStart = 25.dp, bottomStart = 25.dp)
                tabs.lastIndex -> RoundedCornerShape(topEnd = 25.dp, bottomEnd = 25.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(shape)
                    .background(if (isSel) LcarsClassic.Creme else lcarsShopCategoryColor(cat))
                    .clickable(role = Role.Tab, onClickLabel = cat?.title ?: "All") { onSelect(cat) }
            ) {
                Text("$count ITEMS", style = lcarsText(11.sp, Color.Black), modifier = Modifier.align(Alignment.TopStart).padding(start = if (i == 0) 18.dp else 10.dp, top = 5.dp))
                Text((cat?.title ?: "All").uppercase(), style = lcarsText(15.sp, Color.Black), maxLines = 1, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 6.dp))
            }
        }
    }
}

private val ROW_H = 62.dp

@Composable
private fun ItemRow(
    item: ShopItem,
    state: ShopItemState,
    blockColor: Color,
    imageBytes: ByteArray?,
    themeKey: String?,
    isCurrentTheme: Boolean,
    previewActive: Boolean,
    previewSecondsLeft: Int,
    canTry: Boolean,
    onTry: () -> Unit,
    onAction: () -> Unit,
    modifier: Modifier
) {
    val quiet = state == ShopItemState.Owned || state == ShopItemState.ComingSoon || state == ShopItemState.SoldOut
    val affordable = state !is ShopItemState.Short
    Row(modifier.fillMaxWidth().height(ROW_H), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ItemBlock(item, imageBytes, themeKey, blockColor, dimmed = quiet && themeKey == null)
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(item.title.uppercase(), style = lcarsText(21.sp, if (quiet) LcarsClassic.Lilac else LcarsClassic.Creme, FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.quantity != null && item.quantity > 0) Text("${item.quantity} LEFT", style = lcarsText(13.sp, LcarsClassic.Orange))
                Text(item.description.uppercase(), style = lcarsText(13.sp, LcarsClassic.Lilac, FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (canTry || previewActive) {
            LcarsPill(
                if (previewActive) "TRYING ${previewSecondsLeft}S" else "TRY 30S", LcarsClassic.Bluey, onTry,
                Modifier.width(110.dp), height = ROW_H, shape = RoundedCornerShape(0.dp), enabled = !previewActive
            )
        }
        Box(Modifier.widthIn(min = 76.dp).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
            Text("${item.price} KK", style = lcarsText(20.sp, if (affordable) LcarsClassic.Barley else LcarsClassic.Muted, FontWeight.Normal))
        }
        ActionPill(lcarsShopActionLabel(state, themeKey != null, isCurrentTheme, item.quantity != null), state, isCurrentTheme, onAction)
    }
}

@Composable
private fun ActionPill(label: String, state: ShopItemState, isCurrentTheme: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(topEnd = ROW_H / 2, bottomEnd = ROW_H / 2)
    val (bg, fg, outline) = when (state) {
        ShopItemState.CanBuy -> Triple(LcarsClassic.Orange, Color.Black, null)
        ShopItemState.Owned -> if (isCurrentTheme) Triple(LcarsClassic.Creme, Color.Black, null) else Triple(LcarsClassic.Dim, LcarsClassic.Violet, null)
        ShopItemState.PendingApproval -> Triple(LcarsClassic.Dim, LcarsClassic.Butter, null)
        ShopItemState.SoldOut -> Triple(Color.Transparent, LcarsClassic.Red, LcarsClassic.Red)
        else -> Triple(Color.Transparent, LcarsClassic.Muted, LcarsClassic.Dim)
    }
    val clickable = state == ShopItemState.CanBuy || (state == ShopItemState.Owned && label == "USE")
    Box(
        Modifier.width(150.dp).fillMaxHeight().clip(shape).background(bg)
            .then(if (outline != null) Modifier.border(2.dp, outline, shape) else Modifier)
            .clickable(enabled = clickable, role = Role.Button, onClickLabel = label, onClick = onClick)
    ) {
        Text(label, style = lcarsText(15.sp, fg), maxLines = 1, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 7.dp))
    }
}

/** Left block of a row: uploaded image, theme swatch, or the fun icon drawn black on an LCARS color. */
@Composable
private fun ItemBlock(item: ShopItem, imageBytes: ByteArray?, themeKey: String?, color: Color, dimmed: Boolean) {
    val shape = RoundedCornerShape(topStart = ROW_H / 2, bottomStart = ROW_H / 2)
    val bitmap = remember(imageBytes) { imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val fill = when {
        themeKey != null -> ACCENT_UNLOCKS.find { it.first == themeKey }?.second ?: color
        else -> color
    }
    Box(
        Modifier.width(72.dp).fillMaxHeight().clip(shape).background(if (dimmed) fill.copy(alpha = 0.55f) else fill),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(bitmap, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            themeKey == null -> Icon(FunIcons.shopItem(item.id).vector, contentDescription = null, tint = Color.Black, modifier = Modifier.size(34.dp))
        }
    }
}

/** Purchase confirm: an LCARS panel over a dimmed list (CANCEL / CONFIRM pills). */
@Composable
private fun BoxScope.ConfirmPanel(item: ShopItem, coins: Int, onCancel: () -> Unit, onConfirm: () -> Unit, panelModifier: Modifier) {
    val limited = item.quantity != null
    Box(
        Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
    )
    run {
        Column(
            panelModifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .border(2.dp, LcarsClassic.Orange, RoundedCornerShape(topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LcarsTextBar(if (limited) "Submit claim" else "Confirm purchase", LcarsClassic.Orange, LcarsClassic.Orange)
            Text(item.title.uppercase(), style = lcarsText(30.sp, LcarsClassic.Creme, FontWeight.Normal))
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Readout("COST", "${item.price} KK", LcarsClassic.Orange)
                Readout("KK AFTER", "${coins - item.price} KK", if (coins >= item.price) LcarsClassic.Creme else LcarsClassic.Red)
            }
            if (limited) Text("LIMITED ITEM · KK IS TAKEN ONLY AFTER AN ADMIN APPROVES", style = lcarsText(14.sp, LcarsClassic.Butter))
            if (item.id.startsWith("consumable_send")) Text("NEXT: PICK WHO GETS THE NOTE AND WRITE IT", style = lcarsText(14.sp, LcarsClassic.Bluey))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LcarsPill("CANCEL", LcarsClassic.Dim, onCancel, Modifier.weight(1f), textColor = LcarsClassic.Violet)
                LcarsPill(if (limited) "SUBMIT CLAIM" else "CONFIRM", LcarsClassic.Orange, onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Readout(label: String, value: String, color: Color) {
    Column {
        Text(label, style = lcarsText(14.sp, LcarsClassic.Bluey))
        Text(value, style = lcarsText(34.sp, color, FontWeight.Normal))
    }
}
