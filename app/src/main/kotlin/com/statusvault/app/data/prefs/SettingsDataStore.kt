package com.statusvault.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.statusvault.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statusvault_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val isDarkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] == true
    }

    val isSystemTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SYSTEM_THEME] != false
    }

    val isAutoSave: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SAVE] == true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = enabled
            if (enabled) prefs[KEY_SYSTEM_THEME] = false
        }
    }

    suspend fun setSystemTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SYSTEM_THEME] = enabled
        }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_SAVE] = enabled
        }
    }

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey(Constants.PREF_DARK_MODE)
        private val KEY_AUTO_SAVE = booleanPreferencesKey(Constants.PREF_AUTO_SAVE)
        private val KEY_SYSTEM_THEME = booleanPreferencesKey(Constants.PREF_SYSTEM_THEME)
    }
}
