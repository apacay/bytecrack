package com.bytecrack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerBar(
    timeRemaining: Long,
    maxTime: Long = 500L,
    isUrgent: Boolean = false,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fraction = (timeRemaining.toFloat() / maxTime).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(400),
        label = "timerBar"
    )

    val isLow = fraction < 0.15f
    val isMedium = fraction < 0.30f

    val barColor = when {
        isLow -> Color(0xFFFF0000)
        isMedium -> Color(0xFFFF6600)
        else -> MaterialTheme.colorScheme.primary
    }

    // Escala dinamica: 18sp si urgente (<30s), 16sp normal
    val targetFontSize = if (isUrgent) 18f else 16f
    val animatedFontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = tween(400),
        label = "timerFontSize"
    )

    // Highlight: texto y borde se vuelven cyan cuando el bonus de timer esta animando
    val targetTextColor = if (isHighlighted) Color(0xFF00FFFF) else barColor
    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(300),
        label = "timerTextColor"
    )

    val targetBorderAlpha = if (isHighlighted) 0.9f else 0.45f
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = targetBorderAlpha,
        animationSpec = tween(300),
        label = "timerBorderAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val fillAlpha = if (isLow) pulseAlpha else 0.65f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${timeRemaining}s",
            fontFamily = FontFamily.Monospace,
            fontSize = animatedFontSize.sp,
            fontWeight = FontWeight.Bold,
            color = animatedTextColor,
            modifier = Modifier.widthIn(min = 44.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .border(1.dp, barColor.copy(alpha = animatedBorderAlpha))
                .background(barColor.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(barColor.copy(alpha = fillAlpha))
            )
        }
    }
}
