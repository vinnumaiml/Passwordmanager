package com.novavault.ui.screens.setup

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.components.PasswordStrengthIndicator
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MasterPasswordSetupViewModel @Inject constructor(
    private val generatePasswordUseCase: GeneratePasswordUseCase,
) : ViewModel() {
    var masterPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)

    val strength: PasswordStrength
        get() = if (masterPassword.isEmpty()) PasswordStrength.VERY_WEAK
        else generatePasswordUseCase.estimateStrength(masterPassword)

    fun validate(): Boolean {
        if (masterPassword.length < 8) {
            errorMessage = "Master Password must be at least 8 characters long."
            return false
        }
        if (masterPassword != confirmPassword) {
            errorMessage = "Passwords do not match."
            return false
        }
        if (strength == PasswordStrength.VERY_WEAK) {
            errorMessage = "Please choose a stronger password with a mix of characters."
            return false
        }
        errorMessage = null
        return true
    }
}

// Global holder for setup flow so sensitive chars aren't put in plain nav routes
object SetupSessionHolder {
    var masterPasswordChars: CharArray? = null
}

@Composable
fun MasterPasswordSetupScreen(
    navController: NavController,
    viewModel: MasterPasswordSetupViewModel = hiltViewModel(),
) {
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = "Create Master Password",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your Master Password is used to locally derive your vault's encryption key. It is never sent to our servers.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceElevated),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = NovaColors.Warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = " If you lose your Master Password and do not have your Recovery Kit, your vault CANNOT be decrypted. Elvarix has zero knowledge of your secrets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            NovaTextField(
                value = viewModel.masterPassword,
                onValueChange = {
                    viewModel.masterPassword = it
                    viewModel.errorMessage = null
                },
                label = "Master Password",
                isPassword = true,
                leadingIcon = Icons.Default.Lock,
                isError = viewModel.errorMessage != null,
                errorMessage = viewModel.errorMessage,
            )

            Spacer(modifier = Modifier.height(12.dp))

            PasswordStrengthIndicator(strength = viewModel.strength)

            Spacer(modifier = Modifier.height(16.dp))

            NovaTextField(
                value = viewModel.confirmPassword,
                onValueChange = {
                    viewModel.confirmPassword = it
                    viewModel.errorMessage = null
                },
                label = "Confirm Master Password",
                isPassword = true,
                leadingIcon = Icons.Default.Lock,
            )

            Spacer(modifier = Modifier.height(32.dp))

            NovaButton(
                text = "Continue to Recovery Kit",
                onClick = {
                    if (viewModel.validate()) {
                        SetupSessionHolder.masterPasswordChars = viewModel.masterPassword.toCharArray()
                        navController.navigate(NavRoute.RecoveryKitSetup.route)
                    }
                },
                enabled = viewModel.masterPassword.isNotBlank() && viewModel.confirmPassword.isNotBlank(),
            )
        }
    }
}
