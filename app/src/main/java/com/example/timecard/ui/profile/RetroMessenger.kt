package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.timecard.data.model.ActivityEvent
import com.example.timecard.data.model.Employee
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95Modal
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Tabs
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.rememberRetroMotion
import java.util.Locale

/** Buddy list tabs, in order. */
internal enum class BuddyTab(val label: String) { Week("Week"), Month("Month"), Streak("Streak"), Coins("KK") }

/** Buddies sorted for [tab] (most first), the same orders as the leaderboard. */
internal fun buddyOrder(entries: List<LeaderboardEntry>, tab: BuddyTab): List<LeaderboardEntry> = when (tab) {
    BuddyTab.Week -> entries.sortedByDescending { it.weekHours }
    BuddyTab.Month -> entries.sortedByDescending { it.monthHours }
    BuddyTab.Streak -> entries.sortedByDescending { it.currentStreak }
    BuddyTab.Coins -> entries.sortedByDescending { it.allTimeCoins }
}

/** The value shown after a buddy's name for [tab]. */
internal fun buddyValue(e: LeaderboardEntry, tab: BuddyTab): String = when (tab) {
    BuddyTab.Week -> String.format(Locale.US, "%.1f hrs", e.weekHours)
    BuddyTab.Month -> String.format(Locale.US, "%.1f hrs", e.monthHours)
    BuddyTab.Streak -> "${e.currentStreak} days"
    BuddyTab.Coins -> "${e.allTimeCoins} KK"
}

/** "Online" = logged hours this week; everyone else sits in the Offline group. */
internal fun buddyOnline(e: LeaderboardEntry): Boolean = e.weekHours > 0.0

