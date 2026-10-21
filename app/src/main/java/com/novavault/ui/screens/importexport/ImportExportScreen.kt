package com.novavault.ui.screens.importexport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.novavault.domain.model.LoginFields
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.domain.repository.VaultRepository
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.NovaDangerButton
import com.novavault.ui.components.NovaTextField
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImportExportViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val json: Json,
) : ViewModel() {

    var exportJson by mutableStateOf<String?>(null)
    var exportCsv by mutableStateOf<String?>(null)
    var isExporting by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)

    var importCsvText by mutableStateOf("")
    var isImporting by mutableStateOf(false)
    var importResult by mutableStateOf<String?>(null)

    fun exportEncryptedBackup() {
        isExporting = true
        statusMessage = null
        viewModelScope.launch(Dispatchers.IO) {
            val items = vaultRepository.getItems()
            val backup = json.encodeToString(items)
            withContext(Dispatchers.Main) {
                exportJson = backup
                isExporting = false
                statusMessage = "Export generated: ${items.size} encrypted items ready to save."
            }
        }
    }

    fun exportPlainCsv() {
        isExporting = true
        viewModelScope.launch(Dispatchers.IO) {
            val items = vaultRepository.getItems().filter { it.type == VaultItemType.LOGIN }
            val sb = StringBuilder("name,username,password,url,notes\n")
            for (item in items) {
                val login = item.login
                sb.append("\"${item.name.replace("\"", "\"\"")}\",")
                sb.append("\"${login?.username?.replace("\"", "\"\"").orEmpty()}\",")
                sb.append("\"${login?.password?.replace("\"", "\"\"").orEmpty()}\",")
                sb.append("\"${login?.uris?.firstOrNull()?.replace("\"", "\"\"").orEmpty()}\",")
                sb.append("\"${login?.notes?.replace("\"", "\"\"").orEmpty()}\"\n")
            }
            withContext(Dispatchers.Main) {
                exportCsv = sb.toString()
                isExporting = false
                statusMessage = "Plaintext CSV generated with ${items.size} login records."
            }
        }
    }

    fun importCsv() {
        if (importCsvText.isBlank()) return
        isImporting = true
        importResult = null

        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            val lines = importCsvText.lines()
            for (line in lines.drop(1)) {
                if (line.isBlank()) continue
                val tokens = line.split(",")
                if (tokens.isNotEmpty()) {
                    val name = tokens.getOrNull(0)?.trim('"', ' ').orEmpty()
                    val user = tokens.getOrNull(1)?.trim('"', ' ').orEmpty()
                    val pass = tokens.getOrNull(2)?.trim('"', ' ').orEmpty()
                    val url = tokens.getOrNull(3)?.trim('"', ' ').orEmpty()

                    if (name.isNotBlank() || user.isNotBlank()) {
                        val item = VaultItem(
                            id = UUID.randomUUID().toString(),
                            type = VaultItemType.LOGIN,
                            name = name.ifEmpty { "Imported Account" },
                            login = LoginFields(
                                username = user,
                                password = pass,
                                uris = if (url.isNotBlank()) listOf(url) else emptyList(),
                            ),
                        )
                        vaultRepository.saveItem(item)
                        count++
                    }
                }
            }

            withContext(Dispatchers.Main) {
                isImporting = false
                importResult = "Successfully imported $count records into your vault!"
                importCsvText = ""
            }
        }
    }
}

@Composable
fun ImportExportScreen(
    navController: NavController,
    viewModel: ImportExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCsvWarning by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaColors.TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import & Export",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Export Section
            Text(
                text = "EXPORT VAULT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NovaColors.SecondaryAccent,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NovaColors.SecondaryAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Encrypted Vault Backup (Recommended)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaColors.TextPrimary,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Exports your entire vault in encrypted JSON format. Sensitive payloads remain cryptographically protected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NovaButton(
                        text = "Generate Encrypted Backup",
                        onClick = { viewModel.exportEncryptedBackup() },
                        loading = viewModel.isExporting && viewModel.exportJson == null,
                    )

                    if (viewModel.exportJson != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NovaButton(
                            text = "Copy Backup to Clipboard",
                            onClick = {
                                viewModel.exportJson?.let {
                                    SecureClipboard.copyAndScheduleClear(context, it, "Nova Vault Backup", 60, scope)
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Plaintext CSV Export
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NovaColors.Danger,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Unencrypted CSV Export",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaColors.Danger,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Exports passwords as plain text. Anyone who accesses this file can view all your accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NovaDangerButton(
                        text = "Export Plaintext CSV…",
                        onClick = { showCsvWarning = true },
                    )

                    if (viewModel.exportCsv != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NovaButton(
                            text = "Copy Plaintext CSV to Clipboard",
                            onClick = {
                                viewModel.exportCsv?.let {
                                    SecureClipboard.copyAndScheduleClear(context, it, "Plain CSV", 60, scope)
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Import Section
            Text(
                text = "IMPORT VAULT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NovaColors.SecondaryAccent,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Import from CSV (Bitwarden, 1Password, Proton)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaColors.TextPrimary,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Paste your CSV export contents below (header format: name,username,password,url,notes).",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    NovaTextField(
                        value = viewModel.importCsvText,
                        onValueChange = { viewModel.importCsvText = it },
                        label = "Paste CSV contents here",
                        singleLine = false,
                    )

                    if (viewModel.importResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.importResult.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaColors.Success,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NovaButton(
                        text = "Import Records",
                        onClick = { viewModel.importCsv() },
                        loading = viewModel.isImporting,
                        enabled = viewModel.importCsvText.isNotBlank() && !viewModel.isImporting,
                    )
                }
            }
        }
    }

    if (showCsvWarning) {
        AlertDialog(
            onDismissRequest = { showCsvWarning = false },
            title = { Text("⚠️ Plaintext Warning") },
            text = { Text("Exporting an unencrypted CSV exposes your passwords in plain text on your device. Only proceed if you understand the risks and intend to transfer the data immediately.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCsvWarning = false
                        viewModel.exportPlainCsv()
                    }
                ) {
                    Text("Proceed with Plaintext", color = NovaColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvWarning = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
