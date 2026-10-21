package com.novavault.app.di

import android.content.Context
import androidx.room.Room
import com.novavault.data.local.database.NovaVaultDatabase
import com.novavault.data.local.dao.FolderDao
import com.novavault.data.local.dao.VaultItemDao
import com.novavault.data.local.dao.VaultKeyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaVaultDatabase {
        return Room.databaseBuilder(
            context,
            NovaVaultDatabase::class.java,
            NovaVaultDatabase.DATABASE_NAME,
        )
        // Do not allow main-thread queries in production
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
    }

    @Provides
    fun provideVaultItemDao(db: NovaVaultDatabase): VaultItemDao = db.vaultItemDao()

    @Provides
    fun provideVaultKeyDao(db: NovaVaultDatabase): VaultKeyDao = db.vaultKeyDao()

    @Provides
    fun provideFolderDao(db: NovaVaultDatabase): FolderDao = db.folderDao()
}
