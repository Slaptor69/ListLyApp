package ru.misterpotz.listly.features.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputPreparationTest {

    @Test
    fun `prepareAuthInput trims login and password`() {
        val result = prepareAuthInput("  user  ", "  password  ")

        assertTrue(result.isSuccess)
        assertEquals(
            AuthInput("user", "password"),
            result.getOrNull()
        )
    }

    @Test
    fun `prepareAuthInput returns error when login is blank`() {
        val result = prepareAuthInput("   ", "password")

        assertTrue(result.isFailure)
        assertEquals("Введите имя и пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `prepareAuthInput returns error when password is blank`() {
        val result = prepareAuthInput("user", "   ")

        assertTrue(result.isFailure)
        assertEquals("Введите имя и пароль", result.exceptionOrNull()?.message)
    }

    @Test
    fun `prepareAuthInput validates backend login length`() {
        val result = prepareAuthInput("ab", "password")

        assertTrue(result.isFailure)
        assertEquals("Имя должно быть от 3 до 20 символов", result.exceptionOrNull()?.message)
    }

    @Test
    fun `prepareAuthInput validates backend password length`() {
        val result = prepareAuthInput("user", "short")

        assertTrue(result.isFailure)
        assertEquals("Пароль должен быть от 6 до 25 символов", result.exceptionOrNull()?.message)
    }
}