/** Chat-room line for a feed event: "Name: earned the badge X". */
internal fun feedLine(e: ActivityEvent): String = when (e.type) {
    "badge_earned" -> "earned the badge ${e.detail}"
    "streak_milestone" -> "hit a ${e.detail.trim().replace(Regex("""(\d+)\s*days?"""), "$1-day")} streak"
    "record_broken" -> "set a new record: ${e.detail}"
    "coins_earned" -> "earned ${e.detail}"
    else -> e.detail
}

private val BUDDY_COLORS = listOf(Color(0xFF000080), Color(0xFF800080), Color(0xFF008080), Color(0xFF808000), Color(0xFF800000), Color(0xFF008000), Color(0xFFC06000), Color(0xFF404040))

/**
 * Retro Rankings as "KKC Messenger" (2026-11-20 mockup): a buddy list with Week / Month / Streak / KK
 * tabs, Top Workers (logged this week) and Offline groups, Get Info on a buddy, and the Feed as a
 * separate chat-room window.
 */
@Composable
internal fun RetroMessenger(
    viewModel: LeaderboardViewModel,
    myId: String,
    myDisplayName: String,
    feedEmployeeNames: List<Employee>,
    onFeedTabSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    val motion = rememberRetroMotion()
    var tab by remember { mutableIntStateOf(0) }
    var feedOpen by remember { mutableStateOf(false) }
    var feedLoaded by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var offlineOpen by remember { mutableStateOf(false) }
    LaunchedEffect(feedOpen) { if (feedOpen && !feedLoaded) { feedLoaded = true; onFeedTabSelected() } }
    val buddyTab = BuddyTab.entries[tab]
    val ordered = buddyOrder(viewModel.entries, buddyTab)
    val online = ordered.filter(::buddyOnline)
    val offline = ordered.filterNot(::buddyOnline)

    Win95Desktop {
        Win95Window(
            "KKC Messenger", Modifier.width(360.dp).fillMaxHeight().zIndex(if (feedOpen) 1f else 2f),
            onClose = onDismiss, menus = listOf("My KKC", "People", "Help"), active = !feedOpen,
            icon = { Text("☺", style = w95Text(16.sp, W95.Yellow, FontWeight.Bold)) }
        ) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).height(64.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFCC00), Color(0xFFFF8C00)))).border(1.dp, W95.Shadow),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KKC", style = w95Text(30.sp, W95.Navy, FontWeight.Bold))
                    Column {
                        Text("Messenger", style = w95Text(16.sp, weight = FontWeight.Bold))
                        Text("Buddy List", style = w95Text(12.sp))
                    }
                }
            }
            Win95Tabs(BuddyTab.entries.map { it.label }, tab, { tab = it }, Modifier.padding(horizontal = 6.dp))
            Win95Sunken(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (viewModel.isLoading && viewModel.entries.isEmpty()) {
                        Text("Signing on...", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(8.dp))
                    }
                    GroupHeader("Top Workers (${online.size}/${ordered.size})", open = true) {}
                    online.forEachIndexed { i, e -> BuddyRow(e, i, buddyTab, e.id == myId, motion, rank = i + 1) { info = e } }
                    GroupHeader("Offline (${offline.size})", open = offlineOpen) { offlineOpen = !offlineOpen }
                    if (offlineOpen) offline.forEachIndexed { i, e -> BuddyRow(e, online.size + i, buddyTab, e.id == myId, motion, rank = null, dim = true) { info = e } }
                }
            }
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Win95Button("Info", { info = ordered.firstOrNull { it.id == myId } ?: ordered.firstOrNull() }, Modifier.weight(1f))
                Win95Button("Feed", { feedOpen = true }, Modifier.weight(1f), pressed = feedOpen)
                Win95Button("Close", onDismiss, Modifier.weight(1f))
            }
        }

        if (feedOpen) {
            Win95Window(
                "Feed - KKC Chat Room",
                Modifier.align(Alignment.TopEnd).offset(y = 110.dp).fillMaxWidth(0.62f).height(560.dp).zIndex(2f),
                onClose = { feedOpen = false }
            ) {
                Win95Sunken(Modifier.weight(1f).fillMaxWidth().padding(6.dp)) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("*** $myDisplayName has entered the room ***", style = w95Text(15.sp, W95.Green, FontWeight.Bold))
                        if (viewModel.isFeedLoading) Text("Loading chat...", style = w95Text(15.sp, W95.Shadow, italic = true))
                        viewModel.feedEvents.forEachIndexed { i, ev ->
                            Row {
                                Text("${ev.displayName}: ", style = w95Text(15.sp, BUDDY_COLORS[i % BUDDY_COLORS.size], FontWeight.Bold))
                                Text(feedLine(ev), style = w95Text(15.sp))
                            }
                        }
                        if (!viewModel.isFeedLoading && viewModel.feedEvents.isEmpty()) Text("Nobody has said anything yet.", style = w95Text(15.sp, W95.Shadow, italic = true))
                    }
                }
            }
        }

        Win95Modal(info != null, { info = null }) {
            val e = remember { info } ?: return@Win95Modal
            BuddyInfo(e) { info = null }
        }
    }
}

@Composable
private fun GroupHeader(text: String, open: Boolean, onClick: () -> Unit) {
    Text(
        (if (open) "▼ " else "▶ ") + text, style = w95Text(15.sp, weight = FontWeight.Bold),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

@Composable
private fun BuddyRow(e: LeaderboardEntry, index: Int, tab: BuddyTab, me: Boolean, animate: Boolean, rank: Int?, dim: Boolean = false, onClick: () -> Unit) {
    // Buddies "sign on" one by one (slide in), like a messenger door opening.
    val enter = remember(tab) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(tab, animate) { if (animate) { kotlinx.coroutines.delay(index * 60L); enter.animateTo(1f, tween(260)) } else enter.snapTo(1f) }
    val name = e.displayName ?: e.name
    Row(
        Modifier.fillMaxWidth().height(44.dp).graphicsLayer { alpha = enter.value; translationX = (1f - enter.value) * -40f }
            .clickable(role = Role.Button, onClickLabel = "Get info on $name", onClick = onClick).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; null -> ""; else -> "$rank" },
            style = w95Text(15.sp, weight = FontWeight.Bold), modifier = Modifier.width(26.dp)
        )
        BuddyPic(e, name, BUDDY_COLORS[index % BUDDY_COLORS.size], 30)
        Text(name, style = w95Text(15.sp, if (dim) W95.Shadow else W95.Black, if (me) FontWeight.Bold else FontWeight.Normal), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(buddyValue(e, tab), style = w95Text(14.sp, if (dim) W95.Shadow else W95.Black))
    }
}

@Composable
private fun BuddyPic(e: LeaderboardEntry, name: String, color: Color, sizeDp: Int) {
    val bmp = remember(e.avatarBytes) { e.avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Box(Modifier.size(sizeDp.dp).background(color).border(1.dp, W95.Black), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""), style = w95Text((sizeDp * 0.4f).sp, W95.White, FontWeight.Bold))
    }
}

/** Get Info window for a buddy: their records and badge count. */
@Composable
private fun BuddyInfo(e: LeaderboardEntry, onClose: () -> Unit) {
    val name = e.displayName ?: e.name
    val badges = BadgeEngine.ALL_BADGES.count { (e.badges[it.id] ?: 0) > 0 }
    Win95Window("Get Info - $name", Modifier.width(430.dp), onClose = onClose) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BuddyPic(e, name, W95.Navy, 72)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = w95Text(18.sp, weight = FontWeight.Bold))
                listOf(
                    "This week" to String.format(Locale.US, "%.1f hrs", e.weekHours),
                    "This month" to String.format(Locale.US, "%.1f hrs", e.monthHours),
                    "Daily streak" to "${e.currentStreak} (best ${e.bestDailyStreak})",
                    "Weekly streak" to "${e.currentWeeklyStreak} (best ${e.bestWeeklyStreak})",
                    "Best week" to String.format(Locale.US, "%.1f hrs", e.bestWeekHours),
                    "Best day" to String.format(Locale.US, "%.1f hrs", e.bestDayHours),
                    "Kustom Kash" to "${e.allTimeCoins} KK earned",
                    "Badges" to "$badges"
                ).forEach { (k, v) ->
                    Row { Text("$k:", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.width(120.dp)); Text(v, style = w95Text(14.sp)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            Win95Button("OK", onClose, Modifier.width(100.dp), bold = true)
        }
    }
}
