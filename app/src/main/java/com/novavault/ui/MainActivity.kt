package com.novavault.ui

import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.novavault.ui.navigation.NovaVaultNavGraph
import com.novavault.ui.theme.NovaVaultTheme
import com.novavault.util.BiometricPromptManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalBiometricPromptManager = staticCompositionLocalOf<BiometricPromptManager> {
    error("BiometricPromptManager not provided")
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var biometricPromptManager: BiometricPromptManager

    private lateinit var navViewModel: NavViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Defer splash screen removal until auth state has been resolved
        splashScreen.setKeepOnScreenCondition {
            // NavViewModel exposes a `isSplashReady` StateFlow; keep splash while false
            runCatching { !navViewModel.isSplashReady.value }.getOrDefault(true)
        }

        // Edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            navViewModel = hiltViewModel()

            // Observe screenshot-protection setting
            val screenshotProtectionEnabled by navViewModel.screenshotProtectionEnabled
                .collectAsState(initial = false)

            LaunchedEffect(screenshotProtectionEnabled) {
                if (screenshotProtectionEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Detect reduced-motion preference
            val accessibilityManager =
                getSystemService(android.view.accessibility.AccessibilityManager::class.java)
            val animationScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            )
            val reducedMotion = animationScale == 0f ||
                    (accessibilityManager?.isEnabled == true &&
                            accessibilityManager.isTouchExplorationEnabled)

            // Lifecycle observer — auto-lock vault when app goes to background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        navViewModel.onAppBackgrounded()
                    }
                    override fun onStart(owner: LifecycleOwner) {
                        navViewModel.onAppForegrounded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            CompositionLocalProvider(
                LocalReducedMotion provides reducedMotion,
                LocalBiometricPromptManager provides biometricPromptManager,
            ) {
                NovaVaultTheme {
                    NovaVaultNavGraph(navViewModel = navViewModel)
                }
            }
        }
    }
}
