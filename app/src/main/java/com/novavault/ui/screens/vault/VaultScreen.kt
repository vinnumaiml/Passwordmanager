package com.novavault.ui.screens.vault

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.components.VaultItemCard
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedType = MutableStateFlow<VaultItemType?>(null)

    val isVaultLocked: StateFlow<Boolean> = vaultRepository.vaultState.map {
        it is com.novavault.domain.model.VaultState.Locked
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val allItems = vaultRepository.observeItems()

    val filteredItems: StateFlow<List<VaultItem>> = combine(
        allItems,
        searchQuery,
        selectedType,
    ) { items, query, type ->
        items.filter { item ->
            !item.isDeleted &&
                    (type == null || item.type == type) &&
                    (query.isBlank() ||
                            item.name.contains(query, ignoreCase = true) ||
                            item.login?.username?.contains(query, ignoreCase = true) == true ||
                            item.login?.uris?.any { it.contains(query, ignoreCase = true) } == true ||
                            item.tags.any { it.contains(query, ignoreCase = true) })
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleFavorite(item: VaultItem) {
        viewModelScope.launch {
            vaultRepository.saveItem(item.copy(isFavorite = !item.isFavorite))
        }
    }
}

@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isVaultLocked by viewModel.isVaultLocked.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    var showTypePicker by remember { mutableStateOf(false) }

    AuroraBackground(isQuiet = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Header & search
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Vault",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search vault…", color = NovaColors.TextHint) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NovaColors.TextSecondary,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NovaColors.SurfaceVariant,
                        unfocusedContainerColor = NovaColors.SurfaceVariant,
                        focusedBorderColor = NovaColors.PrimaryAccent,
                        unfocusedBorderColor = NovaColors.Border,
                        focusedTextColor = NovaColors.TextPrimary,
                        unfocusedTextColor = NovaColors.TextPrimary,
                    ),
                    singleLine = true,
                )
            }

            // Category filter chips
            val categories = listOf(
                null to "All",
                VaultItemType.LOGIN to "Logins",
                VaultItemType.SECURE_NOTE to "Notes",
                VaultItemType.PAYMENT_CARD to "Cards",
                VaultItemType.IDENTITY to "Identities",
                VaultItemType.WIFI_CREDENTIAL to "Wi-Fi",
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                items(categories) { (type, label) ->
                    val isSelected = selectedType == type
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectedType.value = type },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NovaColors.PrimaryAccent else NovaColors.SurfaceElevated,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NovaColors.TextPrimary else NovaColors.TextSecondary,
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            // Items list
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isVaultLocked) NovaColors.Warning else NovaColors.TextHint,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isVaultLocked) "Vault is Locked"
                            else if (query.isNotBlank()) "No matching records found"
                            else "No items in this category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaColors.TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isVaultLocked) "Unlock your vault with Master Password or biometric authentication to access and manage your records."
                            else "Tap + below to add your first record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaColors.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        if (isVaultLocked) {
                            Spacer(modifier = Modifier.height(18.dp))
                            androidx.compose.material3.Button(
                                onClick = { navController.navigate(NavRoute.VaultLock.route) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NovaColors.PrimaryAccent),
                            ) {
                                Text("Unlock Vault")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredItems, key = { it.id }) { item ->
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

        // Working Floating Action Button (FAB) to Add Item
        FloatingActionButton(
            onClick = {
                if (isVaultLocked) {
                    navController.navigate(NavRoute.VaultLock.route)
                } else {
                    val targetType = selectedType?.name ?: "LOGIN"
                    navController.navigate(NavRoute.AddItem.createRoute(targetType))
                }
            },
            shape = androidx.compose.foundation.shape.CircleShape,
            containerColor = NovaColors.PrimaryAccent,
            contentColor = NovaColors.Background,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 10.dp,
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Item",
                tint = NovaColors.Background,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
