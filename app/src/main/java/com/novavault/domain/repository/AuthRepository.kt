package com.novavault.domain.repository

import com.novavault.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun refreshSession(): Result<Unit>
    fun isSessionValid(): Boolean
    fun isBackendConfigured(): Boolean
    suspend fun activateSovereignMode(): Result<Unit>
}
