package com.bytecrack.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.bytecrack.ui.screens.GameScreen
import com.bytecrack.ui.screens.MainMenuScreen
import com.bytecrack.ui.viewmodel.GameScreen as GameScreenState
import com.bytecrack.ui.viewmodel.GameViewModel

@Composable
fun ByteCrackApp(
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.screen) {
        GameScreenState.MainMenu -> MainMenuScreen(
            highScore = uiState.highScore,
            onStartGame = { viewModel.startPlaying() }
        )
        GameScreenState.Game,
        GameScreenState.DifficultyChoice,
        GameScreenState.GameOver -> GameScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBackToMenu = { viewModel.backToMenu() }
        )
    }
}
