package com.example.timecard.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.timecard.R
import com.example.timecard.ui.theme.CoinAmber
import com.example.timecard.ui.theme.JetBrainsMonoFontFamily

import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.AntonioFontFamily

@Composable
fun CoinIcon(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Image(
        painter = painterResource(id = R.drawable.kk_coin),
        contentDescription = "Coin",
        modifier = modifier.size(size)
    )
}

@Composable
fun CoinAmount(
    amount: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = CoinAmber,
    iconSize: Dp? = null
) {
    val colors = LocalTimecardColors.current
    if (colors.isLcars) {
        Text(
            text = "$amount KK",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AntonioFontFamily,
            color = color,
            modifier = modifier
        )
    } else {
        val resolvedIconSize = iconSize ?: (fontSize.value + 4).dp
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            CoinIcon(size = resolvedIconSize)
            Spacer(Modifier.width(4.dp))
            Text(
                text = amount.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                color = color
            )
        }
    }
}
