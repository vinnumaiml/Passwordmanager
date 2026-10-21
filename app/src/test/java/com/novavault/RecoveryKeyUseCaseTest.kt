package com.novavault

import com.novavault.crypto.CryptoConstants
import com.novavault.domain.usecase.crypto.RecoveryKeyUseCase
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class RecoveryKeyUseCaseTest {

    private lateinit var useCase: RecoveryKeyUseCase

    @Before
    fun setUp() {
        useCase = RecoveryKeyUseCase()
    }

    @Test
    fun testGenerateRecoveryKeyProduces32Bytes() {
        val key = useCase.generateRecoveryKey()
        assertNotNull(key)
        assertEquals(CryptoConstants.RECOVERY_KEY_BYTES, key.size)
    }

    @Test
    fun testFormatAndParseRoundTrip() {
        val key = useCase.generateRecoveryKey()
        val formatted = useCase.formatForDisplay(key)

        assertNotNull(formatted)
        val parsed = useCase.parseFromDisplay(formatted)

        assertNotNull(parsed)
        assertArrayEquals(key, parsed)
    }
}
