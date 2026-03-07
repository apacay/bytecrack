package com.bytecrack.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bytecrack.domain.model.Difficulty
import com.bytecrack.ui.components.CodeInputDisplay
import com.bytecrack.ui.components.DifficultyBadge
import com.bytecrack.ui.components.GlitchText
import com.bytecrack.ui.components.GuessHistoryItem
import com.bytecrack.ui.components.HackerKeyboard
import com.bytecrack.ui.components.LevelProgressBadge
import com.bytecrack.ui.components.MatrixRain
import com.bytecrack.ui.components.ScanlineOverlay
import com.bytecrack.ui.components.TerminalText
import com.bytecrack.ui.components.TimerBar
import com.bytecrack.ui.viewmodel.GameOverReason
import com.bytecrack.ui.viewmodel.GameScreen as GameScreenState
import com.bytecrack.ui.viewmodel.GameUiState
import com.bytecrack.ui.viewmodel.GameViewModel

@Composable
fun GameScreen(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val activity = LocalContext.current as? Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (uiState.screen) {
            GameScreenState.GameOver,
            GameScreenState.Game -> {
                if (uiState.screen == GameScreenState.Game) {
                    LaunchedEffect(uiState.screen) {
                        if (activity != null) viewModel.preloadAds(activity)
                    }
                }
                GamePlayContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBackToMenu = onBackToMenu,
                    activity = activity
                )
            }
            GameScreenState.DifficultyChoice -> DifficultyChoiceContent(
                uiState = uiState,
                onSelectDifficulty = { diff: Difficulty -> viewModel.requestInterstitialAndSelectDifficulty(activity, diff) }
            )
            GameScreenState.MainMenu,
            GameScreenState.Leaderboard -> {}
        }

        ScanlineOverlay()
    }
}

