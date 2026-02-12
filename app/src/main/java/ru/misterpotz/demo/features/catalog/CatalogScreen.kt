package ru.misterpotz.demo.features.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.demo.GlobalAppNavKey
import ru.misterpotz.demo.appComponent
import ru.misterpotz.demo.domain.models.MediaType
import ru.misterpotz.demo.ui.theme.DemoTheme
import ru.misterpotz.demo.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.demo.ui.utils.StandardElmScreen
import ru.misterpotz.demo.utils.log
import ru.misterpotz.demo.utils.toLoadable

@Composable
fun CatalogScreen() {
    StandardElmScreen(
        storeFactory = {
            "creating new catalog store".log()
            appComponent.catalogStoreFactory.create()
        },
        onEffect = { effect ->
            when (effect) {
                is CatalogEffect.NavigateToItem -> {
                    backstack.add(GlobalAppNavKey.MediaItemScreen(effect.mediaItemId))
                }
            }
        },
        body = { state, onEvent ->
            ObserveLifecycleEvents(onEvent)
            when {
                state.items.isLoading -> Text(
                    "Загрузка...",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    textAlign = TextAlign.Center,
                )

                state.items.isError -> Text(
                    "Ошибка: ${state.items.error}",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    textAlign = TextAlign.Center,
                )

                state.items.isContent -> {
                    CatalogScreenContent(state, onEvent)
                }

                else -> Unit
            }
        },
    )
}

@Composable
private fun ObserveLifecycleEvents(
    onEvent: (CatalogEvent) -> Unit
) {
    ObserveLifecycleEvents(
        onResume = { onEvent(CatalogEvent.Ui.OnResume) },
        onPause = {}
    )
}

@Composable
fun CatalogScreenContent(state: CatalogState, onEvent: (CatalogEvent) -> Unit) {
    val items = state.items.requireContent()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ElevatedCard(onClick = { onEvent(CatalogEvent.Ui.ClickItem(item)) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.type.title,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (item.tracked) "Tracked" else "Not tracked",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (state.itemToLoading.contains(item.id)) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .align(Alignment.CenterVertically),
                        )
                    } else {
                        TextButton(onClick = { onEvent(CatalogEvent.Ui.TrackItem(item)) }) {
                            Text(if (item.tracked) "Untrack" else "Track")
                        }
                        val onClick = { onEvent(CatalogEvent.Ui.AddToReadingList(item)) }
                        if (item.inReadlist) {
                            OutlinedButton(onClick) {
                                Text("Remove")
                            }
                        } else {
                            FilledTonalButton(onClick) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    DemoTheme {
        CatalogScreenContent(
            CatalogState(
                items = PreviewMediaItems.toLoadable()
            )
        ) { }
    }
}

private val PreviewMediaItems = listOf(
    MediaItemUi(
        0,
        "Клинок, рассекающий демонов",
        type = MediaType.Anime,
        tracked = false
    ),
    MediaItemUi(
        1,
        "Подземелье вкусностей",
        type = MediaType.Anime,
        tracked = false,
    ),
)