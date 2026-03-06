package com.bytecrack.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HackerKeyboard(
    isHex: Boolean,
    usedDigits: Set<Char>,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val rows = if (isHex) {
        listOf(
            listOf('1', '2', '3', '4'),
            listOf('5', '6', '7', '8'),
            listOf('9', '0', 'A', 'B'),
            listOf('C', 'D', 'E', 'F')
        )
    } else {
        listOf(
            listOf('1', '2', '3', '4', '5'),
            listOf('6', '7', '8', '9', '0')
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { digit ->
                    val isUsed = digit in usedDigits
                    HackerKey(
                        label = digit.toString(),
                        enabled = !isUsed,
                        onClick = { onDigit(digit) }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HackerKey(
                label = "DEL",
                isAction = true,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onDelete
            )
            HackerKey(
                label = "EXEC",
                isAction = true,
                isSubmit = true,
                enabled = submitEnabled,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onSubmit,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

@Composable
fun HackerKey(
    label: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isSubmit: Boolean = false,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderColor = when {
        !enabled -> accentColor.copy(alpha = 0.2f)
        isPressed -> Color.White
        else -> accentColor.copy(alpha = 0.6f)
    }
    val bgColor = when {
        isPressed -> accentColor.copy(alpha = 0.15f)
        else -> Color.Black
    }
    val textColor = when {
        !enabled -> accentColor.copy(alpha = 0.2f)
        isPressed -> Color.White
        else -> accentColor
    }

    Box(
        modifier = modifier
            .width(if (isAction) 70.dp else 48.dp)
            .height(48.dp)
            .border(1.dp, borderColor)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                val hapticType = when {
                    isSubmit && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                        HapticFeedbackConstants.CONFIRM
                    else -> HapticFeedbackConstants.KEYBOARD_TAP
                }
                view.performHapticFeedback(hapticType)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isAction) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isAction) 12.sp else 18.sp,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
