package com.novavault.ui.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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

sealed class SignUpUiState {
    data object Idle : SignUpUiState()
    data object Loading : SignUpUiState()
    data object Success : SignUpUiState()
    data class Error(val message: String) : SignUpUiState()
}

private fun passwordStrength(password: String): Float {
    if (password.isEmpty()) return 0f
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score / 5f
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun signUp(name: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean) {
        if (!termsAccepted) { _uiState.value = SignUpUiState.Error("You must accept the Terms of Service."); return }
        if (email.isBlank() || !email.contains("@")) { _uiState.value = SignUpUiState.Error("Please enter a valid email address."); return }
        if (password.length < 8) { _uiState.value = SignUpUiState.Error("Password must be at least 8 characters."); return }
        if (password != confirmPassword) { _uiState.value = SignUpUiState.Error("Passwords do not match."); return }

        viewModelScope.launch {
            _uiState.value = SignUpUiState.Loading
            val result = authRepository.signUp(email.trim(), password)
            if (result.isSuccess) {
                _uiState.value = SignUpUiState.Success
            } else {
                _uiState.value = SignUpUiState.Error(result.exceptionOrNull()?.message ?: "Sign up failed. Please try again.")
            }
        }
    }

    fun clearError() { if (_uiState.value is SignUpUiState.Error) _uiState.value = SignUpUiState.Idle }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: SignUpViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    val strength = remember(password) { passwordStrength(password) }
    val strengthColor = when {
        strength < 0.4f -> Color(0xFFFF5252)
        strength < 0.7f -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }
    val strengthLabel = when {
        strength == 0f -> ""
        strength < 0.4f -> "Weak"
        strength < 0.7f -> "Moderate"
        else -> "Strong"
    }

    LaunchedEffect(uiState) {
        if (uiState is SignUpUiState.Success) {
            navController.navigate(NavRoute.EmailVerification.route) {
                popUpTo(NavRoute.SignUp.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF0D1B3E))))
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

            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF5B7FFF), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Create Account", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Join Elvarix — Sovereign Privacy", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is SignUpUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(text = (uiState as SignUpUiState.Error).message, color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Name (optional)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF5B7FFF)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = authTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF5B7FFF)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = authTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF5B7FFF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = authTextFieldColors(),
                    )

                    // Strength indicator
                    if (password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { strength },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = strengthColor,
                            trackColor = Color.White.copy(alpha = 0.15f),
                        )
                        Text(text = strengthLabel, color = strengthColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearError() },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF5B7FFF)) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        colors = authTextFieldColors(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Terms checkbox
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
                        Text("I agree to the Terms of Service and Privacy Policy", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.signUp(name, email, password, confirmPassword, termsAccepted)
                        },
                        enabled = uiState !is SignUpUiState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B7FFF)),
                    ) {
                        if (uiState is SignUpUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account? ", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                TextButton(onClick = { navController.navigateUp() }) {
                    Text("Sign In", color = Color(0xFF5B7FFF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
