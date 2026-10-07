package com.example.timecard

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.timecard.kkcnav.KkcLcarsNavBand
import com.example.timecard.kkcnav.KkcNavBar
import com.example.timecard.kkcnav.KkcNavBarPayload
import com.example.timecard.kkcnav.KkcNavBarSource
import com.example.timecard.kkcnav.KkcReturnNavigation
import com.example.timecard.kkcnav.kkcNavBarSource
import com.example.timecard.kkcnav.kkcNavBarStyle
import com.example.timecard.kkcnav.kkcNavColorsFromHt
import com.example.timecard.kkcnav.KKC_GLASS_SWAP_MS
import com.example.timecard.kkcnav.KkcLcarsBlockAnim
import com.example.timecard.kkcnav.KkcNavBarStyle
import com.example.timecard.kkcnav.kkcLcarsEnterTotalMs
import com.example.timecard.kkcnav.kkcLcarsRetractTotalMs
import com.example.timecard.kkcnav.kkcNavSlots
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.SideEffect
import androidx.compose.material3.MaterialTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.example.timecard.ui.login.HeaderMetrics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.zIndex
import com.example.timecard.data.model.Employee
import com.example.timecard.ui.challenges.ChallengesViewModel
import com.example.timecard.ui.components.VideoSplash
import com.example.timecard.ui.alerts.AlertsViewModel
import com.example.timecard.ui.login.LoginViewModel
import com.example.timecard.ui.login.NameCard
import com.example.timecard.ui.profile.BadgePopup
import com.example.timecard.ui.profile.ConfettiBurst
import com.example.timecard.ui.profile.CoinBanner
import com.example.timecard.ui.profile.LeaderboardViewModel
import com.example.timecard.ui.profile.ProfileViewModel
import com.example.timecard.ui.profile.RecordToast
import com.example.timecard.ui.shop.ShopViewModel
import com.example.timecard.ui.stats.StatsViewModel

import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.ui.theme.TimecardTheme
import com.example.timecard.ui.theme.accentColorFor
import com.example.timecard.ui.timesheet.TimesheetScreen
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.timecard.ui.timesheet.TimesheetViewModel
import kotlinx.coroutines.launch

