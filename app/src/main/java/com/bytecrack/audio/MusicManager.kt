package com.bytecrack.audio

import android.content.Context
import android.media.MediaPlayer
import com.bytecrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class MusicTrack(
    val resId: Int
) {
    VOYAGER_1(R.raw.music_voyager_1),       // Menú + nivel 1
    THE_DEAD(R.raw.music_the_dead),        // Niveles 4, 7, 10... (3k+1)
    TWILIGHT_VOYAGE(R.raw.music_twilight_voyage),  // Niveles 2, 5, 8... (3k+2)
    BIOHAZARD(R.raw.music_biohazard);       // Niveles 3, 6, 9... (3k)

    companion object {
        /** Menú y nivel 1: Voyager 1. Luego por nivel: 3k+1→The Dead, 3k+2→Twilight, 3k→Biohazard. */
        fun forLevel(level: Int): MusicTrack = when {
            level <= 1 -> VOYAGER_1
            level % 3 == 1 -> THE_DEAD
            level % 3 == 2 -> TWILIGHT_VOYAGE
            else -> BIOHAZARD
        }
    }
}

@Singleton
class MusicManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private companion object {
        const val MUSIC_VOLUME = 0.5f
        const val FADE_STEP_MS = 50L
    }

    var isMusicEnabled: Boolean = true
        private set

    private var player: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null
    private var fadeJob: Job? = null
    private var isPaused: Boolean = false
    private var currentVolume: Float = MUSIC_VOLUME
    private var isDimmed: Boolean = false

    fun playForLevel(level: Int) = play(MusicTrack.forLevel(level))

    /** Pausa la música (p. ej. durante anuncios). Usar resume() al cerrar el anuncio. */
    fun pause() {
        isPaused = false
        player?.takeIf { it.isPlaying }?.let {
            it.pause()
            isPaused = true
        }
    }

    /** Reanuda la música tras pausar (p. ej. al cerrar un anuncio). */
    fun resume() {
        if (isPaused && player != null) {
            player?.start()
            isPaused = false
        }
    }

    fun play(track: MusicTrack) {
        if (currentTrack == track && player?.isPlaying == true) {
            if (isDimmed) restoreVolume()
            return
        }

        currentTrack = track

        if (!isMusicEnabled) return

        stopInternal()
        isDimmed = false
        currentVolume = MUSIC_VOLUME

        player = MediaPlayer.create(context, track.resId).apply {
            isLooping = true
            setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            start()
        }
    }

    fun stop() {
        currentTrack = null
        stopInternal()
    }

    private fun stopInternal() {
        fadeJob?.cancel()
        isPaused = false
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    fun toggle(): Boolean {
        isMusicEnabled = !isMusicEnabled
        if (isMusicEnabled) {
            currentTrack?.let { play(it) }
        } else {
            stopInternal()
        }
        return isMusicEnabled
    }

    /** Baja el volumen a 0 sin detener el player (para apreciar SFX de victoria). */
    fun fadeToSilence(durationMs: Long = 800L) {
        if (!isMusicEnabled) return
        fadeJob?.cancel()
        isDimmed = true
        fadeJob = scope.launch {
            val p = player ?: return@launch
            if (!p.isPlaying) return@launch
            val steps = (durationMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
            val decrement = currentVolume / steps
            repeat(steps) {
                currentVolume = (currentVolume - decrement).coerceAtLeast(0f)
                p.setVolume(currentVolume, currentVolume)
                delay(FADE_STEP_MS)
            }
            currentVolume = 0f
            p.setVolume(0f, 0f)
        }
    }

    /** Restaura el volumen tras un fadeToSilence. */
    fun restoreVolume(durationMs: Long = 600L) {
        if (!isMusicEnabled || !isDimmed) return
        fadeJob?.cancel()
        isDimmed = false
        fadeJob = scope.launch {
            val p = player ?: return@launch
            val steps = (durationMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
            val increment = (MUSIC_VOLUME - currentVolume) / steps
            repeat(steps) {
                currentVolume = (currentVolume + increment).coerceAtMost(MUSIC_VOLUME)
                p.setVolume(currentVolume, currentVolume)
                delay(FADE_STEP_MS)
            }
            currentVolume = MUSIC_VOLUME
            p.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
        }
    }

    fun fadeOut() {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val currentPlayer = player ?: return@launch
            if (!currentPlayer.isPlaying) return@launch

            var volume = MUSIC_VOLUME
            while (volume > 0) {
                volume -= 0.05f
                if (volume < 0) volume = 0f
                currentPlayer.setVolume(volume, volume)
                delay(100)
            }
            stopInternal()
        }
    }
}
