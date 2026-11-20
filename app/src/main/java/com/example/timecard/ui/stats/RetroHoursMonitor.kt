package com.example.timecard.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.model.DAYS
import com.example.timecard.domain.StatsPeriod
import com.example.timecard.domain.StatsResult
import com.example.timecard.ui.retro95.W95
import com.example.timecard.ui.retro95.Win95Button
import com.example.timecard.ui.retro95.Win95Desktop
import com.example.timecard.ui.retro95.Win95MessageBox
import com.example.timecard.ui.retro95.Win95Modal
import com.example.timecard.ui.retro95.Win95StatusBar
import com.example.timecard.ui.retro95.Win95Sunken
import com.example.timecard.ui.retro95.Win95Window
import com.example.timecard.ui.retro95.w95Text
import com.example.timecard.ui.theme.VT323FontFamily
import com.example.timecard.ui.theme.rememberRetroMotion
import java.time.LocalDate
import java.util.Locale

/** Average hours per weekday (Mon..Sat) over the period's weeks. */
internal fun monitorDayAverages(stats: StatsResult?): List<Double> {
    if (stats == null) return List(6) { 0.0 }
    val weeks = stats.weekCount.coerceAtLeast(1)
    return DAYS.map { (stats.dailyTotals[it] ?: 0.0) / weeks }
}

/** Top jobs by hours (largest first), at most [limit]. */
internal fun monitorTopJobs(stats: StatsResult?, limit: Int = 6): List<Pair<String, Double>> =
    stats?.jobMap?.entries?.filter { it.value > 0 }?.sortedByDescending { it.value }?.take(limit)?.map { it.key to it.value } ?: emptyList()

/** A YYYY-MM-DD custom range is valid when both parse and start <= end. */
internal fun monitorRangeValid(start: String, end: String): Boolean {
    val s = runCatching { LocalDate.parse(start.trim()) }.getOrNull() ?: return false
    val e = runCatching { LocalDate.parse(end.trim()) }.getOrNull() ?: return false
    return !s.isAfter(e)
}

private fun h1(v: Double) = String.format(Locale.US, "%.1f", v)

private val PERIODS = listOf(StatsPeriod.ThisWeek, StatsPeriod.LastWeek, StatsPeriod.TwoWeeks, StatsPeriod.ThisMonth, StatsPeriod.LastMonth, StatsPeriod.AllTime)

