package com.novavault.ui.screens.totp

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.LoginFields
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.VaultRepository
import com.novavault.domain.usecase.vault.TotpUseCase
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TotpAccount(
    val id: String,
    val name: String,
    val username: String,
    val secret: String,
)

@HiltViewModel
class TotpViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val totpUseCase: TotpUseCase,
) : ViewModel() {

    val accounts: StateFlow<List<TotpAccount>> = vaultRepository.observeItems().map { list ->
        list.filter { it.type == VaultItemType.LOGIN && !it.isDeleted && !it.login?.totpSecret.isNullOrBlank() }
            .map {
                TotpAccount(
                    id = it.id,
                    name = it.name,
                    username = it.login?.username.orEmpty(),
                    secret = it.login?.totpSecret.orEmpty(),
                )
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var showAddDialog by mutableStateOf(false)
    var newServiceName by mutableStateOf("")
    var newAccountLabel by mutableStateOf("")
    var newSecret by mutableStateOf("")
    var secretError by mutableStateOf<String?>(null)

    fun calculateCode(secret: String): String {
        return try {
            val code = totpUseCase.generateTotp(secret)
            if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
        } catch (e: Exception) {
            "------"
        }
    }

    val remainingSeconds: Long
        get() = totpUseCase.getRemainingSeconds()

    fun addManualAccount(onDone: () -> Unit) {
        val cleanSecret = newSecret.replace(" ", "").uppercase()
        if (!totpUseCase.validateSecret(cleanSecret)) {
            secretError = "Invalid Base32 secret key"
            return
        }

        viewModelScope.launch {
            val item = VaultItem(
                id = UUID.randomUUID().toString(),
                type = VaultItemType.LOGIN,
                name = newServiceName.trim().ifEmpty { "2FA Service" },
                login = LoginFields(
                    username = newAccountLabel.trim(),
                    totpSecret = cleanSecret,
                ),
            )
            vaultRepository.saveItem(item)
            newServiceName = ""
            newAccountLabel = ""
            newSecret = ""
            secretError = null
            showAddDialog = false
            onDone()
        }
    }
}

@Composable
fun TotpScreen(
    navController: NavController,
    viewModel: TotpViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by viewModel.accounts.collectAsState()

    var secondsLeft by remember { mutableLongStateOf(viewModel.remainingSeconds) }

    // 1-second countdown ticker
    LaunchedEffect(Unit) {
        while (true) {
            secondsLeft = viewModel.remainingSeconds
            delay(1000L)
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Authenticator",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextPrimary,
                    )
                    Text(
                        text = "Time-based One-Time Passwords (RFC 6238)",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                }

                IconButton(onClick = { viewModel.showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Authenticator",
                        tint = NovaColors.SecondaryAccent,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = NovaColors.TextHint,
                            modifier = Modifier.size(60.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No 2FA accounts added yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = NovaColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' to add a two-factor authentication seed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaColors.TextSecondary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(accounts, key = { it.id }) { account ->
                        val code = viewModel.calculateCode(account.secret)
                        TotpAccountCard(
                            account = account,
                            code = code,
                            secondsLeft = secondsLeft,
                            onCopy = {
                                val cleanCode = code.replace(" ", "")
                                SecureClipboard.copyAndScheduleClear(context, cleanCode, "2FA Code", 30, scope)
                            },
                        )
                    }
                }
            }
        }
    }

    if (viewModel.showAddDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddDialog = false },
            title = { Text("Add 2FA Key") },
            text = {
                Column {
                    NovaTextField(
                        value = viewModel.newServiceName,
                        onValueChange = { viewModel.newServiceName = it },
                        label = "Service / Issuer (e.g. GitHub)",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NovaTextField(
                        value = viewModel.newAccountLabel,
                        onValueChange = { viewModel.newAccountLabel = it },
                        label = "Account Name (e.g. user@example.com)",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NovaTextField(
                        value = viewModel.newSecret,
                        onValueChange = {
                            viewModel.newSecret = it
                            viewModel.secretError = null
                        },
                        label = "Secret Key (Base32)",
                        isError = viewModel.secretError != null,
                        errorMessage = viewModel.secretError,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addManualAccount {} }) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showAddDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TotpAccountCard(
    account: TotpAccount,
    code: String,
    secondsLeft: Long,
    onCopy: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )
                if (account.username.isNotBlank()) {
                    Text(
                        text = account.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                    color = NovaColors.SecondaryAccent,
                )
            }

            // Circular countdown & copy button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    val progress = (secondsLeft.toFloat() / 30f).coerceIn(0f, 1f)
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(44.dp),
                        color = if (secondsLeft <= 5) NovaColors.Danger else NovaColors.SecondaryAccent,
                        trackColor = NovaColors.SurfaceElevated,
                        strokeWidth = 3.dp,
                    )
                    Text(
                        text = "$secondsLeft",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (secondsLeft <= 5) NovaColors.Danger else NovaColors.TextPrimary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy 2FA Code",
                        tint = NovaColors.TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
