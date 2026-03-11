package com.bytecrack.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytecrack.ads.AdManager
import com.bytecrack.audio.MusicManager
import com.bytecrack.audio.SoundManager
import com.bytecrack.audio.TypingDuration
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
private const val LEVEL_INTRO_DURATION_MS = 3000L
private const val INTERSTITIAL_INTERVAL = 15
private const val TRACE_AD_OFFER_INTERVAL = 3
private const val TRACES_FOR_CRACK = 3

@HiltViewModel
class GameViewModel @Inject constructor(
    private val codeGenerator: CodeGenerator,
    private val guessEvaluator: GuessEvaluator,
    private val tierCalculator: TierCalculator,
    private val gameSessionDao: GameSessionDao,
    private val soundManager: SoundManager,
    private val musicManager: MusicManager,
    private val adManager: AdManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var isTimerPaused = false
    private var levelStartTime = 0L

    /** True cuando el usuario ya ganó la recompensa del ad pero el ad sigue en primer plano; aplicar estado al cerrar. */
    private var earnedRewardPendingAdDismiss = false

    // Flags para diferir reanudar música/timer hasta que la Activity vuelva al primer plano
    private var pendingMusicResume = false
    private var pendingTimerStart = false
    private var pendingTimerResume = false

    fun onActivityResumed() {
        if (pendingMusicResume) {
            pendingMusicResume = false
            musicManager.resume()
        }
        if (pendingTimerStart) {
            pendingTimerStart = false
            startTimer()
        }
        if (pendingTimerResume) {
            pendingTimerResume = false
            resumeTimer()
        }
    }

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    highScore = gameSessionDao.getHighScore(),
                    isMusicEnabled = musicManager.isMusicEnabled
                )
            }
        }
        musicManager.playForLevel(1)
    }

    fun startNewGame() {
        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.value = GameUiState(
                screen = GameScreen.MainMenu,
                highScore = gameSessionDao.getHighScore(),
                isMusicEnabled = musicManager.isMusicEnabled
            )
        }
    }

    fun startPlaying() {
        timerJob?.cancel()
        levelStartTime = System.currentTimeMillis()
        val secretCode = codeGenerator.generate(Difficulty.NORMAL)
        musicManager.playForLevel(1)
        _uiState.value = GameUiState(
            screen = GameScreen.Game,
            showLevelIntro = true,
            level = 1,
            secretCode = secretCode,
            guesses = emptyList(),
            attemptsRemaining = Difficulty.NORMAL.baseAttempts,
            maxAttempts = Difficulty.NORMAL.baseAttempts,
            timeRemainingSeconds = INITIAL_TIME_SECONDS,
            totalScore = 0,
            difficulty = Difficulty.NORMAL,
            isMusicEnabled = musicManager.isMusicEnabled,
            traceCount = 0,
            levelsPlayedThisSession = 0,
            levelsCompletedSinceLastTraceOffer = 0
        )
        scheduleLevelIntroDismiss()
    }

    fun preloadAds(activity: Activity) {
        adManager.loadInterstitial(activity)
        adManager.loadRewarded(activity)
    }

    fun preloadRewardAdForCheckpoint(activity: Activity) {
        adManager.loadRewarded(activity)
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
            val timeAtGuess = state.timeRemainingSeconds
            // Agregar el guess ganador al log para que el usuario lo vea antes de la transición
            _uiState.update { it.copy(guesses = newGuesses, attemptsRemaining = newAttempts) }
            viewModelScope.launch {
                delay(750)
                onLevelComplete(timeAtGuess)
            }
            return
        }

        if (newAttempts <= 0) {
            timerJob?.cancel()
            soundManager.playAccessDenied()
            _uiState.update {
                it.copy(
                    guesses = newGuesses,
                    attemptsRemaining = 0,
                    showDiscoveredTransition = true,
                    pendingRewardAdFromTimeOut = false
                )
            }
            return
        }

        soundManager.playAccessDenied()
        _uiState.update {
            it.copy(
                guesses = newGuesses,
                attemptsRemaining = newAttempts
            )
        }
    }

    fun buildFullGuess(): List<Char>? {
        val state = _uiState.value
        val secret = state.secretCode ?: return null
        val digitCount = secret.size
        val editableCount = digitCount - state.crackedDigits.size
        if (state.currentInput.size != editableCount) return null

        val full = mutableListOf<Char>()
        var inputIdx = 0
        for (i in 0 until digitCount) {
            full.add(state.crackedDigits[i] ?: state.currentInput[inputIdx++])
        }
        return full
    }

    fun addDigit(digit: Char) {
        val state = _uiState.value
        val secret = state.secretCode ?: return
        val digitCount = secret.size
        val editableCount = digitCount - state.crackedDigits.size
        if (state.currentInput.size >= editableCount) return

        val normalized = digit.uppercaseChar()
        val validDigits = state.difficulty.digits
        if (normalized !in validDigits) return
        val alreadyUsed = normalized in state.currentInput || normalized in state.crackedDigits.values
        if (alreadyUsed) return

        soundManager.playKeyPress()
        _uiState.update {
            it.copy(currentInput = it.currentInput + normalized)
        }
    }

    fun removeDigit() {
        soundManager.playDelete()
        _uiState.update {
            it.copy(currentInput = it.currentInput.dropLast(1))
        }
    }

    fun clearInput() {
        _uiState.update { it.copy(currentInput = emptyList()) }
    }

    fun submitCurrentInput() {
        val fullGuess = buildFullGuess() ?: return
        val state = _uiState.value
        if (fullGuess.size == (state.secretCode?.size ?: 0)) {
            soundManager.playSubmit()
            submitGuess(fullGuess)
            clearInput()
        }
    }

    fun dismissDiscoveredTransition() {
        val fromTimeOut = _uiState.value.pendingRewardAdFromTimeOut
        val hasRewardAd = adManager.isRewardedReady()
        val rewardAdAvailable = hasRewardAd && !_uiState.value.rewardAdUsedThisBlock
        val reason = if (fromTimeOut) GameOverReason.TimeUp else GameOverReason.NoAttemptsLeft
        if (rewardAdAvailable) {
            _uiState.update {
                it.copy(
                    showDiscoveredTransition = false,
                    pendingRewardAdFromTimeOut = false,
                    offerRewardedAd = true,
                    offerRewardedAdFromTimeOut = fromTimeOut,
                    rewardAdOriginalReason = reason
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    showDiscoveredTransition = false,
                    pendingRewardAdFromTimeOut = false,
                    showFailureTransition = true,
                    pendingGameOverReason = reason
                )
            }
        }
    }

    fun requestRewardedAd(activity: Activity) {
        musicManager.pause()
        if (adManager.showRewarded(
                activity,
                onReward = {
                    // Solo marcar que ganó la recompensa; la transición y sonidos se aplican al CERRAR el ad (onDismissed)
                    earnedRewardPendingAdDismiss = true
                },
                onDismissed = {
                    pendingMusicResume = true
                    if (earnedRewardPendingAdDismiss) {
                        earnedRewardPendingAdDismiss = false
                        val state = _uiState.value
                        val n = state.maxAttempts - state.attemptsRemaining
                        val m = state.maxAttempts
                        val p = m / 2
                        val (newAttempts, newMax) = if (n < p) {
                            state.attemptsRemaining to state.maxAttempts
                        } else {
                            p to (n + p)
                        }
                        _uiState.update {
                            it.copy(
                                attemptsRemaining = newAttempts,
                                maxAttempts = newMax,
                                timeRemainingSeconds = it.timeRemainingSeconds + state.difficulty.rewardAdSeconds,
                                offerRewardedAd = false,
                                offerRewardedAdFromTimeOut = false,
                                rewardAdUsedThisBlock = true,
                                wonViaReward = true,
                                showEscapeTransition = true
                            )
                        }
                    } else if (_uiState.value.offerRewardedAd) {
                        declineExtraAttempt()
                    }
                }
            )
        ) {
            return
        }
        pendingMusicResume = true
        declineExtraAttempt()
    }

    fun declineExtraAttempt() {
        val reason = if (_uiState.value.offerRewardedAdFromTimeOut) GameOverReason.TimeUp else GameOverReason.NoAttemptsLeft
        _uiState.update {
            it.copy(
                offerRewardedAd = false,
                offerRewardedAdFromTimeOut = false,
                showFailureTransition = true,
                pendingGameOverReason = reason
            )
        }
    }

    fun dismissEscapeTransition() {
        _uiState.update {
            it.copy(
                showEscapeTransition = false,
                showPotentialGameOverAfterReward = true
            )
        }
        soundManager.playGameOver()
    }

    fun confirmContinueAfterReward() {
        _uiState.update {
            it.copy(
                showPotentialGameOverAfterReward = false,
                showTransitionBackToGame = true
            )
        }
    }

    fun confirmGiveUpAfterReward() {
        _uiState.update {
            it.copy(
                showPotentialGameOverAfterReward = false,
                showTransitionToGiveUp = true
            )
        }
    }

    fun dismissTransitionBackToGame() {
        _uiState.update { it.copy(showTransitionBackToGame = false) }
        startTimer()
    }

    fun dismissTransitionToGiveUp() {
        val reason = _uiState.value.rewardAdOriginalReason ?: GameOverReason.NoAttemptsLeft
        _uiState.update { it.copy(showTransitionToGiveUp = false, rewardAdOriginalReason = null) }
        onGameOver(reason = reason, playSound = true)
    }

    fun completeGameOver() {
        val reason = _uiState.value.pendingGameOverReason ?: GameOverReason.NoAttemptsLeft
        _uiState.update {
            it.copy(
                showFailureTransition = false,
                pendingGameOverReason = null
            )
        }
        onGameOver(reason = reason, playSound = true)
    }

    fun requestContinueWithAd(activity: Activity?) {
        requestInterstitialIfDueAndThen(activity) { continueToNextLevel() }
    }

    fun requestInterstitialAndSelectDifficulty(activity: Activity?, difficulty: Difficulty) {
        requestInterstitialIfDueAndThen(activity) { selectDifficulty(difficulty) }
    }

    private fun requestInterstitialIfDueAndThen(activity: Activity?, onDismiss: () -> Unit) {
        val state = _uiState.value
        val shouldShowInterstitial = state.levelsPlayedThisSession > 0 &&
            state.levelsPlayedThisSession % INTERSTITIAL_INTERVAL == 0
        if (shouldShowInterstitial && activity != null) {
            musicManager.pause()
            if (adManager.showInterstitial(activity) {
                pendingMusicResume = true
                onDismiss()
            }) {
                return
            }
            pendingMusicResume = true
        }
        onDismiss()
    }

    fun requestTraceAd(activity: Activity) {
        musicManager.pause()
        if (adManager.showRewarded(
                activity,
                onReward = {
                    val wasAtStart = _uiState.value.showTraceAdOfferAtStart
                    _uiState.update {
                        it.copy(
                            traceCount = it.traceCount + 1,
                            showTraceAdOffer = false,
                            showTraceAdOfferAtStart = false
                        )
                    }
                    if (wasAtStart) {
                        // Diferir el inicio del timer hasta que la Activity vuelva al primer plano
                        pendingTimerStart = true
                    } else {
                        continueToNextLevel()
                    }
                },
                onDismissed = {
                    pendingMusicResume = true
                    if (_uiState.value.showTraceAdOffer || _uiState.value.showTraceAdOfferAtStart) {
                        skipTraceOffer()
                    }
                }
            )
        ) {
            return
        }
        pendingMusicResume = true
        skipTraceOffer()
    }

    fun skipTraceOffer() {
        val wasAtStart = _uiState.value.showTraceAdOfferAtStart
        _uiState.update {
            it.copy(
                showTraceAdOffer = false,
                showTraceAdOfferAtStart = false
            )
        }
        if (wasAtStart) {
            startTimer()
        } else {
            continueToNextLevel()
        }
    }

    fun useTraceForHint() {
        val state = _uiState.value
        val secret = state.secretCode ?: return
        if (state.traceCount < 1) return

        val alreadyRevealed = state.hintRevealedDigits.toSet()
        val availableDigits = secret
            .filterIndexed { index, _ -> index !in state.crackedDigits }
            .filter { it !in alreadyRevealed }
        if (availableDigits.isEmpty()) return

        soundManager.playTraceUse()
        val digit = availableDigits.random()
        _uiState.update {
            it.copy(
                traceCount = it.traceCount - 1,
                hintRevealedDigits = it.hintRevealedDigits + digit,
                showHintPopup = true,
                lastHintDigit = digit
            )
        }
        pauseTimer()
    }

    fun dismissHintPopup() {
        _uiState.update { it.copy(showHintPopup = false) }
        resumeTimer()
    }

    fun requestTraceAdForPurchase(activity: Activity) {
        musicManager.pause()
        if (adManager.showRewarded(
                activity,
                onReward = {
                    _uiState.update {
                        it.copy(
                            traceCount = it.traceCount + 1,
                            offerTraceAd = false
                        )
                    }
                    pendingTimerResume = true
                },
                onDismissed = {
                    pendingMusicResume = true
                    _uiState.update { it.copy(offerTraceAd = false) }
                    pendingTimerResume = true
                }
            )
        ) {
            return
        }
        pendingMusicResume = true
        _uiState.update { it.copy(offerTraceAd = false) }
        pendingTimerResume = true
    }

    fun declineTraceAd() {
        _uiState.update { it.copy(offerTraceAd = false) }
        resumeTimer()
    }

    fun useTracesForCrack(position: Int) {
        val state = _uiState.value
        val secret = state.secretCode ?: return
        if (state.traceCount < TRACES_FOR_CRACK || position in state.crackedDigits) return
        if (position !in secret.indices) return

        soundManager.playTraceUse()
        val digit = secret[position]
        _uiState.update {
            it.copy(
                traceCount = it.traceCount - TRACES_FOR_CRACK,
                crackedDigits = it.crackedDigits + (position to digit),
                showCrackPopup = true,
                lastCrackPosition = position,
                lastCrackDigit = digit
            )
        }
        pauseTimer()
    }

    fun dismissCrackPopup() {
        _uiState.update { it.copy(showCrackPopup = false) }
        resumeTimer()
    }

    fun continueToNextLevel() {
        val state = _uiState.value
        val tier = state.lastTier
        if (!state.isLevelComplete || tier == null) return

        if (state.showTraceAdOffer || state.showTraceAdOfferAtStart) return

        val newLevel = state.level + 1
        val effectiveBonusSeconds = tier.bonusSeconds * state.difficulty.pointMultiplier
        val newTime = state.timeRemainingSeconds + effectiveBonusSeconds

        if (state.level % DIFFICULTY_CHOICE_INTERVAL == 0) {
            musicManager.restoreVolume()
            _uiState.update {
                it.copy(
                    screen = GameScreen.DifficultyChoice,
                    isLevelComplete = false,
                    lastTier = null,
                    currentTier = null,
                    currentTierPoints = 0,
                    timeRemainingSeconds = newTime,
                    rewardAdUsedThisBlock = false
                )
            }
        } else {
            startNextLevel(newLevel, state.difficulty, newTime)
        }
        _uiState.update { it.copy(timeBonusDisplayApplied = false, scoreBonusDisplayApplied = false) }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        soundManager.playDifficultySelect()
        val state = _uiState.value
        startNextLevel(state.level + 1, difficulty, state.timeRemainingSeconds)
    }

    private fun startNextLevel(level: Int, difficulty: Difficulty, timeBank: Long) {
        timerJob?.cancel()
        levelStartTime = System.currentTimeMillis()
        val secretCode = codeGenerator.generate(difficulty)
        musicManager.playForLevel(level)
        _uiState.update {
            it.copy(
                screen = GameScreen.Game,
                showLevelIntro = true,
                level = level,
                secretCode = secretCode,
                guesses = emptyList(),
                currentInput = emptyList(),
                attemptsRemaining = difficulty.baseAttempts,
                maxAttempts = difficulty.baseAttempts,
                timeRemainingSeconds = timeBank,
                difficulty = difficulty,
                isLevelComplete = false,
                lastTier = null,
                currentTier = null,
                currentTierPoints = 0,
                hintRevealedDigits = emptyList(),
                crackedDigits = emptyMap(),
                wonViaReward = false,
                showTraceAdOffer = false,
                offerTraceAd = false,
                timeBonusDisplayApplied = false,
                scoreBonusDisplayApplied = false
            )
        }
        scheduleLevelIntroDismiss()
    }

    private fun scheduleLevelIntroDismiss() {
        viewModelScope.launch {
            delay(LEVEL_INTRO_DURATION_MS)
            val state = _uiState.value
            val showTraceOfferAtStart = state.level == 1 && state.traceCount == 0
            _uiState.update {
                it.copy(
                    showLevelIntro = false,
                    showTraceAdOfferAtStart = showTraceOfferAtStart
                )
            }
            if (!showTraceOfferAtStart) {
                startTimer()
            }
        }
    }

    fun dismissVictoryPenetration() {
        _uiState.update { it.copy(showVictoryPenetration = false) }
    }

    private fun onLevelComplete(timeRemaining: Long) {
        timerJob?.cancel()
        val state = _uiState.value
        val solveTime = ((System.currentTimeMillis() - levelStartTime) / 1000).toInt()
        val (tier, points) = tierCalculator.calculate(solveTime, state.difficulty)
        val pointsToAdd = if (state.wonViaReward) 0 else points

        val newLevelsCompleted = state.levelsCompletedSinceLastTraceOffer + 1
        val showTraceOfferFromInterval = newLevelsCompleted >= TRACE_AD_OFFER_INTERVAL
        val showTraceOffer = showTraceOfferFromInterval

        musicManager.fadeToSilence()
        _uiState.update {
            it.copy(
                isLevelComplete = true,
                showVictoryPenetration = true,
                lastTier = tier,
                // totalScore NO se modifica aqui; queda en el valor acumulado anterior.
                // lastLevelPoints guarda los puntos pendientes que la animacion del odometro sumara.
                lastLevelPoints = pointsToAdd,
                levelsPlayedThisSession = it.levelsPlayedThisSession + 1,
                levelsCompletedSinceLastTraceOffer = if (showTraceOfferFromInterval) 0 else newLevelsCompleted,
                showTraceAdOffer = showTraceOffer
            )
        }
    }

    // Llamado desde la UI al terminar la animacion del odometro de score.
    fun confirmScoreBonus() {
        _uiState.update { it.copy(totalScore = it.totalScore + it.lastLevelPoints) }
    }

    /** Marca que la animación del bonus de tiempo ya se mostró (para no re-animar en rotación). */
    fun markTimeBonusDisplayed() {
        _uiState.update { it.copy(timeBonusDisplayApplied = true) }
    }

    /** Marca que el bonus de score ya se aplicó (para no volver a sumar en rotación). */
    fun markScoreBonusDisplayed() {
        _uiState.update { it.copy(scoreBonusDisplayApplied = true) }
    }

    private fun onGameOver(reason: GameOverReason, playSound: Boolean = true) {
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
        if (playSound) soundManager.playGameOver()
        musicManager.fadeOut()
        _uiState.update {
            it.copy(
                screen = GameScreen.GameOver,
                isGameOver = true,
                gameOverReason = reason
            )
        }
    }

    private fun pauseTimer() {
        isTimerPaused = true
    }

    private fun resumeTimer() {
        isTimerPaused = false
    }

    private fun addRewardSeconds(seconds: Long) {
        _uiState.update { it.copy(timeRemainingSeconds = it.timeRemainingSeconds + seconds) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        isTimerPaused = false
        timerJob = viewModelScope.launch {
            // Inicializar tier actual inmediatamente al arrancar
            val initState = _uiState.value
            val initElapsed = ((System.currentTimeMillis() - levelStartTime) / 1000).toInt()
            val (initTier, initPoints) = tierCalculator.calculate(initElapsed, initState.difficulty)
            _uiState.update { it.copy(currentTier = initTier, currentTierPoints = initPoints) }

            while (true) {
                delay(1000)
                if (isTimerPaused) continue
                val currentState = _uiState.value
                val newTime = currentState.timeRemainingSeconds - 1

                val elapsedSec = ((System.currentTimeMillis() - levelStartTime) / 1000).toInt()
                val (newCurrentTier, newCurrentPoints) = tierCalculator.calculate(elapsedSec, currentState.difficulty)
                val tierChanged = currentState.currentTier != null && currentState.currentTier != newCurrentTier

                if (newTime <= 0) {
                    if (currentState.attemptsRemaining > 0) {
                        _uiState.update {
                            it.copy(
                                timeRemainingSeconds = 0,
                                showDiscoveredTransition = true,
                                pendingRewardAdFromTimeOut = true,
                                currentTier = newCurrentTier,
                                currentTierPoints = newCurrentPoints
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                timeRemainingSeconds = 0,
                                showFailureTransition = true,
                                pendingGameOverReason = GameOverReason.TimeUp,
                                currentTier = newCurrentTier,
                                currentTierPoints = newCurrentPoints
                            )
                        }
                    }
                    return@launch
                }
                if (currentState.timeRemainingSeconds > 30 && newTime <= 30) {
                    soundManager.playTimerLow()
                }
                _uiState.update {
                    it.copy(
                        timeRemainingSeconds = newTime,
                        currentTier = newCurrentTier,
                        currentTierPoints = newCurrentPoints,
                        tierJustChanged = tierChanged
                    )
                }
                if (tierChanged) {
                    // Resetear la flag despues de que la UI la consuma (shake ~600ms)
                    viewModelScope.launch {
                        delay(600)
                        _uiState.update { it.copy(tierJustChanged = false) }
                    }
                }
            }
        }
    }

    fun playTypingSound(duration: TypingDuration) {
        soundManager.playTyping(duration)
    }

    fun playAccessGranted() = soundManager.playAccessGranted()

    fun playTierWinSound() {
        val tier = _uiState.value.lastTier ?: return
        soundManager.playTierWin(tier)
    }

    fun playTimerOdometer() = soundManager.playTimerOdometer()
    fun playScoreOdometer() = soundManager.playScoreOdometer()
    fun playOdometerSettle() = soundManager.playScoreSettle()

    fun playSystemOk() = soundManager.playSystemOk()
    fun playSystemErr() = soundManager.playSystemErr()
    fun playSystemWarn() = soundManager.playSystemWarn()
    fun playEnterPress() = soundManager.playEnterPress()
    fun playPenetrationSuccess() = soundManager.playPenetrationSuccess()
    fun playSshConnect() = soundManager.playSshConnect()
    fun playDifficultySelect() = soundManager.playDifficultySelect()

    fun toggleMusic() {
        val enabled = musicManager.toggle()
        _uiState.update { it.copy(isMusicEnabled = enabled) }
    }

    fun showLeaderboard() {
        timerJob?.cancel()
        _uiState.update { it.copy(screen = GameScreen.Leaderboard) }
    }

    fun backToMenu() {
        timerJob?.cancel()
        viewModelScope.launch {
            _uiState.value = GameUiState(
                screen = GameScreen.MainMenu,
                isGameOver = false,
                isLevelComplete = false,
                highScore = gameSessionDao.getHighScore(),
                isMusicEnabled = musicManager.isMusicEnabled
            )
        }
        musicManager.playForLevel(1)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

enum class GameScreen { MainMenu, Leaderboard, Game, DifficultyChoice, GameOver }
enum class GameOverReason { TimeUp, NoAttemptsLeft }

data class GameUiState(
    val screen: GameScreen = GameScreen.MainMenu,
    val showLevelIntro: Boolean = false,
    val showVictoryPenetration: Boolean = false,
    val showFailureTransition: Boolean = false,
    val pendingGameOverReason: GameOverReason? = null,
    val showDiscoveredTransition: Boolean = false,
    val pendingRewardAdFromTimeOut: Boolean = false,
    val showEscapeTransition: Boolean = false,
    val showPotentialGameOverAfterReward: Boolean = false,
    val showTransitionBackToGame: Boolean = false,
    val showTransitionToGiveUp: Boolean = false,
    val rewardAdOriginalReason: GameOverReason? = null,
    val level: Int = 1,
    val secretCode: List<Char>? = null,
    val guesses: List<Guess> = emptyList(),
    val currentInput: List<Char> = emptyList(),
    val attemptsRemaining: Int = MAX_ATTEMPTS,
    val maxAttempts: Int = MAX_ATTEMPTS,
    val timeRemainingSeconds: Long = INITIAL_TIME_SECONDS,
    val totalScore: Int = 0,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val isGameOver: Boolean = false,
    val isLevelComplete: Boolean = false,
    val lastTier: Tier? = null,
    val lastLevelPoints: Int = 0,
    val gameOverReason: GameOverReason? = null,
    val highScore: Int? = null,
    val isMusicEnabled: Boolean = true,
    val offerRewardedAd: Boolean = false,
    val offerRewardedAdFromTimeOut: Boolean = false,
    val rewardAdUsedThisBlock: Boolean = false,
    val traceCount: Int = 0,
    val hintRevealedDigits: List<Char> = emptyList(),
    val crackedDigits: Map<Int, Char> = emptyMap(),
    val wonViaReward: Boolean = false,
    val levelsPlayedThisSession: Int = 0,
    val levelsCompletedSinceLastTraceOffer: Int = 0,
    val showTraceAdOffer: Boolean = false,
    val offerTraceAd: Boolean = false,
    val showTraceAdOfferAtStart: Boolean = false,
    val showHintPopup: Boolean = false,
    val lastHintDigit: Char? = null,
    val showCrackPopup: Boolean = false,
    val lastCrackPosition: Int? = null,
    val lastCrackDigit: Char? = null,
    val currentTier: Tier? = null,
    val currentTierPoints: Int = 0,
    val tierJustChanged: Boolean = false,
    /** True cuando la animación del bonus de tiempo ya se mostró (evita re-animación en rotación). */
    val timeBonusDisplayApplied: Boolean = false,
    /** True cuando el bonus de score ya se confirmó (evita re-sumando en rotación). */
    val scoreBonusDisplayApplied: Boolean = false
)
