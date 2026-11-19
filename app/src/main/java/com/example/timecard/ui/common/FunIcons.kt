package com.example.timecard.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.timecard.kkcnav.DUOTONE
import com.example.timecard.kkcnav.block
import com.example.timecard.kkcnav.circle
import com.example.timecard.kkcnav.kkcIcon
import com.example.timecard.kkcnav.line
import com.example.timecard.kkcnav.roundRect
import com.example.timecard.kkcnav.solid

// Fun icons for Shop items and badges (standard theme). Playful subjects, drawn in KKCSheetTracker's
// custom icon family (kkcnav/KkcIconDsl.kt: 24×24, 2-unit rounded strokes, translucent duotone body,
// solid details) and tinted per item on a soft tile of the same color. They replace the emoji that
// shop_catalog.json and BadgeEngine carry. An uploaded image (shop imageFile / admin badge image)
// still wins over these; anything without an icon gets a gift (shop) or medal (badge).

/** An icon plus the color it is tinted (and its tile is washed) with. */
data class FunIcon(val vector: ImageVector, val color: Color)

private fun ImageVector.Builder.duo(shape: PathBuilder.() -> Unit) {
    solid(DUOTONE, pathBuilder = shape)
    line(pathBuilder = shape)
}

private fun ImageVector.Builder.dots(vararg xy: Float, r: Float = 1.1f) = block(width = 0.5f) {
    var i = 0
    while (i + 1 < xy.size) { circle(xy[i], xy[i + 1], r); i += 2 }
}

// ── Shop items ──────────────────────────────────────────────────────────────────
private fun paperPlane() = kkcIcon("FunPaperPlane") {
    duo { moveTo(2.5f, 11f); lineTo(21f, 3f); lineTo(15f, 21f); lineTo(11f, 14f); close() }
    line {
        moveTo(11f, 14f); lineTo(21f, 3f)
        moveTo(2f, 18.5f); horizontalLineTo(5.5f)
        moveTo(3f, 21.5f); horizontalLineTo(6f)
    }
}

private fun ghost() = kkcIcon("FunGhost") {
    duo {
        moveTo(5f, 20f); verticalLineTo(10f)
        arcTo(7f, 7f, 0f, false, true, 19f, 10f)
        verticalLineTo(20f)
        lineTo(16.7f, 18f); lineTo(14.3f, 20f); lineTo(11.9f, 18f); lineTo(9.5f, 20f); lineTo(7.3f, 18f)
        close()
    }
    dots(9.5f, 10.5f, 14.5f, 10.5f)
    line { moveTo(10f, 14f); quadTo(12f, 15.8f, 14f, 14f) }
}

private fun portrait() = kkcIcon("FunPortrait") {
    duo { roundRect(4f, 3f, 20f, 21f, 2f) }
    dots(9.5f, 10f, 14.5f, 10f)
    line { moveTo(8.5f, 14f); quadTo(12f, 18f, 15.5f, 14f) }
}

// "Hello my name is" sticker: solid top band, scribbled name below.
private fun nameTag() = kkcIcon("FunNameTag") {
    duo { roundRect(2.5f, 5f, 21.5f, 19f, 2.5f) }
    block(width = 1f) {
        moveTo(2.5f, 10f); verticalLineTo(7.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 5f, 5f)
        horizontalLineTo(19f)
        arcTo(2.5f, 2.5f, 0f, false, true, 21.5f, 7.5f)
        verticalLineTo(10f); close()
    }
    line {
        moveTo(6.5f, 15f)
        curveTo(8f, 12.5f, 9f, 17f, 10.5f, 14.5f)
        curveTo(12f, 12.5f, 13f, 17f, 14.5f, 14.5f)
        curveTo(15.5f, 13f, 16.5f, 16f, 17.5f, 14.5f)
    }
}

private fun coffee() = kkcIcon("FunCoffee") {
    duo {
        moveTo(4f, 10f); horizontalLineTo(16f); verticalLineTo(17f)
        arcTo(4f, 4f, 0f, false, true, 12f, 21f)
        horizontalLineTo(8f)
        arcTo(4f, 4f, 0f, false, true, 4f, 17f)
        close()
    }
    line {
        moveTo(16f, 12f); horizontalLineTo(17.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 17.5f, 17f)
        horizontalLineTo(16f)
        moveTo(8f, 2.5f); quadTo(6.3f, 4f, 8f, 6f)
        moveTo(12f, 2.5f); quadTo(10.3f, 4f, 12f, 6f)
    }
}

