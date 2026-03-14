package com.example.timecard.ui.timesheet

import android.content.res.Configuration
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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import android.graphics.BitmapFactory
import com.example.timecard.ui.shop.ShopViewModel

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.timecard.ui.theme.ErrorRed
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.backgroundBrush)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top bar: name + week info + controls
                    // Scale factors for different screen sizes
                    val isCompact = remember(screenWidth) { screenWidth < 900 }
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
                        // Left: name placeholder + week info (side by side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Invisible placeholder — sized to match collapsed NameCard width
                            Box(
                                modifier = Modifier
                                    .widthIn(min = namePlaceholderMin)
                                    .height(40.dp)
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
                                // Padding matches Material3 TextField internal horizontal padding
                                // Measure the display name width (or actual name if no display name set)
                                val nameForMeasure = profileViewModel?.profile?.displayName ?: employeeName
                                Text(
                                    text = nameForMeasure,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = Color.Transparent,
                                    modifier = Modifier // No padding, match collapsed input exactly
                                )
                            }
        
                            Spacer(modifier = Modifier.width(4.dp))
        
                            // Week info + nav button stacked vertically
                            Column {
                                Text(
                                    text = "Week of ${uiState.activeWeekDate}",
                                    fontSize = if (isCompact) 10.sp else 12.sp,
                                    color = colors.textSecondary
                                )
                                if (uiState.hasPreviousWeek) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Button(
                                        onClick = { timesheetViewModel.togglePrevWeek() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.isViewingPrevious) {
                                                Color(0xFF48BB78)
                                            } else {
                                                Color(0xFFECC94B)
                                            }
                                        ),
                                        shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(6.dp)),
                                        contentPadding = PaddingValues(horizontal = if (isCompact) 8.dp else 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(if (isCompact) 24.dp else 28.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.isViewingPrevious) {
                                                "📅 Current"
                                            } else {
                                                "⏪ Revise Prev"
                                            },
                                            fontSize = if (isCompact) 9.sp else 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
        
                        // Right: action buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(btnSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Past alerts button removed from here
        
                            // Stats
                            Button(
                                onClick = {
                                    statsViewModel.loadStats(StatsPeriod.ThisWeek)
                                    navController.navigate("stats")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                                shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                modifier = Modifier.height(btnHeight),
                                contentPadding = PaddingValues(horizontal = btnPaddingH)
                            ) {
                                Text("STATS", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                            }
        
                            // Save (animated)
                            SaveButton(
                                status = uiState.saveStatus,
                                onClick = { timesheetViewModel.saveData() },
                                isCompact = isCompact
                            )
        
                            // Logout
                            Button(
                                onClick = {
                                    timesheetViewModel.logout()
                                    onLogout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.textOrange),
                                shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                modifier = Modifier.height(btnHeight),
                                contentPadding = PaddingValues(horizontal = if (isCompact) 20.dp else 30.dp)
                            ) {
                                Text("LOGOUT", color = colors.textOrange, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                            }
        
                            // Leaderboard button
                            if (leaderboardViewModel != null) {
                                Button(
                                    onClick = {
                                        leaderboardViewModel.load(
                                            employees.filter { !it.excluded }.map { it.name },
                                            uiState.activeWeekDate,
                                            repository
                                        )
                                        navController.navigate("leaderboard")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.hover),
                                    shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                                    modifier = Modifier.height(btnHeight),
                                    contentPadding = PaddingValues(horizontal = btnPaddingH)
                                ) {
                                    Text("🏆", color = colors.textPrimary, fontSize = btnFontSize, fontWeight = FontWeight.Bold)
                                }
                            }
        
                            // Profile / Settings button with streak badge
                            if (profileViewModel != null) {
                                val streak = profileViewModel.profile.streaks.currentDaily
                                val avatar = profileViewModel.profile.avatar
                                val avatarImage = profileViewModel.avatarImage

                                Box(
                                    modifier = Modifier
                                        .size(btnHeight)
                                        .clip(com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)))
                                        .clickable { navController.navigate("settings") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatar == "custom" && avatarImage != null) {
                                        Image(
                                            bitmap = remember(avatarImage) { BitmapFactory.decodeByteArray(avatarImage, 0, avatarImage.size).asImageBitmap() },
                                            contentDescription = "Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        com.example.timecard.ui.components.InitialsAvatar(
                                            name = profileViewModel.profile.displayName ?: employeeName,
                                            size = btnHeight,
                                            fontSize = if (isCompact) 18.sp else 22.sp,
                                            bgColor = colors.hover
                                        )
                                    }
                                    
                                    val badgeText = if (streak >= 3) "🔥$streak" else "⭐"
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
        
                            ThemeToggle(themeState = themeState, size = btnHeight)
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
                                Text(
                                    "Timesheet Locked",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "If you are seeing this it means the timecard is currently being used by another tablet or has been modified within the last 5 minutes.\n\n" +
                                    "If this lock persists try:\n" +
                                    "1. Waiting 5 minutes\n" +
                                    "2. Syncing your tablet.\n" +
                                    "3. Restarting then syncing your tablet.\n\n" +
                                    "If this lock persists tell Winston and he will get it unlocked for you.",
                                    fontSize = 16.sp,
                                    color = colors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            TimesheetGrid(
                                uiState = uiState,
                                onJobChange = timesheetViewModel::setJob,
                                onHoursChange = timesheetViewModel::setHours,
                                onFillShopHours = timesheetViewModel::fillShopHours,
                                onSnapHours = timesheetViewModel::snapHours,
                                onAddRow = timesheetViewModel::addRow,
                                onDeliveryTag = timesheetViewModel::toggleDeliveryTag,
                                onJobTag = timesheetViewModel::setJobTag,
                                onToggleNoLunch = timesheetViewModel::toggleNoLunch,
                                chartsContent = null,
                                modifier = Modifier.weight(0.66f)
                            )
                            ChartsSection(
                                currentData = timesheetViewModel.collectTimecardData(),
                                previousData = uiState.previousWeekData,
                                expanded = true,
                                onToggle = {},
                                showToggle = false,
                                modifier = Modifier.weight(0.34f)
                            )
                        }
                    } else {
                        TimesheetGrid(
                            uiState = uiState,
                            onJobChange = timesheetViewModel::setJob,
                            onHoursChange = timesheetViewModel::setHours,
                            onFillShopHours = timesheetViewModel::fillShopHours,
                            onSnapHours = timesheetViewModel::snapHours,
                            onAddRow = timesheetViewModel::addRow,
                            onDeliveryTag = timesheetViewModel::toggleDeliveryTag,
                            onJobTag = timesheetViewModel::setJobTag,
                            onToggleNoLunch = timesheetViewModel::toggleNoLunch,
                            chartsContent = {
                                ChartsSection(
                                    currentData = timesheetViewModel.collectTimecardData(),
                                    previousData = uiState.previousWeekData,
                                    expanded = showCharts,
                                    onToggle = { showCharts = !showCharts }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
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
                com.example.timecard.ui.profile.LeaderboardModal(
                    viewModel = leaderboardViewModel,
                    myName = employeeName,
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
