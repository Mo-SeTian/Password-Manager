package com.mosetian.passwordmanager.feature.security

data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val autoClearClipboardEnabled: Boolean = true,
    val autoClearClipboardSeconds: Int = 30,
    val blockScreenshotsEnabled: Boolean = false,
    val obscureSensitiveContentEnabled: Boolean = false,
    val darkModeEnabled: Boolean = true,
    val autoLockOnBackgroundEnabled: Boolean = true
)