private fun burger() = kkcIcon("FunBurger") {
    duo { moveTo(4f, 11f); arcTo(8f, 6f, 0f, false, true, 20f, 11f); close() }
    line {
        moveTo(3f, 14f); horizontalLineTo(21f)
        moveTo(4.5f, 14f); lineTo(6.5f, 16f); lineTo(8.5f, 14f); lineTo(10.5f, 16f); lineTo(12.5f, 14f)
        lineTo(14.5f, 16f); lineTo(16.5f, 14f); lineTo(18.5f, 16f); lineTo(20f, 14.5f)
    }
    duo { roundRect(4f, 18f, 20f, 21f, 1.5f) }
    dots(9f, 7.6f, 13f, 6.6f, 15.6f, 8.3f, r = 0.8f)
}

private fun ticket() = kkcIcon("FunTicket") {
    duo {
        moveTo(3f, 7f); horizontalLineTo(21f); verticalLineTo(10f)
        arcTo(2f, 2f, 0f, false, false, 21f, 14f)
        verticalLineTo(17f); horizontalLineTo(3f); verticalLineTo(14f)
        arcTo(2f, 2f, 0f, false, false, 3f, 10f)
        close()
    }
    line {
        moveTo(15f, 7.5f); verticalLineTo(9f); moveTo(15f, 11.25f); verticalLineTo(12.75f); moveTo(15f, 15f); verticalLineTo(16.5f)
        moveTo(6.5f, 12f); horizontalLineTo(11f)
        moveTo(9f, 10f); lineTo(11f, 12f); lineTo(9f, 14f)
    }
}

private fun gift() = kkcIcon("FunGift") {
    duo { roundRect(5f, 13f, 19f, 21f, 1f) }
    line {
        roundRect(3f, 9f, 21f, 13f, 1f)
        moveTo(12f, 9f); verticalLineTo(21f)
        moveTo(12f, 9f); quadTo(7f, 3f, 7.5f, 8f)
        moveTo(12f, 9f); quadTo(17f, 3f, 16.5f, 8f)
    }
}

// ── Badges ──────────────────────────────────────────────────────────────────────
private fun bell() = kkcIcon("FunBell") {
    duo {
        moveTo(6f, 16f); verticalLineTo(11f)
        arcTo(6f, 6f, 0f, false, true, 18f, 11f)
        verticalLineTo(16f); lineTo(20f, 18f); horizontalLineTo(4f); lineTo(6f, 16f); close()
    }
    line {
        moveTo(10f, 21f); horizontalLineTo(14f)
        moveTo(2.5f, 8.5f); quadTo(3f, 5.5f, 4.5f, 4f)
        moveTo(21.5f, 8.5f); quadTo(21f, 5.5f, 19.5f, 4f)
    }
}

private fun stopwatch() = kkcIcon("FunStopwatch") {
    duo { circle(14.5f, 14f, 6.5f) }
    line {
        moveTo(14.5f, 14f); verticalLineTo(10.5f)
        moveTo(12.5f, 3.5f); horizontalLineTo(16.5f)
        moveTo(14.5f, 3.5f); verticalLineTo(7.5f)
        moveTo(1.5f, 11f); horizontalLineTo(5f)
        moveTo(2.5f, 15f); horizontalLineTo(5.5f)
        moveTo(1.5f, 19f); horizontalLineTo(6f)
    }
}

private fun timeCard() = kkcIcon("FunTimeCard") {
    duo { roundRect(5f, 2f, 19f, 22f, 2f) }
    line {
        moveTo(8f, 6f); horizontalLineTo(16f)
        circle(12f, 14f, 4f)
        moveTo(12f, 12f); verticalLineTo(14f); horizontalLineTo(13.5f)
    }
}

private fun hopper() = kkcIcon("FunHopper") {
    duo { roundRect(2f, 15f, 8f, 21f, 1f) }
    duo { roundRect(16f, 15f, 22f, 21f, 1f) }
    line {
        moveTo(5f, 12.5f); quadTo(12f, 1f, 19f, 12.5f)
        moveTo(16.2f, 11.4f); lineTo(19f, 12.5f); lineTo(19.6f, 9.6f)
    }
}

private fun calendarChecks() = kkcIcon("FunCalendarChecks") {
    duo { roundRect(3f, 5f, 21f, 21f, 2f) }
    line {
        moveTo(3f, 10f); horizontalLineTo(21f)
        moveTo(8f, 3f); verticalLineTo(7f)
        moveTo(16f, 3f); verticalLineTo(7f)
        moveTo(6.5f, 15f); lineTo(8f, 16.5f); lineTo(10.5f, 14f)
        moveTo(13.5f, 15f); lineTo(15f, 16.5f); lineTo(17.5f, 14f)
    }
}

