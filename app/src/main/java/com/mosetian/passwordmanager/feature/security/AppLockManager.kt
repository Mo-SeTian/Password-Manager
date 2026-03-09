package com.mosetian.passwordmanager.feature.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object AppLockManager {
    fun create(password: String): AppLockState {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hash(password, salt)
        return AppLockState(
            enabled = true,
            passwordHash = Base64.getEncoder().encodeToString(hash),
            passwordSalt = Base64.getEncoder().encodeToString(salt)
        )
    }

    fun verify(password: String, state: AppLockState): Boolean {
        if (!state.enabled || state.passwordHash.isBlank() || state.passwordSalt.isBlank()) return false
        val salt = Base64.getDecoder().decode(state.passwordSalt)
        val hash = hash(password, salt)
        return Base64.getEncoder().encodeToString(hash) == state.passwordHash
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
