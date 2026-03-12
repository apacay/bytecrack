package com.bytecrack.i18n

import java.util.Locale

/**
 * Holder del locale elegido para la app.
 * Lo actualiza [LanguageManager.applyLocale] al cambiar idioma;
 * lo lee MainActivity en attachBaseContext y ByteCrackApp en onCreate.
 */
object ByteCrackLocaleHolder {
    var appLocale: Locale? = null
}