private fun star() = kkcIcon("FunStar") {
    duo {
        moveTo(12f, 3f); lineTo(14.6f, 8.6f); lineTo(20.7f, 9.2f); lineTo(16.1f, 13.3f); lineTo(17.4f, 19.3f)
        lineTo(12f, 16.2f); lineTo(6.6f, 19.3f); lineTo(7.9f, 13.3f); lineTo(3.3f, 9.2f); lineTo(9.4f, 8.6f); close()
    }
    line {
        moveTo(20f, 2.5f); verticalLineTo(5f); moveTo(18.75f, 3.75f); horizontalLineTo(21.25f)
    }
}

private fun crown() = kkcIcon("FunCrown") {
    duo {
        moveTo(3f, 8f); lineTo(7.5f, 12f); lineTo(12f, 5f); lineTo(16.5f, 12f); lineTo(21f, 8f)
        lineTo(19f, 18f); horizontalLineTo(5f); close()
    }
    line { moveTo(5f, 21f); horizontalLineTo(19f) }
    dots(12f, 14.5f, 3f, 6.5f, 21f, 6.5f, 12f, 3.6f)
}

private fun shieldClock() = kkcIcon("FunShieldClock") {
    duo {
        moveTo(12f, 2.5f); lineTo(20f, 5.5f); verticalLineTo(11.5f)
        curveTo(20f, 16.5f, 16.5f, 20f, 12f, 21.5f)
        curveTo(7.5f, 20f, 4f, 16.5f, 4f, 11.5f)
        verticalLineTo(5.5f); close()
    }
    line { moveTo(12f, 8f); verticalLineTo(12f); lineTo(14.5f, 13.5f) }
}

private fun sun() = kkcIcon("FunSun") {
    duo { circle(12f, 12f, 4.5f) }
    line {
        moveTo(12f, 2f); verticalLineTo(4.5f); moveTo(12f, 19.5f); verticalLineTo(22f)
        moveTo(2f, 12f); horizontalLineTo(4.5f); moveTo(19.5f, 12f); horizontalLineTo(22f)
        moveTo(4.9f, 4.9f); lineTo(6.7f, 6.7f); moveTo(17.3f, 17.3f); lineTo(19.1f, 19.1f)
        moveTo(4.9f, 19.1f); lineTo(6.7f, 17.3f); moveTo(17.3f, 6.7f); lineTo(19.1f, 4.9f)
    }
}

private fun truckCrown() = kkcIcon("FunTruckCrown") {
    solid(DUOTONE) { roundRect(2f, 5.5f, 14f, 16.5f, 1.25f) }
    line {
        moveTo(14f, 16.5f); verticalLineTo(5.5f); horizontalLineTo(3.25f)
        arcTo(1.25f, 1.25f, 0f, false, false, 2f, 6.75f)
        verticalLineTo(15.25f)
        arcTo(1.25f, 1.25f, 0f, false, false, 3.25f, 16.5f)
        horizontalLineTo(4.5f)
        moveTo(14f, 16.5f); horizontalLineTo(9f)
        moveTo(14f, 8.5f); horizontalLineTo(17.5f); lineTo(21f, 12f); verticalLineTo(16.5f); horizontalLineTo(19.5f)
    }
    block(width = 1f) {
        moveTo(5f, 13f); lineTo(5.6f, 9f); lineTo(7.2f, 10.6f); lineTo(8f, 8f); lineTo(8.8f, 10.6f); lineTo(10.4f, 9f); lineTo(11f, 13f); close()
        circle(6.75f, 17.25f, 2f)
        circle(17.25f, 17.25f, 2f)
    }
}

// ── Challenges: target with a dart in the bull ──────────────────────────────────
private fun target() = kkcIcon("FunTarget") {
    duo { circle(11f, 13f, 8.5f) }
    line {
        circle(11f, 13f, 4.5f)
        moveTo(11f, 13f); lineTo(19.5f, 4.5f)
        moveTo(17f, 4f); lineTo(20f, 4f); lineTo(20f, 7f)
    }
    dots(11f, 13f)
}

private fun medal() = kkcIcon("FunMedal") {
    line {
        moveTo(8f, 2f); lineTo(11f, 8.5f)
        moveTo(16f, 2f); lineTo(13f, 8.5f)
    }
    duo { circle(12f, 15f, 6f) }
    line { circle(12f, 15f, 2.5f) }
}

