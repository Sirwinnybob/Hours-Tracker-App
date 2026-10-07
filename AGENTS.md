# Agent Instructions

This repository contains an Android application project.

## General
* Build the project using Gradle.
* The main application code is in `app/src/main/java`.
* The `build.gradle.kts` files contain the build configuration and dependencies.

## Coding Style
* Follow Kotlin coding conventions.
* Use Jetpack Compose for the UI.
* Keep composables clean and separate UI from business logic.

## Building and Testing
* Ensure your changes build successfully. You can use `./gradlew assembleDebug` to build the app.
* If you write tests, make sure they run successfully with `./gradlew testDebugUnitTest`.

## Submitting
* When committing changes, provide descriptive commit messages summarizing the bug fixes or feature additions.

## KKC navbar mirror
- When launched by KKCSheetTracker with `extra_kkc_navbar_version == 2`, `TimecardApp` overlays
  `kkcnav/KkcNavBar.kt`, a copy of KKC's bottom navbar (full state). Without a valid payload nothing changes.
- The bar floats over the FULL-height content (no bottom inset, like KKC); the content Box is the Haze source,
  so the glass blurs whatever is actually behind the bar. Do not re-add a bottom padding/inset.
- Source of truth is KKCSheetTracker (`C:\Scripts\KKCSheetTracker`): `ui/components/AppScaffold.kt`
  (`MorphingNavBar`, `MorphingNavIconRow`, `NavDestination`), `ui/components/icons/NavIcons.kt` + `IconDsl.kt`,
  `ui/theme/Type.kt` (`labelSmall`), `ui/theme/Spacing.kt`, `navigation/KkcNavBarContract.kt`.
- Layout, glass and animation always come from KKC. COLORS are chosen by `kkcnav/KkcNavBarStyle.kt`
  (Hours Tracker-only, no KKC counterpart), keyed on the equipped theme (`kkcNavBarSource(accentKey)`):
  - Standard HT theme (no immersive theme equipped) → KKC's colors. The payload carries BOTH KKC sets
    (`*_light` / `*_dark` extras); `TimecardApp` picks with `darkTheme = colors.isDark` — HT's EFFECTIVE mode,
    so the moon toggle re-colors live and immersive themes that force light/dark are respected.
  - Any equipped HT theme (Sunrise, Twilight/Midnight, Isle/Ocean, Daybreak, Red, Hacker, Sunset, Star Wars) →
    that theme's colors (`kkcNavColorsFromHt`: accent / textSecondary / hover pill / surface glass / error badge), never bold.
  - LCARS → no glass bar. `kkcnav/KkcLcarsNav.kt`: the timesheet's `LcarsFrame` bottom band grows to
    `KKC_LCARS_NAV_HEIGHT` and holds the destination blocks (`TimesheetScreen` `kkcNavBar` param); the login screen
    (no frame) gets the standalone `KkcLcarsNavBand`.
  - Bold-mode text/icon color is picked against Hours Tracker's own canvas (`kkcOnGlassColor`, `colors.backgroundTo`),
    not KKC's background — KKC's `frostedContent` would be wrong over HT's canvas (dark-mode readability bug).
- Taps call `KkcReturnNavigation.navigate`, which starts KKC `MainActivity` with `extra_kkc_nav_destination`
  (`CLEAR_TOP|SINGLE_TOP`) and finishes. Hours tap = no-op. The "← KKC" buttons stay as fallback.
- Any change on the KKC side must be copied here in the same session. Contract changes update both
  `KkcNavBarContract.kt` files and both `KkcNavBarContractTest` CANONICAL_KEYS lists, and bump `VERSION` in both repos.
