package com.example.timecard.kkcnav

// Hours Tracker–only LCARS version of the mirrored KKC navbar. NOT a copy of KKCSheetTracker code:
// with the LCARS theme equipped there is no floating glass bar. The KKC destinations live in the
// LCARS frame's bottom band instead (TimesheetScreen passes KkcLcarsNavButtons to LcarsFrame's
// footer, expanded to KKC_LCARS_NAV_HEIGHT), and on the login screen — which has no frame — in a
// standalone band (KkcLcarsNavBand). Destinations, Calc-before-Hours order, badges and tap
// behavior still follow the KKC contract (kkcNavSlots / kkcBadgeCount / KkcReturnNavigation).

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/** The KKC destinations as LCARS blocks, filling the band they are placed in. */
@Composable
internal fun KkcLcarsNavButtons(
    payload: KkcNavBarPayload,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = kkcNavSlots(payload.destinations)
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(LCARS_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, route ->
            val selected = route == KkcNavBarContract.DEST_HOURS
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
        KkcLcarsNavButtons(payload, onNavigate, Modifier.weight(1f))
    }
}
