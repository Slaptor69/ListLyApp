package ru.misterpotz.listly

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNavigationPolicyTest {

    @Test
    fun `opening auth from settings replaces main destination with settings and pushes auth`() {
        val result = backstackForOpeningAuthFromSettings(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Catalog))
        )

        assertEquals(
            listOf(
                GlobalAppNavKey.Main(BottomBarDestination.Settings),
                GlobalAppNavKey.Auth
            ),
            result
        )
    }

    @Test
    fun `closing auth returns to settings when auth is opened over settings`() {
        val result = backstackForClosingAuth(
            listOf(
                GlobalAppNavKey.Main(BottomBarDestination.Settings),
                GlobalAppNavKey.Auth
            )
        )

        assertEquals(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Settings)),
            result
        )
    }

    @Test
    fun `closing auth falls back to settings main when stack is unexpected`() {
        val result = backstackForClosingAuth(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Catalog))
        )

        assertEquals(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Settings)),
            result
        )
    }
}
