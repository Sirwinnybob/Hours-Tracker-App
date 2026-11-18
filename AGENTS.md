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
  - Retro → no glass bar. `kkcnav/KkcRetroNav.kt` is a Windows-95 taskbar (Start button, one task button per
    destination with badges, tray clock). The timesheet's `RetroFrame` carries it as `bottomBar`
    (`TimesheetScreen` `kkcRetroNavBar` param); the login screen (no frame) gets it standalone from `TimecardApp`.
    No animated theme swap: switching to/from Retro swaps the bars instantly.
  - LCARS → no glass bar. `kkcnav/KkcLcarsNav.kt`: the timesheet's `LcarsFrame` bottom band grows to
    `KKC_LCARS_NAV_HEIGHT` and holds the destination blocks (`TimesheetScreen` `kkcNavBar` param); the login screen
    (no frame) gets the standalone `KkcLcarsNavBand`.
  - Switching to/from LCARS while HT is open animates the navbar only (`KkcLcarsNav.kt` timings, driven by
    `TimecardApp`): glass bar slides down/fades, then LCARS blocks power up left→right; reverse retracts right→left
    then the glass bar slides up. Opening straight into a theme doesn't animate; low-end (`animDisabled`) is instant.
  - Bold-mode text/icon color is picked against Hours Tracker's own canvas (`kkcOnGlassColor`, `colors.backgroundTo`),
    not KKC's background — KKC's `frostedContent` would be wrong over HT's canvas (dark-mode readability bug).
- Landscape (HT decides itself, no contract field: `rememberKkcLandscapeRail`, KKC's `useLandscapeChrome` rule —
  landscape orientation AND window wider than tall): the glass bar becomes `kkcnav/KkcNavRail.kt`, a copy of KKC's
  bare `LandscapeNavRail` (`ui/components/LandscapeNavRail.kt` + `LandscapeChrome.kt`): one frosted rail on the right
  edge, clock + battery on top (`KkcRailSystemItems.kt` ← `SideHeaderClock`, `BatteryIndicator`), destinations in fixed
  62dp slots (Calc before Hours, SemiBold labels, Hours highlighted), Settings at the bottom (returns `settings`).
  Same `KkcNavBarStyle` colors as the portrait bar; no KKC update dot on Settings; charging bolt uses KKC's default
  status green. KKC's tool column (decorations) never appears here. The timesheet's standard layout pads its content
  `end = KkcLandscapeRailClearance` (88dp) while its background runs full width under the glass. Portrait is unchanged.
  - Retro landscape: the taskbar docks to the RIGHT edge (`KkcRetroNavRail`, `RetroFrame` `sideBar`; standalone on
    login): Start on top, task buttons stacked, Settings last, tray clock at the bottom.
  - LCARS landscape: `LcarsFrame` `rightSpineContent` adds a mirrored right spine (elbows joined to the header and
    footer bars) holding the blocks top → bottom (`KkcLcarsNavButtons(vertical = true)`); the footer returns to the
    20dp tan strip. Login gets a vertical standalone band. Blocks power up/retract top → bottom.
  - Retro/LCARS landscape slots = `kkcLandscapeSlots` (KKC order + Settings at the end, standing in for the rail's
    bottom Settings). KKC must accept a `settings` return tap (it is not in KKC's visible destination list).
- Taps call `KkcReturnNavigation.navigate`, which starts KKC `MainActivity` with `extra_kkc_nav_destination`
  (`CLEAR_TOP|SINGLE_TOP`) and finishes. Hours tap = no-op. The "← KKC" buttons stay as fallback.
- Any change on the KKC side must be copied here in the same session. Contract changes update both
  `KkcNavBarContract.kt` files and both `KkcNavBarContractTest` CANONICAL_KEYS lists, and bump `VERSION` in both repos.
