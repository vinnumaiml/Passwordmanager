package com.novavault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaShapes
import com.novavault.ui.theme.NovaTypography

// ---------------------------------------------------------------------------
// Primary filled button — High contrast solid violet CTA
// ---------------------------------------------------------------------------

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
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.height(50.dp),
        shape = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NovaColors.PrimaryAccent,
            contentColor = NovaColors.Background,
            disabledContainerColor = NovaColors.SurfaceElevated,
            disabledContentColor = NovaColors.TextHint,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NovaColors.Background,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isEnabled) NovaColors.Background else NovaColors.TextHint,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = NovaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isEnabled) NovaColors.Background else NovaColors.TextHint,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Outlined secondary button — Clean surface with subtle 1dp border
// ---------------------------------------------------------------------------

@Composable
fun NovaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NovaColors.SurfaceVariant,
            contentColor = NovaColors.TextPrimary,
            disabledContainerColor = NovaColors.SurfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = NovaColors.TextHint,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) NovaColors.Border else NovaColors.Border.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) NovaColors.TextPrimary else NovaColors.TextHint,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = NovaTypography.labelLarge,
                color = if (enabled) NovaColors.TextPrimary else NovaColors.TextHint,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Danger / destructive button — High contrast red CTA
// ---------------------------------------------------------------------------

@Composable
fun NovaDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = NovaShapes.medium,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NovaColors.Danger,
            contentColor = Color.White,
            disabledContainerColor = NovaColors.SurfaceElevated,
            disabledContentColor = NovaColors.TextHint,
        ),
    ) {
        Text(
            text = text,
            style = NovaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) Color.White else NovaColors.TextHint,
        )
    }
}
