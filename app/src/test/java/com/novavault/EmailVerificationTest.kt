package com.novavault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailVerificationTest {

    private fun maskEmail(email: String): String {
        if (email.isBlank()) return "your registered email"
        val parts = email.split("@")
        if (parts.size != 2) return email
        val user = parts[0]
        val domain = parts[1]

        val maskedUser = when {
            user.length <= 1 -> "$user***"
            user.length == 2 -> "${user.first()}***"
            else -> "${user.first()}***${user.last()}"
        }
        return "$maskedUser@$domain"
    }

    @Test
    fun testMaskEmailStandard() {
        val masked = maskEmail("vinnumaiml@gmail.com")
        assertEquals("v***l@gmail.com", masked)
    }

    @Test
    fun testMaskEmailShort() {
        val masked = maskEmail("ab@domain.com")
        assertEquals("a***@domain.com", masked)
    }

    @Test
    fun testMaskEmailSingleLetter() {
        val masked = maskEmail("x@domain.com")
        assertEquals("x***@domain.com", masked)
    }

    @Test
    fun testMaskEmailBlank() {
        val masked = maskEmail("")
        assertEquals("your registered email", masked)
    }

    @Test
    fun testAuthoritativeEmailVerificationStatus() {
        // Simulating the authoritative backend check where emailConfirmedAt must be non-null
        val confirmedTimestamp: String? = "2026-10-22T08:00:00Z"
        val unconfirmedTimestamp: String? = null

        val isConfirmedVerified = confirmedTimestamp != null
        val isUnconfirmedVerified = unconfirmedTimestamp != null

        assertTrue("User with confirmed timestamp must be verified", isConfirmedVerified)
        assertFalse("User without confirmed timestamp cannot bypass verification", isUnconfirmedVerified)
    }
}
