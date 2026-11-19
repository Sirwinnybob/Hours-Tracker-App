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
import com.example.timecard.ui.timesheet.standard.AvatarTile
import com.example.timecard.ui.timesheet.standard.GridActions
import com.example.timecard.ui.timesheet.standard.StandardHeader
import com.example.timecard.ui.timesheet.standard.StandardLockedNotice
import com.example.timecard.ui.timesheet.standard.StandardTimesheetBody
import com.example.timecard.ui.timesheet.standard.currentStandardLook
import com.example.timecard.ui.timesheet.standard.rememberActiveJobs
import com.example.timecard.ui.timesheet.standard.weekOfLabel
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
    // Landscape launch from KKC (KKC's right rail): the standard layout leaves room on its right for
    // the glass rail (kkcnav/KkcNavRail.kt; background still runs full width under the glass), LCARS
    // puts the destination blocks in a right spine, Retro docks its taskbar on the right edge.
    kkcLandscapeNav: Boolean = false,
    // Colors for the standard layout (ui/timesheet/standard/StandardLook.kt); null → current HT theme.
    standardLook: com.example.timecard.ui.timesheet.standard.StandardLook? = null,
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
                    footerHeight = if (kkcNavBar != null && !kkcLandscapeNav) KKC_LCARS_NAV_HEIGHT else 20.dp,
                    footerBackground = if (kkcNavBar != null && !kkcLandscapeNav) Color.Black else com.example.timecard.ui.theme.LcarsTan,
                    footerPadding = if (kkcNavBar != null && !kkcLandscapeNav) 0.dp else 8.dp,
                    rightSpineContent = if (kkcNavBar != null && kkcLandscapeNav) {
                        { KkcLcarsNavButtons(payload = kkcNavBar, onNavigate = onKkcNavigate, anim = kkcLcarsAnim, vertical = true) }
                    } else null,
                    footerContent = {
                        if (kkcNavBar != null && !kkcLandscapeNav) {
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
                    bottomBar = kkcRetroNavBar?.takeIf { !kkcLandscapeNav }?.let { payload ->
                        { com.example.timecard.kkcnav.KkcRetroNavBar(payload = payload, onNavigate = onKkcNavigate) }
                    },
                    sideBar = kkcRetroNavBar?.takeIf { kkcLandscapeNav }?.let { payload ->
                        { com.example.timecard.kkcnav.KkcRetroNavRail(payload = payload, onNavigate = onKkcNavigate) }
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
                // ── Standard layout: KKC look (ui/timesheet/standard/) ─────────────────────
                val look = standardLook ?: currentStandardLook()
                val activeJobs = rememberActiveJobs(repository)
                val newShopItems = shopViewModel?.newSpecialItems ?: emptyList()
                val shopLabel = when {
                    newShopItems.isEmpty() -> null
                    newShopItems.size == 1 -> "New: ${newShopItems.first().title}"
                    else -> "${newShopItems.size} new in the Shop"
                }
                val openStats = { statsViewModel.loadStats(StatsPeriod.ThisWeek); navController.navigate("stats") }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(look.background)
                        .statusBarsPadding()
                        .padding(end = if (kkcLandscapeNav) com.example.timecard.kkcnav.KkcLandscapeRailClearance else 0.dp)
                ) {
                    com.example.timecard.ui.timesheet.standard.StandardText { Column(modifier = Modifier.fillMaxSize()) {
                        StandardHeader(
                            look = look,
                            nameForMeasure = profileViewModel?.profile?.displayName ?: employeeDisplayName,
                            weekLabel = weekOfLabel(uiState.activeWeekDate),
                            isViewingPrevious = uiState.isViewingPrevious,
                            canSwitchWeek = uiState.isDataLoaded && uiState.saveStatus == SaveStatus.SAVED && !uiState.isAnimatingWeekSwitch,
                            onToggleWeek = { timesheetViewModel.togglePrevWeek() },
                            saveStatus = uiState.saveStatus,
                            onSave = { timesheetViewModel.saveData() },
                            onStats = openStats,
                            onLeaderboard = leaderboardViewModel?.let { vm ->
                                { vm.load(employees.filter { !it.excluded }, uiState.activeWeekDate, repository); navController.navigate("leaderboard") }
                            },
                            onChallenges = challengesViewModel?.let { { navController.navigate("challenges") } },
                            onLogout = { timesheetViewModel.logout(); onLogout() },
                            themeState = themeState,
                            showKkcBack = launchedByKkc,
                            onKkcBack = { activity?.finish() },
                            avatar = {
                                if (profileViewModel != null) {
                                    val profile = profileViewModel.profile
                                    val avatarImage = profileViewModel.avatarImage
                                    AvatarTile(look, profile.streaks.currentDaily, onClick = { navController.navigate("settings") }) {
                                        val bitmap = remember(avatarImage, profile.avatar) {
                                            if (profile.avatar == "custom" && avatarImage != null) {
                                                BitmapFactory.decodeByteArray(avatarImage, 0, avatarImage.size)?.asImageBitmap()
                                            } else null
                                        }
                                        if (bitmap != null) {
                                            Image(bitmap = bitmap, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            com.example.timecard.ui.components.InitialsAvatar(name = profile.displayName ?: employeeDisplayName, size = 44.dp, fontSize = 18.sp, bgColor = look.soft)
                                        }
                                    }
                                }
                            },
                            onNameMeasured = onNameMeasured
                        )
                        if (uiState.isLockedByAnotherUser) {
                            StandardLockedNotice(look, Modifier.weight(1f))
                        } else {
                            StandardTimesheetBody(
                                look = look,
                                uiState = uiState,
                                activeJobs = activeJobs,
                                canUndo = timesheetViewModel.canUndo(),
                                canRedo = timesheetViewModel.canRedo(),
                                actions = GridActions(
                                    onJobChange = timesheetViewModel::setJob,
                                    onHoursChange = timesheetViewModel::setHours,
                                    onSnapHours = timesheetViewModel::snapHours,
                                    onFillShop = timesheetViewModel::fillShopHours,
                                    onToggleNoLunch = timesheetViewModel::toggleNoLunch,
                                    onDeliveryTag = timesheetViewModel::toggleDeliveryTag,
                                    onAddRow = {
                                        val index = timesheetViewModel.uiState.value.numRows
                                        timesheetViewModel.addRow()
                                        index
                                    },
                                    onUndo = timesheetViewModel::undo,
                                    onRedo = timesheetViewModel::redo
                                ),
                                shopLabel = shopLabel,
                                onShop = { navController.navigate("shop") },
                                onStats = openStats,
                                // The KKC glass bar floats over the bottom in portrait; leave room to scroll past it.
                                bottomPadding = if (launchedByKkc && !kkcLandscapeNav) 112.dp else 24.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } }
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

