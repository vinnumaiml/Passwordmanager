package com.novavault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaShapes

/**
 * Extension that draws a coloured soft-glow shadow behind any composable.
 *
 * Uses [drawIntoCanvas] + a blur layer so it appears as an ambient glow
 * rather than a hard-edged drop-shadow, giving the glassmorphic look.
 */
private fun Modifier.coloredShadow(
    color: Color,
    borderRadius: Dp = 20.dp,
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 2.dp,
): Modifier = this.drawBehind {
    this.drawIntoCanvas {
        val paint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                this.color = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    blurRadius.toPx(),
                    offsetX.toPx(),
                    offsetY.toPx(),
                    color.copy(alpha = 0.40f).toArgb(),
                )
            }
        }
        it.drawRoundRect(
            left   = spread.toPx(),
            top    = spread.toPx(),
            right  = size.width  - spread.toPx(),
            bottom = size.height - spread.toPx(),
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint  = paint,
        )
    }
}

/**
 * Glassmorphic card used throughout NOVA VAULT.
 *
 * Features:
 *  • [NovaColors.SurfaceVariant] background for that frosted-glass feel.
 *  • A translucent [NovaColors.Border] stroke (0.5 dp).
 *  • A soft ambient glow matching [glowColor] drawn via [coloredShadow].
 *  • Optional click handling with a ripple confined to the card shape.
 *
 * @param modifier   Standard modifier chain.
 * @param onClick    If non-null the card becomes clickable with a ripple.
 * @param glowColor  Accent color of the ambient glow (default: PrimaryAccent).
 * @param content    Card body, laid out in a [Column].
 */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color = NovaColors.PrimaryAccent,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication        = androidx.compose.material3.ripple(color = glowColor.copy(alpha = 0.20f)),
            onClick           = onClick,
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .coloredShadow(
                color        = glowColor,
                borderRadius = 20.dp,
                blurRadius   = 18.dp,
                spread       = 1.dp,
            )
            .then(clickableModifier),
        shape  = NovaShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = NovaColors.SurfaceVariant,
            contentColor   = NovaColors.TextPrimary,
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = NovaColors.Border,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content,
        )
    }
}
