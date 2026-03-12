package com.bytecrack

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bytecrack.i18n.ByteCrackLocaleHolder
import com.bytecrack.ui.ByteCrackApp
import com.bytecrack.ui.theme.ByteCrackTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = ByteCrackLocaleHolder.appLocale ?: Locale.getDefault()
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(android.os.LocaleList(locale))
            }
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByteCrackTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ByteCrackApp()
                }
            }
        }
    }
}
