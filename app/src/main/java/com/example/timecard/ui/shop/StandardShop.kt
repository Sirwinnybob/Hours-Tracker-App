package com.example.timecard.ui.shop

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.ShopItem
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.common.FunIconTile
import com.example.timecard.ui.common.FunIcons
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.timesheet.standard.Chip
import com.example.timecard.ui.timesheet.standard.HoursIcons
import com.example.timecard.ui.timesheet.standard.PillButton
import com.example.timecard.ui.timesheet.standard.PillKind
import com.example.timecard.ui.timesheet.standard.SectionTitle
import com.example.timecard.ui.timesheet.standard.StandardDialog
import com.example.timecard.ui.timesheet.standard.StandardLook

/** Shop sections, in the order they are shown. */
enum class ShopCategory(val key: String, val title: String, val subtitle: String) {
    Actions("consumable", "Actions", "use right away"),
    Rewards("reward", "Rewards", "show your inventory to claim"),
    Upgrades("feature", "Upgrades", "unlock once"),
    Themes("theme", "Themes", "change how Hours looks")
}

/** Same grouping the old shop used: "accent" and blank accent_* ids are themes, other blanks are upgrades. */
fun shopCategoryOf(item: ShopItem): ShopCategory {
    val cat = item.category.lowercase()
    val key = when {
        cat == "accent" -> "theme"
        cat.isNotBlank() -> cat
        item.id.startsWith("accent_") -> "theme"
        else -> "feature"
    }
    return ShopCategory.entries.firstOrNull { it.key == key } ?: ShopCategory.Upgrades
}

/** What an item card shows, in priority order (same rules as ShopItemCard). */
sealed interface ShopItemState {
    data object ComingSoon : ShopItemState
    data object Owned : ShopItemState
    data object SoldOut : ShopItemState
    data object PendingApproval : ShopItemState
    data class Short(val need: Int) : ShopItemState
    data object CanBuy : ShopItemState
}

fun shopItemState(item: ShopItem, inventory: List<String>, coins: Int, pendingClaim: Boolean, fullCatalog: Boolean): ShopItemState {
    val consumable = item.id.startsWith("consumable_")
    return when {
        !item.inShop && !fullCatalog -> ShopItemState.ComingSoon
        !consumable && inventory.contains(item.id) -> ShopItemState.Owned
        item.quantity != null && item.quantity <= 0 -> ShopItemState.SoldOut
        pendingClaim -> ShopItemState.PendingApproval
        coins < item.price -> ShopItemState.Short(item.price - coins)
        else -> ShopItemState.CanBuy
    }
}

/** Theme preview background per accent key (accent color comes from ACCENT_UNLOCKS). */
private val THEME_PREVIEW_BG = mapOf(
    "orange" to Color(0xFFFFF5EE), "purple" to Color(0xFFF8F5FC), "teal" to Color(0xFFF2FBFC), "gold" to Color(0xFFFDFCFA),
    "red" to Color(0xFFFFFFFF), "sunset" to Color(0xFF1A0D14), "midnight" to Color(0xFF120E21), "ocean" to Color(0xFF0B1924),
    "hacker" to Color(0xFF000000), "lcars" to Color(0xFF000000), "starwars" to Color(0xFF05080F), "retro" to Color(0xFFC0C0C0)
)

