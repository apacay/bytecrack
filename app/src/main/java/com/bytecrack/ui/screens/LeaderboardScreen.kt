package com.bytecrack.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bytecrack.R
import com.bytecrack.ui.components.GlitchText
import com.bytecrack.ui.components.MatrixRain
import com.bytecrack.ui.components.ScanlineOverlay
import com.bytecrack.ui.viewmodel.LeaderboardViewModel

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = stringResource(R.string.leaderboard_delete_title),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.leaderboard_delete_msg),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLocalScores()
                    showClearConfirm = false
                }) {
                    Text(
                        text = stringResource(R.string.btn_delete),
                        color = Color(0xFFFF6600),
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            containerColor = Color.Black,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MatrixRain(density = 0.2f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlitchText(
                text = "LEADERBOARD",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                glitchIntensity = 0.5f
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Text(
                text = stringResource(R.string.leaderboard_local),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.padding(16.dp))

            if (uiState.localScores.isEmpty()) {
                Text(
                    text = stringResource(R.string.leaderboard_empty),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                uiState.localScores.forEachIndexed { index, session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 4.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${session.highScore} pts",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.leaderboard_level, session.bestLevel),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(12.dp))

            if (uiState.localScores.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .clickable { showClearConfirm = true }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_delete_scores),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.padding(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                uiState.error?.let { error ->
                    Text(
                        text = "> $error",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFFF6600),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                activity?.let { act ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                            .clickable {
                                viewModel.showLeaderboard(act)
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.btn_view_global_leaderboard),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .clickable { onBack() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_back_menu),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }

        ScanlineOverlay()
    }
}
