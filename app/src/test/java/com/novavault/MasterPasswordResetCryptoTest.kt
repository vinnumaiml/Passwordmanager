package com.novavault

import com.novavault.crypto.CryptoConstants
import com.novavault.crypto.CryptoEngine
import com.novavault.crypto.KdfParams
import com.novavault.crypto.VaultKeyEnvelope
import com.novavault.domain.usecase.crypto.RecoveryKeyUseCase
import com.novavault.util.toBase64
import com.novavault.util.toHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class MasterPasswordResetCryptoTest {

    private lateinit var cryptoEngine: CryptoEngine
    private lateinit var recoveryKeyUseCase: RecoveryKeyUseCase

    @Before
    fun setUp() {
        cryptoEngine = CryptoEngine()
        recoveryKeyUseCase = RecoveryKeyUseCase()
    }

    @Test
    fun testRecoveryKeyResetsMasterPasswordAndPreservesDek() {
        // 1. Initial setup
        val initialMasterPassword = "OriginalPassword2026!".toCharArray()
        val recoveryKey = recoveryKeyUseCase.generateRecoveryKey()
        val originalDek = cryptoEngine.generateDek()

        // Derive initial KEK
        val masterSalt = cryptoEngine.generateSalt()
        val kdfParams = KdfParams(
            memoryKib = 1024, // low memory for fast unit test
            iterations = 1,
            parallelism = 1,
            hashLen = 32,
            saltBase64 = masterSalt.toBase64(),
        )
        val initialKek = cryptoEngine.deriveKek(initialMasterPassword, masterSalt, kdfParams)
        val encryptedDek = cryptoEngine.encryptWithKek(originalDek, initialKek)

        // Derive recovery KEK
        val recoverySalt = cryptoEngine.generateSalt()
        val recoveryKdfParams = KdfParams(
            memoryKib = 1024,
            iterations = 1,
            parallelism = 1,
            hashLen = 32,
            saltBase64 = recoverySalt.toBase64(),
        )
        val recoveryKek = cryptoEngine.deriveKek(
            recoveryKey.toHex().toCharArray(),
            recoverySalt,
            recoveryKdfParams,
        )
        val encryptedRecovery = cryptoEngine.encryptWithKek(originalDek, recoveryKek)

        val envelope = VaultKeyEnvelope(
            envelopeVersion = CryptoConstants.ENVELOPE_VERSION,
            kdfParams = kdfParams,
            encryptedDek = encryptedDek,
            encryptedRecovery = encryptedRecovery,
            recoveryKdfParams = recoveryKdfParams,
        )

        // 2. Simulate forgotten password & recovery flow
        val recoverySaltBytes = Base64.getDecoder().decode(envelope.recoveryKdfParams!!.saltBase64)
        val derivedRecoveryKek = cryptoEngine.deriveKek(
            recoveryKey.toHex().toCharArray(),
            recoverySaltBytes,
            envelope.recoveryKdfParams!!,
        )
        val recoveredDek = cryptoEngine.decryptWithKek(envelope.encryptedRecovery!!, derivedRecoveryKek)

        assertArrayEquals("Recovered DEK must match original DEK", originalDek, recoveredDek)

        // 3. User sets new master password
        val newMasterPassword = "BrandNewMasterPassword2026!".toCharArray()
        val newSalt = cryptoEngine.generateSalt()
        val newKdfParams = KdfParams(
            memoryKib = 1024,
            iterations = 1,
            parallelism = 1,
            hashLen = 32,
            saltBase64 = newSalt.toBase64(),
        )
        val newKek = cryptoEngine.deriveKek(newMasterPassword, newSalt, newKdfParams)
        val newEncryptedDek = cryptoEngine.encryptWithKek(recoveredDek, newKek)

        val updatedEnvelope = envelope.copy(
            kdfParams = newKdfParams,
            encryptedDek = newEncryptedDek,
        )

        // 4. Verify new password successfully decrypts DEK
        val testNewKek = cryptoEngine.deriveKek(
            newMasterPassword,
            Base64.getDecoder().decode(updatedEnvelope.kdfParams.saltBase64),
            updatedEnvelope.kdfParams,
        )
        val decryptedWithNewPassword = cryptoEngine.decryptWithKek(updatedEnvelope.encryptedDek, testNewKek)
        assertArrayEquals(originalDek, decryptedWithNewPassword)

        // 5. Verify old password fails against updated envelope
        val testOldKek = cryptoEngine.deriveKek(
            initialMasterPassword,
            Base64.getDecoder().decode(updatedEnvelope.kdfParams.saltBase64),
            updatedEnvelope.kdfParams,
        )
        var oldPasswordSucceeded = false
        try {
            cryptoEngine.decryptWithKek(updatedEnvelope.encryptedDek, testOldKek)
            oldPasswordSucceeded = true
        } catch (expected: Exception) {
            oldPasswordSucceeded = false
        }
        assertFalse("Old master password must not be able to decrypt the new envelope", oldPasswordSucceeded)
    }
}
