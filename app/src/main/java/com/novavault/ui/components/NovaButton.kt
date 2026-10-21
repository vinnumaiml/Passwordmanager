package com.novavault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaShapes
import com.novavault.ui.theme.NovaTypography

// ---------------------------------------------------------------------------
// Gradient brush shared by primary buttons
// ---------------------------------------------------------------------------

private val PrimaryGradient = Brush.horizontalGradient(
    listOf(NovaColors.PrimaryAccent, NovaColors.PrimaryAccentDim),
)

private val DangerGradient = Brush.horizontalGradient(
    listOf(NovaColors.Danger, Color(0xFFCC2F25)),
)

// ---------------------------------------------------------------------------
// Primary filled button
// ---------------------------------------------------------------------------

/**
 * Filled gradient button — primary CTA throughout NOVA VAULT.
 *
 * @param text     Button label.
 * @param onClick  Click handler.
 * @param modifier Standard modifier chain.
 * @param enabled  Whether the button accepts input.
 * @param loading  When `true`, replaces the label/icon with a spinner.
 * @param icon     Optional leading icon shown to the left of the label.
 */
@Composable
fun NovaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val isEnabled = enabled && !loading

    Button(
        onClick       = onClick,
        enabled       = isEnabled,
        modifier      = modifier.height(52.dp),
        shape         = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors        = ButtonDefaults.buttonColors(
            containerColor         = Color.Transparent,
            contentColor           = NovaColors.TextPrimary,
            disabledContainerColor = NovaColors.SurfaceElevated,
            disabledContentColor   = NovaColors.TextHint,
        ),
        // Paint the gradient as a background so the Button container is transparent
        // and the gradient fills behind the content via Modifier.background
    ) {
        Box(
            modifier        = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isEnabled) PrimaryGradient
                    else Brush.horizontalGradient(
                        listOf(NovaColors.SurfaceElevated, NovaColors.SurfaceElevated)
                    ),
                    shape = NovaShapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Spinner overlay
            androidx.compose.animation.AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(22.dp),
                    color     = NovaColors.TextPrimary,
                    strokeWidth = 2.dp,
                )
            }
            // Label + optional icon
            androidx.compose.animation.AnimatedVisibility(visible = !loading, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                            tint               = NovaColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text  = text,
                        style = NovaTypography.labelLarge,
                        color = if (isEnabled) NovaColors.TextPrimary else NovaColors.TextHint,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Outlined secondary button
// ---------------------------------------------------------------------------

/**
 * Outlined button — secondary actions and de-emphasised CTAs.
 *
 * @param text    Button label.
 * @param onClick Click handler.
 * @param modifier Standard modifier chain.
 * @param enabled Whether the button accepts input.
 * @param icon    Optional leading icon.
 */
@Composable
fun NovaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick        = onClick,
        enabled        = enabled,
        modifier       = modifier.height(52.dp),
        shape          = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors         = ButtonDefaults.outlinedButtonColors(
            contentColor         = NovaColors.PrimaryAccent,
            disabledContentColor = NovaColors.TextHint,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = if (enabled)
                Brush.horizontalGradient(listOf(NovaColors.PrimaryAccent, NovaColors.SecondaryAccent))
            else
                Brush.horizontalGradient(listOf(NovaColors.Border, NovaColors.Border)),
        ),
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text  = text,
                style = NovaTypography.labelLarge,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Danger / destructive button
// ---------------------------------------------------------------------------

/**
 * Danger button — used exclusively for destructive, irreversible actions
 * (delete vault, wipe all data, etc.).
 *
 * Renders with a red gradient background to communicate risk clearly.
 *
 * @param text    Button label.
 * @param onClick Click handler.
 * @param modifier Standard modifier chain.
 * @param enabled Whether the button accepts input.
 */
@Composable
fun NovaDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick        = onClick,
        enabled        = enabled,
        modifier       = modifier.height(52.dp),
        shape          = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors         = ButtonDefaults.buttonColors(
            containerColor         = Color.Transparent,
            contentColor           = NovaColors.TextPrimary,
            disabledContainerColor = NovaColors.SurfaceElevated,
            disabledContentColor   = NovaColors.TextHint,
        ),
    ) {
        Box(
            modifier        = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (enabled) DangerGradient
                    else Brush.horizontalGradient(
                        listOf(NovaColors.SurfaceElevated, NovaColors.SurfaceElevated)
                    ),
                    shape = NovaShapes.medium,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = text,
                style = NovaTypography.labelLarge,
                color = if (enabled) NovaColors.TextPrimary else NovaColors.TextHint,
            )
        }
    }
}
