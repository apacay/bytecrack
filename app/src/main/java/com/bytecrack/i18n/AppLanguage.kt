package com.bytecrack.i18n

enum class AppLanguage(val code: String, val displayName: String) {
    ES("es", "Español"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: ES
        }

        fun fromSystemLanguage(): AppLanguage {
            val systemLang = java.util.Locale.getDefault().language
            return fromCode(systemLang)
        }
    }
}
