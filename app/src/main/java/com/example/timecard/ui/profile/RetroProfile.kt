package com.example.timecard.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.domain.BadgeDefinition
import com.example.timecard.ui.common.CoinAmount
import com.example.timecard.ui.components.InitialsAvatar
import com.example.timecard.ui.theme.ACCENT_UNLOCKS
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.RetroAccentPink
import com.example.timecard.ui.theme.RetroBox
import com.example.timecard.ui.theme.RetroButton
import com.example.timecard.ui.theme.RetroRainbowText
import com.example.timecard.ui.theme.RetroRoyal
import com.example.timecard.ui.theme.RetroSparkle
import com.example.timecard.ui.theme.RetroTextHeading
import com.example.timecard.ui.theme.RetroTextPrimary
import com.example.timecard.ui.theme.RetroTextSecondary
import com.example.timecard.ui.theme.RetroTickingCounter
import com.example.timecard.ui.theme.RetroYellow
import com.example.timecard.ui.theme.VT323FontFamily
import com.example.timecard.ui.theme.beveledBorder
import com.example.timecard.ui.theme.rememberRetroMotion
import com.example.timecard.ui.theme.rememberRetroBlink

private val RetroPurple = Color(0xFF7A3CF0)
private val RetroOnline = Color(0xFF00B050)

private val RetroThemeNames = mapOf(
    null to "Default", "orange" to "Sunrise", "purple" to "Twilight",
    "teal" to "Isle", "gold" to "Daybreak", "red" to "Red",
    "hacker" to "Hacker", "sunset" to "Sunset", "midnight" to "Midnight",
    "ocean" to "Ocean", "lcars" to "LCARS", "starwars" to "Star Wars",
    "retro" to "Retro"
)