@Composable
private fun GamePlayContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    activity: Activity?
) {
    val secretCode = uiState.secretCode
    val digitCount = secretCode?.size ?: 0
    val config = LocalConfiguration.current
    val isLandscape = config.screenWidthDp > config.screenHeightDp

    val isInTransition = with(uiState) {
        showLevelIntro || isLevelComplete ||
        showPotentialGameOverAfterReward || showTransitionBackToGame ||
        showTransitionToGiveUp || showFailureTransition ||
        showDiscoveredTransition || offerRewardedAd ||
        showEscapeTransition || offerTraceAd || showTraceAdOfferAtStart ||
        isGameOver
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
    ) {
        // Header — siempre visible, incluso durante transiciones
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0d1117))
                .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.35f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(Color(0xFFFF5F57), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(Modifier.size(5.dp).background(Color(0xFFFFBD2E), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(Modifier.size(5.dp).background(Color(0xFF00FF41), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    LevelProgressBadge(level = uiState.level, difficulty = uiState.difficulty)
                    Spacer(modifier = Modifier.width(6.dp))
                    DifficultyBadge(difficulty = uiState.difficulty)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.3f))
                            .clickable { viewModel.toggleMusic() }
                            .padding(horizontal = 5.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isMusicEnabled) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF00FF41).copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.45f))
                            .background(Color(0xFF00FF41).copy(alpha = 0.05f))
                            .clickable { onBackToMenu() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ESC",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF41).copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerBar(
                    timeRemaining = uiState.timeRemainingSeconds,
                    modifier = Modifier.weight(0.75f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SCORE:${uiState.totalScore}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF41).copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BR:${uiState.attemptsRemaining}/${uiState.maxAttempts}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = when {
                        uiState.attemptsRemaining <= 2 -> Color(0xFFFF0000).copy(alpha = 0.85f)
                        uiState.attemptsRemaining <= 5 -> Color(0xFFFF6600).copy(alpha = 0.85f)
                        else -> Color(0xFF00FF41).copy(alpha = 0.35f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLandscape) {
            // Landscape: consola a la izquierda, controles a la derecha con teclado abajo
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameConsoleBox(
                    uiState = uiState,
                    isInTransition = isInTransition,
                    viewModel = viewModel,
                    activity = activity,
                    digitCount = digitCount,
                    onBackToMenu = onBackToMenu,
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                )
                if (!isInTransition) {
                    Spacer(modifier = Modifier.width(6.dp))
                    GameControls(
                        uiState = uiState,
                        viewModel = viewModel,
                        digitCount = digitCount,
                        pushToBottom = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            // Portrait: layout en columna original
            GameConsoleBox(
                uiState = uiState,
                isInTransition = isInTransition,
                viewModel = viewModel,
                activity = activity,
                digitCount = digitCount,
                onBackToMenu = onBackToMenu,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            if (!isInTransition) {
                Spacer(modifier = Modifier.height(6.dp))
                GameControls(
                    uiState = uiState,
                    viewModel = viewModel,
                    digitCount = digitCount,
                    pushToBottom = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

        // Popup de Game Over — otra consola sobre la principal
        if (uiState.isGameOver) {
            GameOverPopup(
                uiState = uiState,
                onBackToMenu = onBackToMenu
            )
        }
    }
}

private data class LogLine(val text: String, val color: Color)

// ─── Consola principal de juego ───────────────────────────────────────────────

@Composable
private fun GameConsoleBox(
    uiState: GameUiState,
    isInTransition: Boolean,
    viewModel: GameViewModel,
    activity: Activity?,
    digitCount: Int,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val completedLog = remember { mutableStateListOf<LogLine>() }
    var prevUiState by remember { mutableStateOf(uiState) }

    SideEffect {
        val prev = prevUiState
        val curr = uiState

        if (curr.level != prev.level && curr.guesses.isEmpty()) {
            completedLog.clear()
        }

        val green = Color(0xFF00FF41)
        val red = Color(0xFFFF0000)
        val cyan = Color(0xFF00FFFF)
        val orange = Color(0xFFFF6600)

        if (prev.showLevelIntro && !curr.showLevelIntro) {
            listOf(
                "Connecting to target host...",
                "Initializing payload sequence...",
                "Compiling exploit module...",
                "Loading bytecrack engine...",
                "Injecting into game loop...",
                "Establishing secure channel...",
                "Access granted — level ready"
            ).forEach { completedLog.add(LogLine("> $it", green.copy(alpha = 0.7f))) }
        }
        if (prev.showVictoryPenetration && !curr.showVictoryPenetration) {
            listOf(
                "Validating payload integrity...",
                "Bypassing firewall rules...",
                "Injecting exploit module...",
                "Decrypting target buffer...",
                "Core component accessed",
                "Writing session token...",
                "PENETRATION SUCCESSFUL"
            ).forEach { completedLog.add(LogLine("> $it", green)) }
        }
        if (prev.isLevelComplete && !curr.isLevelComplete) {
            val tier = prev.lastTier
            val lines = buildList {
                add("─────────────────────────────")
                add("ACCESS GRANTED")
                if (tier != null) add("TIER: ${tier.name} — ${tier.displayName}")
                if (prev.wonViaReward) add("CONTINUATION (reward — no points)")
                else add("POINTS: +${prev.lastLevelPoints}")
            }
            lines.forEach { completedLog.add(LogLine("> $it", green.copy(alpha = 0.8f))) }
        }
        if (prev.showTransitionBackToGame && !curr.showTransitionBackToGame) {
            listOf(
                "Identity mask confirmed",
                "Breaches restored — timer resumed",
                "Session active"
            ).forEach { completedLog.add(LogLine("> $it", cyan)) }
        }
        if (prev.showTransitionToGiveUp && !curr.showTransitionToGiveUp) {
            listOf("Override refused", "Terminating session...", "Connection closed")
                .forEach { completedLog.add(LogLine("> $it", orange)) }
        }
        if (prev.showFailureTransition && !curr.showFailureTransition) {
            val lines = when (prev.pendingGameOverReason) {
                GameOverReason.TimeUp -> listOf(
                    "Security trace outpaced breach completion",
                    "Source IP identified and logged",
                    "Connection terminated by remote",
                    "─────────────────────────────",
                    "ACCESS DENIED — IP COMPROMISED"
                )
                else -> listOf(
                    "Suspicious repeated breach attempts detected",
                    "Security investigating source",
                    "IP trace completed — identity linked",
                    "─────────────────────────────",
                    "ACCESS DENIED — IP COMPROMISED"
                )
            }
            lines.forEach { completedLog.add(LogLine("> $it", red)) }
        }
        if (prev.showDiscoveredTransition && !curr.showDiscoveredTransition) {
            val lines = if (prev.pendingRewardAdFromTimeOut) {
                listOf(
                    "Security trace outpaced breach completion",
                    "Source IP identified and logged",
                    "Connection at risk"
                )
            } else {
                listOf(
                    "Suspicious repeated breach attempts detected",
                    "Security investigating source",
                    "IP trace in progress"
                )
            }
            lines.forEach { completedLog.add(LogLine("> $it", red)) }
        }
        if (prev.showEscapeTransition && !curr.showEscapeTransition) {
            listOf(
                "Identity mask applied",
                "Routing through alternate node",
                "Session re-established as new user",
                "Breach attempts restored",
                "Timer resumed",
                "Connection active"
            ).forEach { completedLog.add(LogLine("> $it", cyan)) }
        }

        prevUiState = curr
    }

    var showTransitionButtons by remember { mutableStateOf(false) }

    LaunchedEffect(
        uiState.isLevelComplete,
        uiState.offerRewardedAd,
        uiState.showPotentialGameOverAfterReward,
        uiState.offerTraceAd,
        uiState.showTraceAdOfferAtStart
    ) {
        if (!uiState.isLevelComplete && !uiState.offerRewardedAd &&
            !uiState.showPotentialGameOverAfterReward && !uiState.offerTraceAd &&
            !uiState.showTraceAdOfferAtStart
        ) {
            showTransitionButtons = false
        }
    }

    LaunchedEffect(completedLog.size, uiState.guesses.size, uiState.hintRevealedDigits.size, isInTransition, uiState.screen) {
        delay(80)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = modifier) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color(0xFF0a0f0a))
            .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        when {
            uiState.isLevelComplete ->
                MatrixRain(density = 0.10f, color = Color(0xFF00FF41).copy(alpha = 0.15f))
            uiState.showFailureTransition || uiState.showDiscoveredTransition || uiState.offerRewardedAd ->
                MatrixRain(density = 0.10f, color = Color(0xFFFF0000).copy(alpha = 0.12f))
            uiState.showEscapeTransition || uiState.showTransitionBackToGame ->
                MatrixRain(density = 0.08f, color = Color(0xFF00FFFF).copy(alpha = 0.10f))
            uiState.isGameOver ->
                MatrixRain(density = 0.10f, color = Color(0xFFFF0000).copy(alpha = 0.12f))
            else -> {}
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val consoleHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(consoleHeight))
                Text(
                    text = "> bytecrack // breach_log",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF00FF41).copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // 1. Transición de entrada (top)
                when {
                uiState.showLevelIntro -> {
                    InlineTypedLines(
                        lines = listOf(
                            "Connecting to target host...",
                            "Initializing payload sequence...",
                            "Compiling exploit module...",
                            "Loading bytecrack engine...",
                            "Injecting into game loop...",
                            "Establishing secure channel...",
                            "Access granted — level ready"
                        ),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        durationMs = 4500L,
                        onComplete = {}
                    )
                }
                else -> {
                // 2. Partida (medio): completedLog + guesses + hints
                completedLog.forEach { line ->
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = line.color,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                if (uiState.guesses.isEmpty() && !isInTransition) {
                    Text(
                        text = "> Awaiting input...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF00FF41).copy(alpha = 0.4f)
                    )
                }
                uiState.guesses.forEachIndexed { index, guess ->
                    GuessHistoryItem(guess = guess, attemptNumber = index + 1)
                }
                uiState.hintRevealedDigits.forEach { digit ->
                    Text(
                        text = "> TRACE: Dígito [$digit] está en la solución",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF00BFFF).copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 3. Transición de salida + scoring (bottom)
                when {
                uiState.isLevelComplete && uiState.showVictoryPenetration -> {
                    InlineTypedLines(
                        lines = listOf(
                            "Validating payload integrity...",
                            "Bypassing firewall rules...",
                            "Injecting exploit module...",
                            "Decrypting target buffer...",
                            "Core component accessed",
                            "Writing session token...",
                            "PENETRATION SUCCESSFUL"
                        ),
                        accentColor = Color(0xFF00FF41),
                        durationMs = 4500L,
                        onComplete = { viewModel.dismissVictoryPenetration() }
                    )
                }

                uiState.isLevelComplete -> {
                    InlineLevelComplete(
                        uiState = uiState,
                        onContinue = { viewModel.requestContinueWithAd(activity) },
                        onWatchAd = { activity?.let { act -> viewModel.requestTraceAd(act) } },
                        onSkip = { viewModel.skipTraceOffer() },
                        buttonsOutsideConsole = true,
                        onButtonsReady = { showTransitionButtons = true }
                    )
                }

                uiState.showPotentialGameOverAfterReward -> {
                    InlinePotentialGameOver(
                        onContinue = { viewModel.confirmContinueAfterReward() },
                        onGiveUp = { viewModel.confirmGiveUpAfterReward() },
                        buttonsOutsideConsole = true,
                        onButtonsReady = { showTransitionButtons = true }
                    )
                }

                uiState.showTransitionBackToGame -> {
                    InlineTypedLines(
                        lines = listOf(
                            "Identity mask confirmed",
                            "Breaches restored — timer resumed",
                            "Session active"
                        ),
                        accentColor = Color(0xFF00FFFF),
                        durationMs = 2500L,
                        onComplete = { viewModel.dismissTransitionBackToGame() }
                    )
                }

                uiState.showTransitionToGiveUp -> {
                    InlineTypedLines(
                        lines = listOf(
                            "Override refused",
                            "Terminating session...",
                            "Connection closed"
                        ),
                        accentColor = Color(0xFFFF6600),
                        durationMs = 2000L,
                        onComplete = { viewModel.dismissTransitionToGiveUp() }
                    )
                }

                uiState.showFailureTransition -> {
                    val failureLines = when (uiState.pendingGameOverReason) {
                        GameOverReason.TimeUp -> listOf(
                            "Security trace outpaced breach completion",
                            "Source IP identified and logged",
                            "Connection terminated by remote",
                            "─────────────────────────────",
                            "ACCESS DENIED — IP COMPROMISED"
                        )
                        else -> listOf(
                            "Suspicious repeated breach attempts detected",
                            "Security investigating source",
                            "IP trace completed — identity linked",
                            "─────────────────────────────",
                            "ACCESS DENIED — IP COMPROMISED"
                        )
                    }
                    InlineTypedLines(
                        lines = failureLines,
                        accentColor = Color(0xFFFF0000),
                        durationMs = 4500L,
                        onComplete = { viewModel.completeGameOver() }
                    )
                }

                uiState.showDiscoveredTransition -> {
                    val discoveredLines = when (uiState.pendingRewardAdFromTimeOut) {
                        true -> listOf(
                            "Security trace outpaced breach completion",
                            "Source IP identified and logged",
                            "Connection at risk"
                        )
                        else -> listOf(
                            "Suspicious repeated breach attempts detected",
                            "Security investigating source",
                            "IP trace in progress"
                        )
                    }
                    InlineTypedLines(
                        lines = discoveredLines,
                        accentColor = Color(0xFFFF0000),
                        durationMs = 1800L,
                        onComplete = { viewModel.dismissDiscoveredTransition() }
                    )
                }

                uiState.offerRewardedAd -> {
                    InlineRewardedAdPrompt(
                        difficulty = uiState.difficulty,
                        fromTimeOut = uiState.offerRewardedAdFromTimeOut,
                        onWatchAd = { activity?.let { act -> viewModel.requestRewardedAd(act) } },
                        onDecline = { viewModel.declineExtraAttempt() },
                        buttonsOutsideConsole = true,
                        onButtonsReady = { showTransitionButtons = true }
                    )
                }

                uiState.showEscapeTransition -> {
                    InlineTypedLines(
                        lines = listOf(
                            "Identity mask applied",
                            "Routing through alternate node",
                            "Session re-established as new user",
                            "Breach attempts restored",
                            "Timer resumed",
                            "Connection active"
                        ),
                        accentColor = Color(0xFF00FFFF),
                        durationMs = 2200L,
                        onComplete = { viewModel.dismissEscapeTransition() }
                    )
                }

                uiState.offerTraceAd -> {
                    InlineTracePurchasePrompt(
                        onWatchAd = { activity?.let { act -> viewModel.requestTraceAdForPurchase(act) } },
                        onDecline = { viewModel.declineTraceAd() },
                        buttonsOutsideConsole = true,
                        onButtonsReady = { showTransitionButtons = true }
                    )
                }

                uiState.showTraceAdOfferAtStart -> {
                    InlineTraceAdOfferPrompt(
                        onWatchAd = { activity?.let { act -> viewModel.requestTraceAd(act) } },
                        onSkip = { viewModel.skipTraceOffer() },
                        buttonsOutsideConsole = true,
                        onButtonsReady = { showTransitionButtons = true }
                    )
                }

                uiState.isGameOver -> {
                    // No mostrar inline — el popup GameOverPopup se muestra encima
                }
            }
                }
            }

            if (!isInTransition) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    }

        // Botones de transición fuera de la consola (simplificación de lo que hace el hacker)
        if (showTransitionButtons) {
            TransitionActionButtons(
                uiState = uiState,
                activity = activity,
                onContinue = { viewModel.requestContinueWithAd(activity) },
                onWatchAd = { activity?.let { act -> viewModel.requestTraceAd(act) } },
                onSkip = { viewModel.skipTraceOffer() },
                onWatchRewardedAd = { activity?.let { act -> viewModel.requestRewardedAd(act) } },
                onDeclineRewarded = { viewModel.declineExtraAttempt() },
                onWatchTracePurchase = { activity?.let { act -> viewModel.requestTraceAdForPurchase(act) } },
                onDeclineTracePurchase = { viewModel.declineTraceAd() },
                onWatchTraceAtStart = { activity?.let { act -> viewModel.requestTraceAd(act) } },
                onSkipTraceAtStart = { viewModel.skipTraceOffer() },
                onConfirmContinue = { viewModel.confirmContinueAfterReward() },
                onConfirmGiveUp = { viewModel.confirmGiveUpAfterReward() }
            )
        }
    }
}

// ─── Controles de juego ────────────────────────────────────────────────────────

@Composable
private fun GameControls(
    uiState: GameUiState,
    viewModel: GameViewModel,
    digitCount: Int,
    pushToBottom: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRACE:${uiState.traceCount}",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF00BFFF).copy(alpha = 0.8f)
            )
            if (uiState.traceCount >= 1) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFF00BFFF).copy(alpha = 0.5f))
                        .clickable { viewModel.useTraceForHint() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "[HINT]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF00BFFF)
                    )
                }
            }
            if (uiState.traceCount >= 3 && uiState.crackedDigits.size < digitCount) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Toca slot para crackear",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = Color(0xFF00FF41).copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        CodeInputDisplay(
            currentInput = uiState.currentInput,
            digitCount = digitCount,
            crackedDigits = uiState.crackedDigits,
            onCrackPosition = if (uiState.traceCount >= 3) { { viewModel.useTracesForCrack(it) } } else null,
            modifier = Modifier.fillMaxWidth()
        )

        if (pushToBottom) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        HackerKeyboard(
            modifier = Modifier.fillMaxWidth(),
            isHex = uiState.difficulty.radix == 16,
            usedDigits = uiState.currentInput.toSet() + uiState.crackedDigits.values.toSet(),
            onDigit = viewModel::addDigit,
            onDelete = viewModel::removeDigit,
            onSubmit = viewModel::submitCurrentInput,
            submitEnabled = uiState.currentInput.size == digitCount - uiState.crackedDigits.size
        )
    }
}

