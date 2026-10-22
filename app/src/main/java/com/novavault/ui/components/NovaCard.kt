package com.novavault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.ElvarixColors
import com.novavault.ui.theme.NovaColors

/**
 * Precision Card container for Elvarix.
 *
 * Implements a refined, tangible surface with subtle borders and intentional hierarchy,
 * avoiding loud saturated neon glows.
 */
@Composable
fun NovaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = ElvarixColors.Border,
    glowColor: Color = ElvarixColors.Border,
    containerColor: Color = ElvarixColors.SurfaceVariant,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication        = androidx.compose.material3.ripple(color = ElvarixColors.PrimaryAccent.copy(alpha = 0.15f)),
            onClick           = onClick,
        )
    } else {
        Modifier
    }

    val effectiveBorderColor = if (borderColor != ElvarixColors.Border) borderColor else if (glowColor != ElvarixColors.Border) glowColor else ElvarixColors.Border

    Card(
        modifier = modifier.then(clickableModifier),
        shape  = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor   = ElvarixColors.TextPrimary,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = effectiveBorderColor,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content  = content,
        )
    }
}
