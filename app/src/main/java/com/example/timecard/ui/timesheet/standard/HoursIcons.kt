package com.example.timecard.ui.timesheet.standard

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.timecard.kkcnav.DUOTONE
import com.example.timecard.kkcnav.block
import com.example.timecard.kkcnav.circle
import com.example.timecard.kkcnav.kkcIcon
import com.example.timecard.kkcnav.line
import com.example.timecard.kkcnav.roundRect
import com.example.timecard.kkcnav.solid

// Icons for the standard timesheet, drawn in KKCSheetTracker's custom icon family (kkcnav/KkcIconDsl.kt:
// 24×24, 2-unit rounded strokes, duotone = translucent body + solid detail). Undo and Delivery are copies
// of KKC's MarkupUndo and ReferenceDelivery so the same action looks the same in both apps.
// No emoji anywhere in the standard layout.

// ── Stats: three bars on a baseline, duotone bodies ─────────────────────────────
private fun stats() = kkcIcon("HoursStats") {
    solid(DUOTONE) {
        roundRect(4f, 12f, 8f, 20f, 1f)
        roundRect(10f, 5f, 14f, 20f, 1f)
        roundRect(16f, 9f, 20f, 20f, 1f)
    }
    line {
        roundRect(4f, 12f, 8f, 20f, 1f)
        roundRect(10f, 5f, 14f, 20f, 1f)
        roundRect(16f, 9f, 20f, 20f, 1f)
        moveTo(2.5f, 21f); horizontalLineTo(21.5f)
    }
}

// ── Leaderboard: 2-1-3 podium, winner's block solid ─────────────────────────────
private fun leaderboard() = kkcIcon("HoursLeaderboard") {
    solid(DUOTONE) {
        roundRect(2.5f, 12f, 8.5f, 21f, 1f)
        roundRect(15.5f, 15f, 21.5f, 21f, 1f)
    }
    line {
        roundRect(2.5f, 12f, 8.5f, 21f, 1f)
        roundRect(15.5f, 15f, 21.5f, 21f, 1f)
    }
    block { roundRect(9f, 8f, 15f, 21f, 1f) }
    // star over the winner
    line(width = 1.5f) {
        moveTo(12f, 2.5f); verticalLineTo(5.5f)
        moveTo(10.5f, 4f); horizontalLineTo(13.5f)
    }
}

// ── Challenges: target rings, solid bull ────────────────────────────────────────
private fun challenges() = kkcIcon("HoursChallenges") {
    solid(DUOTONE) { circle(12f, 12f, 9f) }
    line {
        circle(12f, 12f, 9f)
        circle(12f, 12f, 5f)
    }
    block { circle(12f, 12f, 1.5f) }
}

// ── Log out: door frame with an arrow leaving it ────────────────────────────────
private fun logout() = kkcIcon("HoursLogout") {
    solid(DUOTONE) { roundRect(3f, 3f, 12f, 21f, 1.5f) }
    line {
        moveTo(12f, 7f); verticalLineTo(4.5f)
        arcTo(1.5f, 1.5f, 0f, false, false, 10.5f, 3f)
        horizontalLineTo(4.5f)
        arcTo(1.5f, 1.5f, 0f, false, false, 3f, 4.5f)
        verticalLineTo(19.5f)
        arcTo(1.5f, 1.5f, 0f, false, false, 4.5f, 21f)
        horizontalLineTo(10.5f)
        arcTo(1.5f, 1.5f, 0f, false, false, 12f, 19.5f)
        verticalLineTo(17f)
        moveTo(9f, 12f); horizontalLineTo(21f)
        moveTo(17.5f, 8.5f); lineTo(21f, 12f); lineTo(17.5f, 15.5f)
    }
}

// ── Undo / Redo (KKC MarkupUndo, and its mirror) ────────────────────────────────
private fun undo() = kkcIcon("HoursUndo") {
    line {
        moveTo(8.5f, 5f); lineTo(4f, 9.5f); lineTo(8.5f, 14f)
        moveTo(4.5f, 9.5f)
        horizontalLineTo(14.5f)
        arcTo(5.5f, 5.5f, 0f, false, true, 14.5f, 20.5f)
        horizontalLineTo(10f)
    }
}

