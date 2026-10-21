package com.novavault.crypto

import kotlinx.serialization.Serializable

/**
 * Serializable top-level vault key envelope stored locally and optionally
 * synchronized to Supabase (server never sees plaintext DEK or master password).
 *
 * envelopeVersion   — migration path for future crypto changes
 * kdfParams         — Argon2id parameters + salt (public; required for re-derivation)
 * encryptedDek      — AES-256-GCM(KEK, DEK) — KEK derived from master password
 * encryptedRecovery — AES-256-GCM(recoveryKek, DEK) — recoveryKek derived from recovery key
 */
@Serializable
data class VaultKeyEnvelope(
    val envelopeVersion: Int = CryptoConstants.ENVELOPE_VERSION,
    val kdfParams: KdfParams,
    val encryptedDek: EncryptedBlob,
    val encryptedRecovery: EncryptedBlob? = null,
    val recoveryKdfParams: KdfParams? = null,
)
