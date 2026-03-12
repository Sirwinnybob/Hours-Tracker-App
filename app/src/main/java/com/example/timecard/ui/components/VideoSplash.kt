package com.example.timecard.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.timecard.R

@OptIn(UnstableApi::class)
@Composable
fun VideoSplash(modifier: Modifier = Modifier, onComplete: () -> Unit = {}) {
    val context = LocalContext.current
    
    // Create ExoPlayer exactly once
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.anim_splash}")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            prepare()
        }
    }

    // Safety net: don't let the splash screen hang the app forever
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000L) // Max 5 seconds
        onComplete()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onComplete()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // If the video fails to load or play, just skip the splash
                onComplete()
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.play()
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Block all touch input until video finishes
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    controllerAutoShow = false
                    // Hide any default buffering/error UI
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    player = exoPlayer
                    hideController()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