private fun redo() = kkcIcon("HoursRedo") {
    line {
        moveTo(15.5f, 5f); lineTo(20f, 9.5f); lineTo(15.5f, 14f)
        moveTo(19.5f, 9.5f)
        horizontalLineTo(9.5f)
        arcTo(5.5f, 5.5f, 0f, false, false, 9.5f, 20.5f)
        horizontalLineTo(14f)
    }
}

// ── Delivery truck (KKC ReferenceDelivery) ──────────────────────────────────────
private fun delivery() = kkcIcon("HoursDelivery") {
    solid(DUOTONE) { roundRect(2f, 5.5f, 14f, 16.5f, 1.25f) }
    line {
        moveTo(14f, 16.5f)
        verticalLineTo(5.5f)
        horizontalLineTo(3.25f)
        arcTo(1.25f, 1.25f, 0f, false, false, 2f, 6.75f)
        verticalLineTo(15.25f)
        arcTo(1.25f, 1.25f, 0f, false, false, 3.25f, 16.5f)
        horizontalLineTo(4.5f)
        moveTo(14f, 16.5f); horizontalLineTo(9f)
        moveTo(14f, 8.5f)
        horizontalLineTo(17.5f)
        lineTo(21f, 12f)
        verticalLineTo(16.5f)
        horizontalLineTo(19.5f)
        moveTo(17.5f, 8.5f); verticalLineTo(12f); horizontalLineTo(21f)
    }
    block {
        circle(6.75f, 17.25f, 2f)
        circle(17.25f, 17.25f, 2f)
    }
}

// ── Lunch: lunch box with a handle and latch ────────────────────────────────────
private fun lunch() = kkcIcon("HoursLunch") {
    solid(DUOTONE) { roundRect(3f, 9f, 21f, 20f, 2f) }
    line {
        roundRect(3f, 9f, 21f, 20f, 2f)
        moveTo(9f, 9f); verticalLineTo(6f)
        arcTo(1f, 1f, 0f, false, true, 10f, 5f)
        horizontalLineTo(14f)
        arcTo(1f, 1f, 0f, false, true, 15f, 6f)
        verticalLineTo(9f)
        moveTo(3f, 13f); horizontalLineTo(21f)
    }
    block { roundRect(10.5f, 12f, 13.5f, 14.5f, 0.75f) }
}

// ── Shop: shopping bag with a coin on it ────────────────────────────────────────
private fun shop() = kkcIcon("HoursShop") {
    solid(DUOTONE) { roundRect(4f, 8f, 20f, 21f, 2f) }
    line {
        roundRect(4f, 8f, 20f, 21f, 2f)
        moveTo(8.5f, 8f); verticalLineTo(6.5f)
        arcTo(3.5f, 3.5f, 0f, false, true, 15.5f, 6.5f)
        verticalLineTo(8f)
    }
    block { circle(12f, 14.5f, 2.25f) }
}

// ── Theme: half-filled disc (light / dark) ──────────────────────────────────────
private fun theme() = kkcIcon("HoursTheme") {
    line { circle(12f, 12f, 9f) }
    block {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, false, true, 12f, 21f)
        close()
    }
}

// ── Small action glyphs ─────────────────────────────────────────────────────────
private fun plus() = kkcIcon("HoursPlus") {
    line(width = 2.5f) {
        moveTo(12f, 5f); verticalLineTo(19f)
        moveTo(5f, 12f); horizontalLineTo(19f)
    }
}

private fun close() = kkcIcon("HoursClose") {
    line(width = 2.5f) {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }
}

private fun check() = kkcIcon("HoursCheck") {
    line(width = 2.5f) {
        moveTo(5f, 12.5f)
        lineTo(9.5f, 17f)
        lineTo(19f, 7.5f)
    }
}

private fun search() = kkcIcon("HoursSearch") {
    line { circle(10.5f, 10.5f, 6.5f) }
    line(width = 2.5f) {
        moveTo(15.5f, 15.5f)
        lineTo(20.5f, 20.5f)
    }
}

