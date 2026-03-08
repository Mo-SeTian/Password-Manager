package com.mosetian.passwordmanager.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferences by preferencesDataStore(name = "password_manager_prefs")

class PreferencesStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val biometricUnlockEnabledKey = booleanPreferencesKey("biometric_unlock_enabled")
    private val autoClearClipboardEnabledKey = booleanPreferencesKey("auto_clear_clipboard_enabled")
    private val blockScreenshotsEnabledKey = booleanPreferencesKey("block_screenshots_enabled")
    private val obscureSensitiveContentEnabledKey = booleanPreferencesKey("obscure_sensitive_content_enabled")
    private val uiScaleKey = floatPreferencesKey("ui_scale")

    val darkModeEnabled: Flow<Boolean> = context.appPreferences.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    val securitySettings: Flow<SecuritySettings> = context.appPreferences.data.map { prefs ->
        SecuritySettings(
            appLockEnabled = prefs[appLockEnabledKey] ?: false,
            biometricUnlockEnabled = prefs[biometricUnlockEnabledKey] ?: false,
            autoClearClipboardEnabled = prefs[autoClearClipboardEnabledKey] ?: true,
            blockScreenshotsEnabled = prefs[blockScreenshotsEnabledKey] ?: false,
            obscureSensitiveContentEnabled = prefs[obscureSensitiveContentEnabledKey] ?: true,
            darkModeEnabled = prefs[darkModeKey] ?: true
        )
    }

    val uiScale: Flow<Float> = context.appPreferences.data.map { prefs ->
        prefs[uiScaleKey] ?: 0.48f
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.appPreferences.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }

    suspend fun updateSecuritySettings(settings: SecuritySettings) {
        context.appPreferences.edit { prefs ->
            prefs[appLockEnabledKey] = settings.appLockEnabled
            prefs[biometricUnlockEnabledKey] = settings.biometricUnlockEnabled
            prefs[autoClearClipboardEnabledKey] = settings.autoClearClipboardEnabled
            prefs[blockScreenshotsEnabledKey] = settings.blockScreenshotsEnabled
            prefs[obscureSensitiveContentEnabledKey] = settings.obscureSensitiveContentEnabled
            prefs[darkModeKey] = settings.darkModeEnabled
        }
    }

    suspend fun setUiScale(scale: Float) {
        context.appPreferences.edit { prefs ->
            prefs[uiScaleKey] = scale
        }
    }
}
