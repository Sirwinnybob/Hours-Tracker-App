package com.example.timecard.ui.profile

import androidx.compose.foundation.layout.imePadding
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.domain.BadgeEngine
import com.example.timecard.ui.common.CoinIcon
import com.example.timecard.ui.common.FunIcons
import com.example.timecard.ui.timesheet.lcars.LcarsClassic
import com.example.timecard.ui.timesheet.lcars.LcarsPill
import com.example.timecard.ui.timesheet.lcars.LcarsScreenHeader
import com.example.timecard.ui.timesheet.lcars.LcarsTextBar
import com.example.timecard.ui.timesheet.lcars.LocalLcarsAnimate
import com.example.timecard.ui.timesheet.lcars.lcarsSlideIn
import com.example.timecard.ui.timesheet.lcars.lcarsText
import java.util.Locale

/** Up to two initials for the photo block when there is no custom photo. */
internal fun lcarsInitials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/** One label/value/sub readout in the summary grid. */
internal data class LcarsReadout(val label: String, val value: String, val sub: String, val bar: Color)

/** Summary grid: KK, streaks with bests, best week / day, favorite job (each figure once). */
internal fun lcarsProfileReadouts(
    coins: Int, daily: Int, bestDaily: Int, weekly: Int, bestWeekly: Int,
    bestWeek: Double, bestDay: Double, favoriteJob: String
): List<LcarsReadout> = listOf(
    LcarsReadout("KUSTOM KASH", "$coins", "KK", LcarsClassic.Orange),
    LcarsReadout("DAILY STREAK", "$daily", "BEST $bestDaily ${if (bestDaily == 1) "DAY" else "DAYS"}", LcarsClassic.Butter),
    LcarsReadout("WEEKLY STREAK", "$weekly", "BEST $bestWeekly ${if (bestWeekly == 1) "WEEK" else "WEEKS"}", LcarsClassic.Violet),
    LcarsReadout("BEST WEEK", String.format(Locale.US, "%.1f", bestWeek), "HOURS", LcarsClassic.Bluey),
    LcarsReadout("BEST DAY", String.format(Locale.US, "%.1f", bestDay), "HOURS", LcarsClassic.Mauve),
    LcarsReadout("FAVORITE JOB", favoriteJob.ifBlank { "—" }.uppercase(), "MOST HOURS", LcarsClassic.Barley)
)

