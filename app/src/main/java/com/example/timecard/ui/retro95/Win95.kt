package com.example.timecard.ui.retro95

import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Win95-era window kit for the Retro theme's "every screen is its own app" layouts (2026-11-20
 * mockups): bevels, window with title bar / menu bar, push buttons, sunken fields, block progress,
 * tabs, status bar, and a modal layer for message boxes. Screen-specific apps live with their feature.
 */
object W95 {
    val Face = Color(0xFFC0C0C0)
    val Light = Color(0xFFDFDFDF)
    val Shadow = Color(0xFF808080)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Navy = Color(0xFF000080)
    val TitleEnd = Color(0xFF1084D0)
    val Desktop = Color(0xFF008080)
    val Link = Color(0xFF0000EE)
    val Red = Color(0xFFC00000)
    val Green = Color(0xFF008000)
    val Purple = Color(0xFF800080)
    val Yellow = Color(0xFFFFFF00)
    val Phosphor = Color(0xFF39FF14)
}

/** Arial-like UI text (Android's sans), as Win95 apps drew it. */
fun w95Text(size: TextUnit, color: Color = W95.Black, weight: FontWeight = FontWeight.Normal, italic: Boolean = false) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = size, color = color, fontWeight = weight, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal)

/** Two-line Win95 bevel: white/black outside, light/dark gray inside; [inset] for sunken fields and pressed buttons. */
fun Modifier.win95Bevel(inset: Boolean = false, line: Dp = 1.dp): Modifier = drawBehind {
    val s = line.toPx()
    val w = size.width
    val h = size.height
    fun frame(i: Float, tl: Color, br: Color) {
        drawLine(tl, Offset(i, i + s / 2), Offset(w - i, i + s / 2), s)
        drawLine(tl, Offset(i + s / 2, i), Offset(i + s / 2, h - i), s)
        drawLine(br, Offset(i, h - i - s / 2), Offset(w - i, h - i - s / 2), s)
        drawLine(br, Offset(w - i - s / 2, i), Offset(w - i - s / 2, h - i), s)
    }
    frame(0f, if (inset) W95.Shadow else W95.White, if (inset) W95.White else W95.Black)
    frame(s, if (inset) W95.Black else W95.Light, if (inset) W95.Light else W95.Shadow)
}

/** Teal (or [color]) desktop the app windows sit on. */
@Composable
fun Win95Desktop(modifier: Modifier = Modifier, color: Color = W95.Desktop, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(color).padding(6.dp), content = content)
}

/**
 * An application window: gradient title bar (icon, title, _ □ ✕ where ✕ calls [onClose]),
 * optional menu bar (first letter underlined, for looks), then [content].
 */
@Composable
fun Win95Window(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    menus: List<String> = emptyList(),
    active: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.background(W95.Face).win95Bevel(line = 1.5.dp).padding(3.dp)) {
        Row(
            Modifier.fillMaxWidth().height(30.dp)
                .background(if (active) Brush.horizontalGradient(listOf(W95.Navy, W95.TitleEnd)) else Brush.horizontalGradient(listOf(W95.Shadow, Color(0xFFB5B5B5))))
                .padding(start = 5.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.invoke()
            Text(title, style = w95Text(15.sp, if (active) W95.White else W95.Light, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TitleButton("_", null)
            if (onClose != null) TitleButton("✕", onClose)
        }
        if (menus.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                menus.forEach { m ->
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(m.first()) }
                        append(m.drop(1))
                    }, style = w95Text(15.sp))
                }
            }
        }
        content()
    }
}

@Composable
private fun TitleButton(glyph: String, onClick: (() -> Unit)?) {
    Box(
        Modifier.size(width = 26.dp, height = 22.dp).background(W95.Face).win95Bevel()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = "Close", onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) { Text(glyph, style = w95Text(12.sp, weight = FontWeight.Bold)) }
}

/** Raised push button; sunken while [pressed] (toggle buttons, selected tabs). */
@Composable
fun Win95Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    enabled: Boolean = true,
    bold: Boolean = false,
    height: Dp = 34.dp,
    fontSize: TextUnit = 14.sp
) {
    Box(
        modifier.height(height).background(if (pressed) Color(0xFFD4D0C8) else W95.Face).win95Bevel(inset = pressed)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, style = w95Text(fontSize, if (enabled) W95.Black else W95.Shadow, if (bold) FontWeight.Bold else FontWeight.Normal), maxLines = 1) }
}

