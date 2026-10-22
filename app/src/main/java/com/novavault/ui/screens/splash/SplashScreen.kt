package com.novavault.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.novavault.ui.AuthState
import com.novavault.ui.NavViewModel
import com.novavault.ui.navigation.NavRoute
import kotlinx.coroutines.delay

private const val SPLASH_MIN_DURATION_MS = 2000L

@Composable
fun SplashScreen(
    navController: NavController,
    navViewModel: NavViewModel,
) {
    val authState by navViewModel.authState.collectAsState()

    // Shield logo scale + alpha entrance
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }

    // Title letter-by-letter reveal
    val title = "ELVARIX"
    val letterAlphas = remember { title.map { Animatable(0f) } }

    // Aurora shift
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    val auroraOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_offset",
    )

    // Animate entrance
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        logoAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(200)
        letterAlphas.forEachIndexed { index, anim ->
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 120, delayMillis = 0),
            )
            delay(60)
        }
    }

    // Navigate once minimum duration is elapsed AND auth state is resolved
    LaunchedEffect(authState) {
        if (authState is AuthState.Loading) return@LaunchedEffect
        delay(SPLASH_MIN_DURATION_MS)
        when (authState) {
            is AuthState.NeedsOnboarding ->
                navController.navigate(NavRoute.Onboarding.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.Unauthenticated ->
                navController.navigate(NavRoute.SignIn.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.NeedsEmailVerification ->
                navController.navigate(NavRoute.EmailVerification.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.NeedsMasterPasswordSetup ->
                navController.navigate(NavRoute.MasterPasswordSetup.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.NeedsVaultUnlock ->
                navController.navigate(NavRoute.VaultLock.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.Authenticated ->
                navController.navigate(NavRoute.Home.route) {
                    popUpTo(NavRoute.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }

            else -> Unit
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    val bgColor1 = Color(0xFF0A0A1A)
    val bgColor2 = Color(0xFF0D1B3E)
    val aurora1 = Color(0xFF1A3A6B)
    val aurora2 = Color(0xFF2D1B69)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgColor1, bgColor2))
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Aurora layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAurora(auroraOffset, aurora1, aurora2)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Animated shield / vault logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawShieldLogo(scale = logoScale.value)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Letter-by-letter title
            androidx.compose.foundation.layout.Row {
                title.forEachIndexed { index, ch ->
                    Text(
                        text = ch.toString(),
                        modifier = Modifier.alpha(letterAlphas[index].value),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Secure. Private. Yours.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha.value),
            )
        }
    }
}

// ─── Canvas draw helpers ───────────────────────────────────────────────────────

private fun DrawScope.drawShieldLogo(scale: Float) {
    val w = size.width * scale
    val h = size.height * scale
    val cx = size.width / 2f
    val cy = size.height / 2f

    val shieldPath = Path().apply {
        moveTo(cx, cy - h * 0.45f)
        cubicTo(cx + w * 0.45f, cy - h * 0.45f, cx + w * 0.45f, cy - h * 0.1f, cx, cy + h * 0.45f)
        cubicTo(cx - w * 0.45f, cy - h * 0.1f, cx - w * 0.45f, cy - h * 0.45f, cx, cy - h * 0.45f)
        close()
    }

    val gradient = Brush.radialGradient(
        colors = listOf(Color(0xFF5B7FFF), Color(0xFF1A2FBB)),
        center = Offset(cx, cy),
        radius = size.minDimension / 1.5f,
    )

    drawPath(
        path = shieldPath,
        brush = gradient,
    )

    // Keyhole circle
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = w * 0.12f,
        center = Offset(cx, cy - h * 0.05f),
    )

    // Keyhole stem
    val stemPath = Path().apply {
        moveTo(cx - w * 0.06f, cy - h * 0.05f)
        lineTo(cx - w * 0.04f, cy + h * 0.15f)
        lineTo(cx + w * 0.04f, cy + h * 0.15f)
        lineTo(cx + w * 0.06f, cy - h * 0.05f)
        close()
    }
    drawPath(path = stemPath, color = Color.White.copy(alpha = 0.9f))
}

private fun DrawScope.drawAurora(offset: Float, color1: Color, color2: Color) {
    val w = size.width
    val h = size.height

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color1.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(w * (0.2f + offset * 0.3f), h * 0.2f),
            radius = w * 0.7f,
        ),
        radius = w * 0.7f,
        center = Offset(w * (0.2f + offset * 0.3f), h * 0.2f),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color2.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(w * (0.8f - offset * 0.3f), h * 0.6f),
            radius = w * 0.6f,
        ),
        radius = w * 0.6f,
        center = Offset(w * (0.8f - offset * 0.3f), h * 0.6f),
    )
}
