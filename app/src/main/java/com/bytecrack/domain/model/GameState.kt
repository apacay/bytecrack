package com.bytecrack.domain.model

data class GameState(
    val level: Int,
    val secretCode: List<Char>,
    val guesses: List<Guess>,
    val attemptsRemaining: Int,
    val timeRemainingSeconds: Long,
    val totalScore: Int,
    val difficulty: Difficulty,
    val isGameOver: Boolean,
    val isLevelComplete: Boolean,
    val lastTier: Tier?
) {
    val currentGuess: List<Char> = guesses.lastOrNull()?.digits ?: emptyList()
}
