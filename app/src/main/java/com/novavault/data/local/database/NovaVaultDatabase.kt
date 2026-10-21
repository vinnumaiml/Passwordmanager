package com.novavault.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.novavault.data.local.dao.FolderDao
import com.novavault.data.local.dao.VaultItemDao
import com.novavault.data.local.dao.VaultKeyDao
import com.novavault.data.local.entity.FolderEntity
import com.novavault.data.local.entity.VaultItemEntity
import com.novavault.data.local.entity.VaultKeyEnvelopeEntity

/**
 * Room database — all sensitive vault content is stored as encrypted payloads.
 * The database file itself is NOT additionally encrypted at the SQLite level in v1;
 * vault-item contents are encrypted individually via the CryptoEngine before insertion.
 *
 * Security consideration: database file is stored in internal storage (MODE_PRIVATE).
 * For additional protection, SQLCipher can be added in a future milestone.
 * Android backup is fully disabled in AndroidManifest and backup_rules.xml.
 */
@Database(
    entities = [
        VaultItemEntity::class,
        VaultKeyEnvelopeEntity::class,
        FolderEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NovaVaultDatabase : RoomDatabase() {
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun vaultKeyDao(): VaultKeyDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "nova_vault.db"
    }
}
