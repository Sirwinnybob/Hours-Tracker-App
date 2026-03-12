package com.example.timecard.ui.timesheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.ui.theme.ErrorRed
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.SaveGreen
import com.example.timecard.ui.theme.SyncingBlue

@Composable
fun ControlsBar(
    saveStatus: SaveStatus,
    onStats: () -> Unit,
    onLogout: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimecardColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Button(
            onClick = onStats,
            colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
            modifier = Modifier.weight(1f).height(44.dp)
        ) {
            Text("STATS", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
            modifier = Modifier.weight(1f).height(44.dp)
        ) {
            Text("LOGOUT", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (saveStatus) {
                    SaveStatus.SAVED -> SaveGreen
                    SaveStatus.SYNCING -> SyncingBlue
                    SaveStatus.ERROR -> ErrorRed
                }
            ),
            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
            modifier = Modifier.weight(1.5f).height(44.dp)
        ) {
            Text(
                text = when (saveStatus) {
                    SaveStatus.SAVED -> "SAVED"
                    SaveStatus.SYNCING -> "SAVING..."
                    SaveStatus.ERROR -> "ERROR"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
