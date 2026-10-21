package com.novavault.app.di

import android.content.Context
import com.novavault.crypto.CryptoEngine
import com.novavault.crypto.KeystoreManager
import com.novavault.data.local.dao.FolderDao
import com.novavault.data.local.dao.VaultItemDao
import com.novavault.data.local.dao.VaultKeyDao
import com.novavault.data.repository.VaultRepositoryImpl
import com.novavault.data.repository.SettingsRepositoryImpl
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCryptoEngine(): CryptoEngine = CryptoEngine()

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
    }

    @Provides
    @Singleton
    fun provideVaultRepository(
        @ApplicationContext context: Context,
        cryptoEngine: CryptoEngine,
        keystoreManager: KeystoreManager,
        vaultItemDao: VaultItemDao,
        vaultKeyDao: VaultKeyDao,
        folderDao: FolderDao,
        json: Json,
    ): VaultRepository = VaultRepositoryImpl(
        context, cryptoEngine, keystoreManager, vaultItemDao, vaultKeyDao, folderDao, json
    )

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
    ): SettingsRepository = SettingsRepositoryImpl(context)
}
