package com.bytecrack.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.bytecrack.R
import com.bytecrack.domain.model.Tier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class TypingDuration { SHORT_06, SHORT_1, MEDIUM_15, MEDIUM_2, LONG_3, FULL }

/**
 * Reproduce efectos de sonido del paquete SCI-FI_UI_SFX_PACK.
 * Correlación con res/raw (archivos del pack):
 * - click_mid_high ← Clicks/Click_Mid-High.wav
 * - click_pitched_down ← Clicks/Click_Pitched_Down.wav
 * - ring_pitched_up ← Rings/Ring_Pitched_Up.wav
 * - glitch_1 ← Glitches/Glitch_1.wav
 * - impact_1_low ← Impacts/Impact_1_Low.wav
 * - air_fx ← FX Sounds/Air_FX.wav
 */
@Singleton
class SoundManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val keyPressId = soundPool.load(context, R.raw.click_mid_high, 1)
    private val deleteId = soundPool.load(context, R.raw.click_pitched_down, 1)
    private val submitId = soundPool.load(context, R.raw.ring_pitched_up, 1)
    private val accessGrantedId = soundPool.load(context, R.raw.ring_pitched_up, 1)
    private val accessDeniedId = soundPool.load(context, R.raw.glitch_1, 1)
    private val gameOverId = soundPool.load(context, R.raw.impact_1_low, 1)
    private val airFxId = soundPool.load(context, R.raw.air_fx, 1)

    private val typingVariants: Map<TypingDuration, IntArray> = mapOf(
        TypingDuration.SHORT_06 to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_06a, 1),
            soundPool.load(context, R.raw.sfx_typing_06b, 1),
            soundPool.load(context, R.raw.sfx_typing_06c, 1),
            soundPool.load(context, R.raw.sfx_kb_05, 1)
        ),
        TypingDuration.SHORT_1 to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_1a, 1),
            soundPool.load(context, R.raw.sfx_typing_1b, 1),
            soundPool.load(context, R.raw.sfx_typing_1c, 1),
            soundPool.load(context, R.raw.sfx_kb_08, 1),
            soundPool.load(context, R.raw.sfx_kb_1a, 1),
            soundPool.load(context, R.raw.sfx_kb_1b, 1),
            soundPool.load(context, R.raw.sfx_kb_1s, 1)
        ),
        TypingDuration.MEDIUM_15 to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_15a, 1),
            soundPool.load(context, R.raw.sfx_typing_15b, 1),
            soundPool.load(context, R.raw.sfx_kb_14a, 1),
            soundPool.load(context, R.raw.sfx_kb_14b, 1),
            soundPool.load(context, R.raw.sfx_kb_14c, 1),
            soundPool.load(context, R.raw.sfx_kb_16, 1)
        ),
        TypingDuration.MEDIUM_2 to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_2a, 1),
            soundPool.load(context, R.raw.sfx_typing_2b, 1),
            soundPool.load(context, R.raw.sfx_typing_2c, 1),
            soundPool.load(context, R.raw.sfx_kb_2, 1),
            soundPool.load(context, R.raw.sfx_kb_2a, 1),
            soundPool.load(context, R.raw.sfx_kb_2b, 1),
            soundPool.load(context, R.raw.sfx_kb_2c, 1)
        ),
        TypingDuration.LONG_3 to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_3a, 1),
            soundPool.load(context, R.raw.sfx_typing_3b, 1),
            soundPool.load(context, R.raw.sfx_kb_3, 1)
        ),
        TypingDuration.FULL to intArrayOf(
            soundPool.load(context, R.raw.sfx_typing_full, 1),
            soundPool.load(context, R.raw.sfx_kb_4, 1)
        )
    )

    private val tierWinVariants: Map<Tier, IntArray> = mapOf(
        Tier.S to intArrayOf(
            soundPool.load(context, R.raw.sfx_tier_s_win_1, 1),
            soundPool.load(context, R.raw.sfx_tier_s_win_2, 1)
        ),
        Tier.A to intArrayOf(
            soundPool.load(context, R.raw.sfx_tier_a_win_1, 1),
            soundPool.load(context, R.raw.sfx_tier_a_win_2, 1)
        ),
        Tier.B to intArrayOf(
            soundPool.load(context, R.raw.sfx_tier_b_win_1, 1),
            soundPool.load(context, R.raw.sfx_tier_b_win_2, 1)
        ),
        Tier.C to intArrayOf(
            soundPool.load(context, R.raw.sfx_tier_c_win_1, 1),
            soundPool.load(context, R.raw.sfx_tier_c_win_2, 1)
        ),
        Tier.D to intArrayOf(
            soundPool.load(context, R.raw.sfx_tier_d_win, 1)
        )
    )

    private val timerOdometerId = soundPool.load(context, R.raw.sfx_timer_odometer, 1)
    private val scoreOdometerId = soundPool.load(context, R.raw.sfx_score_odometer, 1)
    private val scoreSettleId = soundPool.load(context, R.raw.sfx_score_settle, 1)

    private val accessGrantedNewId = soundPool.load(context, R.raw.sfx_access_granted, 1)
    private val penSuccessIds = intArrayOf(
        soundPool.load(context, R.raw.sfx_pen_success_1, 1),
        soundPool.load(context, R.raw.sfx_pen_success_2, 1)
    )
    private val systemOkId = soundPool.load(context, R.raw.sfx_system_ok, 1)
    private val systemErrIds = intArrayOf(
        soundPool.load(context, R.raw.sfx_system_err_1, 1),
        soundPool.load(context, R.raw.sfx_system_err_2, 1)
    )
    private val systemWarnId = soundPool.load(context, R.raw.sfx_system_warn, 1)
    private val enterPressIds = intArrayOf(
        soundPool.load(context, R.raw.sfx_enter_press_1, 1),
        soundPool.load(context, R.raw.sfx_enter_press_2, 1)
    )
    private val sshConnId = soundPool.load(context, R.raw.sfx_ssh_conn, 1)
    private val difficultySelId = soundPool.load(context, R.raw.sfx_difficulty_sel, 1)
    private val timerLowId = soundPool.load(context, R.raw.sfx_timer_low, 1)
    private val traceUseId = soundPool.load(context, R.raw.sfx_trace_use, 1)

    var isSoundEnabled = true

    /** Pulsación de dígito: Click_Mid-High del pack. */
    fun playKeyPress() {
        if (!isSoundEnabled) return
        soundPool.play(keyPressId, 0.6f, 0.6f, 1, 0, 1f)
    }

    /** Borrado: Click_Pitched_Down del pack. */
    fun playDelete() {
        if (!isSoundEnabled) return
        soundPool.play(deleteId, 0.5f, 0.5f, 1, 0, 1f)
    }

    /** Submit (EXEC): Ring_Pitched_Up del pack. */
    fun playSubmit() {
        if (!isSoundEnabled) return
        soundPool.play(submitId, 0.6f, 0.6f, 1, 0, 1f)
    }

    /** ACCESS GRANTED: sonido generado con AudioCraft. */
    fun playAccessGranted() {
        if (!isSoundEnabled) return
        soundPool.play(accessGrantedNewId, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** Intento incorrecto: Glitch_1 del pack. */
    fun playAccessDenied() {
        if (!isSoundEnabled) return
        soundPool.play(accessDeniedId, 0.6f, 0.6f, 1, 0, 1f)
    }

    /** GAME OVER: Impact_1_Low del pack. */
    fun playGameOver() {
        if (!isSoundEnabled) return
        soundPool.play(gameOverId, 0.7f, 0.7f, 1, 0, 1f)
    }

    /** Efecto Air FX para transiciones (opcional). */
    fun playAirFx() {
        if (!isSoundEnabled) return
        soundPool.play(airFxId, 0.5f, 0.5f, 1, 0, 1f)
    }

    /** Sonido de teclado mecánico para animaciones de tipeo en transiciones. */
    fun playTyping(duration: TypingDuration) {
        if (!isSoundEnabled) return
        val ids = typingVariants[duration] ?: return
        soundPool.play(ids.random(), 0.35f, 0.35f, 1, 0, 1f)
    }

    fun playTierWin(tier: Tier) {
        if (!isSoundEnabled) return
        val ids = tierWinVariants[tier] ?: return
        soundPool.play(ids.random(), 0.85f, 0.85f, 1, 0, 1f)
    }

    fun playTimerOdometer() {
        if (!isSoundEnabled) return
        soundPool.play(timerOdometerId, 0.55f, 0.55f, 1, 0, 1f)
    }

    fun playScoreOdometer() {
        if (!isSoundEnabled) return
        soundPool.play(scoreOdometerId, 0.55f, 0.55f, 1, 0, 1f)
    }

    fun playScoreSettle() {
        if (!isSoundEnabled) return
        soundPool.play(scoreSettleId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun playPenetrationSuccess() {
        if (!isSoundEnabled) return
        soundPool.play(penSuccessIds.random(), 0.8f, 0.8f, 1, 0, 1f)
    }

    fun playSystemOk() {
        if (!isSoundEnabled) return
        soundPool.play(systemOkId, 0.4f, 0.4f, 1, 0, 1f)
    }

    fun playSystemErr() {
        if (!isSoundEnabled) return
        soundPool.play(systemErrIds.random(), 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playSystemWarn() {
        if (!isSoundEnabled) return
        soundPool.play(systemWarnId, 0.45f, 0.45f, 1, 0, 1f)
    }

    fun playEnterPress() {
        if (!isSoundEnabled) return
        soundPool.play(enterPressIds.random(), 0.4f, 0.4f, 1, 0, 1f)
    }

    fun playSshConnect() {
        if (!isSoundEnabled) return
        soundPool.play(sshConnId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playDifficultySelect() {
        if (!isSoundEnabled) return
        soundPool.play(difficultySelId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun playTimerLow() {
        if (!isSoundEnabled) return
        soundPool.play(timerLowId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun playTraceUse() {
        if (!isSoundEnabled) return
        soundPool.play(traceUseId, 0.55f, 0.55f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
