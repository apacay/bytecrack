package com.bytecrack.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val highScore: Int,
    val bestLevel: Int,
    val timestamp: Long = 0
)
