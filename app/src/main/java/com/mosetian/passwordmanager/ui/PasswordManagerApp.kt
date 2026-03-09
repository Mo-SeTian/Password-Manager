package com.mosetian.passwordmanager.ui

import android.app.Activity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.mosetian.passwordmanager.data.local.DatabaseProvider
import com.mosetian.passwordmanager.data.local.PreferencesStore
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.security.AppLockManager
import com.mosetian.passwordmanager.feature.security.AppLockScreen
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import com.mosetian.passwordmanager.feature.vault.VaultScreen
import com.mosetian.passwordmanager.ui.theme.PasswordManagerTheme
import kotlinx.coroutines.launch

@Composable
fun PasswordManagerApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val preferencesStore = remember(context) { PreferencesStore(context) }
    val appLockState by preferencesStore.appLockState.collectAsState(initial = com.mosetian.passwordmanager.feature.security.AppLockState())
    val securitySettings by preferencesStore.securitySettings.collectAsState(initial = SecuritySettings())
    val uiScale by preferencesStore.uiScale.collectAsState(initial = 0.48f)
    val scope = rememberCoroutineScope()
    var unlocked by remember(appLockState.enabled, appLockState.passwordHash, appLockState.passwordSalt) {
        mutableStateOf(!appLockState.enabled || appLockState.passwordHash.isBlank())
    }
    val repository = remember(context, unlocked) {
        VaultRepositoryProvider.createPersistent(context)
    }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !securitySettings.darkModeEnabled
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !securitySettings.darkModeEnabled
        }
    }

    PasswordManagerTheme(darkTheme = securitySettings.darkModeEnabled) {
        Surface {
            if (!unlocked) {
                AppLockScreen(
                    lockEnabled = appLockState.enabled,
                    hasPassword = appLockState.passwordHash.isNotBlank(),
                    onCreatePassword = { password ->
                        scope.launch {
                            val state = AppLockManager.create(password)
                            preferencesStore.updateAppLockState(state)
                            preferencesStore.updateSecuritySettings(securitySettings.copy(appLockEnabled = true))
                            unlocked = true
                        }
                    },
                    onUnlock = { password ->
                        val passed = AppLockManager.verify(password, appLockState)
                        if (passed) unlocked = true
                        passed
                    },
                    onResetAllData = {
                        scope.launch {
                            DatabaseProvider.reset(context)
                            preferencesStore.clearAllPreferences()
                            unlocked = true
                        }
                    }
                )
            } else {
                VaultScreen(
                    repository = repository,
                    initialSecuritySettings = securitySettings,
                    initialUiScale = uiScale,
                    onSecuritySettingsChange = { settings ->
                        scope.launch { preferencesStore.updateSecuritySettings(settings) }
                        if (!settings.appLockEnabled) {
                            scope.launch { preferencesStore.updateAppLockState(com.mosetian.passwordmanager.feature.security.AppLockState()) }
                        }
                    },
                    onUiScaleChange = { scale ->
                        scope.launch { preferencesStore.setUiScale(scale) }
                    },
                    onRequestLockSetup = {
                        unlocked = false
                    },
                    onRequestLockNow = {
                        if (appLockState.enabled) unlocked = false
                    }
                )
            }
        }
    }
}
