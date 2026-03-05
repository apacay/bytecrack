package com.bytecrack.domain

import com.bytecrack.domain.model.Difficulty

class CodeGenerator {

    fun generate(difficulty: Difficulty): List<Char> {
        val digits = difficulty.digits.toList()
        val count = difficulty.digitCount
        val shuffled = digits.shuffled()
        return shuffled.take(count)
    }
}
