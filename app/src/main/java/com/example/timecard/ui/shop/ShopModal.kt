package com.example.timecard.ui.shop

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.data.model.ShopItem
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.JetBrainsMonoFontFamily
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsAnakiwa
import com.example.timecard.ui.theme.LocalTimecardColors
import androidx.compose.ui.graphics.RectangleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopModal(
    onDismiss: () -> Unit,
    shopViewModel: ShopViewModel,
    profileViewModel: ProfileViewModel
) {
    val items = shopViewModel.items
    val userCoins = shopViewModel.userCoins
    val inventory = profileViewModel.profile.inventory
    val isWinston = profileViewModel.employeeName.equals("Winston Ferguson", ignoreCase = true)
    val recipients = shopViewModel.recipients
    val itemImages = shopViewModel.itemImages
    val triedThemes = profileViewModel.profile.triedThemes
    val previewItemId = shopViewModel.previewItemId
    val previewExpiresAtMs = shopViewModel.previewExpiresAtMs
    val pendingClaims = shopViewModel.pendingLimitedClaims
    val claimResult = shopViewModel.limitedClaimResult

    // Reload catalog from disk and mark all special items as seen when modal opens
    LaunchedEffect(Unit) {
        shopViewModel.reloadAndMarkSeen()
    }

    // Auto-dismiss claim result after 4 seconds
    LaunchedEffect(claimResult) {
        if (claimResult != null) {
            kotlinx.coroutines.delay(4000)
            shopViewModel.dismissLimitedClaimResult()
        }
    }

    // Countdown ticker — updates every 500ms while a preview is active
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(previewExpiresAtMs) {
        while (previewExpiresAtMs != null && System.currentTimeMillis() < previewExpiresAtMs) {
            kotlinx.coroutines.delay(500)
            nowMs = System.currentTimeMillis()
        }
        nowMs = System.currentTimeMillis()
    }
    val previewSecondsLeft = (((previewExpiresAtMs ?: 0L) - nowMs) / 1000).coerceAtLeast(0).toInt()

    // Purchase confirmation state
    var pendingPurchase by remember { mutableStateOf<ShopItem?>(null) }

    // Send-note dialog state
    var showSendNoteDialog by remember { mutableStateOf(false) }
    var isAnonymousMode by remember { mutableStateOf(false) }

    // Confirmation dialog
    // Confirmation dialog
    pendingPurchase?.let { pending ->
        val isLimited = pending.quantity != null
        val coinsAfter = userCoins - pending.price
        val colors = LocalTimecardColors.current

        val confirmContent = @Composable {
            Column(
                modifier = Modifier
                    .background(
                        if (colors.isLcars) Color.Black else colors.surface,
                        if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp))
                    )
                    .then(
                        if (colors.isLcars) Modifier.border(2.dp, LcarsOrange, RectangleShape) else Modifier
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (colors.isLcars) "CONFIRM PURCHASE" else "Buy ${pending.title}?",
                    fontSize = 18.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (colors.isLcars) "ITEM: " else "Item: ",
                            style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) else MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (colors.isLcars) pending.title.uppercase() else pending.title,
                            style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) else MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (colors.isLcars) "COST: " else "Cost: ",
                            style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) else MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        CoinAmount(
                            amount = pending.price,
                            fontSize = 14.sp,
                            color = if (colors.isLcars) LcarsOrange else CoinAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (colors.isLcars) "KK AFTER: " else "KK after: ",
                            style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp) else MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        CoinAmount(
                            amount = coinsAfter,
                            fontSize = 14.sp,
                            color = if (coinsAfter >= 0) (if (colors.isLcars) LcarsOrange else CoinAmber) else Color.Red
                        )
                    }
                    if (isLimited) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (colors.isLcars) {
                                "LIMITED ITEM. PENDING ADMIN APPROVAL."
                            } else {
                                "This is a limited item. Your coins won't be taken until an admin approves your purchase."
                            },
                            style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontSize = 12.sp) else MaterialTheme.typography.bodySmall,
                            color = if (colors.isLcars) LcarsOrange else Color(0xFFFBBF24)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { pendingPurchase = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isLcars) LcarsRed else colors.hover,
                            contentColor = if (colors.isLcars) Color.White else colors.textPrimary
                        ),
                        shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (colors.isLcars) "CANCEL" else "Cancel",
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            pendingPurchase = null
                            when {
                                pending.id == "consumable_send_note" -> {
                                    showSendNoteDialog = true
                                    isAnonymousMode = false
                                }
                                pending.id == "consumable_send_anonymous_note" -> {
                                    showSendNoteDialog = true
                                    isAnonymousMode = true
                                }
                                isLimited -> shopViewModel.purchaseLimitedItem(pending.id)
                                else -> {
                                    shopViewModel.purchaseItem(pending.id)
                                    // Auto-apply if this is a theme unlock
                                    val themeKey = com.example.timecard.ui.theme.ACCENT_UNLOCKS
                                        .find { it.third == pending.id }?.first
                                    if (themeKey != null) profileViewModel.setAccentColor(themeKey)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isLcars) LcarsAnakiwa else colors.accent,
                            contentColor = if (colors.isLcars) Color.Black else Color.White
                        ),
                        shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (colors.isLcars) {
                                if (isLimited) "SUBMIT CLAIM" else "CONFIRM"
                            } else {
                                if (isLimited) "Submit Claim" else "Confirm"
                            },
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Dialog(onDismissRequest = { pendingPurchase = null }) {
            confirmContent()
        }
    }

    if (showSendNoteDialog) {
        val sendNoteCost = if (isAnonymousMode) {
            items.find { it.id == "consumable_send_anonymous_note" }?.price ?: 60
        } else {
            items.find { it.id == "consumable_send_note" }?.price ?: 30
        }
        SendNoteDialog(
            recipients = recipients,
            cost = sendNoteCost,
            isAnonymousMode = isAnonymousMode,
            onDismiss = {
                showSendNoteDialog = false
                isAnonymousMode = false
            },
            onSend = { folder, msg ->
                if (isAnonymousMode) {
                    shopViewModel.sendAnonymousNote(folder, msg, sendNoteCost)
                } else {
                    shopViewModel.sendNote(folder, msg, sendNoteCost)
                }
                showSendNoteDialog = false
                isAnonymousMode = false
            }
        )
    }

    val modalContent = @Composable {
        val colors = LocalTimecardColors.current
        Surface(
            modifier = if (colors.isLcars) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                    .safeDrawingPadding()
            },
            shape = if (colors.isLcars) RectangleShape else RoundedCornerShape(12.dp),
            color = if (colors.isLcars) Color.Black else MaterialTheme.colorScheme.surface,
            tonalElevation = if (colors.isLcars) 0.dp else 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                if (colors.isLcars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .background(LcarsOrange).padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EXCHANGE",
                                fontFamily = AntonioFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.5.sp,
                                color = Color.Black
                            )
                            CoinAmount(amount = userCoins, fontSize = 14.sp, iconSize = 16.dp)
                        }
                        Box(
                            modifier = Modifier.size(width = 52.dp, height = 26.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFCC6666))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CLOSE", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color.White)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(LcarsTan))
                } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXCHANGE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        CoinAmount(amount = userCoins, fontSize = 28.sp, iconSize = 32.dp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }

                // Claim result banner
                if (claimResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (claimResult.startsWith("\u2705")) Color(0xFF065F46)
                                else Color(0xFF991B1B)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = claimResult,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                // Separate special/featured items from regular catalog
                val specialItems = items.filter { it.isSpecial }
                val regularItems = items.filter { !it.isSpecial }

                val groupedItems = regularItems.groupBy {
                    val cat = it.category?.lowercase() ?: ""
                    when {
                        cat == "accent" -> "theme"
                        cat.isNotBlank() -> cat
                        it.id.startsWith("accent_") -> "theme"
                        else -> "feature"
                    }
                }

                // Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Featured section (special items first) ──────────────────
                    if (specialItems.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = if (colors.isLcars) "FEATURED" else "🌟 Featured",
                                style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp) else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(specialItems) { item ->
                            val isConsumable = item.id.startsWith("consumable_")
                            val isOwned = if (isConsumable) false else inventory.contains(item.id)
                            val missingCoins = if (userCoins < item.price) item.price - userCoins else 0
                            val rawCat = item.category?.lowercase() ?: ""
                            val isThemeCat = rawCat == "theme" || rawCat == "accent" || (rawCat.isBlank() && item.id.startsWith("accent_"))
                            val accentKeyForItem = if (isThemeCat) ACCENT_UNLOCKS.find { it.third.equals(item.id.trim(), ignoreCase = true) }?.first ?: "" else ""
                            ShopItemCard(
                                item = item,
                                isOwned = isOwned,
                                missingCoins = missingCoins,
                                imageBytes = itemImages[item.id],
                                isTried = triedThemes.contains(item.id),
                                isPreviewActive = previewItemId == item.id,
                                previewSecondsLeft = previewSecondsLeft,
                                isPendingClaim = pendingClaims.containsKey(item.id),
                                isSoldOut = item.quantity != null && item.quantity <= 0,
                                isPoolPreview = !item.inShop && !isWinston,
                                onTryTheme = if (isThemeCat && !isOwned && !triedThemes.contains(item.id) && (item.inShop || isWinston)) {
                                    { shopViewModel.tryTheme(item.id, accentKeyForItem) }
                                } else null,
                                onPurchase = { if (item.inShop || isWinston) pendingPurchase = item }
                            )
                        }
                    }

                    // ── Regular sections ────────────────────────────────────────
                    groupedItems.forEach { (category, categoryItems) ->
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            val categoryName = when (category.lowercase()) {
                                "theme" -> if (colors.isLcars) "THEMES & ACCENTS" else "🎨 Themes & Accents"
                                "feature" -> if (colors.isLcars) "FEATURES & UPGRADES" else "✨ Features & Upgrades"
                                "consumable" -> if (colors.isLcars) "ACTIONS" else "📨 Actions"
                                else -> category.uppercase()
                            }
                            Text(
                                text = categoryName,
                                style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp) else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(categoryItems) { item ->
                            val isConsumable = item.id.startsWith("consumable_")
                            val isOwned = if (isConsumable) false else inventory.contains(item.id)
                            val missingCoins = if (userCoins < item.price) item.price - userCoins else 0
                            val rawCat = item.category?.lowercase() ?: ""
                            val isThemeCat = rawCat == "theme" || rawCat == "accent" || (rawCat.isBlank() && item.id.startsWith("accent_"))
                            val accentKeyForItem = if (isThemeCat) ACCENT_UNLOCKS.find { it.third.equals(item.id.trim(), ignoreCase = true) }?.first ?: "" else ""
                            ShopItemCard(
                                item = item,
                                isOwned = isOwned,
                                missingCoins = missingCoins,
                                imageBytes = itemImages[item.id],
                                isTried = triedThemes.contains(item.id),
                                isPreviewActive = previewItemId == item.id,
                                previewSecondsLeft = previewSecondsLeft,
                                isPendingClaim = pendingClaims.containsKey(item.id),
                                isSoldOut = item.quantity != null && item.quantity <= 0,
                                isPoolPreview = !item.inShop && !isWinston,
                                onTryTheme = if (isThemeCat && !isOwned && !triedThemes.contains(item.id) && (item.inShop || isWinston)) {
                                    { shopViewModel.tryTheme(item.id, accentKeyForItem) }
                                } else null,
                                onPurchase = { if (item.inShop || isWinston) pendingPurchase = item }
                            )
                        }
                    }
                }
            }
        }
    }

    val colors = LocalTimecardColors.current
    if (colors.isLcars) {
        modalContent()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            modalContent()
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    missingCoins: Int,
    imageBytes: ByteArray? = null,
    isTried: Boolean = false,
    isPreviewActive: Boolean = false,
    previewSecondsLeft: Int = 0,
    isPendingClaim: Boolean = false,
    isSoldOut: Boolean = false,
    isPoolPreview: Boolean = false,
    onTryTheme: (() -> Unit)? = null,
    onPurchase: () -> Unit
) {
    val colors = LocalTimecardColors.current
    val isLcars = colors.isLcars
    val canAfford = missingCoins == 0

    val rawCat = item.category?.lowercase() ?: ""
    val isTheme = rawCat == "theme" || rawCat == "accent" || (rawCat.isBlank() && item.id.startsWith("accent_"))

    val themeColor = if (isTheme) {
        ACCENT_UNLOCKS.find { it.third.equals(item.id.trim(), ignoreCase = true) }?.second
    } else null

    val borderColor = themeColor?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val backgroundColor = themeColor?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.surface

    val imageBitmap = remember(imageBytes) {
        imageBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }

    // Shared button state
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && canAfford && !isOwned && !isPendingClaim && !isSoldOut && !isPoolPreview) 0.95f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "button_scale"
    )
    val emerald = Color(0xFF34D399)
    val amber = Color(0xFFF59E0B)
    val slate = Color(0xFF64748B)
    val buttonColor = when {
        isPoolPreview -> Color(0xFF374151)
        isOwned -> emerald
        isSoldOut -> Color(0xFF991B1B)
        isPendingClaim -> amber
        !canAfford -> slate
        else -> emerald
    }
    val buttonText = when {
        isPoolPreview -> if (isLcars) "COMING SOON" else "COMING SOON 🔒"
        isOwned -> if (isLcars) "OWNED" else "OWNED ✅"
        isSoldOut -> "SOLD OUT"
        isPendingClaim -> if (isLcars) "PENDING APPROVAL" else "PENDING APPROVAL ⏳"
        !canAfford -> if (isLcars) "NEED $missingCoins MORE KK" else "NEED ${missingCoins} MORE KK COINS"
        else -> "BUY"
    }
    val buttonTextColor = when {
        isPoolPreview -> Color(0xFF9CA3AF)
        isSoldOut -> Color.White
        isPendingClaim -> Color.Black
        isOwned || canAfford -> Color.Black
        else -> Color.White
    }

    val cardBg = if (isLcars) Color(0xFF111111) else backgroundColor
    val cardShape = if (isLcars) RectangleShape else RoundedCornerShape(8.dp)
    val cardBorder = if (isLcars) Modifier.border(1.dp, themeColor ?: LcarsTan, RectangleShape) else Modifier.border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color.Black)
    ) {
        if (isLcars) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(themeColor ?: LcarsTan)
            )
            Spacer(Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .then(cardBorder)
                .background(cardBg, cardShape)
                .padding(10.dp)
        ) {
            if (imageBitmap != null) {
                // ── Side-by-side layout: text/buttons left, full image right ────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: text + buttons
                    Column(modifier = Modifier.weight(1f)) {
                        CoinAmount(
                            amount = item.price,
                            fontSize = 14.sp,
                            color = if (isLcars) (themeColor ?: LcarsOrange) else CoinAmber
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isLcars) item.title.uppercase() else item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = if (isLcars) AntonioFontFamily else null,
                            fontWeight = FontWeight.Bold,
                            color = if (isLcars) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        if (item.quantity != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isLcars) Color.Black else Color(0xFFB45309).copy(alpha = 0.18f),
                                        shape = if (isLcars) RectangleShape else RoundedCornerShape(4.dp)
                                    )
                                    .then(
                                        if (isLcars) Modifier.border(1.dp, LcarsOrange, RectangleShape) else Modifier
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                val quantityText = if (item.quantity!! <= 0) "SOLD OUT" else "${item.quantity} LEFT"
                                Text(
                                    text = if (isLcars) quantityText else (if (item.quantity!! <= 0) "SOLD OUT" else "⚡ ${item.quantity} left"),
                                    fontSize = 10.sp,
                                    fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.quantity <= 0) Color(0xFFEF4444) else Color(0xFFFBBF24)
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                        Text(
                            text = if (isLcars) item.description.uppercase() else item.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = if (isLcars) AntonioFontFamily else null,
                            color = if (isLcars) LcarsTan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // BUY button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .background(
                                    color = if (isLcars) {
                                        if (!canAfford) Color(0xFF333333) else (themeColor ?: LcarsAnakiwa)
                                    } else buttonColor,
                                    shape = if (isLcars) RoundedCornerShape(50) else RoundedCornerShape(4.dp)
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = canAfford && !isOwned && !isPendingClaim && !isSoldOut,
                                    onClick = onPurchase
                                )
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLcars) buttonText.uppercase() else buttonText,
                                color = if (isLcars) {
                                    if (!canAfford) Color.Gray else Color.Black
                                } else buttonTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily
                            )
                        }
                        // TRY button
                        if (onTryTheme != null || isPreviewActive) {
                            Spacer(modifier = Modifier.height(5.dp))
                            TryThemeButton(
                                isPreviewActive = isPreviewActive,
                                previewSecondsLeft = previewSecondsLeft,
                                onTryTheme = onTryTheme
                            )
                        }
                    }

                    // Right: full image
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(96.dp)
                            .heightIn(min = 80.dp, max = 180.dp)
                            .clip(if (isLcars) RectangleShape else RoundedCornerShape(6.dp))
                            .align(Alignment.CenterVertically)
                    )
                }
            } else {
                // ── Standard layout: icon+price row, then text, then buttons ────
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (isTheme && themeColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(themeColor, if (isLcars) RectangleShape else RoundedCornerShape(4.dp))
                            )
                        } else {
                            if (!isLcars) {
                                Text(text = item.icon, fontSize = 24.sp)
                            } else {
                                // Text abbreviation inside a small box instead of emoji
                                Box(
                                    modifier = Modifier
                                        .background(LcarsTan, RectangleShape)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.id.take(3).uppercase(),
                                        fontFamily = AntonioFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        CoinAmount(
                            amount = item.price,
                            fontSize = 14.sp,
                            color = if (isLcars) (themeColor ?: LcarsOrange) else CoinAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLcars) item.title.uppercase() else item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = if (isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        color = if (isLcars) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (item.quantity != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isLcars) Color.Black else Color(0xFFB45309).copy(alpha = 0.18f),
                                    shape = if (isLcars) RectangleShape else RoundedCornerShape(4.dp)
                                )
                                .then(
                                    if (isLcars) Modifier.border(1.dp, LcarsOrange, RectangleShape) else Modifier
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isLcars) "${item.quantity} LEFT" else "⚡ ${item.quantity} left",
                                fontSize = 10.sp,
                                fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                    Text(
                        text = if (isLcars) item.description.uppercase() else item.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (isLcars) AntonioFontFamily else null,
                        color = if (isLcars) LcarsTan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                        maxLines = 2,
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // BUY button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .background(
                                color = if (isLcars) {
                                    if (!canAfford) Color(0xFF333333) else (themeColor ?: LcarsAnakiwa)
                                } else buttonColor,
                                shape = if (isLcars) RoundedCornerShape(50) else RoundedCornerShape(4.dp)
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = canAfford && !isOwned && !isPendingClaim && !isSoldOut,
                                onClick = onPurchase
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLcars) buttonText.uppercase() else buttonText,
                            color = if (isLcars) {
                                if (!canAfford) Color.Gray else Color.Black
                            } else buttonTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily
                        )
                    }
                    // TRY button
                    if (onTryTheme != null || isPreviewActive) {
                        Spacer(modifier = Modifier.height(5.dp))
                        TryThemeButton(
                            isPreviewActive = isPreviewActive,
                            previewSecondsLeft = previewSecondsLeft,
                            onTryTheme = onTryTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TryThemeButton(
    isPreviewActive: Boolean,
    previewSecondsLeft: Int,
    onTryTheme: (() -> Unit)?
) {
    val colors = LocalTimecardColors.current
    val isLcars = colors.isLcars
    if (isPreviewActive) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isLcars) Color(0xFF222222) else Color(0xFF3F51B5).copy(alpha = 0.18f),
                    shape = if (isLcars) RoundedCornerShape(50) else RoundedCornerShape(4.dp)
                )
                .then(
                    if (isLcars) Modifier.border(1.dp, LcarsAnakiwa, RoundedCornerShape(50)) else Modifier
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLcars) "TRYING... ${previewSecondsLeft}S" else "🎨 Trying… ${previewSecondsLeft}s",
                color = if (isLcars) LcarsAnakiwa else Color(0xFF7986CB),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily
            )
        }
    } else if (onTryTheme != null) {
        val tryInteraction = remember { MutableInteractionSource() }
        val tryPressed by tryInteraction.collectIsPressedAsState()
        val tryScale by animateFloatAsState(
            targetValue = if (tryPressed) 0.95f else 1f,
            animationSpec = tween(durationMillis = 80),
            label = "try_scale"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(tryScale)
                .background(
                    color = if (isLcars) LcarsTan else MaterialTheme.colorScheme.surfaceVariant,
                    shape = if (isLcars) RoundedCornerShape(50) else RoundedCornerShape(4.dp)
                )
                .clickable(
                    interactionSource = tryInteraction,
                    indication = null,
                    onClick = onTryTheme
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLcars) "TRY (30S)" else "🎨 Try (30s)",
                color = if (isLcars) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isLcars) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp,
                fontFamily = if (isLcars) AntonioFontFamily else JetBrainsMonoFontFamily
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNoteDialog(
    recipients: List<EmployeeRecipient>,
    cost: Int,
    isAnonymousMode: Boolean = false,
    onDismiss: () -> Unit,
    onSend: (String, String) -> Unit
) {
    val colors = LocalTimecardColors.current
    var selectedRecipient by remember { mutableStateOf<EmployeeRecipient?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val content = @Composable {
        Column(
            modifier = Modifier
                .background(
                    if (colors.isLcars) Color.Black else colors.surface,
                    if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp))
                )
                .then(
                    if (colors.isLcars) Modifier.border(2.dp, LcarsOrange, RectangleShape) else Modifier
                )
                .padding(24.dp)
        ) {
            Text(
                text = if (colors.isLcars) {
                    if (isAnonymousMode) "SEND ANONYMOUS NOTE" else "SEND NOTE"
                } else {
                    if (isAnonymousMode) "✉️ Send Anonymous Note" else "✉️ Send a Note"
                },
                fontSize = 18.sp,
                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (colors.isLcars) {
                    if (isAnonymousMode) {
                        "SEND AN ANONYMOUS MESSAGE TO A COWORKER. THEY WON'T KNOW WHO SENT IT, BUT THEY CAN REPLY."
                    } else {
                        "PICK A COWORKER AND WRITE THEM A MESSAGE. IT WILL SHOW UP AS AN ALERT WHEN THEY LOG IN."
                    }
                } else {
                    if (isAnonymousMode) {
                        "Send an anonymous message to a coworker. They won't know who sent it, but they can reply."
                    } else {
                        "Pick a coworker and write them a message. It will show up as an alert when they log in."
                    }
                },
                style = if (colors.isLcars) androidx.compose.ui.text.TextStyle(fontFamily = AntonioFontFamily, fontSize = 13.sp) else MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                val resolvedText = selectedRecipient?.let {
                    if (it.displayName != null) {
                        if (colors.isLcars) "${it.displayName.uppercase()} (${it.folderName.uppercase()})" else "${it.displayName} (${it.folderName})"
                    } else {
                        if (colors.isLcars) it.folderName.uppercase() else it.folderName
                    }
                } ?: (if (colors.isLcars) "SELECT RECIPIENT..." else "Select recipient...")

                OutlinedTextField(
                    value = resolvedText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = if (colors.isLcars) AntonioFontFamily else com.example.timecard.ui.theme.OutfitFontFamily,
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                        unfocusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                        focusedIndicatorColor = if (colors.isLcars) LcarsTan else colors.border,
                        unfocusedIndicatorColor = if (colors.isLcars) LcarsTan else colors.border
                    ),
                    shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    recipients.forEach { recipient ->
                        val label = if (recipient.displayName != null) {
                            if (colors.isLcars) "${recipient.displayName.uppercase()} (${recipient.folderName.uppercase()})" else "${recipient.displayName} (${recipient.folderName})"
                        } else {
                            if (colors.isLcars) recipient.folderName.uppercase() else recipient.folderName
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedRecipient = recipient
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 140) message = it },
                label = {
                    Text(
                        text = if (colors.isLcars) "MESSAGE" else "Message",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null
                    )
                },
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = if (colors.isLcars) AntonioFontFamily else com.example.timecard.ui.theme.OutfitFontFamily,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                    unfocusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                    focusedIndicatorColor = if (colors.isLcars) LcarsTan else colors.border,
                    unfocusedIndicatorColor = if (colors.isLcars) LcarsTan else colors.border
                ),
                shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = "${message.length}/140",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        color = colors.textSecondary
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isLcars) LcarsRed else colors.hover,
                        contentColor = if (colors.isLcars) Color.White else colors.textPrimary
                    ),
                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (colors.isLcars) "CANCEL" else "Cancel",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold
                    )
                }
                val canSend = selectedRecipient != null && message.isNotBlank()
                Button(
                    onClick = {
                        selectedRecipient?.let { onSend(it.folderName, message) }
                    },
                    enabled = canSend,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isLcars) LcarsAnakiwa else colors.accent,
                        contentColor = if (colors.isLcars) Color.Black else Color.White
                    ),
                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (colors.isLcars) {
                            if (isAnonymousMode) "SEND ANONYMOUS (${cost} KK)" else "SEND (${cost} KK)"
                        } else {
                            if (isAnonymousMode) "✉️ Send Anonymous (${cost}c)" else "✉️ Send (${cost}c)"
                        },
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        content()
    }
}
