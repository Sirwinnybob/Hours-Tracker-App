package com.example.timecard.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.timecard.ui.common.FunIconTile
import com.example.timecard.ui.common.FunIcons
import com.example.timecard.ui.components.InitialsAvatar
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.ThemeMode
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.ui.timesheet.standard.HoursIcons
import com.example.timecard.ui.timesheet.standard.PillButton
import com.example.timecard.ui.timesheet.standard.PillKind
import com.example.timecard.ui.timesheet.standard.SectionCard
import com.example.timecard.ui.timesheet.standard.SectionTitle
import com.example.timecard.ui.timesheet.standard.StandardDialog
import com.example.timecard.ui.timesheet.standard.StandardLook
import java.util.Locale

/** Display names for the theme keys stored in profile.accentColor. */
internal val THEME_NAMES = mapOf(
    "orange" to "Sunrise", "purple" to "Twilight", "teal" to "Isle", "gold" to "Daybreak", "red" to "Ruby",
    "hacker" to "Terminal", "sunset" to "Sunset", "midnight" to "Midnight", "ocean" to "Ocean",
    "lcars" to "LCARS", "starwars" to "Galaxy", "retro" to "Retro"
)

/** One theme choice in Settings: null key = Match KKC (no custom theme). */
internal data class ThemeChoice(val key: String?, val name: String, val swatch: Color?)

/** Match KKC first, then every theme the employee owns (inventory `accent_*`), in shop order. */
internal fun themeChoices(inventory: List<String>): List<ThemeChoice> =
    listOf(ThemeChoice(null, "Match KKC", null)) +
        ACCENT_UNLOCKS.filter { (_, _, itemId) -> inventory.contains(itemId) }
            .map { (key, color, _) -> ThemeChoice(key, THEME_NAMES[key] ?: key, color) }

