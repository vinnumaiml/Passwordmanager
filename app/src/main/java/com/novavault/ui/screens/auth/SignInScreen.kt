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
import com.novavault.domain.repository.AuthRepository
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
    data class Success(val needsMasterPasswordSetup: Boolean) : SignInUiState()
    data class Error(val message: String) : SignInUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.value = SignInUiState.Error("Please enter a valid email address.")
            return
        }
        if (password.isBlank()) {
            _uiState.value = SignInUiState.Error("Password cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = SignInUiState.Loading
            val result = authRepository.signIn(email.trim(), password)
            if (result.isSuccess) {
                _uiState.value = SignInUiState.Success(needsMasterPasswordSetup = false)
            } else {
                _uiState.value = SignInUiState.Error(
                    result.exceptionOrNull()?.message ?: "Sign in failed. Please check your credentials."
                )
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

    // Navigate on success
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SignInUiState.Success -> {
                if (state.needsMasterPasswordSetup) {
                    navController.navigate(NavRoute.MasterPasswordSetup.route) {
                        popUpTo(NavRoute.SignIn.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                // Otherwise NavViewModel's authState collector in NavGraph handles routing
            }
            else -> Unit
        }
    }

    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF0A0A1A), Color(0xFF0D1B3E))
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

            // Logo + Title
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Nova Vault",
                tint = Color(0xFF5B7FFF),
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "NOVA VAULT",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Text(
                text = "Welcome back",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
            )

            Spacer(modifier = Modifier.height(40.dp))

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
                Column(modifier = Modifier.padding(24.dp)) {
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF5B7FFF)) },
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

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF5B7FFF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide" else "Show",
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

                    // Forgot password
                    TextButton(
                        onClick = { navController.navigate(NavRoute.ForgotPassword.route) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Forgot Password?", color = Color(0xFF5B7FFF), fontSize = 13.sp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B7FFF)),
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
                    Text("Create Account", color = Color(0xFF5B7FFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ─── Shared text field colors ──────────────────────────────────────────────────

@Composable
internal fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
    focusedBorderColor = Color(0xFF5B7FFF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    cursorColor = Color(0xFF5B7FFF),
    focusedLabelColor = Color(0xFF5B7FFF),
    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
)
