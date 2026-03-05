package com.bytecrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bytecrack.data.local.entities.GameSessionEntity

@Dao
interface GameSessionDao {

    @Query("SELECT * FROM game_sessions ORDER BY highScore DESC LIMIT 1")
    suspend fun getBestSession(): GameSessionEntity?

    @Query("SELECT MAX(highScore) FROM game_sessions")
    suspend fun getHighScore(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: GameSessionEntity)
}
