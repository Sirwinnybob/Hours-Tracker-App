package com.example.timecard.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LocalTimecardColors
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PastAlertsModal(
    viewModel: AlertsViewModel,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current

    LaunchedEffect(Unit) {
        viewModel.loadPastAlerts()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(bottom = 48.dp)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 32.dp)
                    .background(
                        if (colors.isLcars) Color.Black else colors.surface,
                        if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp))
                    )
            ) {
                if (colors.isLcars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(LcarsOrange).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PAST ALERTS",
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

                Column(modifier = Modifier.padding(if (colors.isLcars) 16.dp else 20.dp)) {
                    if (!colors.isLcars) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\uD83D\uDCCB Past Alerts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textHeading,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismiss) {
                            Text("\u2715 Close", color = colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (viewModel.allAlerts.isEmpty()) {
                        Text(
                            text = "No alerts yet",
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(viewModel.allAlerts, key = { it.id }) { alert ->
                                PastAlertItem(alert = alert)
                            }
                        }
                    }
                }

                if (colors.isLcars) {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(LcarsTan))
                }
            }
        }
    }
}

@Composable
private fun PastAlertItem(alert: AlertsViewModel.MergedAlert) {
    val colors = LocalTimecardColors.current

    Card(
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.acknowledged) {
                colors.surface
            } else {
                Color(0xFFFFF3CD).copy(alpha = 0.15f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Message
            Text(
                text = formatAlertMessage(alert.message),
                fontSize = 14.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Meta row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val dateStr = try {
                    val instant = Instant.parse(alert.sentAt)
                    val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a")
                        .withZone(ZoneId.systemDefault())
                    formatter.format(instant)
                } catch (e: Exception) {
                    alert.sentAt
                }

                Text(
                    text = "From ${alert.sentBy ?: "Admin"} \u2022 $dateStr",
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Badge
                val badgeColor = if (alert.acknowledged) Color(0xFF48BB78) else Color(0xFFECC94B)
                val badgeText = if (alert.acknowledged) "\u2713 Acknowledged" else "Pending"
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }

            // Response if present
            if (!alert.response.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reply: \"${alert.response}\"",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.accent
                )
            }
        }
    }
}
