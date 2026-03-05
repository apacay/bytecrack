package com.bytecrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecrack.data.local.GameSessionDao
import com.bytecrack.data.local.entities.GameSessionEntity
import com.bytecrack.domain.CodeGenerator
import com.bytecrack.domain.GuessEvaluator
import com.bytecrack.domain.TierCalculator
import com.bytecrack.domain.model.Difficulty
import com.bytecrack.domain.model.Guess
import com.bytecrack.domain.model.Tier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INITIAL_TIME_SECONDS = 500L
private const val MAX_ATTEMPTS = 10
private const val DIFFICULTY_CHOICE_INTERVAL = 10

@HiltViewModel
class GameViewModel @Inject constructor(
    private val codeGenerator: CodeGenerator,
    private val guessEvaluator: GuessEvaluator,
    private val tierCalculator: TierCalculator,
    private val gameSessionDao: GameSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var levelStartTime = 0L

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(highScore = gameSessionDao.getHighScore()) }
        }
    }

    fun startNewGame() {
        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.value = GameUiState(
                screen = GameScreen.MainMenu,
                highScore = gameSessionDao.getHighScore()
            )
        }
    }

    fun startPlaying() {
        timerJob?.cancel()
        levelStartTime = System.currentTimeMillis()
        val secretCode = codeGenerator.generate(Difficulty.NORMAL)
        _uiState.value = GameUiState(
            screen = GameScreen.Game,
            level = 1,
            secretCode = secretCode,
            guesses = emptyList(),
            attemptsRemaining = MAX_ATTEMPTS,
            timeRemainingSeconds = INITIAL_TIME_SECONDS,
            totalScore = 0,
            difficulty = Difficulty.NORMAL
        )
        startTimer()
    }

    fun submitGuess(digits: List<Char>) {
        val state = _uiState.value
        if (state.secretCode == null || state.isGameOver || state.isLevelComplete) return
        if (digits.size != state.secretCode.size) return
        if (state.attemptsRemaining <= 0) return

        val guess = guessEvaluator.evaluate(state.secretCode, digits)
        val newGuesses = state.guesses + guess
        val newAttempts = state.attemptsRemaining - 1

        if (guess.isCorrect) {
            onLevelComplete(state.timeRemainingSeconds)
            return
        }

        if (newAttempts <= 0) {
            onGameOver(reason = GameOverReason.NoAttemptsLeft)
            return
        }

        _uiState.update {
            it.copy(
                guesses = newGuesses,
                attemptsRemaining = newAttempts
            )
        }
    }

    fun addDigit(digit: Char) {
        val state = _uiState.value
        if (state.currentInput.size >= (state.secretCode?.size ?: 3)) return
        val validDigits = state.difficulty.digits
        if (digit !in validDigits) return
        if (digit in state.currentInput) return

        _uiState.update {
            it.copy(currentInput = it.currentInput + digit)
        }
    }

    fun removeDigit() {
        _uiState.update {
            it.copy(currentInput = it.currentInput.dropLast(1))
        }
    }

    fun clearInput() {
        _uiState.update { it.copy(currentInput = emptyList()) }
    }

    fun submitCurrentInput() {
        val state = _uiState.value
        if (state.currentInput.size == state.secretCode?.size) {
            submitGuess(state.currentInput)
            clearInput()
        }
    }

    fun continueToNextLevel() {
        val state = _uiState.value
        val tier = state.lastTier
        if (!state.isLevelComplete || tier == null) return

        val newLevel = state.level + 1
        val newTime = state.timeRemainingSeconds + tier.bonusSeconds

        if (state.level % DIFFICULTY_CHOICE_INTERVAL == 0) {
            _uiState.update {
                it.copy(
                    screen = GameScreen.DifficultyChoice,
                    isLevelComplete = false,
                    lastTier = null,
                    timeRemainingSeconds = newTime
                )
            }
        } else {
            startNextLevel(newLevel, state.difficulty, newTime)
        }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        val state = _uiState.value
        startNextLevel(state.level + 1, difficulty, state.timeRemainingSeconds)
    }

    private fun startNextLevel(level: Int, difficulty: Difficulty, timeBank: Long) {
        timerJob?.cancel()
        levelStartTime = System.currentTimeMillis()
        val secretCode = codeGenerator.generate(difficulty)
        _uiState.update {
            it.copy(
                screen = GameScreen.Game,
                level = level,
                secretCode = secretCode,
                guesses = emptyList(),
                currentInput = emptyList(),
                attemptsRemaining = MAX_ATTEMPTS,
                timeRemainingSeconds = timeBank,
                difficulty = difficulty,
                isLevelComplete = false,
                lastTier = null
            )
        }
        startTimer()
    }

    private fun onLevelComplete(timeRemaining: Long) {
        timerJob?.cancel()
        val state = _uiState.value
        val solveTime = ((System.currentTimeMillis() - levelStartTime) / 1000).toInt()
        val (tier, points) = tierCalculator.calculate(solveTime, state.difficulty)
        val newScore = state.totalScore + points

        viewModelScope.launch {
            gameSessionDao.insert(
                GameSessionEntity(
                    highScore = newScore,
                    bestLevel = state.level,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        _uiState.update {
            it.copy(
                isLevelComplete = true,
                lastTier = tier,
                totalScore = newScore,
                lastLevelPoints = points
            )
        }
    }

    private fun onGameOver(reason: GameOverReason) {
        timerJob?.cancel()
        val state = _uiState.value
        viewModelScope.launch {
            gameSessionDao.insert(
                GameSessionEntity(
                    highScore = state.totalScore,
                    bestLevel = state.level,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        _uiState.update {
            it.copy(
                screen = GameScreen.GameOver,
                isGameOver = true,
                gameOverReason = reason
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _uiState.value
                val newTime = currentState.timeRemainingSeconds - 1
                if (newTime <= 0) {
                    onGameOver(GameOverReason.TimeUp)
                    return@launch
                }
                _uiState.update { it.copy(timeRemainingSeconds = newTime) }
            }
        }
    }

    fun backToMenu() {
        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.value = GameUiState(
                screen = GameScreen.MainMenu,
                isGameOver = false,
                isLevelComplete = false,
                highScore = gameSessionDao.getHighScore()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

enum class GameScreen { MainMenu, Game, DifficultyChoice, GameOver }
enum class GameOverReason { TimeUp, NoAttemptsLeft }

data class GameUiState(
    val screen: GameScreen = GameScreen.MainMenu,
    val level: Int = 1,
    val secretCode: List<Char>? = null,
    val guesses: List<Guess> = emptyList(),
    val currentInput: List<Char> = emptyList(),
    val attemptsRemaining: Int = MAX_ATTEMPTS,
    val timeRemainingSeconds: Long = INITIAL_TIME_SECONDS,
    val totalScore: Int = 0,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val isGameOver: Boolean = false,
    val isLevelComplete: Boolean = false,
    val lastTier: Tier? = null,
    val lastLevelPoints: Int = 0,
    val gameOverReason: GameOverReason? = null,
    val highScore: Int? = null
)
