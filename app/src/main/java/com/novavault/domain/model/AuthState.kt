package com.novavault.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val userId: String, val email: String) : AuthState
    data class EmailNotVerified(val email: String) : AuthState
    data class Error(val message: String) : AuthState
}
