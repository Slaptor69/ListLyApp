package ru.misterpotz.listly.features.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log
import ru.misterpotz.listly.utils.toLoadable

/**
 * Экран каталога.
 *
 * В общей структуре это "UI-слой" над CatalogStore.
 * Сам экран не хранит бизнес-логику: он читает State и отправляет Event.
 */
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
                    // Навигацию делаем через Effect, а не через State,
                    // потому что это одноразовое действие.
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

/**
 * Преобразует lifecycle экрана в события Store.
 *
 * Это удобный слой адаптации между Android lifecycle и ELM Event.
 */
@Composable
private fun ObserveLifecycleEvents(
    onEvent: (CatalogEvent) -> Unit
) {
    ObserveLifecycleEvents(
        onResume = { onEvent(CatalogEvent.Ui.OnResume) },
        onPause = {}
    )
}

/**
 * Отрисовка контента каталога.
 *
 * Здесь хорошо видно роль State в ELM:
 * экран полностью зависит от уже подготовленного состояния и не знает, как оно было получено.
 */
@Composable
fun CatalogScreenContent(state: CatalogState, onEvent: (CatalogEvent) -> Unit) {
    val items = state.items.requireContent()
    val readlistFolders = remember(state.readlistFolders, items) {
        buildReadlistFolders(state.readlistFolders + items.mapNotNull { it.readlistFolder })
    }
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
                    }
                    if (state.itemToLoading.contains(item.id)) {
                        // Локальный индикатор показывает асинхронную операцию только для одной карточки.
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp)
                                .align(Alignment.CenterVertically),
                        )
                    } else {
                        CatalogReadlistFolderButton(item, readlistFolders, onEvent)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Кнопка добавления в readlist с выбором папки. */
@Composable
private fun CatalogReadlistFolderButton(
    item: MediaItemUi,
    folders: List<ReadlistFolder>,
    onEvent: (CatalogEvent) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var createDialogVisible by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val selectedFolder = item.readlistFolder
    val buttonTitle = selectedFolder?.title ?: "Добавить"
    val buttonColors = if (selectedFolder == null) {
        ButtonDefaults.filledTonalButtonColors()
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    }

    Box {
        FilledTonalButton(
            colors = buttonColors,
            onClick = { expanded = true }
        ) {
            Text(
                text = buttonTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text(folder.title) },
                    trailingIcon = {
                        if (folder == selectedFolder) {
                            Text("✓")
                        }
                    },
                    onClick = {
                        onEvent(CatalogEvent.Ui.AddToReadingList(item, folder))
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
                            onEvent(
                                CatalogEvent.Ui.AddToReadingList(
                                    item,
                                    ReadlistFolder(folderName)
                                )
                            )
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
    return extraFolders
        .filter { it.title.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
}

/** Preview нужен для локального просмотра UI без запуска всей навигации и Store. */
@Preview(showBackground = true)
@Composable
private fun Preview() {
    ListlyTheme {
        CatalogScreenContent(
            CatalogState(
                items = PreviewMediaItems.toLoadable()
            )
        ) { }
    }
}

/** Небольшой набор данных для превью каталога. */
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
