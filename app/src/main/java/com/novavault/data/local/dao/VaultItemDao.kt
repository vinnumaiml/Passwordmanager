package com.novavault.data.local.dao

import androidx.room.*
import com.novavault.data.local.entity.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultItemDao {

    @Query("SELECT * FROM vault_items WHERE user_id = :userId AND is_deleted = 0 ORDER BY updated_at DESC")
    fun observeItems(userId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    fun observeItem(id: String): Flow<VaultItemEntity?>

    @Query("SELECT * FROM vault_items WHERE user_id = :userId AND is_deleted = 0 ORDER BY updated_at DESC")
    suspend fun getItems(userId: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE user_id = :userId AND is_deleted = 1 ORDER BY updated_at DESC")
    suspend fun getDeletedItems(userId: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_items WHERE user_id = :userId AND is_favorite = 1 AND is_deleted = 0 ORDER BY updated_at DESC")
    fun observeFavorites(userId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE user_id = :userId AND item_type = :type AND is_deleted = 0 ORDER BY updated_at DESC")
    fun observeByType(userId: String, type: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE needs_sync = 1 AND user_id = :userId")
    suspend fun getPendingSyncItems(userId: String): List<VaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: VaultItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VaultItemEntity>)

    @Query("UPDATE vault_items SET is_deleted = 1, needs_sync = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE vault_items SET is_deleted = 0, needs_sync = 1, updated_at = :now WHERE id = :id")
    suspend fun restore(id: String, now: Long)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun permanentDelete(id: String)

    @Query("DELETE FROM vault_items WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE vault_items SET is_favorite = :isFavorite, needs_sync = 1, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, now: Long)

    @Query("UPDATE vault_items SET needs_sync = 0, synced_at = :syncedAt, server_version = :serverVersion WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long, serverVersion: Long)

    @Query("SELECT COUNT(*) FROM vault_items WHERE user_id = :userId AND is_deleted = 0")
    suspend fun getCount(userId: String): Int
}
