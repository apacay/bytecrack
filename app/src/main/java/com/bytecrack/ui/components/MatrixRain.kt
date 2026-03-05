package com.bytecrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val CHAR_SIZE = 14
private const val DROP_SPEED_MIN = 1
private const val DROP_SPEED_MAX = 4

private val matrixChars = "0123456789ABCDEF@#$%&*!?><{}[]|/\\~".toCharArray()

@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41),
    density: Float = 0.6f
) {
    val textMeasurer = rememberTextMeasurer()
    val tick = remember { mutableStateOf(0L) }

    data class Drop(
        var y: Float,
        val x: Float,
        val speed: Int,
        val length: Int,
        var chars: CharArray
    )

    val drops = remember { mutableStateOf<List<Drop>>(emptyList()) }
    val initialized = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            tick.value++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cols = (size.width / CHAR_SIZE).toInt()
        val rows = (size.height / CHAR_SIZE).toInt()

        if (!initialized.value && cols > 0) {
            val numDrops = (cols * density).toInt()
            drops.value = List(numDrops) {
                val x = Random.nextInt(cols) * CHAR_SIZE.toFloat()
                Drop(
                    y = Random.nextFloat() * size.height,
                    x = x,
                    speed = Random.nextInt(DROP_SPEED_MIN, DROP_SPEED_MAX + 1),
                    length = Random.nextInt(4, 16),
                    chars = CharArray(Random.nextInt(4, 16)) { matrixChars.random() }
                )
            }
            initialized.value = true
        }

        @Suppress("UNUSED_EXPRESSION")
        tick.value

        drops.value.forEach { drop ->
            drop.y += drop.speed * 2f
            if (drop.y > size.height + drop.length * CHAR_SIZE) {
                drop.y = -drop.length * CHAR_SIZE.toFloat()
                drop.chars = CharArray(drop.chars.size) { matrixChars.random() }
            }

            drop.chars.forEachIndexed { i, c ->
                val charY = drop.y - i * CHAR_SIZE
                if (charY in 0f..size.height) {
                    val alpha = if (i == 0) 1f
                    else (1f - i.toFloat() / drop.chars.size).coerceIn(0.05f, 0.5f)

                    val charColor = if (i == 0) Color.White.copy(alpha = 0.9f)
                    else color.copy(alpha = alpha)

                    val result = textMeasurer.measure(
                        text = c.toString(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = CHAR_SIZE.sp,
                            color = charColor
                        )
                    )
                    drawText(result, topLeft = Offset(drop.x, charY))
                }
            }
        }
    }
}
