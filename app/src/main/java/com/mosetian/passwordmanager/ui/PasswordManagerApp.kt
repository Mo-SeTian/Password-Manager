package com.mosetian.passwordmanager.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.mosetian.passwordmanager.data.local.PreferencesStore
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.security.SecuritySettings
import com.mosetian.passwordmanager.feature.vault.VaultScreen
import com.mosetian.passwordmanager.ui.theme.PasswordManagerTheme
import kotlinx.coroutines.launch

@Composable
fun PasswordManagerApp() {
    val context = LocalContext.current
    val repository = remember(context) {
        VaultRepositoryProvider.createPersistent(context)
    }
    val preferencesStore = remember(context) { PreferencesStore(context) }
    val securitySettings by preferencesStore.securitySettings.collectAsState(initial = SecuritySettings())
    val uiScale by preferencesStore.uiScale.collectAsState(initial = 0.92f)
    val scope = rememberCoroutineScope()

    PasswordManagerTheme {
        Surface {
            VaultScreen(
                repository = repository,
                initialSecuritySettings = securitySettings,
                initialUiScale = uiScale,
                onSecuritySettingsChange = { settings ->
                    scope.launch { preferencesStore.updateSecuritySettings(settings) }
                },
                onUiScaleChange = { scale ->
                    scope.launch { preferencesStore.setUiScale(scale) }
                }
            )
        }
    }
}
