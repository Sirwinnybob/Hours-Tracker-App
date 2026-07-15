package com.example.timecard.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.timecard.data.model.Employee
import com.example.timecard.ui.components.AutocompleteTextField
import com.example.timecard.ui.components.InitialsAvatar
import com.example.timecard.ui.components.ThemeToggle
import com.example.timecard.ui.theme.AccentBlue
import com.example.timecard.ui.theme.LocalTimecardColors
import com.example.timecard.ui.theme.ThemeState
import androidx.compose.ui.graphics.RectangleShape
import com.example.timecard.ui.theme.AntonioFontFamily
import com.example.timecard.ui.theme.LcarsMelrose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Position + size of the header name placeholder, in root px coordinates */
data class HeaderMetrics(
    val x: Float,       // left edge X
    val centerY: Float, // vertical center Y
    val widthPx: Float, // placeholder width in px
    val heightPx: Float // placeholder height in px
)

@Composable
fun NameCard(
    viewModel: LoginViewModel,
    themeState: ThemeState,
    isExpanded: Boolean,
    employeeName: String?,
    displayName: String? = null,  // Optional override shown in collapsed badge
    avatar: String? = null,       // Emoji avatar shown when collapsed
    avatarImage: ByteArray? = null, // Custom image avatar bytes (when avatar == "custom")
    headerTarget: () -> HeaderMetrics?,
    launchedByKkc: Boolean = false,
    onLoginSuccess: (Employee) -> Unit,
    onCollapseComplete: () -> Unit,
    onExpandComplete: () -> Unit,
    onReinstallLatest: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalTimecardColors.current
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Animation progress: 1 = expanded, 0 = collapsed
    val progress = remember { Animatable(1f) }

    // Card center and width in root coordinates (updates every frame)
    var cardCenter by remember { mutableStateOf<Offset?>(null) }
    var cardWidthPx by remember { mutableStateOf(0f) }

    // Translation for flying between center and header
    val flyX = remember { Animatable(0f) }
    val flyY = remember { Animatable(0f) }

    // Title animation
    val titleAlpha = remember { Animatable(0f) }
    val titleSlideY = remember { Animatable(0f) }

    // Entry animation (first load only)
    var hasEnteredOnce by remember { mutableStateOf(false) }
    val entryCardAlpha = remember { Animatable(0f) }
    val entryCardOffsetY = remember { Animatable(60f) }

    // Initialize and play entry animation
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        if (!isExpanded) {
            // KKC auto-login: start hidden, skip entry animation entirely
            entryCardAlpha.snapTo(0f)
            progress.snapTo(0f)
            hasEnteredOnce = true
            onCollapseComplete()
            return@LaunchedEffect
        }
        titleAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(300)
        launch { entryCardAlpha.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        entryCardOffsetY.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        hasEnteredOnce = true
        focusRequester.requestFocus()
    }

    // Reposition when collapsed and layout changes (e.g., orientation change)
    LaunchedEffect(headerTarget()) {
        if (!isExpanded && hasEnteredOnce && progress.value < 0.01f) {
            val metrics = headerTarget()
            val current = cardCenter
            if (metrics != null && current != null) {
                val dx = metrics.x - (current.x - cardWidthPx / 2f)
                val dy = metrics.centerY - current.y
                flyX.snapTo(dx)
                flyY.snapTo(dy)
            }
        }
    }

    // Collapse / expand animation
    LaunchedEffect(isExpanded) {
        if (!hasEnteredOnce) return@LaunchedEffect

        if (!isExpanded) {
            // COLLAPSE
            keyboardController?.hide()
            focusManager.clearFocus()
            delay(150)

            // Title flies UP off screen while card shrinks
            launch { titleAlpha.animateTo(0f, tween(450)) }
            launch { titleSlideY.animateTo(-400f, tween(450, easing = FastOutSlowInEasing)) }

            // Phase 1: Shrink in place (button/spacer/padding collapse)
            progress.animateTo(0f, tween(600, easing = FastOutSlowInEasing))

            // Phase 2: Fly the compact card to header
            delay(50) // one frame for layout to settle
            val metrics = headerTarget()
            val current = cardCenter
            if (metrics != null && current != null) {
                // Align card left edge with placeholder left edge
                val dx = metrics.x - (current.x - cardWidthPx / 2f)
                val dy = metrics.centerY - current.y
                coroutineScope {
                    launch { flyX.animateTo(dx, tween(950, easing = FastOutSlowInEasing)) }
                    launch { flyY.animateTo(dy, tween(950, easing = FastOutSlowInEasing)) }
                }
            }

            onCollapseComplete()
        } else {
            // EXPAND
            // Snap fly to current offset (at header)
            val metrics = headerTarget()
            val current = cardCenter
            if (metrics != null && current != null) {
                val dx = metrics.x - (current.x - cardWidthPx / 2f)
                val dy = metrics.centerY - current.y
                flyX.snapTo(dx)
                flyY.snapTo(dy)
            }

            // Phase 1: Fly back to center
            coroutineScope {
                launch { flyX.animateTo(0f, tween(950, easing = FastOutSlowInEasing)) }
                launch { flyY.animateTo(0f, tween(950, easing = FastOutSlowInEasing)) }
            }

            // Phase 2: Expand (button/spacer/padding grow)
            progress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))

            // Title slides down from above
            titleSlideY.snapTo(-400f)
            coroutineScope {
                launch { titleAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
                launch { titleSlideY.animateTo(0f, tween(450, easing = FastOutSlowInEasing)) }
            }

            focusRequester.requestFocus()
            onExpandComplete()
        }
    }

    // Auto-login on 3-digit ID with typewriter effect
    var autoMatchedEmployee by remember { mutableStateOf<Employee?>(null) }
    var autoMatchId by remember { mutableStateOf("") }

    // Detect 3-digit match (non-destructive — just sets trigger state)
    LaunchedEffect(viewModel.loginInput) {
        if (autoMatchedEmployee != null) return@LaunchedEffect
        val match = viewModel.checkAutoLogin()
        if (match != null) {
            autoMatchId = viewModel.loginInput
            autoMatchedEmployee = match
        }
    }

    // Run typewriter animation when match is detected (separate effect, won't be cancelled by input changes)
    LaunchedEffect(autoMatchedEmployee) {
        val match = autoMatchedEmployee ?: return@LaunchedEffect
        // Hide keyboard and clear focus first
        keyboardController?.hide()
        focusManager.clearFocus()
        
        // Fetch custom display name if available
        val displayNameToType = viewModel.getDisplayName(match, context)
        
        delay(400)

        // De-type the ID digits
        for (i in autoMatchId.length - 1 downTo 0) {
            viewModel.loginInput = autoMatchId.substring(0, i)
            delay(50)
        }
        delay(100)

        // Type the name in
        val name = displayNameToType
        for (i in 1..name.length) {
            viewModel.loginInput = name.substring(0, i)
            delay(35)
        }
        delay(150)

        autoMatchedEmployee = null
        onLoginSuccess(match)
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.onSyncFolderSelected(context, uri)
    }

    // Interpolated values driven by progress (1 = expanded, 0 = collapsed)
    val metrics = headerTarget()
    val collapsedWidthDp = if (metrics != null) with(density) { metrics.widthPx.toDp().value } else 180f
    val collapsedHeightDp = if (metrics != null) with(density) { metrics.heightPx.toDp().value } else 48f

    val p = progress.value
    val cardPaddingDp = 30f * p                                    // 30dp → 0dp
    val cornerRadius = 20f * p + 12f * (1f - p)                   // 20dp → 12dp
    val elevation = 20f * p                                         // 20dp → 0dp
    val cardMaxWidth = 400f * p + collapsedWidthDp * (1f - p)     // 400dp → measured width
    val spacerHeight = 20f * p                           // 20dp → 0dp
    val buttonHeight = 50f * p                           // 50dp → 0dp
    val buttonAlpha = p

    val resolvedShape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(cornerRadius.dp))
    val cardBackground = if (colors.isLcars) Color.Black else colors.surface
    val cardShape = if (colors.isLcars) RectangleShape else resolvedShape

    val activity = LocalContext.current as? android.app.Activity

    Box(modifier = Modifier.fillMaxSize()) {
        // KKC navigation button — shown just above the login modal when launched by KKC and expanded
        if (launchedByKkc && (p > 0.01f || !hasEnteredOnce)) {
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent
                ),
                shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(8.dp)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-150).dp)
                    .zIndex(15f)
                    .graphicsLayer {
                        alpha = if (!hasEnteredOnce) entryCardAlpha.value else p
                    }
            ) {
                Text("← KKC", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Title — only visible when expanded
        if (p > 0.01f || titleAlpha.value > 0.01f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 120.dp)
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = titleSlideY.value
                    }
            ) {
                Text(
                    text = "Digital Timesheet",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        // The card — always contains the text field, never swaps content
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (24f * p).dp) // padding shrinks to 0 when collapsed
                .zIndex(10f)
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .widthIn(max = cardMaxWidth.dp)
                    // No height clamp — let TextField size naturally (~56dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val sz = coords.size
                        cardCenter = Offset(pos.x + sz.width / 2f, pos.y + sz.height / 2f)
                        cardWidthPx = sz.width.toFloat()
                    }
                    .graphicsLayer {
                        translationX = flyX.value
                        translationY = flyY.value
                        shadowElevation = if (colors.isLcars) 0f else with(density) { elevation.dp.toPx() }
                        shape = cardShape
                        clip = true
                        // Entry animation
                        if (!hasEnteredOnce) {
                            alpha = entryCardAlpha.value
                            translationY = with(density) { entryCardOffsetY.value.dp.toPx() }
                        }
                    }
                    .background(cardBackground, cardShape)
            ) {
                Column(
                    horizontalAlignment = if (p > 0.5f) Alignment.CenterHorizontally else Alignment.Start,
                    modifier = Modifier.padding(if (colors.isLcars && !isExpanded) 0.dp else cardPaddingDp.dp)
                ) {
                    val collapsedText = if (!isExpanded && displayName != null) displayName else viewModel.loginInput
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExpanded) {
                            AutocompleteTextField(
                                value = collapsedText,
                                onValueChange = { viewModel.loginInput = it },
                                suggestions = viewModel.filteredEmployees,
                                onSuggestionSelected = { name -> viewModel.loginInput = name },
                                onSubmit = {
                                    val employee = viewModel.attemptLogin()
                                    if (employee != null) onLoginSuccess(employee)
                                },
                                focusRequester = focusRequester,
                                enabled = true,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = (16f * p).dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            if (colors.isLcars) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .background(LcarsMelrose)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = collapsedText.uppercase(),
                                            color = Color.Black,
                                            fontFamily = AntonioFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp).height(28.dp).background(Color.Black))
                                }
                            } else {
                                Text(
                                    text = collapsedText,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }

                    // Spacer + Button collapse to 0 height when progress → 0
                    if (p > 0.01f) {
                        Spacer(modifier = Modifier.height(spacerHeight.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight.dp)
                                .graphicsLayer { alpha = buttonAlpha }
                        ) {
                            Button(
                                onClick = {
                                    val employee = viewModel.attemptLogin()
                                    if (employee != null) onLoginSuccess(employee)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (colors.isRed) Color(0xFFCC0000) else AccentBlue
                                ),
                                shape = com.example.timecard.ui.theme.timecardShape(RoundedCornerShape(12.dp)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("ENTER", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Links below card — only when expanded
        if (p > 0.01f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 380.dp)
                    .graphicsLayer { alpha = p }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!viewModel.isConnected) {
                        TextButton(onClick = { dirPickerLauncher.launch(null) }) {
                            Text(
                                "📂 Connect Sync Folder",
                                color = Color(0xFFA3BFFA),
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (viewModel.isDebugBuild) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onReinstallLatest) {
                            Text(
                                "🔄 Reinstall Latest Debug APK",
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Theme toggle — positioned to match TimesheetScreen header exactly
        // Hidden when collapsed (p ≈ 0) since TimesheetScreen has its own toggle
        if (p > 0.01f || !hasEnteredOnce) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 10.dp, top = 8.dp)
                    .height(48.dp)
                    .graphicsLayer {
                        alpha = if (!hasEnteredOnce) entryCardAlpha.value else p
                    }
            ) {
                ThemeToggle(themeState = themeState)
            }
        }
    }
}
