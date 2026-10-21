package com.novavault.domain.usecase.crypto

import com.novavault.crypto.CryptoConstants
import java.security.SecureRandom
import javax.inject.Inject

class RecoveryKeyUseCase @Inject constructor() {
    /** Generate a 32-byte cryptographically random recovery key. */
    fun generateRecoveryKey(): ByteArray {
        val key = ByteArray(CryptoConstants.RECOVERY_KEY_BYTES)
        SecureRandom().nextBytes(key)
        return key
    }

    /**
     * Format as human-readable groups: XXXX-XXXX-XXXX... (hex uppercase).
     * 32 bytes = 64 hex chars = 16 groups of 4.
     */
    fun formatForDisplay(key: ByteArray): String {
        val hex = key.joinToString("") { "%02X".format(it) }
        return hex.chunked(CryptoConstants.RECOVERY_KEY_GROUP_SIZE)
            .joinToString("-")
    }

    /** Parse user-entered formatted key back to bytes. */
    fun parseFromDisplay(formatted: String): ByteArray? {
        return try {
            val hex = formatted.replace("-", "").replace(" ", "").uppercase()
            if (hex.length != CryptoConstants.RECOVERY_KEY_BYTES * 2) return null
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) { null }
    }
}
