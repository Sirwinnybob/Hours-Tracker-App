package com.example.timecard.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun ChallengesModal(
    viewModel: ChallengesViewModel,
    onLoad: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current

    LaunchedEffect(Unit) { onLoad() }

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
                    "🎯 Weekly Challenges",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Complete challenges to earn bonus KK Coins!",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Spacer(Modifier.height(16.dp))

                if (viewModel.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(40.dp))
                    }
                } else if (viewModel.challengeProgress.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No challenges this week",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.challengeProgress) { cp ->
                            ChallengeCard(cp = cp)
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

@Composable
private fun ChallengeCard(cp: ChallengeProgress) {
    val colors = LocalTimecardColors.current
    val bgColor = when {
        cp.isComplete -> colors.accent.copy(alpha = 0.12f)
        else          -> colors.hover
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cp.challenge.icon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    cp.challenge.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (cp.isComplete) colors.accent else colors.textPrimary
                )
                Text(
                    cp.challenge.description,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            if (cp.isComplete) {
                Text("✅", fontSize = 20.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(size = 14.dp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "+${cp.challenge.reward}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoinAmber
                    )
                }
            }
        }

        if (!cp.isComplete) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { cp.progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.accent,
                trackColor = colors.surface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${(cp.progress * 100).toInt()}%",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 12.dp)
                Spacer(Modifier.width(3.dp))
                Text(
                    "+${cp.challenge.reward} KK Coins earned!",
                    fontSize = 11.sp,
                    color = colors.accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
