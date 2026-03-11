package com.bytecrack.domain.model

enum class Tier(
    val displayName: String,
    val bonusSeconds: Int,
    val basePoints: Int,
    val maxTimeSeconds: Int,
    /** Duración aproximada del sonido de victoria (ms) para esperar antes del siguiente paso. */
    val winSoundDurationMs: Long
) {
    S("Root Access", 90, 1000, 30, 3_500L),
    A("Admin", 60, 500, 60, 2_500L),
    B("User", 35, 250, 90, 2_000L),
    C("Guest", 20, 100, 120, 1_500L),
    D("Script Kiddie", 5, 50, Int.MAX_VALUE, 1_000L);

    companion object {
        fun fromSolveTime(seconds: Int): Tier = entries.find { seconds < it.maxTimeSeconds } ?: D
    }
}
