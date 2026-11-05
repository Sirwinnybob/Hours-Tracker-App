package com.example.timecard.ui.timesheet

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecard.data.repository.FileRepository
import com.example.timecard.domain.StatsPeriod
import com.example.timecard.ui.alerts.AlertModal
import com.example.timecard.ui.alerts.AlertsViewModel
import com.example.timecard.ui.alerts.PastAlertsModal
import com.example.timecard.ui.charts.ChartsSection
import com.example.timecard.ui.components.ThemeToggle
import com.example.timecard.ui.stats.StatsModal
import com.example.timecard.ui.stats.StatsViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import android.graphics.BitmapFactory
import com.example.timecard.ui.shop.ShopViewModel

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.ErrorRed
import com.example.timecard.ui.theme.LcarsBlueBell
import com.example.timecard.ui.theme.LcarsFrame
import com.example.timecard.ui.theme.LcarsOrange
import com.example.timecard.ui.theme.LcarsPurple
import com.example.timecard.ui.theme.LcarsRed
import com.example.timecard.ui.theme.LcarsTan
import com.example.timecard.ui.theme.LcarsYellow
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.SaveGreen
import com.example.timecard.ui.theme.SyncingBlue
import com.example.timecard.ui.theme.LcarsAnakiwa
import com.example.timecard.ui.theme.LcarsMelrose
import com.example.timecard.ui.theme.LcarsButton
import com.example.timecard.ui.theme.LcarsButtonShape
import com.example.timecard.ui.theme.ThemeState
import com.example.timecard.ui.theme.RetroFrame
import com.example.timecard.ui.theme.RetroButton
import com.example.timecard.ui.theme.retroRainbowBackground
import com.example.timecard.ui.theme.RetroSurface
import com.example.timecard.ui.theme.RetroTextHeading
import com.example.timecard.ui.theme.RetroTextPrimary
import com.example.timecard.ui.theme.RetroTextSecondary
import com.example.timecard.ui.theme.RetroAccent
import com.example.timecard.ui.theme.RetroAccentTeal
import com.example.timecard.ui.theme.VT323FontFamily
import com.example.timecard.ui.theme.doubleBeveledBorder
import com.example.timecard.kkcnav.KKC_LCARS_NAV_HEIGHT
import com.example.timecard.kkcnav.KkcLcarsNavButtons

