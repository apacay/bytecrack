package com.bytecrack.i18n

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(name = "language_prefs")

@Singleton
class LanguageManager @Inject constructor(
    private val context: Context
) {
    private val languageKey = stringPreferencesKey("selected_language")

    val currentLanguage: Flow<AppLanguage> = context.languageDataStore.data
        .map { preferences ->
            val savedCode = preferences[languageKey]
            if (savedCode != null) {
                AppLanguage.fromCode(savedCode)
            } else {
                val systemLang = AppLanguage.fromSystemLanguage()
                systemLang
            }
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[languageKey] = language.code
        }
    }

    suspend fun initializeLanguage(): AppLanguage {
        val systemLang = AppLanguage.fromSystemLanguage()
        return systemLang
    }

    /**
     * Lee el idioma guardado de forma síncrona (para usar en Application/attachBaseContext).
     * Si no hay preferencia guardada, devuelve el idioma del sistema.
     */
    fun getLanguageCodeBlocking(): String = runBlocking {
        context.languageDataStore.data.first()[languageKey]
            ?: Locale.getDefault().language
    }

    /**
     * Aplica el idioma a nivel de app (para que la siguiente recreación use ese locale).
     * Debe llamarse tras guardar con setLanguage() cuando el usuario cambia de idioma.
     */
    fun applyLocale(language: AppLanguage) {
        ByteCrackLocaleHolder.appLocale = Locale.forLanguageTag(language.code)
    }

    fun getLocaleForConfiguration(): Locale =
        ByteCrackLocaleHolder.appLocale ?: Locale.forLanguageTag(getLanguageCodeBlocking())
}
