package com.novavault.ui.theme

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Internal data classes (not exposed publicly)
// ---------------------------------------------------------------------------

private data class StarSpec(
    val x: Float,   // 0..1 relative to canvas width
    val y: Float,   // 0..1 relative to canvas height
    val radius: Float,
    val baseAlpha: Float,
    val phaseOffset: Float, // 0..1 individual twinkle offset
)

private const val STAR_COUNT = 40
private const val AURORA_BLOB_RADIUS_FRACTION = 0.40f

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

/**
 * Full-screen animated aurora background.
 *
 * Renders:
 *  1. Solid dark background fill ([NovaColors.Background])
 *  2. Three large radial-gradient blobs (violet, indigo, cyan) whose centres
 *     drift via sin/cos of continuously animated phase values.
 *  3. ~40 small "star" dots whose alpha gently twinkles.
 *  4. A subtle vertical gradient overlay at the bottom for depth.
 *
 * Performance notes:
 *  • Star positions and specs are computed once inside [remember] — no
 *    allocation on every frame.
 *  • All animated values are driven by [rememberInfiniteTransition], which
 *    schedules Choreographer callbacks automatically and pauses when the
 *    composable leaves the composition.
 *  • When [LocalReducedMotion] is `true` everything is rendered at a fixed,
 *    static snapshot — no animations are started.
 *
 * @param modifier Applied to the root [Box].
 * @param content  Slot rendered on top of the canvas.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    isQuiet: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current

    // -----------------------------------------------------------------------
    // Star specs — computed once, never reallocated
    // -----------------------------------------------------------------------
    val stars: List<StarSpec> = remember {
        val rng = kotlin.random.Random(seed = 0xA2B3C4D5L.toInt())
        List(STAR_COUNT) {
            StarSpec(
                x           = rng.nextFloat(),
                y           = rng.nextFloat(),
                radius      = rng.nextFloat() * 1.5f + 0.5f,   // 0.5..2 px (scaled by density later)
                baseAlpha   = rng.nextFloat() * 0.5f + 0.15f,  // 0.15..0.65
                phaseOffset = rng.nextFloat(),
            )
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle-aware pause: detect when the app goes to background
    // -----------------------------------------------------------------------
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle
        .currentStateFlow
        .collectAsStateWithLifecycle()
    val isInForeground = lifecycleState.isAtLeast(Lifecycle.State.STARTED)

    // -----------------------------------------------------------------------
    // Animated phase values (4 independent oscillators)
    // -----------------------------------------------------------------------
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // Only drive animations when we are in the foreground and motion is allowed
    val shouldAnimate = isInForeground && !reducedMotion

    val phase1 by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = if (shouldAnimate) 1f else 0f,
        animationSpec  = InfiniteRepeatableSpec(
            animation  = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase1",
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue   = 0.25f,
        targetValue    = if (shouldAnimate) 1.25f else 0.25f,
        animationSpec  = InfiniteRepeatableSpec(
            animation  = tween(durationMillis = 9_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase2",
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue   = 0.5f,
        targetValue    = if (shouldAnimate) 1.5f else 0.5f,
        animationSpec  = InfiniteRepeatableSpec(
            animation  = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase3",
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue   = 0.75f,
        targetValue    = if (shouldAnimate) 1.75f else 0.75f,
        animationSpec  = InfiniteRepeatableSpec(
            animation  = tween(durationMillis = 8_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "phase4",
    )

    // -----------------------------------------------------------------------
    // Layout
    // -----------------------------------------------------------------------
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val blobRadius = w * AURORA_BLOB_RADIUS_FRACTION

            // 1. Solid background
            drawRect(color = NovaColors.Background)

            if (isQuiet) {
                // Quiet atmospheric background for content-heavy screens (Home, Vault)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NovaColors.AuroraViolet.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(w * 0.15f, 0f),
                        radius = w * 0.7f,
                    ),
                    radius = w * 0.7f,
                    center = Offset(w * 0.15f, 0f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NovaColors.AuroraCyan.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(w * 0.85f, h * 0.4f),
                        radius = w * 0.6f,
                    ),
                    radius = w * 0.6f,
                    center = Offset(w * 0.85f, h * 0.4f),
                )
                return@Canvas
            }

            // 2. Aurora blobs
            drawAuroraBlob(
                centerX     = w * 0.20f + w * 0.35f * sin(phase1 * Math.PI.toFloat() * 2f),
                centerY     = h * 0.25f + h * 0.20f * cos(phase2 * Math.PI.toFloat() * 2f),
                radius      = blobRadius,
                innerColor  = NovaColors.AuroraViolet,
            )
            drawAuroraBlob(
                centerX     = w * 0.75f + w * 0.20f * cos(phase3 * Math.PI.toFloat() * 2f),
                centerY     = h * 0.50f + h * 0.25f * sin(phase1 * Math.PI.toFloat() * 2f),
                radius      = blobRadius * 0.85f,
                innerColor  = NovaColors.AuroraIndigo,
            )
            drawAuroraBlob(
                centerX     = w * 0.50f + w * 0.25f * sin(phase4 * Math.PI.toFloat() * 2f),
                centerY     = h * 0.80f + h * 0.15f * cos(phase3 * Math.PI.toFloat() * 2f),
                radius      = blobRadius * 0.75f,
                innerColor  = NovaColors.AuroraCyan,
            )

            // 3. Stars
            for (star in stars) {
                val twinkle = if (reducedMotion) star.baseAlpha
                else {
                    val t = (phase4 + star.phaseOffset) % 1f
                    star.baseAlpha * (0.5f + 0.5f * sin(t * Math.PI.toFloat() * 2f))
                }
                drawCircle(
                    color  = NovaColors.TextPrimary,
                    radius = star.radius,
                    center = Offset(star.x * w, star.y * h),
                    alpha  = twinkle.coerceIn(0f, 1f),
                )
            }

            // 4. Bottom vignette gradient overlay
            drawRect(
                brush = Brush.verticalGradient(
                    0f   to Color.Transparent,
                    0.6f to Color.Transparent,
                    1f   to NovaColors.SurfaceVariant.copy(alpha = 0.30f),
                ),
                size  = size,
            )
        }

        // Content slot rendered on top of the canvas
        content()
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

/**
 * Draws a single aurora blob as a radial gradient circle.
 * The gradient goes from [innerColor] (which already carries its own alpha)
 * to fully transparent at the edge, giving a soft glow effect.
 */
private fun DrawScope.drawAuroraBlob(
    centerX: Float,
    centerY: Float,
    radius: Float,
    innerColor: Color,
) {
    drawCircle(
        brush  = Brush.radialGradient(
            colors  = listOf(innerColor, Color.Transparent),
            center  = Offset(centerX, centerY),
            radius  = radius,
        ),
        radius = radius,
        center = Offset(centerX, centerY),
        alpha  = 1f, // alpha is baked into innerColor
    )
}
