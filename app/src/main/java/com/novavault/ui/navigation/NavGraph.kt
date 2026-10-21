package com.novavault.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.novavault.ui.AuthState
import com.novavault.ui.LocalReducedMotion
import com.novavault.ui.NavViewModel
import com.novavault.ui.VaultState
import com.novavault.ui.screens.auth.EmailVerificationScreen
import com.novavault.ui.screens.auth.ForgotPasswordScreen
import com.novavault.ui.screens.auth.SignInScreen
import com.novavault.ui.screens.auth.SignUpScreen
import com.novavault.ui.screens.generator.PasswordGeneratorScreen
import com.novavault.ui.screens.home.HomeScreen
import com.novavault.ui.screens.importexport.ImportExportScreen
import com.novavault.ui.screens.lock.VaultLockScreen
import com.novavault.ui.screens.onboarding.OnboardingScreen
import com.novavault.ui.screens.security.SecurityCenterScreen
import com.novavault.ui.screens.settings.SettingsScreen
import com.novavault.ui.screens.setup.BiometricSetupScreen
import com.novavault.ui.screens.setup.MasterPasswordSetupScreen
import com.novavault.ui.screens.setup.RecoveryKitScreen
import com.novavault.ui.screens.splash.SplashScreen
import com.novavault.ui.screens.totp.TotpScreen
import com.novavault.ui.screens.vault.AddEditItemScreen
import com.novavault.ui.screens.vault.VaultItemDetailScreen
import com.novavault.ui.screens.vault.VaultScreen
import com.novavault.ui.theme.NovaColors

private const val TRANSITION_DURATION = 350

private fun standardEnterTransition(reducedMotion: Boolean): EnterTransition =
    if (reducedMotion) fadeIn(tween(150))
    else slideInHorizontally(tween(TRANSITION_DURATION)) { it / 4 } + fadeIn(tween(TRANSITION_DURATION))

private fun standardExitTransition(reducedMotion: Boolean): ExitTransition =
    if (reducedMotion) fadeOut(tween(150))
    else slideOutHorizontally(tween(TRANSITION_DURATION)) { -it / 4 } + fadeOut(tween(TRANSITION_DURATION))

private fun standardPopEnterTransition(reducedMotion: Boolean): EnterTransition =
    if (reducedMotion) fadeIn(tween(150))
    else slideInHorizontally(tween(TRANSITION_DURATION)) { -it / 4 } + fadeIn(tween(TRANSITION_DURATION))

private fun standardPopExitTransition(reducedMotion: Boolean): ExitTransition =
    if (reducedMotion) fadeOut(tween(150))
    else slideOutHorizontally(tween(TRANSITION_DURATION)) { it / 4 } + fadeOut(tween(TRANSITION_DURATION))

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoute.Home.route, "Home", Icons.Default.Home),
    BottomNavItem(NavRoute.Vault.route, "Vault", Icons.Default.Lock),
    BottomNavItem(NavRoute.SecurityCenter.route, "Security", Icons.Default.Security),
    BottomNavItem(NavRoute.Settings.route, "Settings", Icons.Default.Settings),
)

