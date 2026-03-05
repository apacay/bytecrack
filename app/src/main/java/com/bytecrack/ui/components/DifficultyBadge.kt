package com.bytecrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecrack.domain.model.Difficulty

@Composable
fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (difficulty) {
        Difficulty.NORMAL -> "3 DEC" to MaterialTheme.colorScheme.primary
        Difficulty.HARD -> "4 DEC x2" to Color(0xFFFF6600)
        Difficulty.IRONMAN -> "4 HEX x4" to Color(0xFFFF0000)
    }

    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.5f))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
