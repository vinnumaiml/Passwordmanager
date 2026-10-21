package com.novavault

import com.novavault.crypto.CryptoConstants
import com.novavault.crypto.CryptoEngine
import com.novavault.crypto.KdfParams
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.crypto.AEADBadTagException

class CryptoEngineTest {

    private lateinit var cryptoEngine: CryptoEngine

    @Before
    fun setUp() {
        cryptoEngine = CryptoEngine()
    }

    @Test
    fun testGenerateDekProduces256Bits() {
        val dek = cryptoEngine.generateDek()
        assertNotNull(dek)
        assertEquals(32, dek.size)
    }

    @Test
    fun testAesGcmEncryptDecryptRoundTrip() {
        val dek = cryptoEngine.generateDek()
        val plaintext = "SuperSecretNovaVaultPassword_1234!".toByteArray(Charsets.UTF_8)

        val encryptedBlob = cryptoEngine.encryptWithDek(plaintext, dek)
        assertNotNull(encryptedBlob)
        assertEquals(12, encryptedBlob.nonce.size)
        assertTrue(encryptedBlob.ciphertext.size > plaintext.size)

        val decrypted = cryptoEngine.decryptWithDek(encryptedBlob, dek)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun testDecryptionWithWrongKeyFails() {
        val key1 = cryptoEngine.generateDek()
        val key2 = cryptoEngine.generateDek()
        val plaintext = "Sensitive Data".toByteArray(Charsets.UTF_8)

        val encryptedBlob = cryptoEngine.encryptWithDek(plaintext, key1)

        assertThrows(AEADBadTagException::class.java) {
            cryptoEngine.decryptWithDek(encryptedBlob, key2)
        }
    }

    @Test
    fun testTamperedCiphertextFails() {
        val key = cryptoEngine.generateDek()
        val plaintext = "Confidential Notes".toByteArray(Charsets.UTF_8)

        val blob = cryptoEngine.encryptWithDek(plaintext, key)

        // Tamper with one byte of the ciphertext
        val tamperedCiphertext = blob.ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()
        val tamperedBlob = blob.copy(ciphertext = tamperedCiphertext)

        assertThrows(AEADBadTagException::class.java) {
            cryptoEngine.decryptWithDek(tamperedBlob, key)
        }
    }

    @Test
    fun testDistinctNoncesForRepeatedEncryptions() {
        val key = cryptoEngine.generateDek()
        val plaintext = "Constant Payload".toByteArray(Charsets.UTF_8)

        val blob1 = cryptoEngine.encryptWithDek(plaintext, key)
        val blob2 = cryptoEngine.encryptWithDek(plaintext, key)

        assertFalse(blob1.nonce.contentEquals(blob2.nonce))
        assertFalse(blob1.ciphertext.contentEquals(blob2.ciphertext))
    }

    @Test
    fun testArgon2idKekDerivation() {
        val masterPassword = "TestMasterPassword@2026!".toCharArray()
        val salt = cryptoEngine.generateSalt()
        // Fast test params
        val params = KdfParams(
            memoryKib = 1024,
            iterations = 1,
            parallelism = 1,
            hashLen = 32,
        )

        val kek1 = cryptoEngine.deriveKek(masterPassword.copyOf(), salt, params)
        val kek2 = cryptoEngine.deriveKek(masterPassword.copyOf(), salt, params)

        assertEquals(32, kek1.size)
        assertArrayEquals(kek1, kek2)
    }
}
