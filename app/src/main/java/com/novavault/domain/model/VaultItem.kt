package com.novavault.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.util.UUID

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

@Serializable
enum class VaultItemType {
    LOGIN, SECURE_NOTE, PAYMENT_CARD, IDENTITY,
    WIFI_CREDENTIAL, API_TOKEN, SSH_KEY, CUSTOM
}

@Serializable
data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val type: VaultItemType,
    val name: String,
    val folderId: String? = null,
    val collectionId: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now(),
    // Type-specific fields (null when not applicable)
    val login: LoginFields? = null,
    val secureNote: SecureNoteFields? = null,
    val paymentCard: PaymentCardFields? = null,
    val identity: IdentityFields? = null,
    val wifiCredential: WifiFields? = null,
    val custom: CustomFields? = null,
)

@Serializable
data class LoginFields(
    val username: String = "",
    val password: String = "",
    val uris: List<String> = emptyList(),
    val totpSecret: String? = null,
    val notes: String = "",
    val customFields: List<CustomField> = emptyList(),
    val passwordHistory: List<PasswordHistoryEntry> = emptyList(),
)

@Serializable
data class SecureNoteFields(
    val content: String = "",
    val category: String? = null,
)

@Serializable
data class PaymentCardFields(
    val cardholderName: String = "",
    val number: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val billingAddress: String = "",
    val pin: String = "",
    val notes: String = "",
)

@Serializable
data class IdentityFields(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val country: String = "",
    val notes: String = "",
)

@Serializable
data class WifiFields(
    val ssid: String = "",
    val password: String = "",
    val security: String = "WPA2",
    val notes: String = "",
)

@Serializable
data class CustomFields(
    val fields: List<CustomField> = emptyList(),
    val notes: String = "",
)

@Serializable
data class CustomField(
    val label: String,
    val value: String,
    val isHidden: Boolean = false,
)

@Serializable
data class PasswordHistoryEntry(
    val password: String,
    @Serializable(with = InstantSerializer::class)
    val changedAt: Instant,
)
