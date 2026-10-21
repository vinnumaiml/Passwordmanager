package com.novavault.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.repository.AppSettings
import com.novavault.domain.repository.AuthRepository
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaDangerButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
        }
    }

    fun lockNow(onDone: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.lockVault()
            onDone()
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.lockVault()
            authRepository.signOut()
            onDone()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.lockVault()
            authRepository.deleteAccount()
            onDone()
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showClipboardDialog by remember { mutableStateOf(false) }

    AuroraBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )
            }

            // Security Section
            item {
                SectionHeader("SECURITY & BIOMETRICS")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Unlock",
                        subtitle = "Hardware-backed fingerprint / face unlock",
                        checked = settings.biometricUnlockEnabled,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(biometricUnlockEnabled = it))
                        },
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        icon = Icons.Default.Timer,
                        title = "Auto-Lock Timeout",
                        value = "${settings.autoLockTimeoutMinutes} min",
                        onClick = { showTimeoutDialog = true },
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.Lock,
                        title = "Lock on Background",
                        subtitle = "Lock vault immediately when leaving the app",
                        checked = settings.lockOnBackground,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(lockOnBackground = it))
                        },
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        icon = Icons.Default.ScreenLockPortrait,
                        title = "Screenshot Protection",
                        subtitle = "Prevent screenshots and recent-app previews",
                        checked = settings.screenshotProtection,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(screenshotProtection = it))
                        },
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        icon = Icons.Default.Timer,
                        title = "Clipboard Clear Interval",
                        value = "${settings.clipboardClearSeconds}s",
                        onClick = { showClipboardDialog = true },
                    )
                }
            }

            // System Integration Section
            item {
                SectionHeader("SYSTEM INTEGRATION")
            }

            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Password,
                        title = "Android Autofill Service",
                        value = "Configure in Settings",
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                    )
                }
            }

            // Backup & Data
            item {
                SectionHeader("BACKUP & EXPORT")
            }

            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Upload,
                        title = "Import & Export Vault",
                        value = "Encrypted / CSV",
                        onClick = { navController.navigate(NavRoute.ImportExport.route) },
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        icon = Icons.Default.VpnKey,
                        title = "View Recovery Kit",
                        value = "Offline Backup",
                        onClick = { navController.navigate(NavRoute.RecoveryKitSetup.route) },
                    )
                }
            }

            // Accessibility / Appearance
            item {
                SectionHeader("APPEARANCE")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.MotionPhotosOff,
                        title = "Reduced Motion",
                        subtitle = "Disable animated background & transitions",
                        checked = settings.reducedMotion,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(reducedMotion = it))
                        },
                    )
                }
            }

            // Session & Account
            item {
                SectionHeader("SESSION & ACCOUNT")
            }

            item {
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Lock,
                        title = "Lock Vault Now",
                        value = "",
                        onClick = {
                            viewModel.lockNow {
                                navController.navigate(NavRoute.VaultLock.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        icon = Icons.Default.Logout,
                        title = "Sign Out of Account",
                        value = "",
                        titleColor = NovaColors.Warning,
                        onClick = { showSignOutDialog = true },
                    )

                    SettingsDivider()

                    SettingsClickableRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Delete Account & Cloud Data",
                        value = "",
                        titleColor = NovaColors.Danger,
                        onClick = { showDeleteAccountDialog = true },
                    )
                }
            }

            // Security Disclosure
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NovaColors.SecondaryAccent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nova Vault uses envelope encryption with Argon2id and AES-256-GCM. Your master password and vault records are strictly confidential and zero-knowledge protected. Penetration testing is recommended prior to production release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaColors.TextSecondary,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Auto-lock dialog
    if (showTimeoutDialog) {
        val options = listOf(1, 5, 15, 30)
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Auto-Lock Timeout") },
            text = {
                Column {
                    options.forEach { minutes ->
                        Text(
                            text = "$minutes minutes",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(autoLockTimeoutMinutes = minutes))
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (settings.autoLockTimeoutMinutes == minutes) NovaColors.PrimaryAccent else NovaColors.TextPrimary,
                            fontWeight = if (settings.autoLockTimeoutMinutes == minutes) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Clipboard dialog
    if (showClipboardDialog) {
        val options = listOf(15, 30, 60, 120)
        AlertDialog(
            onDismissRequest = { showClipboardDialog = false },
            title = { Text("Clipboard Clear Interval") },
            text = {
                Column {
                    options.forEach { seconds ->
                        Text(
                            text = "$seconds seconds",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(settings.copy(clipboardClearSeconds = seconds))
                                    showClipboardDialog = false
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (settings.clipboardClearSeconds == seconds) NovaColors.PrimaryAccent else NovaColors.TextPrimary,
                            fontWeight = if (settings.clipboardClearSeconds == seconds) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClipboardDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Sign out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your vault will be locked and local credentials cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut {
                            navController.navigate(NavRoute.SignIn.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Sign Out", color = NovaColors.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Account deletion confirmation dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently destroy your account and all associated encrypted vault backups on the server. This action CANNOT be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount {
                            navController.navigate(NavRoute.SignUp.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Delete Everything", color = NovaColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = NovaColors.SecondaryAccent,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NovaColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaColors.TextSecondary,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NovaColors.TextPrimary,
                checkedTrackColor = NovaColors.PrimaryAccent,
            ),
        )
    }
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    value: String,
    titleColor: androidx.compose.ui.graphics.Color = NovaColors.TextPrimary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != NovaColors.TextPrimary) titleColor else NovaColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaColors.TextSecondary,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NovaColors.TextHint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 4.dp)
    )
}
