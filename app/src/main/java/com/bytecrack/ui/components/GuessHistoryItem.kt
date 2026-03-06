package com.bytecrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecrack.domain.model.Guess

@Composable
fun GuessHistoryItem(
    guess: Guess,
    attemptNumber: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row {
            Text(
                text = "> BREACH ${String.format("%02d", attemptNumber)}:",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFF00FF41).copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = guess.digits.joinToString(" "),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF41)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "CRACKED:${guess.correctPosition}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (guess.correctPosition > 0)
                    Color(0xFF00FF41)
                else
                    Color(0xFF00FF41).copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "FOUND:${guess.correctWrongPosition}",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (guess.correctWrongPosition > 0)
                    Color(0xFF00BFFF)
                else
                    Color(0xFF00BFFF).copy(alpha = 0.35f)
            )
        }
    }
}