// ─── Componente genérico de texto con animación de tipeo ─────────────────────

@Composable
private fun InlineTypedLines(
    lines: List<String>,
    accentColor: Color = Color(0xFF00FF41),
    durationMs: Long = 4000L,
    onComplete: () -> Unit = {}
) {
    var currentLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val totalChars = lines.sumOf { it.length + 2 }
        val charDelay = (durationMs / totalChars.coerceAtLeast(1)).coerceAtLeast(38L)

        for (lineIdx in lines.indices) {
            currentLine = lineIdx
            val fullLine = "> ${lines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(350)
        onComplete()
    }

    Column {
        lines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < currentLine -> fullLine.length
                index == currentLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == currentLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = if (index < currentLine) accentColor.copy(alpha = 0.7f) else accentColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

// ─── Botón de terminal inline ─────────────────────────────────────────────────

@Composable
private fun TerminalButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.55f))
            .background(color.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = color
        )
    }
}

// ─── Level complete inline ────────────────────────────────────────────────────

@Composable
private fun InlineLevelComplete(
    uiState: GameUiState,
    onContinue: () -> Unit,
    onWatchAd: () -> Unit,
    onSkip: () -> Unit,
    buttonsOutsideConsole: Boolean = false,
    onButtonsReady: () -> Unit = {}
) {
    val tier = uiState.lastTier ?: return
    val green = Color(0xFF00FF41)
    val cyan = MaterialTheme.colorScheme.secondary
    val effectiveBonus = tier.bonusSeconds * uiState.difficulty.timeBonusMultiplier

    val infoLines = buildList {
        add("─────────────────────────────")
        add("ACCESS GRANTED")
        add("TIER: ${tier.name} — ${tier.displayName}")
        add("TIME BONUS: +${effectiveBonus}s" +
            if (uiState.difficulty.timeBonusMultiplier > 1) " (×${uiState.difficulty.timeBonusMultiplier})" else "")
        if (uiState.wonViaReward) {
            add("CONTINUATION (reward — no points)")
        } else {
            add("POINTS: +${uiState.lastLevelPoints} (×${uiState.difficulty.pointMultiplier})")
        }
        if (uiState.showTraceAdOffer) {
            add("─────────────────────────────")
            add("TRACE AVAILABLE")
            add("Watch a brief ad to earn +1 TRACE")
            add("(use for HINT or to crack a slot)")
        }
    }

    var currentLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val totalChars = infoLines.sumOf { it.length + 2 }
        val charDelay = (2800L / totalChars.coerceAtLeast(1)).coerceAtLeast(30L)

        for (lineIdx in infoLines.indices) {
            currentLine = lineIdx
            val fullLine = "> ${infoLines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(300)
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < currentLine -> fullLine.length
                index == currentLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                val color = when {
                    line == "ACCESS GRANTED" -> green
                    line.startsWith("TIER:") -> cyan
                    line.startsWith("─") -> green.copy(alpha = 0.3f)
                    line.startsWith("TRACE AVAILABLE") -> cyan
                    else -> green.copy(alpha = 0.8f)
                }
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == currentLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showButtons && !buttonsOutsideConsole) {
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.showTraceAdOffer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TerminalButton(
                        text = "[A] VER ANUNCIO",
                        color = cyan,
                        modifier = Modifier.weight(1f),
                        onClick = onWatchAd
                    )
                    TerminalButton(
                        text = "[B] CONTINUAR",
                        color = green.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        onClick = onSkip
                    )
                }
            } else {
                TerminalButton(
                    text = "[ CONTINUAR ]",
                    color = green,
                    onClick = onContinue
                )
            }
        }
    }
}

