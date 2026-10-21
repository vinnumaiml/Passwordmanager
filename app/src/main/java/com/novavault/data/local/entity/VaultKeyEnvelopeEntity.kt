package com.novavault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the vault key envelope locally.
 * Contains: Argon2id KDF parameters (public), encrypted DEK (ciphertext only),
 * and encrypted recovery envelope. No plaintext secrets.
 */
@Entity(tableName = "vault_key_envelope")
data class VaultKeyEnvelopeEntity(
    @PrimaryKey
    val id: String = "local_vault_key",

    @ColumnInfo(name = "user_id")
    val userId: String,

    /** JSON-serialized VaultKeyEnvelope — see crypto package. */
    @ColumnInfo(name = "envelope_json")
    val envelopeJson: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
