package com.bytecrack.di

import android.content.Context
import androidx.room.Room
import com.bytecrack.data.local.AppDatabase
import com.bytecrack.data.local.GameSessionDao
import com.bytecrack.ads.AdManager
import com.bytecrack.data.remote.PlayGamesRepository
import com.bytecrack.domain.CodeGenerator
import com.bytecrack.domain.GuessEvaluator
import com.bytecrack.domain.TierCalculator
import com.bytecrack.i18n.LanguageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bytecrack_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideGameSessionDao(db: AppDatabase): GameSessionDao = db.gameSessionDao()

    @Provides
    @Singleton
    fun provideCodeGenerator(): CodeGenerator = CodeGenerator()

    @Provides
    @Singleton
    fun provideGuessEvaluator(): GuessEvaluator = GuessEvaluator()

    @Provides
    @Singleton
    fun provideTierCalculator(): TierCalculator = TierCalculator()

    @Provides
    @Singleton
    fun providePlayGamesRepository(): PlayGamesRepository = PlayGamesRepository()

    @Provides
    @Singleton
    fun provideAdManager(): AdManager = AdManager()

    @Provides
    @Singleton
    fun provideLanguageManager(@ApplicationContext context: Context): LanguageManager =
        LanguageManager(context)
}
