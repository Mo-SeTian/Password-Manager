package com.mosetian.passwordmanager.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mosetian.passwordmanager.feature.security.AppLockState
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONObject

private val Context.appPreferences by preferencesDataStore(name = "password_manager_prefs")

class PreferencesStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val biometricUnlockEnabledKey = booleanPreferencesKey("biometric_unlock_enabled")
    private val autoClearClipboardEnabledKey = booleanPreferencesKey("auto_clear_clipboard_enabled")
    private val autoClearClipboardSecondsKey = intPreferencesKey("auto_clear_clipboard_seconds")
    private val blockScreenshotsEnabledKey = booleanPreferencesKey("block_screenshots_enabled")
    private val obscureSensitiveContentEnabledKey = booleanPreferencesKey("obscure_sensitive_content_enabled")
    private val uiScaleKey = floatPreferencesKey("ui_scale")
    private val autoLockOnBackgroundEnabledKey = booleanPreferencesKey("auto_lock_on_background_enabled")
    private val autoLockDelaySecondsKey = intPreferencesKey("auto_lock_delay_seconds")
    private val appLockPasswordHashKey = stringPreferencesKey("app_lock_password_hash")
    private val appLockPasswordSaltKey = stringPreferencesKey("app_lock_password_salt")
    private val appLockPasswordAlgorithmKey = stringPreferencesKey("app_lock_password_algorithm")
    private val plaintextMigrationCompletedKey = booleanPreferencesKey("plaintext_migration_completed")
    private val lastAutofillSelectionKey = stringPreferencesKey("last_autofill_selection")

    val darkModeEnabled: Flow<Boolean> = context.appPreferences.data.map { prefs ->
        prefs[darkModeKey] ?: true
    }

    val securitySettings: Flow<SecuritySettings> = context.appPreferences.data.map { prefs ->
        SecuritySettings(
            appLockEnabled = prefs[appLockEnabledKey] ?: false,
            biometricUnlockEnabled = prefs[biometricUnlockEnabledKey] ?: false,
            autoClearClipboardEnabled = prefs[autoClearClipboardEnabledKey] ?: true,
            autoClearClipboardSeconds = prefs[autoClearClipboardSecondsKey] ?: 30,
            blockScreenshotsEnabled = prefs[blockScreenshotsEnabledKey] ?: false,
            obscureSensitiveContentEnabled = prefs[obscureSensitiveContentEnabledKey] ?: false,
            darkModeEnabled = prefs[darkModeKey] ?: true,
            autoLockOnBackgroundEnabled = prefs[autoLockOnBackgroundEnabledKey] ?: true,
            autoLockDelaySeconds = prefs[autoLockDelaySecondsKey] ?: 0
        )
    }

    val uiScale: Flow<Float> = context.appPreferences.data.map { prefs ->
        prefs[uiScaleKey] ?: 0.48f
    }

    val appLockState: Flow<AppLockState> = context.appPreferences.data.map { prefs ->
        AppLockState(
            enabled = prefs[appLockEnabledKey] ?: false,
            passwordHash = prefs[appLockPasswordHashKey].orEmpty(),
            passwordSalt = prefs[appLockPasswordSaltKey].orEmpty(),
            passwordAlgorithm = prefs[appLockPasswordAlgorithmKey] ?: "sha256"
        )
    }

    val plaintextMigrationCompleted: Flow<Boolean> = context.appPreferences.data.map { prefs ->
        prefs[plaintextMigrationCompletedKey] ?: false
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
            prefs[autoClearClipboardSecondsKey] = settings.autoClearClipboardSeconds
            prefs[blockScreenshotsEnabledKey] = settings.blockScreenshotsEnabled
            prefs[obscureSensitiveContentEnabledKey] = settings.obscureSensitiveContentEnabled
            prefs[darkModeKey] = settings.darkModeEnabled
            prefs[autoLockOnBackgroundEnabledKey] = settings.autoLockOnBackgroundEnabled
            prefs[autoLockDelaySecondsKey] = settings.autoLockDelaySeconds
        }
    }

    suspend fun setUiScale(scale: Float) {
        context.appPreferences.edit { prefs ->
            prefs[uiScaleKey] = scale
        }
    }

    suspend fun updateAppLockState(state: AppLockState) {
        context.appPreferences.edit { prefs ->
            prefs[appLockEnabledKey] = state.enabled
            prefs[appLockPasswordHashKey] = state.passwordHash
            prefs[appLockPasswordSaltKey] = state.passwordSalt
            prefs[appLockPasswordAlgorithmKey] = state.passwordAlgorithm
        }
    }

    suspend fun setPlaintextMigrationCompleted(completed: Boolean) {
        context.appPreferences.edit { prefs ->
            prefs[plaintextMigrationCompletedKey] = completed
        }
    }

    suspend fun clearAllPreferences() {
        context.appPreferences.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun getLastAutofillSelection(key: String): String? {
        val raw = context.appPreferences.data.map { prefs -> prefs[lastAutofillSelectionKey]?.orEmpty() ?: "" }.first()
        if (raw.isBlank()) return null
        return try {
            JSONObject(raw).optString(key).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setLastAutofillSelection(key: String, entryId: String) {
        context.appPreferences.edit { prefs ->
            val raw = prefs[lastAutofillSelectionKey]?.orEmpty() ?: ""
            val json = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
            json.put(key, entryId)
            prefs[lastAutofillSelectionKey] = json.toString()
        }
    }
}
