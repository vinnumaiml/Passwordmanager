package com.novavault.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novavault.BuildConfig
import com.novavault.domain.model.AuthState
import com.novavault.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "elvarix_auth")

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: Auth,
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private object Keys {
        val LOCAL_USER_EMAIL = stringPreferencesKey("user_email")
        val LOCAL_USER_ID = stringPreferencesKey("user_id")
        val EMAIL_VERIFIED = stringPreferencesKey("email_verified")
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    init {
        scope.launch {
            initAuthState()
            observeSupabaseSession()
        }
    }

    private suspend fun initAuthState() {
        val prefs = context.authDataStore.data.first()
        val email = prefs[Keys.LOCAL_USER_EMAIL]
        val userId = prefs[Keys.LOCAL_USER_ID]
        val verified = prefs[Keys.EMAIL_VERIFIED] == "true"

        if (!email.isNullOrBlank() && !userId.isNullOrBlank()) {
            if (verified) {
                _authState.value = AuthState.Authenticated(userId, email)
            } else {
                _authState.value = AuthState.EmailNotVerified(email)
            }
        } else {
            // Check existing Supabase session
            try {
                val currentSession = auth.currentSessionOrNull()
                val user = auth.currentUserOrNull()
                if (currentSession != null && user != null) {
                    if (user.emailConfirmedAt != null) {
                        saveSessionToDataStore(user.id, user.email ?: "", isVerified = true)
                        _authState.value = AuthState.Authenticated(user.id, user.email ?: "")
                    } else {
                        saveSessionToDataStore(user.id, user.email ?: "", isVerified = false)
                        _authState.value = AuthState.EmailNotVerified(user.email ?: "")
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun observeSupabaseSession() {
        scope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = auth.currentUserOrNull()
                        if (user != null) {
                            if (user.emailConfirmedAt != null) {
                                saveSessionToDataStore(user.id, user.email ?: "", isVerified = true)
                                _authState.value = AuthState.Authenticated(user.id, user.email ?: "")
                            } else {
                                saveSessionToDataStore(user.id, user.email ?: "", isVerified = false)
                                _authState.value = AuthState.EmailNotVerified(user.email ?: "")
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        val prefs = context.authDataStore.data.first()
                        val email = prefs[Keys.LOCAL_USER_EMAIL]
                        if (email.isNullOrBlank()) {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    else -> {
                        // Initializing, RefreshFailure, or other statuses: maintain cached state
                    }
                }
            }
        }
    }

    private suspend fun saveSessionToDataStore(userId: String, email: String, isVerified: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.LOCAL_USER_EMAIL] = email
            prefs[Keys.LOCAL_USER_ID] = userId
            prefs[Keys.EMAIL_VERIFIED] = isVerified.toString()
        }
    }

    override fun isBackendConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL
        return url.isNotBlank() && !url.contains("placeholder") && !url.contains("localhost")
    }

    override suspend fun activateSovereignMode(): Result<Unit> {
        val prefs = context.authDataStore.data.first()
        val email = prefs[Keys.LOCAL_USER_EMAIL] ?: "sovereign@elvarix.vault"
        val userId = prefs[Keys.LOCAL_USER_ID] ?: UUID.randomUUID().toString()
        saveSessionToDataStore(userId, email, isVerified = true)
        _authState.value = AuthState.Authenticated(userId, email)
        return Result.success(Unit)
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            var userId: String? = null
            var remoteConfirmed = false

            if (isBackendConfigured()) {
                try {
                    auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    val user = auth.currentUserOrNull()
                    userId = user?.id
                    remoteConfirmed = user?.emailConfirmedAt != null
                } catch (e: Exception) {
                    return Result.failure(e)
                }
            } else {
                // Standalone Sovereign Mode: backend not configured (placeholder credentials)
                userId = UUID.randomUUID().toString()
                remoteConfirmed = true // Instant activation in sovereign offline mode
            }

            val finalUserId = userId ?: UUID.randomUUID().toString()
            saveSessionToDataStore(finalUserId, email, isVerified = remoteConfirmed)

            if (remoteConfirmed) {
                _authState.value = AuthState.Authenticated(finalUserId, email)
            } else {
                _authState.value = AuthState.EmailNotVerified(email)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            var remoteConfirmed = false
            var userId: String? = null

            try {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val user = auth.currentUserOrNull()
                userId = user?.id
                remoteConfirmed = user?.emailConfirmedAt != null
            } catch (e: Exception) {
                // Check if existing local session matches
                val prefs = context.authDataStore.data.first()
                val localEmail = prefs[Keys.LOCAL_USER_EMAIL]
                val localVerified = prefs[Keys.EMAIL_VERIFIED] == "true"
                if (localEmail.equals(email, ignoreCase = true)) {
                    userId = prefs[Keys.LOCAL_USER_ID]
                    remoteConfirmed = localVerified
                } else {
                    throw e
                }
            }

            val finalUserId = userId ?: UUID.randomUUID().toString()
            saveSessionToDataStore(finalUserId, email, isVerified = remoteConfirmed)

            if (remoteConfirmed) {
                _authState.value = AuthState.Authenticated(finalUserId, email)
            } else {
                _authState.value = AuthState.EmailNotVerified(email)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            try {
                auth.signOut()
            } catch (ignored: Exception) {}

            context.authDataStore.edit { prefs ->
                prefs.clear()
            }
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        if (!isBackendConfigured()) {
            return Result.failure(IllegalStateException("Cloud backend not connected (using placeholder URL). Please configure a real Supabase project in local.properties or continue in Sovereign Mode."))
        }
        return try {
            val prefs = context.authDataStore.data.first()
            val email = prefs[Keys.LOCAL_USER_EMAIL] ?: auth.currentUserOrNull()?.email
            if (email.isNullOrBlank()) {
                return Result.failure(IllegalStateException("No registered email address found."))
            }
            auth.resendEmail(OtpType.Email.SIGNUP, email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            try {
                auth.resetPasswordForEmail(email)
            } catch (ignored: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return signOut()
    }

    override suspend fun refreshSession(): Result<Unit> {
        if (!isBackendConfigured()) {
            return activateSovereignMode()
        }
        return try {
            var confirmed = false
            var currentEmail: String? = null
            var currentUserId: String? = null

            try {
                auth.refreshCurrentSession()
                val user = auth.currentUserOrNull()
                if (user != null) {
                    currentEmail = user.email
                    currentUserId = user.id
                    confirmed = user.emailConfirmedAt != null
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }

            if (confirmed && currentEmail != null && currentUserId != null) {
                saveSessionToDataStore(currentUserId, currentEmail, isVerified = true)
                _authState.value = AuthState.Authenticated(currentUserId, currentEmail)
                Result.success(Unit)
            } else {
                val prefs = context.authDataStore.data.first()
                val email = prefs[Keys.LOCAL_USER_EMAIL] ?: ""
                _authState.value = AuthState.EmailNotVerified(email)
                Result.failure(IllegalStateException("Email has not been verified yet on the server. Please click the confirmation link in your email."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isSessionValid(): Boolean {
        return _authState.value is AuthState.Authenticated
    }
}
