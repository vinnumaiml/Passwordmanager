package com.novavault

import com.novavault.domain.usecase.vault.TotpUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TotpUseCaseTest {

    private lateinit var totpUseCase: TotpUseCase

    @Before
    fun setUp() {
        totpUseCase = TotpUseCase()
    }

    @Test
    fun testValidateBase32Secret() {
        assertTrue(totpUseCase.validateSecret("JBSWY3DPEHPK3PXP"))
        assertTrue(totpUseCase.validateSecret("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"))
        assertFalse(totpUseCase.validateSecret("invalid-secret-890!"))
        assertFalse(totpUseCase.validateSecret(""))
    }

    @Test
    fun testGenerateTotpProducesSixDigits() {
        val secret = "JBSWY3DPEHPK3PXP" // Standard test secret
        val code = totpUseCase.generateTotp(secret)
        assertNotNull(code)
        assertEquals(6, code.length)
        assertTrue(code.all { it.isDigit() })
    }

    @Test
    fun testRemainingSecondsWithinWindow() {
        val remaining = totpUseCase.getRemainingSeconds()
        assertTrue(remaining in 1..30)
    }
}