/** Downscales a picked photo to at most 512px and re-encodes it as JPEG (same rules as the standard profile). */
internal fun decodeAvatarBytes(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val original = BitmapFactory.decodeStream(stream) ?: return@use null
        val max = 512
        val scale = minOf(max.toFloat() / original.width, max.toFloat() / original.height, 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else original
        val out = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }

/**
 * The Retro theme's profile page, laid out like a 1999-2005 social profile: marquee banner, photo and
 * "online now" on the left, About Me / Top Badges boxes on the right, then page customisation.
 * Behaviour (display name, photo, theme pick, shop/alerts links) is the standard profile's.
 */
@Composable
internal fun RetroProfileContent(
    profileViewModel: ProfileViewModel,
    actualName: String,
    earnedBadges: List<BadgeDefinition>,
    displayNameInput: String,
    onDisplayNameChange: (String) -> Unit,
    onBadgeClick: (BadgeDefinition) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onOpenShop: () -> Unit
) {
    val profile = profileViewModel.profile
    val shownName = profile.displayName ?: actualName
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) decodeAvatarBytes(context, uri)?.let { profileViewModel.saveCustomAvatar(it) }
    }
    val avatarBitmap = if (profile.avatar == "custom") {
        profileViewModel.avatarImage?.let { bytes ->
            remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
        }
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RetroMarqueeBanner("*~*~* WELCOME TO MY PAGE *~*~* THANKS FOR VISITING *~*~* ADD ME TO YOUR TOP 8 *~*~* ")

        if (profileViewModel.isBackfilling) {
            val on = rememberRetroBlink(700)
            Text(
                text = if (on) "LOADING YOUR HISTORY... KUSTOM KASH IS BEING CALCULATED" else " ",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = RetroAccentPink
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            // ── Left column: name, photo, online status, links ─────────────────────
            Column(modifier = Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(RetroRoyal).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetroSparkle(phase = 0.1f, fontSize = 13.sp)
                    RetroRainbowText(text = " ${shownName.uppercase()} ", fontSize = 21.sp)
                    RetroSparkle(phase = 0.6f, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .beveledBorder(isInset = true, borderWidth = 2.dp)
                        .padding(4.dp)
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            InitialsAvatar(name = shownName, size = 164.dp, fontSize = 60.sp, bgColor = RetroRoyal)
                        }
                    }
                }

                val onlineOn = rememberRetroBlink(900)
                Text(
                    text = "● ONLINE NOW!",
                    fontFamily = VT323FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (onlineOn) RetroOnline else Color(0xFF7FD79F),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                val mood = if (profile.streaks.currentDaily > 0) "on fire 🔥" else "caffeinated ☕"
                Text(
                    text = "Mood: $mood",
                    fontFamily = VT323FontFamily,
                    fontSize = 17.sp,
                    color = RetroTextHeading,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                RetroBox(title = "Contacting", headerColor = RetroAccentPink) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RetroButton(
                            onClick = { onDismiss(); onOpenShop() },
                            label = "VISIT SHOP",
                            icon = "🛍️",
                            backgroundColor = RetroYellow,
                            textColor = Color.Black,
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        )
                        RetroButton(
                            onClick = { onDismiss(); onNavigateToAlerts() },
                            label = "PAST ALERTS",
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        )
                        RetroButton(
                            onClick = onDismiss,
                            label = "CLOSE",
                            icon = "🚪",
                            modifier = Modifier.fillMaxWidth().height(34.dp)
                        )
                    }
                }
            }

            // ── Right column: about me + top badges ─────────────────────────────────
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RetroBox(title = "$shownName's About Me") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        CoinAmount(amount = profile.coins, fontSize = 26.sp, iconSize = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Kustom Kash", fontFamily = VT323FontFamily, fontSize = 17.sp, color = RetroTextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    RetroInfoRow("Daily streak", "${profile.streaks.currentDaily}  (best ${profile.streaks.bestDaily})", shaded = true)
                    RetroInfoRow("Weekly streak", "${profile.streaks.currentWeekly}  (best ${profile.streaks.bestWeekly})")
                    RetroInfoRow("Best week", "${String.format("%.2f", profile.records.bestWeekHours)} hrs", shaded = true)
                    RetroInfoRow("Best day", "${String.format("%.2f", profile.records.busiestDay)} hrs")
                    if (profile.records.favoriteJob.isNotBlank()) {
                        RetroInfoRow("Favorite job", profile.records.favoriteJob, shaded = true)
                    }
                }

                val totalEarned = profile.badges.values.sum()
                RetroBox(
                    title = "Top Badges${if (earnedBadges.isNotEmpty()) " ($totalEarned)" else ""}",
                    headerColor = RetroPurple
                ) {
                    if (earnedBadges.isEmpty()) {
                        Text(
                            "Save your first timecard to start earning badges!",
                            fontFamily = VT323FontFamily,
                            fontSize = 16.sp,
                            color = RetroTextSecondary
                        )
                    } else {
                        earnedBadges.chunked(4).forEach { rowBadges ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                rowBadges.forEach { def ->
                                    RetroBadgeTile(
                                        def = def,
                                        count = profile.badges[def.id] ?: 1,
                                        image = profileViewModel.badgeImages[def.id],
                                        onClick = { onBadgeClick(def) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(4 - rowBadges.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        // ── Customise my page ────────────────────────────────────────────────────
        RetroBox(title = "Customize My Page", headerColor = RetroPurple) {
            Text("DISPLAY NAME", fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RetroTextHeading)
            Text(
                "Shown in the app instead of your actual name. Your login ID never changes.",
                fontFamily = VT323FontFamily, fontSize = 14.sp, color = RetroTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            if (profile.inventory.contains("feature_display_name")) {
                TextField(
                    value = displayNameInput,
                    onValueChange = onDisplayNameChange,
                    placeholder = { Text(actualName, fontFamily = VT323FontFamily, fontSize = 18.sp, color = RetroTextSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = VT323FontFamily, fontSize = 20.sp, color = RetroTextPrimary),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().beveledBorder(isInset = true, borderWidth = 2.dp)
                )
                Spacer(Modifier.height(6.dp))
                RetroButton(
                    onClick = { profileViewModel.setDisplayName(displayNameInput) },
                    label = "SAVE DISPLAY NAME",
                    backgroundColor = RetroYellow,
                    textColor = Color.Black,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            } else {
                Text("🔒 Unlock custom name in Shop", fontFamily = VT323FontFamily, fontSize = 16.sp, color = RetroTextSecondary)
            }

            Spacer(Modifier.height(10.dp))
            Text("PROFILE PHOTO", fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RetroTextHeading)
            Spacer(Modifier.height(4.dp))
            if (profile.inventory.contains("feature_custom_avatar")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetroButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        label = if (profile.avatar == "custom") "CHANGE PHOTO" else "UPLOAD PHOTO",
                        backgroundColor = RetroYellow,
                        textColor = Color.Black,
                        modifier = Modifier.weight(1f).height(36.dp)
                    )
                    if (profile.avatar == "custom") {
                        RetroButton(
                            onClick = { profileViewModel.clearCustomAvatar() },
                            label = "REMOVE PHOTO",
                            modifier = Modifier.weight(1f).height(36.dp)
                        )
                    }
                }
            } else {
                Text("🔒 Unlock custom photo in Shop", fontFamily = VT323FontFamily, fontSize = 16.sp, color = RetroTextSecondary)
            }
        }

        // ── Pick your layout (immersive themes) ──────────────────────────────────
        RetroBox(title = "Pick Your Layout", headerColor = RetroAccentPink) {
            val entries = buildList {
                add(Triple<String?, Color, String?>(null, AccentBlue, null))
                ACCENT_UNLOCKS.forEach { (key, color, itemId) -> add(Triple<String?, Color, String?>(key, color, itemId)) }
            }
            entries.chunked(5).forEach { chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    chunk.forEach { (key, color, itemId) ->
                        RetroLayoutSwatch(
                            name = RetroThemeNames[key] ?: key ?: "?",
                            color = color,
                            unlocked = itemId == null || profile.inventory.contains(itemId),
                            selected = profile.accentColor == key,
                            onClick = { profileViewModel.setAccentColor(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(5 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(
                "Buy new layouts in the Shop. Tap to apply.",
                fontFamily = VT323FontFamily, fontSize = 14.sp, color = RetroTextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PROFILE VIEWS: ",
                fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RetroTextHeading
            )
            RetroTickingCounter(start = 1995)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMarqueeBanner(text: String) {
    val motion = rememberRetroMotion()
    Text(
        text = text,
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = RetroYellow,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroRoyal)
            .padding(vertical = 3.dp)
            .then(if (motion) Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 70.dp) else Modifier)
    )
}

@Composable
private fun RetroInfoRow(label: String, value: String, shaded: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (shaded) Color(0xFFCFE6FF) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = VT323FontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RetroTextHeading)
        Text(value, fontFamily = VT323FontFamily, fontSize = 17.sp, color = RetroTextPrimary)
    }
}

@Composable
private fun RetroBadgeTile(
    def: BadgeDefinition,
    count: Int,
    image: ByteArray?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = image?.let { bytes ->
        remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    }
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .beveledBorder(isInset = false, borderWidth = 2.dp)
                .clickable(onClick = onClick)
                .padding(4.dp)
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = def.name, contentScale = ContentScale.Fit, modifier = Modifier.size(36.dp))
            } else {
                Text(def.emoji, fontSize = 28.sp)
            }
            Text(
                text = def.name,
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = RetroRoyal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        if (def.repeatable && count > 1) {
            Text(
                text = "×$count",
                fontFamily = VT323FontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.align(Alignment.TopEnd).background(RetroYellow).padding(horizontal = 3.dp)
            )
        }
    }
}

@Composable
private fun RetroLayoutSwatch(
    name: String,
    color: Color,
    unlocked: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blink = rememberRetroBlink(600)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(if (unlocked) color else color.copy(alpha = 0.25f))
                .beveledBorder(isInset = selected, borderWidth = 2.dp)
                .then(
                    if (selected) Modifier.border(2.dp, if (blink) RetroYellow else Color.Black) else Modifier
                )
                .then(if (unlocked) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            if (!unlocked) Text("🔒", fontSize = 12.sp)
        }
        Text(
            text = name,
            fontFamily = VT323FontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            color = if (selected) RetroRoyal else RetroTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
