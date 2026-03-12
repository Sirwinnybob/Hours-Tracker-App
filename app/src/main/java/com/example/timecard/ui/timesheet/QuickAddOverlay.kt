package com.example.timecard.ui.timesheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.LocalTimecardColors

@Composable
fun QuickAddOverlay(
    initialValue: Double,
    onValueChange: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTimecardColors.current
    var currentValue by remember { mutableDoubleStateOf(initialValue) }

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(16.dp)))
                .background(colors.surface)
                .padding(16.dp)
        ) {
            // Close button
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(4.dp)))
                        .background(colors.hover)
                ) {
                    Text("\u2715", color = colors.textSecondary, fontSize = 14.sp)
                }
            }

            // Current value display
            Text(
                text = String.format("%.2f", currentValue),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily,
                color = colors.textTotal
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                QuickAddButton("-0.25", colors.textOrange) {
                    currentValue = maxOf(0.0, currentValue - 0.25)
                    currentValue = Math.round(currentValue * 100.0) / 100.0
                    onValueChange(currentValue)
                }
                QuickAddButton("+0.25", colors.accent) {
                    currentValue += 0.25
                    currentValue = Math.round(currentValue * 100.0) / 100.0
                    onValueChange(currentValue)
                }
                QuickAddButton("+0.5", colors.accent) {
                    currentValue += 0.5
                    currentValue = Math.round(currentValue * 100.0) / 100.0
                    onValueChange(currentValue)
                }
                QuickAddButton("+1", colors.accent) {
                    currentValue += 1.0
                    currentValue = Math.round(currentValue * 100.0) / 100.0
                    onValueChange(currentValue)
                }
                QuickAddButton("+1.5", colors.accent) {
                    currentValue += 1.5
                    currentValue = Math.round(currentValue * 100.0) / 100.0
                    onValueChange(currentValue)
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalTimecardColors.current
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.input),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier
    ) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.material3.MaterialTheme.typography.labelLarge.fontFamily)
    }
}
