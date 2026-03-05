package com.bytecrack.domain.model

enum class Tier(
    val displayName: String,
    val bonusSeconds: Int,
    val basePoints: Int,
    val maxTimeSeconds: Int
) {
    S("Root Access", 30, 1000, 15),
    A("Admin", 20, 500, 30),
    B("User", 10, 250, 60),
    C("Guest", 5, 100, 90),
    D("Script Kiddie", 0, 50, Int.MAX_VALUE);

    companion object {
        fun fromSolveTime(seconds: Int): Tier = entries.find { seconds < it.maxTimeSeconds } ?: D
    }
}