/** Profile & settings in the KKC look (standard theme). LCARS and Retro keep SettingsModal's own layouts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StandardSettingsModal(
    profileViewModel: ProfileViewModel,
    actualName: String,
    themeState: ThemeState?,
    onDismiss: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenShop: () -> Unit
) {
    val profile = profileViewModel.profile
    val name = profile.displayName ?: actualName
    var displayNameInput by remember(profile.displayName) { mutableStateOf(profile.displayName ?: "") }
    var selectedBadge by remember { mutableStateOf<BadgeDefinition?>(null) }
    val earnedBadges = BadgeEngine.ALL_BADGES.filter { (profile.badges[it.id] ?: 0) > 0 }
    val avatarBitmap = rememberAvatarBitmap(profileViewModel)
    val pickPhoto = rememberAvatarPicker(profileViewModel)

    StandardDialog(
        title = name,
        subtitle = "Profile & settings",
        onDismiss = onDismiss,
        fullHeight = true,
        leading = { AvatarSquare(avatarBitmap, name, 56) },
        footer = {
            PillButton("Visit Shop", currentLookFooter(), onOpenShop, Modifier.weight(1f), kind = PillKind.Tonal, icon = HoursIcons.Shop, height = 52.dp, fontSize = 16)
            PillButton("Past alerts", currentLookFooter(), onNavigateToAlerts, Modifier.weight(1f), kind = PillKind.Outlined, icon = HoursIcons.Bell, height = 52.dp, fontSize = 16)
        }
    ) { look ->
        if (profileViewModel.isLockedByAnotherUser) {
            SectionCard(look) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(HoursIcons.Lock, null, tint = look.warn, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Profile locked", color = look.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Your profile is open on another tablet. Close it there, then try again.", color = look.text2, fontSize = 14.sp)
                    }
                }
            }
            return@StandardDialog
        }
        if (profileViewModel.isBackfilling) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(look.soft).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(color = look.softText, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Text("Loading your history… Kustom Kash is being calculated", color = look.softText, fontSize = 14.sp)
            }
        }

        SummaryBand(look, profile.coins, profile.streaks.currentDaily, profile.streaks.bestDaily,
            profile.streaks.currentWeekly, profile.streaks.bestWeekly,
            profile.records.bestWeekHours, profile.records.busiestDay, profile.records.favoriteJob)

        // ── Profile ─────────────────────────────────────────────────────────────
        SectionCard(look) {
            SectionTitle(look, "Profile")
            val hasPhoto = profile.inventory.contains("feature_custom_avatar")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AvatarSquare(avatarBitmap, name, 64)
                Column(Modifier.weight(1f)) {
                    Text("Profile photo", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(if (hasPhoto) "Shows on your header and the leaderboard" else "Unlock Custom Avatar in the Shop", color = look.text2, fontSize = 13.sp)
                }
                if (hasPhoto) {
                    if (profile.avatar == "custom") {
                        PillButton("Remove", look, { profileViewModel.clearCustomAvatar() }, kind = PillKind.Outlined, height = 44.dp)
                    }
                    PillButton("Change photo", look, pickPhoto, kind = PillKind.Tonal, icon = HoursIcons.Camera, height = 44.dp)
                } else {
                    PillButton("Shop", look, onOpenShop, kind = PillKind.Tonal, icon = HoursIcons.Shop, height = 44.dp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(look.line))
            Text("Display name", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (profile.inventory.contains("feature_display_name")) {
                Text("Shown in the app instead of your actual name. Your login ID never changes.", color = look.text2, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, look.border, RoundedCornerShape(12.dp))
                            .background(look.surface).padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (displayNameInput.isEmpty()) Text(actualName, color = look.empty, fontSize = 17.sp)
                        BasicTextField(
                            value = displayNameInput,
                            onValueChange = { displayNameInput = it.take(40) },
                            singleLine = true,
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = look.text, fontSize = 17.sp),
                            cursorBrush = SolidColor(look.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    PillButton("Save", look, { profileViewModel.setDisplayName(displayNameInput) }, kind = PillKind.Filled, height = 48.dp, fontSize = 15,
                        enabled = displayNameInput.trim() != (profile.displayName ?: "").trim())
                }
            } else {
                Text("Unlock Display Name in the Shop to pick the name the app shows.", color = look.text2, fontSize = 13.sp)
            }
        }

        // ── Theme ───────────────────────────────────────────────────────────────
        SectionCard(look) {
            SectionTitle(look, "Theme", "tap to apply") {
                if (themeState != null) BrightnessSwitch(look, themeState)
            }
            val choices = themeChoices(profile.inventory)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
                choices.forEach { choice ->
                    val selected = profile.accentColor == choice.key
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) look.today else look.surface)
                            .border(if (selected) 2.dp else 1.dp, if (selected) look.primary else look.line, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton, onClickLabel = "Use ${choice.name}") { profileViewModel.setAccentColor(choice.key) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                                .background(if (choice.swatch != null) SolidColor(choice.swatch) else look.weekBar)
                        )
                        Text(choice.name, color = look.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat((4 - choices.size % 4) % 4) { Box(Modifier.weight(1f)) }
            }
            Text("Match KKC = no custom theme: Hours uses the same colors as KKCSheetTracker. Buy more themes in the Shop.", color = look.text2, fontSize = 13.sp)
        }

        // ── Badges ──────────────────────────────────────────────────────────────
        SectionCard(look) {
            SectionTitle(look, "Badges", if (earnedBadges.isEmpty()) null else "${earnedBadges.sumOf { profile.badges[it.id] ?: 0 }} earned")
            if (earnedBadges.isEmpty()) {
                Text("Save your first timecard to start earning badges.", color = look.text2, fontSize = 14.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
                    earnedBadges.forEach { def ->
                        BadgeTile(look, def, profile.badges[def.id] ?: 1, profileViewModel.badgeImages[def.id], Modifier.weight(1f)) { selectedBadge = def }
                    }
                    repeat((4 - earnedBadges.size % 4) % 4) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        StandardDialog(title = badge.name, onDismiss = { selectedBadge = null }, maxWidth = 460.dp) { look ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BadgeArt(look, badge, profileViewModel.badgeImages[badge.id], 88)
                Text(badge.description, color = look.text, fontSize = 16.sp, textAlign = TextAlign.Center)
                if (badge.flavorText.isNotBlank()) Text("“${badge.flavorText}”", color = look.text2, fontSize = 14.sp, textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CoinIcon(size = 20.dp)
                    Text("+${badge.coinReward} KK${if (badge.repeatable) " each time" else ""}", color = look.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun currentLookFooter(): StandardLook = com.example.timecard.ui.timesheet.standard.currentStandardLook()

@Composable
private fun AvatarSquare(bitmap: androidx.compose.ui.graphics.ImageBitmap?, name: String, sizeDp: Int) {
    val look = com.example.timecard.ui.timesheet.standard.currentStandardLook()
    Box(Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp / 4).dp)).background(look.soft), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            InitialsAvatar(name = name, size = sizeDp.dp, fontSize = (sizeDp * 0.36f).sp, bgColor = look.soft)
        }
    }
}

/** Coins, streaks and records in one gradient band (each figure once). Keeps the original KK coin. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryBand(
    look: StandardLook,
    coins: Int,
    daily: Int, bestDaily: Int,
    weekly: Int, bestWeekly: Int,
    bestWeek: Double, bestDay: Double,
    favoriteJob: String
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(look.weekBar).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoinIcon(size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text("$coins", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Kustom Kash", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (favoriteJob.isNotBlank()) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Favorite job", color = look.onSegSelected, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(favoriteJob, color = look.onSegSelected, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BandPill(HoursIcons.Flame, "Daily streak $daily · best $bestDaily", Color(0xFFB23C00))
            BandPill(HoursIcons.Calendar, "Weekly streak $weekly · best $bestWeekly", Color(0xFF154B8A))
            BandPill(HoursIcons.Stats, "Best week ${fmt1(bestWeek)} h · best day ${fmt1(bestDay)} h", Color(0xFF1F5E36))
        }
    }
}

private fun fmt1(v: Double) = String.format(Locale.US, "%.1f", v)

@Composable
private fun BandPill(icon: ImageVector, text: String, color: Color) {
    Row(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Light / Dark / OLED segmented control driving ThemeState (same effects as the header toggle). */
