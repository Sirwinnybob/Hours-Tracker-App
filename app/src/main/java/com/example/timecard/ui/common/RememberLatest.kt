package com.example.timecard.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The latest non-null [value] seen. For dialogs that fade out after their state is cleared: the
 * closing panel keeps showing what it showed, and reopening it with a new value (even mid-fade)
 * shows and acts on the NEW value. A plain `remember { value }` inside AnimatedVisibility keeps the
 * first value while the content stays composed, so a confirm reopened during the fade-out could
 * show and buy the previous item.
 */
@Composable
fun <T : Any> rememberLatestNonNull(value: T?): T? {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return holder[0] as T?
}
