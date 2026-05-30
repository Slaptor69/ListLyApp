package ru.misterpotz.listly.features.settings

//параметр для состояния
enum class ThemeMode {
    LIGHT, DARK
}
//состояние, т е то что экран должен показывать прям щас в любой момент времени
data class SettingsState(
    val currentThemeMode: ThemeMode = ThemeMode.LIGHT,
    val isThemeDialogVisible: Boolean = false,
    val isAuthorized: Boolean = false,
    val login: String? = null,
    val isLogoutDialogVisible: Boolean = false
)
//события т е что произошло
sealed interface SettingsEvent {
    data object Init : SettingsEvent
    //события от Ui
    sealed interface Ui : SettingsEvent {
        data object OnResume : Ui
        data object ThemeClicked : Ui
        data class ThemeSelected(val mode: ThemeMode) : Ui
        data object ThemeDialogDismissed : Ui
        data object AuthClicked : Ui
        data object FoldersClicked : Ui
        data object LogoutClicked : Ui
        data object LogoutDismissed : Ui
        data object LogoutConfirmed : Ui
    }
    //события из логики
    sealed interface Internal : SettingsEvent {
        data class ThemeLoaded(val mode: ThemeMode) : Internal
        data class AccountLoaded(val isAuthorized: Boolean, val login: String?) : Internal
        data class ThemeSaved(val mode: ThemeMode) : Internal
        data object LoggedOut : Internal
    }
}
//эффекты т е одноразовые действия UI, т е то что UI не рbсует на постоянке
sealed interface SettingsEffect {
    data object OpenAuth : SettingsEffect
    data object OpenFolders : SettingsEffect
}
//команды т е указания для actor, тип что ему нужно сделать
sealed interface SettingsCommand {
    data object LoadTheme : SettingsCommand
    data object LoadAccount : SettingsCommand
    data class SaveTheme(val mode: ThemeMode) : SettingsCommand
    data object Logout : SettingsCommand
}
