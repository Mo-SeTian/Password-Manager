package com.mosetian.passwordmanager.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.mosetian.passwordmanager.feature.vault.VaultScreen
import com.mosetian.passwordmanager.ui.theme.PasswordManagerTheme

@Composable
fun PasswordManagerApp() {
    PasswordManagerTheme {
        Surface {
            VaultScreen()
        }
    }
}
