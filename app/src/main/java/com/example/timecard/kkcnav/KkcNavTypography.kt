package com.example.timecard.kkcnav

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

// Copy of KKCSheetTracker ui/theme/Type.kt InterFontFamily + KKCTypography.labelSmall.
private val kkcFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.example.timecard.R.array.com_google_android_gms_fonts_certs
)

private val kkcInterFont = GoogleFont("Inter")

internal val KkcInterFontFamily = FontFamily(
    Font(googleFont = kkcInterFont, fontProvider = kkcFontProvider, weight = FontWeight.Normal),
    Font(googleFont = kkcInterFont, fontProvider = kkcFontProvider, weight = FontWeight.Medium),
    Font(googleFont = kkcInterFont, fontProvider = kkcFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = kkcInterFont, fontProvider = kkcFontProvider, weight = FontWeight.Bold)
)

internal val KkcNavLabelStyle = TextStyle(
    fontFamily = KkcInterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)
