package com.novavault.domain.repository

import androidx.biometric.BiometricPrompt
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultState
import com.novavault.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    val vaultState: Flow<VaultState>

    // Unlock/lock
    suspend fun setupMasterPassword(masterPassword: CharArray, recoveryKey: ByteArray): Result<Unit>
    suspend fun unlockWithMasterPassword(masterPassword: CharArray): Result<Unit>
    suspend fun unlockWithRecoveryKey(recoveryKey: ByteArray): Result<Unit>
    suspend fun resetMasterPasswordWithRecoveryKey(recoveryKey: ByteArray, newMasterPassword: CharArray): Result<Unit>
    suspend fun setupBiometricUnlock(cryptoObject: BiometricPrompt.CryptoObject): Result<Unit>
    suspend fun unlockWithBiometrics(cryptoObject: BiometricPrompt.CryptoObject): Result<Unit>
    suspend fun disableBiometricUnlock(): Result<Unit>
    fun canSetupBiometric(): Boolean
    suspend fun lockVault()
    fun isUnlocked(): Boolean

    // CRUD
    fun observeItems(): Flow<List<VaultItem>>
    fun observeItem(id: String): Flow<VaultItem?>
    suspend fun getItems(): List<VaultItem>
    suspend fun saveItem(item: VaultItem): Result<Unit>
    suspend fun deleteItem(id: String, permanent: Boolean = false): Result<Unit>
    suspend fun restoreItem(id: String): Result<Unit>
    suspend fun getDeletedItems(): List<VaultItem>

    // Folders
    fun observeFolders(): Flow<List<Folder>>
    suspend fun saveFolder(folder: Folder): Result<Unit>
    suspend fun deleteFolder(id: String): Result<Unit>

    // Sync
    suspend fun syncWithRemote(): Result<Unit>
    fun observeSyncStatus(): Flow<SyncStatus>
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val timestamp: Long) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
