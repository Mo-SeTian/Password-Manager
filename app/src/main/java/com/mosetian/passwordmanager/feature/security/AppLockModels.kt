package com.mosetian.passwordmanager.feature.security

data class AppLockState(
    val enabled: Boolean = false,
    val passwordHash: String = "",
    val passwordSalt: String = ""
)
