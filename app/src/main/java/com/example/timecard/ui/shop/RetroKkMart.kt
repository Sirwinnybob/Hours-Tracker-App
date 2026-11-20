package com.example.timecard.ui.shop

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.ShopItem
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95MessageBox
import com.example.timecard.ui.retro95.Win95Modal
import com.example.timecard.ui.retro95.Win95StatusBar
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.rememberRetroMotion

/** Store button text for an item state (90s web store words). */
internal fun martButtonLabel(state: ShopItemState, isTheme: Boolean, isCurrentTheme: Boolean, limited: Boolean): String = when (state) {
    ShopItemState.CanBuy -> if (limited) "CLAIM IT!" else "BUY NOW!"
    is ShopItemState.Short -> "NEED ${state.need} MORE"
    ShopItemState.Owned -> if (!isTheme) "OWNED" else if (isCurrentTheme) "IN USE" else "USE IT"
    ShopItemState.ComingSoon -> "COMING SOON"
    ShopItemState.SoldOut -> "SOLD OUT"
    ShopItemState.PendingApproval -> "PENDING"
}

/** Fake page name in the location bar for a category tab. */
internal fun martPage(cat: ShopCategory?): String = (cat?.key ?: "index") + ".html"

private val CAT_COLORS = mapOf<ShopCategory?, Color>(
    null to Color(0xFFC00000), ShopCategory.Actions to Color(0xFFFF8C00), ShopCategory.Rewards to Color(0xFF008000),
    ShopCategory.Upgrades to Color(0xFF000080), ShopCategory.Themes to Color(0xFF800080)
)

/**
 * Retro Shop as "Kustom Kash Mart", a 1997 web store in a browser window (2026-11-20 mockup):
 * spinning NEW! star, balance with the original KK coin, colored category buttons, a product table
 * with BUY NOW! buttons, and a JavaScript-style alert to confirm. Rules are ShopActions.kt's.
 */
