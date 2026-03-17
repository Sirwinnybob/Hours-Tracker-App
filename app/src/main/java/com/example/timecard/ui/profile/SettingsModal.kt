package com.example.timecard.ui.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.components.InitialsAvatar
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun SettingsModal(
    profileViewModel: ProfileViewModel,
    actualName: String,
    onDismiss: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenShop: () -> Unit
) {
    val colors = LocalTimecardColors.current
    val profile = profileViewModel.profile
    var displayNameInput by remember(profile.displayName) {
        mutableStateOf(profile.displayName ?: "")
    }
    var selectedBadge by remember { mutableStateOf<BadgeDefinition?>(null) }

    // Only show badges the employee has actually earned
    val earnedBadges = BadgeEngine.ALL_BADGES.filter { def ->
        (profile.badges[def.id] ?: 0) > 0
    }

    // Badge detail popup
    selectedBadge?.let { badge ->
        Dialog(onDismissRequest = { selectedBadge = null }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(colors.surface, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp)))
                    .padding(24.dp)
            ) {
                val detailImgBytes = profileViewModel.badgeImages[badge.id]
                if (detailImgBytes != null) {
                    Image(
                        bitmap = remember(detailImgBytes) { BitmapFactory.decodeByteArray(detailImgBytes, 0, detailImgBytes.size).asImageBitmap() },
                        contentDescription = badge.name,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    Text(badge.emoji, fontSize = 44.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(badge.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(badge.description, fontSize = 13.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text("\"${badge.flavorText}\"", fontSize = 12.sp, color = colors.accent, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("+${badge.coinReward} Coins${if (badge.repeatable) " · Repeatable" else ""}", fontSize = 11.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { selectedBadge = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (profileViewModel.isLockedByAnotherUser) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("🔒", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Profile Locked",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "This profile is currently being accessed by another tablet or was opened within the last 5 minutes.\n\n" +
                            "If this lock persists try:\n" +
                            "1. Waiting 5 minutes\n" +
                            "2. Syncing your tablet.\n" +
                            "3. Restarting then syncing your tablet.\n\n" +
                            "If this lock persists tell Winston and he will get it unlocked for you.",
                            fontSize = 16.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profile.avatar == "custom" && profileViewModel.avatarImage != null) {
                            val imgBytes = profileViewModel.avatarImage!!
                            Image(
                                bitmap = remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap() },
                                contentDescription = "Avatar",
                                modifier = Modifier.size(36.dp).clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            InitialsAvatar(
                                name = profile.displayName ?: actualName,
                                size = 36.dp,
                                fontSize = 13.sp,
                                bgColor = colors.accent
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                "⚙️ Profile & Settings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(actualName, fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }

                // Backfill indicator
                if (profileViewModel.isBackfilling) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.accent.copy(alpha = 0.12f), com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Loading your history… Coins are being calculated",
                            fontSize = 12.sp,
                            color = colors.accent
                        )
                    }
                }

                // --- Profile Photo ---
                val context = LocalContext.current
                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                            val original = BitmapFactory.decodeStream(stream)
                            if (original != null) {
                                // Downscale to max 512px to keep file size reasonable
                                val max = 512
                                val scale = minOf(max.toFloat() / original.width, max.toFloat() / original.height, 1f)
                                val scaled = if (scale < 1f) {
                                    Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
                                } else original
                                val out = java.io.ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                out.toByteArray()
                            } else null
                        }
                        if (bytes != null) profileViewModel.saveCustomAvatar(bytes)
                    }
                }

                Text("📷 Profile Photo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))

                if (profile.inventory.contains("feature_custom_avatar")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current avatar preview
                        if (profile.avatar == "custom" && profileViewModel.avatarImage != null) {
                            val imgBytes = profileViewModel.avatarImage!!
                            Image(
                                bitmap = remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap() },
                                contentDescription = "Current avatar",
                                modifier = Modifier.size(56.dp).clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                            )
                        } else {
                            InitialsAvatar(
                                name = profile.displayName ?: actualName,
                                size = 56.dp,
                                fontSize = 20.sp,
                                bgColor = colors.accent
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(if (profile.avatar == "custom") "Change Photo" else "Upload Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (profile.avatar == "custom") {
                                Button(
                                    onClick = { profileViewModel.clearCustomAvatar() },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Remove Photo", fontSize = 13.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.hover, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        InitialsAvatar(
                            name = profile.displayName ?: actualName,
                            size = 40.dp,
                            fontSize = 15.sp,
                            bgColor = colors.accent.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Initials Avatar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text("🔒 Unlock custom photo in Shop", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Coin Balance Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.hover, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "🪙 ${profile.coins}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = com.example.timecard.ui.theme.JetBrainsMonoFontFamily,
                                color = com.example.timecard.ui.theme.CoinAmber
                            )
                            Text(
                                "Total Coins",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Streaks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dailyStreak = profile.streaks.currentDaily
                    StatChip(
                        label = "Daily Streak",
                        value = "🔥 $dailyStreak",
                        sub = "Best: ${profile.streaks.bestDaily}",
                        modifier = Modifier.weight(1f),
                        colors = colors,
                        pulse = dailyStreak >= 7 && dailyStreak % 7 == 0
                    )
                    StatChip(
                        label = "Weekly Streak",
                        value = "📆 ${profile.streaks.currentWeekly}",
                        sub = "Best: ${profile.streaks.bestWeekly}",
                        modifier = Modifier.weight(1f),
                        colors = colors
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Personal Records
                Text("🏅 Personal Records", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatChip("Best Week", "${String.format("%.2f", profile.records.bestWeekHours)} hrs", "", Modifier.weight(1f), colors)
                    StatChip("Best Day", "${String.format("%.2f", profile.records.busiestDay)} hrs", "", Modifier.weight(1f), colors)
                }
                Spacer(Modifier.height(8.dp))
                if (profile.records.favoriteJob.isNotBlank()) {
                    Text(
                        "Favorite job: ${profile.records.favoriteJob}",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Badges — only earned ones shown
                val totalEarned = profile.badges.values.sum()
                Text(
                    "🏆 Badges${if (earnedBadges.isNotEmpty()) " ($totalEarned earned)" else ""}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                if (earnedBadges.isEmpty()) {
                    Text(
                        "Save your first timecard to start earning badges!",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((((earnedBadges.size + 3) / 4) * 78).dp.coerceAtLeast(78.dp)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(earnedBadges) { def ->
                            val count = profile.badges[def.id] ?: 1
                            Box {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(colors.accent.copy(alpha = 0.12f), com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)))
                                        .clickable { selectedBadge = def }
                                        .padding(6.dp)
                                ) {
                                    val imgBytes = profileViewModel.badgeImages[def.id]
                                    if (imgBytes != null) {
                                        Image(
                                            bitmap = remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap() },
                                            contentDescription = def.name,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Text(def.emoji, fontSize = 22.sp)
                                    }
                                    Text(
                                        def.name,
                                        fontSize = 9.sp,
                                        color = colors.accent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2
                                    )
                                }
                                // Count badge for repeatable badges earned more than once
                                if (def.repeatable && count > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .background(Color(0xFFD4AF37), com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            "×$count",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Display Name
                Text("Display Name", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(
                    "Shown in the app instead of your actual name. Your login ID never changes.",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(8.dp))
                if (profile.inventory.contains("feature_display_name")) {
                    TextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        placeholder = { Text(actualName, color = colors.textSecondary, fontFamily = com.example.timecard.ui.theme.OutfitFontFamily) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = com.example.timecard.ui.theme.OutfitFontFamily,
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.input,
                            unfocusedContainerColor = colors.input,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { profileViewModel.setDisplayName(displayNameInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Display Name", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.hover, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("🔒", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Locked", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Text("🔒 Unlock custom name in Shop", fontSize = 11.sp, color = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Accent Colors / Immersive Themes
                val unlockedCount = ACCENT_UNLOCKS.count { (_, _, itemId) -> profile.inventory.contains(itemId) }
                Text(
                    "🎨 Immersive Theme${if (unlockedCount == 0) " (buy colors in Shop)" else ""}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Default (no accent)
                    val defaultSelected = profile.accentColor == null
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                            .background(com.example.timecard.ui.theme.AccentBlue)
                            .then(
                                if (defaultSelected) Modifier.border(3.dp, Color.White, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                else Modifier
                            )
                            .clickable { profileViewModel.setAccentColor(null) }
                    )
                    ACCENT_UNLOCKS.forEach { (key, color, itemId) ->
                        val unlocked = profile.inventory.contains(itemId)
                        val selected = profile.accentColor == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                .background(if (unlocked) color else color.copy(alpha = 0.25f))
                                .then(
                                    if (selected) Modifier.border(3.dp, Color.White, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                                    else Modifier
                                )
                                .then(
                                    if (unlocked) Modifier.clickable { profileViewModel.setAccentColor(key) }
                                    else Modifier
                                )
                        ) {
                            if (!unlocked) {
                                Text("🔒", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Buy new colors in the Shop. Tap to apply.",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(24.dp))
                
                // Add Open Shop Button
                Button(
                    onClick = { 
                        onDismiss()
                        onOpenShop()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.timecard.ui.theme.CoinAmber.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.timecard.ui.theme.CoinAmber),
                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("🛒 Visit Shop", color = com.example.timecard.ui.theme.CoinAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { 
                        onDismiss()
                        onNavigateToAlerts()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("📋 View Past Alerts", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier,
    colors: com.example.timecard.ui.theme.TimecardColors,
    pulse: Boolean = false
) {
    val scale = if (pulse) {
        val transition = rememberInfiniteTransition(label)
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse"
        ).value
    } else 1f

    Column(
        modifier = modifier
            .background(colors.hover, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            fontFamily = com.example.timecard.ui.theme.JetBrainsMonoFontFamily,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        if (sub.isNotBlank()) Text(sub, fontSize = 10.sp, color = colors.textSecondary)
    }
}
