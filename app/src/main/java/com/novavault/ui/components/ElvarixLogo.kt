package com.novavault.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavault.ui.theme.NovaColors

/**
 * The Elvarix Core — Geometric Jetpack Compose Vector Logo.
 * Constructs the interlocking orbital ribbons of the futuristic 'E'
 * with its central cryptographic keyway core.
 */
@Composable
fun ElvarixCoreSymbol(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animatedGlow: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ElvarixPulse")
    val pulseAlpha by if (animatedGlow) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "GlowAlpha",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.3f) }
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scaleX = w / 512f
        val scaleY = h / 512f

        val coreGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF8B5CF6), // Primary Violet
                Color(0xFF6366F1), // Cybernetic Indigo
                Color(0xFF43E7D0), // Electric Cyan
            ),
            start = Offset(120f * scaleX, 110f * scaleY),
            end = Offset(420f * scaleX, 400f * scaleY),
        )

        val cyanAccent = Brush.linearGradient(
            colors = listOf(
                Color(0xFF6366F1),
                Color(0xFF43E7D0),
            ),
            start = Offset(260f * scaleX, 180f * scaleY),
            end = Offset(420f * scaleX, 340f * scaleY),
        )

        // 1. Ambient Glow Pass
        val glowPath = Path().apply {
            moveTo(376f * scaleX, 116f * scaleY)
            lineTo(180f * scaleX, 116f * scaleY)
            lineTo(124f * scaleX, 172f * scaleY)
            lineTo(124f * scaleX, 340f * scaleY)
            lineTo(180f * scaleX, 396f * scaleY)
            lineTo(376f * scaleX, 396f * scaleY)
            lineTo(400f * scaleX, 360f * scaleY)
            lineTo(204f * scaleX, 360f * scaleY)
            lineTo(168f * scaleX, 324f * scaleY)
            lineTo(168f * scaleX, 188f * scaleY)
            lineTo(204f * scaleX, 152f * scaleY)
            lineTo(400f * scaleX, 152f * scaleY)
            close()
        }
        drawPath(glowPath, brush = coreGradient, alpha = pulseAlpha, style = Fill)

        // 2. Primary Outer Orbital Ribbon ('E' Spine & Flanges)
        val outerRibbon = Path().apply {
            moveTo(376f * scaleX, 116f * scaleY)
            lineTo(180f * scaleX, 116f * scaleY)
            lineTo(124f * scaleX, 172f * scaleY)
            lineTo(124f * scaleX, 340f * scaleY)
            lineTo(180f * scaleX, 396f * scaleY)
            lineTo(376f * scaleX, 396f * scaleY)
            lineTo(400f * scaleX, 360f * scaleY)
            lineTo(204f * scaleX, 360f * scaleY)
            lineTo(168f * scaleX, 324f * scaleY)
            lineTo(168f * scaleX, 188f * scaleY)
            lineTo(204f * scaleX, 152f * scaleY)
            lineTo(400f * scaleX, 152f * scaleY)
            close()
        }
        drawPath(outerRibbon, brush = coreGradient, style = Fill)

        // Chamfer accent highlight
        val chamferPath = Path().apply {
            moveTo(376f * scaleX, 116f * scaleY)
            lineTo(400f * scaleX, 152f * scaleY)
            lineTo(384f * scaleX, 152f * scaleY)
            lineTo(360f * scaleX, 116f * scaleY)
            close()
        }
        drawPath(chamferPath, color = Color.White.copy(alpha = 0.35f), style = Fill)

        // 3. Central Cryptographic Core Tongue
        val coreTongue = Path().apply {
            moveTo(164f * scaleX, 232f * scaleY)
            lineTo(324f * scaleX, 232f * scaleY)
            lineTo(356f * scaleX, 256f * scaleY)
            lineTo(324f * scaleX, 280f * scaleY)
            lineTo(164f * scaleX, 280f * scaleY)
            close()
        }
        drawPath(coreTongue, brush = coreGradient, style = Fill)

        // Precision facet highlight on core head
        val coreHeadHighlight = Path().apply {
            moveTo(324f * scaleX, 232f * scaleY)
            lineTo(356f * scaleX, 256f * scaleY)
            lineTo(344f * scaleX, 256f * scaleY)
            lineTo(312f * scaleX, 232f * scaleY)
            close()
        }
        drawPath(coreHeadHighlight, color = Color.White.copy(alpha = 0.45f), style = Fill)

        // Negative Space Keyway Slot Aperture
        val keyway = Path().apply {
            moveTo(270f * scaleX, 248f * scaleY)
            cubicTo(
                265f * scaleX, 248f * scaleY,
                261f * scaleX, 251.5f * scaleY,
                261f * scaleX, 256f * scaleY,
            )
            lineTo(264.8f * scaleX, 262.6f * scaleY)
            lineTo(263f * scaleX, 268f * scaleY)
            lineTo(277f * scaleX, 268f * scaleY)
            lineTo(275.2f * scaleX, 262.6f * scaleY)
            cubicTo(
                277.5f * scaleX, 261.2f * scaleY,
                279f * scaleX, 258.8f * scaleY,
                279f * scaleX, 256f * scaleY,
            )
            close()
        }
        drawPath(keyway, color = Color(0xFF080A14), style = Fill)

        // 4. Interlocking Orbital Protective Ribbon (Electric Cyan Accent)
        val orbitalRibbon = Path().apply {
            moveTo(400f * scaleX, 180f * scaleY)
            lineTo(364f * scaleX, 216f * scaleY)
            lineTo(364f * scaleX, 296f * scaleY)
            lineTo(400f * scaleX, 332f * scaleY)
            lineTo(420f * scaleX, 304f * scaleY)
            lineTo(396f * scaleX, 280f * scaleY)
            lineTo(396f * scaleX, 232f * scaleY)
            lineTo(420f * scaleX, 208f * scaleY)
            close()
        }
        drawPath(orbitalRibbon, brush = cyanAccent, style = Fill)
    }
}

/**
 * Standard horizontal or stacked lockup for ELVARIX branding.
 */
@Composable
fun ElvarixBrandLockup(
    modifier: Modifier = Modifier,
    symbolSize: Dp = 48.dp,
    stacked: Boolean = false,
    subtitle: String? = "ZERO-KNOWLEDGE CIPHER VAULT",
) {
    if (stacked) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ElvarixCoreSymbol(size = symbolSize, animatedGlow = true)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ELVARIX",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    color = NovaColors.TextPrimary,
                ),
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp,
                        color = NovaColors.SecondaryAccent,
                    ),
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ElvarixCoreSymbol(size = symbolSize)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "ELVARIX",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                        color = NovaColors.TextPrimary,
                    ),
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.8.sp,
                            color = NovaColors.SecondaryAccent,
                        ),
                    )
                }
            }
        }
    }
}
