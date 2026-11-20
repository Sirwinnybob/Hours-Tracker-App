package com.example.timecard.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Check
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95Progress
import com.example.timecard.ui.retro95.Win95Rule
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.RetroIcon
import com.example.timecard.ui.theme.RetroPixelIcon
import com.example.timecard.ui.theme.rememberRetroMotion

/** Installer wording for a challenge's progress. */
internal fun setupState(progress: Double, complete: Boolean): String = when {
    complete -> "Installed"
    progress > 0.0 -> "Installing... ${(progress.coerceIn(0.0, 1.0) * 100).toInt()}%"
    else -> "Not started"
}

/** KK already installed (completed rewards) and still available this week. */
internal fun setupTotals(items: List<ChallengeProgress>): Pair<Int, Int> {
    val earned = items.filter { it.isComplete }.sumOf { it.challenge.reward }
    return earned to items.sumOf { it.challenge.reward } - earned
}

/**
 * Retro Targets as "Weekly Challenges Setup", an installer wizard (2026-11-20 mockup): each challenge
 * is a component with a check box (done), its KK reward and a block progress bar; the selected one's
 * description shows below. Next / Cancel close it.
 */
@Composable
internal fun RetroChallengesSetup(viewModel: ChallengesViewModel, onLoad: () -> Unit, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { onLoad() }
    val motion = rememberRetroMotion()
    val items = viewModel.challengeProgress
    var selected by remember { mutableIntStateOf(0) }
    val (earned, left) = setupTotals(items)

    Win95Desktop(color = Color(0xFF000055)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0000AA), Color(0xFF000033))))) {
            Text(
                "Weekly Challenges Setup",
                style = TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = W95.White),
                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
            )
            Win95Window("Weekly Challenges Setup", Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 70.dp, bottom = 24.dp), onClose = onDismiss) {
                Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Wizard banner
                    Column(
                        Modifier.width(150.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(W95.Desktop, W95.Navy))).border(1.dp, W95.Shadow).padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        RetroPixelIcon(RetroIcon.Target, size = 72.dp, bounce = motion)
                        Text("THIS WEEK'S\nTARGETS", style = w95Text(18.sp, W95.White, FontWeight.Bold).copy(textAlign = TextAlign.Center))
                        Text("Finish a target and Setup installs the KK reward for you.", style = w95Text(13.sp, W95.White).copy(textAlign = TextAlign.Center))
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select Components", style = w95Text(17.sp, weight = FontWeight.Bold))
                        Text("Complete these components to install your Kustom Kash rewards:", style = w95Text(15.sp))
                        Win95Sunken(Modifier.weight(1f).fillMaxWidth()) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(4.dp)) {
                                if (viewModel.isLoading && items.isEmpty()) Text("Preparing to install...", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(8.dp))
                                if (!viewModel.isLoading && items.isEmpty()) Text("No challenges this week.", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(8.dp))
                                items.forEachIndexed { i, cp ->
                                    val sel = i == selected
                                    val fg = if (sel) W95.White else W95.Black
                                    Column(
                                        Modifier.fillMaxWidth().background(if (sel) W95.Navy else Color.Transparent)
                                            .clickable(role = Role.Button, onClickLabel = cp.challenge.title) { selected = i }.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Win95Check(cp.isComplete)
                                            Text(cp.challenge.title, style = w95Text(16.sp, fg, FontWeight.Bold), modifier = Modifier.weight(1f))
                                            Text("+${cp.challenge.reward} KK", style = w95Text(15.sp, fg))
                                        }
                                        Row(Modifier.padding(start = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Win95Progress(cp.progress.toFloat(), Modifier.weight(1f), animate = motion, delayMs = 200 + i * 250)
                                            Text(
                                                setupState(cp.progress, cp.isComplete),
                                                style = w95Text(14.sp, when { sel -> W95.White; cp.isComplete -> W95.Green; cp.progress > 0 -> W95.Navy; else -> W95.Shadow }, if (cp.isComplete) FontWeight.Bold else FontWeight.Normal),
                                                modifier = Modifier.width(130.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().border(1.dp, W95.Shadow).padding(8.dp)) {
                            Text("Description: " + (items.getOrNull(selected)?.challenge?.description ?: ""), style = w95Text(15.sp))
                        }
                        Row { Text("KK installed this week:", style = w95Text(15.sp), modifier = Modifier.weight(1f)); Text("$earned KK", style = w95Text(15.sp, weight = FontWeight.Bold)) }
                        Row { Text("KK still available:", style = w95Text(15.sp), modifier = Modifier.weight(1f)); Text("$left KK", style = w95Text(15.sp, weight = FontWeight.Bold)) }
                    }
                }
                Win95Rule(Modifier.padding(horizontal = 12.dp))
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    Win95Button("< Back", { selected = (selected - 1).coerceAtLeast(0) }, Modifier.width(100.dp), enabled = selected > 0)
                    Win95Button(if (selected < items.lastIndex) "Next >" else "Finish", { if (selected < items.lastIndex) selected++ else onDismiss() }, Modifier.width(100.dp), bold = true)
                    Win95Button("Cancel", onDismiss, Modifier.width(100.dp).padding(start = 8.dp))
                }
            }
        }
    }
}
