package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.theme.LocalTimecardColors
import kotlinx.coroutines.delay

@Composable
fun BadgePopup(
    badgeId: String?,
    badgeImages: Map<String, ByteArray> = emptyMap(),
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(badgeId) {
        if (badgeId != null) {
            visible = true
            delay(3500)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(400)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(300))
        ) {
            val def = badgeId?.let { BadgeEngine.getDefinition(it) }
            if (def != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .background(
                            Color(0xFF1E3A2F),
                            com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp))
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    val imgBytes = badgeImages[def.id]
                    if (imgBytes != null) {
                        Image(
                            bitmap = remember(imgBytes) { BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size).asImageBitmap() },
                            contentDescription = def.name,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Text(def.emoji, fontSize = 36.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Badge Unlocked!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4ADE80),
                            letterSpacing = 1.sp
                        )
                        Text(
                            def.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "\"${def.flavorText}\"",
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
        }
    }
}
