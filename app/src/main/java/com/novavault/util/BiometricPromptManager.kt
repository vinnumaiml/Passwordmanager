package com.novavault.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricPromptManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface BiometricResult {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : BiometricResult
        data class Error(val errorCode: Int, val errString: CharSequence) : BiometricResult
        data object Failed : BiometricResult
        data object HardwareUnavailable : BiometricResult
        data object NoneEnrolled : BiometricResult
        data object NotAvailable : BiometricResult
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt(
        activity: FragmentActivity,
        title: String = "Unlock Nova Vault",
        subtitle: String = "Use your biometric or device credential",
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        onResult: (BiometricResult) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success(result.cryptoObject))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> onResult(BiometricResult.HardwareUnavailable)
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> onResult(BiometricResult.NoneEnrolled)
                    else -> onResult(BiometricResult.Error(errorCode, errString))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricResult.Failed)
            }
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (cryptoObject == null) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            promptInfoBuilder.setNegativeButtonText("Use Master Password")
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        if (cryptoObject != null) {
            prompt.authenticate(promptInfoBuilder.build(), cryptoObject)
        } else {
            prompt.authenticate(promptInfoBuilder.build())
        }
    }
}