@Composable
internal fun RetroKkMart(onDismiss: () -> Unit, shopViewModel: ShopViewModel, profileViewModel: ProfileViewModel) {
    val items = shopViewModel.items
    val coins = shopViewModel.userCoins
    val profile = profileViewModel.profile
    val fullCatalog = isFullCatalog(profileViewModel)
    val motion = rememberRetroMotion()
    var cat by remember { mutableStateOf<ShopCategory?>(null) }
    var pending by remember { mutableStateOf<ShopItem?>(null) }
    var sendNote by remember { mutableStateOf<Boolean?>(null) }
    ShopOpenEffects(shopViewModel)
    val previewSecondsLeft = rememberPreviewSecondsLeft(shopViewModel)
    val spin by rememberInfiniteTransition(label = "star").animateFloat(0f, if (motion) 360f else 0f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "starSpin")
    val blink by rememberInfiniteTransition(label = "new").animateFloat(1f, if (motion) 0f else 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "newBlink")

    Win95Desktop {
        Win95Window("Kustom Kash Mart - KKC Navigator", Modifier.fillMaxSize(), onClose = onDismiss, menus = listOf("File", "Edit", "View", "Go", "Bookmarks", "Help"),
            icon = { Text("🌐", style = w95Text(15.sp)) }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Win95Button("◀ Back", {}, enabled = false)
                Win95Button("↻ Reload", { shopViewModel.reloadAndMarkSeen() })
                Win95Button("⌂ Home", { cat = null })
                Spacer(Modifier.weight(1f))
                Win95Button("Close", onDismiss)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Location:", style = w95Text(14.sp))
                Win95Sunken(Modifier.weight(1f).height(30.dp)) {
                    Text("http://www.kustomkashmart.com/${martPage(cat)}", style = w95Text(14.sp), modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 6.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
                Win95Sunken(Modifier.fillMaxSize(), color = Color(0xFFFFFBE6)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Kustom Kash Mart", style = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = W95.Red))
                            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }.clip(StarShape).background(W95.Yellow))
                                Text("NEW!", style = w95Text(15.sp, W95.Red, FontWeight.Bold), modifier = Modifier.graphicsLayer { alpha = if (blink > 0.5f) 1f else 0f })
                            }
                            Spacer(Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Your balance:", style = w95Text(14.sp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CoinIcon(size = 28.dp)
                                    Text("$coins KK", style = w95Text(28.sp, W95.Navy, FontWeight.Bold))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (listOf<ShopCategory?>(null) + ShopCategory.entries).forEach { c ->
                                val on = c == cat
                                Box(
                                    Modifier.weight(1f).height(40.dp).background(if (on) W95.Yellow else CAT_COLORS[c]!!).border(2.dp, if (on) W95.Black else W95.White)
                                        .clickable(role = Role.Tab, onClickLabel = c?.title ?: "All") { cat = c },
                                    contentAlignment = Alignment.Center
                                ) { Text((c?.title ?: "All").uppercase(), style = w95Text(15.sp, if (on) W95.Black else W95.White, FontWeight.Bold)) }
                            }
                        }
                        shopViewModel.limitedClaimResult?.let { raw ->
                            val (msg, ok) = claimResultParts(raw)
                            Text(msg, style = w95Text(15.sp, if (ok) W95.Green else W95.Red, FontWeight.Bold))
                        }
                        Column(Modifier.fillMaxWidth().background(W95.White).border(2.dp, W95.Shadow)) {
                            Row(Modifier.fillMaxWidth().background(W95.Navy).padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Spacer(Modifier.width(56.dp))
                                Text("Item", style = w95Text(15.sp, W95.White, FontWeight.Bold), modifier = Modifier.weight(1f))
                                Text("Price", style = w95Text(15.sp, W95.White, FontWeight.Bold), modifier = Modifier.width(90.dp))
                                Spacer(Modifier.width(150.dp))
                            }
                            val shown = items.filter { cat == null || (!it.isSpecial && shopCategoryOf(it) == cat) }
                                .sortedByDescending { if (cat == null) it.isSpecial else false }
                            shown.forEachIndexed { i, item ->
                                val state = shopItemState(item, profile.inventory, coins, shopViewModel.pendingLimitedClaims.containsKey(item.id), fullCatalog)
                                val themeKey = shopThemeKey(item)
                                MartRow(
                                    item, state, i, themeKey, shopViewModel.itemImages[item.id],
                                    isCurrentTheme = themeKey != null && profile.accentColor == themeKey,
                                    canTry = canTryTheme(item, themeKey, state, profile.triedThemes, fullCatalog),
                                    previewActive = shopViewModel.previewItemId == item.id, previewSecondsLeft = previewSecondsLeft,
                                    onTry = { if (themeKey != null) shopViewModel.tryTheme(item.id, themeKey) },
                                    onAction = {
                                        when {
                                            state == ShopItemState.Owned && themeKey != null -> profileViewModel.setAccentColor(themeKey)
                                            state == ShopItemState.CanBuy && (item.inShop || fullCatalog) -> pending = item
                                        }
                                    }
                                )
                            }
                            if (shown.isEmpty()) Text("This aisle is empty.", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(10.dp))
                        }
                        Text("🔒 SECURE SITE  |  Shipping info  |  Return policy  |  © 1997 Kustom Kash Mart", style = w95Text(13.sp, Color(0xFF404040)), modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
                val latestPending = com.example.timecard.ui.common.rememberLatestNonNull(pending)
                Win95Modal(pending != null, { pending = null }) {
                    val item = latestPending ?: return@Win95Modal
                    val limited = item.quantity != null
                    Win95MessageBox("[JavaScript Application]", "?", message = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${if (limited) "Claim" else "Buy"} ${item.title} for ${item.price} KK?", style = w95Text(16.sp))
                            Text("Your balance after this order: ${coins - item.price} KK", style = w95Text(15.sp, weight = FontWeight.Bold))
                            if (limited) Text("Limited item: KK is taken only after an admin approves.", style = w95Text(14.sp, W95.Red))
                            if (item.id.startsWith("consumable_send")) Text("Next you pick who gets the note.", style = w95Text(14.sp))
                        }
                    }) {
                        Win95Button("OK", { pending = null; completePurchase(item, shopViewModel, profileViewModel) { sendNote = it } }, Modifier.width(100.dp), bold = true)
                        Win95Button("Cancel", { pending = null }, Modifier.width(100.dp))
                    }
                }
            }
            Win95StatusBar(listOf("Document: Done" to null, "🔒" to 44.dp))
        }
    }
    sendNote?.let { anonymous -> ShopSendNote(shopViewModel, anonymous) { sendNote = null } }
}

