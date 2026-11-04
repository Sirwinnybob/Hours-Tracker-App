package com.example.timecard.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── 90s Web & Desktop Bevel Borders ──────────────────────────────────────────

/**
 * Renders an authentic 1990s 3D beveled border:
 * - Outset (default): top/left highlight (#FFFFFF), bottom/right shadow (#65466C).
 * - Inset (sunken wells, text boxes): top/left shadow, bottom/right highlight.
 */
fun Modifier.beveledBorder(
    isInset: Boolean = false,
    highlight: Color = RetroBorderHighlight,
    shadow: Color = RetroBorderShadow,
    borderWidth: Dp = 2.dp
): Modifier = this.drawBehind {
    val stroke = borderWidth.toPx()
    val halfStroke = stroke / 2f
    val w = size.width
    val h = size.height

    val topColor = if (isInset) shadow else highlight
    val bottomColor = if (isInset) highlight else shadow

    // Top border
    drawLine(
        color = topColor,
        start = Offset(0f, halfStroke),
        end = Offset(w, halfStroke),
        strokeWidth = stroke
    )
    // Left border
    drawLine(
        color = topColor,
        start = Offset(halfStroke, 0f),
        end = Offset(halfStroke, h),
        strokeWidth = stroke
    )
    // Bottom border
    drawLine(
        color = bottomColor,
        start = Offset(0f, h - halfStroke),
        end = Offset(w, h - halfStroke),
        strokeWidth = stroke
    )
    // Right border
    drawLine(
        color = bottomColor,
        start = Offset(w - halfStroke, 0f),
        end = Offset(w - halfStroke, h),
        strokeWidth = stroke
    )
}

/**
 * Double 3D beveled border for classic Windows 95 button and frame look.
 */
fun Modifier.doubleBeveledBorder(
    isInset: Boolean = false,
    outerHighlight: Color = Color(0xFFFFFFFF),
    innerHighlight: Color = Color(0xFFEAF4FF),
    innerShadow: Color = Color(0xFF6A8CC8),
    outerShadow: Color = Color(0xFF1C2A66)
): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height

    val c1 = if (isInset) outerShadow else outerHighlight
    val c2 = if (isInset) innerShadow else innerHighlight
    val c3 = if (isInset) innerHighlight else innerShadow
    val c4 = if (isInset) outerHighlight else outerShadow

    // Outer Top & Left (c1)
    drawLine(c1, Offset(0f, 0.5f), Offset(w, 0.5f), 1f)
    drawLine(c1, Offset(0.5f, 0f), Offset(0.5f, h), 1f)

    // Inner Top & Left (c2)
    drawLine(c2, Offset(1f, 1.5f), Offset(w - 1f, 1.5f), 1f)
    drawLine(c2, Offset(1.5f, 1f), Offset(1.5f, h - 1f), 1f)

    // Inner Bottom & Right (c3)
    drawLine(c3, Offset(1f, h - 1.5f), Offset(w - 1f, h - 1.5f), 1f)
    drawLine(c3, Offset(w - 1.5f, 1f), Offset(w - 1.5f, h - 1f), 1f)

    // Outer Bottom & Right (c4)
    drawLine(c4, Offset(0f, h - 0.5f), Offset(w, h - 0.5f), 1f)
    drawLine(c4, Offset(w - 0.5f, 0f), Offset(w - 0.5f, h), 1f)
}

// ── 90s Web Button ────────────────────────────────────────────────────────────

@Composable
fun RetroButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    backgroundColor: Color = RetroSurface,
    textColor: Color = RetroTextPrimary,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isSunken = isSelected || isPressed

    Box(
        modifier = modifier
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f))
            .doubleBeveledBorder(isInset = isSunken)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val pixelIcon = icon?.let { RetroIcon.fromEmoji(it) }
            if (pixelIcon != null) {
                RetroPixelIcon(
                    icon = pixelIcon,
                    modifier = Modifier.padding(end = 6.dp),
                    bounce = isSelected
                )
            } else if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = label,
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp,
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ── Retro Visitor Counter ─────────────────────────────────────────────────────

@Composable
fun RetroHitCounter(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RetroSparkle(phase = 0f, fontSize = 11.sp)
            Text(
                text = " VISITOR ",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = RetroTextHeading,
                letterSpacing = 1.sp
            )
            RetroSparkle(phase = 0.5f, fontSize = 11.sp)
        }
        Spacer(Modifier.height(2.dp))
        RetroTickingCounter()
    }
}

// ── Retro Full-Frame Layout ────────────────────────────────────────────────

val RETRO_SIDEBAR_WIDTH = 138.dp

