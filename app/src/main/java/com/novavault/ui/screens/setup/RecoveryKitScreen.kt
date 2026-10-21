package com.novavault.ui.screens.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.crypto.RecoveryKeyUseCase
import com.novavault.ui.components.NovaButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecoveryKitViewModel @Inject constructor(
    private val recoveryKeyUseCase: RecoveryKeyUseCase,
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var rawRecoveryKey by mutableStateOf<ByteArray?>(null)
    var formattedKey by mutableStateOf("")
    var isConfirmed by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        val key = recoveryKeyUseCase.generateRecoveryKey()
        rawRecoveryKey = key
        formattedKey = recoveryKeyUseCase.formatForDisplay(key)
    }

    fun completeSetup(onSuccess: () -> Unit) {
        val masterPassword = SetupSessionHolder.masterPasswordChars ?: return
        val key = rawRecoveryKey ?: return
        isSubmitting = true
        errorMessage = null

        viewModelScope.launch {
            val result = vaultRepository.setupMasterPassword(masterPassword, key)
            if (result.isSuccess) {
                val currentSettings = settingsRepository.getSettings()
                settingsRepository.updateSettings(currentSettings.copy(hasSetupVault = true))
                SetupSessionHolder.masterPasswordChars = null
                isSubmitting = false
                onSuccess()
            } else {
                isSubmitting = false
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to set up vault"
            }
        }
    }
}

@Composable
fun RecoveryKitScreen(
    navController: NavController,
    viewModel: RecoveryKitViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Recovery Key",
                tint = NovaColors.PrimaryAccent,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Recovery Kit",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Store this recovery key in a safe, offline location. It is your ONLY safeguard if you ever forget your Master Password.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recovery key display box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NovaColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, NovaColors.Border, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "256-BIT EMERGENCY RECOVERY KEY",
                            style = MaterialTheme.typography.labelSmall,
                            color = NovaColors.SecondaryAccent,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(
                            onClick = {
                                SecureClipboard.copyAndScheduleClear(
                                    context = context,
                                    text = viewModel.formattedKey,
                                    label = "Nova Vault Recovery Key",
                                    clearAfterSeconds = 60,
                                    scope = scope,
                                )
                                copied = true
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy Recovery Key",
                                tint = if (copied) NovaColors.Success else NovaColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = viewModel.formattedKey,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            lineHeight = 26.sp,
                        ),
                        color = NovaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Confirmation checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = viewModel.isConfirmed,
                    onCheckedChange = { viewModel.isConfirmed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NovaColors.PrimaryAccent,
                        uncheckedColor = NovaColors.TextSecondary,
                    ),
                )
                Text(
                    text = "I have safely written down or stored my Recovery Kit offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (viewModel.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaColors.Danger,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            NovaButton(
                text = "Complete Vault Setup",
                onClick = {
                    viewModel.completeSetup {
                        navController.navigate(NavRoute.BiometricSetup.route) {
                            popUpTo(NavRoute.MasterPasswordSetup.route) { inclusive = true }
                        }
                    }
                },
                loading = viewModel.isSubmitting,
                enabled = viewModel.isConfirmed && !viewModel.isSubmitting,
            )
        }
    }
}
