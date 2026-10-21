package com.novavault.crypto

import kotlinx.serialization.Serializable

@Serializable
data class KdfParams(
    val algorithm: String = CryptoConstants.KDF_ALGORITHM,
    val version: Int = CryptoConstants.KDF_VERSION,
    val memoryKib: Int = CryptoConstants.ARGON2_MEMORY_KIB,
    val iterations: Int = CryptoConstants.ARGON2_ITERATIONS,
    val parallelism: Int = CryptoConstants.ARGON2_PARALLELISM,
    val hashLen: Int = CryptoConstants.ARGON2_HASH_LEN,
    val saltBase64: String = "",   // populated when storing params
)
