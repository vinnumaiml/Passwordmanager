package com.novavault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavault.ui.theme.NovaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectType: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF10162A),
        contentColor = NovaColors.TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Add to Vault",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NovaColors.TextPrimary,
                ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose the type of secret to encrypt and store",
                style = MaterialTheme.typography.bodySmall.copy(color = NovaColors.TextSecondary),
            )

            Spacer(modifier = Modifier.height(20.dp))

            QuickAddOptionItem(
                icon = Icons.Default.Key,
                iconTint = NovaColors.PrimaryAccent,
                title = "Login Credential",
                subtitle = "Username, password, TOTP secret & URLs",
                onClick = { onSelectType("LOGIN") },
            )

            QuickAddOptionItem(
                icon = Icons.Default.Description,
                iconTint = NovaColors.SecondaryAccent,
                title = "Secure Note",
                subtitle = "Encrypted markdown, recovery phrases & keys",
                onClick = { onSelectType("SECURE_NOTE") },
            )

            QuickAddOptionItem(
                icon = Icons.Default.CreditCard,
                iconTint = Color(0xFFFFB340),
                title = "Payment Card",
                subtitle = "Credit/debit cards, CVVs & expiry dates",
                onClick = { onSelectType("PAYMENT_CARD") },
            )

            QuickAddOptionItem(
                icon = Icons.Default.Wifi,
                iconTint = Color(0xFF60A5FA),
                title = "Wi-Fi Credential",
                subtitle = "Network SSID, WPA2/WPA3 password & router notes",
                onClick = { onSelectType("WIFI_CREDENTIAL") },
            )

            QuickAddOptionItem(
                icon = Icons.Default.Person,
                iconTint = Color(0xFFA78BFA),
                title = "Identity",
                subtitle = "Personal records, passports & addresses",
                onClick = { onSelectType("IDENTITY") },
            )
        }
    }
}

@Composable
private fun QuickAddOptionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = Color(0x1AFFFFFF),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = NovaColors.TextPrimary,
                    ),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = NovaColors.TextSecondary,
                    ),
                )
            }
        }
    }
}
