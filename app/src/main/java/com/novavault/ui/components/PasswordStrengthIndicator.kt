package com.novavault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaTypography

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private val StrengthColors = listOf(
    NovaColors.StrengthVeryWeak,
    NovaColors.StrengthWeak,
    NovaColors.StrengthFair,
    NovaColors.StrengthStrong,
    NovaColors.StrengthVeryStrong,
)

private fun PasswordStrength.segmentsFilled(): Int = when (this) {
    PasswordStrength.VERY_WEAK   -> 1
    PasswordStrength.WEAK        -> 2
    PasswordStrength.FAIR        -> 3
    PasswordStrength.STRONG      -> 4
    PasswordStrength.VERY_STRONG -> 5
}

private fun PasswordStrength.label(): String = when (this) {
    PasswordStrength.VERY_WEAK   -> "Very Weak"
    PasswordStrength.WEAK        -> "Weak"
    PasswordStrength.FAIR        -> "Fair"
    PasswordStrength.STRONG      -> "Strong"
    PasswordStrength.VERY_STRONG -> "Very Strong"
}

private fun PasswordStrength.accentColor(): Color = StrengthColors[segmentsFilled() - 1]

// ---------------------------------------------------------------------------
// Public composable
// ---------------------------------------------------------------------------

/**
 * Segmented horizontal password-strength indicator.
 *
 * Renders 5 equal-width bar segments whose fill and colour animate smoothly
 * whenever [strength] changes.  A descriptive label is shown below the bar.
 *
 * @param strength Current [PasswordStrength] level.
 * @param modifier Applied to the outermost [Column].
 */
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val filled = strength.segmentsFilled()
    val accentColor = strength.accentColor()

    Column(modifier = modifier) {
        // -----------------------------------------------------------------------
        // Segmented bar
        // -----------------------------------------------------------------------
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (index in 1..5) {
                val targetFill = if (index <= filled) 1f else 0f

                val fillFraction by animateFloatAsState(
                    targetValue   = targetFill,
                    animationSpec = tween(durationMillis = 300, delayMillis = index * 40),
                    label         = "strength_segment_$index",
                )

                // Interpolate segment colour from dim background to accent
                val segmentColor = lerp(
                    start = NovaColors.SurfaceElevated,
                    stop  = accentColor,
                    fraction = fillFraction,
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NovaColors.SurfaceElevated),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillFraction)
                            .background(segmentColor),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // -----------------------------------------------------------------------
        // Label
        // -----------------------------------------------------------------------
        Text(
            text  = strength.label(),
            style = NovaTypography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
