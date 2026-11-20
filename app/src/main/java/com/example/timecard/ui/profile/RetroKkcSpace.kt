package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95Modal
import com.example.timecard.ui.retro95.Win95StatusBar
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.RetroTickingCounter
import com.example.timecard.ui.theme.rememberRetroMotion
import java.util.Locale

private val KsBlue = Color(0xFF003399)
private val KsLight = Color(0xFF6699CC)
private val KsPage = Color(0xFFE5ECF9)
private val KsOrangeBg = Color(0xFFFFCC99)
private val KsOrange = Color(0xFFFF6600)
private val KsRow = Color(0xFFD5E8FB)
private val KsLink = Color(0xFF003399)

/** URL handle: the display name lowercased, letters and digits only ("winstonferguson"). */
internal fun kkcSpaceHandle(name: String): String = name.lowercase(Locale.US).filter { it.isLetterOrDigit() }.ifEmpty { "me" }

/** Mood line from the daily streak. */
internal fun kkcSpaceMood(dailyStreak: Int): String = if (dailyStreak > 0) "on fire 🔥" else "caffeinated ☕"

/** Auto "About me" blurb from the records. */
internal fun kkcSpaceAbout(bestWeek: Double, favoriteJob: String, coins: Int): String = buildString {
    append("I build cabinets at KKC. ")
    append(String.format(Locale.US, "My best week was %.1f hours. ", bestWeek))
    if (favoriteJob.isNotBlank()) append("Favorite job so far: $favoriteJob. ")
    append("$coins KK in the bank and it's going straight to the Shop.")
}

/** One "Friends Comments" entry: the app congratulating you on a badge. */
internal data class KkcSpaceComment(val badge: BadgeDefinition, val count: Int) {
    val text: String get() = "Congrats on ${badge.name}${if (count > 1) " (x$count)" else ""}! ${badge.description} +${badge.coinReward} KK"
}

internal fun kkcSpaceComments(badges: Map<String, Int>): List<KkcSpaceComment> =
    BadgeEngine.ALL_BADGES.mapNotNull { def -> (badges[def.id] ?: 0).takeIf { it > 0 }?.let { KkcSpaceComment(def, it) } }

/** This week's Top 8 coworkers (most hours first), without yourself. */
internal fun kkcSpaceTop8(entries: List<LeaderboardEntry>, myId: String?): List<LeaderboardEntry> =
    entries.filter { it.id != myId && it.weekHours > 0 }.sortedByDescending { it.weekHours }.take(8)

private val FRIEND_COLORS = listOf(Color(0xFF000080), Color(0xFF800080), Color(0xFF008080), Color(0xFF808000), Color(0xFF008000), Color(0xFFC06000), Color(0xFF404040), Color(0xFF800000))

