package com.example.timecard.ui.timesheet.standard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.timecard.kkcnav.KkcInterFontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.JetBrainsMonoFontFamily

// Building blocks of the standard layout, sized to KKC's rules: 44–48dp touch targets, 16dp card
// radius with a soft shadow, tonal secondary buttons, one filled primary per surface.

internal val CardShape = RoundedCornerShape(16.dp)
internal val CellShape = RoundedCornerShape(10.dp)
internal val PillShape = RoundedCornerShape(50)

/** Monospace for every hour figure, so columns of numbers line up. */
internal val HoursMono: FontFamily = JetBrainsMonoFontFamily

internal fun monoStyle(size: Int, color: Color, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = HoursMono, fontSize = size.sp, fontWeight = weight, color = color)

/** KKC's UI font (Inter, kkcnav/KkcNavTypography.kt) for everything in the standard layout. */
@Composable
internal fun StandardText(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = KkcInterFontFamily, fontWeight = FontWeight.Normal)),
        content = content
    )
}

/** White (surface) section card: 16dp radius, soft shadow, 16dp padding. */
@Composable
internal fun SectionCard(
    look: StandardLook,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, CardShape, ambientColor = look.shadow, spotColor = look.shadow)
            .clip(CardShape)
            .background(look.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** Card title row: bold title, muted subtitle on the same baseline, trailing actions. */
@Composable
internal fun SectionTitle(
    look: StandardLook,
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = look.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = look.text2, fontSize = 14.sp, maxLines = 1)
        Box(Modifier.weight(1f))
        trailing()
    }
}

/** Icon-only button, 44dp square tap target. [tonal] gives it the soft fill. */
@Composable
internal fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    look: StandardLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 44.dp
) {
    val tint = when {
        !enabled -> look.empty
        tonal -> look.softText
        else -> look.text
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (tonal) look.soft else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Pill button. Filled = the surface's one primary action; tonal = secondary; outlined = quiet. */
enum class PillKind { Filled, Tonal, Outlined, Dashed }

@Composable
internal fun PillButton(
    label: String,
    look: StandardLook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PillKind = PillKind.Tonal,
    icon: ImageVector? = null,
    outlineColor: Color? = null,
    height: Dp = 40.dp,
    fontSize: Int = 14,
    enabled: Boolean = true
) {
    val (bg, fg) = when (kind) {
        PillKind.Filled -> look.primary to look.onPrimary
        PillKind.Tonal -> look.soft to look.softText
        PillKind.Outlined -> Color.Transparent to (outlineColor ?: look.text)
        PillKind.Dashed -> Color.Transparent to look.softText
    }
    val border = when (kind) {
        PillKind.Outlined -> BorderStroke(1.5.dp, outlineColor ?: look.border)
        PillKind.Dashed -> BorderStroke(1.5.dp, look.border)
        else -> null
    }
    Row(
        modifier = modifier
            .height(height)
            .clip(PillShape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, PillShape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        Text(label, color = if (enabled) fg else look.empty, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Thin rounded progress bar. */
@Composable
internal fun ProgressBar(
    progress: Float,
    fill: Brush,
    track: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
            .background(track)
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .clip(PillShape)
                .background(fill)
        )
    }
}

/** Small status chip: a tinted pill with optional icon. */
@Composable
internal fun Chip(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outline: Color? = null
) {
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(if (outline != null) Modifier.border(1.5.dp, outline, RoundedCornerShape(8.dp)) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
