package com.example.timecard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Wraps modal content in an authentic 1990s Windows 95 / Web dialog frame:
 *  [=== Mauve Title Bar: /// TITLE ///                 [✕] ===]
 *  [ 3D Inset Content Well                                     ]
 */
@Composable
fun RetroDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(RetroBackground)
            .doubleBeveledBorder(isInset = false)
            .padding(4.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(RetroHeader)
                .beveledBorder(isInset = false, borderWidth = 1.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "/// ${title.uppercase()} ///",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                color = RetroTextHeading
            )

            // Close button [✕]
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(RetroSurface)
                    .doubleBeveledBorder(isInset = false)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontFamily = VT323FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = RetroTextPrimary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Content Area inside Sunken 3D Well
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RetroSurface)
                .beveledBorder(isInset = true, borderWidth = 2.dp)
                .padding(12.dp)
        ) {
            content()
        }
    }
}

/** Translucent navy scrim for Retro modals (the Retro frame shows through, tinted blue). */
val RetroScrim = Color(0xB3000066)

fun retroAwareScrim(colors: TimecardColors, default: Color = Color.Black.copy(alpha = 0.6f)): Color =
    if (colors.isRetro) RetroScrim else default

/**
 * Windows-95-style title bar shared by Retro popups: navy-to-azure gradient, spinning globe,
 * rainbow title and a bevelled close box.
 */
@Composable
fun RetroTitleBar(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(RetroTitleStart, RetroTitleEnd)))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RetroGlobe(modifier = Modifier.padding(end = 6.dp), fontSize = 16.sp)
        RetroRainbowText(
            text = "/// ${title.uppercase()} ///",
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(RetroBackground)
                .doubleBeveledBorder(isInset = false)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
        }
    }
}

/**
 * MySpace-style content box: coloured title strip with chunky white caps over a pale body,
 * framed in the same colour.
 */
@Composable
fun RetroBox(
    title: String,
    modifier: Modifier = Modifier,
    headerColor: Color = RetroRoyal,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(2.dp, headerColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = title.uppercase(),
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAF4FF))
                .padding(8.dp),
            content = content
        )
    }
}
