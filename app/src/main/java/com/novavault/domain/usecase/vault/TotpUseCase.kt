package com.novavault.domain.usecase.vault

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

// ──────────────────────────────────────────────────────────────────────────────
// Data model
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Parsed representation of an `otpauth://` URI as defined by the Google Authenticator
 * Key URI Format specification.
 */
data class OtpAuthParams(
    /** Base32-encoded TOTP secret (without spaces or hyphens). */
    val secret: String,
    /** Issuer / service name (e.g., "Google", "GitHub"). */
    val issuer: String,
    /** Account identifier (e.g., user@example.com). */
    val account: String,
    /** Number of OTP digits (usually 6 or 8). */
    val digits: Int = 6,
    /** Time step in seconds (usually 30). */
    val period: Long = 30L,
    /** HMAC algorithm: SHA1, SHA256, or SHA512. */
    val algorithm: String = "SHA1",
)

// ──────────────────────────────────────────────────────────────────────────────
// Use case
// ──────────────────────────────────────────────────────────────────────────────

/**
 * RFC 6238 TOTP implementation.
 *
 * Supported HMAC algorithms: HmacSHA1, HmacSHA256, HmacSHA512.
 * Secret encoding: Base32 (RFC 4648, no padding required).
 */
@Singleton
class TotpUseCase @Inject constructor() {

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generate the current TOTP code for [secret].
     *
     * @param secret    Base32-encoded TOTP secret (spaces and hyphens are stripped)
     * @param timeStep  TOTP window in seconds (default 30)
     * @param digits    Output length in digits (default 6)
     * @param algorithm HMAC algorithm — "SHA1", "SHA256", or "SHA512"
     * @return zero-padded numeric string of length [digits]
     * @throws IllegalArgumentException if [secret] is invalid Base32 or algorithm unsupported
     */
    fun generateTotp(
        secret: String,
        timeStep: Long = 30L,
        digits: Int = 6,
        algorithm: String = "SHA1",
    ): String {
        val keyBytes = decodeBase32(secret)
        val counter  = System.currentTimeMillis() / 1000L / timeStep
        return computeHotp(keyBytes, counter, digits, algorithm)
    }

