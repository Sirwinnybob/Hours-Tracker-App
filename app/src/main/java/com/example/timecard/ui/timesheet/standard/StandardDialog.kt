package com.example.timecard.ui.timesheet.standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * KKC-style popup for the standard theme: 20dp sheet on the tinted canvas color, white title bar
 * (leading slot, title + subtitle, trailing slot, close), scrolling body of cards, optional footer row.
 * [fullHeight] for big screens (Settings, Shop); otherwise it hugs its content.
 */
@Composable
fun StandardDialog(
    title: String,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    fullHeight: Boolean = false,
    maxWidth: Dp = 880.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.(StandardLook) -> Unit
) {
    val look = currentStandardLook()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        StandardText {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .fillMaxWidth()
                        .then(if (fullHeight) Modifier.fillMaxSize() else Modifier)
                        .shadow(16.dp, shape)
                        .clip(shape)
                        .background(look.head)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(look.surface)
                            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (leading != null) leading()
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, color = look.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (subtitle != null) Text(subtitle, color = look.text2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (trailing != null) trailing()
                        IconAction(HoursIcons.Close, "Close", look, onDismiss, tonal = true, size = 48.dp)
                    }
                    Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(look.line))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (fullHeight) Modifier.weight(1f) else Modifier.weight(1f, fill = false))
                            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) { content(look) }
                    if (footer != null) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(look.line))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(look.surface).padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = footer
                        )
                    }
                }
            }
        }
    }
}

/** True when the standard (KKC-look) layout applies — not LCARS, not Retro. */
@Composable
fun isStandardLook(): Boolean {
    val c = com.example.timecard.ui.theme.LocalTimecardColors.current
    return !c.isLcars && !c.isRetro
}