@Composable
fun TimecardApp(
    themeState: ThemeState,
    autoLoginInput: String? = null,
    jobNumberInput: String? = null,
    hoursInput: Double? = null,
    launchedByKkc: Boolean = false,
    kkcNavBar: KkcNavBarPayload? = null,
    onReinstallLatest: () -> Unit
) {
    // ViewModels must be instantiated before TimecardTheme so we can read
    // the profile's accent color and pass it into the theme.
    val loginViewModel: LoginViewModel = viewModel()
    val timesheetViewModel: TimesheetViewModel = viewModel()
    val alertsViewModel: AlertsViewModel = viewModel()
    val statsViewModel: StatsViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val leaderboardViewModel: LeaderboardViewModel = viewModel()
    val challengesViewModel: ChallengesViewModel = viewModel()
    val shopViewModel: ShopViewModel = viewModel()


    // Pass the raw accent string to activate immersive themes.
    // If a theme trial is active in the shop, it temporarily overrides the stored accent.
    val accentKey = shopViewModel.previewAccentKey ?: profileViewModel.profile.accentColor

    TimecardTheme(themeMode = themeState.mode, accentKey = accentKey) {
        val tsState by timesheetViewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val colors = LocalTimecardColors.current
        val kkcHazeState = remember { HazeState() }
        // KKC navbar styling (kkcnav/KkcNavBarStyle.kt): standard HT theme → KKC's colors in HT's
        // effective light/dark; equipped HT theme → that theme's colors; LCARS → LCARS band.
        val kkcNavSource = kkcNavBarSource(accentKey)
        val kkcOnNavigate: (String) -> Unit = { route ->
            (context as? Activity)?.let { KkcReturnNavigation.navigate(it, route, kkcNavBar?.animDisabled == true) }
        }
        // Normal ↔ LCARS navbar transition (kkcnav/KkcLcarsNav.kt timings). Only a CHANGE of theme while
        // Hours Tracker is open animates; opening straight into LCARS shows the band as-is.
        val kkcIsLcars = kkcNavSource == KkcNavBarSource.Lcars
        val kkcAnimate = kkcNavBar != null && !kkcNavBar.animDisabled
        val kkcSlotCount = kkcNavBar?.let { kkcNavSlots(it.destinations).size } ?: 0
        var kkcWasLcars by remember { mutableStateOf(kkcIsLcars) }
        var kkcLcarsAnim by remember { mutableStateOf(KkcLcarsBlockAnim.None) }
        var kkcLcarsRetracting by remember { mutableStateOf(false) }
        LaunchedEffect(kkcIsLcars) {
            if (kkcIsLcars == kkcWasLcars) return@LaunchedEffect
            kkcWasLcars = kkcIsLcars
            if (!kkcAnimate) {
                kkcLcarsAnim = KkcLcarsBlockAnim.None
                kkcLcarsRetracting = false
            } else if (kkcIsLcars) {
                kkcLcarsRetracting = false
                kkcLcarsAnim = KkcLcarsBlockAnim.Enter
                kotlinx.coroutines.delay(kkcLcarsEnterTotalMs(kkcSlotCount).toLong())
                // Settled: a later recomposition of the frame (e.g. re-login) must not replay it.
                kkcLcarsAnim = KkcLcarsBlockAnim.None
            } else {
                kkcLcarsAnim = KkcLcarsBlockAnim.None
                kkcLcarsRetracting = true
                kotlinx.coroutines.delay(kkcLcarsRetractTotalMs(kkcSlotCount).toLong())
                kkcLcarsRetracting = false
            }
        }
        // The LaunchedEffect above only runs AFTER the frame where the theme flipped. Derive the
        // transition for that very first frame here, so the LCARS blocks compose already at 0 (Enter)
        // and the glass bar never flickers back in before the retract starts.
        val kkcThemeJustFlipped = kkcIsLcars != kkcWasLcars && kkcAnimate
        val kkcLcarsAnimNow = if (kkcThemeJustFlipped && kkcIsLcars) KkcLcarsBlockAnim.Enter else kkcLcarsAnim
        val kkcRetractingNow = kkcLcarsRetracting || (kkcThemeJustFlipped && !kkcIsLcars)
        // The glass bar keeps its last non-LCARS look while it slides out, so it doesn't flash
        // LCARS-theme colors on the way down.
        var kkcLastGlassStyle by remember { mutableStateOf<KkcNavBarStyle?>(null) }

        var showSplash by remember { mutableStateOf(autoLoginInput == null && !launchedByKkc) }
        var loggedInEmployee by remember { mutableStateOf<Employee?>(null) }
        // Start collapsed when launched by KKC so the NameCard entry animation is skipped
        var isExpanded by remember { mutableStateOf(autoLoginInput == null) }

        // Detect app foreground — trigger inactivity logout if the background
        // pause caused the coroutine timer to be suspended.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    timesheetViewModel.checkInactivityOnResume()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val timesheetAlpha = remember { Animatable(0f) }
        val gradientAlpha = remember { Animatable(1f) }
        var nameMetrics by remember { mutableStateOf<HeaderMetrics?>(null) }

        // Splash screen state

        LaunchedEffect(autoLoginInput, loginViewModel.employees) {
            val input = autoLoginInput?.trim() ?: return@LaunchedEffect
            if (input.isEmpty()) return@LaunchedEffect
            if (loginViewModel.employees.isEmpty()) {
                loginViewModel.initialize(context)
                return@LaunchedEffect
            }
            val employee = loginViewModel.employees.find { it.id == input }
                ?: loginViewModel.employees.find { it.name.equals(input, ignoreCase = true) }
            if (employee != null) {
                loginViewModel.loginInput = employee.name
                loggedInEmployee = employee
                isExpanded = false
            } else {
                isExpanded = true  // show login form if name not recognized
            }
        }

        LaunchedEffect(isExpanded, loggedInEmployee) {
            if (!isExpanded && loggedInEmployee != null) {
                if (autoLoginInput != null) {
                    // KKC launch: NameCard already hidden, show timesheet immediately
                    timesheetAlpha.snapTo(1f)
                    gradientAlpha.snapTo(0f)
                } else {
                    kotlinx.coroutines.delay(600)
                    launch { timesheetAlpha.animateTo(1f, tween(700)) }
                    launch { gradientAlpha.animateTo(0f, tween(600)) }
                }
            }
        }

        LaunchedEffect(loggedInEmployee) {
            val emp = loggedInEmployee
            if (emp != null) {
                val repo = loginViewModel.getRepository(context)
                val effectiveDisplayName = emp.displayName.ifBlank { emp.name }
                profileViewModel.initialize(emp.id, effectiveDisplayName, repo)
                shopViewModel.initialize(repo, profileViewModel)
                alertsViewModel.onAcknowledged = { profileViewModel.onAlertAcknowledged() }
            } else {
                profileViewModel.logout()
                alertsViewModel.onAcknowledged = null
            }
        }

        var prefillDone by remember { mutableStateOf(false) }
        LaunchedEffect(jobNumberInput, tsState.isDataLoaded, tsState.isLockedByAnotherUser) {
            if (jobNumberInput == null || prefillDone) return@LaunchedEffect
            if (!tsState.isDataLoaded) return@LaunchedEffect
            if (tsState.isLockedByAnotherUser) return@LaunchedEffect
            timesheetViewModel.prefillJobEntry(jobNumberInput, hoursInput ?: 0.0)
            prefillDone = true
        }

        LaunchedEffect(tsState.lastSavedData) {
            val saved = tsState.lastSavedData ?: return@LaunchedEffect
            val dates = timesheetViewModel.getAvailableDates()
            profileViewModel.onTimecardSaved(saved, dates)
        }

        LaunchedEffect(tsState.triggerAutoLogout) {
            if (tsState.triggerAutoLogout) {
                launch { timesheetAlpha.animateTo(0f, tween(300)) }
                launch { gradientAlpha.animateTo(1f, tween(300)) }
                kotlinx.coroutines.delay(300)

                isExpanded = true
                loggedInEmployee = null
                timesheetViewModel.logout()
                alertsViewModel.logout()
                statsViewModel.logout()
                timesheetViewModel.resetAutoLogout()

                android.widget.Toast.makeText(context, "Logged out due to inactivity", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        LaunchedEffect(profileViewModel.triggerAutoLogout) {
            if (profileViewModel.triggerAutoLogout) {
                launch { timesheetAlpha.animateTo(0f, tween(300)) }
                launch { gradientAlpha.animateTo(1f, tween(300)) }
                kotlinx.coroutines.delay(300)

                isExpanded = true
                loggedInEmployee = null
                timesheetViewModel.logout()
                alertsViewModel.logout()
                statsViewModel.logout()
                profileViewModel.resetAutoLogout()

                android.widget.Toast.makeText(context, "Logged out due to inactivity", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        LaunchedEffect(profileViewModel.isLockedByAnotherUser) {
            if (profileViewModel.isLockedByAnotherUser) {
                launch { timesheetAlpha.animateTo(0f, tween(300)) }
                launch { gradientAlpha.animateTo(1f, tween(300)) }
                kotlinx.coroutines.delay(300)

                isExpanded = true
                loggedInEmployee = null
                timesheetViewModel.logout()
                alertsViewModel.logout()
                statsViewModel.logout()
                profileViewModel.resetLockError()

                android.widget.Toast.makeText(context, "This profile is open on another device", android.widget.Toast.LENGTH_LONG).show()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (kkcNavBar != null) Modifier.hazeSource(kkcHazeState) else Modifier)
                .background(colors.landingBrush)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed }) {
                                timesheetViewModel.refreshInteraction()
                                profileViewModel.interact()
                            }
                        }
                    }
                }
                // No bottom inset: like KKC, content runs full height and the bar floats over it;
                // hazeSource above makes the glass blur the actual content behind the bar.
        ) {
            if (!showSplash) {
                // Layer 1: Timesheet
            val employee = loggedInEmployee
            if (employee != null) {
                val repository = remember(employee.name) { loginViewModel.getRepository(context) }
                val effectiveDisplayName = employee.displayName.ifBlank { employee.name }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = timesheetAlpha.value }) {
                    TimesheetScreen(
                        employeeId = employee.id,
                        employeeDisplayName = effectiveDisplayName,
                        repository = repository,
                        themeState = themeState,
                        launchedByKkc = launchedByKkc,
                        kkcNavBar = if (kkcNavSource == KkcNavBarSource.Lcars) kkcNavBar else null,
                        onKkcNavigate = kkcOnNavigate,
                        kkcLcarsAnim = kkcLcarsAnimNow,
                        timesheetViewModel = timesheetViewModel,
                        alertsViewModel = alertsViewModel,
                        statsViewModel = statsViewModel,
                        profileViewModel = profileViewModel,
                        leaderboardViewModel = leaderboardViewModel,
                        challengesViewModel = challengesViewModel,
                        shopViewModel = shopViewModel,
                        employees = loginViewModel.employees,
                        onLogout = {
                            scope.launch {
                                launch { timesheetAlpha.animateTo(0f, tween(300)) }
                                launch { gradientAlpha.animateTo(1f, tween(300)) }
                                kotlinx.coroutines.delay(300)
                                isExpanded = true
                                timesheetViewModel.logout()
                                alertsViewModel.logout()
                                statsViewModel.logout()
                                loginViewModel.loginInput = ""
                            }
                        },
                        onNameMeasured = { m -> nameMetrics = m }
                    )
                }
            }

            // Layer 2: Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = gradientAlpha.value }
                    .background(colors.landingBrush)
            )

            // Layer 3b: Gamification overlays

            ConfettiBurst(
                trigger = profileViewModel.pendingConfetti,
                onDone = { profileViewModel.dismissConfetti() }
            )
            BadgePopup(
                badgeId = profileViewModel.pendingBadges.firstOrNull(),
                badgeImages = profileViewModel.badgeImages,
                onDismiss = { profileViewModel.dismissNextBadge() }
            )
            CoinBanner(
                coinsEarned = profileViewModel.recentCoinsEarned,
                streakBonus = profileViewModel.recentStreakBonus,
                streakMultiplier = profileViewModel.recentStreakMultiplier,
                onDismiss = { profileViewModel.dismissCoinsEarned() }
            )
            RecordToast(
                message = profileViewModel.newRecordMessage,
                onDismiss = { profileViewModel.dismissRecord() }
            )

            // Layer 3: NameCard overlay
            NameCard(
                viewModel = loginViewModel,
                themeState = themeState,
                isExpanded = isExpanded,
                employeeName = loggedInEmployee?.name,
                displayName = profileViewModel.profile.displayName,
                avatar = profileViewModel.profile.avatar,
                avatarImage = profileViewModel.avatarImage,
                headerTarget = { nameMetrics },
                launchedByKkc = launchedByKkc,
                onLoginSuccess = { emp ->
                    loggedInEmployee = emp
                    isExpanded = false
                },
                onCollapseComplete = {
                    alertsViewModel.allowAlertsToDisplay()
                },
                onExpandComplete = {
                    loggedInEmployee = null
                    nameMetrics = null
                },
                onReinstallLatest = onReinstallLatest
            )
            }

            // Splash screen — plays once on launch, fades out after animation completes
            AnimatedVisibility(
                visible = showSplash,
                enter = EnterTransition.None,
                exit = fadeOut(animationSpec = tween(600)),
                modifier = Modifier.fillMaxSize().zIndex(100f)
            ) {
                VideoSplash(onComplete = { showSplash = false })
            }
        }

            // ═══════════════════════════════════════════════════════════════════════════════════════════
            // KEEP IN SYNC — THIS IS A COPY OF KKCSHEETTRACKER'S BOTTOM NAVBAR
            // Source of truth: C:\Scripts\KKCSheetTracker\app\src\main\java\com\kkc\sheettracker\
            //   ui/components/AppScaffold.kt   MorphingNavBar + MorphingNavIconRow (full state) → KkcNavBar.kt
            //   ui/components/AppScaffold.kt   enum NavDestination labels / badge + tint rules → KkcNavBarModel.kt
            //   ui/components/icons/NavIcons.kt + IconDsl.kt  → KkcNavIcons.kt + KkcIconDsl.kt (verbatim copies)
            //   ui/theme/Type.kt  InterFontFamily + labelSmall → KkcNavTypography.kt
            //   ui/theme/Spacing.kt  navBarHorizontal 24dp, floatingNavMinSideMargin 24dp, floatingNavBottomGap 12dp
            //   navigation/KkcNavBarContract.kt + KkcNavBarPayloadBuilder.kt → KkcNavBarContract.kt
            // Layout, glass and animation are KKC's; COLORS follow kkcNavBarSource (KkcNavBarStyle.kt, HT-only):
            // standard HT theme → KKC's light/dark set for HT's effective mode (colors.isDark, live);
            // equipped HT theme → that theme's colors; LCARS → LCARS band (KkcLcarsNav.kt), no glass bar.
            // Any change to the KKC files above must be copied here in the same session; any contract
            // change bumps VERSION in both repos. Full rules: Hours Tracker AGENTS.md "KKC navbar mirror".
            // Fallback "← KKC" buttons (TimesheetScreen.kt, NameCard.kt) are intentionally kept.
            // ═══════════════════════════════════════════════════════════════════════════════════════════
            if (kkcNavBar != null) {
                // Standalone LCARS band: screens without the LCARS frame (login card / no employee) —
                // the timesheet's frame carries the band itself — and the retract animation after
                // leaving LCARS (the frame is already gone by then, so it plays over the new theme).
                if ((kkcIsLcars && (loggedInEmployee == null || isExpanded)) || kkcRetractingNow) {
                    KkcLcarsNavBand(
                        payload = kkcNavBar,
                        onNavigate = kkcOnNavigate,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        anim = if (kkcRetractingNow) KkcLcarsBlockAnim.Exit else kkcLcarsAnimNow
                    )
                }

                val glassStyle = if (!kkcIsLcars) {
                    val scheme = MaterialTheme.colorScheme
                    kkcNavBarStyle(
                        payload = kkcNavBar,
                        source = kkcNavSource,
                        darkTheme = colors.isDark,
                        htColors = kkcNavColorsFromHt(
                            accent = colors.accent,
                            textSecondary = colors.textSecondary,
                            pill = colors.hover,
                            surface = colors.surface,
                            error = scheme.error,
                            onError = scheme.onError
                        ),
                        canvas = colors.backgroundTo
                    ).also { style -> SideEffect { kkcLastGlassStyle = style } }
                } else {
                    kkcLastGlassStyle
                }
                // Initially-visible AnimatedVisibility doesn't animate, so opening Hours Tracker shows
                // the bar as-is; only a theme change slides it out (→ LCARS) or back in (← LCARS).
                AnimatedVisibility(
                    visible = !kkcIsLcars && !kkcRetractingNow,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = if (kkcAnimate) {
                        slideInVertically(tween(KKC_GLASS_SWAP_MS)) { it } + fadeIn(tween(KKC_GLASS_SWAP_MS))
                    } else EnterTransition.None,
                    exit = if (kkcAnimate) {
                        slideOutVertically(tween(KKC_GLASS_SWAP_MS)) { it } + fadeOut(tween(KKC_GLASS_SWAP_MS))
                    } else ExitTransition.None
                ) {
                    glassStyle?.let { style ->
                        KkcNavBar(
                            payload = kkcNavBar,
                            style = style,
                            hazeState = kkcHazeState,
                            onNavigate = kkcOnNavigate
                        )
                    }
                }
            }
        }
    }
}

