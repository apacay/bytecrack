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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import kotlin.random.Random
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
    // Notificar al ViewModel cuando la Activity vuelve al primer plano (tras un anuncio)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onActivityResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Animación de bonus al completar nivel ──────────────────────────────────
    var bonusTimerTarget by remember { mutableStateOf<Long?>(null) }
    var bonusAnimDoneCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var scoreFlashActive by remember { mutableStateOf(false) }

    val animatedTimerValue by animateFloatAsState(
        targetValue = (bonusTimerTarget ?: uiState.timeRemainingSeconds).toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bonusTimer",
        finishedListener = {
            bonusAnimDoneCallback?.invoke()
            bonusAnimDoneCallback = null
        }
    )
    val animatedScoreColor by animateColorAsState(
        targetValue = if (scoreFlashActive) Color(0xFF00FFFF) else Color(0xFF00FF41).copy(alpha = 0.9f),
        animationSpec = tween(durationMillis = 450),
        label = "scoreColor"
    )
    // Resetear al cambiar isLevelComplete
    LaunchedEffect(uiState.isLevelComplete) {
        bonusTimerTarget = null
        scoreFlashActive = false
    }
    // Apagar el flash de score después de 1.6s (mientras la barra sube)
    LaunchedEffect(scoreFlashActive) {
        if (scoreFlashActive) {
            delay(1600)
            scoreFlashActive = false
        }
    }
    val onBonusPhaseStart: (Long, () -> Unit) -> Unit = { targetTime, onDone ->
        bonusTimerTarget = targetTime
        bonusAnimDoneCallback = onDone
        scoreFlashActive = true
    }
    val displayTimerValue: Long = if (uiState.isLevelComplete && bonusTimerTarget != null) {
        animatedTimerValue.toLong()
    } else {
        uiState.timeRemainingSeconds
    }
    // ──────────────────────────────────────────────────────────────────────────

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
        isGameOver ||
        showHintPopup || showCrackPopup
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
                    timeRemaining = displayTimerValue,
                    modifier = Modifier.weight(0.75f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SCORE:${uiState.totalScore}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedScoreColor
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
                    onBonusPhaseStart = onBonusPhaseStart,
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
                onBonusPhaseStart = onBonusPhaseStart,
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

        // Popup de HINT — overlay pequeño con animación de ejecución
        if (uiState.showHintPopup && uiState.lastHintDigit != null) {
            TraceHintPopup(
                digit = uiState.lastHintDigit,
                onDismiss = { viewModel.dismissHintPopup() }
            )
        }

        // Popup de CRACK — overlay pequeño con animación de ejecución
        if (uiState.showCrackPopup &&
            uiState.lastCrackPosition != null &&
            uiState.lastCrackDigit != null
        ) {
            TraceCrackPopup(
                position = uiState.lastCrackPosition,
                digit = uiState.lastCrackDigit,
                onDismiss = { viewModel.dismissCrackPopup() }
            )
        }
    }
}

private sealed class LogEntry {
    data class Line(val text: String, val color: Color) : LogEntry()
    data class Guess(val guess: com.bytecrack.domain.model.Guess, val attemptNumber: Int) : LogEntry()
}

// ─── Consola principal de juego ───────────────────────────────────────────────

