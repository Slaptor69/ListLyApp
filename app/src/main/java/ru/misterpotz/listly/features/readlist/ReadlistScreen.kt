package ru.misterpotz.listly.features.readlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.models.ReadlistFolders
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

    val typeFilters = buildList {
        add(ReadlistFilter.All)
        addAll(MediaType.values().map { ReadlistFilter.ByType(it) })
    }
    val folders = remember(state.readlistFolders, items) {
        buildReadlistFolders(state.readlistFolders + items.orEmpty().mapNotNull { it.readlistFolder })
    }
    val folderFilters = remember(folders) {
        listOf(ReadlistFolderFilter.All) + folders.map { ReadlistFolderFilter.ByFolder(it) }
    }
    var activeTypeFilter by remember { mutableStateOf<ReadlistFilter>(ReadlistFilter.All) }
    var activeFolderFilter by remember { mutableStateOf<ReadlistFolderFilter>(ReadlistFolderFilter.All) }
    val visibleItems = items.orEmpty().filter { item ->
        activeTypeFilter.matches(item) && activeFolderFilter.matches(item)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReadlistFilterButton(
                modifier = Modifier.weight(1f),
                title = "Тип: ${activeTypeFilter.title}",
                options = typeFilters,
                selectedOption = activeTypeFilter,
                optionTitle = { it.title },
                onOptionSelected = { activeTypeFilter = it }
            )
            ReadlistFolderFilterButton(
                modifier = Modifier.weight(1f),
                title = "Папка: ${activeFolderFilter.title}",
                options = folderFilters,
                selectedOption = activeFolderFilter,
                onOptionSelected = { activeFolderFilter = it },
                onFolderCreated = { folder ->
                    onEvent(ReadlistEvent.Ui.CreateFolder(folder))
                    activeFolderFilter = ReadlistFolderFilter.ByFolder(folder)
                }
            )
        }

        if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Readlist is empty")
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

/** Кнопка фильтра со всплывающим списком вариантов. */
@Composable
private fun <T> ReadlistFilterButton(
    modifier: Modifier = Modifier,
    title: String,
    options: List<T>,
    selectedOption: T,
    optionTitle: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 160.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionTitle(option)) },
                    trailingIcon = {
                        if (option == selectedOption) {
                            Text("✓")
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Кнопка фильтра папок с возможностью создать новую папку. */
@Composable
private fun ReadlistFolderFilterButton(
    modifier: Modifier = Modifier,
    title: String,
    options: List<ReadlistFolderFilter>,
    selectedOption: ReadlistFolderFilter,
    onOptionSelected: (ReadlistFolderFilter) -> Unit,
    onFolderCreated: (ReadlistFolder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var createDialogVisible by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    trailingIcon = {
                        if (option == selectedOption) {
                            Text("✓")
                        }
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = "+ Создать новую папку",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {
                    expanded = false
                    newFolderName = ""
                    createDialogVisible = true
                }
            )
        }
    }

    if (createDialogVisible) {
        AlertDialog(
            onDismissRequest = { createDialogVisible = false },
            title = { Text("Введите имя папки") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val folderName = newFolderName.trim()
                        if (folderName.isNotEmpty()) {
                            onFolderCreated(ReadlistFolder(folderName))
                            createDialogVisible = false
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { createDialogVisible = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun buildReadlistFolders(extraFolders: List<ReadlistFolder>): List<ReadlistFolder> {
    return (ReadlistFolders.Default + extraFolders)
        .filter { it.title.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
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

/**
 * Локальный фильтр папок readlist.
 *
 * Суммируется с фильтром типа медиаконтента в ReadlistScreenContent.
 */
private sealed interface ReadlistFolderFilter {
    val title: String

    /** Показывает элементы из всех папок. */
    data object All : ReadlistFolderFilter {
        override val title = "Все"
    }

    /** Оставляет только элементы из выбранной пользовательской папки. */
    data class ByFolder(val folder: ReadlistFolder) : ReadlistFolderFilter {
        override val title: String = folder.title
    }

    /** Проверяет, должен ли конкретный элемент пройти фильтр папки. */
    fun matches(item: MediaItem): Boolean {
        return when (this) {
            All -> true
            is ByFolder -> item.readlistFolder == folder
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
