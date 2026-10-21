package com.novavault.util

import java.util.Base64

fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
fun String.fromHex(): ByteArray {
    val clean = replace(" ", "").replace("-", "").lowercase()
    return ByteArray(clean.length / 2) { i ->
        clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isStrongEnoughPassword(): Boolean {
    return length >= 8
}
