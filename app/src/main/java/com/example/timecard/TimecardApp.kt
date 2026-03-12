package com.example.timecard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.timecard.ui.login.HeaderMetrics
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.zIndex
import com.example.timecard.data.model.Employee
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
    val shopViewModel: ShopViewModel = viewModel()


    // Pass the raw accent string to activate immersive themes
    val accentKey = profileViewModel.profile.accentColor

    TimecardTheme(themeMode = themeState.mode, accentKey = accentKey) {
        val tsState by timesheetViewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val colors = LocalTimecardColors.current

        var showSplash by remember { mutableStateOf(true) }
        var loggedInEmployee by remember { mutableStateOf<Employee?>(null) }
        var isExpanded by remember { mutableStateOf(true) }

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


        LaunchedEffect(isExpanded) {
            if (!isExpanded && loggedInEmployee != null) {
                kotlinx.coroutines.delay(600)
                launch { timesheetAlpha.animateTo(1f, tween(700)) }
                launch { gradientAlpha.animateTo(0f, tween(600)) }
            }
        }

        LaunchedEffect(loggedInEmployee) {
            val emp = loggedInEmployee
            if (emp != null) {
                val repo = loginViewModel.getRepository(context)
                profileViewModel.initialize(emp.name, repo)
                shopViewModel.initialize(repo, profileViewModel)
                alertsViewModel.onAcknowledged = { profileViewModel.onAlertAcknowledged() }
            } else {
                profileViewModel.logout()
                alertsViewModel.onAcknowledged = null
            }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
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
        ) {
            if (!showSplash) {
                // Layer 1: Timesheet
            val employee = loggedInEmployee
            if (employee != null) {
                val repository = remember(employee.name) { loginViewModel.getRepository(context) }
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = timesheetAlpha.value }) {
                    TimesheetScreen(
                        employeeName = employee.name,
                        repository = repository,
                        themeState = themeState,
                        timesheetViewModel = timesheetViewModel,
                        alertsViewModel = alertsViewModel,
                        statsViewModel = statsViewModel,
                        profileViewModel = profileViewModel,
                        leaderboardViewModel = leaderboardViewModel,
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
    }
}

