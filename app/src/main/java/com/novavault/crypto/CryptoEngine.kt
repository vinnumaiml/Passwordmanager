package com.novavault.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core cryptographic operations for NOVA VAULT.
 *
 * Responsibilities:
 *  - DEK / salt / nonce generation via SecureRandom
 *  - Key Encryption Key (KEK) derivation from master password using Argon2id (BouncyCastle)
 *  - AES-256-GCM encryption / decryption of DEK and vault items
 *  - Secure memory wipe helpers for sensitive byte/char arrays
 *
 * Threading: All methods are stateless and thread-safe; callers should invoke
 * long-running operations (e.g. [deriveKek]) on a background dispatcher.
 */
@Singleton
class CryptoEngine @Inject constructor() {

    // ──────────────────────────────────────────────────────────────────────────
    // Key / nonce generation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generate a fresh 256-bit Data Encryption Key (DEK).
     * Caller is responsible for zeroing this array after use.
     */
    fun generateDek(): ByteArray {
        val dek = ByteArray(CryptoConstants.AES_KEY_BYTES)
        SecureRandom().nextBytes(dek)
        return dek
    }

    /**
     * Generate a 32-byte cryptographically random salt for Argon2id.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(CryptoConstants.ARGON2_SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Generate a 12-byte nonce (IV) for AES-GCM.
     * Must never be reused with the same key.
     */
    fun generateNonce(): ByteArray {
        val nonce = ByteArray(CryptoConstants.GCM_NONCE_BYTES)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Key derivation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Derive a 256-bit Key Encryption Key (KEK) from [masterPassword] using Argon2id.
     *
     * The derived key is used solely to wrap/unwrap the DEK; the raw master password
     * is never persisted or transmitted.
     *
     * @param masterPassword caller must zero this array after use
     * @param salt           32-byte random salt stored alongside the vault envelope
     * @param params         KDF tuning parameters (memory, iterations, parallelism)
     * @return 32-byte KEK; caller must zero after use
     */
    fun deriveKek(
        masterPassword: CharArray,
        salt: ByteArray,
        params: KdfParams,
    ): ByteArray {
        val passwordBytes = charArrayToUtf8(masterPassword)
        return try {
            val argon2Params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(params.memoryKib)
                .withIterations(params.iterations)
                .withParallelism(params.parallelism)
                .build()

            val generator = Argon2BytesGenerator()
            generator.init(argon2Params)

            val kek = ByteArray(params.hashLen)
            generator.generateBytes(passwordBytes, kek, 0, kek.size)
            kek
        } finally {
            passwordBytes.fill(0)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AES-256-GCM with KEK
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Encrypt [plaintext] with [kek] using AES-256-GCM.
     * A fresh nonce is generated internally; it is stored in the returned [EncryptedBlob].
     */
    fun encryptWithKek(plaintext: ByteArray, kek: ByteArray): EncryptedBlob {
        require(kek.size == CryptoConstants.AES_KEY_BYTES) {
            "KEK must be ${CryptoConstants.AES_KEY_BYTES} bytes, got ${kek.size}"
        }
        return aesGcmEncrypt(plaintext, kek)
    }

    /**
     * Decrypt an [EncryptedBlob] that was produced by [encryptWithKek].
     *
     * @throws javax.crypto.AEADBadTagException if authentication fails (wrong key or tampered data)
     */
    fun decryptWithKek(blob: EncryptedBlob, kek: ByteArray): ByteArray {
        require(kek.size == CryptoConstants.AES_KEY_BYTES) {
            "KEK must be ${CryptoConstants.AES_KEY_BYTES} bytes, got ${kek.size}"
        }
        return aesGcmDecrypt(blob, kek)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AES-256-GCM with DEK
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Encrypt [plaintext] with [dek] using AES-256-GCM.
     * A fresh nonce is generated for every call.
     */
    fun encryptWithDek(plaintext: ByteArray, dek: ByteArray): EncryptedBlob {
        require(dek.size == CryptoConstants.AES_KEY_BYTES) {
            "DEK must be ${CryptoConstants.AES_KEY_BYTES} bytes, got ${dek.size}"
        }
        return aesGcmEncrypt(plaintext, dek)
    }

    /**
     * Decrypt an [EncryptedBlob] that was produced by [encryptWithDek].
     *
     * @throws javax.crypto.AEADBadTagException if authentication fails
     */
    fun decryptWithDek(blob: EncryptedBlob, dek: ByteArray): ByteArray {
        require(dek.size == CryptoConstants.AES_KEY_BYTES) {
            "DEK must be ${CryptoConstants.AES_KEY_BYTES} bytes, got ${dek.size}"
        }
        return aesGcmDecrypt(blob, dek)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Memory wipe helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Overwrite [key] with zeros. Call immediately after the key is no longer needed.
     */
    fun wipeKey(key: ByteArray) {
        key.fill(0)
    }

    /**
     * Overwrite [password] with null chars. Call immediately after derivation.
     */
    fun wipePassword(password: CharArray) {
        password.fill('\u0000')
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun aesGcmEncrypt(plaintext: ByteArray, key: ByteArray): EncryptedBlob {
        val nonce = generateNonce()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedBlob(
            version = CryptoConstants.ENVELOPE_VERSION,
            nonce = nonce,
            ciphertext = ciphertext,
        )
    }

    private fun aesGcmDecrypt(blob: EncryptedBlob, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, blob.nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(blob.ciphertext)
    }

    /**
     * Convert a [CharArray] to a UTF-8 [ByteArray] without going through [String],
     * keeping sensitive data off the JVM string pool.
     */
    private fun charArrayToUtf8(chars: CharArray): ByteArray {
        val charBuffer = java.nio.CharBuffer.wrap(chars)
        val byteBuffer = java.nio.charset.Charset.forName("UTF-8").encode(charBuffer)
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        // Zero backing array of the ByteBuffer to avoid lingering copies
        if (byteBuffer.hasArray()) byteBuffer.array().fill(0)
        return bytes
    }
}
