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
- When launched by KKCSheetTracker with `extra_kkc_navbar_version == 1`, `TimecardApp` overlays
  `kkcnav/KkcNavBar.kt`, a copy of KKC's bottom navbar (full state). Without a valid payload nothing changes.
- Source of truth is KKCSheetTracker (`C:\Scripts\KKCSheetTracker`): `ui/components/AppScaffold.kt`
  (`MorphingNavBar`, `MorphingNavIconRow`, `NavDestination`), `ui/components/icons/NavIcons.kt` + `IconDsl.kt`,
  `ui/theme/Type.kt` (`labelSmall`), `ui/theme/Spacing.kt`, `navigation/KkcNavBarContract.kt`.
- Never restyle `KkcNavBar` from Hours Tracker's theme — every color comes from `KkcNavBarPayload`.
- Taps call `KkcReturnNavigation.navigate`, which starts KKC `MainActivity` with `extra_kkc_nav_destination`
  (`CLEAR_TOP|SINGLE_TOP`) and finishes. Hours tap = no-op. The "← KKC" buttons stay as fallback.
- Any change on the KKC side must be copied here in the same session. Contract changes update both
  `KkcNavBarContract.kt` files and both `KkcNavBarContractTest` CANONICAL_KEYS lists, and bump `VERSION` in both repos.