@Composable
private fun GameConsoleBox(
    uiState: GameUiState,
    isInTransition: Boolean,
    viewModel: GameViewModel,
    activity: Activity?,
    digitCount: Int,
    onBackToMenu: () -> Unit,
    onBonusPhaseStart: ((Long, () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val completedLog = remember { mutableStateListOf<LogEntry>() }
    var prevUiState by remember { mutableStateOf(uiState) }

    SideEffect {
        val prev = prevUiState
        val curr = uiState

        val green = Color(0xFF00FF41)
        val red = Color(0xFFFF0000)
        val cyan = Color(0xFF00FFFF)
        val orange = Color(0xFFFF6600)

        if (curr.level != prev.level && curr.guesses.isEmpty()) {
            if (curr.level > prev.level) {
                // Avanzando al siguiente nivel en la misma partida → separador visual, no borrar
                completedLog.add(LogEntry.Line("", green.copy(alpha = 0f)))
                completedLog.add(LogEntry.Line("─────────────────────────────", green.copy(alpha = 0.18f)))
                completedLog.add(LogEntry.Line("> LAYER ${curr.level} — TARGETING NEXT NODE", green.copy(alpha = 0.45f)))
                completedLog.add(LogEntry.Line("", green.copy(alpha = 0f)))
            } else {
                // Nueva partida (nivel volvió a 1) → limpiar log
                completedLog.clear()
            }
        }

        if (prev.showLevelIntro && !curr.showLevelIntro) {
            val node = curr.level * 7 + 81
            listOf(
                "$ ssh -p 443 target.node.$node" to green.copy(alpha = 0.9f),
                "[OK] Connected to 10.${curr.level}.88.1" to green.copy(alpha = 0.6f),
                "$ ./bytecrack --inject --stealth" to green.copy(alpha = 0.9f),
                "[OK] Payload compiled" to green.copy(alpha = 0.6f),
                "[OK] Exploit module loaded" to green.copy(alpha = 0.6f),
                "$ breach --init" to green.copy(alpha = 0.9f),
                "[OK] Secure channel established" to green.copy(alpha = 0.6f),
                "[OK] Access granted — ready" to green.copy(alpha = 0.6f)
            ).forEach { (text, color) ->             completedLog.add(LogEntry.Line(text, color)) }
        }
        if (prev.showVictoryPenetration && !curr.showVictoryPenetration) {
            listOf(
                "$ ./validate --checksum payload" to green.copy(alpha = 0.9f),
                "[OK] Integrity verified" to green.copy(alpha = 0.6f),
                "$ breach --escalate" to green.copy(alpha = 0.9f),
                "[OK] Firewall bypassed" to green.copy(alpha = 0.6f),
                "[OK] Buffer decrypted" to green.copy(alpha = 0.6f),
                "$ write session.token" to green.copy(alpha = 0.9f),
                "[OK] PENETRATION SUCCESSFUL" to green
            ).forEach { (text, color) ->             completedLog.add(LogEntry.Line(text, color)) }
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
            lines.forEach { completedLog.add(LogEntry.Line("> $it", green.copy(alpha = 0.8f))) }
        }
        if (prev.showTransitionBackToGame && !curr.showTransitionBackToGame) {
            listOf(
                "$ confirm --identity" to cyan.copy(alpha = 0.9f),
                "[OK] Node verified" to cyan.copy(alpha = 0.6f),
                "[OK] Breaches restored" to cyan.copy(alpha = 0.6f),
                "[OK] Timer resumed" to cyan.copy(alpha = 0.6f)
            ).forEach { (text, color) -> completedLog.add(LogEntry.Line(text, color)) }
        }
        if (prev.showTransitionToGiveUp && !curr.showTransitionToGiveUp) {
            listOf(
                "$ abort --flush" to orange.copy(alpha = 0.9f),
                "[OK] Buffers flushed" to orange.copy(alpha = 0.6f),
                "[OK] Trace incomplete — safe exit" to orange.copy(alpha = 0.6f)
            ).forEach { (text, color) -> completedLog.add(LogEntry.Line(text, color)) }
        }
        if (prev.showFailureTransition && !curr.showFailureTransition) {
            val lines = when (prev.pendingGameOverReason) {
                GameOverReason.TimeUp -> listOf(
                    "[WARN] Trace approaching source" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[WARN] Countermeasures too slow" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[ERR] Source IP identified" to red,
                    "[ERR] Connection killed by remote" to red,
                    "─────────────────────────────" to red.copy(alpha = 0.4f),
                    "[ERR] ACCESS DENIED — IP COMPROMISED" to red
                )
                else -> listOf(
                    "[WARN] Anomalous breach pattern logged" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[WARN] Security correlating attempts" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[ERR] IP trace completed" to red,
                    "[ERR] Identity linked" to red,
                    "─────────────────────────────" to red.copy(alpha = 0.4f),
                    "[ERR] ACCESS DENIED — IP COMPROMISED" to red
                )
            }
            lines.forEach { (text, color) -> completedLog.add(LogEntry.Line(text, color)) }
        }
        if (prev.showDiscoveredTransition && !curr.showDiscoveredTransition) {
            val lines = if (prev.pendingRewardAdFromTimeOut) {
                listOf(
                    "[WARN] Trace closing in" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[ERR] Source flagged" to red
                )
            } else {
                listOf(
                    "[WARN] Repeated attempts flagged" to Color(0xFFFF6600).copy(alpha = 0.85f),
                    "[ERR] Trace in progress" to red
                )
            }
            lines.forEach { (text, color) -> completedLog.add(LogEntry.Line(text, color)) }
        }
        if (prev.showEscapeTransition && !curr.showEscapeTransition) {
            listOf(
                "$ burn-node --activate" to cyan.copy(alpha = 0.9f),
                "[OK] Identity purged" to cyan.copy(alpha = 0.6f),
                "[OK] New fingerprint generated" to cyan.copy(alpha = 0.6f),
                "$ breach --resume" to cyan.copy(alpha = 0.9f),
                "[OK] Attempts restored" to cyan.copy(alpha = 0.6f),
                "[OK] Node is live" to cyan.copy(alpha = 0.6f)
            ).forEach { (text, color) -> completedLog.add(LogEntry.Line(text, color)) }
        }

        // Nuevo guess → agregar al log en orden cronológico
        if (curr.guesses.size > prev.guesses.size) {
            val newGuess = curr.guesses.last()
            completedLog.add(LogEntry.Guess(newGuess, curr.guesses.size))
        }

        // Hint popup cerrado → línea de resultado en consola
        if (prev.showHintPopup && !curr.showHintPopup && curr.lastHintDigit != null) {
            completedLog.add(LogEntry.Line(
                "[OK] TRACE: digit ${curr.lastHintDigit} found in sequence",
                cyan
            ))
        }

        // Crack popup cerrado → línea de resultado en consola
        if (prev.showCrackPopup && !curr.showCrackPopup &&
            curr.lastCrackPosition != null && curr.lastCrackDigit != null
        ) {
            completedLog.add(LogEntry.Line(
                "[OK] TRACE: position ${curr.lastCrackPosition} locked — ${curr.lastCrackDigit}",
                cyan
            ))
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
        // Esperar a que el layout mida el contenido nuevo (especialmente tras transición de nivel)
        delay(200)
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
                // 1. Historial cronológico — siempre visible (incluso durante el intro de nivel)
                completedLog.forEach { entry ->
                    when (entry) {
                        is LogEntry.Line -> Text(
                            text = entry.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = entry.color,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        is LogEntry.Guess -> GuessHistoryItem(
                            guess = entry.guess,
                            attemptNumber = entry.attemptNumber
                        )
                    }
                }
                if (uiState.guesses.isEmpty() && !isInTransition) {
                    Text(
                        text = "> Awaiting input...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF00FF41).copy(alpha = 0.4f)
                    )
                }

                // 2. Transición de entrada (SSH intro) o salida + scoring (bottom)
                when {
                uiState.showLevelIntro -> {
                    InlineTypedLines(
                        lines = listOf(
                            TransitionLine("ssh -p 443 target.node.${uiState.level * 7 + 81}"),
                            TransitionLine("[OK] Connected to 10.${uiState.level}.88.1", isSystemResponse = true),
                            TransitionLine("./bytecrack --inject --stealth"),
                            TransitionLine("[OK] Payload compiled", isSystemResponse = true),
                            TransitionLine("[OK] Exploit module loaded", isSystemResponse = true),
                            TransitionLine("breach --init"),
                            TransitionLine("[OK] Secure channel established", isSystemResponse = true),
                            TransitionLine("[OK] Access granted — ready", isSystemResponse = true)
                        ),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onComplete = {}
                    )
                }
                else -> {
                // 3. Transición de salida + scoring (bottom)
                when {
                uiState.isLevelComplete && uiState.showVictoryPenetration -> {
                    InlineTypedLines(
                        lines = listOf(
                            TransitionLine("./validate --checksum payload"),
                            TransitionLine("[OK] Integrity verified", isSystemResponse = true),
                            TransitionLine("breach --escalate"),
                            TransitionLine("[OK] Firewall bypassed", isSystemResponse = true),
                            TransitionLine("[OK] Buffer decrypted", isSystemResponse = true),
                            TransitionLine("write session.token"),
                            TransitionLine("[OK] PENETRATION SUCCESSFUL", isSystemResponse = true)
                        ),
                        accentColor = Color(0xFF00FF41),
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
                        onButtonsReady = { showTransitionButtons = true },
                        onBonusPhaseStart = onBonusPhaseStart
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
                            TransitionLine("confirm --identity"),
                            TransitionLine("[OK] Node verified", isSystemResponse = true),
                            TransitionLine("[OK] Breaches restored", isSystemResponse = true),
                            TransitionLine("[OK] Timer resumed", isSystemResponse = true)
                        ),
                        accentColor = Color(0xFF00FFFF),
                        onComplete = { viewModel.dismissTransitionBackToGame() }
                    )
                }

                uiState.showTransitionToGiveUp -> {
                    InlineTypedLines(
                        lines = listOf(
                            TransitionLine("abort --flush"),
                            TransitionLine("[OK] Buffers flushed", isSystemResponse = true),
                            TransitionLine("[OK] Trace incomplete — safe exit", isSystemResponse = true)
                        ),
                        accentColor = Color(0xFFFF6600),
                        onComplete = { viewModel.dismissTransitionToGiveUp() }
                    )
                }

                uiState.showFailureTransition -> {
                    val failureLines = when (uiState.pendingGameOverReason) {
                        GameOverReason.TimeUp -> listOf(
                            TransitionLine("[WARN] Trace approaching source", isSystemResponse = true),
                            TransitionLine("[WARN] Countermeasures too slow", isSystemResponse = true),
                            TransitionLine("[ERR] Source IP identified", isSystemResponse = true),
                            TransitionLine("[ERR] Connection killed by remote", isSystemResponse = true),
                            TransitionLine("─────────────────────────────", isSystemResponse = true),
                            TransitionLine("[ERR] ACCESS DENIED — IP COMPROMISED", isSystemResponse = true)
                        )
                        else -> listOf(
                            TransitionLine("[WARN] Anomalous breach pattern logged", isSystemResponse = true),
                            TransitionLine("[WARN] Security correlating attempts", isSystemResponse = true),
                            TransitionLine("[ERR] IP trace completed", isSystemResponse = true),
                            TransitionLine("[ERR] Identity linked", isSystemResponse = true),
                            TransitionLine("─────────────────────────────", isSystemResponse = true),
                            TransitionLine("[ERR] ACCESS DENIED — IP COMPROMISED", isSystemResponse = true)
                        )
                    }
                    InlineTypedLines(
                        lines = failureLines,
                        accentColor = Color(0xFFFF0000),
                        onComplete = { viewModel.completeGameOver() }
                    )
                }

                uiState.showDiscoveredTransition -> {
                    val discoveredLines = when (uiState.pendingRewardAdFromTimeOut) {
                        true -> listOf(
                            TransitionLine("[WARN] Trace closing in", isSystemResponse = true),
                            TransitionLine("[ERR] Source flagged", isSystemResponse = true)
                        )
                        else -> listOf(
                            TransitionLine("[WARN] Repeated attempts flagged", isSystemResponse = true),
                            TransitionLine("[ERR] Trace in progress", isSystemResponse = true)
                        )
                    }
                    InlineTypedLines(
                        lines = discoveredLines,
                        accentColor = Color(0xFFFF0000),
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
                    val hexSuffix = remember { (0..3).map { "0123456789ABCDEF".random() }.joinToString("") }
                    InlineTypedLines(
                        lines = listOf(
                            TransitionLine("burn-node --activate"),
                            TransitionLine("[OK] Identity purged", isSystemResponse = true),
                            TransitionLine("[OK] New fingerprint: 0x${hexSuffix}..", isSystemResponse = true),
                            TransitionLine("breach --resume"),
                            TransitionLine("[OK] Attempts restored", isSystemResponse = true),
                            TransitionLine("[OK] Node is live", isSystemResponse = true)
                        ),
                        accentColor = Color(0xFF00FFFF),
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

// ─── Modelo de líneas de transición ──────────────────────────────────────────

/**
 * Una línea de transición.
 * [isSystemResponse] = true → aparece de golpe tras una pausa corta (output del servidor).
 * [isSystemResponse] = false → se tipea char a char con velocidad variable (comando del hacker).
 */
private data class TransitionLine(
    val text: String,
    val isSystemResponse: Boolean = false
)

/** Delay variable que simula tipeo humano: velocidad base + ruido aleatorio + micro-pausas en espacios. */
private suspend fun humanTypeDelay(isSpace: Boolean, baseMs: Long = 32L) {
    delay((baseMs + Random.nextLong(-10, 18)).coerceAtLeast(15L))
    if (isSpace && Random.nextFloat() < 0.35f) delay(Random.nextLong(30, 65))
}

// ─── Componente genérico de texto con animación de tipeo ─────────────────────

@Composable
private fun InlineTypedLines(
    lines: List<TransitionLine>,
    accentColor: Color = Color(0xFF00FF41),
    onComplete: () -> Unit = {}
) {
    // revealedLines[i] = número de chars mostrados para la línea i (-1 = aún no iniciada, Int.MAX = completa)
    val revealedChars = remember { mutableStateListOf<Int>().also { list -> lines.forEach { _ -> list.add(-1) } } }

    LaunchedEffect(Unit) {
        for (lineIdx in lines.indices) {
            val tl = lines[lineIdx]
            val prefix = if (tl.isSystemResponse) tl.text else "$ ${tl.text}"
            if (tl.isSystemResponse) {
                delay(Random.nextLong(120, 220))
                revealedChars[lineIdx] = prefix.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in prefix.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = prefix[i] == ' ')
                }
                delay(Random.nextLong(80, 180))
            }
        }
        delay(300)
        onComplete()
    }

    Column {
        lines.forEachIndexed { index, tl ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val prefix = if (tl.isSystemResponse) tl.text else "$ ${tl.text}"
            val isTyping = shown < prefix.length
            val displayText = prefix.substring(0, shown.coerceAtMost(prefix.length)) +
                if (isTyping) "█" else ""

            val color = if (tl.isSystemResponse) {
                when {
                    tl.text.startsWith("[ERR]") -> Color(0xFFFF0000).copy(alpha = 0.85f)
                    tl.text.startsWith("[WARN]") -> Color(0xFFFF6600).copy(alpha = 0.85f)
                    tl.text.startsWith("[OK]") -> accentColor.copy(alpha = 0.65f)
                    tl.text.startsWith("─") -> accentColor.copy(alpha = 0.25f)
                    else -> accentColor.copy(alpha = 0.75f)
                }
            } else {
                accentColor
            }

            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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
    onButtonsReady: () -> Unit = {},
    onBonusPhaseStart: ((Long, () -> Unit) -> Unit)? = null
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

    val revealedChars = remember { mutableStateListOf<Int>().also { list -> infoLines.forEach { _ -> list.add(-1) } } }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (lineIdx in infoLines.indices) {
            val line = infoLines[lineIdx]
            val isSystem = line.startsWith("─") || line.startsWith("ACCESS") ||
                line.startsWith("TIER") || line.startsWith("TIME") ||
                line.startsWith("POINTS") || line.startsWith("CONTINUATION") ||
                line.startsWith("TRACE AVAILABLE") || line.startsWith("Watch") ||
                line.startsWith("(use")
            if (isSystem) {
                delay(Random.nextLong(100, 200))
                revealedChars[lineIdx] = line.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in line.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = line[i] == ' ')
                }
                delay(Random.nextLong(80, 160))
            }
        }
        delay(300)
        // ── Fase de animación de bonus (timer sube + score parpadea cyan) ─────
        if (onBonusPhaseStart != null && !uiState.wonViaReward && effectiveBonus > 0) {
            val targetTime = uiState.timeRemainingSeconds + effectiveBonus
            val deferred = CompletableDeferred<Unit>()
            onBonusPhaseStart(targetTime) { deferred.complete(Unit) }
            // Esperar a que la animación del header termine (máx 2.5s como seguridad)
            withTimeoutOrNull(2500L) { deferred.await() }
            delay(300)
        }
        // ─────────────────────────────────────────────────────────────────────
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val isTyping = shown < line.length
            val displayText = "> " + line.substring(0, shown.coerceAtMost(line.length)) +
                if (isTyping) "█" else ""

            val color = when {
                line == "ACCESS GRANTED" -> green
                line.startsWith("TIER:") -> cyan
                line.startsWith("─") -> green.copy(alpha = 0.3f)
                line.startsWith("TRACE AVAILABLE") -> cyan
                else -> green.copy(alpha = 0.8f)
            }
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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

    // El contexto anterior ya quedó en la consola desde la discovered transition
    // Solo mostramos el prompt de override
    val promptLines = if (fromTimeOut) listOf(
        TransitionLine("[WARN] Trace closing in", isSystemResponse = true),
        TransitionLine("[ERR] Source flagged", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("[ERR] BREACH FAILED — IP compromised", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("Emergency override available"),
        TransitionLine("Burn a borrowed node — single use")
    ) else listOf(
        TransitionLine("[WARN] Repeated attempts flagged", isSystemResponse = true),
        TransitionLine("[ERR] Trace in progress", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("[ERR] BREACH FAILED — attempts exhausted", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("Emergency override available"),
        TransitionLine("Burn a borrowed node — single use")
    )

    val revealedChars = remember { mutableStateListOf<Int>().also { list -> promptLines.forEach { _ -> list.add(-1) } } }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        for (lineIdx in promptLines.indices) {
            val tl = promptLines[lineIdx]
            if (tl.isSystemResponse) {
                delay(Random.nextLong(100, 210))
                revealedChars[lineIdx] = tl.text.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in tl.text.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = tl.text[i] == ' ')
                }
                delay(Random.nextLong(80, 160))
            }
        }
        delay(300)
        showButtons = true
        onButtonsReady()
    }

    Column {
        promptLines.forEachIndexed { index, tl ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val isTyping = shown < tl.text.length
            val prefix = if (tl.isSystemResponse) "" else "$ "
            val displayText = prefix + tl.text.substring(0, shown.coerceAtMost(tl.text.length)) +
                if (isTyping) "█" else ""
            val color = when {
                tl.text.startsWith("[ERR]") -> red
                tl.text.startsWith("[WARN]") -> orange.copy(alpha = 0.85f)
                tl.text.startsWith("─") -> orange.copy(alpha = 0.35f)
                tl.text.startsWith("Emergency") -> orange
                tl.isSystemResponse -> orange.copy(alpha = 0.75f)
                else -> orange
            }
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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

    val revealedChars = remember { mutableStateListOf<Int>().also { list -> infoLines.forEach { _ -> list.add(-1) } } }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (lineIdx in infoLines.indices) {
            val line = infoLines[lineIdx]
            val isSystem = line.startsWith("─") || line.startsWith("Game Over")
            if (isSystem) {
                delay(Random.nextLong(130, 250))
                revealedChars[lineIdx] = line.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in line.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = line[i] == ' ')
                }
                delay(Random.nextLong(80, 160))
            }
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        infoLines.forEachIndexed { index, line ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val isTyping = shown < line.length
            val displayText = "> " + line.substring(0, shown.coerceAtMost(line.length)) +
                if (isTyping) "█" else ""
            val color = when {
                line == "Game Over ...?" -> red
                line.startsWith("─") -> cyan.copy(alpha = 0.3f)
                else -> cyan.copy(alpha = 0.8f)
            }
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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

    val traceLines = listOf(
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("[OK] TRACE module available", isSystemResponse = true),
        TransitionLine("trace --acquire --source ad"),
        TransitionLine("[OK] +1 TRACE unlocked", isSystemResponse = true),
        TransitionLine("[OK] Use: HINT (reveal digit) or CRACK (lock slot)", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true)
    )

    val revealedChars = remember { mutableStateListOf<Int>().also { list -> traceLines.forEach { _ -> list.add(-1) } } }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (lineIdx in traceLines.indices) {
            val tl = traceLines[lineIdx]
            if (tl.isSystemResponse) {
                delay(Random.nextLong(90, 190))
                revealedChars[lineIdx] = tl.text.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in tl.text.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = tl.text[i] == ' ')
                }
                delay(Random.nextLong(80, 150))
            }
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        traceLines.forEachIndexed { index, tl ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val isTyping = shown < tl.text.length
            val prefix = if (tl.isSystemResponse) "" else "$ "
            val displayText = prefix + tl.text.substring(0, shown.coerceAtMost(tl.text.length)) +
                if (isTyping) "█" else ""
            val color = when {
                tl.text.startsWith("[OK]") -> cyan.copy(alpha = 0.7f)
                tl.text.startsWith("─") -> cyan.copy(alpha = 0.25f)
                else -> cyan
            }
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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

    val traceLines = listOf(
        TransitionLine("─────────────────────────────", isSystemResponse = true),
        TransitionLine("[OK] TRACE module available", isSystemResponse = true),
        TransitionLine("trace --acquire --source ad"),
        TransitionLine("[OK] +1 TRACE unlocked", isSystemResponse = true),
        TransitionLine("[OK] Use: HINT (reveal digit) or CRACK (lock slot)", isSystemResponse = true),
        TransitionLine("─────────────────────────────", isSystemResponse = true)
    )

    val revealedChars = remember { mutableStateListOf<Int>().also { list -> traceLines.forEach { _ -> list.add(-1) } } }
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        for (lineIdx in traceLines.indices) {
            val tl = traceLines[lineIdx]
            if (tl.isSystemResponse) {
                delay(Random.nextLong(90, 190))
                revealedChars[lineIdx] = tl.text.length
            } else {
                revealedChars[lineIdx] = 0
                for (i in tl.text.indices) {
                    revealedChars[lineIdx] = i + 1
                    humanTypeDelay(isSpace = tl.text[i] == ' ')
                }
                delay(Random.nextLong(80, 150))
            }
        }
        delay(250)
        showButtons = true
        onButtonsReady()
    }

    Column {
        traceLines.forEachIndexed { index, tl ->
            val shown = revealedChars.getOrNull(index) ?: -1
            if (shown < 0) return@forEachIndexed
            val isTyping = shown < tl.text.length
            val prefix = if (tl.isSystemResponse) "" else "$ "
            val displayText = prefix + tl.text.substring(0, shown.coerceAtMost(tl.text.length)) +
                if (isTyping) "█" else ""
            val color = when {
                tl.text.startsWith("[OK]") -> cyan.copy(alpha = 0.7f)
                tl.text.startsWith("─") -> cyan.copy(alpha = 0.25f)
                else -> cyan
            }
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.padding(bottom = 4.dp)
            )
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
    val mutedGray = Color(0xFF00FF41).copy(alpha = 0.45f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // Explicaciones en castellano fuera de la consola
        Spacer(modifier = Modifier.height(8.dp))
        when {
            uiState.offerRewardedAd -> {
                Text(
                    text = "[A] +${uiState.difficulty.rewardAdBreachExtension} intentos extra, +${uiState.difficulty.rewardAdSeconds}s al timer",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray
                )
                Text(
                    text = "[B] Termina la partida",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            uiState.isLevelComplete && uiState.showTraceAdOffer -> {
                Text(
                    text = "[A] Ganas +1 TRACE (usar para HINT o bloquear un slot)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray
                )
                Text(
                    text = "[B] Continuar sin TRACE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            uiState.offerTraceAd -> {
                Text(
                    text = "[A] Ganas +1 TRACE (usar para HINT o bloquear un slot)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray
                )
                Text(
                    text = "[B] Continuar sin TRACE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            uiState.showTraceAdOfferAtStart -> {
                Text(
                    text = "[A] Ganas +1 TRACE (usar para HINT o bloquear un slot)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray
                )
                Text(
                    text = "[B] Empezar sin TRACE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = mutedGray,
                    modifier = Modifier.padding(top = 2.dp)
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

// ─── Popup TRACE HINT ────────────────────────────────────────────────────────

@Composable
private fun TraceHintPopup(
    digit: Char,
    onDismiss: () -> Unit
) {
    val cyan = Color(0xFF00BFFF)
    var animationDone by remember { mutableStateOf(false) }

    LaunchedEffect(animationDone) {
        if (animationDone) {
            delay(500)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF0a0f0a))
                .border(1.dp, cyan.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "> trace_module // hint_execution",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = cyan.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                InlineTypedLines(
                    lines = listOf(
                        TransitionLine("trace --scan --memory"),
                        TransitionLine("[OK] Memory segment loaded", isSystemResponse = true),
                        TransitionLine("[OK] Scanning known sequence patterns", isSystemResponse = true),
                        TransitionLine("[OK] TRACE: digit $digit found in sequence", isSystemResponse = true)
                    ),
                    accentColor = cyan,
                    onComplete = { animationDone = true }
                )
            }
        }
    }
}

// ─── Popup TRACE CRACK ────────────────────────────────────────────────────────

@Composable
private fun TraceCrackPopup(
    position: Int,
    digit: Char,
    onDismiss: () -> Unit
) {
    val cyan = Color(0xFF00BFFF)
    var animationDone by remember { mutableStateOf(false) }
    val hexSuffix = remember { (0..5).map { "0123456789ABCDEF".random() }.joinToString("") }

    LaunchedEffect(animationDone) {
        if (animationDone) {
            delay(500)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(Color(0xFF0a0f0a))
                .border(1.dp, cyan.copy(alpha = 0.7f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "> trace_module // crack_execution",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = cyan.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                InlineTypedLines(
                    lines = listOf(
                        TransitionLine("trace --inject --position $position"),
                        TransitionLine("[OK] Buffer overflow at 0x$hexSuffix", isSystemResponse = true),
                        TransitionLine("[OK] Segment ${position + 1} compromised", isSystemResponse = true),
                        TransitionLine("[OK] TRACE: position $position locked → $digit", isSystemResponse = true)
                    ),
                    accentColor = cyan,
                    onComplete = { animationDone = true }
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
