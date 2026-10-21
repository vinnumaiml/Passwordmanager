package com.novavault.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android Keystore-backed AES-256 keys used to wrap/unwrap the DEK
 * via biometric or device-credential authentication.
 *
 * Key characteristics:
 *  - AES/GCM/NoPadding, 256-bit
 *  - Requires BIOMETRIC_STRONG or device credential (PIN/pattern/password)
 *  - invalidatedByBiometricEnrollment = true for security on biometric changes
 *  - Auth timeout = 0 (per-use; fresh auth required for every crypto operation)
 */
@Singleton
class KeystoreManager @Inject constructor() {

    // ──────────────────────────────────────────────────────────────────────────
    // Key lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Retrieve the Keystore-backed [SecretKey] if it exists, or generate a new one.
     *
     * @throws KeyPermanentlyInvalidatedException if biometric enrollment changed and the
     *         existing key is invalidated — callers should catch this, call [deleteKey],
     *         and re-create the key, then prompt the user to re-enable biometric unlock.
     */
    fun getOrCreateBiometricKey(): SecretKey {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(CryptoConstants.KEYSTORE_ALIAS_BIOMETRIC)) {
            val key = keyStore.getKey(
                CryptoConstants.KEYSTORE_ALIAS_BIOMETRIC,
                null,
            ) as? SecretKey
            if (key != null) return key
        }
        return generateBiometricKey()
    }

    /**
     * Delete the Keystore-backed key. Should be called on logout or when the user
     * disables biometric unlock.
     */
    fun deleteKey() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(CryptoConstants.KEYSTORE_ALIAS_BIOMETRIC)) {
            keyStore.deleteEntry(CryptoConstants.KEYSTORE_ALIAS_BIOMETRIC)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DEK wrapping / unwrapping
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Encrypt [dek] with the Keystore-backed key using the [Cipher] inside [cryptoObject].
     * The [cryptoObject] must have been obtained from [getCipherForEncrypt] and then
     * authenticated via [BiometricPrompt].
     *
     * @return ciphertext including GCM auth tag
     */
    fun wrapDek(dek: ByteArray, cryptoObject: BiometricPrompt.CryptoObject): ByteArray {
        val cipher = requireNotNull(cryptoObject.cipher) {
            "CryptoObject does not contain a Cipher"
        }
        return cipher.doFinal(dek)
    }

    /**
     * Decrypt [wrappedDek] with the Keystore-backed key using the [Cipher] inside [cryptoObject].
     * The [cryptoObject] must have been obtained from [getCipherForDecrypt] and then authenticated.
     *
     * @return plaintext DEK; caller must zero after use
     */
    fun unwrapDek(wrappedDek: ByteArray, cryptoObject: BiometricPrompt.CryptoObject): ByteArray {
        val cipher = requireNotNull(cryptoObject.cipher) {
            "CryptoObject does not contain a Cipher"
        }
        return cipher.doFinal(wrappedDek)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cipher factories (pass to BiometricPrompt)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Return an AES/GCM/NoPadding [Cipher] initialized for encryption with the
     * Keystore-backed key. Pass this cipher wrapped in [BiometricPrompt.CryptoObject]
     * to [BiometricPrompt.authenticate].
     *
     * Capture the IV from `cipher.iv` after successful authentication and store it
     * alongside the wrapped DEK so it can be used during decryption.
     *
     * @throws KeyPermanentlyInvalidatedException if biometric enrollment has changed
     */
    fun getCipherForEncrypt(): Cipher {
        val key = getOrCreateBiometricKey()
        val cipher = buildCipher()
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Return an AES/GCM/NoPadding [Cipher] initialized for decryption with the
     * Keystore-backed key, using [iv] that was captured during encryption.
     *
     * @throws KeyPermanentlyInvalidatedException if biometric enrollment has changed
     */
    fun getCipherForDecrypt(iv: ByteArray): Cipher {
        val key = getOrCreateBiometricKey()
        val cipher = buildCipher()
        val gcmSpec = GCMParameterSpec(CryptoConstants.GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(CryptoConstants.KEYSTORE_PROVIDER).also { it.load(null) }

    private fun buildCipher(): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding")

    private fun generateBiometricKey(): SecretKey {
        val keyGenSpec = KeyGenParameterSpec.Builder(
            CryptoConstants.KEYSTORE_ALIAS_BIOMETRIC,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(CryptoConstants.AES_KEY_BITS)
            .setUserAuthenticationRequired(true)
            // timeout = 0 means the key requires fresh auth for every use
            .setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            // Invalidate the key if the user enrolls new biometrics — prevents
            // an attacker who adds their fingerprint from accessing the vault.
            .setInvalidatedByBiometricEnrollment(true)
            .build()

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            CryptoConstants.KEYSTORE_PROVIDER,
        )
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
}