    /**
     * Parse an `otpauth://totp/` URI into structured [OtpAuthParams].
     *
     * Expected format:
     * `otpauth://totp/LABEL?secret=SECRET&issuer=ISSUER&digits=6&period=30&algorithm=SHA1`
     *
     * @throws IllegalArgumentException if URI scheme/type is not `otpauth://totp/`
     * @throws IllegalArgumentException if the `secret` parameter is missing
     */
    fun parseOtpAuthUri(uri: String): OtpAuthParams {
        require(uri.startsWith("otpauth://totp/", ignoreCase = true)) {
            "Unsupported OTP URI: must start with otpauth://totp/"
        }

        val withoutScheme = uri.removePrefix("otpauth://totp/")
        val queryStart    = withoutScheme.indexOf('?')
        val rawLabel      = if (queryStart >= 0) withoutScheme.substring(0, queryStart)
                            else withoutScheme
        val queryString   = if (queryStart >= 0) withoutScheme.substring(queryStart + 1) else ""

        // Decode label: "Issuer:Account" or just "Account"
        val decodedLabel = rawLabel.urlDecode()
        val (labelIssuer, account) = if (':' in decodedLabel) {
            val parts = decodedLabel.split(":", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            "" to decodedLabel.trim()
        }

        // Parse query parameters
        val params = queryString
            .split("&")
            .filter { it.isNotBlank() }
            .associate { pair ->
                val eqIdx = pair.indexOf('=')
                if (eqIdx < 0) pair.urlDecode() to ""
                else pair.substring(0, eqIdx).urlDecode() to pair.substring(eqIdx + 1).urlDecode()
            }

        val secret = params["secret"]?.replace(" ", "")?.replace("-", "")?.uppercase()
            ?: throw IllegalArgumentException("OTP URI is missing required 'secret' parameter")

        require(validateSecret(secret)) { "OTP secret is not valid Base32" }

        val issuer    = params["issuer"]?.urlDecode() ?: labelIssuer
        val digits    = params["digits"]?.toIntOrNull() ?: 6
        val period    = params["period"]?.toLongOrNull() ?: 30L
        val algorithm = params["algorithm"]?.uppercase() ?: "SHA1"

        return OtpAuthParams(
            secret    = secret,
            issuer    = issuer,
            account   = account,
            digits    = digits,
            period    = period,
            algorithm = algorithm,
        )
    }

    /**
     * Number of seconds remaining in the current TOTP window (default 30-second step).
     */
    fun getRemainingSeconds(timeStep: Long = 30L): Long {
        val nowSeconds = System.currentTimeMillis() / 1000L
        return timeStep - (nowSeconds % timeStep)
    }

    /**
     * Return `true` if [secret] is a non-empty, valid Base32 string.
     * Accepts both padded and unpadded Base32 (spaces and hyphens stripped).
     */
    fun validateSecret(secret: String): Boolean {
        if (secret.isBlank()) return false
        val cleaned = secret.replace(" ", "").replace("-", "").uppercase().trimEnd('=')
        return cleaned.isNotEmpty() && cleaned.all { it in BASE32_ALPHABET }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HOTP / HMAC internals
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * RFC 4226 HOTP for a given [counter] value.
     */
    private fun computeHotp(
        keyBytes: ByteArray,
        counter: Long,
        digits: Int,
        algorithm: String,
    ): String {
        val jcaAlgorithm = when (algorithm.uppercase()) {
            "SHA1"   -> "HmacSHA1"
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else     -> throw IllegalArgumentException("Unsupported TOTP algorithm: $algorithm")
        }

        // Counter as big-endian 8-byte array
        val msg = ByteArray(8)
        var c   = counter
        for (i in 7 downTo 0) {
            msg[i] = (c and 0xFF).toByte()
            c = c shr 8
        }

        val mac = Mac.getInstance(jcaAlgorithm)
        mac.init(SecretKeySpec(keyBytes, jcaAlgorithm))
        val hmac = mac.doFinal(msg)

        // Dynamic truncation (RFC 4226 §5.3)
        val offset = hmac.last().toInt() and 0x0F
        val code   = ((hmac[offset].toInt()     and 0x7F) shl 24) or
                     ((hmac[offset + 1].toInt() and 0xFF) shl 16) or
                     ((hmac[offset + 2].toInt() and 0xFF) shl 8)  or
                      (hmac[offset + 3].toInt() and 0xFF)

        val divisor = Math.pow(10.0, digits.toDouble()).toInt()
        return (code % divisor).toString().padStart(digits, '0')
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Base32 decoder (RFC 4648)
    // ──────────────────────────────────────────────────────────────────────────

    private val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Decode a Base32-encoded string (unpadded or padded) to bytes.
     *
     * @throws IllegalArgumentException if the input contains characters outside the Base32 alphabet
     */
    private fun decodeBase32(input: String): ByteArray {
        val cleaned = input.replace(" ", "").replace("-", "").uppercase().trimEnd('=')
        require(cleaned.all { it in BASE32_ALPHABET }) {
            "Invalid Base32 character in secret"
        }

        val outputBits = cleaned.length * 5
        val outputBytes = outputBits / 8
        val result = ByteArray(outputBytes)

        var buffer   = 0L
        var bitsLeft = 0
        var byteIdx  = 0

        for (ch in cleaned) {
            val value = BASE32_ALPHABET.indexOf(ch)
            buffer   = (buffer shl 5) or value.toLong()
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result[byteIdx++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }

        return result
    }

    // ──────────────────────────────────────────────────────────────────────────
    // URL percent-decoding helper (no external deps)
    // ──────────────────────────────────────────────────────────────────────────

    private fun String.urlDecode(): String {
        val sb = StringBuilder()
        var i  = 0
        while (i < length) {
            when {
                this[i] == '+' -> { sb.append(' '); i++ }
                this[i] == '%' && i + 2 < length -> {
                    val hex = substring(i + 1, i + 3)
                    sb.append(hex.toInt(16).toChar())
                    i += 3
                }
                else -> { sb.append(this[i]); i++ }
            }
        }
        return sb.toString()
    }
}
