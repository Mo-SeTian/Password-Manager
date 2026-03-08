package com.mosetian.passwordmanager.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferences by preferencesDataStore(name = "password_manager_prefs")

class PreferencesStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val darkModeEnabled: Flow<Boolean> = context.appPreferences.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.appPreferences.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}
