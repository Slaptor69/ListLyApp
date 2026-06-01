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
import ru.misterpotz.listly.features.settings.FolderManagementScreen
import ru.misterpotz.listly.features.settings.SettingsScreen

@Composable
fun MainScreenEntry(
    initialDestination: BottomBarDestination = BottomBarDestination.Catalog
) {
    MainBottomNavScreen(initialDestination)
}


@Serializable
enum class BottomBarDestination(@DrawableRes val icon: Int, val title: String,) {
    Catalog(R.drawable.ic_home, "Каталог"),
    Readlist(R.drawable.ic_readlist, "Мои списки"),
    Settings(R.drawable.ic_settings, "Настройки")
}

@Serializable
data object CatalogNavKey : NavKey

@Serializable
data object ReadlistNavKey : NavKey

@Serializable
sealed interface SettingsTabNavKey : NavKey

@Serializable
data object SettingsNavKey : SettingsTabNavKey

@Serializable
data object FolderManagementNavKey : SettingsTabNavKey


data class BottomTab<T : NavKey>(
    val backStack: NavBackStack<T>,
    val decoratedNavEntries: List<NavEntry<T>>
)

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

@Composable
fun rememberCatalogTab(): BottomTab<CatalogNavKey> {
    return rememberBottomTab(CatalogNavKey, entryProvider {
        entry<CatalogNavKey> {
            CatalogScreen()
        }
    })
}

@Composable
fun rememberReadlistTab(): BottomTab<ReadlistNavKey> {
    return rememberBottomTab(ReadlistNavKey, entryProvider {
        entry<ReadlistNavKey> {
            ReadlistScreen()
        }
    })
}
@Composable
fun rememberSettingsTab(): BottomTab<NavKey> {
    val settingsBackstack = rememberNavBackStack(SettingsNavKey)
    val settingsNavEntries = rememberDecoratedNavEntries(
        settingsBackstack,
        rememberStandardDecorators(),
        entryProvider = entryProvider {
        entry<SettingsNavKey> {
            SettingsScreen(
                onOpenFolders = {
                    settingsBackstack.add(FolderManagementNavKey)
                }
            )
        }
        entry<FolderManagementNavKey> {
            FolderManagementScreen(
                onBack = { settingsBackstack.removeLastOrNull() }
            )
        }
        } as (NavKey) -> NavEntry<NavKey>,
    )
    return remember(settingsBackstack, settingsNavEntries) {
        BottomTab(settingsBackstack, settingsNavEntries)
    }
}

@Composable
fun MainBottomNavScreen(
    initialDestination: BottomBarDestination = BottomBarDestination.Catalog
) {
    var currentDestination by remember(initialDestination) { mutableStateOf(initialDestination) }
    val settingsTab = rememberSettingsTab()
    val bottomTabs = mapOf(
        BottomBarDestination.Catalog to rememberCatalogTab(),
        BottomBarDestination.Readlist to rememberReadlistTab(),
        BottomBarDestination.Settings to settingsTab
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                BottomBarDestination.entries.forEach {
                    NavigationBarItem(
                        selected = it == currentDestination,
                        onClick = {
                            if (it == BottomBarDestination.Settings) {
                                settingsTab.backStack.popToRoot()
                            }
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
            // Неактивные вкладки остаются в памяти, чтобы не сбрасывать их состояние.
            NavDisplay(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                entries = bottomTabs[currentDestination]!!.decoratedNavEntries,
                onBack = {
                    // Back внутри таба пока обрабатывается выше.
                }
            )
        }
    }
}

private fun NavBackStack<NavKey>.popToRoot() {
    while (size > 1) {
        removeLastOrNull()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    ListlyTheme {
        MainBottomNavScreen()
    }
}
