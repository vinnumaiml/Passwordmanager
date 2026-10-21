package com.novavault.data.local.dao

import androidx.room.*
import com.novavault.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE user_id = :userId ORDER BY name ASC")
    fun observeFolders(userId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE user_id = :userId ORDER BY name ASC")
    suspend fun getFolders(userId: String): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM folders WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