private val StarShape = GenericShape { size, _ ->
    val cx = size.width / 2; val cy = size.height / 2
    val outer = size.minDimension / 2; val inner = outer * 0.45f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + (r * Math.cos(a)).toFloat(); val y = cy + (r * Math.sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun MartRow(
    item: ShopItem,
    state: ShopItemState,
    index: Int,
    themeKey: String?,
    imageBytes: ByteArray?,
    isCurrentTheme: Boolean,
    canTry: Boolean,
    previewActive: Boolean,
    previewSecondsLeft: Int,
    onTry: () -> Unit,
    onAction: () -> Unit
) {
    val bitmap = remember(imageBytes) { imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val label = martButtonLabel(state, themeKey != null, isCurrentTheme, item.quantity != null)
    val actionable = state == ShopItemState.CanBuy || (state == ShopItemState.Owned && label == "USE IT")
    Row(
        Modifier.fillMaxWidth().background(if (index % 2 == 1) Color(0xFFF0F0FF) else W95.White).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).padding(end = 8.dp), contentAlignment = Alignment.Center) {
            when {
                bitmap != null -> Image(bitmap, item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                themeKey != null -> Box(Modifier.size(34.dp).background(ACCENT_UNLOCKS.find { it.first == themeKey }?.second ?: W95.Navy).border(1.dp, W95.Black))
                else -> Text(item.icon, style = w95Text(26.sp))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = w95Text(16.sp, W95.Link, FontWeight.Bold).copy(textDecoration = TextDecoration.Underline))
                if (item.isSpecial) Text("HOT!", style = w95Text(12.sp, W95.Red, FontWeight.Bold))
            }
            Text(item.description, style = w95Text(13.sp, Color(0xFF404040)), maxLines = 2)
            if (item.quantity != null && item.quantity > 0) Text("Only ${item.quantity} left!", style = w95Text(13.sp, W95.Red, FontWeight.Bold))
            if (canTry || previewActive) {
                Text(
                    if (previewActive) "Trying it on... ${previewSecondsLeft}s" else "[Try it on for 30 seconds]",
                    style = w95Text(13.sp, W95.Link).copy(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.clickable(enabled = !previewActive, role = Role.Button, onClick = onTry).padding(vertical = 4.dp)
                )
            }
        }
        Text("${item.price} KK", style = w95Text(15.sp, W95.Red, FontWeight.Bold), modifier = Modifier.width(90.dp))
        val (bg, fg) = when {
            state == ShopItemState.CanBuy -> W95.Yellow to W95.Red
            label == "USE IT" -> W95.Yellow to W95.Navy
            state == ShopItemState.Owned -> W95.Face to Color(0xFF404040)
            else -> Color(0xFFE0E0E0) to W95.Shadow
        }
        Box(
            Modifier.widthIn(min = 150.dp).height(40.dp).background(bg).border(2.dp, if (actionable) W95.Black else W95.Shadow)
                .clickable(enabled = actionable, role = Role.Button, onClickLabel = label, onClick = onAction),
            contentAlignment = Alignment.Center
        ) { Text(label, style = w95Text(14.sp, fg, FontWeight.Bold)) }
    }
}
