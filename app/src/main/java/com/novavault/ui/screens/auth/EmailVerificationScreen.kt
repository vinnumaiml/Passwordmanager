package com.novavault.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.AuthState
import com.novavault.domain.repository.AuthRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaCard
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var isChecking by mutableStateOf(false)
    var isResending by mutableStateOf(false)
    var resendCooldown by mutableIntStateOf(0)
    var statusMessage by mutableStateOf<String?>(null)
    var isError by mutableStateOf(false)

    val currentEmail: StateFlow<String> = authRepository.authState
        .map { state ->
            when (state) {
                is AuthState.EmailNotVerified -> state.email
                is AuthState.Authenticated -> state.email
                else -> ""
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = "",
        )

    fun maskEmail(email: String): String {
        if (!email.contains("@")) return email
        val parts = email.split("@")
        val name = parts[0]
        val domain = parts[1]
        val maskedName = if (name.length <= 2) {
            "${name.first()}*"
        } else {
            "${name.take(2)}${"*".repeat((name.length - 2).coerceAtMost(5))}"
        }
        return "$maskedName@$domain"
    }

    fun startCooldown(seconds: Int = 60) {
        resendCooldown = seconds
        viewModelScope.launch {
            while (resendCooldown > 0) {
                delay(1000)
                resendCooldown--
            }
        }
    }

    fun resendEmail() {
        if (resendCooldown > 0 || isResending) return
        isResending = true
        isError = false
        statusMessage = null

        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()
            isResending = false
            if (result.isSuccess) {
                statusMessage = "Verification link resent. Please check your inbox and spam folder."
                startCooldown(60)
            } else {
                isError = true
                statusMessage = "Failed to resend email: ${result.exceptionOrNull()?.localizedMessage ?: "Please try again later."}"
            }
        }
    }

    fun checkVerification(onVerified: () -> Unit) {
        if (isChecking) return
        isChecking = true
        isError = false
        statusMessage = null

        viewModelScope.launch {
            val result = authRepository.refreshSession()
            isChecking = false

            if (result.isSuccess && authRepository.isSessionValid()) {
                onVerified()
            } else {
                isError = true
                statusMessage = "Email is not yet verified on the server. Please click the activation link in your email and tap Check Status again."
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
    val email by viewModel.currentEmail.collectAsState()

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Futuristic Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = NovaColors.PrimaryAccentDim.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.SecondaryAccent.copy(alpha = 0.4f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = "Verify Email",
                        tint = NovaColors.SecondaryAccent,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Check Your Inbox",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (email.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NovaColors.SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NovaColors.Border),
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = viewModel.maskEmail(email),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NovaColors.SecondaryAccent,
                            letterSpacing = 0.5.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "We sent a secure verification link to confirm your identity. To protect your sovereign vault, you must verify your email before continuing.",
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Spam warning card
            NovaCard(
                glowColor = NovaColors.SecondaryAccent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NovaColors.Warning,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Don\'t see the email? Please check your Spam or Junk folder. It typically arrives within 1-2 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                        lineHeight = 18.sp,
                    )
                }
            }

            // Status or Error Message
            AnimatedVisibility(
                visible = viewModel.statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (viewModel.isError) NovaColors.DangerDim else NovaColors.SuccessDim,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (viewModel.isError) NovaColors.Danger else NovaColors.Success,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = viewModel.statusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (viewModel.isError) NovaColors.Danger else NovaColors.Success,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Check Status Button (Authoritative Verification Query)
            NovaButton(
                text = if (viewModel.isChecking) "Checking Status…" else "I\'ve Clicked the Verification Link",
                loading = viewModel.isChecking,
                onClick = {
                    viewModel.checkVerification {
                        navController.navigate(NavRoute.MasterPasswordSetup.route) {
                            popUpTo(NavRoute.EmailVerification.route) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resend with Cooldown
            TextButton(
                onClick = { viewModel.resendEmail() },
                enabled = viewModel.resendCooldown == 0 && !viewModel.isResending,
            ) {
                Text(
                    text = when {
                        viewModel.isResending -> "Sending…"
                        viewModel.resendCooldown > 0 -> "Resend email in ${viewModel.resendCooldown}s"
                        else -> "Resend Verification Email"
                    },
                    color = if (viewModel.resendCooldown > 0) NovaColors.TextHint else NovaColors.SecondaryAccent,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sign out / return to login
            TextButton(
                onClick = {
                    viewModel.signOut {
                        navController.navigate(NavRoute.SignIn.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            ) {
                Text(
                    text = "Sign Out / Change Email",
                    color = NovaColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