// ─── Rewarded ad prompt — last chance inline ──────────────────────────────────

@Composable
private fun InlineRewardedAdPrompt(
    difficulty: Difficulty,
    fromTimeOut: Boolean,
    onWatchAd: () -> Unit,
    onDecline: () -> Unit,
    buttonsOutsideConsole: Boolean = false,
    onButtonsReady: () -> Unit = {}
) {
    val red = Color(0xFFFF0000)
    val orange = Color(0xFFFF6600)
    val green = Color(0xFF00FF41)

    val contextLines = if (fromTimeOut) {
        listOf(
            "Security trace faster than breach completion",
            "Source IP identified and logged",
            "Connection flagged"
        )
    } else {
        listOf(
            "Suspicious repeated breach attempts detected",
            "Security investigating source",
            "IP trace in progress"
        )
    }

    val promptLines = listOf(
        "─────────────────────────────",
        "BREACH FAILED",
        if (fromTimeOut) "Trace outpaced payload — IP compromised" else "Repeated attempts flagged — IP trace initiated",
        "─────────────────────────────",
        "Override: mask as alternate identity",
        "Continue breach attempts as different user",
        "+${difficulty.rewardAdBreachExtension} attempts, +${difficulty.rewardAdSeconds}s — requires brief ad"
    )

    var typedPromptLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(180)
        val totalChars = promptLines.sumOf { it.length + 2 }
        val charDelay = (2200L / totalChars.coerceAtLeast(1)).coerceAtLeast(32L)

        for (lineIdx in promptLines.indices) {
            typedPromptLine = lineIdx
            val fullLine = "> ${promptLines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(300)
        showButtons = true
        onButtonsReady()
    }

    Column {
        // Contexto anterior (instantáneo — ya se "escribió" en la transición previa)
        contextLines.forEach { line ->
            Text(
                text = "> $line",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = red.copy(alpha = 0.65f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Prompt con tipeo animado
        promptLines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < typedPromptLine -> fullLine.length
                index == typedPromptLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                val color = when {
                    line == "BREACH FAILED" -> red
                    line.startsWith("─") -> orange.copy(alpha = 0.4f)
                    line.startsWith("Override") -> orange
                    else -> orange.copy(alpha = 0.8f)
                }
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == typedPromptLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showButtons && !buttonsOutsideConsole) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalButton(
                    text = "[A] EJECUTAR OVERRIDE",
                    color = orange,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchAd
                )
                TerminalButton(
                    text = "[B] ABANDONAR SESIÓN",
                    color = red.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = onDecline
                )
            }
        }
    }
}

