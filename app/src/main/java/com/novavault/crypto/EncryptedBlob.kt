package com.novavault.crypto

import kotlinx.serialization.Serializable

/** Serializable envelope for a single AES-256-GCM encrypted value. */
@Serializable
data class EncryptedBlob(
    val version: Int = CryptoConstants.ENVELOPE_VERSION,
    val nonce: ByteArray,          // 12 bytes, base64 when serialized
    val ciphertext: ByteArray,     // includes GCM auth tag
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBlob) return false
        return version == other.version &&
               nonce.contentEquals(other.nonce) &&
               ciphertext.contentEquals(other.ciphertext)
    }
    override fun hashCode(): Int {
        var result = version
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}
