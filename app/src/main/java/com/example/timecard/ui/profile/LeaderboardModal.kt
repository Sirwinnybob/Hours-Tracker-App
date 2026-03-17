package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.LocalTimecardColors

private fun activityEventDescription(event: ActivityEvent): String = when (event.type) {
    "badge_earned"     -> "earned the ${event.detail} badge"
    "streak_milestone" -> "reached a ${event.detail}-day streak!"
    "record_broken"    -> "set a new record: ${event.detail}"
    "coins_earned"     -> "earned ${event.detail}"
    else               -> event.detail
}

private fun relativeTime(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val diff = java.time.Instant.now().epochSecond - instant.epochSecond
        when {
            diff < 60      -> "just now"
            diff < 3600    -> "${diff / 60}m ago"
            diff < 86400   -> "${diff / 3600}h ago"
            diff < 604800  -> "${diff / 86400}d ago"
            else           -> "${diff / 604800}w ago"
        }
    } catch (_: Exception) { "" }
}

@Composable
fun LeaderboardModal(
    viewModel: LeaderboardViewModel,
    myName: String,
    badgeImages: Map<String, ByteArray> = emptyMap(),
    feedEmployeeNames: List<String> = emptyList(),
    onFeedTabSelected: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    var tab by remember { mutableIntStateOf(0) } // 0=Week, 1=Month, 2=Streak, 3=Coins, 4=Feed
    var feedLoaded by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }

    LaunchedEffect(tab) {
        if (tab == 4 && !feedLoaded) {
            feedLoaded = true
            onFeedTabSelected()
        }
    }

    // Show profile sheet when an entry is tapped
    selectedEntry?.let { entry ->
        UserProfileSheet(
            entry = entry,
            badgeImages = badgeImages,
            onDismiss = { selectedEntry = null }
        )
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
            ) {
                Text(
                    "🏆 Leaderboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.height(16.dp))

                // Tab bar — 5 tabs: Week / Month / Streak / Coins / Feed
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tab 0–2: text labels
                    listOf("Week", "Month", "🔥 Streak").forEachIndexed { index, label ->
                        val selected = tab == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                                .background(if (selected) colors.accent else colors.hover)
                                .clickable { tab = index }
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else colors.textSecondary
                            )
                        }
                    }
                    // Tab 3: Coins (icon + label)
                    val coinsSelected = tab == 3
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                            .background(if (coinsSelected) colors.accent else colors.hover)
                            .clickable { tab = 3 }
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            CoinIcon(size = 13.dp)
                            Text(
                                "KK Coins",
                                fontSize = 11.sp,
                                fontWeight = if (coinsSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (coinsSelected) Color.White else colors.textSecondary
                            )
                        }
                    }
                    // Tab 4: Feed
                    val feedSelected = tab == 4
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                            .background(if (feedSelected) colors.accent else colors.hover)
                            .clickable { tab = 4 }
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "📰 Feed",
                            fontSize = 11.sp,
                            fontWeight = if (feedSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (feedSelected) Color.White else colors.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (tab == 4) {
                    // ── Feed Tab ──────────────────────────────────────────────
                    if (viewModel.isFeedLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(40.dp))
                        }
                    } else if (viewModel.feedEvents.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No activity yet", color = colors.textSecondary, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(viewModel.feedEvents) { event ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.hover, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(event.detailIcon, fontSize = 20.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row {
                                            Text(
                                                event.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (event.employeeName == myName) colors.accent else colors.textPrimary
                                            )
                                            Text(
                                                " ${activityEventDescription(event)}",
                                                fontSize = 13.sp,
                                                color = colors.textPrimary
                                            )
                                        }
                                        val timeStr = relativeTime(event.timestamp)
                                        if (timeStr.isNotEmpty()) {
                                            Text(timeStr, fontSize = 11.sp, color = colors.textSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (viewModel.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(40.dp))
                    }
                } else if (viewModel.entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data available", color = colors.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    val sorted = when (tab) {
                        0 -> viewModel.entries.sortedByDescending { it.weekHours }
                        1 -> viewModel.entries.sortedByDescending { it.monthHours }
                        2 -> viewModel.entries.sortedByDescending { it.currentStreak }
                        else -> viewModel.entries.sortedByDescending { it.allTimeCoins }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(sorted) { index, entry ->
                            val isMe = entry.name == myName
                            val rank = index + 1
                            val rankEmoji = when (rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                3 -> "🥉"
                                else -> " $rank "
                            }
                            val displayedName = entry.displayName ?: entry.name
                            val valueText = when (tab) {
                                0 -> "${String.format("%.2f", entry.weekHours)} hrs"
                                1 -> "${String.format("%.2f", entry.monthHours)} hrs"
                                2 -> "🔥 ${entry.currentStreak} days"
                                else -> null // coins tab uses CoinAmount composable
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isMe) colors.accent.copy(alpha = 0.12f) else colors.hover,
                                        com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                                    )
                                    .clickable { selectedEntry = entry }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(rankEmoji, fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))

                                // Avatar
                                val avatarBytes = entry.avatarBytes
                                if (avatarBytes != null) {
                                    Image(
                                        bitmap = remember(avatarBytes) {
                                            BitmapFactory.decodeByteArray(avatarBytes, 0, avatarBytes.size).asImageBitmap()
                                        },
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val initials = displayedName.take(2).uppercase()
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(colors.accent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            initials,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        displayedName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isMe) colors.accent else colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (valueText != null) {
                                    Text(
                                        valueText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = com.example.timecard.ui.theme.JetBrainsMonoFontFamily,
                                        color = if (isMe) colors.accent else colors.textPrimary
                                    )
                                } else {
                                    CoinAmount(
                                        amount = entry.allTimeCoins,
                                        fontSize = 14.sp,
                                        color = if (isMe) colors.accent else CoinAmber
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

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
