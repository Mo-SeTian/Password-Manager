package com.mosetian.passwordmanager.ui

import android.app.Activity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mosetian.passwordmanager.data.local.DatabaseProvider
import com.mosetian.passwordmanager.data.local.PreferencesStore
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.security.AppLockManager
import com.mosetian.passwordmanager.feature.security.AppLockScreen
import com.mosetian.passwordmanager.feature.security.AppLockState
import com.mosetian.passwordmanager.feature.security.ChangeAppLockPasswordDialog
import com.mosetian.passwordmanager.feature.security.DisableAppLockDialog
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import com.mosetian.passwordmanager.feature.vault.VaultScreen
import com.mosetian.passwordmanager.ui.theme.PasswordManagerTheme
import kotlinx.coroutines.launch

@Composable
fun PasswordManagerApp() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val preferencesStore = remember(context) { PreferencesStore(context) }
    val appLockState by preferencesStore.appLockState.collectAsState(initial = AppLockState())
    val securitySettings by preferencesStore.securitySettings.collectAsState(initial = SecuritySettings())
    val uiScale by preferencesStore.uiScale.collectAsState(initial = 0.48f)
    val scope = rememberCoroutineScope()
    var unlocked by remember(appLockState.enabled, appLockState.passwordHash, appLockState.passwordSalt) {
        mutableStateOf(!appLockState.enabled || appLockState.passwordHash.isBlank())
    }
    var forcingLockSetup by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDisableAppLockDialog by remember { mutableStateOf(false) }
    val repository = remember(context, unlocked) {
        VaultRepositoryProvider.createPersistent(context)
    }

    LaunchedEffect(repository, unlocked) {
        if (unlocked) {
            repository.migratePlaintextDataIfNeeded()
        }
    }

    DisposableEffect(lifecycleOwner, appLockState.enabled, securitySettings.autoLockOnBackgroundEnabled, unlocked) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_STOP &&
                appLockState.enabled &&
                securitySettings.autoLockOnBackgroundEnabled &&
                unlocked
            ) {
                unlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !securitySettings.darkModeEnabled
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !securitySettings.darkModeEnabled
        }
    }

    val shouldShowLockScreen = forcingLockSetup || !unlocked
    val shouldCreatePassword = forcingLockSetup || appLockState.passwordHash.isBlank()

    PasswordManagerTheme(darkTheme = securitySettings.darkModeEnabled) {
        Surface {
            if (shouldShowLockScreen) {
                AppLockScreen(
                    lockEnabled = appLockState.enabled && !shouldCreatePassword,
                    hasPassword = !shouldCreatePassword,
                    onCreatePassword = { password ->
                        scope.launch {
                            val state = AppLockManager.create(password)
                            preferencesStore.updateAppLockState(state)
                            preferencesStore.updateSecuritySettings(securitySettings.copy(appLockEnabled = true))
                            forcingLockSetup = false
                            unlocked = true
                        }
                    },
                    onUnlock = { password ->
                        val passed = AppLockManager.verify(password, appLockState)
                        if (passed) {
                            scope.launch {
                                val upgradedState = AppLockManager.upgradeIfNeeded(password, appLockState)
                                if (upgradedState != appLockState) {
                                    preferencesStore.updateAppLockState(upgradedState)
                                }
                            }
                            forcingLockSetup = false
                            unlocked = true
                        }
                        passed
                    },
                    onResetAllData = {
                        scope.launch {
                            DatabaseProvider.reset(context)
                            preferencesStore.clearAllPreferences()
                            forcingLockSetup = false
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
                            forcingLockSetup = false
                            scope.launch { preferencesStore.updateAppLockState(AppLockState()) }
                        }
                    },
                    onUiScaleChange = { scale ->
                        scope.launch { preferencesStore.setUiScale(scale) }
                    },
                    onRequestLockSetup = {
                        forcingLockSetup = true
                        unlocked = false
                    },
                    onRequestLockNow = {
                        if (appLockState.enabled) {
                            forcingLockSetup = false
                            unlocked = false
                        }
                    },
                    onRequestChangePassword = {
                        showChangePasswordDialog = true
                    },
                    onRequestDisableAppLock = {
                        showDisableAppLockDialog = true
                    }
                )
            }

            if (showChangePasswordDialog) {
                ChangeAppLockPasswordDialog(
                    onDismiss = { showChangePasswordDialog = false },
                    onSubmit = { currentPassword, newPassword ->
                        val updatedState = AppLockManager.changePassword(currentPassword, newPassword, appLockState)
                        if (updatedState == null) {
                            "当前主密码错误"
                        } else {
                            scope.launch { preferencesStore.updateAppLockState(updatedState) }
                            showChangePasswordDialog = false
                            null
                        }
                    }
                )
            }

            if (showDisableAppLockDialog) {
                DisableAppLockDialog(
                    onDismiss = { showDisableAppLockDialog = false },
                    onSubmit = { currentPassword ->
                        if (!AppLockManager.verify(currentPassword, appLockState)) {
                            "当前主密码错误"
                        } else {
                            showDisableAppLockDialog = false
                            scope.launch {
                                preferencesStore.updateSecuritySettings(securitySettings.copy(appLockEnabled = false))
                                preferencesStore.updateAppLockState(AppLockState())
                            }
                            null
                        }
                    }
                )
            }
        }
    }
}
