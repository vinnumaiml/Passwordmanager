package com.novavault

import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordOptions
import com.novavault.domain.usecase.vault.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeneratePasswordUseCaseTest {

    private lateinit var useCase: GeneratePasswordUseCase

    @Before
    fun setUp() {
        useCase = GeneratePasswordUseCase()
    }

    @Test
    fun testGeneratedPasswordExactLength() {
        val lengths = listOf(8, 16, 24, 32, 64)
        for (len in lengths) {
            val pwd = useCase.generatePassword(PasswordOptions(length = len))
            assertEquals(len, pwd.length)
        }
    }

    @Test
    fun testGeneratePassphraseWordCount() {
        val passphrase = useCase.generatePassphrase(wordCount = 5, separator = "-")
        val words = passphrase.split("-")
        assertEquals(5, words.size)
        assertTrue(words.all { it.isNotBlank() })
    }

    @Test
    fun testExcludeAmbiguousCharacters() {
        val options = PasswordOptions(
            length = 50,
            uppercase = true,
            lowercase = true,
            digits = true,
            symbols = false,
            excludeAmbiguous = true,
        )
        val pwd = useCase.generatePassword(options)
        val ambiguous = setOf('0', 'O', 'l', '1', 'I')
        assertFalse(pwd.any { it in ambiguous })
    }

    @Test
    fun testPasswordStrengthEstimation() {
        assertEquals(PasswordStrength.VERY_WEAK, useCase.estimateStrength("123"))
        assertEquals(PasswordStrength.VERY_WEAK, useCase.estimateStrength("password"))
        assertTrue(useCase.estimateStrength("K7#mP9\$xL2!qR5@w") in listOf(PasswordStrength.STRONG, PasswordStrength.VERY_STRONG))
    }
}
