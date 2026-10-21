package com.novavault.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.AuthRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.isValidEmail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var email by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var successMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    fun sendReset(onSuccess: () -> Unit) {
        if (!email.isValidEmail()) {
            errorMessage = "Please enter a valid email address."
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            isLoading = false
            if (result.isSuccess) {
                successMessage = "Password reset instructions sent to $email."
                onSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to send reset email."
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = NovaColors.TextPrimary,
                )
            }

            Text(
                text = "Reset Account Password",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your registered email address to receive password reset instructions.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                        imageVector = Icons.Default.Info,
                        contentDescription = "Important Note",
                        tint = NovaColors.Warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = " Resetting your account password does NOT automatically decrypt or reset your vault. Your vault is protected by a separate Master Password that only you know.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            NovaTextField(
                value = viewModel.email,
                onValueChange = {
                    viewModel.email = it
                    viewModel.errorMessage = null
                },
                label = "Account Email",
                leadingIcon = Icons.Default.Email,
                isError = viewModel.errorMessage != null,
                errorMessage = viewModel.errorMessage,
            )

            if (viewModel.successMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.successMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaColors.Success,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            NovaButton(
                text = "Send Reset Link",
                onClick = { viewModel.sendReset {} },
                loading = viewModel.isLoading,
                enabled = viewModel.email.isNotBlank() && !viewModel.isLoading,
            )
        }
    }
}
