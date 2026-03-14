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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.JetBrainsMonoFontFamily

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
    val recipients = shopViewModel.recipients
    val itemImages = shopViewModel.itemImages
    val soldCounts = shopViewModel.soldCounts
    val newSpecialItems = shopViewModel.newSpecialItems

    var showSendNoteDialog by remember { mutableStateOf(false) }
    var isAnonymousMode by remember { mutableStateOf(false) }

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                .safeDrawingPadding(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = userCoins.toString(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoinAmber
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Special items float to the top; the rest group by category
                val specialItems = items.filter { it.isSpecial }
                val regularItems = items.filter { !it.isSpecial }
                val groupedRegular = regularItems.groupBy {
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
                    // ── Special items section at the top ──────────────────────
                    if (specialItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "⭐ Special Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(specialItems) { item ->
                            val isConsumable = item.id.startsWith("consumable_")
                            val isOwned = if (isConsumable) false else inventory.contains(item.id)
                            val missingCoins = if (userCoins < item.price) item.price - userCoins else 0
                            val soldCount = soldCounts[item.id] ?: 0
                            val remaining = item.quantity?.let { it - soldCount }
                            val isSoldOut = remaining != null && remaining <= 0
                            ShopItemCard(
                                item = item,
                                isOwned = isOwned,
                                missingCoins = missingCoins,
                                imageBytes = itemImages[item.id],
                                remaining = remaining,
                                isSoldOut = isSoldOut,
                                onPurchase = { shopViewModel.purchaseItem(item.id) }
                            )
                        }
                    }

                    // ── Regular category sections ─────────────────────────────
                    groupedRegular.forEach { (category, categoryItems) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val categoryName = when (category.lowercase()) {
                                "theme" -> "🎨 Themes & Accents"
                                "feature" -> "✨ Features & Upgrades"
                                "consumable" -> "📨 Actions"
                                "reward" -> "🎁 Rewards"
                                else -> category.uppercase()
                            }
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(categoryItems) { item ->
                            val isConsumable = item.id.startsWith("consumable_")
                            val isOwned = if (isConsumable) false else inventory.contains(item.id)
                            val missingCoins = if (userCoins < item.price) item.price - userCoins else 0
                            val soldCount = soldCounts[item.id] ?: 0
                            val remaining = item.quantity?.let { it - soldCount }
                            val isSoldOut = remaining != null && remaining <= 0

                            ShopItemCard(
                                item = item,
                                isOwned = isOwned,
                                missingCoins = missingCoins,
                                imageBytes = itemImages[item.id],
                                remaining = remaining,
                                isSoldOut = isSoldOut,
                                onPurchase = {
                                    when (item.id) {
                                        "consumable_send_note" -> {
                                            showSendNoteDialog = true
                                            isAnonymousMode = false
                                        }
                                        "consumable_send_anonymous_note" -> {
                                            showSendNoteDialog = true
                                            isAnonymousMode = true
                                        }
                                        else -> shopViewModel.purchaseItem(item.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact banner shown on the timecard main screen when new special items exist.
 * Tapping "Open Shop" navigates into the shop; the X marks all items as seen.
 */
@Composable
fun ShopAnnouncementBanner(
    items: List<ShopItem>,
    onOpenShop: () -> Unit,
    onDismiss: () -> Unit
) {
    val amber = Color(0xFFF59E0B)
    val amberBg = Color(0xFF78350F).copy(alpha = 0.20f)
    val titles = items.joinToString(" · ") { it.title }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(amberBg)
            .clickable(onClick = onOpenShop)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = amber, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "NEW IN THE SHOP: ",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = amber
        )
        Text(
            text = titles,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Open Shop →",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = amber
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = amber.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun SpecialItemsBanner(
    items: List<ShopItem>,
    itemImages: Map<String, ByteArray>,
    onDismiss: () -> Unit
) {
    val amber = Color(0xFFF59E0B)
    val amberBg = Color(0xFF78350F).copy(alpha = 0.25f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = amberBg),
        border = BorderStroke(1.dp, amber.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = amber, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "NEW IN THE SHOP",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = amber,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = amber.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    val imgBytes = itemImages[item.id]
                    if (imgBytes != null) {
                        val bitmap = remember(imgBytes) {
                            BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = item.title,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(item.icon, fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        Text(item.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
                    }
                    Text(
                        "🪙 ${item.price}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = CoinAmber
                    )
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    isOwned: Boolean,
    missingCoins: Int,
    imageBytes: ByteArray? = null,
    remaining: Int? = null,
    isSoldOut: Boolean = false,
    onPurchase: () -> Unit
) {
    val canAfford = missingCoins == 0

    val rawCat = item.category?.lowercase() ?: ""
    val isTheme = rawCat == "theme" || rawCat == "accent" || (rawCat.isBlank() && item.id.startsWith("accent_"))

    val themeColor = if (isTheme) {
        com.example.timecard.ui.theme.ACCENT_UNLOCKS.find { it.third.equals(item.id.trim(), ignoreCase = true) }?.second
    } else null

    val borderColor = themeColor?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val backgroundColor = themeColor?.copy(alpha = 0.1f) ?: MaterialTheme.colorScheme.surface

    android.util.Log.d("ShopModal", "Item: ${item.id} | Cat: ${item.category} | isTheme: $isTheme | Color: $themeColor")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Icon / image / color swatch
            if (imageBytes != null) {
                val bitmap = remember(imageBytes) {
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size).asImageBitmap()
                }
                Image(
                    bitmap = bitmap,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (isTheme && themeColor != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(themeColor, RoundedCornerShape(4.dp))
                )
            } else {
                Text(text = item.icon, fontSize = 24.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "🪙 ${item.price}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = CoinAmber,
                    fontSize = 14.sp
                )
                // Stock badge for limited items
                if (remaining != null) {
                    val stockColor = when {
                        isSoldOut -> Color(0xFFEF4444)
                        remaining <= 3 -> Color(0xFFF59E0B)
                        else -> Color(0xFF6EE7B7)
                    }
                    Text(
                        text = if (isSoldOut) "SOLD OUT" else "$remaining left",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = stockColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )

        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            maxLines = 2,
            minLines = 2
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Buy Button
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val clickable = canAfford && !isOwned && !isSoldOut
        val scale by animateFloatAsState(
            targetValue = if (isPressed && clickable) 0.95f else 1f,
            animationSpec = tween(durationMillis = 80),
            label = "button_scale"
        )

        val emerald = Color(0xFF34D399)
        val slate = Color(0xFF64748B)
        val red = Color(0xFFEF4444)

        val buttonColor = when {
            isSoldOut -> red.copy(alpha = 0.7f)
            isOwned -> emerald
            !canAfford -> slate
            else -> emerald
        }
        val buttonText = when {
            isSoldOut -> "SOLD OUT"
            isOwned -> "OWNED \u2705"
            !canAfford -> "NEED ${missingCoins}c MORE"
            else -> "BUY"
        }
        val buttonTextColor = when {
            isSoldOut || (!isOwned && !canAfford) -> Color.White
            else -> Color.Black
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .background(buttonColor, RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = clickable,
                    onClick = onPurchase
                )
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                color = buttonTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = JetBrainsMonoFontFamily
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
    var selectedRecipient by remember { mutableStateOf<EmployeeRecipient?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isAnonymousMode) "\u2709\uFE0F Send Anonymous Note" else "\u2709\uFE0F Send a Note"
            )
        },
        text = {
            Column {
                Text(
                    text = if (isAnonymousMode) {
                        "Send an anonymous message to a coworker. They won't know who sent it, but they can reply."
                    } else {
                        "Pick a coworker and write them a message. It will show up as an alert when they log in."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRecipient?.let {
                            if (it.displayName != null) "${it.displayName} (${it.folderName})" else it.folderName
                        } ?: "Select recipient...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        recipients.forEach { recipient ->
                            val label = if (recipient.displayName != null) {
                                "${recipient.displayName} (${recipient.folderName})"
                            } else {
                                recipient.folderName
                            }
                            DropdownMenuItem(
                                text = { Text(label) },
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
                    label = { Text("Message") },
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("${message.length}/140", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isAnonymousMode) {
                        selectedRecipient?.let { onSend(it.folderName, message) }
                    } else {
                        selectedRecipient?.let { onSend(it.folderName, message) }
                    }
                },
                enabled = (!isAnonymousMode && selectedRecipient != null && message.isNotBlank()) ||
                          (isAnonymousMode && message.isNotBlank())
            ) {
                Text(
                    if (isAnonymousMode) "\uD83E\uDE99 Send Anonymous (${cost}c)" else "\uD83E\uDE99 Send (${cost}c)"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
