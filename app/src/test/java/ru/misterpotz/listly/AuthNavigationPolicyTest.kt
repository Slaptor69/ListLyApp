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

    @Test
    fun `closing media item opened from readlist returns to readlist`() {
        val result = backstackForClosingMediaItem(
            currentStack = listOf(
                GlobalAppNavKey.Main(BottomBarDestination.Catalog),
                GlobalAppNavKey.MediaItemScreen(
                    id = "tmdb-25898",
                    returnDestination = BottomBarDestination.Readlist
                )
            ),
            returnDestination = BottomBarDestination.Readlist
        )

        assertEquals(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Readlist)),
            result
        )
    }

    @Test
    fun `closing media item opened from catalog returns to catalog`() {
        val result = backstackForClosingMediaItem(
            currentStack = listOf(
                GlobalAppNavKey.Main(BottomBarDestination.Readlist),
                GlobalAppNavKey.MediaItemScreen(
                    id = "tmdb-25898",
                    returnDestination = BottomBarDestination.Catalog
                )
            ),
            returnDestination = BottomBarDestination.Catalog
        )

        assertEquals(
            listOf(GlobalAppNavKey.Main(BottomBarDestination.Catalog)),
            result
        )
    }
}
