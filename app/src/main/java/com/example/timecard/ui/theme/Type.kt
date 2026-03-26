package com.example.timecard.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import com.example.timecard.R

val VT323FontFamily = FontFamily(
    Font(R.font.vt323, FontWeight.Normal),
    Font(R.font.vt323, FontWeight.Bold) // VT323 often only has regular, but compose handles fallback if bold is missing
)

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val AntonioFontFamily = FontFamily(
    Font(R.font.antonio, FontWeight.Bold)
)

val TimecardTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

// LCARS typography: Antonio condensed sans — authentic Star Trek LCARS font
val LcarsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = 1.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = 1.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 0.8.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AntonioFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp
    )
)

val TerminalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp // VT323 runs tight, bumping these up slightly
    ),
    bodyMedium = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VT323FontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