// ─── Potential game over inline ───────────────────────────────────────────────

@Composable
private fun InlinePotentialGameOver(
    onContinue: () -> Unit,
    onGiveUp: () -> Unit,
    buttonsOutsideConsole: Boolean = false,
    onButtonsReady: () -> Unit = {}
) {
    val cyan = Color(0xFF00FFFF)
    val red = Color(0xFFFF0000)
    val green = Color(0xFF00FF41)

    val infoLines = listOf(
        "─────────────────────────────",
        "Game Over ...?",
        "─────────────────────────────",
        "Continue or end session?"
    )

    var currentLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val totalChars = infoLines.sumOf { it.length + 2 }
        val charDelay = (1800L / totalChars.coerceAtLeast(1)).coerceAtLeast(30L)

        for (lineIdx in infoLines.indices) {
            currentLine = lineIdx
            val fullLine = "> ${infoLines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < currentLine -> fullLine.length
                index == currentLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                val color = when {
                    line == "Game Over ...?" -> red
                    line.startsWith("─") -> cyan.copy(alpha = 0.3f)
                    else -> cyan.copy(alpha = 0.8f)
                }
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == currentLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showButtons && !buttonsOutsideConsole) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalButton(
                    text = "[A] CONTINUAR",
                    color = green,
                    modifier = Modifier.weight(1f),
                    onClick = onContinue
                )
                TerminalButton(
                    text = "[B] RENDIRSE",
                    color = red.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = onGiveUp
                )
            }
        }
    }
}

