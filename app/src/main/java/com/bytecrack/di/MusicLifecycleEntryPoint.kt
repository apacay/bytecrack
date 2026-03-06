package com.bytecrack.di

import com.bytecrack.audio.MusicManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MusicLifecycleEntryPoint {
    fun musicManager(): MusicManager
}
