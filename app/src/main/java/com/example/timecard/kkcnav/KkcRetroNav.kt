package com.example.timecard.kkcnav

// Hours Tracker–only Retro version of the mirrored KKC navbar (no KKC counterpart; nothing here
// needs to stay in sync with KKC). With the Retro theme equipped there is no floating glass bar:
// the KKC destinations become a Windows-95 taskbar — a spinning-globe Start button, one task
// button per destination (Hours is pressed in), a badge on buttons that have one, and a ticking
// tray clock. TimesheetScreen puts it under RetroFrame's status bar; the login screen (no frame)
// gets it standalone from TimecardApp. Destinations, Calc-before-Hours order, badges and tap
// behavior still follow the KKC contract (kkcNavSlots / kkcBadgeCount / KkcReturnNavigation).

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.RetroBackground
import com.example.timecard.ui.theme.RetroGlobe
import com.example.timecard.ui.theme.RetroInput
import com.example.timecard.ui.theme.RetroIcon
import com.example.timecard.ui.theme.RetroPixelIcon
import com.example.timecard.ui.theme.RetroTextHeading
import com.example.timecard.ui.theme.RetroTextPrimary
import com.example.timecard.ui.theme.VT323FontFamily
import com.example.timecard.ui.theme.beveledBorder
import com.example.timecard.ui.theme.doubleBeveledBorder
import com.example.timecard.ui.theme.rememberRetroBlink
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RetroBadgeRed = Color(0xFFE02020)

/** Candy-bright icon colors, one per destination, so the taskbar reads like a 90s desktop. */
private val RetroIconColors = mapOf(
    "dashboard" to Color(0xFFFF2DAA),
    "jobs" to Color(0xFFFF7A00),
    "search" to Color(0xFF00A8F0),
    KkcNavBarContract.DEST_CALCULATOR to Color(0xFF00B050),
    "hours" to Color(0xFF7A3CF0),
    "timecard" to Color(0xFFE02020),
    "supply" to Color(0xFF1A1AE6),
    "settings" to Color(0xFF808080),
    "standards" to Color(0xFFC08000)
)

internal fun kkcRetroIconColor(route: String): Color = RetroIconColors[route] ?: Color(0xFF1A1AE6)

/** "Wed 11/4" style date shown under the tray clock. */
internal fun kkcRetroDateText(nowMillis: Long): String =
    SimpleDateFormat("EEE M/d", Locale.US).format(Date(nowMillis))

/** "1:23 PM" style tray clock text. */
internal fun kkcRetroClockText(nowMillis: Long, colonVisible: Boolean = true): String {
    val text = SimpleDateFormat("h:mm a", Locale.US).format(Date(nowMillis))
    return if (colonVisible) text else text.replace(':', ' ')
}

@Composable
internal fun KkcRetroNavBar(
    payload: KkcNavBarPayload,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = kkcNavSlots(payload.destinations)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RetroBackground)
            .navigationBarsPadding()
            .height(64.dp)
            .beveledBorder(isInset = false, borderWidth = 2.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start button: spinning globe + KKC
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .background(RetroBackground)
                .doubleBeveledBorder(isInset = false)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroGlobe(modifier = Modifier.padding(end = 6.dp), fontSize = 26.sp)
            Text(
                text = "KKC",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = RetroTextHeading
            )
        }

        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .beveledBorder(isInset = true, borderWidth = 1.dp)
        )

        slots.forEach { route ->
            val selected = route == KkcNavBarContract.DEST_HOURS
            val badgeCount = kkcBadgeCount(route, payload)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (selected) RetroInput else RetroBackground)
                        .doubleBeveledBorder(isInset = selected)
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // The KKC destination's glyph, redrawn as chunky 16-bit pixel art.
                    val pixelIcon = RetroIcon.forKkcRoute(route)
                    if (pixelIcon != null) {
                        RetroPixelIcon(
                            icon = pixelIcon,
                            size = 34.dp,
                            bounce = selected,
                            modifier = Modifier.semantics { contentDescription = kkcNavLabel(route) }
                        )
                    }
                    Text(
                        text = kkcNavLabel(route),
                        fontFamily = VT323FontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (selected) kkcRetroIconColor(route) else RetroTextPrimary,
                        maxLines = 1
                    )
                }
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .background(RetroBadgeRed)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = badgeCount.toString(),
                            fontFamily = VT323FontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // System tray: sunken clock with a blinking colon
        var now by remember { mutableStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis()
                delay(15_000)
            }
        }
        val colonVisible = rememberRetroBlink(1000)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(RetroBackground)
                .beveledBorder(isInset = true, borderWidth = 1.dp)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = kkcRetroClockText(now, colonVisible),
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = RetroTextPrimary,
                maxLines = 1
            )
            Text(
                text = kkcRetroDateText(now),
                fontFamily = VT323FontFamily,
                fontSize = 15.sp,
                color = RetroTextHeading,
                maxLines = 1
            )
        }
    }
}
