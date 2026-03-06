package com.bytecrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

private const val MILESTONE_INTERVAL = 10

@Composable
fun LevelProgressBadge(
    level: Int,
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val remainder = if (level == 0) 0 else level % MILESTONE_INTERVAL
    val levelsToMilestone = if (remainder == 0) 0 else MILESTONE_INTERVAL - remainder
    val isMilestoneLevel = level > 0 && remainder == 0

    val difficultyColor = when (difficulty) {
        Difficulty.NORMAL -> MaterialTheme.colorScheme.primary
        Difficulty.HARD -> Color(0xFFFF6600)
        Difficulty.VERY_HARD -> Color(0xFFFF3300)
        Difficulty.IRONMAN -> Color(0xFFFF0000)
    }

    Row(
        modifier = modifier
            .border(1.dp, difficultyColor.copy(alpha = 0.4f))
            .background(difficultyColor.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "LVL",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = difficultyColor.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%03d", level),
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = difficultyColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "|",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = difficultyColor.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isMilestoneLevel) "UPGRADE!" else "${levelsToMilestone} to ↑",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (isMilestoneLevel)
                MaterialTheme.colorScheme.secondary
            else
                difficultyColor.copy(alpha = 0.6f)
        )
    }
}
