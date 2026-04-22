package ru.misterpotz.listly.features.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputPreparationTest {

    @Test
    fun `prepareAuthInput trims login and password`() {
        val result = prepareAuthInput("  user  ", "  pass  ")

        assertTrue(result.isSuccess)
        assertEquals(
            AuthInput("user", "pass"),
            result.getOrNull()
        )
    }

    @Test
    fun `prepareAuthInput returns error when login is blank`() {
        val result = prepareAuthInput("   ", "pass")

        assertTrue(result.isFailure)
        assertEquals("Введите имя и пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `prepareAuthInput returns error when password is blank`() {
        val result = prepareAuthInput("user", "   ")

        assertTrue(result.isFailure)
        assertEquals("Введите имя и пароль", result.exceptionOrNull()?.message)
    }
}
