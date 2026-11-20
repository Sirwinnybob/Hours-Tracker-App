package com.example.timecard.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Where a popup goes so the on-screen keyboard never covers it.
 *
 * Preferred: under the anchor ([overlayAnchor] = false), or on top of it (true, for in-cell editors).
 * If that would reach under the keyboard (or off the bottom), a below-anchor popup flips above the
 * anchor; if it still doesn't fit (or it overlays the anchor), it slides up until its bottom sits
 * just above the keyboard. Never above the top of the window.
 */
fun imeAwarePopupY(anchorTop: Int, anchorBottom: Int, popupHeight: Int, windowHeight: Int, imeHeight: Int, overlayAnchor: Boolean): Int {
    val usableBottom = (windowHeight - imeHeight).coerceAtLeast(0)
    val preferred = if (overlayAnchor) anchorTop else anchorBottom
    if (preferred + popupHeight <= usableBottom) return preferred
    if (!overlayAnchor) {
        val above = anchorTop - popupHeight
        if (above >= 0) return above
    }
    return (usableBottom - popupHeight).coerceAtLeast(0)
}

/** [PopupPositionProvider] using [imeAwarePopupY]; x stays aligned with the anchor's left edge, kept on screen. */
class ImeAwarePopupPositionProvider(private val imeHeightPx: Int, private val overlayAnchor: Boolean) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val y = imeAwarePopupY(anchorBounds.top, anchorBounds.bottom, popupContentSize.height, windowSize.height, imeHeightPx, overlayAnchor)
        val x = anchorBounds.left.coerceAtMost((windowSize.width - popupContentSize.width).coerceAtLeast(0)).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * Position provider that follows the keyboard: it reads the IME inset of the main window (read
 * outside the popup, whose own window doesn't get it), so the popup moves up when the keyboard opens.
 */
@Composable
fun rememberImeAwarePopupPosition(overlayAnchor: Boolean = false): PopupPositionProvider {
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    return remember(imeHeight, overlayAnchor) { ImeAwarePopupPositionProvider(imeHeight, overlayAnchor) }
}
