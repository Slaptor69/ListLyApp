package ru.misterpotz.listly.features.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsStatePolicyTest {

    @Test
    fun `theme selection updates current theme and closes dialog`() {
        val state = SettingsState(
            currentThemeMode = ThemeMode.LIGHT,
            isThemeDialogVisible = true
        )

        val result = state.afterThemeSelected(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, result.currentThemeMode)
        assertFalse(result.isThemeDialogVisible)
    }

    @Test
    fun `logout clears authorized account state and closes confirmation dialog`() {
        val state = SettingsState(
            isAuthorized = true,
            login = "user",
            isLogoutDialogVisible = true
        )

        val result = state.afterLogout()

        assertFalse(result.isAuthorized)
        assertNull(result.login)
        assertFalse(result.isLogoutDialogVisible)
    }
}
