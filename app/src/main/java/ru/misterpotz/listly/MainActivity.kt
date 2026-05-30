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

/**
 * Глобальные точки навигации приложения.
 *
 * В общей структуре проекта это верхний уровень роутинга: отсюда мы решаем,
 * какой большой раздел показать пользователю.
 * Это не часть ELM напрямую, но именно сюда приходят эффекты экранов,
 * которые хотят инициировать переход на другой экран.
 */
@Serializable
sealed interface GlobalAppNavKey : NavKey {
    /** Экран авторизации показывается, пока у пользователя нет токена. */
    @Serializable
    data object Auth : GlobalAppNavKey

    /** Корневой экран приложения с нижней навигацией. */
    @Serializable
    data class Main(
        val initialDestination: BottomBarDestination = BottomBarDestination.Catalog
    ) : GlobalAppNavKey

    /** Детальный экран конкретной медиапозиции. */
    @Serializable
    data class MediaItemScreen(
        val id: String,
        val returnDestination: BottomBarDestination = BottomBarDestination.Catalog
    ) : GlobalAppNavKey
}

/**
 * Главная Activity всего приложения.
 *
 * В общей структуре это внешний контейнер Compose/UI.
 * ELM-логика живёт ниже, внутри конкретных экранов и их Store,
 * а Activity только поднимает тему, навигацию и общие CompositionLocal.
 */
class MainActivity : ComponentActivity() {

    /**
     * Создаёт корневую Compose-иерархию.
     *
     * Здесь важно заметить разделение ответственности:
     * 1. Activity решает стартовый экран.
     * 2. Экран сам поднимает свой Store.
     * 3. Store уже крутит ELM-цикл: Event -> Reducer -> Command -> Actor -> Event.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeRepository = remember { appComponent.themeRepository }
            val authRepository = remember { appComponent.authRepository }
            val themeMode by themeRepository.themeMode.collectAsState()

            // Решаем, начинать ли приложение с авторизации или сразу с основного раздела.
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
                // Даём всем экранам доступ к одному глобальному backstack.
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
