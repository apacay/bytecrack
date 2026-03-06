package com.bytecrack.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecrack.data.local.GameSessionDao
import com.bytecrack.data.local.entities.GameSessionEntity
import com.bytecrack.data.remote.PlayGamesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val playGamesRepository: PlayGamesRepository,
    private val gameSessionDao: GameSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLocalScores()
    }

    fun loadLocalScores() {
        viewModelScope.launch {
            val sessions = gameSessionDao.getTopSessions()
            _uiState.update { it.copy(localScores = sessions) }
        }
    }

    fun showLeaderboard(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            gameSessionDao.getHighScore()?.let { score ->
                playGamesRepository.submitScore(activity, score.toLong())
            }
            val success = playGamesRepository.showLeaderboard(activity)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = if (success) null else "Play Games no disponible. Mostrando clasificación local."
                )
            }
        }
    }

    fun signInAndShowLeaderboard(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val signedIn = playGamesRepository.signIn(activity)
            if (signedIn) {
                val success = playGamesRepository.showLeaderboard(activity)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (success) null else "No se pudo abrir el leaderboard"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Inicio de sesión cancelado"
                    )
                }
            }
        }
    }

    fun submitScore(activity: Activity, score: Int) {
        viewModelScope.launch {
            playGamesRepository.submitScore(activity, score.toLong())
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearLocalScores() {
        viewModelScope.launch {
            gameSessionDao.deleteAll()
            loadLocalScores()
        }
    }
}

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val localScores: List<GameSessionEntity> = emptyList()
)