/**
 * Retro profile & settings as "KKCspace", a mid-2000s social profile page in a browser window
 * (2026-11-20 mockup): name, photo, quote, Online Now!, mood, Contacting box, details, a profile song
 * player, blurbs, Friend Space (this week's Top 8), comments (your badges), and an Edit Profile window
 * for display name, photo and layout (theme).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RetroKkcSpace(
    profileViewModel: ProfileViewModel,
    actualName: String,
    friends: List<LeaderboardEntry>,
    myId: String?,
    onDismiss: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenShop: () -> Unit
) {
    val profile = profileViewModel.profile
    val name = profile.displayName ?: actualName
    val first = name.substringBefore(' ')
    val handle = kkcSpaceHandle(name)
    val motion = rememberRetroMotion()
    val avatar = rememberAvatarBitmap(profileViewModel)
    var editing by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    val blink by rememberInfiniteTransition(label = "online").animateFloat(1f, if (motion) 0f else 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "onlineA")
    val top8 = kkcSpaceTop8(friends, myId)

    Win95Desktop {
        Win95Window("KKCspace.com | $name - KKC Navigator", Modifier.fillMaxSize(), onClose = onDismiss, icon = { Text("🌐", style = w95Text(15.sp)) }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Location:", style = w95Text(14.sp))
                Win95Sunken(Modifier.weight(1f).height(30.dp)) {
                    Text("http://www.kkcspace.com/$handle", style = w95Text(14.sp), modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 6.dp))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(4.dp)) {
                Win95Sunken(Modifier.fillMaxSize(), color = KsPage) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        // Site header + nav
                        Row(Modifier.fillMaxWidth().background(KsBlue).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("KKCspace", style = w95Text(24.sp, W95.White, FontWeight.Bold))
                            Text("a place for coworkers", style = w95Text(12.sp, W95.White))
                            Spacer(Modifier.weight(1f))
                            Text("Help | Close", style = w95Text(12.sp, W95.White), modifier = Modifier.clickable(role = Role.Button, onClick = onDismiss))
                        }
                        Text(
                            "Home | Browse | Search | Mail | Blog | Favorites | Forum | Groups | Events | Music",
                            style = w95Text(12.sp, W95.White, FontWeight.Bold), modifier = Modifier.fillMaxWidth().background(KsLight).padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // ── Left column ──
                            Column(Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(name, style = w95Text(22.sp, weight = FontWeight.Bold))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.size(110.dp).background(W95.Navy).border(1.dp, W95.Black), contentAlignment = Alignment.Center) {
                                        if (avatar != null) Image(avatar, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        else Text(lcarsInitials(name), style = w95Text(40.sp, W95.White, FontWeight.Bold))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("\"one more sheet then lunch\"", style = w95Text(12.sp, italic = true))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Shop floor\nKustom Kraft\nOregon", style = w95Text(12.sp))
                                        Spacer(Modifier.height(6.dp))
                                        Text("● Online Now!", style = w95Text(13.sp, KsOrange, FontWeight.Bold), modifier = Modifier.graphicsLayer { alpha = if (blink > 0.5f) 1f else 0.25f })
                                    }
                                }
                                Text("Mood: ${kkcSpaceMood(profile.streaks.currentDaily)}", style = w95Text(12.sp))
                                KsBox("Contacting $first") {
                                    FlowRow(Modifier.padding(6.dp), maxItemsInEachRow = 2, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        KsAction("🛒 Visit Shop", Modifier.weight(1f)) { onDismiss(); onOpenShop() }
                                        KsAction("✉️ Past Alerts", Modifier.weight(1f)) { onDismiss(); onNavigateToAlerts() }
                                        KsAction("✏️ Edit Profile", Modifier.weight(1f)) { editing = true }
                                        KsAction("🎨 Pick Layout", Modifier.weight(1f)) { editing = true }
                                        KsAction("🚪 Close", Modifier.weight(1f), onDismiss)
                                    }
                                }
                                KsBox("KKCspace URL:") { Text("http://www.kkcspace.com/$handle", style = w95Text(12.sp), modifier = Modifier.padding(6.dp)) }
                                KsBox("$first's Details") {
                                    listOf(
                                        "Kustom Kash:" to "${profile.coins} KK",
                                        "Daily streak:" to "${profile.streaks.currentDaily} (best ${profile.streaks.bestDaily})",
                                        "Weekly streak:" to "${profile.streaks.currentWeekly} (best ${profile.streaks.bestWeekly})",
                                        "Best week:" to String.format(Locale.US, "%.1f hrs", profile.records.bestWeekHours),
                                        "Best day:" to String.format(Locale.US, "%.1f hrs", profile.records.busiestDay),
                                        "Favorite job:" to profile.records.favoriteJob.ifBlank { "-" }
                                    ).forEachIndexed { i, (k, v) ->
                                        Row(Modifier.fillMaxWidth().background(if (i % 2 == 0) KsRow else W95.White).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                            Text(k, style = w95Text(12.sp, Color(0xFF336699), FontWeight.Bold), modifier = Modifier.width(100.dp))
                                            Text(v, style = w95Text(12.sp))
                                        }
                                    }
                                }
                                if (profileViewModel.isBackfilling) Text("Loading your history... Kustom Kash is being calculated", style = w95Text(12.sp, KsOrange, italic = true))
                            }
                            // ── Right column ──
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.fillMaxWidth().background(W95.White).border(1.dp, KsLight).padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text("$first is in your extended network", style = w95Text(17.sp, weight = FontWeight.Bold), textAlign = TextAlign.Center)
                                }
                                SongPlayer(playing, motion) { playing = !playing }
                                KsBox("$first's Blurbs", orange = true) {
                                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("About me:", style = w95Text(12.sp, weight = FontWeight.Bold))
                                        Text(kkcSpaceAbout(profile.records.bestWeekHours, profile.records.favoriteJob, profile.coins), style = w95Text(12.sp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("Who I'd like to meet:", style = w95Text(12.sp, weight = FontWeight.Bold))
                                        Text(String.format(Locale.US, "Anyone who can beat my best week of %.1f hours.", profile.records.bestWeekHours), style = w95Text(12.sp))
                                    }
                                }
                                KsBox("$first's Friend Space", orange = true) {
                                    Text("$first has ${friends.size.coerceAtLeast(top8.size)} friends. (this week's Top 8)", style = w95Text(12.sp, weight = FontWeight.Bold), modifier = Modifier.padding(8.dp))
                                    if (top8.isEmpty()) Text("Nobody has logged hours this week yet.", style = w95Text(12.sp, W95.Shadow, italic = true), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                                    FlowRow(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), maxItemsInEachRow = 4, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        top8.forEachIndexed { i, f -> FriendTile(f, FRIEND_COLORS[i % FRIEND_COLORS.size], Modifier.weight(1f)) }
                                        repeat((4 - top8.size % 4) % 4) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                                val comments = kkcSpaceComments(profile.badges)
                                KsBox("$first's Friends Comments", orange = true) {
                                    Text("Displaying ${comments.size} of ${comments.size} comments", style = w95Text(12.sp), modifier = Modifier.padding(8.dp))
                                    comments.forEachIndexed { i, c ->
                                        Row(Modifier.fillMaxWidth().background(if (i % 2 == 0) W95.White else Color(0xFFFFF7EF)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Hours Tracker", style = w95Text(11.sp, KsLink).copy(textDecoration = TextDecoration.Underline), textAlign = TextAlign.Center)
                                                BadgeArtSmall(c.badge, profileViewModel.badgeImages[c.badge.id])
                                            }
                                            Text(c.text, style = w95Text(12.sp), modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (comments.isEmpty()) Text("No comments yet. Save a timecard to earn badges!", style = w95Text(12.sp, W95.Shadow, italic = true), modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Profile views: ", style = w95Text(13.sp, weight = FontWeight.Bold))
                            RetroTickingCounter(start = 1995)
                        }
                    }
                }
                Win95Modal(editing, { editing = false }) { EditProfile(profileViewModel, actualName) { editing = false } }
            }
            Win95StatusBar(listOf("Document: Done" to null, "${profile.coins} KK" to 110.dp))
        }
    }
}

@Composable
private fun KsBox(title: String, orange: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(W95.White).border(1.dp, if (orange) KsOrangeBg else KsLight)) {
        Text(
            title, style = w95Text(13.sp, if (orange) KsOrange else W95.White, FontWeight.Bold),
            modifier = Modifier.fillMaxWidth().background(if (orange) KsOrangeBg else KsLight).padding(horizontal = 6.dp, vertical = 3.dp)
        )
        content()
    }
}

@Composable
private fun KsAction(label: String, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label, style = w95Text(13.sp, KsLink).copy(textDecoration = TextDecoration.Underline), maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick).padding(vertical = 4.dp)
    )
}

/** Fake profile song player (no audio): play/pause, progress, bouncing equalizer while "playing". */
@Composable
private fun SongPlayer(playing: Boolean, motion: Boolean, onToggle: () -> Unit) {
    val t = rememberInfiniteTransition(label = "eq")
    val bars = listOf(420, 560, 380, 640, 500, 450).map { d ->
        t.animateFloat(0.2f, if (playing && motion) 1f else 0.2f, infiniteRepeatable(tween(d), RepeatMode.Reverse), label = "eq$d").value
    }
    Row(Modifier.fillMaxWidth().background(Color(0xFF222222)).border(2.dp, Color(0xFF555555)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(width = 36.dp, height = 30.dp).background(Color(0xFF444444)).border(1.dp, Color(0xFF888888)).clickable(role = Role.Button, onClickLabel = if (playing) "Pause" else "Play", onClick = onToggle), contentAlignment = Alignment.Center) {
            Text(if (playing) "❚❚" else "▶", style = w95Text(12.sp, W95.White))
        }
        Column(Modifier.weight(1f)) {
            Text("♫ Kustom Krew - Sawdust Anthem (1997 Remix)", style = w95Text(12.sp, Color(0xFF99FF99)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(4.dp).background(Color(0xFF555555))) { Box(Modifier.fillMaxHeight().fillMaxWidth(0.38f).background(Color(0xFF99FF99))) }
        }
        Row(Modifier.height(24.dp).width(44.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEach { Box(Modifier.weight(1f).fillMaxHeight(it).background(Color(0xFF99FF99))) }
        }
    }
}

@Composable
private fun FriendTile(f: LeaderboardEntry, color: Color, modifier: Modifier) {
    val name = (f.displayName ?: f.name).substringBefore(' ')
    val bmp = remember(f.avatarBytes) { f.avatarBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = w95Text(11.sp, KsLink, FontWeight.Bold).copy(textDecoration = TextDecoration.Underline), maxLines = 1)
        Box(Modifier.padding(vertical = 2.dp).size(64.dp).background(color).border(1.dp, W95.Black), contentAlignment = Alignment.Center) {
            if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(lcarsInitials(f.displayName ?: f.name), style = w95Text(22.sp, W95.White, FontWeight.Bold))
        }
        Text(String.format(Locale.US, "%.1f hrs", f.weekHours), style = w95Text(11.sp, Color(0xFF666666)))
    }
}

@Composable
private fun BadgeArtSmall(def: BadgeDefinition, image: ByteArray?) {
    val bmp = remember(image) { image?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Box(Modifier.padding(top = 2.dp).size(46.dp).background(KsOrangeBg), contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp, def.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        else Text(def.emoji, style = w95Text(24.sp))
    }
}

/** Edit Profile window: display name, photo, layout (theme). Same actions as the other profile layouts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfile(profileViewModel: ProfileViewModel, actualName: String, onClose: () -> Unit) {
    val profile = profileViewModel.profile
    val pickPhoto = rememberAvatarPicker(profileViewModel)
    var input by remember(profile.displayName) { mutableStateOf(profile.displayName ?: "") }
    Win95Window("Edit Profile", Modifier.width(520.dp), onClose = onClose) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Display name", style = w95Text(15.sp, weight = FontWeight.Bold))
            if (profile.inventory.contains("feature_display_name")) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Win95Sunken(Modifier.weight(1f).height(36.dp)) {
                        if (input.isEmpty()) Text(actualName, style = w95Text(16.sp, W95.Shadow), modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 6.dp))
                        BasicTextField(input, { input = it.take(40) }, singleLine = true, textStyle = w95Text(16.sp), cursorBrush = SolidColor(W95.Black), modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart).padding(horizontal = 6.dp))
                    }
                    Win95Button("Save", { profileViewModel.setDisplayName(input) }, enabled = input.trim() != (profile.displayName ?: "").trim(), bold = true)
                }
            } else Text("Unlock Display Name in the Shop.", style = w95Text(14.sp, W95.Shadow, italic = true))
            Text("Profile photo", style = w95Text(15.sp, weight = FontWeight.Bold))
            if (profile.inventory.contains("feature_custom_avatar")) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Win95Button("Change photo...", pickPhoto)
                    if (profile.avatar == "custom") Win95Button("Remove", { profileViewModel.clearCustomAvatar() })
                }
            } else Text("Unlock Custom Avatar in the Shop.", style = w95Text(14.sp, W95.Shadow, italic = true))
            Text("Layout (theme)", style = w95Text(15.sp, weight = FontWeight.Bold))
            val choices = themeChoices(profile.inventory)
            FlowRow(maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                choices.forEach { c ->
                    val on = profile.accentColor == c.key
                    Row(
                        Modifier.weight(1f).height(36.dp).background(if (on) Color(0xFFD4D0C8) else W95.Face)
                            .then(Modifier.border(if (on) 2.dp else 1.dp, if (on) W95.Black else W95.Shadow))
                            .clickable(role = Role.RadioButton, onClickLabel = "Use ${c.name}") { profileViewModel.setAccentColor(c.key) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(14.dp).background(c.swatch ?: Color(0xFF2F6FDE)).border(1.dp, W95.Black))
                        Text(c.name, style = w95Text(13.sp, weight = if (on) FontWeight.Bold else FontWeight.Normal), maxLines = 1)
                    }
                }
                repeat((3 - choices.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Win95Button("Close", onClose, Modifier.width(100.dp), bold = true) }
        }
    }
}