/**
 * Retro Statistics as "Hours Monitor", a system-monitor style app (2026-11-20 mockup): period buttons,
 * green LED counters, a phosphor line of average hours per weekday against the targets, yellow bars for
 * the top jobs, and Find Job (the job search) in its own window.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RetroHoursMonitor(viewModel: StatsViewModel, onDismiss: () -> Unit) {
    val stats = viewModel.stats
    val motion = rememberRetroMotion()
    var customOpen by remember { mutableStateOf(false) }
    var findOpen by remember { mutableStateOf(false) }
    val period = viewModel.selectedPeriod
    val periodLabel = if (period is StatsPeriod.Custom) "${period.startDate} to ${period.endDate}" else period.label

    Win95Desktop {
        Win95Window("Hours Monitor", Modifier.fillMaxSize(), onClose = onDismiss, menus = listOf("File", "Edit", "View", "Options", "Help"), icon = { MonitorIcon() }) {
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PERIODS.forEach { p -> Win95Button(p.label, { viewModel.loadStats(p) }, pressed = period == p) }
                Win95Button("Custom...", { customOpen = true }, pressed = period is StatsPeriod.Custom)
                Win95Button("Find Job...", { findOpen = true })
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val total = stats?.totalHours ?: 0.0
                    val weeks = (stats?.weekCount ?: 0).coerceAtLeast(1)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Led("TOTAL HRS", h1(total), Modifier.weight(1f))
                        Led("SHOP HRS", h1(stats?.shopHours ?: 0.0), Modifier.weight(1f))
                        Led("JOBS", "${stats?.jobMap?.count { it.value > 0 } ?: 0}", Modifier.weight(1f))
                        Led("AVG / WEEK", h1(total / weeks), Modifier.weight(1f))
                    }
                    Text("Average hours per day — $periodLabel", style = w95Text(14.sp, weight = FontWeight.Bold))
                    Scope(monitorDayAverages(stats), key = periodLabel, animate = motion, modifier = Modifier.fillMaxWidth().height(300.dp))
                    Text("Top jobs", style = w95Text(14.sp, weight = FontWeight.Bold))
                    TopJobs(monitorTopJobs(stats), key = periodLabel, animate = motion)
                }
                Win95Modal(customOpen, { customOpen = false }) {
                    CustomRange(onCancel = { customOpen = false }, onOk = { s, e -> customOpen = false; viewModel.loadStats(StatsPeriod.Custom(s, e)) })
                }
                Win95Modal(findOpen, { findOpen = false }) {
                    FindJob(viewModel, onClose = { findOpen = false })
                }
            }
            Win95StatusBar(listOf("Monitoring $periodLabel" to null, "Total ${h1(stats?.totalHours ?: 0.0)} h" to 130.dp, "Weeks ${stats?.weekCount ?: 0}" to 96.dp))
        }
    }
}

@Composable
private fun MonitorIcon() {
    Box(Modifier.width(18.dp).height(16.dp).background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            val p = Path().apply { moveTo(0f, size.height * 0.7f); lineTo(size.width * 0.35f, size.height * 0.4f); lineTo(size.width * 0.6f, size.height * 0.6f); lineTo(size.width, size.height * 0.2f) }
            drawPath(p, W95.Phosphor, style = Stroke(1.5.dp.toPx()))
        }
    }
}

private val ledStyle = TextStyle(fontFamily = VT323FontFamily, fontSize = 36.sp, color = W95.Phosphor, textAlign = TextAlign.End)

@Composable
private fun Led(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = w95Text(13.sp, weight = FontWeight.Bold))
        Win95Sunken(Modifier.fillMaxWidth(), color = Color.Black) {
            Text(value, style = ledStyle, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        }
    }
}

/** Green phosphor line of the day averages over a scrolling grid, red dashed targets, drawn in on change. */
@Composable
private fun Scope(values: List<Double>, key: String, animate: Boolean, modifier: Modifier) {
    val draw = remember(key) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(key, animate) { if (animate) draw.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) else draw.snapTo(1f) }
    val scroll by rememberInfiniteTransition(label = "grid").animateFloat(0f, if (animate) 1f else 0f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "gridX")
    val targets = listOf(9.0, 9.0, 9.0, 9.0, 4.0, 0.0)
    Win95Sunken(modifier, color = Color.Black) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Average hours per day: " + values.joinToString { h1(it) } }) {
            val cell = 20.dp.toPx()
            val grid = Color(0xFF0A3A0A)
            var x = -scroll * cell
            while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f); x += cell }
            var y = 0f
            while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f); y += cell }
            val bottom = size.height - 26.dp.toPx()
            val top = 12.dp.toPx()
            fun px(i: Int) = size.width * (i + 0.5f) / 6f
            fun py(v: Double) = bottom - ((v / 12.0).coerceIn(0.0, 1.0) * (bottom - top)).toFloat()
            val target = Path().apply { targets.forEachIndexed { i, v -> if (i == 0) moveTo(px(i), py(v)) else lineTo(px(i), py(v)) } }
            drawPath(target, Color(0xFFFF2D2D), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))))
            // Draw the value line up to the animated fraction.
            val segs = values.size - 1
            val upTo = draw.value * segs
            val line = Path()
            values.forEachIndexed { i, v ->
                if (i == 0) line.moveTo(px(0), py(v))
                else if (i <= upTo) line.lineTo(px(i), py(v))
                else if (i - 1 < upTo) {
                    val f = upTo - (i - 1)
                    line.lineTo(px(i - 1) + (px(i) - px(i - 1)) * f, py(values[i - 1]) + (py(v) - py(values[i - 1])) * f)
                }
            }
            drawPath(line, W95.Phosphor.copy(alpha = 0.35f), style = Stroke(8.dp.toPx()))
            drawPath(line, W95.Phosphor, style = Stroke(3.dp.toPx()))
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 2.dp)) {
            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT").forEach {
                Text(it, style = TextStyle(fontFamily = VT323FontFamily, fontSize = 17.sp, color = W95.Phosphor, textAlign = TextAlign.Center), modifier = Modifier.weight(1f))
            }
        }
        Text("12h", style = TextStyle(fontFamily = VT323FontFamily, fontSize = 16.sp, color = W95.Phosphor), modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
    }
}

