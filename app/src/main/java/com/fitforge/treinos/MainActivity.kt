package com.fitforge.treinos

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitforge.treinos.data.files.TextFiles
import com.fitforge.treinos.data.repository.AppSettings
import com.fitforge.treinos.ui.navigation.AppNavGraph
import com.fitforge.treinos.ui.theme.ForjaTreinosTheme
import com.fitforge.treinos.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TreinosApplication).container
        val textFiles = TextFiles(contentResolver)

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Ícones da barra de status acompanham o tema escolhido no app, não só o do sistema.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            ForjaTreinosTheme(palette = settings.palette, themeMode = settings.themeMode) {
                AppNavGraph(container = container, textFiles = textFiles)
            }
        }
    }
}
