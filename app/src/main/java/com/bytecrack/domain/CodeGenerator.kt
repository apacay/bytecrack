package com.bytecrack.domain

import com.bytecrack.domain.model.Difficulty

class CodeGenerator {

    fun generate(difficulty: Difficulty): List<Char> {
        val digits = difficulty.digits.toList()
        val count = difficulty.digitCount
        require(count <= digits.size) { "digitCount cannot exceed available digits" }
        return digits.shuffled().take(count)
    }
}
