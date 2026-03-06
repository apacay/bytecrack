package com.bytecrack.data.remote

import android.app.Activity
import javax.inject.Inject

/**
 * Repositorio para Google Play Games Services v2.
 *
 * Para activar: configurar game_services_project_id en strings.xml,
 * crear leaderboard en Play Console, y descomentar la implementación real.
 */
class PlayGamesRepository @Inject constructor() {

    companion object {
        const val LEADERBOARD_HIGH_SCORE = "CgkI" // Reemplazar con ID real de Play Console
    }

    suspend fun signIn(activity: Activity): Boolean = false

    suspend fun isAuthenticated(activity: Activity): Boolean = false

    suspend fun submitScore(activity: Activity, score: Long): Boolean = false

    suspend fun showLeaderboard(activity: Activity): Boolean = false
}
