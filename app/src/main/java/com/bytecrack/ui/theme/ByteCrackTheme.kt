package com.bytecrack.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val HackerDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF41),
    onPrimary = Color.Black,
    secondary = Color(0xFF00FFFF),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color(0xFF00FF41),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFF00FF41),
    error = Color(0xFFFF0000),
    onError = Color.Black
)

@Composable
fun ByteCrackTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = HackerDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HackerTypography,
        content = content
    )
}
