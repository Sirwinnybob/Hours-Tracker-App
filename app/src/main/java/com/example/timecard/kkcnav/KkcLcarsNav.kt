package com.example.timecard.kkcnav

// Hours Tracker–only LCARS version of the mirrored KKC navbar. NOT a copy of KKCSheetTracker code:
// with the LCARS theme equipped there is no floating glass bar. The KKC destinations live in the
// LCARS frame's bottom band instead (TimesheetScreen passes KkcLcarsNavButtons to LcarsFrame's
// footer, expanded to KKC_LCARS_NAV_HEIGHT), and on the login screen — which has no frame — in a
// standalone band (KkcLcarsNavBand). Destinations, Calc-before-Hours order, badges and tap
// behavior still follow the KKC contract (kkcNavSlots / kkcBadgeCount / KkcReturnNavigation).

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LCARS_GAP
import com.example.timecard.ui.theme.LcarsBlueBell
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsPurple
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsYellow

/** Height of the LCARS bottom band when it carries the KKC destinations (default band is 20.dp). */
val KKC_LCARS_NAV_HEIGHT: Dp = 52.dp

private val KkcLcarsPalette = listOf(LcarsOrange, LcarsTan, LcarsPurple, LcarsBlueBell)

/** Blocks cycle orange → tan → lilac → blue-bell; the shown destination (Hours) is pale yellow. */
internal fun kkcLcarsBlockColor(index: Int, selected: Boolean): Color =
    if (selected) LcarsYellow else KkcLcarsPalette[index % KkcLcarsPalette.size]

internal fun kkcLcarsLabel(route: String): String = kkcNavLabel(route).uppercase()

// ── Normal ↔ LCARS navbar transition (navbar only; the rest of the theme switch is instant) ──────
// Normal → LCARS: the glass bar slides down + fades (KKC_GLASS_SWAP_MS), then the LCARS blocks power
// up left → right, one every KKC_LCARS_STAGGER_MS. LCARS → normal: blocks retract right → left, then
// the glass bar slides back up. Low-end (payload.animDisabled) swaps instantly. Driven by TimecardApp.
const val KKC_GLASS_SWAP_MS = 220
const val KKC_LCARS_STAGGER_MS = 45
const val KKC_LCARS_BLOCK_MS = 170
/**
 * Launch from KKC into LCARS: delay before the glass → LCARS morph, so it starts as the window
 * cross-fade (android.R.anim.fade_in = 500 ms) is finishing. Tuned on the tablet (450 felt late).
 */
const val KKC_LAUNCH_FADE_MS = 250L

/** How the LCARS blocks are shown right now. */
enum class KkcLcarsBlockAnim { None, Enter, Exit }

internal fun kkcLcarsBlockDelayMs(index: Int, count: Int, entering: Boolean): Int =
    if (entering) KKC_GLASS_SWAP_MS + index * KKC_LCARS_STAGGER_MS
    else (count - 1 - index) * KKC_LCARS_STAGGER_MS

/** Time until the last block has finished powering up. */
internal fun kkcLcarsEnterTotalMs(count: Int): Int =
    kkcLcarsBlockDelayMs(count - 1, count, entering = true) + KKC_LCARS_BLOCK_MS

/** Time until the left-most block has finished retracting (the glass bar may then return). */
internal fun kkcLcarsRetractTotalMs(count: Int): Int =
    kkcLcarsBlockDelayMs(0, count, entering = false) + KKC_LCARS_BLOCK_MS

