package com.example.timecard.kkcnav

// Hours Tracker–only LCARS version of the mirrored KKC navbar. NOT a copy of KKCSheetTracker code:
// with the LCARS theme equipped there is no floating glass bar. The KKC destinations live in the
// LCARS frame's bottom band instead (TimesheetScreen passes KkcLcarsNavButtons to LcarsFrame's
// footer, expanded to KKC_LCARS_NAV_HEIGHT), and on the login screen — which has no frame — in a
// standalone band (KkcLcarsNavBand). Destinations, Calc-before-Hours order, badges and tap
// behavior still follow the KKC contract (kkcNavSlots / kkcBadgeCount / KkcReturnNavigation).

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.example.timecard.ui.theme.LcarsButton
import com.example.timecard.ui.theme.LcarsButtonShape
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
                LcarsButton(
                    onClick = { onNavigate(route) },
                    label = kkcLcarsLabel(route),
                    code = (index + 1).toString().padStart(2, '0'),
                    color = kkcLcarsBlockColor(index, selected),
                    shapeType = if (index == slots.lastIndex) LcarsButtonShape.RightCap else LcarsButtonShape.Rect,
                    modifier = Modifier.fillMaxSize()
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