private fun back() = kkcIcon("HoursBack") {
    line(width = 2.5f) {
        moveTo(10f, 5f); lineTo(3.5f, 12f); lineTo(10f, 19f)
        moveTo(4f, 12f); horizontalLineTo(20.5f)
    }
}

private fun flame() = kkcIcon("HoursFlame") {
    val body: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit = {
        moveTo(12f, 22f)
        curveTo(16f, 22f, 19f, 19f, 19f, 15f)
        curveTo(19f, 11.5f, 16.5f, 9f, 15f, 6f)
        curveTo(13.5f, 8f, 12.5f, 9.5f, 12f, 11f)
        curveTo(11f, 10f, 10.5f, 8.5f, 10.5f, 7f)
        curveTo(7.5f, 9f, 5f, 12f, 5f, 15f)
        curveTo(5f, 19f, 8f, 22f, 12f, 22f)
        close()
    }
    solid(DUOTONE, pathBuilder = body)
    line(pathBuilder = body)
}

private fun calendar() = kkcIcon("HoursCalendar") {
    solid(DUOTONE) { roundRect(3f, 5f, 21f, 21f, 2f) }
    line {
        roundRect(3f, 5f, 21f, 21f, 2f)
        moveTo(3f, 10f); horizontalLineTo(21f)
        moveTo(8f, 3f); verticalLineTo(7f)
        moveTo(16f, 3f); verticalLineTo(7f)
    }
}

private fun bell() = kkcIcon("HoursBell") {
    val body: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit = {
        moveTo(6f, 16f); verticalLineTo(11f)
        arcTo(6f, 6f, 0f, false, true, 18f, 11f)
        verticalLineTo(16f); lineTo(20f, 18f); horizontalLineTo(4f); lineTo(6f, 16f); close()
    }
    solid(DUOTONE, pathBuilder = body)
    line(pathBuilder = body)
    line { moveTo(10f, 21f); horizontalLineTo(14f) }
}

private fun camera() = kkcIcon("HoursCamera") {
    val body: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit = {
        moveTo(3f, 8f); horizontalLineTo(7f); lineTo(9f, 5f); horizontalLineTo(15f); lineTo(17f, 8f); horizontalLineTo(21f)
        verticalLineTo(19f); horizontalLineTo(3f); close()
    }
    solid(DUOTONE, pathBuilder = body)
    line(pathBuilder = body)
    line { circle(12f, 13f, 3f) }
}

private fun lock() = kkcIcon("HoursLock") {
    solid(DUOTONE) { roundRect(4f, 10f, 20f, 21f, 2f) }
    line {
        roundRect(4f, 10f, 20f, 21f, 2f)
        moveTo(8f, 10f); verticalLineTo(7f)
        arcTo(4f, 4f, 0f, false, true, 16f, 7f)
        verticalLineTo(10f)
    }
    block { circle(12f, 15.5f, 1.25f) }
}

object HoursIcons {
    val Flame: ImageVector by lazy { flame() }
    val Calendar: ImageVector by lazy { calendar() }
    val Bell: ImageVector by lazy { bell() }
    val Camera: ImageVector by lazy { camera() }
    val Lock: ImageVector by lazy { lock() }
    val Stats: ImageVector by lazy { stats() }
    val Leaderboard: ImageVector by lazy { leaderboard() }
    val Challenges: ImageVector by lazy { challenges() }
    val Logout: ImageVector by lazy { logout() }
    val Undo: ImageVector by lazy { undo() }
    val Redo: ImageVector by lazy { redo() }
    val Delivery: ImageVector by lazy { delivery() }
    val Lunch: ImageVector by lazy { lunch() }
    val Shop: ImageVector by lazy { shop() }
    val Theme: ImageVector by lazy { theme() }
    val Plus: ImageVector by lazy { plus() }
    val Close: ImageVector by lazy { close() }
    val Check: ImageVector by lazy { check() }
    val Search: ImageVector by lazy { search() }
    val Back: ImageVector by lazy { back() }
}
