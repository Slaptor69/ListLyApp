package ru.misterpotz.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.features.mediaitem.MediaItemScreenEntry
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.LocalGlobalBackstackProvider
import ru.misterpotz.demo.ui.utils.rememberStandardDecorators

@Serializable
sealed interface GlobalAppNavKey : NavKey {
    @Serializable
    data object Main : GlobalAppNavKey

    @Serializable
    data class MediaItemScreen(val id: Int): GlobalAppNavKey
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val globalBackstack = rememberNavBackStack(GlobalAppNavKey.Main)

            DemoTheme {
                CompositionLocalProvider(
                    LocalGlobalBackstackProvider provides (globalBackstack as NavBackStack<GlobalAppNavKey>)
                ) {
                    NavDisplay(
                        backStack = globalBackstack,
                        entryDecorators = rememberStandardDecorators(),
                        entryProvider = entryProvider {
                            entry<GlobalAppNavKey.Main> {
                                MainScreenEntry()
                            }
                            entry<GlobalAppNavKey.MediaItemScreen> {
                                MediaItemScreenEntry(it.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DemoTheme {
        Greeting("Android")
    }
}