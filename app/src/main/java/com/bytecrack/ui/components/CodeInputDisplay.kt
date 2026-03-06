package com.bytecrack.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeInputDisplay(
    currentInput: List<Char>,
    digitCount: Int,
    crackedDigits: Map<Int, Char> = emptyMap(),
    onCrackPosition: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    val editablePositions = (0 until digitCount).filter { it !in crackedDigits }.sorted()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ">",
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        repeat(digitCount) { index ->
            val crackedDigit = crackedDigits[index]
            val editableIdx = editablePositions.indexOf(index)
            val digit = crackedDigit ?: currentInput.getOrNull(editableIdx)
            val isCracked = crackedDigit != null
            val isCurrentSlot = !isCracked && editablePositions.getOrNull(currentInput.size) == index

            val borderColor = when {
                isCracked -> MaterialTheme.colorScheme.secondary
                digit != null -> MaterialTheme.colorScheme.primary
                isCurrentSlot -> MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            }

            val canCrack = onCrackPosition != null && !isCracked && index !in crackedDigits

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .then(
                        if (canCrack) Modifier
                            .clickable { onCrackPosition(index) }
                            .padding(2.dp)
                        else Modifier
                    )
                    .border(
                        2.dp,
                        if (isCracked) borderColor.copy(alpha = 0.8f) else borderColor
                    )
                    .background(
                        if (isCracked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        else Color.Black.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (digit != null) {
                    Text(
                        text = digit.toString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCracked) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                } else if (isCurrentSlot) {
                    Text(
                        text = "█",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(cursorAlpha)
                    )
                } else if (canCrack) {
                    Text(
                        text = "?",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
