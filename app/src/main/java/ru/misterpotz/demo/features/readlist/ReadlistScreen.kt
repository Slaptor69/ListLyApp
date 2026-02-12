package ru.misterpotz.demo.features.readlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.demo.appComponent
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.domain.models.MediaType
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.demo.ui.utils.StandardElmScreen
import ru.misterpotz.demo.utils.log


@Composable
fun ReadlistScreen() {
    StandardElmScreen(
        storeFactory = {
            "creating new readlist store".log()
            appComponent.readlistStoreFactory.create()
        },
        onEffect = {},
        body = { state, onEvent ->
            ReadlistScreenContent(state, onEvent)
        },
    )
}

@Composable
fun ReadlistScreenContent(state: ReadlistState, onEvent: (ReadlistEvent) -> Unit) {
    ObserveLifecycleEvents(
        onResume = { onEvent(ReadlistEvent.Ui.OnResume) },
        onPause = { onEvent(ReadlistEvent.Ui.OnPause) }
    )
    val items = state.mediaItems.content

    if (items.isNullOrEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Readlist is empty")
        }
        return
    }

    val filters = buildList {
        add(ReadlistFilter.All)
        addAll(MediaType.values().map { ReadlistFilter.ByType(it) })
    }
    var activeFilter by remember { mutableStateOf<ReadlistFilter>(ReadlistFilter.All) }
    val visibleItems = items.filter { activeFilter.matches(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                TextButton(onClick = { activeFilter = filter }) {
                    Text(filter.title)
                }
            }
        }

        if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("В этом разделе пока пусто")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleItems, key = { it.id }) { item ->
                ElevatedCard {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                item.type.title,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = { }) { Text("Up") }
                        OutlinedButton(onClick = { }) { Text("Down") }
                    }
                }
            }
        }
    }
}
private sealed interface ReadlistFilter {
    val title: String

    data object All : ReadlistFilter {
        override val title = "Все"
    }

    data class ByType(private val type: MediaType) : ReadlistFilter {
        override val title: String = type.title

        fun mediaType() = type
    }

    fun matches(item: MediaItem): Boolean {
        return when (this) {
            All -> true
            is ByType -> item.type == mediaType()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    DemoTheme {
        ReadlistScreen()
    }
}