package com.mosetian.passwordmanager.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mosetian.passwordmanager.data.vault.VaultRepositoryProvider
import com.mosetian.passwordmanager.feature.vault.VaultScreen
import com.mosetian.passwordmanager.ui.theme.PasswordManagerTheme

@Composable
fun PasswordManagerApp() {
    val context = LocalContext.current
    val repository = remember(context) {
        VaultRepositoryProvider.createPersistent(context)
    }

    PasswordManagerTheme {
        Surface {
            VaultScreen(repository = repository)
        }
    }
}
