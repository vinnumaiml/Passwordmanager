package com.novavault.ui.screens.setup

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.novavault.crypto.KeystoreManager
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.LocalBiometricPromptManager
import com.novavault.ui.components.NovaButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.BiometricPromptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricSetupViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val keystoreManager: KeystoreManager,
) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun prepareEncryptCipher(): javax.crypto.Cipher? {
        return try {
            keystoreManager.getCipherForEncrypt()
        } catch (e: Exception) {
            errorMessage = e.message
            null
        }
    }

    fun enrollBiometrics(cryptoObject: BiometricPrompt.CryptoObject, onDone: () -> Unit) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val result = vaultRepository.setupBiometricUnlock(cryptoObject)
            isLoading = false
            if (result.isSuccess) {
                onDone()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to enroll biometrics"
            }
        }
    }

    fun skipBiometric(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(
                current.copy(
                    biometricUnlockEnabled = false,
                    wrappedDekBase64 = "",
                    biometricIvBase64 = "",
                )
            )
            onDone()
        }
    }
}

@Composable
fun BiometricSetupScreen(
    navController: NavController,
    viewModel: BiometricSetupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val biometricPromptManager = LocalBiometricPromptManager.current

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Biometric",
                tint = NovaColors.SecondaryAccent,
                modifier = Modifier.size(80.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enable Biometric Unlock",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Unlock your Elvarix vault effortlessly using your fingerprint. Your Master Key stays hardware-protected inside Android Keystore.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            if (viewModel.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaColors.Danger,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            NovaButton(
                text = "Enable Biometrics",
                loading = viewModel.isLoading,
                onClick = {
                    if (context is FragmentActivity && biometricPromptManager.canAuthenticate()) {
                        val cipher = viewModel.prepareEncryptCipher()
                        if (cipher != null) {
                            biometricPromptManager.prompt(
                                activity = context,
                                title = "Enroll Biometrics",
                                subtitle = "Scan fingerprint to authorize hardware key",
                                cryptoObject = BiometricPrompt.CryptoObject(cipher),
                            ) { result ->
                                if (result is BiometricPromptManager.BiometricResult.Success && result.cryptoObject != null) {
                                    viewModel.enrollBiometrics(result.cryptoObject) {
                                        navController.navigate(NavRoute.Home.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Device has no biometric hardware/enrolled fingers, skip gracefully
                        viewModel.skipBiometric {
                            navController.navigate(NavRoute.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    viewModel.skipBiometric {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                Text(
                    text = "Skip for Now",
                    color = NovaColors.TextSecondary,
                )
            }
        }
    }
}
