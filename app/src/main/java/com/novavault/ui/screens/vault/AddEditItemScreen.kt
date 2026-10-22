package com.novavault.ui.screens.vault

import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.crypto.KeystoreManager
import com.novavault.domain.model.IdentityFields
import com.novavault.domain.model.LoginFields
import com.novavault.domain.model.PaymentCardFields
import com.novavault.domain.model.SecureNoteFields
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.model.WifiFields
import com.novavault.domain.repository.SettingsRepository
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordOptions
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.LocalBiometricPromptManager
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.components.PasswordStrengthIndicator
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.BiometricPromptManager
import com.novavault.util.fromBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditItemViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val settingsRepository: SettingsRepository,
    private val keystoreManager: KeystoreManager,
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

    // In-line unlock dialog state to guarantee user input is NEVER lost
    var showUnlockDialog by mutableStateOf(false)
    var unlockPasswordInput by mutableStateOf("")
    var unlockErrorMessage by mutableStateOf<String?>(null)
    var isUnlocking by mutableStateOf(false)
    var isBiometricEnabled by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            isBiometricEnabled = settings.biometricUnlockEnabled &&
                    settings.wrappedDekBase64.isNotBlank() &&
                    settings.biometricIvBase64.isNotBlank()
        }

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

    fun isVaultUnlocked(): Boolean = vaultRepository.isUnlocked()

    val passwordStrength: PasswordStrength
        get() = generatePasswordUseCase.estimateStrength(password)

    fun generateNewPassword() {
        password = generatePasswordUseCase.generatePassword(
            PasswordOptions(length = 20, uppercase = true, lowercase = true, digits = true, symbols = true)
        )
    }

    suspend fun prepareBiometricCipher(): javax.crypto.Cipher? = withContext(Dispatchers.Default) {
        try {
            val settings = settingsRepository.getSettings()
            if (settings.biometricUnlockEnabled &&
                settings.biometricIvBase64.isNotBlank() &&
                settings.wrappedDekBase64.isNotBlank()
            ) {
                val iv = settings.biometricIvBase64.fromBase64()
                keystoreManager.getCipherForDecrypt(iv)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun save(onDone: () -> Unit) {
        if (name.isBlank()) {
            errorMessage = "Name is required"
            return
        }

        if (!vaultRepository.isUnlocked()) {
            showUnlockDialog = true
            return
        }

        executeSave(onDone)
    }

    private fun executeSave(onDone: () -> Unit) {
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
                val err = result.exceptionOrNull()?.message ?: "Failed to save item"
                if (err.contains("locked", ignoreCase = true)) {
                    showUnlockDialog = true
                } else {
                    errorMessage = err
                }
            }
        }
    }

    fun unlockWithPasswordAndSave(onDone: () -> Unit) {
        if (unlockPasswordInput.isBlank()) return
        isUnlocking = true
        unlockErrorMessage = null

        viewModelScope.launch {
            val result = vaultRepository.unlockWithMasterPassword(unlockPasswordInput.toCharArray())
            isUnlocking = false
            if (result.isSuccess) {
                showUnlockDialog = false
                unlockPasswordInput = ""
                executeSave(onDone)
            } else {
                unlockErrorMessage = "Incorrect Master Password. Please try again."
            }
        }
    }

    fun unlockWithBiometricsAndSave(cryptoObject: BiometricPrompt.CryptoObject, onDone: () -> Unit) {
        isUnlocking = true
        unlockErrorMessage = null
        viewModelScope.launch {
            val result = vaultRepository.unlockWithBiometrics(cryptoObject)
            isUnlocking = false
            if (result.isSuccess) {
                showUnlockDialog = false
                executeSave(onDone)
            } else {
                unlockErrorMessage = result.exceptionOrNull()?.message ?: "Biometric unlock failed"
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
    val context = LocalContext.current
    val biometricPromptManager = LocalBiometricPromptManager.current

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
                        contentDescription = "Back",
                        tint = NovaColors.TextPrimary,
                    )
                }
                Text(
                    text = if (viewModel.itemId != null) "Edit Record" else "Add New Item",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )
                IconButton(onClick = { viewModel.save { navController.popBackStack() } }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = NovaColors.SecondaryAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            if (viewModel.itemId == null) {
                Text(
                    text = "RECORD TYPE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val types = listOf(
                        VaultItemType.LOGIN to "Login",
                        VaultItemType.SECURE_NOTE to "Note",
                        VaultItemType.PAYMENT_CARD to "Card",
                        VaultItemType.WIFI_CREDENTIAL to "Wi-Fi",
                    )
                    items(types) { (type, label) ->
                        val selected = viewModel.itemType == type
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) NovaColors.PrimaryAccent else NovaColors.SurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.itemType = type },
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else NovaColors.TextSecondary,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (viewModel.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = viewModel.errorMessage.orEmpty(),
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // Common Name Field
            NovaTextField(
                value = viewModel.name,
                onValueChange = {
                    viewModel.name = it
                    viewModel.errorMessage = null
                },
                label = "Record Name (e.g. GitHub, Netflix, Home Wi-Fi)",
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (viewModel.itemType) {
                VaultItemType.LOGIN -> {
                    NovaTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        label = "Username or Email",
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
                                tint = NovaColors.SecondaryAccent,
                            )
                        }
                    }
                    if (viewModel.password.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthIndicator(
                            strength = viewModel.passwordStrength,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
                        label = "2FA Secret Key (TOTP Optional)",
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    NovaTextField(
                        value = viewModel.notes,
                        onValueChange = { viewModel.notes = it },
                        label = "Notes (Optional)",
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

    // Vault Locked In-line Unlock Dialog — ensures user never loses entered password data
    if (viewModel.showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.isUnlocking) viewModel.showUnlockDialog = false },
            containerColor = Color(0xFF10162A),
            titleContentColor = Color.White,
            textContentColor = NovaColors.TextSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = NovaColors.PrimaryAccent)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Vault is Locked", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Master Password or authenticate to decrypt the vault and save this record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (viewModel.unlockErrorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = viewModel.unlockErrorMessage.orEmpty(),
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }

                    NovaTextField(
                        value = viewModel.unlockPasswordInput,
                        onValueChange = {
                            viewModel.unlockPasswordInput = it
                            viewModel.unlockErrorMessage = null
                        },
                        label = "Master Password",
                        isPassword = true,
                        leadingIcon = Icons.Default.Lock,
                    )

                    if (viewModel.isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            IconButton(
                                onClick = {
                                    if (context is FragmentActivity) {
                                        kotlinx.coroutines.MainScope().launch {
                                            val cipher = viewModel.prepareBiometricCipher()
                                            if (cipher != null) {
                                                biometricPromptManager.prompt(
                                                    activity = context,
                                                    title = "Unlock Elvarix",
                                                    subtitle = "Authenticate to save record",
                                                    cryptoObject = BiometricPrompt.CryptoObject(cipher),
                                                ) { result ->
                                                    if (result is BiometricPromptManager.BiometricResult.Success && result.cryptoObject != null) {
                                                        viewModel.unlockWithBiometricsAndSave(result.cryptoObject) {
                                                            navController.popBackStack()
                                                        }
                                                    }
                                                }
                                            } else {
                                                viewModel.unlockErrorMessage = "Biometrics changed. Use Master Password."
                                            }
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = NovaColors.SecondaryAccent,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlockWithPasswordAndSave {
                            navController.popBackStack()
                        }
                    },
                    enabled = !viewModel.isUnlocking && viewModel.unlockPasswordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NovaColors.PrimaryAccent),
                ) {
                    Text(if (viewModel.isUnlocking) "Unlocking..." else "Unlock & Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.showUnlockDialog = false },
                    enabled = !viewModel.isUnlocking,
                ) {
                    Text("Cancel", color = NovaColors.TextSecondary)
                }
            },
        )
    }
}
