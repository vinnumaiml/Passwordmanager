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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.AuthState
import com.novavault.domain.repository.AuthRepository
import com.novavault.ui.components.ElvarixCoreSymbol
import com.novavault.ui.navigation.NavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI state ─────────────────────────────────────────────────────────────────

sealed class SignInUiState {
    data object Idle : SignInUiState()
    data object Loading : SignInUiState()
    data class Success(val email: String) : SignInUiState()
    data class EmailNotVerified(val email: String) : SignInUiState()
    data class Error(val message: String) : SignInUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.value = SignInUiState.Success(authState.email)
                    }
                    is AuthState.EmailNotVerified -> {
                        _uiState.value = SignInUiState.EmailNotVerified(authState.email)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _uiState.value = SignInUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = SignInUiState.Error("Please enter your account password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignInUiState.Loading
            val result = authRepository.signIn(trimmedEmail, password)
            if (result.isFailure) {
                val rawMsg = result.exceptionOrNull()?.message ?: "Sign in failed"
                val friendlyMsg = when {
                    rawMsg.contains("Invalid login", ignoreCase = true) ->
                        "Incorrect email or password. Please try again."
                    rawMsg.contains("Email not confirmed", ignoreCase = true) -> {
                        _uiState.value = SignInUiState.EmailNotVerified(trimmedEmail)
                        return@launch
                    }
                    rawMsg.contains("network", ignoreCase = true) || rawMsg.contains("connect", ignoreCase = true) ->
                        "Cannot connect to auth service. Check your internet connection."
                    else -> rawMsg
                }
                _uiState.value = SignInUiState.Error(friendlyMsg)
            }
        }
    }

    fun clearError() {
        if (_uiState.value is SignInUiState.Error) {
            _uiState.value = SignInUiState.Idle
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: SignInViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is SignInUiState.Success -> {
                navController.navigate(NavRoute.MasterPasswordSetup.route) {
                    popUpTo(NavRoute.SignIn.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            is SignInUiState.EmailNotVerified -> {
                navController.navigate(NavRoute.EmailVerification.route) {
                    popUpTo(NavRoute.SignIn.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            else -> Unit
        }
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF080A14),
            Color(0xFF10162A),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // The Elvarix Core Vector Logo
            ElvarixCoreSymbol(size = 64.dp, animatedGlow = true)

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ELVARIX",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Welcome back to Sovereign Privacy",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Error banner
            if (uiState is SignInUiState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = (uiState as SignInUiState.Error).message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // Glass card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.07f),
                ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                        },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF8B5CF6))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = authTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            viewModel.clearError()
                        },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8B5CF6))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.signIn(email, password)
                            }
                        ),
                        colors = authTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot password link
                    TextButton(
                        onClick = { navController.navigate(NavRoute.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot Password?", color = Color(0xFF43E7D0), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign In button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.signIn(email, password)
                        },
                        enabled = uiState !is SignInUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    ) {
                        if (uiState is SignInUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create account link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Don't have an account? ", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                TextButton(onClick = {
                    navController.navigate(NavRoute.SignUp.route) {
                        launchSingleTop = true
                    }
                }) {
                    Text("Create Account", color = Color(0xFF43E7D0), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
    focusedBorderColor = Color(0xFF8B5CF6),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = Color(0xFF8B5CF6),
    focusedLabelColor = Color(0xFF8B5CF6),
    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
)
