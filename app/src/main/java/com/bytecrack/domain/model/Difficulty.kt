package com.bytecrack.domain.model

enum class Difficulty(
    val digitCount: Int,
    val radix: Int,
    val pointMultiplier: Int,
    /** Multiplicador solo para el bonus de tiempo: x2, x4, x10 según dificultad. */
    val timeBonusMultiplier: Int,
    val baseAttempts: Int,
    val rewardAdSeconds: Long,
    val rewardAdBreachExtension: Int,
    val rewardAdBreachThreshold: Int,
    val displayName: String
) {
    NORMAL(digitCount = 3, radix = 10, pointMultiplier = 1, timeBonusMultiplier = 1, baseAttempts = 10, rewardAdSeconds = 90L, rewardAdBreachExtension = 5, rewardAdBreachThreshold = 6, displayName = "Normal"),
    HARD(digitCount = 3, radix = 16, pointMultiplier = 2, timeBonusMultiplier = 2, baseAttempts = 10, rewardAdSeconds = 90L, rewardAdBreachExtension = 5, rewardAdBreachThreshold = 6, displayName = "Hard"),
    VERY_HARD(digitCount = 4, radix = 10, pointMultiplier = 5, timeBonusMultiplier = 4, baseAttempts = 10, rewardAdSeconds = 90L, rewardAdBreachExtension = 5, rewardAdBreachThreshold = 6, displayName = "Very Hard"),
    IRONMAN(digitCount = 4, radix = 16, pointMultiplier = 20, timeBonusMultiplier = 10, baseAttempts = 25, rewardAdSeconds = 200L, rewardAdBreachExtension = 10, rewardAdBreachThreshold = 10, displayName = "Ironman");

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
