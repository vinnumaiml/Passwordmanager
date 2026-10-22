package com.novavault.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavault.domain.model.AuthState as DomainAuthState
import com.novavault.domain.model.VaultState as DomainVaultState
import com.novavault.domain.repository.AuthRepository
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sealed auth state observed by the nav graph to route the user. */
sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data object Authenticated : AuthState()
    data object NeedsOnboarding : AuthState()
    data object NeedsEmailVerification : AuthState()
    data object NeedsMasterPasswordSetup : AuthState()
    data object NeedsVaultUnlock : AuthState()
}

/** Sealed vault state for determining if vault is unlocked. */
sealed class VaultState {
    data object Unknown : VaultState()
    data object Locked : VaultState()
    data object Unlocked : VaultState()
}

@HiltViewModel
class NavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isSplashReady = MutableStateFlow(false)
    val isSplashReady: StateFlow<Boolean> = _isSplashReady.asStateFlow()

    val authState: StateFlow<AuthState> = combine(
        authRepository.authState,
        vaultRepository.vaultState,
        settingsRepository.settings,
    ) { domainAuth, domainVault, settings ->
        if (!settings.hasCompletedOnboarding) {
            AuthState.NeedsOnboarding
        } else {
            when (domainAuth) {
                is DomainAuthState.Loading -> AuthState.Loading
                is DomainAuthState.Unauthenticated,
                is DomainAuthState.Error -> AuthState.Unauthenticated
                is DomainAuthState.EmailNotVerified -> AuthState.NeedsEmailVerification
                is DomainAuthState.Authenticated -> {
                    when (domainVault) {
                        is DomainVaultState.NeverSetup -> AuthState.NeedsMasterPasswordSetup
                        is DomainVaultState.Locked -> AuthState.NeedsVaultUnlock
                        is DomainVaultState.Unlocked -> AuthState.Authenticated
                        else -> AuthState.Loading
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val vaultState: StateFlow<VaultState> = vaultRepository.vaultState.map { domainVault ->
        when (domainVault) {
            is DomainVaultState.Unlocked -> VaultState.Unlocked
            is DomainVaultState.Locked -> VaultState.Locked
            else -> VaultState.Unknown
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, VaultState.Unknown)

    val screenshotProtectionEnabled: Flow<Boolean> =
        settingsRepository.settings.map { it.screenshotProtection }

    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state !is AuthState.Loading) {
                    _isSplashReady.value = true
                }
            }
        }
    }

    private var lastBackgroundTimestamp: Long? = null

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val bgTime = lastBackgroundTimestamp ?: return
        lastBackgroundTimestamp = null
        val elapsed = System.currentTimeMillis() - bgTime

        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.lockOnBackground) {
                // If backgrounded for more than 2 seconds (avoids locking on transient biometric prompts or system popups)
                if (elapsed >= 2000L) {
                    vaultRepository.lockVault()
                }
            } else if (settings.autoLockTimeoutMinutes > 0) {
                val timeoutMs = settings.autoLockTimeoutMinutes * 60 * 1000L
                if (elapsed >= timeoutMs) {
                    vaultRepository.lockVault()
                }
            }
        }
    }
}
