package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.timecardShape

@Composable
fun UserProfileSheet(
    entry: LeaderboardEntry,
    badgeImages: Map<String, ByteArray>,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    val scrollState = rememberScrollState()
    val earnedBadges = BadgeEngine.ALL_BADGES.filter { def -> (entry.badges[def.id] ?: 0) > 0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(16.dp)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, timecardShape(RoundedCornerShape(20.dp)))
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                val avatarBytes = entry.avatarBytes
                if (avatarBytes != null) {
                    Image(
                        bitmap = remember(avatarBytes) {
                            BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size).asImageBitmap()
                        },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                    )
                } else {
                    val initials = (entry.displayName ?: entry.name).take(2).uppercase()
                    Box(
                        modifier = Modifier.size(72.dp).background(colors.accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    entry.displayName ?: entry.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                if (entry.displayName != null) {
                    Text(entry.name, fontSize = 12.sp, color = colors.textSecondary)
                }

                Spacer(Modifier.height(16.dp))

                // KK Coins
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.hover, timecardShape(RoundedCornerShape(12.dp)))
                        .padding(16.dp)
                ) {
                    CoinAmount(amount = entry.coins, fontSize = 22.sp, iconSize = 26.dp)
                    Text("Kustom Kash", fontSize = 12.sp, color = colors.textSecondary)
                }

                Spacer(Modifier.height(12.dp))

                // Streaks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.hover, timecardShape(RoundedCornerShape(10.dp)))
                            .padding(10.dp)
                    ) {
                        Text("Daily Streak", fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            "🔥 ${entry.currentStreak}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text("Best: ${entry.bestDailyStreak}", fontSize = 10.sp, color = colors.textSecondary)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.hover, timecardShape(RoundedCornerShape(10.dp)))
                            .padding(10.dp)
                    ) {
                        Text("Weekly Streak", fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            "📆 ${entry.currentWeeklyStreak}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text("Best: ${entry.bestWeeklyStreak}", fontSize = 10.sp, color = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Personal Records
                Text(
                    "🏅 Personal Records",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.hover, timecardShape(RoundedCornerShape(10.dp)))
                            .padding(10.dp)
                    ) {
                        Text("Best Week", fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            "${String.format("%.2f", entry.bestWeekHours)} hrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(colors.hover, timecardShape(RoundedCornerShape(10.dp)))
                            .padding(10.dp)
                    ) {
                        Text("Best Day", fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            "${String.format("%.2f", entry.bestDayHours)} hrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Badges
                if (earnedBadges.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "🏆 Badges (${earnedBadges.size} earned)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((((earnedBadges.size + 3) / 4) * 80).dp.coerceAtLeast(80.dp)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(earnedBadges) { def ->
                            val imgBytes = badgeImages[def.id]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        colors.accent.copy(alpha = 0.12f),
                                        timecardShape(RoundedCornerShape(10.dp))
                                    )
                                    .padding(6.dp)
                            ) {
                                if (imgBytes != null) {
                                    Image(
                                        bitmap = remember(imgBytes) {
                                            BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap()
                                        },
                                        contentDescription = def.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(timecardShape(RoundedCornerShape(6.dp)))
                                    )
                                } else {
                                    Text(def.emoji, fontSize = 22.sp)
                                }
                                Text(
                                    def.name,
                                    fontSize = 9.sp,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Purchases
                if (entry.purchaseHistory.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "🛒 Purchases",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        entry.purchaseHistory.forEach { purchase ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.hover, timecardShape(RoundedCornerShape(8.dp)))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    purchase.itemTitle,
                                    fontSize = 13.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                CoinAmount(amount = purchase.price, fontSize = 12.sp, color = CoinAmber)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                    shape = timecardShape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