@Composable
fun RetroFrame(
    modifier: Modifier = Modifier,
    headerContent: @Composable RowScope.() -> Unit = {},
    footerContent: @Composable RowScope.() -> Unit = {},
    sidebarContent: @Composable ColumnScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RetroBackground)
    ) {
        // ── 1. Desktop Window Title Bar (navy -> azure gradient, rainbow title, spinning globe) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(Brush.horizontalGradient(listOf(RetroTitleStart, RetroTitleEnd)))
                .beveledBorder(isInset = false, borderWidth = 1.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroGlobe(modifier = Modifier.padding(end = 6.dp), fontSize = 15.sp)
            RetroRainbowText(
                text = "/// KKC HOURS TRACKER 1995: RETRO DIRECTORY ///",
                modifier = Modifier.weight(1f)
            )

            // Window control buttons: [_] [🗖] [✕]
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(RetroSurface)
                        .beveledBorder(isInset = false, borderWidth = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("_", fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetroTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(RetroSurface)
                        .beveledBorder(isInset = false, borderWidth = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("□", fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetroTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(RetroSurface)
                        .beveledBorder(isInset = false, borderWidth = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RetroTextPrimary)
                }
            }
        }

        // ── 2. Web Browser Address & Toolbar Bar ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RetroBackgroundTo)
                .beveledBorder(isInset = false, borderWidth = 1.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location:",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RetroTextPrimary,
                modifier = Modifier.padding(end = 6.dp)
            )

            // Sunken URL bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .background(RetroInput)
                    .beveledBorder(isInset = true, borderWidth = 1.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val caretOn = rememberRetroBlink(1000)
                Text(
                    text = "http://kkc.retro/hours/timesheet" + if (caretOn) "▌" else " ",
                    fontFamily = VT323FontFamily,
                    fontSize = 14.sp,
                    color = RetroTextTotal,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            headerContent()
        }

        // ── 3. Main Center Area (Sidebar + Content Cutout) ────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            // Left Sidebar: drifting clouds behind the directory buttons
            Box(
                modifier = Modifier
                    .width(RETRO_SIDEBAR_WIDTH)
                    .fillMaxHeight()
                    .beveledBorder(isInset = false, borderWidth = 2.dp)
                    .padding(2.dp)
            ) {
                RetroClouds()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sidebar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(RetroTitleStart, RetroTitleEnd)))
                            .beveledBorder(isInset = false, borderWidth = 1.dp)
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RetroSparkle(phase = 0.0f, fontSize = 12.sp)
                        Text(
                            text = " DIRECTORY ",
                            fontFamily = VT323FontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        RetroSparkle(phase = 0.5f, fontSize = 12.sp)
                    }

                    sidebarContent()

                    Spacer(Modifier.weight(1f))

                    RetroConstructionTape()

                    RetroWater(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RetroBackground)
                            .beveledBorder(isInset = false, borderWidth = 1.dp)
                            .padding(vertical = 3.dp)
                    ) {
                        RetroHitCounter()
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // Main Content Well: Inset Beveled Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(RetroRoyal)
                    .padding(4.dp)
                    .background(RetroSurface)
                    .beveledBorder(isInset = true, borderWidth = 2.dp)
                    .padding(4.dp)
            ) {
                content()
            }
        }

        // ── 4. Bottom Browser Status Bar (gray, with a scrolling marquee) ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(RetroBackground)
                .beveledBorder(isInset = false, borderWidth = 1.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✓ Done",
                fontFamily = VT323FontFamily,
                fontSize = 13.sp,
                color = RetroTextPrimary
            )

            RetroMarquee(
                text = MARQUEE_TEXT,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            footerContent()

            Text(
                text = "© 1995 KKC",
                fontFamily = VT323FontFamily,
                fontSize = 13.sp,
                color = RetroTextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // ── 5. Optional taskbar (the KKC destinations when launched from KKC) ──
        bottomBar?.invoke()
    }
}

private const val MARQUEE_TEXT =
    "★ WELCOME TO THE KKC HOURS TRACKER HOMEPAGE ★ BEST VIEWED IN NETSCAPE NAVIGATOR AT 800x600 ★ " +
        "PLEASE SIGN MY GUESTBOOK ★ YOU ARE VISITOR NUMBER 001995 ★ THIS SITE IS UNDER CONSTRUCTION ★ "

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMarquee(text: String, modifier: Modifier = Modifier) {
    val motion = rememberRetroMotion()
    Text(
        text = text,
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = RetroTextTotal,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.then(
            if (motion) Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 70.dp) else Modifier
        )
    )
}