object FunIcons {
    private val Blue = Color(0xFF1565C0)
    private val Purple = Color(0xFF6A3FB5)
    private val Teal = Color(0xFF00897B)
    private val Orange = Color(0xFFE65100)
    private val Brown = Color(0xFF6D4C41)
    private val Amber = Color(0xFFC77700)
    private val Red = Color(0xFFC62828)
    private val Green = Color(0xFF2E7D32)
    private val Pink = Color(0xFFC2185B)

    private val PaperPlane by lazy { paperPlane() }
    private val Ghost by lazy { ghost() }
    private val Portrait by lazy { portrait() }
    private val NameTag by lazy { nameTag() }
    private val Coffee by lazy { coffee() }
    private val Burger by lazy { burger() }
    private val Ticket by lazy { ticket() }
    private val Gift by lazy { gift() }
    private val Bell by lazy { bell() }
    private val Stopwatch by lazy { stopwatch() }
    private val TimeCard by lazy { timeCard() }
    private val Hopper by lazy { hopper() }
    private val CalendarChecks by lazy { calendarChecks() }
    private val Star by lazy { star() }
    private val Crown by lazy { crown() }
    private val ShieldClock by lazy { shieldClock() }
    private val Sun by lazy { sun() }
    private val TruckCrown by lazy { truckCrown() }
    private val Medal by lazy { medal() }
    private val Target by lazy { target() }

    /** Weekly challenges (their catalog icon is an emoji). */
    val Challenge: FunIcon get() = FunIcon(Target, Green)

    /** Icon for a shop_catalog.json item id (themes use a color preview instead). */
    fun shopItem(id: String): FunIcon = when (id) {
        "consumable_send_note" -> FunIcon(PaperPlane, Blue)
        "consumable_send_anonymous_note" -> FunIcon(Ghost, Purple)
        "feature_custom_avatar" -> FunIcon(Portrait, Teal)
        "feature_display_name" -> FunIcon(NameTag, Orange)
        "reward_coffee" -> FunIcon(Coffee, Brown)
        "reward_lunch" -> FunIcon(Burger, Amber)
        "reward_early_out" -> FunIcon(Ticket, Red)
        else -> FunIcon(Gift, Pink)
    }

    /** Icon for a BadgeEngine badge id. Admin-made badges without an image get the medal. */
    fun badge(id: String): FunIcon = when (id) {
        "clock_puncher" -> FunIcon(TimeCard, Teal)
        "speed_logger" -> FunIcon(Stopwatch, Blue)
        "job_hopper" -> FunIcon(Hopper, Green)
        "consistent" -> FunIcon(CalendarChecks, Purple)
        "perfect_week" -> FunIcon(Star, Amber)
        "shop_king" -> FunIcon(Crown, Amber)
        "overtime_warrior" -> FunIcon(ShieldClock, Red)
        "saturday_warrior" -> FunIcon(Sun, Orange)
        "delivery_king" -> FunIcon(TruckCrown, Blue)
        "alert_responder" -> FunIcon(Bell, Amber)
        else -> FunIcon(Medal, Pink)
    }
}

/** A fun icon on its soft color tile. */
@Composable
fun FunIconTile(icon: FunIcon, modifier: Modifier = Modifier, size: Dp = 52.dp, iconSize: Dp = 32.dp, dark: Boolean = false) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.27f))
            .background(icon.color.copy(alpha = if (dark) 0.24f else 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon.vector, contentDescription = null, tint = if (dark) lerpLight(icon.color) else icon.color, modifier = Modifier.size(iconSize))
    }
}

private fun lerpLight(c: Color) = androidx.compose.ui.graphics.lerp(c, Color.White, 0.35f)

/**
 * Retro keeps its emoji (unchanged by request); every other theme draws [fun] instead. LCARS screens
 * that reach here already branch away before this call.
 */
@Composable
fun FunEmojiOr(emoji: String, emojiSizeSp: Int = 22, fun_: @Composable () -> Unit) {
    if (com.example.timecard.ui.theme.LocalTimecardColors.current.isRetro) {
        androidx.compose.material3.Text(emoji, fontSize = androidx.compose.ui.unit.TextUnit(emojiSizeSp.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp))
    } else {
        fun_()
    }
}

/** Leaderboard place: gold / silver / bronze medal disc for 1–3 (instead of medal emoji), plain disc after. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val colors = com.example.timecard.ui.theme.LocalTimecardColors.current
    val (bg, fg) = when (rank) {
        1 -> Color(0xFFE0A800) to Color(0xFF3A2A00)
        2 -> Color(0xFFB0BEC5) to Color(0xFF263238)
        3 -> Color(0xFFC77A3A) to Color.White
        else -> colors.hover to colors.textSecondary
    }
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            "$rank",
            color = fg,
            fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
        )
    }
}
