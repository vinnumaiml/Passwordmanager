package com.novavault.ui.screens.security

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.components.NovaButton
import com.novavault.ui.navigation.NavRoute
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

data class SecurityHealthReport(
    val weakPasswords: List<VaultItem>,
    val reusedPasswords: List<VaultItem>,
    val missingTotp: List<VaultItem>,
    val oldPasswords: List<VaultItem>,
    val score: Int,
    val grade: String,
)

@HiltViewModel
class SecurityCenterViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val passwordUseCase: GeneratePasswordUseCase,
) : ViewModel() {

    private val items = vaultRepository.observeItems()

    val report: StateFlow<SecurityHealthReport> = items.map { list ->
        val logins = list.filter { it.type == VaultItemType.LOGIN && !it.isDeleted }

        val weak = logins.filter {
            val s = passwordUseCase.estimateStrength(it.login?.password.orEmpty())
            s == PasswordStrength.VERY_WEAK || s == PasswordStrength.WEAK
        }

        val passwordGroups = logins.groupBy { it.login?.password.orEmpty() }
        val reused = logins.filter {
            val count = passwordGroups[it.login?.password.orEmpty()]?.size ?: 0
            count > 1 && it.login?.password?.isNotBlank() == true
        }

        val noTotp = logins.filter { it.login?.totpSecret.isNullOrBlank() }

        val now = Instant.now()
        val old = logins.filter {
            Duration.between(it.updatedAt, now).toDays() > 180
        }

        val totalDeductions = (weak.size * 20) + (reused.size * 15) + (old.size * 5)
        val score = (100 - totalDeductions).coerceIn(0, 100)

        val grade = when {
            score >= 95 -> "A+"
            score >= 85 -> "A"
            score >= 75 -> "B"
            score >= 60 -> "C"
            score >= 40 -> "D"
            else -> "F"
        }

        SecurityHealthReport(
            weakPasswords = weak,
            reusedPasswords = reused,
            missingTotp = noTotp,
            oldPasswords = old,
            score = if (logins.isEmpty()) 100 else score,
            grade = if (logins.isEmpty()) "A+" else grade,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        SecurityHealthReport(emptyList(), emptyList(), emptyList(), emptyList(), 100, "A+"),
    )

    var isCheckingBreaches by mutableStateOf(false)
    var breachCount by mutableStateOf<Int?>(null)

    fun checkPwnedPasswords() {
        isCheckingBreaches = true
        breachCount = null
        viewModelScope.launch(Dispatchers.IO) {
            val logins = vaultRepository.getItems().filter { it.type == VaultItemType.LOGIN && !it.isDeleted }
            var foundBreached = 0

            for (login in logins) {
                val pwd = login.login?.password.orEmpty()
                if (pwd.isBlank()) continue
                if (isPasswordExposedViaHibp(pwd)) {
                    foundBreached++
                }
            }

            withContext(Dispatchers.Main) {
                isCheckingBreaches = false
                breachCount = foundBreached
            }
        }
    }

    private fun isPasswordExposedViaHibp(password: String): Boolean {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val hash = md.digest(password.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02X".format(it) }

            val prefix = hash.take(5)
            val suffix = hash.substring(5)

            val url = URL("https://api.pwnedpasswords.com/range/$prefix")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "NovaVault-Android")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                response.lineSequence().any { line ->
                    line.startsWith(suffix, ignoreCase = true)
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

@Composable
fun SecurityCenterScreen(
    navController: NavController,
    viewModel: SecurityCenterViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsState()

    AuroraBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Security Center",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextPrimary,
                    )
                }
            }

            // Overall Grade Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceElevated),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Vault Health Score",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NovaColors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${report.score} / 100 points",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaColors.TextSecondary,
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = when {
                                report.score >= 80 -> NovaColors.SuccessDim
                                report.score >= 60 -> NovaColors.WarningDim
                                else -> NovaColors.DangerDim
                            },
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = report.grade,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            report.score >= 80 -> NovaColors.Success
                                            report.score >= 60 -> NovaColors.Warning
                                            else -> NovaColors.Danger
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // Breach Check Card (HIBP k-anonymity)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NovaColors.SecondaryAccent,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Known Breach Exposure Check",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = NovaColors.TextPrimary,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Audits your passwords against exposed breach databases using k-anonymity. Only the first 5 characters of your password's SHA-1 hash are checked. Your password is never sent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NovaColors.TextSecondary,
                        )

                        if (viewModel.breachCount != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (viewModel.breachCount == 0) "✅ No exposed passwords found in breaches!" else "⚠️ ${viewModel.breachCount} passwords detected in known data breaches!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (viewModel.breachCount == 0) NovaColors.Success else NovaColors.Danger,
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        NovaButton(
                            text = if (viewModel.isCheckingBreaches) "Checking Breach Database…" else "Scan for Exposed Passwords",
                            onClick = { viewModel.checkPwnedPasswords() },
                            loading = viewModel.isCheckingBreaches,
                        )
                    }
                }
            }

            // Category Findings
            item {
                Text(
                    text = "Vulnerability Breakdown",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextSecondary,
                )
            }

            item {
                FindingCard(
                    icon = Icons.Default.Warning,
                    iconTint = NovaColors.Danger,
                    title = "Weak Passwords",
                    count = report.weakPasswords.size,
                    description = "Short or predictable passwords vulnerable to brute force.",
                    onClick = {},
                )
            }

            item {
                FindingCard(
                    icon = Icons.Default.ContentCopy,
                    iconTint = NovaColors.Warning,
                    title = "Reused Passwords",
                    count = report.reusedPasswords.size,
                    description = "Passwords shared across multiple online accounts.",
                    onClick = {},
                )
            }

            item {
                FindingCard(
                    icon = Icons.Default.Security,
                    iconTint = NovaColors.PrimaryAccent,
                    title = "Accounts Missing TOTP",
                    count = report.missingTotp.size,
                    description = "Accounts without two-factor authentication configured.",
                    onClick = {},
                )
            }

            item {
                FindingCard(
                    icon = Icons.Default.Schedule,
                    iconTint = NovaColors.SecondaryAccent,
                    title = "Old Credentials (> 180 days)",
                    count = report.oldPasswords.size,
                    description = "Passwords that haven't been changed in over 6 months.",
                    onClick = {},
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun FindingCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    count: Int,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NovaColors.SurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (count == 0) NovaColors.SuccessDim else NovaColors.DangerDim,
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (count == 0) NovaColors.Success else NovaColors.Danger,
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
