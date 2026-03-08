package com.mosetian.passwordmanager.feature.security

data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val autoClearClipboardEnabled: Boolean = true,
    val blockScreenshotsEnabled: Boolean = false,
    val obscureSensitiveContentEnabled: Boolean = false,
    val darkModeEnabled: Boolean = true
)
