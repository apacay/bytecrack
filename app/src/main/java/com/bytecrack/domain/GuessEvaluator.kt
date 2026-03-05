package com.bytecrack.domain

import com.bytecrack.domain.model.Guess

class GuessEvaluator {

    fun evaluate(secret: List<Char>, guess: List<Char>): Guess {
        require(secret.size == guess.size) { "Guess must have same length as secret" }

        var correctPosition = 0
        var correctWrongPosition = 0

        val secretUsed = secret.toMutableList()
        val guessUsed = guess.toMutableList()

        for (i in secret.indices) {
            if (guess[i] == secret[i]) {
                correctPosition++
                secretUsed[i] = '_'
                guessUsed[i] = '_'
            }
        }

        for (i in guessUsed.indices) {
            if (guessUsed[i] == '_') continue
            val idx = secretUsed.indexOf(guessUsed[i])
            if (idx >= 0) {
                correctWrongPosition++
                secretUsed[idx] = '_'
            }
        }

        return Guess(
            digits = guess,
            correctPosition = correctPosition,
            correctWrongPosition = correctWrongPosition
        )
    }
}
