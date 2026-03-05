package com.bytecrack.domain.model

data class Guess(
    val digits: List<Char>,
    val correctPosition: Int,
    val correctWrongPosition: Int
) {
    val isCorrect: Boolean
        get() = correctPosition == digits.size && correctWrongPosition == 0
}
