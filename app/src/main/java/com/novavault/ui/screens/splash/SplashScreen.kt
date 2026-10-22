package com.novavault.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.novavault.ui.AuthState
import com.novavault.ui.NavViewModel
import com.novavault.ui.components.ElvarixCoreSymbol
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.NovaColors
import kotlinx.coroutines.delay

private const val SPLASH_MIN_DURATION_MS = 2000L

@Composable
fun SplashScreen(
    navController: NavController,
    navViewModel: NavViewModel,
) {
    val authState by navViewModel.authState.collectAsState()

    // Elvarix Core scale + alpha entrance
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
        letterAlphas.forEachIndexed { _, anim ->
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
            is AuthState.Loading -> { /* Handled above */ }
        }
    }

    val bgColor1 = Color(0xFF080A14)
    val bgColor2 = Color(0xFF10162A)
    val aurora1 = Color(0xFF8B5CF6)
    val aurora2 = Color(0xFF43E7D0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgColor1, bgColor2))
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient Aurora layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAurora(auroraOffset, aurora1, aurora2)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // The Elvarix Core Symbol with animated glow
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                ElvarixCoreSymbol(
                    size = (128 * logoScale.value).dp,
                    animatedGlow = true,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Letter-by-letter ELVARIX title
            Row {
                title.forEachIndexed { index, ch ->
                    Text(
                        text = ch.toString(),
                        modifier = Modifier.alpha(letterAlphas[index].value),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ZERO-KNOWLEDGE CIPHER VAULT",
                color = NovaColors.SecondaryAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha.value),
            )
        }
    }
}

private fun DrawScope.drawAurora(offset: Float, color1: Color, color2: Color) {
    val w = size.width
    val h = size.height

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color1.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(w * (0.2f + offset * 0.3f), h * 0.2f),
            radius = w * 0.7f,
        ),
        radius = w * 0.7f,
        center = Offset(w * (0.2f + offset * 0.3f), h * 0.2f),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color2.copy(alpha = 0.16f), Color.Transparent),
            center = Offset(w * (0.8f - offset * 0.3f), h * 0.6f),
            radius = w * 0.6f,
        ),
        radius = w * 0.6f,
        center = Offset(w * (0.8f - offset * 0.3f), h * 0.6f),
    )
}
