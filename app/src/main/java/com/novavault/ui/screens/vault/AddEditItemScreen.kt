package com.novavault.ui.screens.vault

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.IdentityFields
import com.novavault.domain.model.LoginFields
import com.novavault.domain.model.PaymentCardFields
import com.novavault.domain.model.SecureNoteFields
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.model.WifiFields
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordOptions
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.components.PasswordStrengthIndicator
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val generatePasswordUseCase: GeneratePasswordUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val itemId: String? = savedStateHandle["itemId"]
    val typeParam: String? = savedStateHandle["itemType"]

    var itemType by mutableStateOf(VaultItemType.LOGIN)
    var name by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var url by mutableStateOf("")
    var totpSecret by mutableStateOf("")
    var notes by mutableStateOf("")

    // Card fields
    var cardholder by mutableStateOf("")
    var cardNumber by mutableStateOf("")
    var expiryMonth by mutableStateOf("")
    var expiryYear by mutableStateOf("")
    var cvv by mutableStateOf("")

    // Note fields
    var noteContent by mutableStateOf("")

    // Wi-Fi fields
    var wifiSsid by mutableStateOf("")
    var wifiPassword by mutableStateOf("")

    var isSaving by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        typeParam?.takeIf { it.isNotBlank() }?.let { param ->
            itemType = runCatching { VaultItemType.valueOf(param) }.getOrDefault(VaultItemType.LOGIN)
        }

        if (!itemId.isNullOrBlank()) {
            viewModelScope.launch {
                val existing = vaultRepository.observeItem(itemId).firstOrNull()
                if (existing != null) {
                    itemType = existing.type
                    name = existing.name
                    existing.login?.let {
                        username = it.username
                        password = it.password
                        url = it.uris.firstOrNull().orEmpty()
                        totpSecret = it.totpSecret.orEmpty()
                        notes = it.notes
                    }
                    existing.secureNote?.let {
                        noteContent = it.content
                    }
                    existing.paymentCard?.let {
                        cardholder = it.cardholderName
                        cardNumber = it.number
                        expiryMonth = it.expiryMonth
                        expiryYear = it.expiryYear
                        cvv = it.cvv
                    }
                    existing.wifiCredential?.let {
                        wifiSsid = it.ssid
                        wifiPassword = it.password
                    }
                }
            }
        }
    }

    fun generateNewPassword() {
        password = generatePasswordUseCase.generatePassword(
            PasswordOptions(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        )
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) {
            errorMessage = "Name is required"
            return
        }

        isSaving = true
        errorMessage = null

        viewModelScope.launch {
            val item = VaultItem(
                id = itemId ?: UUID.randomUUID().toString(),
                type = itemType,
                name = name.trim(),
                updatedAt = Instant.now(),
                login = if (itemType == VaultItemType.LOGIN) {
                    LoginFields(
                        username = username.trim(),
                        password = password,
                        uris = if (url.isNotBlank()) listOf(url.trim()) else emptyList(),
                        totpSecret = totpSecret.trim().ifEmpty { null },
                        notes = notes.trim(),
                    )
                } else null,
                secureNote = if (itemType == VaultItemType.SECURE_NOTE) {
                    SecureNoteFields(content = noteContent)
                } else null,
                paymentCard = if (itemType == VaultItemType.PAYMENT_CARD) {
                    PaymentCardFields(
                        cardholderName = cardholder.trim(),
                        number = cardNumber.trim(),
                        expiryMonth = expiryMonth.trim(),
                        expiryYear = expiryYear.trim(),
                        cvv = cvv.trim(),
                    )
                } else null,
                wifiCredential = if (itemType == VaultItemType.WIFI_CREDENTIAL) {
                    WifiFields(ssid = wifiSsid.trim(), password = wifiPassword)
                } else null,
            )

            val result = vaultRepository.saveItem(item)
            isSaving = false
            if (result.isSuccess) {
                onDone()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Failed to save item"
            }
        }
    }
}

@Composable
fun AddEditItemScreen(
    itemId: String? = null,
    itemType: String? = null,
    navController: NavController,
    viewModel: AddEditItemViewModel = hiltViewModel(),
) {
    AuroraBackground {
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
                        contentDescription = "Cancel",
                        tint = NovaColors.TextPrimary,
                    )
                }

                IconButton(
                    onClick = { viewModel.save { navController.popBackStack() } },
                    enabled = !viewModel.isSaving,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = NovaColors.SecondaryAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (viewModel.itemId != null) "Edit ${viewModel.itemType.name.replace("_", " ")}" else "New ${viewModel.itemType.name.replace("_", " ")}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = NovaColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Common field: Name
            NovaTextField(
                value = viewModel.name,
                onValueChange = {
                    viewModel.name = it
                    viewModel.errorMessage = null
                },
                label = "Title / Service Name",
                isError = viewModel.errorMessage != null,
                errorMessage = viewModel.errorMessage,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Type-specific fields
            when (viewModel.itemType) {
                VaultItemType.LOGIN -> {
                    NovaTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        label = "Username / Email",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NovaTextField(
                                value = viewModel.password,
                                onValueChange = { viewModel.password = it },
                                label = "Password",
                                isPassword = true,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.generateNewPassword() },
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Generate Password",
                                tint = NovaColors.PrimaryAccent,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    NovaTextField(
                        value = viewModel.url,
                        onValueChange = { viewModel.url = it },
                        label = "Website URL",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NovaTextField(
                        value = viewModel.totpSecret,
                        onValueChange = { viewModel.totpSecret = it },
                        label = "TOTP Seed / Secret (Optional)",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NovaTextField(
                        value = viewModel.notes,
                        onValueChange = { viewModel.notes = it },
                        label = "Notes",
                        singleLine = false,
                    )
                }

                VaultItemType.SECURE_NOTE -> {
                    NovaTextField(
                        value = viewModel.noteContent,
                        onValueChange = { viewModel.noteContent = it },
                        label = "Secure Content",
                        singleLine = false,
                    )
                }

                VaultItemType.PAYMENT_CARD -> {
                    NovaTextField(
                        value = viewModel.cardholder,
                        onValueChange = { viewModel.cardholder = it },
                        label = "Cardholder Name",
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    NovaTextField(
                        value = viewModel.cardNumber,
                        onValueChange = { viewModel.cardNumber = it },
                        label = "Card Number",
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            NovaTextField(
                                value = viewModel.expiryMonth,
                                onValueChange = { viewModel.expiryMonth = it },
                                label = "MM",
                            )
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            NovaTextField(
                                value = viewModel.expiryYear,
                                onValueChange = { viewModel.expiryYear = it },
                                label = "YY",
                            )
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            NovaTextField(
                                value = viewModel.cvv,
                                onValueChange = { viewModel.cvv = it },
                                label = "CVV",
                                isPassword = true,
                            )
                        }
                    }
                }

                VaultItemType.WIFI_CREDENTIAL -> {
                    NovaTextField(
                        value = viewModel.wifiSsid,
                        onValueChange = { viewModel.wifiSsid = it },
                        label = "Wi-Fi Network Name (SSID)",
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    NovaTextField(
                        value = viewModel.wifiPassword,
                        onValueChange = { viewModel.wifiPassword = it },
                        label = "Password",
                        isPassword = true,
                    )
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(32.dp))

            NovaButton(
                text = if (viewModel.itemId != null) "Update Record" else "Save to Vault",
                onClick = { viewModel.save { navController.popBackStack() } },
                loading = viewModel.isSaving,
            )
        }
    }
}
