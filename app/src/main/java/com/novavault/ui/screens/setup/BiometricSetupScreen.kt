package com.novavault.ui.screens.setup

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.SettingsRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BiometricSetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun enableBiometric(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(current.copy(biometricUnlockEnabled = true))
            onDone()
        }
    }

    fun skipBiometric(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.updateSettings(current.copy(biometricUnlockEnabled = false))
            onDone()
        }
    }
}

@Composable
fun BiometricSetupScreen(
    navController: NavController,
    viewModel: BiometricSetupViewModel = hiltViewModel(),
) {
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
                text = "Unlock your vault effortlessly using your fingerprint, face, or device credential. Your encryption key stays hardware-protected inside Android Keystore.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(40.dp))

            NovaButton(
                text = "Enable Biometrics",
                onClick = {
                    viewModel.enableBiometric {
                        navController.navigate(NavRoute.Home.route) {
                            popUpTo(0) { inclusive = true }
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
