package com.fitforge.treinos.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fitforge.treinos.ui.theme.AppPalette
import com.fitforge.treinos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val palette: AppPalette = AppPalette.EMERALD,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

/** Preferências guardadas no aparelho com DataStore (sobrevivem ao fechar o app). */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            palette = AppPalette.fromKey(prefs[PALETTE] ?: AppPalette.EMERALD.key),
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[THEME_MODE] } ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setPalette(palette: AppPalette) {
        dataStore.edit { it[PALETTE] = palette.key }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun isSeeded(): Boolean = dataStore.data.first()[SEEDED] ?: false

    suspend fun markSeeded() {
        dataStore.edit { it[SEEDED] = true }
    }

    private companion object {
        val PALETTE = stringPreferencesKey("palette")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SEEDED = booleanPreferencesKey("seeded_examples")
    }
}