@Composable
fun TimesheetScreen(
    employeeId: String,
    employeeDisplayName: String,
    repository: FileRepository?,
    themeState: ThemeState,
    timesheetViewModel: TimesheetViewModel,
    alertsViewModel: AlertsViewModel,
    statsViewModel: StatsViewModel,
    profileViewModel: com.example.timecard.ui.profile.ProfileViewModel? = null,
    leaderboardViewModel: com.example.timecard.ui.profile.LeaderboardViewModel? = null,
    challengesViewModel: com.example.timecard.ui.challenges.ChallengesViewModel? = null,
    shopViewModel: com.example.timecard.ui.shop.ShopViewModel? = null,
    employees: List<com.example.timecard.data.model.Employee> = emptyList(),
    onLogout: () -> Unit,
    onNameMeasured: ((com.example.timecard.ui.login.HeaderMetrics) -> Unit)? = null,
    launchedByKkc: Boolean = false,
    // Only set when the LCARS theme is equipped and KKC launched us: the KKC destinations then live
    // in the LCARS frame's bottom band instead of the floating glass bar (kkcnav/KkcLcarsNav.kt).
    kkcNavBar: com.example.timecard.kkcnav.KkcNavBarPayload? = null,
    kkcRetroNavBar: com.example.timecard.kkcnav.KkcNavBarPayload? = null,
    onKkcNavigate: (String) -> Unit = {},
    kkcLcarsAnim: com.example.timecard.kkcnav.KkcLcarsBlockAnim = com.example.timecard.kkcnav.KkcLcarsBlockAnim.None
) {
    val uiState by timesheetViewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTimecardColors.current
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val activity = LocalContext.current as? Activity
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    var showCharts by remember { mutableStateOf(false) }
    var activeLcarsTab by remember { mutableStateOf("timesheet") }
    var activeRetroTab by remember { mutableStateOf("timesheet") }

    LaunchedEffect(employeeId) {
        timesheetViewModel.initialize(employeeId, repository)
        alertsViewModel.initialize(employeeId, employeeDisplayName, repository)
        statsViewModel.initialize(employeeId, repository, timesheetViewModel)
    }

    NavHost(navController = navController, startDestination = "main", modifier = Modifier.fillMaxSize()) {
        composable("main") {
            val isCompact = remember(screenWidth) { screenWidth < 900 }

            if (colors.isLcars) {
                // ── LCARS full-frame layout ───────────────────────────────────
                LcarsFrame(
                    modifier = Modifier.statusBarsPadding(),
                    headerContent = {
                        val namePlaceholderMin = if (isCompact) 100.dp else 120.dp
                        val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeDisplayName
                        Box(
                            modifier = Modifier
                                .widthIn(min = namePlaceholderMin)
                                .height(28.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val sz = coords.size
                                    onNameMeasured?.invoke(
                                        com.example.timecard.ui.login.HeaderMetrics(
                                            x = pos.x,
                                            centerY = pos.y + sz.height / 2f,
                                            widthPx = sz.width.toFloat(),
                                            heightPx = sz.height.toFloat()
                                        )
                                    )
                                }
                        ) {
                            Text(
                                text = nameForMeasure,
                                fontSize = 18.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = Color.Transparent
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        run {
                            val btnBg = if (uiState.isViewingPrevious) com.example.timecard.ui.theme.LcarsOrange else com.example.timecard.ui.theme.LcarsYellow
                            val btnText = if (uiState.isViewingPrevious) "CURRENT WEEK" else "PREVIOUS WEEK"
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black)
                                    .clickable(enabled = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch) {
                                        activeLcarsTab = "timesheet"
                                        timesheetViewModel.togglePrevWeek()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = btnText,
                                    color = btnBg,
                                    fontFamily = AntonioFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (launchedByKkc) {
                            Spacer(modifier = Modifier.width(6.dp).height(28.dp).background(Color.Black))
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .background(Color(0xFF99FF66))
                                    .clickable {
                                        activity?.finish()
                                    }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "← KKC",
                                    color = Color.Black,
                                    fontFamily = AntonioFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp).height(28.dp).background(Color.Black))
                        }

                        Text(
                            text = "WEEK OF ${uiState.activeWeekDate}",
                            color = Color.Black,
                            fontFamily = AntonioFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                        )
                    },
                    footerHeight = if (kkcNavBar != null) KKC_LCARS_NAV_HEIGHT else 20.dp,
                    footerBackground = if (kkcNavBar != null) Color.Black else com.example.timecard.ui.theme.LcarsTan,
                    footerPadding = if (kkcNavBar != null) 0.dp else 8.dp,
                    footerContent = {
                        if (kkcNavBar != null) {
                            KkcLcarsNavButtons(payload = kkcNavBar, onNavigate = onKkcNavigate, anim = kkcLcarsAnim)
                        } else {
                            Text(
                                text = "TIMECARD SYSTEM  //  ${employeeDisplayName.uppercase()}",
                                color = Color.Black,
                                fontFamily = AntonioFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    },
                    sidebarContent = {
                        val btnModifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 32.dp else 36.dp)

                        LcarsButton(
                            onClick = {
                                if (activeLcarsTab == "stats") {
                                    activeLcarsTab = "timesheet"
                                } else {
                                    statsViewModel.loadStats(com.example.timecard.domain.StatsPeriod.ThisWeek)
                                    activeLcarsTab = "stats"
                                }
                            },
                            label = "STATS",
                            color = if (activeLcarsTab == "stats") LcarsAnakiwa else LcarsTan,
                            code = "01",
                            modifier = btnModifier,
                            shapeType = LcarsButtonShape.LeftCap
                        )

                        if (leaderboardViewModel != null) {
                            LcarsButton(
                                onClick = {
                                    if (activeLcarsTab == "leaderboard") {
                                        activeLcarsTab = "timesheet"
                                    } else {
                                        leaderboardViewModel.load(
                                            employees.filter { !it.excluded },
                                            uiState.activeWeekDate,
                                            repository
                                        )
                                        activeLcarsTab = "leaderboard"
                                    }
                                },
                                label = "BOARD",
                                color = if (activeLcarsTab == "leaderboard") LcarsAnakiwa else LcarsPurple,
                                code = "02",
                                modifier = btnModifier,
                                shapeType = LcarsButtonShape.LeftCap
                            )
                        }

                        if (challengesViewModel != null) {
                            LcarsButton(
                                onClick = {
                                    activeLcarsTab = if (activeLcarsTab == "challenges") "timesheet" else "challenges"
                                },
                                label = "TARGETS",
                                color = if (activeLcarsTab == "challenges") LcarsAnakiwa else LcarsBlueBell,
                                code = "03",
                                modifier = btnModifier,
                                shapeType = LcarsButtonShape.LeftCap
                            )
                        }

                        LcarsButton(
                            onClick = {
                                activeLcarsTab = "timesheet"
                                showCharts = !showCharts
                            },
                            label = "CHARTS",
                            color = if (activeLcarsTab == "timesheet" && showCharts) LcarsAnakiwa else LcarsOrange,
                            code = "04",
                            modifier = btnModifier,
                            shapeType = LcarsButtonShape.LeftCap
                        )

                        if (shopViewModel != null) {
                            LcarsButton(
                                onClick = {
                                    activeLcarsTab = if (activeLcarsTab == "shop") "timesheet" else "shop"
                                },
                                label = "SHOP",
                                color = if (activeLcarsTab == "shop") LcarsAnakiwa else LcarsYellow,
                                code = "05",
                                modifier = btnModifier,
                                shapeType = LcarsButtonShape.LeftCap
                            )
                        }

                        if (profileViewModel != null) {
                            LcarsButton(
                                onClick = {
                                    activeLcarsTab = if (activeLcarsTab == "settings" || activeLcarsTab == "alerts") "timesheet" else "settings"
                                },
                                label = "USER",
                                color = if (activeLcarsTab == "settings" || activeLcarsTab == "alerts") LcarsAnakiwa else LcarsMelrose,
                                code = "06",
                                modifier = btnModifier,
                                shapeType = LcarsButtonShape.LeftCap
                            )
                        }

                        val saveColor = when (uiState.saveStatus) {
                            SaveStatus.SAVED -> Color(0xFF99FF66)
                            SaveStatus.SYNCING -> LcarsOrange
                            SaveStatus.ERROR -> LcarsRed
                        }
                        val saveLabel = when (uiState.saveStatus) {
                            SaveStatus.SAVED -> "SAVED"
                            SaveStatus.SYNCING -> "SYNCING"
                            SaveStatus.ERROR -> "ERROR"
                        }
                        LcarsButton(
                            onClick = { timesheetViewModel.saveData() },
                            label = saveLabel,
                            color = saveColor,
                            code = "07",
                            modifier = btnModifier,
                            shapeType = LcarsButtonShape.LeftCap,
                            enabled = uiState.saveStatus != SaveStatus.SYNCING
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        LcarsButton(
                            onClick = { timesheetViewModel.logout(); onLogout() },
                            label = "LOGOUT",
                            color = LcarsRed,
                            textColor = Color.White,
                            code = "08",
                            modifier = btnModifier,
                            shapeType = LcarsButtonShape.LeftCap
                        )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Shop banner
                        val newShopItemsLcars = shopViewModel?.newSpecialItems ?: emptyList()
                        AnimatedVisibility(
                            visible = newShopItemsLcars.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LcarsOrange)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { activeLcarsTab = "shop" },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val bannerText = if (newShopItemsLcars.size == 1) {
                                    "NEW IN SHOP: ${newShopItemsLcars.first().title.uppercase()}"
                                } else {
                                    "${newShopItemsLcars.size} NEW ITEMS IN THE SHOP"
                                }
                                Text(text = bannerText, color = Color.Black, fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 11.sp else 13.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                Text(text = "VISIT SHOP >>", color = Color.Black, fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 11.sp else 13.sp, letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        // Content (Animated swap of tabs)
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            AnimatedContent(
                                targetState = activeLcarsTab,
                                transitionSpec = {
                                    if (targetState == "timesheet") {
                                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                                    } else {
                                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                                    }
                                },
                                label = "lcarsContentTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { tab ->
                                when (tab) {
                                    "stats" -> {
                                        StatsModal(
                                            viewModel = statsViewModel,
                                            onDismiss = { activeLcarsTab = "timesheet" }
                                        )
                                    }
                                    "leaderboard" -> {
                                        if (leaderboardViewModel != null) {
                                            val feedEmployees = remember(employees) {
                                                employees.filter { !it.excluded }
                                            }
                                            com.example.timecard.ui.profile.LeaderboardModal(
                                                viewModel = leaderboardViewModel,
                                                myId = employeeId,
                                                myDisplayName = employeeDisplayName,
                                                badgeImages = profileViewModel?.badgeImages ?: emptyMap(),
                                                feedEmployeeNames = feedEmployees,
                                                onFeedTabSelected = { leaderboardViewModel.loadFeed(feedEmployees, repository) },
                                                onDismiss = { activeLcarsTab = "timesheet" }
                                            )
                                        }
                                    }
                                    "challenges" -> {
                                        if (challengesViewModel != null && profileViewModel != null) {
                                            com.example.timecard.ui.challenges.ChallengesModal(
                                                viewModel = challengesViewModel,
                                                onLoad = {
                                                    challengesViewModel.load(
                                                        employeeName = employeeId,
                                                        weekDate = uiState.activeWeekDate,
                                                        profile = profileViewModel.profile,
                                                        repository = repository
                                                    )
                                                },
                                                onDismiss = { activeLcarsTab = "timesheet" }
                                            )
                                        }
                                    }
                                    "shop" -> {
                                        if (shopViewModel != null && profileViewModel != null) {
                                            com.example.timecard.ui.shop.ShopModal(
                                                onDismiss = { activeLcarsTab = "timesheet" },
                                                shopViewModel = shopViewModel,
                                                profileViewModel = profileViewModel
                                            )
                                        }
                                    }
                                    "settings" -> {
                                        if (profileViewModel != null) {
                                            com.example.timecard.ui.profile.SettingsModal(
                                                profileViewModel = profileViewModel,
                                                actualName = employeeDisplayName,
                                                onDismiss = { activeLcarsTab = "timesheet" },
                                                onNavigateToAlerts = { activeLcarsTab = "alerts" },
                                                onOpenShop = { activeLcarsTab = "shop" }
                                            )
                                        }
                                    }
                                    "alerts" -> {
                                        PastAlertsModal(
                                            viewModel = alertsViewModel,
                                            onDismiss = { activeLcarsTab = "settings" }
                                        )
                                    }
                                    else -> {
                                        if (uiState.isLockedByAnotherUser) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🔒", fontSize = 64.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text("TIMESHEET LOCKED", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontFamily = AntonioFontFamily, letterSpacing = 2.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        "If you are seeing this it means the timecard is currently being used by another tablet or has been modified within the last 5 minutes.\n\n" +
                                                        "If this lock persists try:\n1. Waiting 5 minutes\n2. Syncing your tablet.\n3. Restarting then syncing your tablet.\n\nIf this lock persists tell Winston and he will get it unlocked for you.",
                                                        fontSize = 16.sp, color = colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                                                    )
                                                }
                                            }
                                        } else if (isLandscape) {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(), chartsContent = null, modifier = Modifier.weight(0.66f))
                                                ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = true, onToggle = {}, showToggle = false, modifier = Modifier.weight(0.34f))
                                            }
                                        } else {
                                            TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(),
                                                chartsContent = { ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = showCharts, onToggle = { showCharts = !showCharts }) },
                                                modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (colors.isRetro) {
                // ── Retro full-frame layout ───────────────────────────
                RetroFrame(
                    modifier = Modifier.statusBarsPadding(),
                    bottomBar = kkcRetroNavBar?.let { payload ->
                        { com.example.timecard.kkcnav.KkcRetroNavBar(payload = payload, onNavigate = onKkcNavigate) }
                    },
                    headerContent = {
                        // Reserves the slot the collapsed NameCard chip animates into (same contract as LCARS)
                        val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeDisplayName
                        Box(
                            modifier = Modifier
                                .widthIn(min = if (isCompact) 100.dp else 120.dp)
                                .height(24.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val sz = coords.size
                                    onNameMeasured?.invoke(
                                        com.example.timecard.ui.login.HeaderMetrics(
                                            x = pos.x,
                                            centerY = pos.y + sz.height / 2f,
                                            widthPx = sz.width.toFloat(),
                                            heightPx = sz.height.toFloat()
                                        )
                                    )
                                }
                        ) {
                            Text(
                                text = nameForMeasure,
                                fontFamily = VT323FontFamily,
                                fontSize = 18.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = Color.Transparent
                            )
                        }

                        Text(
                            text = "WEEK OF ${uiState.activeWeekDate}",
                            fontFamily = VT323FontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = RetroTextHeading,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        run {
                            val btnText = if (uiState.isViewingPrevious) "CURRENT WEEK ▶" else "◀ PREV WEEK"
                            RetroButton(
                                onClick = {
                                    activeRetroTab = "timesheet"
                                    timesheetViewModel.togglePrevWeek()
                                },
                                label = btnText,
                                enabled = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch,
                                backgroundColor = if (uiState.isViewingPrevious) RetroAccentTeal else RetroSurface,
                                textColor = if (uiState.isViewingPrevious) Color.Black else RetroTextPrimary,
                                modifier = Modifier.height(26.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }

                        if (launchedByKkc) {
                            RetroButton(
                                onClick = { activity?.finish() },
                                label = "◀ KKC RETURN",
                                backgroundColor = RetroAccentTeal,
                                textColor = Color.Black,
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    },
                    footerContent = {
                        Text(
                            text = "USER: ${employeeDisplayName.uppercase()}",
                            fontFamily = VT323FontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RetroTextPrimary
                        )
                    },
                    sidebarContent = {
                        val btnModifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 28.dp else 32.dp)

                        RetroButton(
                            onClick = { activeRetroTab = "timesheet" },
                            label = "TIMESHEET",
                            icon = "🕒",
                            isSelected = activeRetroTab == "timesheet",
                            modifier = btnModifier
                        )

                        RetroButton(
                            onClick = {
                                if (activeRetroTab == "stats") {
                                    activeRetroTab = "timesheet"
                                } else {
                                    statsViewModel.loadStats(com.example.timecard.domain.StatsPeriod.ThisWeek)
                                    activeRetroTab = "stats"
                                }
                            },
                            label = "STATISTICS",
                            icon = "📊",
                            isSelected = activeRetroTab == "stats",
                            modifier = btnModifier
                        )

                        if (leaderboardViewModel != null) {
                            RetroButton(
                                onClick = {
                                    if (activeRetroTab == "leaderboard") {
                                        activeRetroTab = "timesheet"
                                    } else {
                                        leaderboardViewModel.load(
                                            employees.filter { !it.excluded },
                                            uiState.activeWeekDate,
                                            repository
                                        )
                                        activeRetroTab = "leaderboard"
                                    }
                                },
                                label = "RANKINGS",
                                icon = "🏆",
                                isSelected = activeRetroTab == "leaderboard",
                                modifier = btnModifier
                            )
                        }

                        if (challengesViewModel != null) {
                            RetroButton(
                                onClick = {
                                    activeRetroTab = if (activeRetroTab == "challenges") "timesheet" else "challenges"
                                },
                                label = "TARGETS",
                                icon = "🎯",
                                isSelected = activeRetroTab == "challenges",
                                modifier = btnModifier
                            )
                        }

                        RetroButton(
                            onClick = {
                                activeRetroTab = "timesheet"
                                showCharts = !showCharts
                            },
                            label = "CHARTS",
                            icon = "📈",
                            isSelected = activeRetroTab == "timesheet" && showCharts,
                            modifier = btnModifier
                        )

                        if (shopViewModel != null) {
                            RetroButton(
                                onClick = {
                                    activeRetroTab = if (activeRetroTab == "shop") "timesheet" else "shop"
                                },
                                label = "SHOP",
                                icon = "🛍️",
                                isSelected = activeRetroTab == "shop",
                                modifier = btnModifier
                            )
                        }

                        if (profileViewModel != null) {
                            RetroButton(
                                onClick = {
                                    activeRetroTab = if (activeRetroTab == "settings" || activeRetroTab == "alerts") "timesheet" else "settings"
                                },
                                label = "CONFIG",
                                icon = "⚙️",
                                isSelected = activeRetroTab == "settings" || activeRetroTab == "alerts",
                                modifier = btnModifier
                            )
                        }

                        val saveColor = when (uiState.saveStatus) {
                            SaveStatus.SAVED -> Color(0xFFC4EBC4)
                            SaveStatus.SYNCING -> Color(0xFFFBE4C3)
                            SaveStatus.ERROR -> Color(0xFFF5C6C6)
                        }
                        val saveLabel = when (uiState.saveStatus) {
                            SaveStatus.SAVED -> "SAVED OK"
                            SaveStatus.SYNCING -> "SAVING..."
                            SaveStatus.ERROR -> "SAVE ERROR"
                        }
                        RetroButton(
                            onClick = { timesheetViewModel.saveData() },
                            label = saveLabel,
                            icon = "💾",
                            backgroundColor = saveColor,
                            modifier = btnModifier,
                            enabled = uiState.saveStatus != SaveStatus.SYNCING
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        RetroButton(
                            onClick = { timesheetViewModel.logout(); onLogout() },
                            label = "LOGOUT",
                            icon = "🚪",
                            backgroundColor = Color(0xFFCC6666),
                            textColor = Color.White,
                            modifier = btnModifier
                        )
                    }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Shop Banner
                        val newShopItemsRetro = shopViewModel?.newSpecialItems ?: emptyList()
                        AnimatedVisibility(
                            visible = newShopItemsRetro.isNotEmpty(),
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .retroRainbowBackground(durationMs = 2200, saturation = 0.5f)
                                    .doubleBeveledBorder(isInset = false)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clickable { activeRetroTab = "shop" },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val bannerText = if (newShopItemsRetro.size == 1) {
                                    "★ NEW AT THE SHOP: ${newShopItemsRetro.first().title.uppercase()} ★"
                                } else {
                                    "★ ${newShopItemsRetro.size} NEW ITEMS AT THE SHOP ★"
                                }
                                Text(
                                    text = bannerText,
                                    color = RetroTextHeading,
                                    fontFamily = VT323FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "ENTER SHOP ▶",
                                    color = RetroAccent,
                                    fontFamily = VT323FontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Tab content
                        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                            AnimatedContent(
                                targetState = activeRetroTab,
                                label = "retroContentTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { tab ->
                                when (tab) {
                                    "stats" -> {
                                        StatsModal(
                                            viewModel = statsViewModel,
                                            onDismiss = { activeRetroTab = "timesheet" }
                                        )
                                    }
                                    "leaderboard" -> {
                                        if (leaderboardViewModel != null) {
                                            val feedEmployees = remember(employees) {
                                                employees.filter { !it.excluded }
                                            }
                                            com.example.timecard.ui.profile.LeaderboardModal(
                                                viewModel = leaderboardViewModel,
                                                myId = employeeId,
                                                myDisplayName = employeeDisplayName,
                                                badgeImages = profileViewModel?.badgeImages ?: emptyMap(),
                                                feedEmployeeNames = feedEmployees,
                                                onFeedTabSelected = { leaderboardViewModel.loadFeed(feedEmployees, repository) },
                                                onDismiss = { activeRetroTab = "timesheet" }
                                            )
                                        }
                                    }
                                    "challenges" -> {
                                        if (challengesViewModel != null && profileViewModel != null) {
                                            com.example.timecard.ui.challenges.ChallengesModal(
                                                viewModel = challengesViewModel,
                                                onLoad = {
                                                    challengesViewModel.load(
                                                        employeeName = employeeId,
                                                        weekDate = uiState.activeWeekDate,
                                                        profile = profileViewModel.profile,
                                                        repository = repository
                                                    )
                                                },
                                                onDismiss = { activeRetroTab = "timesheet" }
                                            )
                                        }
                                    }
                                    "shop" -> {
                                        if (shopViewModel != null && profileViewModel != null) {
                                            com.example.timecard.ui.shop.ShopModal(
                                                onDismiss = { activeRetroTab = "timesheet" },
                                                shopViewModel = shopViewModel,
                                                profileViewModel = profileViewModel
                                            )
                                        }
                                    }
                                    "settings" -> {
                                        if (profileViewModel != null) {
                                            com.example.timecard.ui.profile.SettingsModal(
                                                profileViewModel = profileViewModel,
                                                actualName = employeeDisplayName,
                                                onDismiss = { activeRetroTab = "timesheet" },
                                                onNavigateToAlerts = { activeRetroTab = "alerts" },
                                                onOpenShop = { activeRetroTab = "shop" }
                                            )
                                        }
                                    }
                                    "alerts" -> {
                                        PastAlertsModal(
                                            viewModel = alertsViewModel,
                                            onDismiss = { activeRetroTab = "settings" }
                                        )
                                    }
                                    else -> {
                                        if (uiState.isLockedByAnotherUser) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🔒", fontSize = 64.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text("TIMECARD LOCKED (BUSY)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontFamily = VT323FontFamily, letterSpacing = 2.sp)
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Text(
                                                        "This timecard is currently being modified by another tablet.\n\nPlease wait 5 minutes or contact your supervisor.",
                                                        fontSize = 17.sp, color = colors.textSecondary, fontFamily = VT323FontFamily, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                                                    )
                                                }
                                            }
                                        } else if (isLandscape) {
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(), chartsContent = null, modifier = Modifier.weight(0.66f))
                                                ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = true, onToggle = {}, showToggle = false, modifier = Modifier.weight(0.34f))
                                            }
                                        } else {
                                            TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(),
                                                chartsContent = { ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = showCharts, onToggle = { showCharts = !showCharts }) },
                                                modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Standard layout (unchanged) ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.backgroundBrush)
                        .statusBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val btnHeight = 40.dp
                        val btnFontSize = if (isCompact) 11.sp else 13.sp
                        val btnSpacing = if (isCompact) 4.dp else 6.dp
                        val btnPaddingH = if (isCompact) 10.dp else 14.dp
                        val namePlaceholderMin = if (isCompact) 120.dp else 140.dp

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surface)
                                .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(min = namePlaceholderMin)
                                        .height(40.dp)
                                        .onGloballyPositioned { coords ->
                                            val pos = coords.positionInRoot()
                                            val sz = coords.size
                                            onNameMeasured?.invoke(com.example.timecard.ui.login.HeaderMetrics(x = pos.x, centerY = pos.y + sz.height / 2f, widthPx = sz.width.toFloat(), heightPx = sz.height.toFloat()))
                                        }
                                ) {
                                    val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeDisplayName
                                    Text(text = nameForMeasure, fontSize = 18.sp, maxLines = 1, softWrap = false, color = Color.Transparent, modifier = Modifier)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(text = "Week of ${uiState.activeWeekDate}", fontSize = if (isCompact) 10.sp else 12.sp, color = colors.textSecondary)
                                    run {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Button(
                                            onClick = { timesheetViewModel.togglePrevWeek() },
                                            enabled = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch,
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.hover, contentColor = colors.textPrimary),
                                            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
                                            contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(if (isCompact) 24.dp else 28.dp)
                                        ) {
                                            Text(text = if (uiState.isViewingPrevious) "Current week" else "Previous week", fontSize = if (isCompact) 9.sp else 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(btnSpacing), verticalAlignment = Alignment.CenterVertically) {
                                if (launchedByKkc) {
                                    Button(onClick = { activity?.finish() }, colors = ButtonDefaults.buttonColors(containerColor = colors.accent), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
                                        Text("← KKC", color = Color.White, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(onClick = { statsViewModel.loadStats(StatsPeriod.ThisWeek); navController.navigate("stats") }, colors = ButtonDefaults.buttonColors(containerColor = colors.hover), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
                                    Text("STATS", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                }
                                SaveButton(status = uiState.saveStatus, onClick = { timesheetViewModel.saveData() }, isCompact = isCompact)
                                Button(onClick = { timesheetViewModel.logout(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = colors.surface), border = androidx.compose.foundation.BorderStroke(1.dp, colors.textOrange), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = if (isCompact) 20.dp else 30.dp)) {
                                    Text("LOGOUT", color = colors.textOrange, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                }
                                if (leaderboardViewModel != null) {
                                    Button(onClick = { leaderboardViewModel.load(employees.filter { !it.excluded }, uiState.activeWeekDate, repository); navController.navigate("leaderboard") }, colors = ButtonDefaults.buttonColors(containerColor = colors.hover), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
                                        Text("🏆", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (challengesViewModel != null) {
                                    Button(onClick = { navController.navigate("challenges") }, colors = ButtonDefaults.buttonColors(containerColor = colors.hover), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
                                        Text("🎯", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (profileViewModel != null) {
                                    val streak = profileViewModel.profile.streaks.currentDaily
                                    val avatar = profileViewModel.profile.avatar
                                    val avatarImage = profileViewModel.avatarImage
                                    Box(modifier = Modifier.size(btnHeight).clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp))).clickable { navController.navigate("settings") }, contentAlignment = Alignment.Center) {
                                        if (avatar == "custom" && avatarImage != null) {
                                            val bitmap = remember(avatarImage) { BitmapFactory.decodeByteArray(avatarImage, 0, avatarImage.size)?.asImageBitmap() }
                                            if (bitmap != null) {
                                                Image(bitmap = bitmap, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            } else {
                                                com.example.timecard.ui.components.InitialsAvatar(name = profileViewModel.profile.displayName ?: employeeDisplayName, size = btnHeight, fontSize = if (isCompact) 18.sp else 22.sp, bgColor = colors.hover)
                                            }
                                        } else {
                                            com.example.timecard.ui.components.InitialsAvatar(name = profileViewModel.profile.displayName ?: employeeDisplayName, size = btnHeight, fontSize = if (isCompact) 18.sp else 22.sp, bgColor = colors.hover)
                                        }
                                        val badgeText = if (streak >= 3) "🔥$streak" else "⭐"
                                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
                                            Text(text = badgeText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                ThemeToggle(themeState = themeState, size = btnHeight)
                            }
                        }

                        // Shop banner
                        val newShopItems = shopViewModel?.newSpecialItems ?: emptyList()
                        AnimatedVisibility(visible = newShopItems.isNotEmpty(), enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()) {
                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFD4AC0D)).padding(horizontal = 12.dp, vertical = 6.dp).clickable { navController.navigate("shop") }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                val bannerText = if (newShopItems.size == 1) "✨ New in the Shop: ${newShopItems.first().title}" else "✨ ${newShopItems.size} new items in the Shop!"
                                Text(text = bannerText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 11.sp else 13.sp, modifier = Modifier.weight(1f))
                                Text(text = "Visit Shop →", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = if (isCompact) 11.sp else 13.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        // Content
                        if (uiState.isLockedByAnotherUser) {
                            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔒", fontSize = 64.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Timesheet Locked", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("If you are seeing this it means the timecard is currently being used by another tablet or has been modified within the last 5 minutes.\n\nIf this lock persists try:\n1. Waiting 5 minutes\n2. Syncing your tablet.\n3. Restarting then syncing your tablet.\n\nIf this lock persists tell Winston and he will get it unlocked for you.", fontSize = 16.sp, color = colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                                }
                            }
                        } else if (isLandscape) {
                            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                                TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(), chartsContent = null, modifier = Modifier.weight(0.66f))
                                ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = true, onToggle = {}, showToggle = false, modifier = Modifier.weight(0.34f))
                            }
                        } else {
                            TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, onUndo = timesheetViewModel::undo, onRedo = timesheetViewModel::redo, canUndo = timesheetViewModel.canUndo(), canRedo = timesheetViewModel.canRedo(),
                                chartsContent = { ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = showCharts, onToggle = { showCharts = !showCharts }) },
                                modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } // end of main composable

        dialog("stats") {
            StatsModal(
                viewModel = statsViewModel,
                onDismiss = { navController.popBackStack() }
            )
        }
        
        dialog("alerts") {
            PastAlertsModal(
                viewModel = alertsViewModel,
                onDismiss = { navController.popBackStack() }
            )
        }
        
        dialog("settings") {
            if (profileViewModel != null) {
                com.example.timecard.ui.profile.SettingsModal(
                    profileViewModel = profileViewModel,
                    actualName = employeeDisplayName,
                    onDismiss = { navController.popBackStack() },
                    onNavigateToAlerts = { navController.navigate("alerts") },
                    onOpenShop = { navController.navigate("shop") {
                        popUpTo("settings") { inclusive = true }
                    } }
                )
            }
        }
        
        dialog("shop") {
            if (shopViewModel != null && profileViewModel != null) {
                com.example.timecard.ui.shop.ShopModal(
                    onDismiss = { navController.popBackStack() },
                    shopViewModel = shopViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
        
        dialog("leaderboard") {
            if (leaderboardViewModel != null) {
                val feedEmployees = remember(employees) {
                    employees.filter { !it.excluded }
                }
                com.example.timecard.ui.profile.LeaderboardModal(
                    viewModel = leaderboardViewModel,
                    myId = employeeId,
                    myDisplayName = employeeDisplayName,
                    badgeImages = profileViewModel?.badgeImages ?: emptyMap(),
                    feedEmployeeNames = feedEmployees,
                    onFeedTabSelected = { leaderboardViewModel.loadFeed(feedEmployees, repository) },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }

        dialog("challenges") {
            if (challengesViewModel != null && profileViewModel != null) {
                com.example.timecard.ui.challenges.ChallengesModal(
                    viewModel = challengesViewModel,
                    onLoad = {
                        challengesViewModel.load(
                            employeeName = employeeId,
                            weekDate = uiState.activeWeekDate,
                            profile = profileViewModel.profile,
                            repository = repository
                        )
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }

    // Alert modal (auto-shown for unacknowledged alerts on launch, not part of regular nav actions)
    if (alertsViewModel.showAlertModal) {
        AlertModal(viewModel = alertsViewModel)
    }
}

@Composable
fun SaveButton(
    status: SaveStatus,
    onClick: () -> Unit,
    isCompact: Boolean = false
) {
    val colors = LocalTimecardColors.current
    val btnHeight = 40.dp
    val btnWidth = if (isCompact) 62.dp else 75.dp
    val syncWidth = if (isCompact) 30.dp else 36.dp
    val spinnerSize = if (isCompact) 20.dp else 24.dp
    val fontSize = if (isCompact) 11.sp else 13.sp

    val targetWidth = if (status == SaveStatus.SYNCING) syncWidth else btnWidth
    val targetColor = when (status) {
        SaveStatus.SAVED -> colors.textGreen
        SaveStatus.SYNCING -> colors.accent
        SaveStatus.ERROR -> colors.textOrange
    }

    val width by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetWidth,
        label = "width"
    )

    val color by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        label = "color"
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
        modifier = Modifier.height(btnHeight).width(width),
        contentPadding = PaddingValues(0.dp)
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = status,
            transitionSpec = {
                androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
            },
            label = "content"
        ) { currentStatus ->
            when (currentStatus) {
                SaveStatus.SYNCING -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(spinnerSize),
                        color = color,
                        strokeWidth = 2.dp
                    )
                }
                SaveStatus.SAVED -> {
                    Text("SAVED", color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
                SaveStatus.ERROR -> {
                    Text("ERROR", color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

