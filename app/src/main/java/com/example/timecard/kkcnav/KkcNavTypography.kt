package com.example.timecard.kkcnav

// ═══════════════════════════════════════════════════════════════════════════════════════════
// KEEP IN SYNC — THIS IS A COPY OF KKCSHEETTRACKER'S BOTTOM NAVBAR
// Source of truth: C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\
//   ui/components/AppScaffold.kt   MorphingNavBar + MorphingNavIconRow (full state) → KkcNavBar.kt
//   ui/components/AppScaffold.kt   enum NavDestination labels / badge + tint rules → KkcNavBarModel.kt
//   ui/components/icons/NavIcons.kt + IconDsl.kt  → KkcNavIcons.kt + KkcIconDsl.kt (verbatim copies)
//   ui/theme/Type.kt  InterFontFamily + labelSmall → KkcNavTypography.kt
//   ui/theme/Spacing.kt  navBarHorizontal 24dp, floatingNavMinSideMargin 24dp, floatingNavBottomGap 12dp
//   navigation/KkcNavBarContract.kt + KkcNavBarPayloadBuilder.kt → KkcNavBarContract.kt
// Layout, glass and animation are KKC's; colors are picked by KkcNavBarStyle.kt (HT-only); LCARS → KkcLcarsNav.kt.
// Any change to the KKC files above must be copied here in the same session; any contract
// change bumps VERSION in both repos. Full rules: Hours Tracker AGENTS.md "KKC navbar mirror".
// Fallback "← KKC" buttons (TimesheetScreen.kt, NameCard.kt) are intentionally kept.
// ═══════════════════════════════════════════════════════════════════════════════════════════

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
