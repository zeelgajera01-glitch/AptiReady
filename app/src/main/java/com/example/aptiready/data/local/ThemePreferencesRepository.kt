package com.example.aptiready.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aptiready.data.model.ThemeMode
import com.example.aptiready.data.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aptirise_settings")

class ThemePreferencesRepository(private val context: Context) : ThemeRepository {

    private val keyTheme = stringPreferencesKey("app_theme_mode")

    override val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[keyTheme]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[keyTheme] = mode.name
        }
    }
}