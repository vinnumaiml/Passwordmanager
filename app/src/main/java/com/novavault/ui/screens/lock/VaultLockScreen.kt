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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Reset with Recovery Key state
    var showRecoveryDialog by mutableStateOf(false)
    var recoveryKeyInput by mutableStateOf("")
    var newPasswordInput by mutableStateOf("")
    var confirmPasswordInput by mutableStateOf("")
    var recoveryError by mutableStateOf<String?>(null)
    var isResetting by mutableStateOf(false)

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

    fun resetMasterPasswordWithRecovery(onSuccess: () -> Unit) {
        val parsed = recoveryKeyUseCase.parseFromDisplay(recoveryKeyInput.trim())
        if (parsed == null) {
            recoveryError = "Invalid recovery key format. Please enter your 64-character hex key."
            return
        }
        if (newPasswordInput.length < 8) {
            recoveryError = "New Master Password must be at least 8 characters long."
            return
        }
        if (newPasswordInput != confirmPasswordInput) {
            recoveryError = "New passwords do not match."
            return
        }

        isResetting = true
        recoveryError = null
        viewModelScope.launch {
            val result = vaultRepository.resetMasterPasswordWithRecoveryKey(
                recoveryKey = parsed,
                newMasterPassword = newPasswordInput.toCharArray(),
            )
            isResetting = false
            if (result.isSuccess) {
                showRecoveryDialog = false
                recoveryKeyInput = ""
                newPasswordInput = ""
                confirmPasswordInput = ""
                onSuccess()
            } else {
                recoveryError = result.exceptionOrNull()?.message ?: "Failed to reset Master Password with this Recovery Key."
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

            // Forgot Master Password? Reset with Recovery Key (Not casual bypass unlock)
            TextButton(onClick = { viewModel.showRecoveryDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = NovaColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Forgot Master Password? Reset with Recovery Key",
                        color = NovaColors.SecondaryAccent,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        }
    }

    if (viewModel.showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isResetting) viewModel.showRecoveryDialog = false },
            containerColor = Color(0xFF10162A),
            titleContentColor = Color.White,
            textContentColor = NovaColors.TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = NovaColors.SecondaryAccent)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Reset Master Password", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "The recovery key is only used to establish a new Master Password when forgotten. Direct bypass unlock is disabled for zero-knowledge safety.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.recoveryError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = viewModel.recoveryError.orEmpty(),
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }

                    NovaTextField(
                        value = viewModel.recoveryKeyInput,
                        onValueChange = {
                            viewModel.recoveryKeyInput = it
                            viewModel.recoveryError = null
                        },
                        label = "Emergency Recovery Key",
                        singleLine = false,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NovaTextField(
                        value = viewModel.newPasswordInput,
                        onValueChange = {
                            viewModel.newPasswordInput = it
                            viewModel.recoveryError = null
                        },
                        label = "New Master Password",
                        isPassword = true,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    NovaTextField(
                        value = viewModel.confirmPasswordInput,
                        onValueChange = {
                            viewModel.confirmPasswordInput = it
                            viewModel.recoveryError = null
                        },
                        label = "Confirm New Password",
                        isPassword = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetMasterPasswordWithRecovery {
                            navController.navigate(NavRoute.Home.route) {
                                popUpTo(NavRoute.VaultLock.route) { inclusive = true }
                            }
                        }
                    },
                    enabled = !viewModel.isResetting &&
                            viewModel.recoveryKeyInput.isNotBlank() &&
                            viewModel.newPasswordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaColors.PrimaryAccent),
                ) {
                    Text(if (viewModel.isResetting) "Resetting..." else "Reset & Unlock")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.showRecoveryDialog = false },
                    enabled = !viewModel.isResetting,
                ) {
                    Text("Cancel", color = NovaColors.TextSecondary)
                }
            },
        )
    }
}
