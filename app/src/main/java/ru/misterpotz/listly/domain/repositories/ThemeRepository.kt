package ru.misterpotz.listly.domain.repositories

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.misterpotz.listly.features.settings.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
//репо для тем приложения
@Singleton
class ThemeRepository @Inject constructor(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    //наружу ток для чтения
    val themeMode = _themeMode.asStateFlow()

    fun getThemeMode(): ThemeMode = _themeMode.value

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(THEME_KEY, mode.name).apply()
        _themeMode.value = mode
    }

    private fun readThemeMode(): ThemeMode {
        val raw = preferences.getString(THEME_KEY, ThemeMode.LIGHT.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.LIGHT
    }

    private companion object {
        private const val PREFERENCES_NAME = "theme_preferences"
        private const val THEME_KEY = "theme_mode"
    }
}