/** The KKC destinations as LCARS blocks, filling the band they are placed in. */
@Composable
internal fun KkcLcarsNavButtons(
    payload: KkcNavBarPayload,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    anim: KkcLcarsBlockAnim = KkcLcarsBlockAnim.None
) {
    val slots = kkcNavSlots(payload.destinations)
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(LCARS_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, route ->
            val selected = route == KkcNavBarContract.DEST_HOURS
            // 0 = retracted (invisible, dropped, squeezed toward its left edge), 1 = fully shown.
            val progress = remember { Animatable(if (anim == KkcLcarsBlockAnim.Enter) 0f else 1f) }
            LaunchedEffect(anim) {
                when (anim) {
                    KkcLcarsBlockAnim.None -> progress.snapTo(1f)
                    KkcLcarsBlockAnim.Enter -> {
                        progress.snapTo(0f)
                        delay(kkcLcarsBlockDelayMs(index, slots.size, entering = true).toLong())
                        progress.animateTo(1f, tween(KKC_LCARS_BLOCK_MS, easing = FastOutSlowInEasing))
                    }
                    KkcLcarsBlockAnim.Exit -> {
                        delay(kkcLcarsBlockDelayMs(index, slots.size, entering = false).toLong())
                        progress.animateTo(0f, tween(KKC_LCARS_BLOCK_MS, easing = FastOutSlowInEasing))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val p = progress.value
                        alpha = p
                        scaleX = 0.4f + 0.6f * p
                        translationY = (1f - p) * size.height * 0.5f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            ) {
                KkcLcarsNavBlock(
                    route = route,
                    selected = selected,
                    code = (index + 1).toString().padStart(2, '0'),
                    color = kkcLcarsBlockColor(index, selected),
                    rightCap = index == slots.lastIndex,
                    onClick = { onNavigate(route) }
                )
                val badgeCount = kkcBadgeCount(route, payload)
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .background(LcarsRed)
                            .padding(horizontal = 5.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * One LCARS destination block: a flat palette-colored block (rounded right cap on the last one) with
 * the KKC nav icon over the Antonio label in black, and the block code in the top-left corner like
 * the frame's sidebar buttons (LcarsButton). LcarsButton itself has no icon slot, hence this block.
 */
@Composable
private fun KkcLcarsNavBlock(
    route: String,
    selected: Boolean,
    code: String,
    color: Color,
    rightCap: Boolean,
    onClick: () -> Unit
) {
    val shape = if (rightCap) RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100) else RectangleShape
    val label = kkcLcarsLabel(route)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color)
            .clickable(onClick = onClick)
            .padding(end = if (rightCap) 12.dp else 0.dp)
    ) {
        Text(
            text = code,
            fontFamily = AntonioFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 2.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                kkcNavIcon(route, selected),
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontFamily = AntonioFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
                color = Color.Black,
                maxLines = 1
            )
        }
    }
}

/**
 * Standalone band for screens without the LCARS frame (login). A tan left cap stands in for the
 * frame's bottom-left elbow, then the destination blocks, all on black.
 */
@Composable
internal fun KkcLcarsNavBand(
    payload: KkcNavBarPayload,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    anim: KkcLcarsBlockAnim = KkcLcarsBlockAnim.None
) {
    // The band's own black backing + left cap: fades in as the glass bar leaves (before the blocks
    // power up) and fades out only after the last block has retracted.
    val count = kkcNavSlots(payload.destinations).size
    val bandAlpha = remember { Animatable(if (anim == KkcLcarsBlockAnim.Enter) 0f else 1f) }
    LaunchedEffect(anim) {
        when (anim) {
            KkcLcarsBlockAnim.None -> bandAlpha.snapTo(1f)
            KkcLcarsBlockAnim.Enter -> {
                bandAlpha.snapTo(0f)
                bandAlpha.animateTo(1f, tween(KKC_GLASS_SWAP_MS))
            }
            KkcLcarsBlockAnim.Exit -> {
                delay((kkcLcarsRetractTotalMs(count) - KKC_LCARS_BLOCK_MS).toLong())
                bandAlpha.animateTo(0f, tween(KKC_LCARS_BLOCK_MS))
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = bandAlpha.value }
            .background(Color.Black)
            .navigationBarsPadding()
            .padding(LCARS_GAP)
            .height(KKC_LCARS_NAV_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(LCARS_GAP)
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(LcarsTan, shape = RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100))
        )
        KkcLcarsNavButtons(payload, onNavigate, Modifier.weight(1f), anim)
    }
}
