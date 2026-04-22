package ru.misterpotz.listly.features.readlist

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
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log


/**
 * Экран списка чтения.
 *
 * Роль в проекте: показывает второй пример подписки на поток данных через ELM.
 */
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

/**
 * Отрисовка readlist-экрана.
 *
 * Фильтрация по типу сейчас локальная и UI-специфичная, поэтому она живёт прямо в composable,
 * а не внутри Store.
 */
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

/**
 * Локальные фильтры только для отображения readlist.
 *
 * Они не вынесены в Store, потому что пока не влияют на доменные данные
 * и нужны лишь для текущей отрисовки.
 */
private sealed interface ReadlistFilter {
    val title: String

    /** Показывает все элементы без ограничений. */
    data object All : ReadlistFilter {
        override val title = "Все"
    }

    /** Оставляет только элементы выбранного типа. */
    data class ByType(private val type: MediaType) : ReadlistFilter {
        override val title: String = type.title

        /** Возвращает тип, к которому привязан фильтр. */
        fun mediaType() = type
    }

    /** Проверяет, должен ли конкретный элемент пройти фильтр. */
    fun matches(item: MediaItem): Boolean {
        return when (this) {
            All -> true
            is ByType -> item.type == mediaType()
        }
    }
}

/** Preview нужен для локального просмотра разметки экрана. */
@Preview(showBackground = true)
@Composable
private fun Preview() {
    ListlyTheme {
        ReadlistScreen()
    }
}
