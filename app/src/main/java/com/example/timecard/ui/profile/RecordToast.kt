package com.example.timecard.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun RecordToast(
    message: String?,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            visible = true
            delay(3000)
            visible = false
            delay(350)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(99f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(250))
        ) {
            if (message != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 88.dp)
                        .background(Color(0xFF2A2000), com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(14.dp)))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text("🏅", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        message,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                }
            }
        }
    }
}