/**
 * Profile & settings in the LCARS Classic look (2026-11-19 mockup): readouts instead of boxes, then
 * PROFILE (photo block, display name), THEME (owned themes as swatch pills, Match KKC first) and BADGES
 * (fun icons in black on round LCARS blocks) text-bar sections, with VISIT SHOP / PAST ALERTS pills.
 * Same actions as the standard layout (StandardSettings.kt); photo plumbing is shared (AvatarPicker.kt).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun LcarsSettingsModal(
    profileViewModel: ProfileViewModel,
    actualName: String,
    onDismiss: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenShop: () -> Unit
) {
    val profile = profileViewModel.profile
    val name = profile.displayName ?: actualName
    var selectedBadge by remember { mutableStateOf<BadgeDefinition?>(null) }
    val earnedBadges = BadgeEngine.ALL_BADGES.filter { (profile.badges[it.id] ?: 0) > 0 }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LcarsScreenHeader("Profile & settings", onClose = onDismiss, color = LcarsClassic.Mauve)
            if (profileViewModel.isLockedByAnotherUser) {
                Locked()
                return@Column
            }
            Column(Modifier.weight(1f).imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (profileViewModel.isBackfilling) Backfilling()
                ReadoutGrid(
                    lcarsProfileReadouts(
                        profile.coins, profile.streaks.currentDaily, profile.streaks.bestDaily,
                        profile.streaks.currentWeekly, profile.streaks.bestWeekly,
                        profile.records.bestWeekHours, profile.records.busiestDay, profile.records.favoriteJob
                    ),
                    Modifier.lcarsSlideIn(80)
                )
                LcarsTextBar("Profile", LcarsClassic.Mauve, LcarsClassic.Mauve, Modifier.lcarsSlideIn(140))
                ProfileSection(profileViewModel, name, actualName, onOpenShop, Modifier.lcarsSlideIn(180))
                val choices = themeChoices(profile.inventory)
                val current = choices.firstOrNull { it.key == profile.accentColor }?.name ?: profile.accentColor?.let { THEME_NAMES[it] } ?: "Match KKC"
                LcarsTextBar("Theme · $current", LcarsClassic.Bluey, LcarsClassic.Bluey, Modifier.lcarsSlideIn(220))
                ThemeGrid(choices, profile.accentColor, onPick = { profileViewModel.setAccentColor(it) }, onOpenShop = onOpenShop)
                LcarsTextBar(
                    if (earnedBadges.isEmpty()) "Badges" else "Badges · ${earnedBadges.sumOf { profile.badges[it.id] ?: 0 }} earned",
                    LcarsClassic.Barley, LcarsClassic.Barley, Modifier.lcarsSlideIn(300)
                )
                if (earnedBadges.isEmpty()) {
                    Text("SAVE YOUR FIRST TIMECARD TO START EARNING BADGES", style = lcarsText(16.sp, LcarsClassic.Lilac))
                } else {
                    BadgeGrid(earnedBadges, profile.badges, profileViewModel.badgeImages) { selectedBadge = it }
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(Modifier.fillMaxWidth().lcarsSlideIn(360), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LcarsPill("06-Visit shop", LcarsClassic.Barley, onOpenShop, Modifier.weight(1f), shape = RoundedCornerShape(topStart = 27.dp, bottomStart = 27.dp))
                LcarsPill("Past alerts", LcarsClassic.Violet, onNavigateToAlerts, Modifier.weight(1f), shape = RoundedCornerShape(topEnd = 27.dp, bottomEnd = 27.dp))
            }
        }

        val latestBadge = com.example.timecard.ui.common.rememberLatestNonNull(selectedBadge)
        AnimatedVisibility(selectedBadge != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            val shown = latestBadge ?: return@AnimatedVisibility
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.7f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedBadge = null }
                )
                BadgeDetail(
                    shown, profileViewModel.badgeImages[shown.id], profile.badges[shown.id] ?: 1,
                    onClose = { selectedBadge = null },
                    modifier = Modifier.align(Alignment.Center).animateEnterExit(enter = scaleIn(initialScale = 0.92f))
                )
            }
        }
    }
}

@Composable
private fun Locked() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LcarsTextBar("Profile locked", LcarsClassic.Red, LcarsClassic.Red)
        Text("YOUR PROFILE IS OPEN ON ANOTHER TABLET. CLOSE IT THERE, THEN TRY AGAIN.", style = lcarsText(18.sp, LcarsClassic.Creme, FontWeight.Normal))
    }
}

@Composable
private fun Backfilling() {
    val animate = LocalLcarsAnimate.current
    val alpha = if (animate) {
        rememberInfiniteTransition(label = "backfill").animateFloat(1f, 0.25f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "backfillA").value
    } else 1f
    Text(
        "LOADING HISTORY · KUSTOM KASH BEING CALCULATED",
        style = lcarsText(16.sp, LcarsClassic.Orange),
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    )
}

@Composable
private fun ReadoutGrid(readouts: List<LcarsReadout>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        readouts.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { r ->
                    Row(Modifier.weight(1f).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(8.dp).fillMaxHeight().background(r.bar))
                        Column {
                            Text(r.label, style = lcarsText(13.sp, LcarsClassic.Bluey))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (r.label == "KUSTOM KASH") CoinIcon(size = 30.dp)
                                Text(
                                    r.value,
                                    style = lcarsText(36.sp, if (r.label == "KUSTOM KASH") LcarsClassic.Orange else LcarsClassic.Creme, FontWeight.Normal).copy(lineHeight = 38.sp),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(r.sub, style = lcarsText(12.sp, LcarsClassic.DimText))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(profileViewModel: ProfileViewModel, name: String, actualName: String, onOpenShop: () -> Unit, modifier: Modifier) {
    val profile = profileViewModel.profile
    val avatar = rememberAvatarBitmap(profileViewModel)
    val pickPhoto = rememberAvatarPicker(profileViewModel)
    val hasPhoto = profile.inventory.contains("feature_custom_avatar")
    val hasName = profile.inventory.contains("feature_display_name")
    var input by remember(profile.displayName) { mutableStateOf(profile.displayName ?: "") }
    Row(modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.width(120.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 60.dp, bottomStart = 60.dp)).background(LcarsClassic.Butter),
            contentAlignment = Alignment.Center
        ) {
            if (avatar != null) Image(avatar, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Text(lcarsInitials(name), style = lcarsText(46.sp, Color.Black, FontWeight.Normal))
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DISPLAY NAME", style = lcarsText(13.sp, LcarsClassic.Bluey))
                if (hasName) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.BottomStart) {
                            if (input.isEmpty()) Text(actualName.uppercase(), style = lcarsText(28.sp, LcarsClassic.Muted, FontWeight.Normal), modifier = Modifier.padding(bottom = 4.dp))
                            BasicTextField(
                                value = input,
                                onValueChange = { input = it.take(40) },
                                singleLine = true,
                                textStyle = lcarsText(28.sp, LcarsClassic.Creme, FontWeight.Normal),
                                cursorBrush = SolidColor(LcarsClassic.Orange),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            )
                            Box(Modifier.fillMaxWidth().height(3.dp).background(LcarsClassic.Mauve))
                        }
                        val changed = input.trim() != (profile.displayName ?: "").trim()
                        LcarsPill(
                            if (changed) "Save" else "Saved", LcarsClassic.Orange, { profileViewModel.setDisplayName(input) },
                            Modifier.width(110.dp), height = 44.dp, shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp), enabled = changed
                        )
                    }
                } else {
                    Text("UNLOCK DISPLAY NAME IN THE SHOP", style = lcarsText(18.sp, LcarsClassic.Lilac, FontWeight.Normal))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasPhoto) {
                    val canRemove = profile.avatar == "custom"
                    LcarsPill(
                        "Change photo", LcarsClassic.Butter, pickPhoto, Modifier.weight(1f), height = 44.dp,
                        shape = if (canRemove) RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp) else RoundedCornerShape(50)
                    )
                    if (canRemove) {
                        LcarsPill(
                            "Remove", LcarsClassic.Dim, { profileViewModel.clearCustomAvatar() }, Modifier.weight(1f), height = 44.dp,
                            shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp), textColor = LcarsClassic.Violet
                        )
                    }
                } else {
                    LcarsPill("Unlock photo · shop", LcarsClassic.Barley, onOpenShop, Modifier.weight(1f), height = 44.dp)
                }
            }
        }
    }
}

@Composable
private fun ThemeGrid(choices: List<ThemeChoice>, selectedKey: String?, onPick: (String?) -> Unit, onOpenShop: () -> Unit) {
    val cells: List<ThemeChoice?> = choices + null // last cell: more themes in the shop
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cells.chunked(3).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { c, choice ->
                    val i = r * 3 + c
                    val cellMod = Modifier.weight(1f).height(54.dp).lcarsSlideIn(240 + i * 40)
                    if (choice == null) {
                        Box(
                            cellMod.clip(RoundedCornerShape(27.dp)).border(2.dp, LcarsClassic.Dim, RoundedCornerShape(27.dp))
                                .clickable(role = Role.Button, onClickLabel = "Open shop", onClick = onOpenShop)
                        ) {
                            Text("MORE IN SHOP", style = lcarsText(15.sp, LcarsClassic.DimText), modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp))
                        }
                    } else {
                        val selected = choice.key == selectedKey
                        Row(
                            cellMod.clickable(role = Role.RadioButton, onClickLabel = "Use ${choice.name}") { onPick(choice.key) },
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(Modifier.width(34.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 27.dp, bottomStart = 27.dp)).background(choice.swatch ?: LcarsClassic.Lilac))
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 27.dp, bottomEnd = 27.dp))
                                    .background(if (selected) LcarsClassic.Creme else LcarsClassic.RowColors[i % LcarsClassic.RowColors.size])
                            ) {
                                Text(
                                    if (selected) "IN USE" else if (choice.key == null) "FOLLOWS KKC" else "OWNED",
                                    style = lcarsText(11.sp, Color.Black), modifier = Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 5.dp)
                                )
                                Text(
                                    choice.name.uppercase(), style = lcarsText(15.sp, Color.Black), maxLines = 1,
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 6.dp)
                                )
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BadgeGrid(badges: List<BadgeDefinition>, counts: Map<String, Int>, images: Map<String, ByteArray>, onOpen: (BadgeDefinition) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        badges.chunked(6).forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { c, def ->
                    val i = r * 6 + c
                    Column(
                        Modifier.weight(1f).lcarsSlideIn(320 + i * 50).clickable(role = Role.Button, onClickLabel = "Badge details") { onOpen(def) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box {
                            LcarsBadgeArt(def, images[def.id], LcarsClassic.RowColors[i % LcarsClassic.RowColors.size], 70)
                            val count = counts[def.id] ?: 1
                            if (count > 1) {
                                Box(
                                    Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 2.dp).height(22.dp).clip(RoundedCornerShape(11.dp))
                                        .background(Color.Black).padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text("x$count", style = lcarsText(13.sp, LcarsClassic.Creme)) }
                            }
                        }
                        Text(def.name.uppercase(), style = lcarsText(12.sp, LcarsClassic.Violet), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Admin-uploaded badge image if there is one, else the fun icon in black on a round LCARS block. */
