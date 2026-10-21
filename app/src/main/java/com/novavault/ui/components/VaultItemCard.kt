package com.novavault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novavault.domain.model.VaultItem
import com.novavault.domain.model.VaultItemType
import com.novavault.ui.theme.NovaColors

@Composable
fun VaultItemCard(
    item: VaultItem,
    onClick: () -> Unit,
    onCopyPassword: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NovaColors.SurfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon pill
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = NovaColors.SurfaceElevated,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getItemIcon(item.type),
                        contentDescription = item.type.name,
                        tint = NovaColors.SecondaryAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val subtitle = when (item.type) {
                    VaultItemType.LOGIN -> item.login?.username.orEmpty().ifEmpty { item.login?.uris?.firstOrNull() ?: "Login" }
                    VaultItemType.SECURE_NOTE -> item.secureNote?.category ?: "Secure Note"
                    VaultItemType.PAYMENT_CARD -> "•••• ${item.paymentCard?.number?.takeLast(4).orEmpty()}"
                    VaultItemType.IDENTITY -> "${item.identity?.firstName.orEmpty()} ${item.identity?.lastName.orEmpty()}".trim().ifEmpty { "Identity" }
                    VaultItemType.WIFI_CREDENTIAL -> item.wifiCredential?.ssid ?: "Wi-Fi"
                    VaultItemType.API_TOKEN -> "API Token"
                    VaultItemType.SSH_KEY -> "SSH Key"
                    VaultItemType.CUSTOM -> "Custom Item"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Quick actions
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) NovaColors.Warning else NovaColors.TextHint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (item.type == VaultItemType.LOGIN && onCopyPassword != null) {
                IconButton(onClick = onCopyPassword, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Password",
                        tint = NovaColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun getItemIcon(type: VaultItemType): ImageVector = when (type) {
    VaultItemType.LOGIN -> Icons.Default.Lock
    VaultItemType.SECURE_NOTE -> Icons.Default.Description
    VaultItemType.PAYMENT_CARD -> Icons.Default.CreditCard
    VaultItemType.IDENTITY -> Icons.Default.Badge
    VaultItemType.WIFI_CREDENTIAL -> Icons.Default.Wifi
    VaultItemType.API_TOKEN -> Icons.Default.Key
    VaultItemType.SSH_KEY -> Icons.Default.Terminal
    VaultItemType.CUSTOM -> Icons.Default.VpnKey
}