/** Shop in the KKC look (standard theme). LCARS and Retro keep ShopModal's own layouts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StandardShopModal(onDismiss: () -> Unit, shopViewModel: ShopViewModel, profileViewModel: ProfileViewModel) {
    val items = shopViewModel.items
    val coins = shopViewModel.userCoins
    val profile = profileViewModel.profile
    val fullCatalog = isFullCatalog(profileViewModel)
    val claimResult = shopViewModel.limitedClaimResult
    var tab by remember { mutableStateOf<ShopCategory?>(null) }
    var pendingPurchase by remember { mutableStateOf<ShopItem?>(null) }
    var sendNote by remember { mutableStateOf<Boolean?>(null) } // anonymous? (null = closed)

    ShopOpenEffects(shopViewModel)
    val previewSecondsLeft = rememberPreviewSecondsLeft(shopViewModel)

    StandardDialog(
        title = "Shop",
        subtitle = "Spend Kustom Kash on themes, rewards and upgrades",
        onDismiss = onDismiss,
        fullHeight = true,
        trailing = { WalletPill(currentLook(), coins) }
    ) { look ->
        CategoryTabs(look, items, tab) { tab = it }
        if (claimResult != null) {
            val (message, ok) = claimResultParts(claimResult)
            Chip(
                message,
                if (ok) look.doneSoft else look.warn.copy(alpha = 0.15f),
                if (ok) look.doneText else look.warn,
                icon = if (ok) HoursIcons.Check else HoursIcons.Close
            )
        }
        val card: @Composable (ShopItem, Modifier) -> Unit = { item, mod ->
            val state = shopItemState(item, profile.inventory, coins, shopViewModel.pendingLimitedClaims.containsKey(item.id), fullCatalog)
            val accentKey = shopThemeKey(item)
            ShopCard(
                look = look,
                item = item,
                state = state,
                imageBytes = shopViewModel.itemImages[item.id],
                themeKey = accentKey,
                isCurrentTheme = accentKey != null && profile.accentColor == accentKey,
                previewActive = shopViewModel.previewItemId == item.id,
                previewSecondsLeft = previewSecondsLeft,
                canTry = canTryTheme(item, accentKey, state, profile.triedThemes, fullCatalog),
                onTry = { if (accentKey != null) shopViewModel.tryTheme(item.id, accentKey) },
                onUse = { if (accentKey != null) profileViewModel.setAccentColor(accentKey) },
                onBuy = { if (item.inShop || fullCatalog) pendingPurchase = item },
                modifier = mod
            )
        }
        val featured = items.filter { it.isSpecial }
        if (featured.isNotEmpty() && tab == null) {
            SectionTitle(look, "Featured", "limited and new")
            ItemGrid(featured, card)
        }
        ShopCategory.entries.filter { tab == null || tab == it }.forEach { cat ->
            val catItems = items.filter { !it.isSpecial && shopCategoryOf(it) == cat }
            if (catItems.isEmpty()) return@forEach
            SectionTitle(look, cat.title, cat.subtitle)
            ItemGrid(catItems, card)
        }
    }

    pendingPurchase?.let { pending ->
        val limited = pending.quantity != null
        StandardDialog(
            title = "Buy ${pending.title}?",
            onDismiss = { pendingPurchase = null },
            maxWidth = 460.dp,
            footer = {
                val look = currentLook()
                PillButton("Cancel", look, { pendingPurchase = null }, Modifier.weight(1f), kind = PillKind.Outlined, height = 48.dp)
                PillButton(if (limited) "Submit claim" else "Buy", look, {
                    pendingPurchase = null
                    completePurchase(pending, shopViewModel, profileViewModel) { sendNote = it }
                }, Modifier.weight(1f), kind = PillKind.Filled, height = 48.dp)
            }
        ) { look ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(look.surface).border(1.dp, look.line, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp)
            ) {
                ConfirmRow(look, "Cost") { CoinAmount(amount = pending.price, fontSize = 16.sp, color = look.text) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
                ConfirmRow(look, "KK after") { CoinAmount(amount = coins - pending.price, fontSize = 16.sp, color = if (coins >= pending.price) look.text else look.warn) }
            }
            if (limited) Text("This is a limited item. Your coins won't be taken until an admin approves your purchase.", color = look.warn, fontSize = 13.sp)
            if (pending.id.startsWith("consumable_send")) Text("Next you pick who gets the note and write the message.", color = look.text2, fontSize = 13.sp)
        }
    }

    sendNote?.let { anonymous -> ShopSendNote(shopViewModel, anonymous) { sendNote = null } }
}

@Composable
private fun currentLook(): StandardLook = com.example.timecard.ui.timesheet.standard.currentStandardLook()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemGrid(items: List<ShopItem>, card: @Composable (ShopItem, Modifier) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
        items.forEach { card(it, Modifier.weight(1f).fillMaxRowHeight()) }
        repeat((3 - items.size % 3) % 3) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun WalletPill(look: StandardLook, coins: Int) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(look.weekBar).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CoinIcon(size = 30.dp)
        Text("$coins", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text("KK", color = look.onSegSelected, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryTabs(look: StandardLook, items: List<ShopItem>, selected: ShopCategory?, onSelect: (ShopCategory?) -> Unit) {
    val tabs = listOf<ShopCategory?>(null) + ShopCategory.entries
    Row(
        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(12.dp)).background(look.primary).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEach { cat ->
            val isSel = cat == selected
            val count = items.count { cat == null || (!it.isSpecial && shopCategoryOf(it) == cat) }
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (isSel) look.segSelected else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(cat) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                val fg = if (isSel) look.onSegSelected else look.onPrimary
                Text(cat?.title ?: "All", color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("$count items", color = fg.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ShopCard(
    look: StandardLook,
    item: ShopItem,
    state: ShopItemState,
    imageBytes: ByteArray?,
    themeKey: String?,
    isCurrentTheme: Boolean,
    previewActive: Boolean,
    previewSecondsLeft: Int,
    canTry: Boolean,
    onTry: () -> Unit,
    onUse: () -> Unit,
    onBuy: () -> Unit,
    modifier: Modifier
) {
    val quiet = state == ShopItemState.Owned || state == ShopItemState.ComingSoon || state == ShopItemState.SoldOut
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (quiet) look.head else look.surface)
            .border(1.dp, look.line, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            ItemArt(look, item, imageBytes, themeKey)
            Box(Modifier.weight(1f))
            CoinAmount(amount = item.price, fontSize = 15.sp, color = if (state is ShopItemState.Short) look.empty else look.progressText)
        }
        Column(Modifier.heightIn(min = 58.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(item.title, color = if (quiet) look.text2 else look.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, color = look.text2, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (item.quantity != null && item.quantity > 0) Text("${item.quantity} left", color = look.warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f, fill = false))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state) {
                ShopItemState.Owned -> {
                    Chip("Owned", look.doneSoft, look.doneText, icon = HoursIcons.Check)
                    if (themeKey != null) {
                        PillButton(if (isCurrentTheme) "In use" else "Use", look, onUse, Modifier.weight(1f), kind = PillKind.Tonal, height = 40.dp, enabled = !isCurrentTheme)
                    }
                }
                ShopItemState.ComingSoon -> Chip("Coming soon", look.head, look.text2, icon = HoursIcons.Lock, outline = look.line)
                ShopItemState.SoldOut -> Chip("Sold out", look.warn.copy(alpha = 0.15f), look.warn)
                ShopItemState.PendingApproval -> Chip("Pending approval", look.progress.copy(alpha = 0.2f), look.progressText)
                is ShopItemState.Short -> {
                    if (canTry || previewActive) TryButton(look, previewActive, previewSecondsLeft, onTry)
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(22.dp)).border(1.5.dp, look.line, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("Need ${state.need} more", color = look.empty, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                }
                ShopItemState.CanBuy -> {
                    if (canTry || previewActive) TryButton(look, previewActive, previewSecondsLeft, onTry)
                    PillButton("Buy", look, onBuy, Modifier.weight(1f), kind = PillKind.Filled, height = 44.dp, fontSize = 15)
                }
            }
        }
    }
}

@Composable
private fun TryButton(look: StandardLook, active: Boolean, secondsLeft: Int, onTry: () -> Unit) {
    PillButton(if (active) "Trying · ${secondsLeft}s" else "Try 30 s", look, onTry, kind = PillKind.Tonal, height = 44.dp, enabled = !active)
}

@Composable
private fun ConfirmRow(look: StandardLook, label: String, value: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = look.text2, fontSize = 15.sp, modifier = Modifier.weight(1f))
        value()
    }
}

/** Uploaded item image if any; themes get a color preview; everything else its fun icon (never the emoji). */
@Composable
private fun ItemArt(look: StandardLook, item: ShopItem, imageBytes: ByteArray?, themeKey: String?) {
    val bitmap = remember(imageBytes) { imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    when {
        bitmap != null -> Image(bitmap, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
        themeKey != null -> {
            val accent = ACCENT_UNLOCKS.find { it.first == themeKey }?.second ?: look.primary
            Column(
                Modifier.width(60.dp).height(46.dp).clip(RoundedCornerShape(10.dp)).background(THEME_PREVIEW_BG[themeKey] ?: Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.Bottom)
            ) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                Box(Modifier.fillMaxWidth(0.6f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.5f)))
            }
        }
        else -> FunIconTile(FunIcons.shopItem(item.id), dark = look.isDark)
    }
}
