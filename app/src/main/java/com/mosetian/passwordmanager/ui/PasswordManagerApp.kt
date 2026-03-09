package com.mosetian.passwordmanager.ui

import android.app.Activity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mosetian.passwordmanager.data.local.DatabaseProvider
import com.mosetian.passwordmanager.data.local.PreferencesStore
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.security.AppLockManager
import com.mosetian.passwordmanager.feature.security.AppLockScreen
import com.mosetian.passwordmanager.feature.security.AppLockState
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
    val repository = remember(context, unlocked) {
        VaultRepositoryProvider.createPersistent(context)
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

    PasswordManagerTheme(darkTheme = securitySettings.darkModeEnabled) {
        BoxWithConstraints {
            val autoScale = when {
                maxWidth < 360.dp -> 0.92f
                maxWidth > 600.dp -> 1.08f
                else -> 1f
            }
            val responsiveTypography = MaterialTheme.typography.scaled(autoScale)

            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                shapes = MaterialTheme.shapes,
                typography = responsiveTypography
            ) {
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
                                    scope.launch { preferencesStore.updateAppLockState(AppLockState()) }
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
    }
}

private fun Typography.scaled(scale: Float): Typography {
    fun TextStyle.scaleText(): TextStyle = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        letterSpacing = letterSpacing * scale
    )

    return copy(
        displayLarge = displayLarge.scaleText(),
        displayMedium = displayMedium.scaleText(),
        displaySmall = displaySmall.scaleText(),
        headlineLarge = headlineLarge.scaleText(),
        headlineMedium = headlineMedium.scaleText(),
        headlineSmall = headlineSmall.scaleText(),
        titleLarge = titleLarge.scaleText(),
        titleMedium = titleMedium.scaleText(),
        titleSmall = titleSmall.scaleText(),
        bodyLarge = bodyLarge.scaleText(),
        bodyMedium = bodyMedium.scaleText(),
        bodySmall = bodySmall.scaleText(),
        labelLarge = labelLarge.scaleText(),
        labelMedium = labelMedium.scaleText(),
        labelSmall = labelSmall.scaleText()
    )
}
