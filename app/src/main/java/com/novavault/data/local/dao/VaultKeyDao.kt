package com.novavault.data.local.dao

import androidx.room.*
import com.novavault.data.local.entity.VaultKeyEnvelopeEntity

@Dao
interface VaultKeyDao {
    @Query("SELECT * FROM vault_key_envelope WHERE user_id = :userId LIMIT 1")
    suspend fun getEnvelope(userId: String): VaultKeyEnvelopeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(envelope: VaultKeyEnvelopeEntity)

    @Query("DELETE FROM vault_key_envelope WHERE user_id = :userId")
    suspend fun delete(userId: String)
}
