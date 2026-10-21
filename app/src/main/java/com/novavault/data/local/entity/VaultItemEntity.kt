package com.novavault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores encrypted vault items. ALL sensitive fields are stored as Base64-encoded
 * AES-256-GCM ciphertext. Only non-sensitive metadata (type, timestamps, soft-delete flag)
 * is stored in plain columns for indexing.
 *
 * The [encryptedPayload] column contains a JSON-serialized [EncryptedBlob] that decrypts to
 * a JSON-serialized [VaultItem] domain model.
 *
 * Security note: folderId and userId are stored in plaintext only as foreign keys /
 * ownership identifiers — they contain no secret information.
 */
@Entity(
    tableName = "vault_items",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["item_type"]),
        Index(value = ["is_deleted"]),
        Index(value = ["is_favorite"]),
        Index(value = ["updated_at"]),
    ]
)
data class VaultItemEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    /** Item type stored in plain for category filtering (contains no secret data). */
    @ColumnInfo(name = "item_type")
    val itemType: String,

    /** Encrypted JSON payload — decrypts to full VaultItem domain model. */
    @ColumnInfo(name = "encrypted_payload")
    val encryptedPayload: String,           // Base64(JSON(EncryptedBlob))

    @ColumnInfo(name = "folder_id")
    val folderId: String? = null,

    @ColumnInfo(name = "collection_id")
    val collectionId: String? = null,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,                    // epoch millis

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    /** Last sync timestamp — null if never synced. */
    @ColumnInfo(name = "synced_at")
    val syncedAt: Long? = null,

    /** Whether local version is ahead of remote. */
    @ColumnInfo(name = "needs_sync")
    val needsSync: Boolean = true,

    /** Remote server-assigned version for conflict resolution. */
    @ColumnInfo(name = "server_version")
    val serverVersion: Long = 0L,
)
