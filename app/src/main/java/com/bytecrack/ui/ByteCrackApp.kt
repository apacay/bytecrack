package com.bytecrack.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bytecrack.ui.screens.GameScreen
import com.bytecrack.ui.screens.LanguagePopup
import com.bytecrack.ui.screens.LeaderboardScreen
import com.bytecrack.ui.screens.MainMenuScreen
import com.bytecrack.ui.viewmodel.GameScreen as GameScreenState
import com.bytecrack.ui.viewmodel.GameViewModel

@Composable
fun ByteCrackApp(
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.screen) {
            GameScreenState.MainMenu -> MainMenuScreen(
                highScore = uiState.highScore,
                isMusicEnabled = uiState.isMusicEnabled,
                currentLanguage = uiState.currentLanguage,
                onStartGame = { viewModel.startPlaying() },
                onToggleMusic = { viewModel.toggleMusic() },
                onShowLeaderboard = { viewModel.showLeaderboard() },
                onShowLanguagePopup = { viewModel.showLanguagePopup() }
            )
            GameScreenState.Leaderboard -> LeaderboardScreen(
                onBack = { viewModel.backToMenu() }
            )
            GameScreenState.Game,
            GameScreenState.DifficultyChoice,
            GameScreenState.GameOver -> GameScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBackToMenu = { viewModel.backToMenu() }
            )
        }

        AnimatedVisibility(
            visible = uiState.showLanguagePopup,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            LanguagePopup(
                currentLanguage = uiState.currentLanguage,
                onLanguageSelected = { viewModel.selectLanguage(it) },
                onDismiss = { viewModel.dismissLanguagePopup() }
            )
        }
    }
}
