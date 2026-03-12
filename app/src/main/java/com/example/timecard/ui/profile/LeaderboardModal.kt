package com.example.timecard.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun LeaderboardModal(
    viewModel: LeaderboardViewModel,
    myName: String,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    var tab by remember { mutableIntStateOf(0) } // 0=Week, 1=Month, 2=XP

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

                // Tab bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("This Week", "This Month", "🪙 All-Time Coins").forEachIndexed { index, label ->
                        val selected = tab == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(20.dp)))
                                .background(if (selected) colors.accent else colors.hover)
                                .clickable { tab = index }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (viewModel.isLoading) {
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
                                else -> "🪙 ${entry.allTimeCoins}"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isMe) colors.accent.copy(alpha = 0.12f) else colors.hover,
                                        com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp))
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(rankEmoji, fontSize = 18.sp)
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
                                Text(
                                    valueText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = com.example.timecard.ui.theme.JetBrainsMonoFontFamily,
                                    color = if (isMe) colors.accent else colors.textPrimary
                                )
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