// ─── Trace offer inline (inicio de partida y cada 3 niveles) ─────────────────

@Composable
private fun InlineTraceAdOfferPrompt(
    onWatchAd: () -> Unit,
    onSkip: () -> Unit,
    buttonsOutsideConsole: Boolean = false,
    onButtonsReady: () -> Unit = {}
) {
    val cyan = Color(0xFF00BFFF)
    val green = Color(0xFF00FF41)

    val infoLines = listOf(
        "─────────────────────────────",
        "TRACE AVAILABLE",
        "Earn +1 TRACE by watching a brief ad",
        "TRACE gives: HINT (reveal digit) or CRACK (fix slot)",
        "─────────────────────────────"
    )

    var currentLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val totalChars = infoLines.sumOf { it.length + 2 }
        val charDelay = (2200L / totalChars.coerceAtLeast(1)).coerceAtLeast(30L)

        for (lineIdx in infoLines.indices) {
            currentLine = lineIdx
            val fullLine = "> ${infoLines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < currentLine -> fullLine.length
                index == currentLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                val color = when {
                    line == "TRACE AVAILABLE" -> cyan
                    line.startsWith("─") -> cyan.copy(alpha = 0.3f)
                    else -> cyan.copy(alpha = 0.8f)
                }
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == currentLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showButtons && !buttonsOutsideConsole) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalButton(
                    text = "[A] VER ANUNCIO",
                    color = cyan,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchAd
                )
                TerminalButton(
                    text = "[B] OMITIR",
                    color = green.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    onClick = onSkip
                )
            }
        }
    }
}

// ─── Trace purchase prompt inline (durante el juego) ─────────────────────────

@Composable
private fun InlineTracePurchasePrompt(
    onWatchAd: () -> Unit,
    onDecline: () -> Unit,
    buttonsOutsideConsole: Boolean = false,
    onButtonsReady: () -> Unit = {}
) {
    val cyan = Color(0xFF00BFFF)
    val green = Color(0xFF00FF41)

    val infoLines = listOf(
        "─────────────────────────────",
        "TRACE AVAILABLE",
        "Watch a brief ad to earn +1 TRACE",
        "Use TRACE for HINT or to crack a slot",
        "─────────────────────────────"
    )

    var currentLine by remember { mutableIntStateOf(0) }
    var currentChars by remember { mutableIntStateOf(0) }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val totalChars = infoLines.sumOf { it.length + 2 }
        val charDelay = (2000L / totalChars.coerceAtLeast(1)).coerceAtLeast(30L)

        for (lineIdx in infoLines.indices) {
            currentLine = lineIdx
            val fullLine = "> ${infoLines[lineIdx]}"
            for (i in fullLine.indices) {
                currentChars = i + 1
                delay(charDelay)
            }
            delay(charDelay * 2)
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val fullLine = "> $line"
            val charsToShow = when {
                index < currentLine -> fullLine.length
                index == currentLine -> currentChars
                else -> 0
            }
            if (charsToShow > 0) {
                val color = when {
                    line == "TRACE AVAILABLE" -> cyan
                    line.startsWith("─") -> cyan.copy(alpha = 0.3f)
                    else -> cyan.copy(alpha = 0.8f)
                }
                Text(
                    text = fullLine.substring(0, charsToShow.coerceAtMost(fullLine.length)) +
                        if (index == currentLine && charsToShow < fullLine.length) "█" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showButtons && !buttonsOutsideConsole) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalButton(
                    text = "[A] VER ANUNCIO",
                    color = cyan,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchAd
                )
                TerminalButton(
                    text = "[B] RECHAZAR",
                    color = green.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = onDecline
                )
            }
        }
    }
}

