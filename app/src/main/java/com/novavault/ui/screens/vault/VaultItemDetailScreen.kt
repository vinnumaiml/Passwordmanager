package com.novavault.ui.screens.vault

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaDangerButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    val item: StateFlow<VaultItem?> = vaultRepository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun toggleFavorite() {
        val current = item.value ?: return
        viewModelScope.launch {
            vaultRepository.saveItem(current.copy(isFavorite = !current.isFavorite))
        }
    }

    fun deleteItem(permanent: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            vaultRepository.deleteItem(itemId, permanent)
            onDone()
        }
    }
}

@Composable
fun VaultItemDetailScreen(
    itemId: String,
    navController: NavController,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val item by viewModel.item.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AuroraBackground {
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                contentAlignment = Alignment.Center,
            ) {
                Text("Item not found", color = NovaColors.TextSecondary)
            }
            return@AuroraBackground
        }

        val currentItem = item!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaColors.TextPrimary,
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (currentItem.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (currentItem.isFavorite) NovaColors.Warning else NovaColors.TextSecondary,
                        )
                    }

                    IconButton(onClick = { navController.navigate(NavRoute.EditItem.createRoute(itemId)) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = NovaColors.PrimaryAccent,
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = NovaColors.Danger,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Type Badge
            Text(
                text = currentItem.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentItem.type.name.replace("_", " "),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = NovaColors.SecondaryAccent,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Type-specific field cards
            when (currentItem.type) {
                VaultItemType.LOGIN -> {
                    val login = currentItem.login
                    if (login != null) {
                        DetailFieldCard(
                            label = "Username",
                            value = login.username,
                            onCopy = {
                                SecureClipboard.copyAndScheduleClear(context, login.username, "Username", 30, scope)
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailPasswordCard(
                            password = login.password,
                            isVisible = isPasswordVisible,
                            onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCopy = {
                                SecureClipboard.copyAndScheduleClear(context, login.password, "Password", 30, scope)
                            },
                        )

                        if (login.uris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailFieldCard(
                                label = "Website URL",
                                value = login.uris.first(),
                                onOpenUrl = {
                                    val uri = login.uris.first()
                                    val url = if (uri.startsWith("http://") || uri.startsWith("https://")) uri else "https://$uri"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                            )
                        }

                        if (!login.totpSecret.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailFieldCard(
                                label = "TOTP Seed",
                                value = "••••••••••••",
                                onCopy = {
                                    SecureClipboard.copyAndScheduleClear(context, login.totpSecret, "TOTP", 30, scope)
                                },
                            )
                        }

                        if (login.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailFieldCard(label = "Notes", value = login.notes)
                        }
                    }
                }

                VaultItemType.SECURE_NOTE -> {
                    DetailFieldCard(
                        label = "Note Content",
                        value = currentItem.secureNote?.content.orEmpty(),
                        onCopy = {
                            currentItem.secureNote?.content?.let {
                                SecureClipboard.copyAndScheduleClear(context, it, "Note", 30, scope)
                            }
                        },
                    )
                }

                VaultItemType.PAYMENT_CARD -> {
                    val card = currentItem.paymentCard
                    if (card != null) {
                        DetailFieldCard(label = "Cardholder", value = card.cardholderName)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(
                            label = "Card Number",
                            value = if (isPasswordVisible) card.number else "•••• •••• •••• ${card.number.takeLast(4)}",
                            onCopy = {
                                SecureClipboard.copyAndScheduleClear(context, card.number, "Card", 30, scope)
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(label = "Expiration", value = "${card.expiryMonth}/${card.expiryYear}")
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(
                            label = "CVV",
                            value = if (isPasswordVisible) card.cvv else "•••",
                        )
                    }
                }

                VaultItemType.IDENTITY -> {
                    val id = currentItem.identity
                    if (id != null) {
                        DetailFieldCard(label = "Full Name", value = "${id.firstName} ${id.lastName}")
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(label = "Email", value = id.email)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(label = "Phone", value = id.phone)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailFieldCard(label = "Address", value = "${id.address}, ${id.city}, ${id.country}")
                    }
                }

                VaultItemType.WIFI_CREDENTIAL -> {
                    val wifi = currentItem.wifiCredential
                    if (wifi != null) {
                        DetailFieldCard(label = "Network SSID", value = wifi.ssid)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailPasswordCard(
                            password = wifi.password,
                            isVisible = isPasswordVisible,
                            onToggleVisibility = { isPasswordVisible = !isPasswordVisible },
                            onCopy = {
                                SecureClipboard.copyAndScheduleClear(context, wifi.password, "Wi-Fi Password", 30, scope)
                            },
                        )
                    }
                }

                else -> {
                    DetailFieldCard(label = "Record Name", value = currentItem.name)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timestamps
            Text(
                text = "Created: ${currentItem.createdAt}\nLast updated: ${currentItem.updatedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = NovaColors.TextHint,
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item? It will be moved to the recycle bin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteItem(permanent = false) {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Delete", color = NovaColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DetailFieldCard(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null,
    onOpenUrl: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = NovaColors.TextPrimary,
                )
            }

            if (onOpenUrl != null) {
                IconButton(onClick = onOpenUrl) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open URL",
                        tint = NovaColors.SecondaryAccent,
                    )
                }
            }

            if (onCopy != null) {
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = NovaColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPasswordCard(
    password: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isVisible) password else "••••••••••••",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = if (isVisible) FontFamily.Monospace else FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = NovaColors.TextPrimary,
                )
            }

            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide" else "Show",
                    tint = NovaColors.TextSecondary,
                )
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Password",
                    tint = NovaColors.TextSecondary,
                )
            }
        }
    }
}
