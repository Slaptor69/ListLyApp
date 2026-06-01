package ru.misterpotz.listly

import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {

    @Test
    fun `app starts with auth screen when token is missing`() {
        assertEquals(
            GlobalAppNavKey.Auth,
            startDestinationForAuth(isAuthorized = false)
        )
    }

    @Test
    fun `app starts with main screen when token exists`() {
        assertEquals(
            GlobalAppNavKey.Main(),
            startDestinationForAuth(isAuthorized = true)
        )
    }
}
