package com.example.timecard.ui.timesheet

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.timecard.ui.theme.ThemeState

@Composable
fun TimesheetScreen(
    employeeName: String,
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
    onNameMeasured: ((com.example.timecard.ui.login.HeaderMetrics) -> Unit)? = null
) {
    val uiState by timesheetViewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTimecardColors.current
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp

    var showCharts by remember { mutableStateOf(false) }

    LaunchedEffect(employeeName) {
        timesheetViewModel.initialize(employeeName, repository)
        alertsViewModel.initialize(employeeName, repository)
        statsViewModel.initialize(employeeName, repository, timesheetViewModel)
    }

    NavHost(navController = navController, startDestination = "main", modifier = Modifier.fillMaxSize()) {
        composable("main") {
            val isCompact = remember(screenWidth) { screenWidth < 900 }

            if (colors.isLcars) {
                // ── LCARS full-frame layout ───────────────────────────────────
                LcarsFrame(
                    title = "WEEK OF ${uiState.activeWeekDate}",
                    statusText = "TIMECARD SYSTEM  //  ${employeeName}",
                    modifier = Modifier.statusBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LcarsActionBar(
                            uiState = uiState,
                            employeeName = employeeName,
                            isCompact = isCompact,
                            statsViewModel = statsViewModel,
                            timesheetViewModel = timesheetViewModel,
                            navController = navController,
                            themeState = themeState,
                            profileViewModel = profileViewModel,
                            leaderboardViewModel = leaderboardViewModel,
                            challengesViewModel = challengesViewModel,
                            repository = repository,
                            employees = employees,
                            onLogout = { timesheetViewModel.logout(); onLogout() },
                            onNameMeasured = onNameMeasured
                        )

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
                                    .clickable { navController.navigate("shop") },
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

                        // Content
                        if (uiState.isLockedByAnotherUser) {
                            Box(
                                modifier = Modifier.fillMaxSize().weight(1f),
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
                            Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                                TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, chartsContent = null, modifier = Modifier.weight(0.66f))
                                ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = true, onToggle = {}, showToggle = false, modifier = Modifier.weight(0.34f))
                            }
                        } else {
                            TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch,
                                chartsContent = { ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = showCharts, onToggle = { showCharts = !showCharts }) },
                                modifier = Modifier.weight(1f))
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
                                    val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeName
                                    Text(text = nameForMeasure, fontSize = 18.sp, maxLines = 1, softWrap = false, color = Color.Transparent, modifier = Modifier)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(text = "Week of ${uiState.activeWeekDate}", fontSize = if (isCompact) 10.sp else 12.sp, color = colors.textSecondary)
                                    if (uiState.hasPreviousWeek) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Button(
                                            onClick = { timesheetViewModel.togglePrevWeek() },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isViewingPrevious) Color(0xFF48BB78) else Color(0xFFECC94B)),
                                            shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
                                            contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 0.dp),
                                            modifier = Modifier.height(if (isCompact) 24.dp else 28.dp)
                                        ) {
                                            Text(text = if (uiState.isViewingPrevious) "📅 Current" else "⏪ Revise Prev", fontSize = if (isCompact) 9.sp else 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(btnSpacing), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { statsViewModel.loadStats(StatsPeriod.ThisWeek); navController.navigate("stats") }, colors = ButtonDefaults.buttonColors(containerColor = colors.hover), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
                                    Text("STATS", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                }
                                SaveButton(status = uiState.saveStatus, onClick = { timesheetViewModel.saveData() }, isCompact = isCompact)
                                Button(onClick = { timesheetViewModel.logout(); onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = colors.surface), border = androidx.compose.foundation.BorderStroke(1.dp, colors.textOrange), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = if (isCompact) 20.dp else 30.dp)) {
                                    Text("LOGOUT", color = colors.textOrange, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                }
                                if (leaderboardViewModel != null) {
                                    Button(onClick = { leaderboardViewModel.load(employees.filter { !it.excluded }.map { it.name }, uiState.activeWeekDate, repository); navController.navigate("leaderboard") }, colors = ButtonDefaults.buttonColors(containerColor = colors.hover), shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)), modifier = Modifier.height(btnHeight), contentPadding = PaddingValues(horizontal = btnPaddingH)) {
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
                                            Image(bitmap = remember(avatarImage) { BitmapFactory.decodeByteArray(avatarImage, 0, avatarImage.size).asImageBitmap() }, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            com.example.timecard.ui.components.InitialsAvatar(name = profileViewModel.profile.displayName ?: employeeName, size = btnHeight, fontSize = if (isCompact) 18.sp else 22.sp, bgColor = colors.hover)
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
                                TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch, chartsContent = null, modifier = Modifier.weight(0.66f))
                                ChartsSection(currentData = timesheetViewModel.collectTimecardData(), previousData = uiState.previousWeekData, expanded = true, onToggle = {}, showToggle = false, modifier = Modifier.weight(0.34f))
                            }
                        } else {
                            TimesheetGrid(uiState = uiState, onJobChange = timesheetViewModel::setJob, onHoursChange = timesheetViewModel::setHours, onFillShopHours = timesheetViewModel::fillShopHours, onSnapHours = timesheetViewModel::snapHours, onAddRow = timesheetViewModel::addRow, onDeliveryTag = timesheetViewModel::toggleDeliveryTag, onJobTag = timesheetViewModel::setJobTag, onToggleNoLunch = timesheetViewModel::toggleNoLunch,
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
                    actualName = employeeName,
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
                val feedNames = remember(employees) {
                    employees.filter { !it.excluded }.map { it.name }
                }
                com.example.timecard.ui.profile.LeaderboardModal(
                    viewModel = leaderboardViewModel,
                    myName = employeeName,
                    badgeImages = profileViewModel?.badgeImages ?: emptyMap(),
                    feedEmployeeNames = feedNames,
                    onFeedTabSelected = { leaderboardViewModel.loadFeed(feedNames, repository) },
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
                            employeeName = employeeName,
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
// LCARS Action Bar — replaces the standard top-bar Row when isLcars is true.
// A row of distinctly coloured pill buttons on a black background.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LcarsActionBar(
    uiState: TimesheetUiState,
    employeeName: String,
    isCompact: Boolean,
    statsViewModel: com.example.timecard.ui.stats.StatsViewModel,
    timesheetViewModel: TimesheetViewModel,
    navController: androidx.navigation.NavController,
    themeState: com.example.timecard.ui.theme.ThemeState,
    profileViewModel: com.example.timecard.ui.profile.ProfileViewModel?,
    leaderboardViewModel: com.example.timecard.ui.profile.LeaderboardViewModel?,
    challengesViewModel: com.example.timecard.ui.challenges.ChallengesViewModel?,
    repository: com.example.timecard.data.repository.FileRepository?,
    employees: List<com.example.timecard.data.model.Employee>,
    onLogout: () -> Unit,
    onNameMeasured: ((com.example.timecard.ui.login.HeaderMetrics) -> Unit)?
) {
    val btnHeight = if (isCompact) 34.dp else 38.dp
    val fontSize = if (isCompact) 11.sp else 13.sp
    val hPad = if (isCompact) 10.dp else 14.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Invisible name-placeholder for NameCard positioning
        Box(
            modifier = Modifier
                .widthIn(min = if (isCompact) 100.dp else 120.dp)
                .height(btnHeight)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val sz = coords.size
                    onNameMeasured?.invoke(com.example.timecard.ui.login.HeaderMetrics(x = pos.x, centerY = pos.y + sz.height / 2f, widthPx = sz.width.toFloat(), heightPx = sz.height.toFloat()))
                }
        ) {
            val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeName
            Text(text = nameForMeasure, fontSize = 18.sp, maxLines = 1, softWrap = false, color = Color.Transparent)
        }

        // Week nav button (shown when previous week exists)
        if (uiState.hasPreviousWeek) {
            Button(
                onClick = { timesheetViewModel.togglePrevWeek() },
                colors = ButtonDefaults.buttonColors(containerColor = LcarsYellow),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
                modifier = Modifier.height(btnHeight)
            ) {
                Text(
                    text = if (uiState.isViewingPrevious) "CURRENT" else "PREV WK",
                    fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold,
                    fontSize = fontSize, letterSpacing = 0.8.sp, color = Color.Black
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // STATS
        Button(
            onClick = { statsViewModel.loadStats(com.example.timecard.domain.StatsPeriod.ThisWeek); navController.navigate("stats") },
            colors = ButtonDefaults.buttonColors(containerColor = LcarsTan),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
            modifier = Modifier.height(btnHeight)
        ) {
            Text("STATS", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = fontSize, letterSpacing = 0.8.sp, color = Color.Black)
        }

        // SAVE — LCARS-coloured animated save button
        LcarsSaveButton(status = uiState.saveStatus, onClick = { timesheetViewModel.saveData() }, isCompact = isCompact)

        // LOGOUT
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = LcarsRed),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
            modifier = Modifier.height(btnHeight)
        ) {
            Text("LOGOUT", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = fontSize, letterSpacing = 0.8.sp, color = Color.White)
        }

        // Leaderboard
        if (leaderboardViewModel != null) {
            Button(
                onClick = { leaderboardViewModel.load(employees.filter { !it.excluded }.map { it.name }, uiState.activeWeekDate, repository); navController.navigate("leaderboard") },
                colors = ButtonDefaults.buttonColors(containerColor = LcarsPurple),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
                modifier = Modifier.height(btnHeight)
            ) {
                Text("🏆", fontSize = fontSize)
            }
        }

        // Challenges
        if (challengesViewModel != null) {
            Button(
                onClick = { navController.navigate("challenges") },
                colors = ButtonDefaults.buttonColors(containerColor = LcarsBlueBell),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = hPad, vertical = 0.dp),
                modifier = Modifier.height(btnHeight)
            ) {
                Text("🎯", fontSize = fontSize)
            }
        }

        // Profile avatar with LCARS orange border
        if (profileViewModel != null) {
            val streak = profileViewModel.profile.streaks.currentDaily
            val avatar = profileViewModel.profile.avatar
            val avatarImage = profileViewModel.avatarImage
            Box(
                modifier = Modifier
                    .size(btnHeight)
                    .border(2.dp, LcarsOrange, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable { navController.navigate("settings") },
                contentAlignment = Alignment.Center
            ) {
                if (avatar == "custom" && avatarImage != null) {
                    Image(bitmap = remember(avatarImage) { android.graphics.BitmapFactory.decodeByteArray(avatarImage, 0, avatarImage.size).asImageBitmap() }, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    com.example.timecard.ui.components.InitialsAvatar(name = profileViewModel.profile.displayName ?: employeeName, size = btnHeight, fontSize = if (isCompact) 16.sp else 18.sp, bgColor = LcarsOrange.copy(alpha = 0.2f))
                }
                val badgeText = if (streak >= 3) "🔥$streak" else "⭐"
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(vertical = 1.dp), contentAlignment = Alignment.Center) {
                    Text(text = badgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        ThemeToggle(themeState = themeState, size = btnHeight)
    }
}

// LCARS-styled animated save button
@Composable
private fun LcarsSaveButton(status: SaveStatus, onClick: () -> Unit, isCompact: Boolean) {
    val btnHeight = if (isCompact) 34.dp else 38.dp
    val btnWidth = if (isCompact) 72.dp else 80.dp
    val syncWidth = if (isCompact) 34.dp else 38.dp
    val fontSize = if (isCompact) 11.sp else 13.sp

    val targetColor = when (status) {
        SaveStatus.SAVED   -> Color(0xFF99FF66)
        SaveStatus.SYNCING -> LcarsOrange
        SaveStatus.ERROR   -> LcarsRed
    }
    val targetWidth = if (status == SaveStatus.SYNCING) syncWidth else btnWidth
    val width by androidx.compose.animation.core.animateDpAsState(targetValue = targetWidth, label = "w")
    val color by androidx.compose.animation.animateColorAsState(targetValue = targetColor, label = "c")

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(btnHeight).width(width),
        contentPadding = PaddingValues(0.dp)
    ) {
        androidx.compose.animation.AnimatedContent(targetState = status, transitionSpec = { androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut() }, label = "save") { s ->
            when (s) {
                SaveStatus.SYNCING -> androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(if (isCompact) 18.dp else 20.dp), color = Color.Black, strokeWidth = 2.dp)
                SaveStatus.SAVED   -> Text("SAVED", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = fontSize, letterSpacing = 0.8.sp, color = Color.Black)
                SaveStatus.ERROR   -> Text("ERROR", fontFamily = AntonioFontFamily, fontWeight = FontWeight.Bold, fontSize = fontSize, letterSpacing = 0.8.sp, color = Color.White)
            }
        }
    }
}