// ─── Botones de transición fuera de la consola ───────────────────────────────

@Composable
private fun TransitionActionButtons(
    uiState: GameUiState,
    activity: Activity?,
    onContinue: () -> Unit,
    onWatchAd: () -> Unit,
    onSkip: () -> Unit,
    onWatchRewardedAd: () -> Unit,
    onDeclineRewarded: () -> Unit,
    onWatchTracePurchase: () -> Unit,
    onDeclineTracePurchase: () -> Unit,
    onWatchTraceAtStart: () -> Unit,
    onSkipTraceAtStart: () -> Unit,
    onConfirmContinue: () -> Unit,
    onConfirmGiveUp: () -> Unit
) {
    val green = Color(0xFF00FF41)
    val cyan = Color(0xFF00BFFF)
    val orange = Color(0xFFFF6600)
    val red = Color(0xFFFF0000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            uiState.isLevelComplete -> {
                if (uiState.showTraceAdOffer) {
                    TerminalButton(
                        text = "[A] VER ANUNCIO",
                        color = cyan,
                        modifier = Modifier.weight(1f),
                        onClick = onWatchAd
                    )
                    TerminalButton(
                        text = "[B] CONTINUAR",
                        color = green.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        onClick = onSkip
                    )
                } else {
                    TerminalButton(
                        text = "[ CONTINUAR ]",
                        color = green,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onContinue
                    )
                }
            }
            uiState.offerRewardedAd -> {
                TerminalButton(
                    text = "[A] EJECUTAR OVERRIDE",
                    color = orange,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchRewardedAd
                )
                TerminalButton(
                    text = "[B] ABANDONAR SESIÓN",
                    color = red.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = onDeclineRewarded
                )
            }
            uiState.showPotentialGameOverAfterReward -> {
                TerminalButton(
                    text = "[A] CONTINUAR",
                    color = green,
                    modifier = Modifier.weight(1f),
                    onClick = onConfirmContinue
                )
                TerminalButton(
                    text = "[B] RENDIRSE",
                    color = red.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                    onClick = onConfirmGiveUp
                )
            }
            uiState.offerTraceAd -> {
                TerminalButton(
                    text = "[A] VER ANUNCIO",
                    color = cyan,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchTracePurchase
                )
                TerminalButton(
                    text = "[B] RECHAZAR",
                    color = green.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    onClick = onDeclineTracePurchase
                )
            }
            uiState.showTraceAdOfferAtStart -> {
                TerminalButton(
                    text = "[A] VER ANUNCIO",
                    color = cyan,
                    modifier = Modifier.weight(1f),
                    onClick = onWatchTraceAtStart
                )
                TerminalButton(
                    text = "[B] OMITIR",
                    color = green.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    onClick = onSkipTraceAtStart
                )
            }
        }
    }
}

// ─── Pantallas separadas: DifficultyChoice y GameOver ────────────────────────

