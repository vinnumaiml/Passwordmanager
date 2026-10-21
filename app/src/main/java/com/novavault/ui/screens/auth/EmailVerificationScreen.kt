package com.novavault.ui.screens.auth

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
import androidx.compose.material.icons.filled.MarkEmailRead
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.AuthRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    var isResending by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)

    fun resendEmail() {
        isResending = true
        viewModelScope.launch {
            authRepository.sendEmailVerification()
            isResending = false
            statusMessage = "Verification link resent. Please check your inbox."
        }
    }

    fun checkVerification(onVerified: () -> Unit) {
        viewModelScope.launch {
            authRepository.refreshSession()
            if (authRepository.isSessionValid()) {
                onVerified()
            } else {
                statusMessage = "Email not yet verified. Please click the link sent to your inbox."
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}

@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: EmailVerificationViewModel = hiltViewModel(),
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
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = "Verify Email",
                tint = NovaColors.PrimaryAccent,
                modifier = Modifier.size(72.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium,
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "We sent a verification link to your email address. Please click the link to confirm your account and continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            if (viewModel.statusMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.statusMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaColors.SecondaryAccent,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            NovaButton(
                text = "I've Verified My Email",
                onClick = {
                    viewModel.checkVerification {
                        navController.navigate(NavRoute.MasterPasswordSetup.route) {
                            popUpTo(NavRoute.EmailVerification.route) { inclusive = true }
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { viewModel.resendEmail() }) {
                Text(
                    text = if (viewModel.isResending) "Resending..." else "Resend Verification Email",
                    color = NovaColors.PrimaryAccent,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    viewModel.signOut {
                        navController.navigate(NavRoute.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                Text(
                    text = "Sign Out",
                    color = NovaColors.Danger,
                )
            }
        }
    }
}
