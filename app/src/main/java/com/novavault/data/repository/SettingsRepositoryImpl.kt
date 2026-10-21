package com.novavault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novavault.domain.repository.AppSettings
import com.novavault.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_vault_prefs")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        val LOCK_ON_BACKGROUND = booleanPreferencesKey("lock_on_background")
        val CLIPBOARD_CLEAR_SECONDS = intPreferencesKey("clipboard_clear_seconds")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val COMPLETED_ONBOARDING = booleanPreferencesKey("completed_onboarding")
        val SETUP_VAULT = booleanPreferencesKey("setup_vault")
        val WRAPPED_DEK = stringPreferencesKey("wrapped_dek")
        val BIOMETRIC_IV = stringPreferencesKey("biometric_iv")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            autoLockTimeoutMinutes = prefs[Keys.AUTO_LOCK_MINUTES] ?: 5,
            lockOnBackground = prefs[Keys.LOCK_ON_BACKGROUND] ?: true,
            clipboardClearSeconds = prefs[Keys.CLIPBOARD_CLEAR_SECONDS] ?: 30,
            screenshotProtection = prefs[Keys.SCREENSHOT_PROTECTION] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            hasCompletedOnboarding = prefs[Keys.COMPLETED_ONBOARDING] ?: false,
            hasSetupVault = prefs[Keys.SETUP_VAULT] ?: false,
            wrappedDekBase64 = prefs[Keys.WRAPPED_DEK] ?: "",
            biometricIvBase64 = prefs[Keys.BIOMETRIC_IV] ?: "",
        )
    }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = settings.biometricUnlockEnabled
            prefs[Keys.AUTO_LOCK_MINUTES] = settings.autoLockTimeoutMinutes
            prefs[Keys.LOCK_ON_BACKGROUND] = settings.lockOnBackground
            prefs[Keys.CLIPBOARD_CLEAR_SECONDS] = settings.clipboardClearSeconds
            prefs[Keys.SCREENSHOT_PROTECTION] = settings.screenshotProtection
            prefs[Keys.REDUCED_MOTION] = settings.reducedMotion
            prefs[Keys.IS_DARK_THEME] = settings.isDarkTheme
            prefs[Keys.COMPLETED_ONBOARDING] = settings.hasCompletedOnboarding
            prefs[Keys.SETUP_VAULT] = settings.hasSetupVault
            prefs[Keys.WRAPPED_DEK] = settings.wrappedDekBase64
            prefs[Keys.BIOMETRIC_IV] = settings.biometricIvBase64
        }
    }

    override suspend fun getSettings(): AppSettings = settings.first()
}