@Composable
private fun DifficultyChoiceContent(
    uiState: GameUiState,
    onSelectDifficulty: (Difficulty) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        MatrixRain(density = 0.15f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlitchText(
                text = "UPGRADING PAYLOAD",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                glitchIntensity = 0.6f
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Level ${uiState.level} complete // Choose payload:",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            DifficultyOption(
                code = "01",
                label = "MODO DIFÍCIL",
                description = "3 dígitos hex · 3.360 combos · Puntos ×3",
                color = Color(0xFFFF6600),
                onClick = { onSelectDifficulty(Difficulty.HARD) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultyOption(
                code = "02",
                label = "MUY DIFÍCIL",
                description = "4 dígitos decimales · 5.040 combos · Puntos ×2",
                color = Color(0xFFFF3300),
                onClick = { onSelectDifficulty(Difficulty.VERY_HARD) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultyOption(
                code = "03",
                label = "IRONMAN",
                description = "4 dígitos hex · 43.680 combos · 25 brechas · Puntos ×20",
                color = Color(0xFFFF0000),
                onClick = { onSelectDifficulty(Difficulty.IRONMAN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultyOption(
                code = "04",
                label = "MANTENER NIVEL",
                description = "${uiState.difficulty.displayName} · ${uiState.difficulty.digitCount} dígitos · ×${uiState.difficulty.pointMultiplier}",
                color = MaterialTheme.colorScheme.primary,
                onClick = { onSelectDifficulty(uiState.difficulty) }
            )
        }
    }
}

@Composable
private fun DifficultyOption(
    code: String,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f))
            .background(color.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[$code]",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = description,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = color.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ─── Popup Game Over (otra consola sobre la principal) ────────────────────────

@Composable
private fun GameOverPopup(
    uiState: GameUiState,
    onBackToMenu: () -> Unit
) {
    val red = Color(0xFFFF0000)
    val orange = Color(0xFFFF6600)
    val green = Color(0xFF00FF41)

    val (reasonTitle, reasonDetail) = when (uiState.gameOverReason) {
        GameOverReason.TimeUp -> "TRACE OUTPACED PAYLOAD" to "Security was faster — source IP identified and logged"
        GameOverReason.NoAttemptsLeft -> "REPEATED ATTEMPTS FLAGGED" to "Suspicious breach pattern — IP trace completed"
        null -> "CONNECTION TERMINATED" to "Unknown cause"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { /* bloquea interacción con consola detrás */ },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF0a0f0a))
                .border(2.dp, Color(0xFFFF0000).copy(alpha = 0.6f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "> security_alert // breach_log",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = red.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "─────────────────────────────",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = red.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "ACCESS DENIED — IP COMPROMISED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = red,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "> $reasonTitle",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = orange,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "> $reasonDetail",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = orange.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "─── SESSION REPORT ───",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = green.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "> FINAL SCORE: ${uiState.totalScore}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = green.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "> LEVEL REACHED: ${uiState.level}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = green.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "> DIFFICULTY: ${uiState.difficulty.displayName.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = green.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "> ATTEMPTS USED: ${uiState.guesses.size}/${uiState.maxAttempts}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = green.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (uiState.secretCode != null) {
                    Text(
                        text = "─── TARGET CODE ───",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = orange.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "> ${uiState.secretCode.joinToString(" ")}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = orange,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                TerminalButton(
                    text = "[ DESCONECTAR ]",
                    color = red,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBackToMenu
                )
            }
        }
    }
}

@Composable
private fun InlineGameOver(
    uiState: GameUiState,
    onBackToMenu: () -> Unit
) {
    val red = Color(0xFFFF0000)
    val orange = Color(0xFFFF6600)
    val green = Color(0xFF00FF41)

    val errorLine = when (uiState.gameOverReason) {
        GameOverReason.TimeUp -> "> ERROR: SESSION TIMEOUT"
        GameOverReason.NoAttemptsLeft -> "> ERROR: MAX BREACH ATTEMPTS"
        null -> "> ERROR: UNKNOWN"
    }

    Column {
        Text(
            text = "─────────────────────────────",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = red.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "CONNECTION TERMINATED",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = red,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = errorLine,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = orange,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "─── SESSION REPORT ───",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = green.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "> FINAL SCORE: ${uiState.totalScore}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = green.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "> LEVEL REACHED: ${uiState.level}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = green.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "> DIFFICULTY: ${uiState.difficulty.displayName.uppercase()}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = green.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "> ATTEMPTS USED: ${uiState.guesses.size}/${uiState.maxAttempts}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = green.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (uiState.secretCode != null) {
            Text(
                text = "─── TARGET CODE ───",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = orange.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Text(
                text = "> ${uiState.secretCode.joinToString(" ")}",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = orange,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        TerminalButton(
            text = "[ DESCONECTAR ]",
            color = red,
            modifier = Modifier.fillMaxWidth(),
            onClick = onBackToMenu
        )
    }
}

@Composable
private fun GameOverContent(
    uiState: GameUiState,
    onBackToMenu: () -> Unit
) {
    val showStats = remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "gameOver")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlitchText(
                text = "CONNECTION",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 4.sp
                ),
                color = Color(0xFFFF0000).copy(alpha = flicker),
                glitchIntensity = 1.5f
            )
            GlitchText(
                text = "TERMINATED",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = 4.sp
                ),
                color = Color(0xFFFF0000).copy(alpha = flicker),
                glitchIntensity = 1.5f
            )

            Spacer(modifier = Modifier.height(16.dp))

            TerminalText(
                fullText = when (uiState.gameOverReason) {
                    GameOverReason.TimeUp -> "> ERROR: SESSION TIMEOUT"
                    GameOverReason.NoAttemptsLeft -> "> ERROR: MAX BREACH ATTEMPTS"
                    null -> "> ERROR: UNKNOWN"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF6600),
                charDelayMs = 35L,
                showCursor = false,
                onComplete = { showStats.value = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showStats.value,
                enter = fadeIn(tween(800))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "─── SESSION REPORT ───",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "FINAL SCORE: ${uiState.totalScore}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "LEVEL REACHED: ${uiState.level}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "DIFFICULTY: ${uiState.difficulty.displayName.uppercase()}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "ATTEMPTS USED: ${uiState.guesses.size}/${uiState.maxAttempts}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    if (uiState.secretCode != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "─── TARGET CODE ───",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFFF6600).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.secretCode.joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = Color(0xFFFF6600)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.5f))
                            .background(Color(0xFFFF0000).copy(alpha = 0.03f))
                            .clickable { onBackToMenu() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[ DISCONNECT ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFF0000)
                        )
                    }
                }
            }
        }
    }
}
