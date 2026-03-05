package com.bytecrack.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.White,
    glitchIntensity: Float = 1f
) {
    val glitchOffset = remember { mutableStateOf(0f) }
    val glitchActive = remember { mutableStateOf(false) }
    val scrambledText = remember { mutableStateOf(text) }

    val glitchChars = "!@#$%^&*<>{}[]|/\\~`░▒▓█"

    LaunchedEffect(text) {
        while (true) {
            delay(Random.nextLong(2000, 5000))
            glitchActive.value = true
            repeat(Random.nextInt(2, 5)) {
                glitchOffset.value = Random.nextFloat() * 4f * glitchIntensity - 2f * glitchIntensity
                scrambledText.value = buildString {
                    text.forEach { c ->
                        if (Random.nextFloat() < 0.3f * glitchIntensity) {
                            append(glitchChars.random())
                        } else {
                            append(c)
                        }
                    }
                }
                delay(50)
            }
            glitchOffset.value = 0f
            scrambledText.value = text
            glitchActive.value = false
        }
    }

    Box(modifier = modifier) {
        if (glitchActive.value) {
            Text(
                text = scrambledText.value,
                style = style,
                color = Color(0xFFFF0000).copy(alpha = 0.4f),
                modifier = Modifier.offset(x = glitchOffset.value.dp, y = (-1).dp)
            )
            Text(
                text = scrambledText.value,
                style = style,
                color = Color(0xFF00FFFF).copy(alpha = 0.4f),
                modifier = Modifier.offset(x = (-glitchOffset.value).dp, y = 1.dp)
            )
        }
        Text(
            text = if (glitchActive.value) scrambledText.value else text,
            style = style,
            color = color
        )
    }
}
