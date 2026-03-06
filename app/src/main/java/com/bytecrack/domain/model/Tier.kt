package com.bytecrack.domain.model

enum class Tier(
    val displayName: String,
    val bonusSeconds: Int,
    val basePoints: Int,
    val maxTimeSeconds: Int
) {
    S("Root Access", 90, 1000, 30),
    A("Admin", 60, 500, 60),
    B("User", 35, 250, 90),
    C("Guest", 20, 100, 120),
    D("Script Kiddie", 5, 50, Int.MAX_VALUE);

    companion object {
        fun fromSolveTime(seconds: Int): Tier = entries.find { seconds < it.maxTimeSeconds } ?: D
    }
}
