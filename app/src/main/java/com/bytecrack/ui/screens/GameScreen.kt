package com.bytecrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (uiState.screen) {
            GameScreenState.GameOver -> GameOverContent(
                uiState = uiState,
                onBackToMenu = onBackToMenu
            )
            GameScreenState.DifficultyChoice -> DifficultyChoiceContent(
                uiState = uiState,
                onSelectDifficulty = viewModel::selectDifficulty
            )
            GameScreenState.Game -> {
                if (uiState.isLevelComplete) {
                    LevelCompleteContent(
                        uiState = uiState,
                        onContinue = viewModel::continueToNextLevel
                    )
                } else {
                    GamePlayContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBackToMenu = onBackToMenu
                    )
                }
            }
            GameScreenState.MainMenu -> {}
        }

        ScanlineOverlay()
    }
}

@Composable
private fun GamePlayContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit
) {
    val secretCode = uiState.secretCode ?: return
    val digitCount = secretCode.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelProgressBadge(
                    level = uiState.level,
                    difficulty = uiState.difficulty
                )
                Spacer(modifier = Modifier.width(8.dp))
                DifficultyBadge(difficulty = uiState.difficulty)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SCORE: ${uiState.totalScore}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "BREACH ${uiState.attemptsRemaining}/10",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = when {
                        uiState.attemptsRemaining <= 2 -> Color(0xFFFF0000)
                        uiState.attemptsRemaining <= 5 -> Color(0xFFFF6600)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .clickable { onBackToMenu() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ESC",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TimerBar(timeRemaining = uiState.timeRemainingSeconds)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.guesses.isEmpty()) {
                Text(
                    text = "> Awaiting input...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
            uiState.guesses.reversed().forEachIndexed { index, guess ->
                val attemptNum = uiState.guesses.size - index
                GuessHistoryItem(
                    guess = guess,
                    attemptNumber = attemptNum
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.height(8.dp))

        CodeInputDisplay(
            currentInput = uiState.currentInput,
            digitCount = digitCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        HackerKeyboard(
            isHex = uiState.difficulty.radix == 16,
            usedDigits = uiState.currentInput.toSet(),
            onDigit = viewModel::addDigit,
            onDelete = viewModel::removeDigit,
            onSubmit = viewModel::submitCurrentInput,
            submitEnabled = uiState.currentInput.size == digitCount
        )
    }
}

@Composable
private fun LevelCompleteContent(
    uiState: GameUiState,
    onContinue: () -> Unit
) {
    val tier = uiState.lastTier ?: return
    val showDetails = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        MatrixRain(density = 0.2f, color = Color(0xFF00FF41))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlitchText(
                text = "ACCESS GRANTED",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                glitchIntensity = 0.5f
            )

            Spacer(modifier = Modifier.height(24.dp))

            TerminalText(
                fullText = "> TIER: ${tier.name} - ${tier.displayName}",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.secondary,
                charDelayMs = 30L,
                showCursor = false,
                onComplete = { showDetails.value = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = showDetails.value,
                enter = fadeIn(tween(500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIME BONUS: +${tier.bonusSeconds}s",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "POINTS: +${uiState.lastLevelPoints} (x${uiState.difficulty.pointMultiplier})",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .clickable { onContinue() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[ CONTINUE ]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

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
                label = "HARD MODE",
                description = "4 decimal digits · 5,040 combos · Points ×2",
                color = Color(0xFFFF6600),
                onClick = { onSelectDifficulty(Difficulty.HARD) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultyOption(
                code = "02",
                label = "IRONMAN",
                description = "4 hex digits · 43,680 combos · Points ×4",
                color = Color(0xFFFF0000),
                onClick = { onSelectDifficulty(Difficulty.IRONMAN) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DifficultyOption(
                code = "03",
                label = "STAY CURRENT",
                description = "${uiState.difficulty.displayName} · ${uiState.difficulty.digitCount} digits · ×${uiState.difficulty.pointMultiplier}",
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
