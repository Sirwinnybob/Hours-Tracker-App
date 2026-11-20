package com.example.timecard.ui.alerts

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95StatusBar
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.rememberRetroMotion

/** Mail folders over the past alerts. */
internal enum class MailFolder(val label: String) { Inbox("Inbox"), NeedsReply("Needs reply"), Acknowledged("Acknowledged") }

internal fun mailFolderItems(all: List<AlertsViewModel.MergedAlert>, folder: MailFolder) = when (folder) {
    MailFolder.Inbox -> all
    MailFolder.NeedsReply -> all.filter { !it.acknowledged }
    MailFolder.Acknowledged -> all.filter { it.acknowledged }
}

/** Subject line: the message's first line, cut to 48 characters. */
internal fun mailSubject(message: String): String {
    val first = message.trim().lineSequence().firstOrNull()?.trim().orEmpty()
    return if (first.length > 48) first.take(47).trimEnd() + "…" else first.ifEmpty { "(no subject)" }
}

/** Sender shown in the From column. */
internal fun mailFrom(a: AlertsViewModel.MergedAlert): String = a.sentBy?.takeIf { it.isNotBlank() } ?: "Office"

/** "2026-11-19T15:42:00" → "2026-11-19 15:42". */
internal fun mailDate(iso: String?): String = iso?.replace('T', ' ')?.take(16) ?: ""

/**
 * Retro Past Alerts as "KKC Mail" (2026-11-20 mockup): folder tree, message list (unanswered = bold
 * with a bouncing envelope), preview pane with your reply.
 */
@Composable
internal fun RetroMail(viewModel: AlertsViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadPastAlerts() }
    val motion = rememberRetroMotion()
    var folder by remember { mutableStateOf(MailFolder.Inbox) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val all = viewModel.allAlerts
    val list = mailFolderItems(all, folder)
    val current = list.firstOrNull { it.id == selectedId } ?: list.firstOrNull()
    val unread = all.count { !it.acknowledged }
    val bounce by rememberInfiniteTransition(label = "mail").animateFloat(0f, if (motion) 1f else 0f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "mailBounce")

    Win95Desktop {
        Win95Window("Inbox - KKC Mail", Modifier.fillMaxSize(), onClose = onDismiss, menus = listOf("File", "Edit", "View", "Tools", "Message", "Help"),
            icon = { Text("✉", style = w95Text(16.sp, W95.White)) }) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Win95Button("Send/Recv", { viewModel.loadPastAlerts() })
                Win95Button("Close", onDismiss)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Win95Sunken(Modifier.width(160.dp).fillMaxHeight()) {
                    Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📁 KKC Mail", style = w95Text(15.sp, weight = FontWeight.Bold))
                        MailFolder.entries.forEach { f ->
                            val on = f == folder
                            val count = if (f == MailFolder.Inbox && unread > 0) " ($unread)" else ""
                            Text(
                                "${f.label}$count", style = w95Text(15.sp, if (on) W95.White else W95.Black, if (f == MailFolder.Inbox && unread > 0) FontWeight.Bold else FontWeight.Normal),
                                modifier = Modifier.fillMaxWidth().padding(start = 12.dp).background(if (on) W95.Navy else Color.Transparent)
                                    .clickable(role = Role.Tab) { folder = f; selectedId = null }.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Win95Sunken(Modifier.fillMaxWidth().height(320.dp)) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().background(W95.Face).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                Text("!", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.width(18.dp))
                                Text("From", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.width(130.dp))
                                Text("Subject", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                Text("Received", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.width(130.dp))
                            }
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                if (list.isEmpty()) Text("There are no items in this folder.", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(8.dp))
                                list.forEach { a ->
                                    val sel = a.id == current?.id
                                    val fg = if (sel) W95.White else W95.Black
                                    val weight = if (a.acknowledged) FontWeight.Normal else FontWeight.Bold
                                    Row(
                                        Modifier.fillMaxWidth().height(34.dp).background(if (sel) W95.Navy else Color.Transparent)
                                            .clickable(role = Role.Button, onClickLabel = "Open message") { selectedId = a.id }.padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (a.acknowledged) "" else "✉", style = w95Text(14.sp, if (sel) W95.White else W95.Red, FontWeight.Bold),
                                            modifier = Modifier.width(18.dp).graphicsLayer { translationY = if (a.acknowledged) 0f else -bounce * 3.dp.toPx() }
                                        )
                                        Text(mailFrom(a), style = w95Text(15.sp, fg, weight), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(130.dp))
                                        Text(mailSubject(a.message), style = w95Text(15.sp, fg, weight), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(mailDate(a.sentAt), style = w95Text(13.sp, fg, weight), maxLines = 1, modifier = Modifier.width(130.dp))
                                    }
                                }
                            }
                        }
                    }
                    Win95Sunken(Modifier.weight(1f).fillMaxWidth()) {
                        if (current == null) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No message selected.", style = w95Text(15.sp, W95.Shadow, italic = true)) }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Column(Modifier.fillMaxWidth().background(W95.Face).padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Header("From:", mailFrom(current))
                                    Header("Date:", mailDate(current.sentAt))
                                    Header("Subject:", mailSubject(current.message))
                                }
                                Text(
                                    current.message,
                                    style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, color = W95.Black, lineHeight = 26.sp),
                                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)
                                )
                                if (current.acknowledged) {
                                    Text(
                                        "Your reply: ${current.response?.takeIf { it.isNotBlank() } ?: "Acknowledged"}  (${mailDate(current.acknowledgedAt)})",
                                        style = w95Text(15.sp), modifier = Modifier.fillMaxWidth().background(Color(0xFFF0F0F0)).padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Win95StatusBar(listOf("${list.size} message(s), $unread unread" to null, "Working Online" to 140.dp))
        }
    }
}

@Composable
private fun Header(k: String, v: String) {
    Row { Text(k, style = w95Text(15.sp, weight = FontWeight.Bold), modifier = Modifier.width(80.dp)); Text(v, style = w95Text(15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
}