@Composable
private fun BrightnessSwitch(look: StandardLook, themeState: ThemeState) {
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(look.primary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(ThemeMode.Light to "Light", ThemeMode.Dark to "Dark", ThemeMode.Oled to "OLED").forEach { (mode, label) ->
            val selected = themeState.mode == mode
            Box(
                Modifier.height(38.dp).clip(RoundedCornerShape(8.dp)).background(if (selected) look.segSelected else Color.Transparent)
                    .clickable(enabled = !selected, role = Role.Tab) { setMode(themeState, mode) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (selected) look.onSegSelected else look.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** ThemeState only exposes toggle/cycle; reach [target] with those. */
internal fun setMode(state: ThemeState, target: ThemeMode) {
    if (state.mode == target) return
    when (target) {
        ThemeMode.Light -> state.toggleTheme()                       // Dark→Light, Oled→Light
        ThemeMode.Dark -> if (state.mode == ThemeMode.Light) state.toggleTheme() else state.cycleDarkVariant() // Oled→Dark
        ThemeMode.Oled -> state.cycleDarkVariant()                   // Dark→Oled, Light→Oled
    }
}

@Composable
private fun BadgeTile(look: StandardLook, def: BadgeDefinition, count: Int, image: ByteArray?, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(look.surface)
            .border(1.dp, look.line, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Badge details", onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BadgeArt(look, def, image, 48)
        Column(Modifier.weight(1f)) {
            Text(def.name, color = look.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (count > 1) Text("×$count", color = look.text2, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Admin-uploaded badge image if there is one, else the badge's fun icon. Never the emoji. */
@Composable
internal fun BadgeArt(look: StandardLook, def: BadgeDefinition, image: ByteArray?, sizeDp: Int) {
    val bitmap = remember(image) { image?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = def.name, contentScale = ContentScale.Fit, modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp / 4).dp)))
    } else {
        FunIconTile(FunIcons.badge(def.id), size = sizeDp.dp, iconSize = (sizeDp * 0.62f).dp, dark = look.isDark)
    }
}
