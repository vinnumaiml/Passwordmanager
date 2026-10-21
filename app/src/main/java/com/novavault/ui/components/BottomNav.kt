package com.novavault.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaTypography

// ---------------------------------------------------------------------------
// Destination definition
// ---------------------------------------------------------------------------

private data class NavDestination(
    val route:        String,
    val label:        String,
    val selectedIcon: ImageVector,
    val icon:         ImageVector,
)

private val Destinations = listOf(
    NavDestination(
        route        = "home",
        label        = "Home",
        selectedIcon = Icons.Filled.Home,
        icon         = Icons.Outlined.Home,
    ),
    NavDestination(
        route        = "vault",
        label        = "Vault",
        selectedIcon = Icons.Filled.Lock,
        icon         = Icons.Outlined.Lock,
    ),
    NavDestination(
        route        = "security",
        label        = "Security",
        selectedIcon = Icons.Filled.Security,
        icon         = Icons.Outlined.Security,
    ),
    NavDestination(
        route        = "settings",
        label        = "Settings",
        selectedIcon = Icons.Filled.Settings,
        icon         = Icons.Outlined.Settings,
    ),
)

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

/**
 * Bottom navigation bar for NOVA VAULT.
 *
 * Features:
 *  • Translucent [NovaColors.SurfaceVariant] background with a top divider.
 *  • Selected item highlighted with [NovaColors.PrimaryAccent] icon + label.
 *  • Icon transitions driven by [AnimatedContent] so the filled/outlined swap
 *    cross-fades rather than snapping.
 *  • Respects system navigation bar insets via [Modifier.navigationBarsPadding].
 *
 * @param currentRoute  Active navigation route string.
 * @param onNavigate    Callback invoked with the destination route on tap.
 * @param modifier      Applied to the root container.
 */
@Composable
fun NovaBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NovaColors.SurfaceVariant)
            .drawBehind {
                // Subtle top divider line
                drawLine(
                    color       = NovaColors.Divider,
                    start       = Offset(0f, 0f),
                    end         = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .navigationBarsPadding(),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            for (dest in Destinations) {
                NavItem(
                    destination  = dest,
                    isSelected   = currentRoute == dest.route,
                    onNavigate   = onNavigate,
                    modifier     = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private per-item composable
// ---------------------------------------------------------------------------

@Composable
private fun NavItem(
    destination: NavDestination,
    isSelected: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue   = if (isSelected) NovaColors.PrimaryAccent else NovaColors.TextHint,
        animationSpec = tween(durationMillis = 200),
        label         = "navItemColor_${destination.route}",
    )

    IconButton(
        onClick  = { if (!isSelected) onNavigate(destination.route) },
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState   = isSelected,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                },
                label         = "navIcon_${destination.route}",
            ) { selected ->
                Icon(
                    imageVector        = if (selected) destination.selectedIcon else destination.icon,
                    contentDescription = destination.label,
                    tint               = contentColor,
                    modifier           = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text  = destination.label,
                style = NovaTypography.labelSmall,
                color = contentColor,
            )

            // Selection indicator dot
            if (isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = NovaColors.PrimaryAccent,
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
                )
            }
        }
    }
}