/** White sunken well (list boxes, text fields, preview panes). */
@Composable
fun Win95Sunken(modifier: Modifier = Modifier, color: Color = W95.White, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(color).win95Bevel(inset = true).padding(2.dp), content = content)
}

/** Win95 check box with an optional label. */
@Composable
fun Win95Check(checked: Boolean, label: String? = null, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier.then(if (onClick != null) Modifier.clickable(role = Role.Checkbox, onClickLabel = label, onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(20.dp).background(W95.White).win95Bevel(inset = true), contentAlignment = Alignment.Center) {
            if (checked) Text("✓", style = w95Text(15.sp, weight = FontWeight.Bold))
        }
        if (label != null) Text(label, style = w95Text(15.sp))
    }
}

/**
 * Block progress bar (navy blocks in a sunken well). Fills from 0 the first time it shows,
 * [delayMs] after composition, unless [animate] is false.
 */
@Composable
fun Win95Progress(fraction: Float, modifier: Modifier = Modifier, animate: Boolean = true, delayMs: Int = 0, blocks: Int = 20, color: Color = W95.Navy) {
    val target = fraction.coerceIn(0f, 1f)
    val shown = remember { Animatable(if (animate) 0f else target) }
    LaunchedEffect(target, animate) {
        if (!animate) { shown.snapTo(target); return@LaunchedEffect }
        kotlinx.coroutines.delay(delayMs.toLong())
        shown.animateTo(target, tween(900, easing = FastOutSlowInEasing))
    }
    Win95Sunken(modifier.height(22.dp)) {
        val lit = (shown.value * blocks).toInt()
        Row(Modifier.fillMaxSize().padding(1.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(blocks) { i -> Box(Modifier.weight(1f).fillMaxHeight().background(if (i < lit) color else Color.Transparent)) }
        }
    }
}

/** Property-sheet tabs: the selected tab is taller and joins the page below. */
@Composable
fun Win95Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        labels.forEachIndexed { i, l ->
            val on = i == selected
            Box(
                Modifier.height(if (on) 38.dp else 32.dp).background(if (on) W95.Face else Color(0xFFB0B0B0)).win95Bevel()
                    .clickable(enabled = !on, role = Role.Tab, onClickLabel = l) { onSelect(i) }.padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text(l, style = w95Text(15.sp, weight = if (on) FontWeight.Bold else FontWeight.Normal)) }
        }
    }
}

/** Status bar: sunken cells; a null width takes the remaining space. */
@Composable
fun Win95StatusBar(cells: List<Pair<String, Dp?>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(30.dp).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        cells.forEach { (text, width) ->
            Box(
                (if (width == null) Modifier.weight(1f) else Modifier.width(width)).fillMaxHeight().win95Bevel(inset = true).padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) { Text(text, style = w95Text(14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

/** A thin separator row (toolbar edge): white over gray. */
@Composable
fun Win95Rule(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(W95.Shadow))
        Box(Modifier.fillMaxWidth().height(1.dp).background(W95.White))
    }
}

/**
 * Modal layer over a screen (message boxes, "Get Info", Edit Profile): dims the app and pops the
 * window in; tapping outside calls [onDismiss].
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun BoxScope.Win95Modal(visible: Boolean, onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.matchParentSize()) {
        // imePadding: the window re-centers above the on-screen keyboard (Find Job, Edit Profile...).
        Box(Modifier.fillMaxSize().imePadding()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            )
            Box(Modifier.align(Alignment.Center).animateEnterExit(enter = scaleIn(initialScale = 0.8f)), content = content)
        }
    }
}

/** Big round "?" / "!" / "i" icon used by message boxes. */
@Composable
fun Win95MessageIcon(glyph: String, color: Color = W95.Navy) {
    Box(Modifier.size(38.dp).background(color, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
        Text(glyph, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = W95.White))
    }
}

/** Simple Win95 message box body: icon + text, then the buttons row. */
@Composable
fun Win95MessageBox(
    title: String,
    glyph: String,
    message: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit
) {
    Win95Window(title, modifier.width(420.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Win95MessageIcon(glyph)
            Column(Modifier.weight(1f)) { message() }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), content = buttons)
    }
}