@Composable
private fun LcarsBadgeArt(def: BadgeDefinition, image: ByteArray?, color: Color, sizeDp: Int) {
    val bitmap = remember(image) { image?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, contentDescription = def.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(FunIcons.badge(def.id).vector, contentDescription = def.name, tint = Color.Black, modifier = Modifier.size((sizeDp * 0.5f).dp))
    }
}

@Composable
private fun BadgeDetail(badge: BadgeDefinition, image: ByteArray?, count: Int, onClose: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    Column(
        modifier.fillMaxWidth().background(Color.Black, shape).border(2.dp, LcarsClassic.Barley, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LcarsTextBar(if (count > 1) "Badge · x$count" else "Badge", LcarsClassic.Barley, LcarsClassic.Barley)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LcarsBadgeArt(badge, image, LcarsClassic.Barley, 96)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(badge.name.uppercase(), style = lcarsText(30.sp, LcarsClassic.Creme, FontWeight.Normal))
                Text(badge.description.uppercase(), style = lcarsText(16.sp, LcarsClassic.Violet, FontWeight.Normal))
                if (badge.flavorText.isNotBlank()) Text("\"${badge.flavorText.uppercase()}\"", style = lcarsText(14.sp, LcarsClassic.Lilac, FontWeight.Normal))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoinIcon(size = 26.dp)
            Text("+${badge.coinReward} KK${if (badge.repeatable) " EACH TIME" else ""}", style = lcarsText(20.sp, LcarsClassic.Orange))
            Spacer(Modifier.weight(1f))
            LcarsPill("Close", LcarsClassic.Red, onClose, Modifier.width(120.dp), height = 44.dp)
        }
    }
}
