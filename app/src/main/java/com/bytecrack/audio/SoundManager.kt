package com.bytecrack.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.bytecrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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
        .setMaxStreams(6)
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

    /** ACCESS GRANTED: Ring_Pitched_Up del pack. */
    fun playAccessGranted() {
        if (!isSoundEnabled) return
        soundPool.play(accessGrantedId, 0.7f, 0.7f, 1, 0, 1f)
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

    fun release() {
        soundPool.release()
    }
}
