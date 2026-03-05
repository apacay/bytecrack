package com.bytecrack.domain

import com.bytecrack.domain.model.Difficulty
import com.bytecrack.domain.model.Tier

class TierCalculator {

    fun calculate(
        solveTimeSeconds: Int,
        difficulty: Difficulty
    ): Pair<Tier, Int> {
        val tier = Tier.fromSolveTime(solveTimeSeconds)
        val points = tier.basePoints * difficulty.pointMultiplier
        return tier to points
    }
}