@Composable
private fun TopJobs(jobs: List<Pair<String, Double>>, key: String, animate: Boolean) {
    val max = jobs.maxOfOrNull { it.second } ?: 1.0
    Win95Sunken(Modifier.fillMaxWidth(), color = Color.Black) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (jobs.isEmpty()) Text("NO JOBS IN THIS PERIOD", style = TextStyle(fontFamily = VT323FontFamily, fontSize = 20.sp, color = W95.Yellow))
            jobs.forEachIndexed { i, (job, hours) ->
                val grow = remember(key) { Animatable(if (animate) 0f else 1f) }
                LaunchedEffect(key, animate) { if (animate) { kotlinx.coroutines.delay(i * 120L); grow.animateTo(1f, tween(800)) } else grow.snapTo(1f) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val vt = TextStyle(fontFamily = VT323FontFamily, fontSize = 21.sp, color = W95.Yellow)
                    Text(job.uppercase(), style = vt, maxLines = 1, modifier = Modifier.width(150.dp))
                    Box(Modifier.weight(1f).height(18.dp)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((hours / max).toFloat().coerceIn(0.02f, 1f) * grow.value).background(W95.Yellow))
                    }
                    Text(h1(hours), style = vt.copy(textAlign = TextAlign.End), modifier = Modifier.width(64.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomRange(onCancel: () -> Unit, onOk: (String, String) -> Unit) {
    var start by remember { mutableStateOf(LocalDate.now().minusDays(30).toString()) }
    var end by remember { mutableStateOf(LocalDate.now().toString()) }
    val ok = monitorRangeValid(start, end)
    Win95MessageBox("Custom Range", "?", message = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enter dates as YYYY-MM-DD.", style = w95Text(15.sp))
            DateField("From:", start) { start = it }
            DateField("To:", end) { end = it }
            if (!ok) Text("Check the dates.", style = w95Text(14.sp, W95.Red))
        }
    }) {
        Win95Button("OK", { onOk(start.trim(), end.trim()) }, Modifier.width(96.dp), enabled = ok, bold = true)
        Win95Button("Cancel", onCancel, Modifier.width(96.dp))
    }
}

@Composable
private fun DateField(label: String, value: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = w95Text(15.sp), modifier = Modifier.width(50.dp))
        Win95Sunken(Modifier.weight(1f).height(34.dp)) {
            BasicTextField(value, onChange, singleLine = true, textStyle = w95Text(16.sp), cursorBrush = SolidColor(W95.Black), modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart).padding(horizontal = 6.dp))
        }
    }
}

/** Find Job window: the existing job search (StatsViewModel.searchJob) with Win95 results list. */
@Composable
private fun FindJob(viewModel: StatsViewModel, onClose: () -> Unit) {
    Win95Window("Find Job", Modifier.width(470.dp), onClose = onClose) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Job:", style = w95Text(15.sp))
                Win95Sunken(Modifier.weight(1f).height(36.dp)) {
                    BasicTextField(
                        viewModel.searchQuery, { viewModel.searchQuery = it }, singleLine = true, textStyle = w95Text(16.sp), cursorBrush = SolidColor(W95.Black),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { viewModel.searchJob() }),
                        modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart).padding(horizontal = 6.dp).semantics { contentDescription = "Job to find" }
                    )
                }
                Win95Button("Find Now", { viewModel.searchJob() }, bold = true)
            }
            Win95Sunken(Modifier.fillMaxWidth().height(300.dp)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().background(W95.Face).padding(horizontal = 6.dp, vertical = 4.dp)) {
                        Text("Week", style = w95Text(14.sp, weight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        Text("Hours", style = w95Text(14.sp, weight = FontWeight.Bold))
                    }
                    viewModel.searchResults.forEach { r ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp)) {
                            Text(r.date, style = w95Text(15.sp), modifier = Modifier.weight(1f))
                            Text(h1(r.hours), style = w95Text(15.sp))
                        }
                    }
                    if (viewModel.searchQuery.isNotBlank() && viewModel.searchResults.isEmpty()) {
                        Text("No weeks found.", style = w95Text(15.sp, W95.Shadow, italic = true), modifier = Modifier.padding(8.dp))
                    }
                }
            }
            Text("Total: ${h1(viewModel.searchTotalHours)} hours", style = w95Text(15.sp, weight = FontWeight.Bold))
        }
    }
}
