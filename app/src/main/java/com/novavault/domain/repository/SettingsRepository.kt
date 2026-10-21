package com.novavault.domain.repository

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val biometricUnlockEnabled: Boolean = false,
    val autoLockTimeoutMinutes: Int = 5,
    val lockOnBackground: Boolean = true,
    val clipboardClearSeconds: Int = 30,
    val screenshotProtection: Boolean = true,
    val reducedMotion: Boolean = false,
    val isDarkTheme: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val hasSetupVault: Boolean = false,
    val wrappedDekBase64: String = "",   // Keystore-wrapped DEK for biometric unlock
    val biometricIvBase64: String = "",  // IV for Keystore-wrapped DEK
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun getSettings(): AppSettings
}
