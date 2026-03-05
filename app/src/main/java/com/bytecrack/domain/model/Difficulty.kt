package com.bytecrack.domain.model

enum class Difficulty(
    val digitCount: Int,
    val radix: Int,
    val pointMultiplier: Int,
    val displayName: String
) {
    NORMAL(digitCount = 3, radix = 10, pointMultiplier = 1, displayName = "Normal"),
    HARD(digitCount = 4, radix = 10, pointMultiplier = 2, displayName = "Hard"),
    IRONMAN(digitCount = 4, radix = 16, pointMultiplier = 4, displayName = "Ironman");

    val digits: CharArray
        get() = when (radix) {
            10 -> charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            16 -> charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F')
            else -> charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        }

    fun digitToChar(value: Int): Char = digits[value]
    fun charToDigit(c: Char): Int = digits.indexOf(c.uppercaseChar())
}
