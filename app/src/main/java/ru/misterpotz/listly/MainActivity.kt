package ru.misterpotz.listly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.misterpotz.listly.features.auth.AuthScreenEntry
import ru.misterpotz.listly.features.mediaitem.MediaItemScreenEntry
import ru.misterpotz.listly.features.settings.ThemeMode
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.LocalGlobalBackstackProvider
import ru.misterpotz.listly.ui.utils.rememberStandardDecorators

@Serializable
sealed interface GlobalAppNavKey : NavKey {
    @Serializable
    data object Auth : GlobalAppNavKey

    @Serializable
    data class Main(
        val initialDestination: BottomBarDestination = BottomBarDestination.Catalog
    ) : GlobalAppNavKey

    @Serializable
    data class MediaItemScreen(
        val id: String,
        val returnDestination: BottomBarDestination = BottomBarDestination.Catalog
    ) : GlobalAppNavKey
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeRepository = remember { appComponent.themeRepository }
            val authRepository = remember { appComponent.authRepository }
            val themeMode by themeRepository.themeMode.collectAsState()

            // Если токен уже сохранён, сразу открываем основной экран.
            val startDestination = startDestinationForAuth(authRepository.isAuthorized())

            val globalBackstack = rememberNavBackStack(startDestination)

            fun replaceGlobalBackstack(targetStack: List<GlobalAppNavKey>) {
                while (globalBackstack.isNotEmpty()) {
                    globalBackstack.removeLastOrNull()
                }
                targetStack.forEach { globalBackstack.add(it) }
            }

            fun closeAuth() {
                val targetStack = backstackForClosingAuth(globalBackstack.toGlobalAppNavKeys())
                replaceGlobalBackstack(targetStack)
            }

            fun closeMediaItem(mediaItemScreen: GlobalAppNavKey.MediaItemScreen) {
                val targetStack = backstackForClosingMediaItem(
                    currentStack = globalBackstack.toGlobalAppNavKeys(),
                    returnDestination = mediaItemScreen.returnDestination
                )
                replaceGlobalBackstack(targetStack)
            }

            ListlyTheme(
                darkTheme = themeMode == ThemeMode.DARK
            ) {
                // Глобальный backstack нужен для переходов из вложенных экранов.
                CompositionLocalProvider(
                    LocalGlobalBackstackProvider provides (globalBackstack as NavBackStack<GlobalAppNavKey>)
                ) {
                    NavDisplay(
                        backStack = globalBackstack,
                        entryDecorators = rememberStandardDecorators(),
                        onBack = {
                            when (val current = globalBackstack.lastOrNull()) {
                                is GlobalAppNavKey.MediaItemScreen -> closeMediaItem(current)
                                else -> globalBackstack.removeLastOrNull()
                            }
                        },
                        entryProvider = entryProvider {
                            entry<GlobalAppNavKey.Auth> {
                                AuthScreenEntry(
                                    onAuthorized = {
                                        closeAuth()
                                    },
                                    onBack = {
                                        closeAuth()
                                    }
                                )
                            }
                            entry<GlobalAppNavKey.Main> {
                                MainScreenEntry(it.initialDestination)
                            }
                            entry<GlobalAppNavKey.MediaItemScreen> {
                                MediaItemScreenEntry(
                                    mediaItem = it.id,
                                    returnDestination = it.returnDestination
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

internal fun startDestinationForAuth(isAuthorized: Boolean): GlobalAppNavKey {
    return if (isAuthorized) {
        GlobalAppNavKey.Main()
    } else {
        GlobalAppNavKey.Auth
    }
}
