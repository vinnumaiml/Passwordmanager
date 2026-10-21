package com.novavault.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * The single Material 3 dark color scheme for NOVA VAULT.
 *
 * Every M3 slot is deliberately mapped to our palette — dynamic color is
 * always disabled so we own every pixel.
 */
private val NovaDarkColorScheme = darkColorScheme(
    // Primary family — violet
    primary            = NovaColors.PrimaryAccent,
    onPrimary          = NovaColors.TextPrimary,
    primaryContainer   = NovaColors.PrimaryAccentDim,
    onPrimaryContainer = NovaColors.TextPrimary,

    // Secondary family — cyan/teal
    secondary            = NovaColors.SecondaryAccent,
    onSecondary          = NovaColors.Background,
    secondaryContainer   = NovaColors.SecondaryDim,
    onSecondaryContainer = NovaColors.TextPrimary,

    // Tertiary family — reuse indigo aurora stop for decorative slots
    tertiary            = NovaColors.AuroraIndigo,
    onTertiary          = NovaColors.TextPrimary,
    tertiaryContainer   = NovaColors.SurfaceElevated,
    onTertiaryContainer = NovaColors.TextSecondary,

    // Error family
    error            = NovaColors.Danger,
    onError          = NovaColors.TextPrimary,
    errorContainer   = NovaColors.DangerDim,
    onErrorContainer = NovaColors.Danger,

    // Background / surface stack
    background            = NovaColors.Background,
    onBackground          = NovaColors.TextPrimary,
    surface               = NovaColors.Surface,
    onSurface             = NovaColors.TextPrimary,
    surfaceVariant        = NovaColors.SurfaceVariant,
    onSurfaceVariant      = NovaColors.TextSecondary,
    surfaceTint           = NovaColors.PrimaryAccent,

    // Inverse (used by Snackbar, tooltips, etc.)
    inverseSurface        = NovaColors.TextPrimary,
    inverseOnSurface      = NovaColors.Background,
    inversePrimary        = NovaColors.PrimaryAccentDim,

    // Chrome
    outline               = NovaColors.Border,
    outlineVariant        = NovaColors.Divider,
    scrim                 = NovaColors.Background,
)

/**
 * Root theme composable for NOVA VAULT.
 *
 * @param reduceMotion When `true` all decorative animations are disabled.
 *   Defaults to `false`; callers should derive this from
 *   `LocalAccessibilityManager` or a user preference.
 * @param content Screen content.
 */
@Composable
fun NovaVaultTheme(
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    // Apply edge-to-edge and set transparent system bars inside a SideEffect
    // so it only runs when the composition reaches the Activity window.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Extend content behind system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Make status bar / nav bar transparent so the aurora bleeds through
            @Suppress("DEPRECATION")
            window.statusBarColor = NovaColors.Background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = NovaColors.Background.toArgb()

            // Use light (white) icons on our dark background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalReducedMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = NovaDarkColorScheme,
            typography  = NovaTypography,
            shapes      = NovaShapes,
            content     = content,
        )
    }
}
