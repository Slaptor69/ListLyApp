package ru.misterpotz.demo

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
import ru.misterpotz.demo.features.catalog.CatalogScreen
import ru.misterpotz.demo.features.news.NewsScreen
import ru.misterpotz.demo.features.readlist.ReadlistScreen
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.rememberStandardDecorators

@Composable
fun MainScreenEntry() {
    MainBottomNavScreen()
}


enum class BottomBarDestination(@DrawableRes val icon: Int, val title: String,) {
    Catalog(R.drawable.ic_home, "Каталог"),
    Readlist(R.drawable.ic_readlist, "Readlist"),
    News(R.drawable.ic_news, "Новости")
}

@Serializable
data object CatalogNavKey : NavKey

@Serializable
data object ReadlistNavKey : NavKey

@Serializable
data object NewsNavKey : NavKey


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
fun rememberNewsTab(): BottomTab<NewsNavKey> {
    return rememberBottomTab(NewsNavKey, entryProvider {
        entry<NewsNavKey> {
            NewsScreen()
        }
    })
}

@Composable
fun MainBottomNavScreen() {
    var currentDestination by remember { mutableStateOf(BottomBarDestination.Catalog) }
    val bottomTabs = mapOf(
        BottomBarDestination.Catalog to rememberCatalogTab(),
        BottomBarDestination.Readlist to rememberReadlistTab(),
        BottomBarDestination.News to rememberNewsTab()
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

@Preview(showBackground = true)
@Composable
private fun Preview() {
    DemoTheme {
        MainBottomNavScreen()
    }
}