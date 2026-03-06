package com.bytecrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
private const val DROP_SPEED_MAX = 5

private val matrixChars = "0123456789ABCDEF@#$%&*!?><{}[]|/\\~".toCharArray()

@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41),
    density: Float = 0.6f,
    instanceId: Int = 0
) {
    val textMeasurer = rememberTextMeasurer()
    val tick = remember(instanceId) { androidx.compose.runtime.mutableStateOf(0L) }
    val rng = remember(instanceId) { Random(instanceId + System.nanoTime().hashCode()) }

    data class Drop(
        var y: Float,
        var x: Float,
        var speed: Int,
        var length: Int,
        var chars: CharArray
    )

    val drops = remember(instanceId) { androidx.compose.runtime.mutableStateOf<List<Drop>>(emptyList()) }
    val initialized = remember(instanceId) { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(instanceId) {
        while (true) {
            delay(50)
            tick.value++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cols = (size.width / CHAR_SIZE).toInt().coerceAtLeast(1)

        if (!initialized.value && cols > 0) {
            val numDrops = (cols * density).toInt().coerceAtLeast(2)
            drops.value = List(numDrops) {
                Drop(
                    y = rng.nextFloat() * size.height,
                    x = rng.nextInt(cols) * CHAR_SIZE.toFloat(),
                    speed = rng.nextInt(DROP_SPEED_MIN, DROP_SPEED_MAX + 1),
                    length = rng.nextInt(4, 18),
                    chars = CharArray(rng.nextInt(4, 18)) { matrixChars[rng.nextInt(matrixChars.size)] }
                )
            }
            initialized.value = true
        }

        @Suppress("UNUSED_EXPRESSION")
        tick.value

        drops.value.forEach { drop ->
            drop.y += drop.speed * 2f
            if (drop.y > size.height + drop.length * CHAR_SIZE) {
                drop.y = (-drop.length * CHAR_SIZE - rng.nextFloat() * 80f)
                drop.x = rng.nextInt(cols) * CHAR_SIZE.toFloat()
                drop.speed = rng.nextInt(DROP_SPEED_MIN, DROP_SPEED_MAX + 1)
                drop.length = rng.nextInt(4, 18)
                drop.chars = CharArray(drop.length) { matrixChars[rng.nextInt(matrixChars.size)] }
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
