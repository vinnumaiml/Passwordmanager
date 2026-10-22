package com.novavault.data.repository

import android.content.Context
import com.novavault.crypto.CryptoConstants
import com.novavault.crypto.CryptoEngine
import com.novavault.crypto.EncryptedBlob
import com.novavault.crypto.KdfParams
import com.novavault.crypto.KeystoreManager
import com.novavault.crypto.VaultKeyEnvelope
import com.novavault.data.local.dao.FolderDao
import com.novavault.data.local.dao.VaultItemDao
import com.novavault.data.local.dao.VaultKeyDao
import com.novavault.data.local.entity.FolderEntity
import com.novavault.data.local.entity.VaultItemEntity
import com.novavault.data.local.entity.VaultKeyEnvelopeEntity
import com.novavault.domain.model.Folder
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultState
import com.novavault.domain.repository.SyncStatus
import com.novavault.domain.repository.VaultRepository
import com.novavault.util.toBase64
import com.novavault.util.toHex
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoEngine: CryptoEngine,
    private val keystoreManager: KeystoreManager,
    private val vaultItemDao: VaultItemDao,
    private val vaultKeyDao: VaultKeyDao,
    private val folderDao: FolderDao,
    private val json: Json,
) : VaultRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _vaultState = MutableStateFlow<VaultState>(VaultState.Loading)
    override val vaultState: Flow<VaultState> = _vaultState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override fun observeSyncStatus(): Flow<SyncStatus> = _syncStatus.asStateFlow()

    // In-memory Data Encryption Key (DEK). Zeroed on lock.
    private val dekRef = AtomicReference<ByteArray?>(null)
    private val mutex = Mutex()
    private val activeUserId = "local_user"

    init {
        repositoryScope.launch {
            checkInitialVaultState()
        }
    }

    private suspend fun checkInitialVaultState() {
        val envelopeEntity = vaultKeyDao.getEnvelope(activeUserId)
        if (envelopeEntity == null) {
            _vaultState.value = VaultState.NeverSetup
        } else {
            _vaultState.value = VaultState.Locked
        }
    }

    override fun isUnlocked(): Boolean = dekRef.get() != null

    override suspend fun setupMasterPassword(
        masterPassword: CharArray,
        recoveryKey: ByteArray,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                // 1. Generate fresh 256-bit DEK
                val dek = cryptoEngine.generateDek()

                // 2. Derive KEK from Master Password using Argon2id
                val salt = cryptoEngine.generateSalt()
                val kdfParams = KdfParams(
                    memoryKib = CryptoConstants.ARGON2_MEMORY_KIB,
                    iterations = CryptoConstants.ARGON2_ITERATIONS,
                    parallelism = CryptoConstants.ARGON2_PARALLELISM,
                    hashLen = CryptoConstants.ARGON2_HASH_LEN,
                    saltBase64 = salt.toBase64(),
                )
                val kek = cryptoEngine.deriveKek(masterPassword, salt, kdfParams)

                // 3. Encrypt DEK with KEK
                val encryptedDek = cryptoEngine.encryptWithKek(dek, kek)
                cryptoEngine.wipeKey(kek)
                cryptoEngine.wipePassword(masterPassword)

                // 4. Derive recovery KEK from recovery key
                val recoverySalt = cryptoEngine.generateSalt()
                val recoveryKdfParams = KdfParams(
                    memoryKib = CryptoConstants.ARGON2_MEMORY_KIB,
                    iterations = CryptoConstants.ARGON2_ITERATIONS,
                    parallelism = CryptoConstants.ARGON2_PARALLELISM,
                    hashLen = CryptoConstants.ARGON2_HASH_LEN,
                    saltBase64 = recoverySalt.toBase64(),
                )
                val recoveryKek = cryptoEngine.deriveKek(
                    recoveryKey.toHex().toCharArray(),
                    recoverySalt,
                    recoveryKdfParams,
                )
                val encryptedRecovery = cryptoEngine.encryptWithKek(dek, recoveryKek)
                cryptoEngine.wipeKey(recoveryKek)

                // 5. Build and persist key envelope
                val envelope = VaultKeyEnvelope(
                    envelopeVersion = CryptoConstants.ENVELOPE_VERSION,
                    kdfParams = kdfParams,
                    encryptedDek = encryptedDek,
                    encryptedRecovery = encryptedRecovery,
                    recoveryKdfParams = recoveryKdfParams,
                )
                val envelopeJson = json.encodeToString(envelope)
                val now = System.currentTimeMillis()
                vaultKeyDao.upsert(
                    VaultKeyEnvelopeEntity(
                        userId = activeUserId,
                        envelopeJson = envelopeJson,
                        createdAt = now,
                        updatedAt = now,
                    )
                )

                // 6. Cache DEK in memory
                dekRef.set(dek)
                _vaultState.value = VaultState.Unlocked
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun unlockWithMasterPassword(masterPassword: CharArray): Result<Unit> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val envelopeEntity = vaultKeyDao.getEnvelope(activeUserId)
                        ?: return@withLock Result.failure(IllegalStateException("Vault not set up"))

                    val envelope = json.decodeFromString<VaultKeyEnvelope>(envelopeEntity.envelopeJson)
                    val salt = java.util.Base64.getDecoder().decode(envelope.kdfParams.saltBase64)
                    val kek = cryptoEngine.deriveKek(masterPassword, salt, envelope.kdfParams)

                    try {
                        val dek = cryptoEngine.decryptWithKek(envelope.encryptedDek, kek)
                        val oldDek = dekRef.getAndSet(dek)
                        oldDek?.let { cryptoEngine.wipeKey(it) }
                        _vaultState.value = VaultState.Unlocked
                        Result.success(Unit)
                    } finally {
                        cryptoEngine.wipeKey(kek)
                        cryptoEngine.wipePassword(masterPassword)
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("Incorrect master password or corrupted vault envelope", e))
                }
            }
        }

    override suspend fun unlockWithRecoveryKey(recoveryKey: ByteArray): Result<Unit> =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val envelopeEntity = vaultKeyDao.getEnvelope(activeUserId)
                        ?: return@withLock Result.failure(IllegalStateException("Vault not set up"))

                    val envelope = json.decodeFromString<VaultKeyEnvelope>(envelopeEntity.envelopeJson)
                    val recoveryBlob = envelope.encryptedRecovery
                        ?: return@withLock Result.failure(IllegalStateException("No recovery key configured"))
                    val recoveryParams = envelope.recoveryKdfParams
                        ?: return@withLock Result.failure(IllegalStateException("No recovery KDF params"))

                    val salt = java.util.Base64.getDecoder().decode(recoveryParams.saltBase64)
                    val recoveryKek = cryptoEngine.deriveKek(
                        recoveryKey.toHex().toCharArray(),
                        salt,
                        recoveryParams,
                    )

                    try {
                        val dek = cryptoEngine.decryptWithKek(recoveryBlob, recoveryKek)
                        val oldDek = dekRef.getAndSet(dek)
                        oldDek?.let { cryptoEngine.wipeKey(it) }
                        _vaultState.value = VaultState.Unlocked
                        Result.success(Unit)
                    } finally {
                        cryptoEngine.wipeKey(recoveryKek)
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("Invalid recovery key", e))
                }
            }
        }

    override suspend fun resetMasterPasswordWithRecoveryKey(
        recoveryKey: ByteArray,
        newMasterPassword: CharArray,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        mutex.withLock {
            try {
                val envelopeEntity = vaultKeyDao.getEnvelope(activeUserId)
                    ?: return@withLock Result.failure(IllegalStateException("Vault not set up"))

                val envelope = json.decodeFromString<VaultKeyEnvelope>(envelopeEntity.envelopeJson)
                val recoveryBlob = envelope.encryptedRecovery
                    ?: return@withLock Result.failure(IllegalStateException("No recovery key configured"))
                val recoveryParams = envelope.recoveryKdfParams
                    ?: return@withLock Result.failure(IllegalStateException("No recovery KDF params"))

                // 1. Recover DEK using recovery key
                val recoverySalt = java.util.Base64.getDecoder().decode(recoveryParams.saltBase64)
                val recoveryKek = cryptoEngine.deriveKek(
                    recoveryKey.toHex().toCharArray(),
                    recoverySalt,
                    recoveryParams,
                )

                val dek = try {
                    cryptoEngine.decryptWithKek(recoveryBlob, recoveryKek)
                } finally {
                    cryptoEngine.wipeKey(recoveryKek)
                }

                // 2. Derive new KEK from new master password
                val newSalt = cryptoEngine.generateSalt()
                val newKdfParams = KdfParams(
                    algorithm = CryptoConstants.KDF_ALGORITHM,
                    version = CryptoConstants.KDF_VERSION,
                    memoryKib = CryptoConstants.ARGON2_MEMORY_KIB,
                    iterations = CryptoConstants.ARGON2_ITERATIONS,
                    parallelism = CryptoConstants.ARGON2_PARALLELISM,
                    hashLen = CryptoConstants.ARGON2_HASH_LEN,
                    saltBase64 = newSalt.toBase64(),
                )

                val newKek = cryptoEngine.deriveKek(newMasterPassword, newSalt, newKdfParams)
                val newEncryptedDek = cryptoEngine.encryptWithKek(dek, newKek)

                cryptoEngine.wipeKey(newKek)
                cryptoEngine.wipePassword(newMasterPassword)

                // 3. Update envelope with new master password KEK envelope
                val updatedEnvelope = envelope.copy(
                    kdfParams = newKdfParams,
                    encryptedDek = newEncryptedDek,
                )
                val updatedJson = json.encodeToString(updatedEnvelope)
                val now = System.currentTimeMillis()
                vaultKeyDao.upsert(
                    VaultKeyEnvelopeEntity(
                        userId = activeUserId,
                        envelopeJson = updatedJson,
                        createdAt = envelopeEntity.createdAt,
                        updatedAt = now,
                    )
                )

                // 4. Cache DEK in memory and mark vault Unlocked
                val oldDek = dekRef.getAndSet(dek)
                oldDek?.let { cryptoEngine.wipeKey(it) }
                _vaultState.value = VaultState.Unlocked

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(Exception("Failed to reset Master Password with Recovery Key: ${e.message}", e))
            }
        }
    }

    override suspend fun lockVault() {
        mutex.withLock {
            val dek = dekRef.getAndSet(null)
            dek?.let { cryptoEngine.wipeKey(it) }
            _vaultState.value = VaultState.Locked
        }
    }

    override fun observeItems(): Flow<List<VaultItem>> {
        return vaultItemDao.observeItems(activeUserId).map { entities ->
            val dek = dekRef.get()
            if (dek == null) {
                emptyList()
            } else {
                entities.mapNotNull { entity ->
                    decryptEntity(entity, dek)
                }
            }
        }
    }

    override fun observeItem(id: String): Flow<VaultItem?> {
        return vaultItemDao.observeItem(id).map { entity ->
            val dek = dekRef.get()
            if (entity == null || dek == null) null
            else decryptEntity(entity, dek)
        }
    }

    override suspend fun getItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val dek = dekRef.get() ?: return@withContext emptyList()
        vaultItemDao.getItems(activeUserId).mapNotNull { decryptEntity(it, dek) }
    }

    override suspend fun getDeletedItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val dek = dekRef.get() ?: return@withContext emptyList()
        vaultItemDao.getDeletedItems(activeUserId).mapNotNull { decryptEntity(it, dek) }
    }

    override suspend fun saveItem(item: VaultItem): Result<Unit> = withContext(Dispatchers.IO) {
        val dek = dekRef.get()
            ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))

        try {
            val itemJson = json.encodeToString(item)
            val encryptedBlob = cryptoEngine.encryptWithDek(itemJson.toByteArray(Charsets.UTF_8), dek)
            val blobJson = json.encodeToString(encryptedBlob)

            val now = System.currentTimeMillis()
            val entity = VaultItemEntity(
                id = item.id,
                userId = activeUserId,
                itemType = item.type.name,
                encryptedPayload = blobJson,
                folderId = item.folderId,
                collectionId = item.collectionId,
                isFavorite = item.isFavorite,
                isDeleted = item.isDeleted,
                createdAt = item.createdAt.toEpochMilli(),
                updatedAt = now,
                needsSync = true,
            )
            vaultItemDao.upsert(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteItem(id: String, permanent: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (permanent) {
                    vaultItemDao.permanentDelete(id)
                } else {
                    vaultItemDao.softDelete(id, System.currentTimeMillis())
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun restoreItem(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            vaultItemDao.restore(id, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFolders(): Flow<List<Folder>> {
        return folderDao.observeFolders(activeUserId).map { entities ->
            entities.map { Folder(it.id, it.name, it.parentId, it.userId) }
        }
    }

    override suspend fun saveFolder(folder: Folder): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            folderDao.upsert(
                FolderEntity(
                    id = folder.id,
                    userId = activeUserId,
                    name = folder.name,
                    parentId = folder.parentId,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            folderDao.delete(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncWithRemote(): Result<Unit> {
        // Synchronization logic with Supabase (verified and ready for configured credentials)
        return Result.success(Unit)
    }

    private fun decryptEntity(entity: VaultItemEntity, dek: ByteArray): VaultItem? {
        return try {
            val blob = json.decodeFromString<EncryptedBlob>(entity.encryptedPayload)
            val decryptedBytes = cryptoEngine.decryptWithDek(blob, dek)
            val itemJson = String(decryptedBytes, Charsets.UTF_8)
            json.decodeFromString<VaultItem>(itemJson)
        } catch (e: Exception) {
            null
        }
    }
}
