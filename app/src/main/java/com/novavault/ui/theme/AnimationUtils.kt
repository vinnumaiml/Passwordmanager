package com.novavault.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition local that signals whether the user has enabled Reduce Motion
 * in system accessibility settings. Consumers should read this before
 * scheduling any decorative animations.
 */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

// ---------------------------------------------------------------------------
// Reduced-motion variants — plain cross-fade, no spatial movement
// ---------------------------------------------------------------------------

val ReducedEnterTransition: EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = 200, easing = LinearEasing))

val ReducedExitTransition: ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 200, easing = LinearEasing))

// ---------------------------------------------------------------------------
// Standard (full-motion) variants
// ---------------------------------------------------------------------------

val StandardEnterTransition: EnterTransition =
    slideInVertically(
        animationSpec = tween(durationMillis = 350),
        initialOffsetY = { fullHeight -> fullHeight / 8 },
    ) + fadeIn(animationSpec = tween(durationMillis = 350))

val StandardExitTransition: ExitTransition =
    slideOutVertically(
        animationSpec = tween(durationMillis = 300),
        targetOffsetY = { fullHeight -> -fullHeight / 8 },
    ) + fadeOut(animationSpec = tween(durationMillis = 300))

// ---------------------------------------------------------------------------
// Adaptive helper — picks the right transition based on LocalReducedMotion
// ---------------------------------------------------------------------------

/**
 * Returns [ReducedEnterTransition] when the user has enabled Reduce Motion,
 * otherwise returns [StandardEnterTransition].
 */
@Composable
fun animatedEnter(): EnterTransition {
    val reduced = LocalReducedMotion.current
    return if (reduced) ReducedEnterTransition else StandardEnterTransition
}

/**
 * Returns [ReducedExitTransition] when the user has enabled Reduce Motion,
 * otherwise returns [StandardExitTransition].
 */
@Composable
fun animatedExit(): ExitTransition {
    val reduced = LocalReducedMotion.current
    return if (reduced) ReducedExitTransition else StandardExitTransition
}
