package ru.misterpotz.listly

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.misterpotz.listly.features.catalog.CatalogScreen
import ru.misterpotz.listly.features.readlist.ReadlistScreen
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.rememberStandardDecorators
import ru.misterpotz.listly.features.settings.SettingsScreen
/**
 * Точка входа в главный раздел приложения.
 *
 * В общей структуре этот composable отделяет верхнеуровневую навигацию приложения
 * от внутренней навигации между табами.
 */
@Composable
fun MainScreenEntry(
    initialDestination: BottomBarDestination = BottomBarDestination.Catalog
) {
    MainBottomNavScreen(initialDestination)
}


/**
 * Описание вкладок нижней навигации.
 *
 * Это не ELM-сущность, а просто UI-модель для переключения между большими разделами.
 */
@Serializable
enum class BottomBarDestination(@DrawableRes val icon: Int, val title: String,) {
    Catalog(R.drawable.ic_home, "Каталог"),
    Readlist(R.drawable.ic_readlist, "Мои списки"),
    Settings(R.drawable.ic_settings, "Настройки")
}

/** Ключ внутреннего backstack для вкладки каталога. */
@Serializable
data object CatalogNavKey : NavKey

/** Ключ внутреннего backstack для вкладки readlist. */
@Serializable
data object ReadlistNavKey : NavKey

@Serializable
data object SettingsNavKey : NavKey


/**
 * Состояние одной вкладки нижней навигации.
 *
 * Мы храним отдельно backstack и уже подготовленные decorated entries,
 * чтобы при переключении вкладок не терялось локальное состояние каждого раздела.
 */
data class BottomTab<T : NavKey>(
    val backStack: NavBackStack<T>,
    val decoratedNavEntries: List<NavEntry<T>>
)

/**
 * Создаёт и запоминает внутренний backstack для одной вкладки.
 *
 * Роль в структуре: это навигационный helper, который делает табы независимыми друг от друга.
 */
@Composable
private fun <T : NavKey> rememberBottomTab(
    key: T,
    entryProvider: (T) -> NavEntry<T>
): BottomTab<T> {
    val catalogBackstack = rememberNavBackStack(key)
    val catalogNavEntries = rememberDecoratedNavEntries(
        catalogBackstack,
        rememberStandardDecorators(),
        entryProvider = entryProvider as (NavKey) -> NavEntry<NavKey>,
    )
    return remember(catalogBackstack, catalogNavEntries) {
        BottomTab(catalogBackstack, catalogNavEntries) as BottomTab<T>
    }
}

/** Поднимает экран каталога как содержимое первой вкладки. */
@Composable
fun rememberCatalogTab(): BottomTab<CatalogNavKey> {
    return rememberBottomTab(CatalogNavKey, entryProvider {
        entry<CatalogNavKey> {
            CatalogScreen()
        }
    })
}

/** Поднимает экран списков как содержимое второй вкладки. */
@Composable
fun rememberReadlistTab(): BottomTab<ReadlistNavKey> {
    return rememberBottomTab(ReadlistNavKey, entryProvider {
        entry<ReadlistNavKey> {
            ReadlistScreen()
        }
    })
}
@Composable
fun rememberSettingsTab(): BottomTab<SettingsNavKey> {
    return rememberBottomTab(SettingsNavKey, entryProvider {
        entry<SettingsNavKey> {
            SettingsScreen()
        }
    })
}

/**
 * Основной экран с нижней навигацией.
 *
 * В общей структуре это контейнер над основными фичами.
 * Сами фичи внутри уже построены по ELM, а этот composable только переключает разделы.
 */
@Composable
fun MainBottomNavScreen(
    initialDestination: BottomBarDestination = BottomBarDestination.Catalog
) {
    var currentDestination by remember(initialDestination) { mutableStateOf(initialDestination) }
    val bottomTabs = mapOf(
        BottomBarDestination.Catalog to rememberCatalogTab(),
        BottomBarDestination.Readlist to rememberReadlistTab(),
        BottomBarDestination.Settings to rememberSettingsTab()
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BottomBarDestination.entries.forEach {
                    NavigationBarItem(
                        selected = it == currentDestination,
                        onClick = {
                            currentDestination = it
                        },
                        icon = {
                            Icon(painter = painterResource(it.icon), null)
                        },
                        label = {
                            Text(it.title)
                        }
                    )
                }
            }

        }
    ) { paddingValues ->
        if (LocalInspectionMode.current) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Здесь отображаются табы")

            }
        } else {
            // Показываем только активную вкладку, но состояние остальных сохраняем в памяти.
            NavDisplay(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                entries = bottomTabs[currentDestination]!!.decoratedNavEntries,
                onBack = {
                    // todo delegate on back properly
                }
            )
        }
    }
}

/** Preview нужен только для быстрой проверки в Android Studio. */
@Preview(showBackground = true)
@Composable
private fun Preview() {
    ListlyTheme {
        MainBottomNavScreen()
    }
}
