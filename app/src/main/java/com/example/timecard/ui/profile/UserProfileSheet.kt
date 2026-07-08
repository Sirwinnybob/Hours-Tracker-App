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

import com.example.timecard.ui.theme.LcarsDialogFrame
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsPurple
import com.example.timecard.ui.theme.LcarsAnakiwa
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.border

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
        val sheetContent = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (colors.isLcars) Color.Black else colors.surface)
                    .then(if (!colors.isLcars) Modifier.padding(24.dp) else Modifier.padding(16.dp))
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                val avatarBytes = entry.avatarBytes
                if (avatarBytes != null) {
                    val bitmap = remember(avatarBytes) {
                        BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(
                                if (colors.isLcars) RectangleShape else CircleShape
                            )
                        )
                    } else {
                        val initials = (entry.displayName ?: entry.name).take(2).uppercase()
                        Box(
                            modifier = Modifier.size(72.dp).background(
                                if (colors.isLcars) LcarsTan else colors.accent,
                                if (colors.isLcars) RectangleShape else CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = if (colors.isLcars) AntonioFontFamily else null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (colors.isLcars) (entry.displayName ?: entry.name).uppercase() else (entry.displayName ?: entry.name),
                    fontSize = 18.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                if (entry.displayName != null) {
                    Text(
                        text = if (colors.isLcars) entry.name.uppercase() else entry.name,
                        fontSize = 12.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(16.dp))

                // KK Coins
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (colors.isLcars) Color.Black else colors.hover,
                            if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(12.dp))
                        )
                        .then(
                            if (colors.isLcars) Modifier.border(1.dp, LcarsOrange, RectangleShape) else Modifier
                        )
                        .padding(16.dp)
                ) {
                    CoinAmount(
                        amount = entry.coins,
                        fontSize = 22.sp,
                        iconSize = 26.dp,
                        color = if (colors.isLcars) LcarsOrange else CoinAmber
                    )
                    Text(
                        text = if (colors.isLcars) "KUSTOM KASH" else "Kustom Kash",
                        fontSize = 12.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                        fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
                        color = colors.textSecondary
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Streaks
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dailyStreak = entry.currentStreak
                    val displayDaily = if (colors.isLcars) dailyStreak.toString() else "🔥 $dailyStreak"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (colors.isLcars) Color.Black else colors.hover,
                                if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (colors.isLcars) "DAILY STREAK" else "Daily Streak",
                            fontSize = 11.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (colors.isLcars) displayDaily.uppercase() else displayDaily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (colors.isLcars) "BEST: ${entry.bestDailyStreak}" else "Best: ${entry.bestDailyStreak}",
                            fontSize = 10.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textSecondary
                        )
                    }

                    val weeklyStreak = entry.currentWeeklyStreak
                    val displayWeekly = if (colors.isLcars) weeklyStreak.toString() else "📆 $weeklyStreak"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (colors.isLcars) Color.Black else colors.hover,
                                if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (colors.isLcars) "WEEKLY STREAK" else "Weekly Streak",
                            fontSize = 11.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (colors.isLcars) displayWeekly.uppercase() else displayWeekly,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (colors.isLcars) "BEST: ${entry.bestWeeklyStreak}" else "Best: ${entry.bestWeeklyStreak}",
                            fontSize = 10.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Personal Records
                Text(
                    text = if (colors.isLcars) "PERSONAL RECORDS" else "🏅 Personal Records",
                    fontSize = 15.sp,
                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
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
                            .background(
                                if (colors.isLcars) Color.Black else colors.hover,
                                if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (colors.isLcars) "BEST WEEK" else "Best Week",
                            fontSize = 11.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${String.format("%.2f", entry.bestWeekHours)} HRS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textPrimary
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (colors.isLcars) Color.Black else colors.hover,
                                if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(10.dp))
                            )
                            .then(
                                if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (colors.isLcars) "BEST DAY" else "Best Day",
                            fontSize = 11.sp,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            fontWeight = if (colors.isLcars) FontWeight.Bold else FontWeight.Normal,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "${String.format("%.2f", entry.bestDayHours)} HRS",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                            color = colors.textPrimary
                        )
                    }
                }

                // Badges
                if (earnedBadges.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (colors.isLcars) "BADGES (${earnedBadges.size} EARNED)" else "🏆 Badges (${earnedBadges.size} earned)",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
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
                            val itemBg = if (colors.isLcars) Color.Black else colors.accent.copy(alpha = 0.12f)
                            val itemShape = if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(10.dp))
                            val itemBorder = if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBg, itemShape)
                                    .then(itemBorder)
                                    .padding(6.dp)
                            ) {
                                if (imgBytes != null) {
                                    val bitmap = remember(imgBytes) {
                                        BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)?.asImageBitmap()
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = def.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(6.dp)))
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
                                }
                                Text(
                                    text = if (colors.isLcars) def.name.uppercase() else def.name,
                                    fontSize = 9.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    color = if (colors.isLcars) LcarsTan else colors.accent,
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
                        text = if (colors.isLcars) "PURCHASES" else "🛒 Purchases",
                        fontSize = 15.sp,
                        fontFamily = if (colors.isLcars) AntonioFontFamily else null,
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
                                    .background(
                                        if (colors.isLcars) Color.Black else colors.hover,
                                        if (colors.isLcars) RectangleShape else timecardShape(RoundedCornerShape(8.dp))
                                    )
                                    .then(
                                        if (colors.isLcars) Modifier.border(1.dp, LcarsTan, RectangleShape) else Modifier
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (colors.isLcars) purchase.itemTitle.uppercase() else purchase.itemTitle,
                                    fontSize = 13.sp,
                                    fontFamily = if (colors.isLcars) AntonioFontFamily else null,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                CoinAmount(
                                    amount = purchase.price,
                                    fontSize = 12.sp,
                                    color = if (colors.isLcars) LcarsOrange else CoinAmber
                                )
                            }
                        }
                    }
                }

                if (!colors.isLcars) {
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

        if (colors.isLcars) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                LcarsDialogFrame(
                    title = "USER PROFILE",
                    onDismiss = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sheetContent()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                sheetContent()
            }
        }
    }
}
