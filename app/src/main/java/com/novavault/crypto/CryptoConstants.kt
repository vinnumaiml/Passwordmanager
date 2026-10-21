package com.novavault.crypto

object CryptoConstants {
    // AES-GCM
    const val GCM_TAG_BITS = 128
    const val GCM_NONCE_BYTES = 12
    const val AES_KEY_BITS = 256
    const val AES_KEY_BYTES = 32

    // Argon2id KDF — versioned parameters
    const val KDF_ALGORITHM = "argon2id"
    const val KDF_VERSION = 1

    // Baseline per OWASP MASVS (benchmarked on startup, these are minimums)
    const val ARGON2_MEMORY_KIB = 65536    // 64 MiB
    const val ARGON2_ITERATIONS = 3
    const val ARGON2_PARALLELISM = 4
    const val ARGON2_HASH_LEN = 32         // 256-bit KEK
    const val ARGON2_SALT_BYTES = 32

    // Envelope versioning
    const val ENVELOPE_VERSION = 1

    // Android Keystore
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val KEYSTORE_ALIAS_BIOMETRIC = "nova_vault_biometric_key"
    const val KEYSTORE_ALIAS_DEK_WRAP = "nova_vault_dek_wrap"

    // Recovery key
    const val RECOVERY_KEY_BYTES = 32
    const val RECOVERY_KEY_DISPLAY_GROUPS = 8
    const val RECOVERY_KEY_GROUP_SIZE = 4
}
