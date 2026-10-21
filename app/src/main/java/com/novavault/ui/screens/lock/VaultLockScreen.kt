package com.novavault.ui.screens.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.crypto.RecoveryKeyUseCase
import com.novavault.ui.LocalBiometricPromptManager
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.BiometricPromptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultLockViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val recoveryKeyUseCase: RecoveryKeyUseCase,
) : ViewModel() {
    var password by mutableStateOf("")
    var isUnlocking by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isBiometricEnabled by mutableStateOf(false)
    var showRecoveryDialog by mutableStateOf(false)
    var recoveryKeyInput by mutableStateOf("")
    var recoveryError by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            isBiometricEnabled = settings.biometricUnlockEnabled
        }
    }

    fun unlockWithPassword(onSuccess: () -> Unit) {
        if (password.isEmpty()) return
        isUnlocking = true
        errorMessage = null
        viewModelScope.launch {
            val result = vaultRepository.unlockWithMasterPassword(password.toCharArray())
            isUnlocking = false
            if (result.isSuccess) {
                password = ""
                onSuccess()
            } else {
                errorMessage = "Incorrect Master Password. Please try again."
            }
        }
    }

    fun unlockWithRecoveryKey(onSuccess: () -> Unit) {
        val parsed = recoveryKeyUseCase.parseFromDisplay(recoveryKeyInput)
        if (parsed == null) {
            recoveryError = "Invalid recovery key format. Please enter the 64-character hex key."
            return
        }
        isUnlocking = true
        recoveryError = null
        viewModelScope.launch {
            val result = vaultRepository.unlockWithRecoveryKey(parsed)
            isUnlocking = false
            if (result.isSuccess) {
                showRecoveryDialog = false
                recoveryKeyInput = ""
                onSuccess()
            } else {
                recoveryError = "Failed to unlock with this recovery key."
            }
        }
    }
}

@Composable
fun VaultLockScreen(
    navController: NavController,
    viewModel: VaultLockViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val biometricPromptManager = LocalBiometricPromptManager.current

    LaunchedEffect(viewModel.isBiometricEnabled) {
        if (viewModel.isBiometricEnabled && context is FragmentActivity && biometricPromptManager.canAuthenticate()) {
            biometricPromptManager.prompt(context) { result ->
                if (result is BiometricPromptManager.BiometricResult.Success) {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.VaultLock.route) { inclusive = true }
                    }
                }
            }
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = NovaColors.PrimaryAccent,
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Vault Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your Master Password to decrypt your vault items.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            NovaTextField(
                value = viewModel.password,
                onValueChange = {
                    viewModel.password = it
                    viewModel.errorMessage = null
                },
                label = "Master Password",
                isPassword = true,
                leadingIcon = Icons.Default.Lock,
                isError = viewModel.errorMessage != null,
                errorMessage = viewModel.errorMessage,
            )

            Spacer(modifier = Modifier.height(24.dp))

            NovaButton(
                text = "Unlock Vault",
                onClick = {
                    viewModel.unlockWithPassword {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(NavRoute.VaultLock.route) { inclusive = true }
                        }
                    }
                },
                loading = viewModel.isUnlocking,
                enabled = viewModel.password.isNotBlank() && !viewModel.isUnlocking,
            )

            if (viewModel.isBiometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = {
                        if (context is FragmentActivity) {
                            biometricPromptManager.prompt(context) { result ->
                                if (result is BiometricPromptManager.BiometricResult.Success) {
                                    navController.navigate(NavRoute.Home.route) {
                                        popUpTo(NavRoute.VaultLock.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Unlock with Biometrics",
                        tint = NovaColors.SecondaryAccent,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { viewModel.showRecoveryDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = NovaColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Unlock with Recovery Key",
                        color = NovaColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (viewModel.showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showRecoveryDialog = false },
            title = { Text("Emergency Recovery Key") },
            text = {
                Column {
                    Text(
                        "Paste or type your 64-character emergency recovery key:",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NovaTextField(
                        value = viewModel.recoveryKeyInput,
                        onValueChange = {
                            viewModel.recoveryKeyInput = it
                            viewModel.recoveryError = null
                        },
                        label = "Recovery Key",
                        isError = viewModel.recoveryError != null,
                        errorMessage = viewModel.recoveryError,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlockWithRecoveryKey {
                            navController.navigate(NavRoute.Home.route) {
                                popUpTo(NavRoute.VaultLock.route) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showRecoveryDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
