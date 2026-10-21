package com.novavault.domain.model

sealed interface VaultState {
    data object NeverSetup : VaultState      // No master password established
    data object Locked : VaultState           // Encrypted, master password needed
    data object Unlocked : VaultState         // DEK in memory, items accessible
    data object Loading : VaultState
    data class Error(val message: String) : VaultState
}
