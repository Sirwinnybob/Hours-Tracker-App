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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.components.InitialsAvatar
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsAnakiwa
import com.example.timecard.ui.theme.LocalTimecardColors
import androidx.compose.ui.graphics.RectangleShape

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
    // Badge detail popup
    selectedBadge?.let { badge ->
        val dialogContent = @Composable {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                val detailImgBytes = profileViewModel.badgeImages[badge.id]
                val detailBadgeBitmap = detailImgBytes?.let { bytes ->
                    remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                }
                if (detailBadgeBitmap != null) {
                    Image(
                        bitmap = detailBadgeBitmap,
                        contentDescription = badge.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(64.dp).clip(
                            if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp))
                        )
                    )
                } else {
                    if (colors.isLcars) {
                        Text(
                            text = badge.name.uppercase(),
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = LcarsOrange
                        )
                    } else {
                        Text(badge.emoji, fontSize = 44.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (colors.isLcars) badge.name.uppercase() else badge.name,
                    fontSize = 18.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (colors.isLcars) badge.description.uppercase() else badge.description,
                    fontSize = 13.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (colors.isLcars) "\"${badge.flavorText.uppercase()}\"" else "\"${badge.flavorText}\"",
                    fontSize = 12.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    color = colors.accent,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (colors.isLcars) {
                        "+${badge.coinReward} KK${if (badge.repeatable) " · REPEATABLE" else ""}"
                    } else {
                        "+${badge.coinReward} Kustom Kash${if (badge.repeatable) " · Repeatable" else ""}"
                    },
                    fontSize = 11.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    color = Color(0xFFD4AF37),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { selectedBadge = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isLcars) LcarsRed else colors.hover,
                        contentColor = if (colors.isLcars) Color.White else colors.textPrimary
                    ),
                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (colors.isLcars) "CLOSE" else "Close",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Dialog(onDismissRequest = { selectedBadge = null }) {
            dialogContent()
        }
    }

    val modalContent = @Composable {
        Box(
            modifier = if (colors.isLcars) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp)
                    .safeDrawingPadding()
            },
            contentAlignment = if (colors.isLcars) Alignment.TopCenter else Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (colors.isLcars) Color.Black else colors.surface,
                        if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp))
                    )
            ) {
                if (colors.isLcars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(LcarsOrange).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PROFILE & SETTINGS",
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.size(width = 52.dp, height = 26.dp)
                                .clip(RoundedCornerShape(50))
                                .background(LcarsRed)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CLOSE", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (colors.isLcars) 16.dp else 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                if (profileViewModel.isLockedByAnotherUser) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (!colors.isLcars) {
                            Text("🔒", fontSize = 64.sp)
                        } else {
                            Text(
                                "ACCESS DENIED",
                                fontSize = 32.sp,
                                fontFamily = AntonioFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = LcarsRed
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (colors.isLcars) "PROFILE LOCKED" else "Profile Locked",
                            fontSize = 24.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (colors.isLcars) {
                                "THIS PROFILE IS CURRENTLY BEING ACCESSED BY ANOTHER STATION OR WAS OPENED WITHIN THE LAST 5 MINUTES.\n\n" +
                                "TO RESOLVE:\n" +
                                "1. WAIT 5 MINUTES\n" +
                                "2. SYNC TERMINAL DATA\n" +
                                "3. RESTART AND RE-SYNC TERMINAL\n\n" +
                                "CONTACT SYSTEM ADMINISTRATOR (WINSTON) IF LOCK PERSISTS."
                            } else {
                                "This profile is currently being accessed by another tablet or was opened within the last 5 minutes.\n\n" +
                                "If this lock persists try:\n" +
                                "1. Waiting 5 minutes\n" +
                                "2. Syncing your tablet.\n" +
                                "3. Restarting then syncing your tablet.\n\n" +
                                "If this lock persists tell Winston and he will get it unlocked for you."
                            },
                            fontSize = 16.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colors.isLcars) LcarsRed else colors.hover,
                                contentColor = if (colors.isLcars) Color.White else Color.White
                            ),
                            shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (colors.isLcars) "CLOSE" else "Close",
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val headerAvatarBitmap = if (profile.avatar == "custom" && profileViewModel.avatarImage != null) {
                            val imgBytes = profileViewModel.avatarImage!!
                            remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)?.asImageBitmap() }
                        } else null
                        if (headerAvatarBitmap != null) {
                            Image(
                                bitmap = headerAvatarBitmap,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(36.dp).clip(
                                    if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp))
                                )
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            InitialsAvatar(
                                name = profile.displayName ?: actualName,
                                size = 36.dp,
                                fontSize = 13.sp,
                                bgColor = if (colors.isLcars) LcarsTan else colors.accent
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = if (colors.isLcars) "PROFILE & SETTINGS" else "⚙️ Profile & Settings",
                                fontSize = 20.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (colors.isLcars) actualName.uppercase() else actualName,
                                fontSize = 13.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textSecondary
                            )
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
                            "Loading your history… Kustom Kash is being calculated",
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

                Text(
                    text = if (colors.isLcars) "PROFILE PHOTO" else "📷 Profile Photo",
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))

                if (profile.inventory.contains("feature_custom_avatar")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current avatar preview
                        val previewAvatarBitmap = if (profile.avatar == "custom" && profileViewModel.avatarImage != null) {
                            val imgBytes = profileViewModel.avatarImage!!
                            remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)?.asImageBitmap() }
                        } else null
                        if (previewAvatarBitmap != null) {
                            Image(
                                bitmap = previewAvatarBitmap,
                                contentDescription = "Current avatar",
                                modifier = Modifier.size(56.dp).clip(
                                    if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp))
                                )
                            )
                        } else {
                            InitialsAvatar(
                                name = profile.displayName ?: actualName,
                                size = 56.dp,
                                fontSize = 20.sp,
                                bgColor = if (colors.isLcars) LcarsTan else colors.accent
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (colors.isLcars) LcarsAnakiwa else colors.accent,
                                    contentColor = if (colors.isLcars) Color.Black else Color.White
                                ),
                                shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (colors.isLcars) {
                                        if (profile.avatar == "custom") "CHANGE PHOTO" else "UPLOAD PHOTO"
                                    } else {
                                        if (profile.avatar == "custom") "Change Photo" else "Upload Photo"
                                    },
                                    fontSize = 13.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (profile.avatar == "custom") {
                                Button(
                                    onClick = { profileViewModel.clearCustomAvatar() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (colors.isLcars) LcarsRed else colors.hover,
                                        contentColor = if (colors.isLcars) Color.White else colors.textSecondary
                                    ),
                                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (colors.isLcars) "REMOVE PHOTO" else "Remove Photo",
                                        fontSize = 13.sp,
                                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (colors.isLcars) Color(0xFF111111) else colors.hover,
                                if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        InitialsAvatar(
                            name = profile.displayName ?: actualName,
                            size = 40.dp,
                            fontSize = 15.sp,
                            bgColor = if (colors.isLcars) LcarsTan.copy(alpha = 0.5f) else colors.accent.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (colors.isLcars) "INITIALS AVATAR" else "Initials Avatar",
                                fontSize = 13.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (colors.isLcars) "UNLOCK CUSTOM PHOTO IN SHOP" else "🔒 Unlock custom photo in Shop",
                                fontSize = 11.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Coin Balance Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (colors.isLcars) Color.Black else colors.hover,
                            if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp))
                        )
                        .then(
                            if (colors.isLcars) Modifier.border(1.dp, LcarsOrange, RectangleShape) else Modifier
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            CoinAmount(
                                amount = profile.coins,
                                fontSize = 24.sp,
                                iconSize = 28.dp,
                                color = if (colors.isLcars) LcarsOrange else com.example.timecard.ui.theme.CoinAmber
                            )
                            Text(
                                text = if (colors.isLcars) "KUSTOM KASH" else "Kustom Kash",
                                fontSize = 12.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
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
                Text(
                    text = if (colors.isLcars) "PERSONAL RECORDS" else "🏅 Personal Records",
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatChip("Best Week", "${String.format("%.2f", profile.records.bestWeekHours)} hrs", "", Modifier.weight(1f), colors)
                    StatChip("Best Day", "${String.format("%.2f", profile.records.busiestDay)} hrs", "", Modifier.weight(1f), colors)
                }
                Spacer(Modifier.height(8.dp))
                if (profile.records.favoriteJob.isNotBlank()) {
                    Text(
                        text = if (colors.isLcars) "FAVORITE JOB: ${profile.records.favoriteJob.uppercase()}" else "Favorite job: ${profile.records.favoriteJob}",
                        fontSize = 13.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Badges — only earned ones shown
                val totalEarned = profile.badges.values.sum()
                Text(
                    text = if (colors.isLcars) {
                        "BADGES${if (earnedBadges.isNotEmpty()) " ($totalEarned EARNED)" else ""}"
                    } else {
                        "🏆 Badges${if (earnedBadges.isNotEmpty()) " ($totalEarned earned)" else ""}"
                    },
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                if (earnedBadges.isEmpty()) {
                    Text(
                        text = if (colors.isLcars) "SAVE YOUR FIRST TIMECARD TO START EARNING BADGES!" else "Save your first timecard to start earning badges!",
                        fontSize = 12.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        color = colors.textSecondary
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((((earnedBadges.size + 3) / 4) * 90).dp.coerceAtLeast(90.dp)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(earnedBadges) { def ->
                            val count = profile.badges[def.id] ?: 1
                            val itemBg = if (colors.isLcars) Color.Black else colors.accent.copy(alpha = 0.12f)
                            val itemShape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                            val itemBorder = if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(itemBg, itemShape)
                                        .then(itemBorder)
                                        .clickable { selectedBadge = def }
                                        .padding(6.dp)
                                ) {
                                    val imgBytes = profileViewModel.badgeImages[def.id]
                                    val gridBadgeBitmap = imgBytes?.let { bytes ->
                                        remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                                    }
                                    if (gridBadgeBitmap != null) {
                                        Image(
                                            bitmap = gridBadgeBitmap,
                                            contentDescription = def.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(32.dp).clip(
                                                if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp))
                                            )
                                        )
                                    } else {
                                        if (colors.isLcars) {
                                            Text(
                                                text = def.name.take(3).uppercase(),
                                                fontFamily = AntonioFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = LcarsTan
                                            )
                                        } else {
                                            Text(def.emoji, fontSize = 22.sp)
                                        }
                                    }
                                    Text(
                                        text = if (colors.isLcars) def.name.uppercase() else def.name,
                                        fontSize = 9.sp,
                                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                        color = if (colors.isLcars) LcarsTan else colors.accent,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                // Count badge for repeatable badges earned more than once
                                if (def.repeatable && count > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .background(
                                                color = if (colors.isLcars) LcarsOrange else Color(0xFFD4AF37),
                                                shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp))
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "×$count",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                            color = if (colors.isLcars) Color.Black else Color(0xFF1A1A1A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Display Name
                Text(
                    text = if (colors.isLcars) "DISPLAY NAME" else "Display Name",
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = if (colors.isLcars) {
                        "SHOWN IN THE APP INSTEAD OF YOUR ACTUAL NAME. YOUR LOGIN ID NEVER CHANGES."
                    } else {
                        "Shown in the app instead of your actual name. Your login ID never changes."
                    },
                    fontSize = 12.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(8.dp))
                if (profile.inventory.contains("feature_display_name")) {
                    TextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        placeholder = {
                            Text(
                                text = if (colors.isLcars) actualName.uppercase() else actualName,
                                color = colors.textSecondary,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else com.example.timecard.ui.theme.OutfitFontFamily
                            )
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = if (colors.isLcars) AntonioFontFamily else com.example.timecard.ui.theme.OutfitFontFamily,
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                            unfocusedContainerColor = if (colors.isLcars) Color.Black else colors.input,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { profileViewModel.setDisplayName(displayNameInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isLcars) LcarsAnakiwa else colors.accent,
                            contentColor = if (colors.isLcars) Color.Black else Color.White
                        ),
                        shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (colors.isLcars) "SAVE DISPLAY NAME" else "Save Display Name",
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (colors.isLcars) Color(0xFF111111) else colors.hover,
                                if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (!colors.isLcars) {
                            Text("🔒", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = if (colors.isLcars) "LOCKED" else "Locked",
                                fontSize = 13.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (colors.isLcars) "UNLOCK CUSTOM NAME IN SHOP" else "🔒 Unlock custom name in Shop",
                                fontSize = 11.sp,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Accent Colors / Immersive Themes
                val unlockedCount = ACCENT_UNLOCKS.count { (_, _, itemId) -> profile.inventory.contains(itemId) }
                Text(
                    text = if (colors.isLcars) {
                        "IMMERSIVE THEME${if (unlockedCount == 0) " (BUY COLORS IN SHOP)" else ""}"
                    } else {
                        "🎨 Immersive Theme${if (unlockedCount == 0) " (buy colors in Shop)" else ""}"
                    },
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                val themeDisplayNames = mapOf(
                    null to "Default", "orange" to "Sunrise", "purple" to "Twilight",
                    "teal" to "Isle", "gold" to "Daybreak", "red" to "Red",
                    "hacker" to "Hacker", "sunset" to "Sunset", "midnight" to "Midnight",
                    "ocean" to "Ocean", "lcars" to "LCARS", "starwars" to "Star Wars"
                )
                val allThemeEntries = buildList {
                    add(Triple<String?, androidx.compose.ui.graphics.Color, String?>(null, com.example.timecard.ui.theme.AccentBlue, null))
                    ACCENT_UNLOCKS.forEach { (k, c, i) -> add(Triple<String?, androidx.compose.ui.graphics.Color, String?>(k, c, i)) }
                }
                allThemeEntries.chunked(6).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        chunk.forEach { (key, color, itemId) ->
                            val unlocked = itemId == null || profile.inventory.contains(itemId)
                            val selected = profile.accentColor == key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(
                                            if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(3.dp))
                                        )
                                        .background(if (unlocked) color else color.copy(alpha = 0.25f))
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = Color.White,
                                                    shape = if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(3.dp))
                                                )
                                            } else Modifier
                                        )
                                        .then(if (unlocked) Modifier.clickable { profileViewModel.setAccentColor(key) } else Modifier)
                                ) {
                                    if (!unlocked) {
                                        if (colors.isLcars) {
                                            Text("L", fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("🔒", fontSize = 7.sp, color = Color.White.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                val themeName = themeDisplayNames[key] ?: key ?: "?"
                                Text(
                                    text = if (colors.isLcars) themeName.uppercase() else themeName,
                                    fontSize = 8.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    maxLines = 1,
                                    color = if (selected) color else colors.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        repeat(6 - chunk.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (colors.isLcars) "BUY NEW COLORS IN THE SHOP. TAP TO APPLY." else "Buy new colors in the Shop. Tap to apply.",
                    fontSize = 11.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(24.dp))
                
                // Add Open Shop Button
                Button(
                    onClick = { 
                        onDismiss()
                        onOpenShop()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isLcars) LcarsTan.copy(alpha = 0.15f) else com.example.timecard.ui.theme.CoinAmber.copy(alpha = 0.15f),
                        contentColor = if (colors.isLcars) LcarsTan else com.example.timecard.ui.theme.CoinAmber
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (colors.isLcars) LcarsTan else com.example.timecard.ui.theme.CoinAmber
                    ),
                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        text = if (colors.isLcars) "VISIT SHOP" else "🛒 Visit Shop",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { 
                        onDismiss()
                        onNavigateToAlerts()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (colors.isLcars) Color.Black else colors.surface,
                        contentColor = if (colors.isLcars) Color.White else colors.textPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (colors.isLcars) LcarsTan else colors.border
                    ),
                    shape = if (colors.isLcars) RoundedCornerShape(50) else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (colors.isLcars) "VIEW PAST ALERTS" else "📋 View Past Alerts",
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!colors.isLcars) {
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
                } // end else block
                } // end inner Column

                if (colors.isLcars) {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(LcarsTan))
                }
            }
        }
    }

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

    val isLcars = colors.isLcars
    val chipBg = if (isLcars) Color.Black else colors.hover
    val chipBorderModifier = if (isLcars) {
        Modifier.border(1.dp, LcarsTan, RectangleShape)
    } else {
        Modifier
    }
    val chipShape = if (isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
    
    // Strip emoji if LCARS
    val displayValue = if (isLcars) {
        value.replace("🔥", "").replace("📆", "").trim()
    } else {
        value
    }

    Column(
        modifier = modifier
            .background(chipBg, chipShape)
            .then(chipBorderModifier)
            .padding(10.dp)
    ) {
        Text(
            text = if (isLcars) label.uppercase() else label,
            fontSize = 11.sp,
            fontFamily = if (isLcars) AntonioFontFamily else null,
            fontWeight = if (isLcars) FontWeight.Bold else FontWeight.Normal,
            color = colors.textSecondary
        )
        Text(
            text = if (isLcars) displayValue.uppercase() else displayValue,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            fontFamily = if (isLcars) AntonioFontFamily else com.example.timecard.ui.theme.JetBrainsMonoFontFamily,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        if (sub.isNotBlank()) {
            Text(
                text = if (isLcars) sub.uppercase() else sub,
                fontSize = 10.sp,
                fontFamily = if (isLcars) AntonioFontFamily else null,
                color = colors.textSecondary
            )
        }
    }
}
