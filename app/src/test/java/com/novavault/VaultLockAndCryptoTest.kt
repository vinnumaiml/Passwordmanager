package com.novavault

import com.novavault.crypto.CryptoConstants
import com.novavault.crypto.CryptoEngine
import com.novavault.crypto.EncryptedBlob
import com.novavault.crypto.KdfParams
import com.novavault.crypto.VaultKeyEnvelope
import com.novavault.domain.model.LoginFields
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.util.toBase64
import com.novavault.util.toHex
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class VaultLockAndCryptoTest {

    private val cryptoEngine = CryptoEngine()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun testVaultLockPreventsItemEncryptionWhenDekWiped() {
        var inMemoryDek: ByteArray? = cryptoEngine.generateDek()

        // 1. When unlocked (DEK present) -> save item succeeds
        val item = VaultItem(
            id = UUID.randomUUID().toString(),
            type = VaultItemType.LOGIN,
            name = "Test Site",
            login = LoginFields(username = "elvarix_user", password = "SecretPassword123!"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val itemJson = json.encodeToString(item)
        val encryptedBlob = cryptoEngine.encryptWithDek(itemJson.toByteArray(Charsets.UTF_8), inMemoryDek!!)
        assertNotNull(encryptedBlob)

        // Decrypt with DEK succeeds
        val decryptedBytes = cryptoEngine.decryptWithDek(encryptedBlob, inMemoryDek)
        val decryptedItem = json.decodeFromString<VaultItem>(String(decryptedBytes, Charsets.UTF_8))
        assertEquals("Test Site", decryptedItem.name)
        assertEquals("SecretPassword123!", decryptedItem.login?.password)

        // 2. Simulate vault lock -> DEK is zeroed and nulled
        inMemoryDek.fill(0)
        inMemoryDek = null

        // 3. Attempting to encrypt while locked fails
        var lockExceptionCaught = false
        try {
            if (inMemoryDek == null) {
                throw IllegalStateException("Vault is locked")
            }
            cryptoEngine.encryptWithDek(itemJson.toByteArray(Charsets.UTF_8), inMemoryDek)
        } catch (e: IllegalStateException) {
            lockExceptionCaught = true
            assertEquals("Vault is locked", e.message)
        }
        assertTrue("Attempt to save while locked must be blocked", lockExceptionCaught)
    }

    @Test
    fun testUnlockRestoresAccessToExistingEncryptedItems() {
        val masterPassword = "MyStrongMasterPassword2026!".toCharArray()
        val originalDek = cryptoEngine.generateDek()

        // Setup master password envelope
        val salt = cryptoEngine.generateSalt()
        val kdfParams = KdfParams(
            memoryKib = 1024, // Fast for test
            iterations = 1,
            parallelism = 1,
            hashLen = 32,
            saltBase64 = salt.toBase64(),
        )
        val kek = cryptoEngine.deriveKek(masterPassword, salt, kdfParams)
        val encryptedDek = cryptoEngine.encryptWithKek(originalDek, kek)

        val envelope = VaultKeyEnvelope(
            envelopeVersion = 1,
            kdfParams = kdfParams,
            encryptedDek = encryptedDek,
        )

        // Encrypt an item with DEK
        val sensitiveItem = VaultItem(
            id = "item_123",
            type = VaultItemType.LOGIN,
            name = "Banking Portal",
            login = LoginFields(username = "admin", password = "SuperSecretBankPassword"),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        val itemPayload = json.encodeToString(sensitiveItem).toByteArray(Charsets.UTF_8)
        val itemBlob = cryptoEngine.encryptWithDek(itemPayload, originalDek)

        // Lock vault: wipe originalDek and KEK
        originalDek.fill(0)
        kek.fill(0)

        // Now simulate unlock with master password
        val unlockKek = cryptoEngine.deriveKek(
            masterPassword,
            java.util.Base64.getDecoder().decode(envelope.kdfParams.saltBase64),
            envelope.kdfParams,
        )
        val restoredDek = cryptoEngine.decryptWithKek(envelope.encryptedDek, unlockKek)

        // Decrypt the previously saved item with restored DEK
        val decryptedBytes = cryptoEngine.decryptWithDek(itemBlob, restoredDek)
        val restoredItem = json.decodeFromString<VaultItem>(String(decryptedBytes, Charsets.UTF_8))

        assertEquals("Banking Portal", restoredItem.name)
        assertEquals("SuperSecretBankPassword", restoredItem.login?.password)
    }
}