private val mainRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun NovaVaultNavGraph(
    navViewModel: NavViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val reducedMotion = LocalReducedMotion.current

    val authState by navViewModel.authState.collectAsState()
    val vaultState by navViewModel.vaultState.collectAsState()

    // Global auth / vault-state navigation guard
    LaunchedEffect(authState, vaultState) {
        when (authState) {
            is AuthState.Loading -> Unit

            is AuthState.NeedsOnboarding -> navController.navigate(NavRoute.Onboarding.route) {
                popUpTo(NavRoute.Splash.route) { inclusive = true }
                launchSingleTop = true
            }

            is AuthState.Unauthenticated -> navController.navigate(NavRoute.SignIn.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }

            is AuthState.NeedsEmailVerification ->
                navController.navigate(NavRoute.EmailVerification.route) {
                    popUpTo(NavRoute.SignUp.route) { inclusive = false }
                    launchSingleTop = true
                }

            is AuthState.NeedsMasterPasswordSetup ->
                navController.navigate(NavRoute.MasterPasswordSetup.route) {
                    popUpTo(NavRoute.EmailVerification.route) { inclusive = true }
                    launchSingleTop = true
                }

            is AuthState.NeedsVaultUnlock -> {
                if (vaultState is VaultState.Locked) {
                    navController.navigate(NavRoute.VaultLock.route) {
                        popUpTo(NavRoute.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }

            is AuthState.Authenticated -> {
                if (vaultState is VaultState.Unlocked) {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in mainRoutes
    val showFab = currentRoute == NavRoute.Vault.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NovaColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NovaVaultBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(NavRoute.AddItem.createRoute("LOGIN"))
                    },
                    containerColor = NovaColors.PrimaryAccent,
                    contentColor = NovaColors.TextPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { standardEnterTransition(reducedMotion) },
            exitTransition = { standardExitTransition(reducedMotion) },
            popEnterTransition = { standardPopEnterTransition(reducedMotion) },
            popExitTransition = { standardPopExitTransition(reducedMotion) },
        ) {
            // Splash
            composable(NavRoute.Splash.route) {
                SplashScreen(navController = navController, navViewModel = navViewModel)
            }

            // Onboarding
            composable(NavRoute.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }

            // Auth
            composable(NavRoute.SignIn.route) {
                SignInScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.SignUp.route) {
                SignUpScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.ForgotPassword.route) {
                ForgotPasswordScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.EmailVerification.route) {
                EmailVerificationScreen(navController = navController, viewModel = hiltViewModel())
            }

            // Setup
            composable(NavRoute.MasterPasswordSetup.route) {
                MasterPasswordSetupScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.RecoveryKitSetup.route) {
                RecoveryKitScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.BiometricSetup.route) {
                BiometricSetupScreen(navController = navController, viewModel = hiltViewModel())
            }

            // Lock
            composable(NavRoute.VaultLock.route) {
                VaultLockScreen(navController = navController, viewModel = hiltViewModel())
            }

            // Main tabs
            composable(NavRoute.Home.route) {
                HomeScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.Vault.route) {
                VaultScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.SecurityCenter.route) {
                SecurityCenterScreen(navController = navController, viewModel = hiltViewModel())
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(navController = navController, viewModel = hiltViewModel())
            }

            // Vault sub-routes
            composable(route = NavRoute.AddItem.route) { backStackEntry ->
                val itemType = backStackEntry.arguments?.getString("itemType") ?: "LOGIN"
                AddEditItemScreen(itemType = itemType, navController = navController)
            }

            composable(NavRoute.ItemDetail.route) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                VaultItemDetailScreen(itemId = itemId, navController = navController)
            }

            composable(NavRoute.EditItem.route) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                AddEditItemScreen(itemId = itemId, navController = navController)
            }

            composable(NavRoute.PasswordGenerator.route) {
                PasswordGeneratorScreen(navController = navController)
            }

            composable(NavRoute.TotpAuthenticator.route) {
                TotpScreen(navController = navController)
            }

            composable(NavRoute.ImportExport.route) {
                ImportExportScreen(navController = navController)
            }

            composable(NavRoute.AccountSettings.route) {
                SettingsScreen(navController = navController)
            }

            composable(NavRoute.SecuritySettings.route) {
                SecurityCenterScreen(navController = navController)
            }

            composable(NavRoute.AutofillSettings.route) {
                SettingsScreen(navController = navController)
            }

            composable(NavRoute.BackupSettings.route) {
                ImportExportScreen(navController = navController)
            }
        }
    }
}

@Composable
private fun NovaVaultBottomBar(navController: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = NovaColors.SurfaceVariant,
        contentColor = NovaColors.TextPrimary,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NovaColors.TextPrimary,
                    selectedTextColor = NovaColors.SecondaryAccent,
                    indicatorColor = NovaColors.PrimaryAccent,
                    unselectedIconColor = NovaColors.TextSecondary,
                    unselectedTextColor = NovaColors.TextSecondary,
                ),
            )
        }
    }
}
