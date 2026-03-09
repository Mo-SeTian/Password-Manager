package com.mosetian.passwordmanager.feature.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AppLockManager {
    private const val LEGACY_ALGORITHM = "sha256"
    private const val CURRENT_ALGORITHM = "pbkdf2-sha256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH = 256

    fun create(password: String): AppLockState {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPbkdf2(password, salt)
        return AppLockState(
            enabled = true,
            passwordHash = Base64.getEncoder().encodeToString(hash),
            passwordSalt = Base64.getEncoder().encodeToString(salt),
            passwordAlgorithm = CURRENT_ALGORITHM
        )
    }

    fun verify(password: String, state: AppLockState): Boolean {
        if (!state.enabled || state.passwordHash.isBlank() || state.passwordSalt.isBlank()) return false
        val salt = Base64.getDecoder().decode(state.passwordSalt)
        val expected = state.passwordHash
        val actual = when (state.passwordAlgorithm) {
            CURRENT_ALGORITHM -> Base64.getEncoder().encodeToString(hashPbkdf2(password, salt))
            else -> Base64.getEncoder().encodeToString(hashLegacy(password, salt))
        }
        return actual == expected
    }

    fun needsUpgrade(state: AppLockState): Boolean {
        return state.enabled && state.passwordHash.isNotBlank() && state.passwordSalt.isNotBlank() && state.passwordAlgorithm != CURRENT_ALGORITHM
    }

    fun upgradeIfNeeded(password: String, state: AppLockState): AppLockState {
        return if (needsUpgrade(state) && verify(password, state)) create(password) else state
    }

    fun changePassword(currentPassword: String, newPassword: String, state: AppLockState): AppLockState? {
        if (!verify(currentPassword, state)) return null
        return create(newPassword)
    }

    private fun hashPbkdf2(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        return factory.generateSecret(spec).encoded
    }

    private fun hashLegacy(password: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return digest.digest(password.toByteArray(Charsets.UTF_8))
    }
}
