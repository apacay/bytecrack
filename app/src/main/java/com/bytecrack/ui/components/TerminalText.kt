package com.bytecrack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

@Composable
fun TerminalText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.primary,
    charDelayMs: Long = 40L,
    showCursor: Boolean = true,
    onComplete: () -> Unit = {}
) {
    val displayedText = remember(fullText) { mutableStateOf("") }
    val cursorVisible = remember { mutableStateOf(true) }
    val complete = remember(fullText) { mutableStateOf(false) }

    LaunchedEffect(fullText) {
        displayedText.value = ""
        complete.value = false
        for (i in fullText.indices) {
            displayedText.value = fullText.substring(0, i + 1)
            kotlinx.coroutines.delay(charDelayMs)
        }
        complete.value = true
        onComplete()
    }

    LaunchedEffect(complete.value) {
        if (complete.value && showCursor) {
            while (true) {
                cursorVisible.value = !cursorVisible.value
                kotlinx.coroutines.delay(530)
            }
        }
    }

    val cursor = if (showCursor && cursorVisible.value) "█" else if (showCursor) " " else ""

    Text(
        text = displayedText.value + cursor,
        modifier = modifier,
        style = style,
        color = color
    )
}
