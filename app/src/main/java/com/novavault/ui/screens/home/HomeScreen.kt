package com.novavault.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.AuthRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.components.VaultItemCard
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.screens.update.UpdateDialog
import com.novavault.ui.screens.update.UpdateViewModel
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val passwordUseCase: GeneratePasswordUseCase,
) : ViewModel() {

    val userName: StateFlow<String> = authRepository.currentUserName
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val isVaultLocked: StateFlow<Boolean> = vaultRepository.vaultState.map {
        it is com.novavault.domain.model.VaultState.Locked
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val items: StateFlow<List<VaultItem>> = vaultRepository.observeItems()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites: StateFlow<List<VaultItem>> = items.map { list ->
        list.filter { it.isFavorite && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentItems: StateFlow<List<VaultItem>> = items.map { list ->
        list.filter { !it.isDeleted }.sortedByDescending { it.updatedAt }.take(5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val securityScore: StateFlow<Int> = items.map { list ->
        val logins = list.filter { it.type == VaultItemType.LOGIN && !it.isDeleted }
        if (logins.isEmpty()) 100
        else {
            val strongCount = logins.count {
                val strength = passwordUseCase.estimateStrength(it.login?.password.orEmpty())
                strength == PasswordStrength.STRONG || strength == PasswordStrength.VERY_STRONG
            }
            (strongCount * 100) / logins.size
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 100)

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            vaultRepository.saveItem(item.copy(isFavorite = !item.isFavorite))
        }
    }
}

private fun getPersonalizedGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
}

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userName by viewModel.userName.collectAsState()
    val isVaultLocked by viewModel.isVaultLocked.collectAsState()
    val items by viewModel.items.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentItems by viewModel.recentItems.collectAsState()
    val securityScore by viewModel.securityScore.collectAsState()
    val updateState by updateViewModel.checkState.collectAsState()

    // Active update check on Home screen start
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdates(force = true)
    }

    val greetingName = if (userName.isNotBlank()) userName else "User"

    AuroraBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(bottom = 90.dp),
        ) {
            // Header with Elvarix Branding & Personalized Greeting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "ELVARIX",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NovaColors.SecondaryAccent,
                                letterSpacing = 2.5.sp,
                            ),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${getPersonalizedGreeting()}, $greetingName",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaColors.TextPrimary,
                        )
                    }

                    // Security Shield indicator badge
                    Surface(
                        shape = CircleShape,
                        color = NovaColors.SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.GlowCyan),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { navController.navigate(NavRoute.SecurityCenter.route) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Status",
                                tint = NovaColors.SecondaryAccent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Update Notification Banner (if update available)
            item {
                AnimatedVisibility(
                    visible = updateState is UpdateCheckState.UpdateAvailable,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val manifest = (updateState as? UpdateCheckState.UpdateAvailable)?.manifest
                    if (manifest != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NovaColors.PrimaryAccentDim.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.SecondaryAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .clickable { updateViewModel.showUpdateDialog() },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = NovaColors.SecondaryAccent,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Elvarix v${manifest.versionName} Available",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NovaColors.TextPrimary,
                                    )
                                    Text(
                                        text = "Tap to review release notes and install update",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NovaColors.TextSecondary,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // Vault Locked Banner (if vault locked in background)
            if (isVaultLocked) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFB00020).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { navController.navigate(NavRoute.VaultLock.route) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Vault is Locked",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NovaColors.TextPrimary,
                                )
                                Text(
                                    text = "Tap to unlock and decrypt your credentials",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovaColors.TextSecondary,
                                )
                            }
                            Text(
                                text = "Unlock",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NovaColors.SecondaryAccent,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Search bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NovaColors.SurfaceVariant)
                        .clickable { navController.navigate(NavRoute.Vault.route) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaColors.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search credentials, notes, cards…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaColors.TextHint,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Security health summary card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { navController.navigate(NavRoute.SecurityCenter.route) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceElevated),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Security Health Score",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = NovaColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (securityScore >= 80) "All systems optimal" else "Vulnerabilities detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (securityScore >= 80) NovaColors.Success else NovaColors.Warning,
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (securityScore >= 80) NovaColors.SuccessDim else NovaColors.WarningDim,
                            modifier = Modifier.size(54.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$securityScore%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (securityScore >= 80) NovaColors.Success else NovaColors.Warning,
                                    ),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Category Shortcuts Row
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Categories & Tools",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            QuickActionPill(
                                icon = Icons.Default.Key,
                                label = "Add Login",
                                onClick = {
                                    if (isVaultLocked) navController.navigate(NavRoute.VaultLock.route)
                                    else navController.navigate(NavRoute.AddItem.createRoute("LOGIN"))
                                },
                            )
                        }
                        item {
                            QuickActionPill(
                                icon = Icons.Default.Description,
                                label = "Add Note",
                                onClick = {
                                    if (isVaultLocked) navController.navigate(NavRoute.VaultLock.route)
                                    else navController.navigate(NavRoute.AddItem.createRoute("SECURE_NOTE"))
                                },
                            )
                        }
                        item {
                            QuickActionPill(
                                icon = Icons.Default.CreditCard,
                                label = "Add Card",
                                onClick = {
                                    if (isVaultLocked) navController.navigate(NavRoute.VaultLock.route)
                                    else navController.navigate(NavRoute.AddItem.createRoute("PAYMENT_CARD"))
                                },
                            )
                        }
                        item {
                            QuickActionPill(
                                icon = Icons.Default.Person,
                                label = "Add Identity",
                                onClick = {
                                    if (isVaultLocked) navController.navigate(NavRoute.VaultLock.route)
                                    else navController.navigate(NavRoute.AddItem.createRoute("IDENTITY"))
                                },
                            )
                        }
                        item {
                            QuickActionPill(
                                icon = Icons.Default.Wifi,
                                label = "Add Wi-Fi",
                                onClick = {
                                    if (isVaultLocked) navController.navigate(NavRoute.VaultLock.route)
                                    else navController.navigate(NavRoute.AddItem.createRoute("WIFI_CREDENTIAL"))
                                },
                            )
                        }
                        item {
                            QuickActionPill(
                                icon = Icons.Default.Security,
                                label = "Key Generator",
                                onClick = { navController.navigate(NavRoute.PasswordGenerator.route) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Favorites section (if any)
            if (favorites.isNotEmpty()) {
                item {
                    Text(
                        text = "Favorites",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favorites, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = NovaColors.SurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, NovaColors.Border),
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { navController.navigate(NavRoute.ItemDetail.createRoute(item.id)) }
                                    .padding(12.dp),
                            ) {
                                Column {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = NovaColors.SecondaryAccent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = NovaColors.TextPrimary,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = item.login?.username.orEmpty().ifBlank { item.type.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NovaColors.TextSecondary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Recent items
            item {
                Text(
                    text = "Recently Updated",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (recentItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NovaColors.TextHint,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your Elvarix vault is empty",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaColors.TextSecondary,
                            )
                        }
                    }
                }
            } else {
                items(recentItems, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        VaultItemCard(
                            item = item,
                            onClick = { navController.navigate(NavRoute.ItemDetail.createRoute(item.id)) },
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onCopyPassword = {
                                item.login?.password?.let { pwd ->
                                    SecureClipboard.copyAndScheduleClear(context, pwd, "Password", 30, scope)
                                }
                            },
                        )
                    }
                }
            }
        }

        // Render In-App Update Dialog when update is available
        UpdateDialog(viewModel = updateViewModel)
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = NovaColors.SurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, NovaColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NovaColors.SecondaryAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = NovaColors.TextPrimary,
            )
        }
    }
}
