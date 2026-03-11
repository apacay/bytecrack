package com.bytecrack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bytecrack.R
import com.bytecrack.i18n.AppLanguage
import com.bytecrack.ui.components.GlitchText
import com.bytecrack.ui.components.MatrixRain
import com.bytecrack.ui.components.ScanlineOverlay
import com.bytecrack.ui.components.TerminalText

@Composable
fun MainMenuScreen(
    highScore: Int?,
    isMusicEnabled: Boolean,
    currentLanguage: AppLanguage = AppLanguage.ES,
    onStartGame: () -> Unit,
    onToggleMusic: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    onShowLanguagePopup: () -> Unit = {}
) {
    val showMenu = remember { mutableStateOf(false) }
    val showCredits = remember { mutableStateOf(false) }
    val showHowToPlay = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MatrixRain(density = 0.3f)

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .clickable { onShowLanguagePopup() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentLanguage.code.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .clickable { onToggleMusic() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (isMusicEnabled) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMusicEnabled) stringResource(R.string.cd_music_off) else stringResource(R.string.cd_music_on),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlitchText(
                text = "BYTECRACK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                glitchIntensity = 0.8f
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.menu_version),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TerminalText(
                fullText = "> SYSTEM READY...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                charDelayMs = 50L,
                showCursor = false,
                onComplete = { showMenu.value = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (showMenu.value) {
                if (highScore != null && highScore > 0) {
                    Text(
                        text = "HIGH SCORE: $highScore pts",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                HackerMenuButton(
                    label = stringResource(R.string.menu_new_session),
                    onClick = onStartGame
                )

                Spacer(modifier = Modifier.height(12.dp))

                HackerMenuButton(
                    label = "[ LEADERBOARD ]",
                    onClick = onShowLeaderboard
                )

                Spacer(modifier = Modifier.height(12.dp))

                HackerMenuButton(
                    label = stringResource(R.string.menu_how_to_play),
                    onClick = { showHowToPlay.value = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "[ CREDITS ]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.clickable { showCredits.value = true }
                )
            }
        }

        ScanlineOverlay()

        AnimatedVisibility(
            visible = showCredits.value,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            CreditsOverlay(onClose = { showCredits.value = false })
        }

        AnimatedVisibility(
            visible = showHowToPlay.value,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            HowToPlayOverlay(onClose = { showHowToPlay.value = false })
        }
    }
}

@Composable
fun LanguagePopup(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val green = Color(0xFF00FF41)
    val cyan = Color(0xFF00BFFF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, green.copy(alpha = 0.4f))
                .background(Color.Black)
                .padding(24.dp)
                .clickable(enabled = false) { },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.language_popup_title),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = green
            )

            Spacer(modifier = Modifier.height(20.dp))

            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == currentLanguage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSelected) green else green.copy(alpha = 0.3f))
                        .background(if (isSelected) green.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { onLanguageSelected(lang) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${lang.code.uppercase()}] ${lang.displayName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = if (isSelected) green else green.copy(alpha = 0.7f)
                    )
                    if (isSelected) {
                        Text(
                            text = stringResource(R.string.language_active),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = cyan
                        )
                    }
                }
                if (lang != AppLanguage.entries.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .border(1.dp, green.copy(alpha = 0.3f))
                    .background(green.copy(alpha = 0.05f))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_close),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = green.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun HowToPlayOverlay(onClose: () -> Unit) {
    val currentSheet = remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                .background(Color.Black)
                .padding(28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.how_to_play_title) + " (${currentSheet.value}/2)",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (currentSheet.value == 1) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.how_to_play_objective),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_cracked_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_cracked_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_found_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_found_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_hints_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_hints_1),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.how_to_play_hints_2),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.how_to_play_hints_3),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_exec_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_exec_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.how_to_play_page2_intro),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_hard_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_hard_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HowToPlaySection(title = stringResource(R.string.how_to_play_ironman_title)) {
                        Text(
                            text = stringResource(R.string.how_to_play_ironman_desc),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.how_to_play_difficulty_choice),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSheet.value == 2) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable { currentSheet.value = 1 }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (currentSheet.value == 1) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            .clickable { currentSheet.value = 2 }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_next),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .clickable { onClose() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_close),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HowToPlaySection(title: String, content: @Composable () -> Unit) {
    Text(
        text = "── $title",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
    )
    Spacer(modifier = Modifier.height(6.dp))
    content()
}

@Composable
private fun CreditsOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                .background(Color.Black)
                .padding(28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.credits_title),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            CreditSection(title = "MUSIC") {
                CreditEntry(label = "Voyager 1", value = "John Tasoulas")
                CreditEntry(label = "Album", value = "Free Synthwave Music (For Videos)")

                Spacer(modifier = Modifier.height(8.dp))

                CreditEntry(label = "The Dead", value = "John Tasoulas")
                CreditEntry(label = "Album", value = "Dark Suspense & Synthwave (Music For Videos)")

                Spacer(modifier = Modifier.height(8.dp))

                CreditEntry(label = "TWILIGHT VOYAGE", value = "Ghostrifter Official")
                CreditEntry(label = "Album", value = "Retrowave (Free Music)")

                Spacer(modifier = Modifier.height(8.dp))

                CreditEntry(label = "Biohazard", value = "Lesion X")
                CreditEntry(label = "Album", value = "Sci Fi Music [Copyright Free Music]")
            }

            Spacer(modifier = Modifier.height(20.dp))

            CreditSection(title = "SFX") {
                CreditEntry(
                    label = "Pack",
                    value = "SCI-FI UI SFX Pack"
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.btn_tap_close),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun CreditSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = "── $title",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
    )
    Spacer(modifier = Modifier.height(8.dp))
    content()
}

@Composable
private fun CreditEntry(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun HackerMenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menuBtn")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth(0.7f)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )
    }
}
