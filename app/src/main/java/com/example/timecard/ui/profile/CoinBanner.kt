package com.example.timecard.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay

@Composable
fun CoinBanner(
    coinsEarned: Int?,
    onDismiss: () -> Unit
) {
    val visible = coinsEarned != null

    LaunchedEffect(visible) {
        if (visible) {
            delay(3000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(50f), // Above other overlays
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it - 50 },
                animationSpec = tween(500)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it - 50 },
                animationSpec = tween(400)
            )
        ) {
            if (coinsEarned != null) {
                Row(
                    modifier = Modifier
                        .padding(top = 40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = CoinAmber,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onDismiss() }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "+$coinsEarned COINS EARNED",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CoinAmber
                    )
                }
            }
        }
    }
}
