package ru.misterpotz.listly.features.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.repositories.AuthRepository
import ru.misterpotz.listly.domain.repositories.ThemeRepository
import javax.inject.Inject

class SettingsActor @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
) : Actor<SettingsCommand, SettingsEvent>() {

    override fun execute(command: SettingsCommand): Flow<SettingsEvent> {
        return when (command) {
            SettingsCommand.LoadTheme -> flow {
                emit(SettingsEvent.Internal.ThemeLoaded(themeRepository.getThemeMode()))
            }

            SettingsCommand.LoadAccount -> flow {
                emit(
                    SettingsEvent.Internal.AccountLoaded(
                        isAuthorized = authRepository.isAuthorized(),
                        login = authRepository.getLogin()
                    )
                )
            }

            is SettingsCommand.SaveTheme -> flow {
                themeRepository.setThemeMode(command.mode)
                emit(SettingsEvent.Internal.ThemeSaved(command.mode))
            }

            SettingsCommand.Logout -> flow {
                authRepository.clearToken()
                emit(SettingsEvent.Internal.LoggedOut)
            }
        }
    }
}

class SettingsStoreFactory @Inject constructor(
    private val settingsActor: SettingsActor
) {
    fun create(): ElmStore<SettingsEvent, SettingsState, SettingsEffect, SettingsCommand> {
        return ElmStore(
            initialState = SettingsState(),
            reducer = SettingsReducer,
            actor = settingsActor,
            startEvent = SettingsEvent.Init,
        )
    }
}

object SettingsReducer :
    StateReducer<SettingsEvent, SettingsState, SettingsEffect, SettingsCommand>() {

    override fun Result.reduce(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Init -> commands {
                +SettingsCommand.LoadTheme
                +SettingsCommand.LoadAccount
            }

            SettingsEvent.Ui.OnResume -> commands {
                +SettingsCommand.LoadAccount
            }

            SettingsEvent.Ui.ThemeClicked -> state {
                copy(isThemeDialogVisible = true)
            }

            SettingsEvent.Ui.ThemeDialogDismissed -> state {
                copy(isThemeDialogVisible = false)
            }

            is SettingsEvent.Ui.ThemeSelected -> commands {
                +SettingsCommand.SaveTheme(event.mode)
                state {
                    afterThemeSelected(event.mode)
                }
            }

            SettingsEvent.Ui.AuthClicked -> effects {
                +SettingsEffect.OpenAuth
            }

            SettingsEvent.Ui.LogoutClicked -> state {
                copy(isLogoutDialogVisible = true)
            }

            SettingsEvent.Ui.LogoutDismissed -> state {
                copy(isLogoutDialogVisible = false)
            }

            SettingsEvent.Ui.LogoutConfirmed -> commands {
                +SettingsCommand.Logout
                state {
                    copy(isLogoutDialogVisible = false)
                }
            }

            SettingsEvent.Ui.FoldersClicked -> effects {
                +SettingsEffect.OpenFolders
            }

            is SettingsEvent.Internal.ThemeLoaded -> state {
                copy(currentThemeMode = event.mode)
            }

            is SettingsEvent.Internal.AccountLoaded -> state {
                copy(
                    isAuthorized = event.isAuthorized,
                    login = event.login
                )
            }

            is SettingsEvent.Internal.ThemeSaved -> state {
                copy(currentThemeMode = event.mode)
            }

            SettingsEvent.Internal.LoggedOut -> state {
                afterLogout()
            }
        }
    }
}

internal fun SettingsState.afterThemeSelected(mode: ThemeMode): SettingsState {
    return copy(
        currentThemeMode = mode,
        isThemeDialogVisible = false
    )
}

internal fun SettingsState.afterLogout(): SettingsState {
    return copy(
        isAuthorized = false,
        login = null,
        isLogoutDialogVisible = false
    )
}
