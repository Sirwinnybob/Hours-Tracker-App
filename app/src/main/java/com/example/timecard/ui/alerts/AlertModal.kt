package com.example.timecard.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LocalTimecardColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import com.example.timecard.ui.profile.ConfettiBurst
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AlertModal(viewModel: AlertsViewModel) {
    val colors = LocalTimecardColors.current
    val alert = viewModel.currentAlert ?: return
    val isPeerNote = alert.sentBy != null && alert.sentBy != "Admin"

    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(alert.id) {
        if (isPeerNote) {
            showConfetti = true
        } else {
            showConfetti = false
        }
    }

    Dialog(
        onDismissRequest = { /* Cannot dismiss alerts, must acknowledge */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(bottom = 48.dp)
                .safeDrawingPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(24.dp)
                    .background(
                        if (colors.isLcars) Color.Black else colors.surface,
                        if (colors.isLcars) RectangleShape else com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp))
                    )
            ) {
                if (colors.isLcars) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(LcarsOrange).padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isPeerNote) "NOTE FROM ${alert.sentBy.uppercase()}" else "SYSTEM ALERT",
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Column(
                    modifier = Modifier
                        .padding(if (colors.isLcars) 16.dp else 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!colors.isLcars) {
                    // Counter
                    Text(
                        text = "${viewModel.alertQueueIndex + 1} of ${viewModel.alertQueue.size}",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPeerNote) "\uD83D\uDCE8 Note from ${alert.sentBy}" else "\u26A0\uFE0F Alert",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message with basic markdown
                    Text(
                        text = formatAlertMessage(alert.message),
                        fontSize = 18.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meta (from + date)
                    val sentDate = try {
                        val instant = Instant.parse(alert.sentAt)
                        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                            .withZone(ZoneId.systemDefault())
                        formatter.format(instant)
                    } catch (e: Exception) {
                        alert.sentAt
                    }
                    Text(
                        text = "From ${alert.sentBy ?: "Admin"} \u2022 $sentDate",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Optional response or restriction message
                    val canReply = (alert.senderFolder != null)
                    
                    if (canReply) {
                        OutlinedTextField(
                            value = viewModel.alertResponse,
                            onValueChange = { viewModel.alertResponse = it },
                            placeholder = { Text("Optional response...", color = colors.textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                                cursorColor = AccentBlue
                            ),
                            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    } else if (isPeerNote) {
                        // This is a reply to the user's note
                        Text(
                            text = "Replies are restricted to one response. To continue the conversation, purchase a new note in the shop!",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOrange,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.textOrange.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))


                    // Acknowledge button
                    Button(
                        onClick = { viewModel.acknowledgeCurrentAlert() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isLcars) LcarsOrange
                                else if (colors.isRed) Color(0xFFCC0000)
                                else AccentBlue
                        ),
                        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            if (colors.isLcars) "ACKNOWLEDGE" else "\u2705 Acknowledge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (colors.isLcars) Color.Black else Color.White,
                            fontFamily = if (colors.isLcars) AntonioFontFamily else null
                        )
                    }
                } // end inner Column

                if (colors.isLcars) {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(LcarsTan))
                }
            }

            ConfettiBurst(
                trigger = showConfetti,
                onDone = { showConfetti = false }
            )
        }
    }
}

/**
 * Parses markdown into AnnotatedString.
 * Supports: **bold**, *italic*, _italic_, ~~strikethrough~~, `code`,
 * [link text](url), # headers, - bullet lists, 1. numbered lists, line breaks.
 */
fun formatAlertMessage(raw: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = raw.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append("\n")

            val trimmed = line.trimStart()

            // Headers: # ## ###
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("### "))
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("## "))
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        appendInlineMarkdown(trimmed.removePrefix("# "))
                    }
                }
                // Bullet list: - item or * item (but not **bold**)
                trimmed.startsWith("- ") || (trimmed.startsWith("* ") && !trimmed.startsWith("**")) -> {
                    append("  \u2022 ")
                    appendInlineMarkdown(trimmed.substringAfter(" ").trimStart())
                }
                // Numbered list: 1. item
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val num = trimmed.substringBefore(".")
                    append("  $num. ")
                    appendInlineMarkdown(trimmed.substringAfter(".").trimStart())
                }
                else -> {
                    appendInlineMarkdown(line)
                }
            }
        }
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, _italic_, ~~strikethrough~~,
 * `code`, [text](url)
 */
private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            // Bold: **text**
            i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end >= 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        appendInlineMarkdown(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Strikethrough: ~~text~~
            i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                val end = text.indexOf("~~", i + 2)
                if (end >= 0) {
                    withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                        appendInlineMarkdown(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code: `text`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end >= 0) {
                    withStyle(SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        background = Color(0x20808080)
                    )) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Link: [text](url)
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket >= 0 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen >= 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        withStyle(SpanStyle(
                            color = Color(0xFF4A9FE5),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )) {
                            append(linkText)
                        }
                        i = closeParen + 1
                    } else {
                        append(text[i])
                        i++
                    }
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic: *text* or _text_
            text[i] == '*' || text[i] == '_' -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end >= 0) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineMarkdown(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
