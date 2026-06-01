package ru.misterpotz.listly.features.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ru.misterpotz.listly.BottomBarDestination
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.features.liststate.UserMediaListManagementDialog
import ru.misterpotz.listly.features.liststate.userMediaListActionTitle
import ru.misterpotz.listly.features.media.MediaListItemCard
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log
import ru.misterpotz.listly.utils.toUserFriendlyMessage
import ru.misterpotz.listly.utils.toLoadable
import androidx.compose.ui.unit.dp
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
                    // Навигация идёт через Effect, чтобы не повторяться при перерисовке.
                    backstack.add(
                        GlobalAppNavKey.MediaItemScreen(
                            id = effect.mediaItemId,
                            returnDestination = BottomBarDestination.Catalog
                        )
                    )
                }
            }
        },
        body = { state, onEvent ->
            ObserveLifecycleEvents(onEvent)
            when {
                state.items.isLoading && state.items.content == null -> Text(
                    "Загрузка...",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    textAlign = TextAlign.Center,
                )

                state.items.isError -> Text(
                    state.items.error.toUserFriendlyMessage(),
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    textAlign = TextAlign.Center,
                )

                state.items.isContent || state.items.content != null -> {
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
    val items = state.items.content.orEmpty()
    val readlistFolders = remember(state.readlistFolders, items) {
        buildReadlistFolders(state.readlistFolders + items.flatMap { it.readlistFolders })
    }
    val typeFilters = remember { buildCatalogTypeFilters() }
    var activeTypeFilter by remember { mutableStateOf<CatalogTypeFilter>(CatalogTypeFilter.All) }
    val visibleItems = remember(items, activeTypeFilter) {
        filterCatalogItems(items, activeTypeFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(CatalogEvent.Ui.SearchChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true,
            label = { Text("Поиск") }
        )

        CatalogFilterButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            title = "Тип: ${activeTypeFilter.title}",
            options = typeFilters,
            selectedOption = activeTypeFilter,
            optionTitle = { it.title },
            onOptionSelected = { activeTypeFilter = it }
        )

        state.actionError?.let { error ->
            Text(
                text = error.toUserFriendlyMessage(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (visibleItems.isEmpty()) {
                item {
                    Text(
                        text = catalogEmptyMessage(state.searchQuery),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(visibleItems, key = { it.id }) { item ->
                CatalogMediaCard(
                    item = item,
                    folders = readlistFolders,
                    isLoading = state.itemToLoading.contains(item.id),
                    onClick = { onEvent(CatalogEvent.Ui.ClickItem(item)) },
                    onAddToReadList = { status, folders ->
                        onEvent(CatalogEvent.Ui.AddToReadList(item, status, folders))
                    },
                    onCreateFolder = { folder ->
                        onEvent(CatalogEvent.Ui.CreateFolder(folder))
                    },
                    onRemoveFromReadlist = {
                        onEvent(CatalogEvent.Ui.RemoveFromReadingList(item))
                    },
                    onToggleFavourite = {
                        onEvent(CatalogEvent.Ui.ToggleFavourite(item))
                    }
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CatalogMediaCard(
    item: MediaItemUi,
    folders: List<ReadlistFolder>,
    isLoading: Boolean,
    onClick: () -> Unit,
    onAddToReadList: (CollectionStatus, List<ReadlistFolder>) -> Unit,
    onCreateFolder: (ReadlistFolder) -> Unit,
    onRemoveFromReadlist: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    var managementDialogVisible by remember { mutableStateOf(false) }
    val inUserList = item.isInUserList()

    MediaListItemCard(
        title = item.title,
        typeTitle = item.type.title,
        imageUrl = item.imageUrl,
        inUserList = inUserList,
        status = item.collectionStatus,
        folderNames = item.readlistFolders.map { it.title },
        isFavourite = item.isFavourite,
        userRating = item.userRating,
        userNote = item.userNote,
        isLoading = isLoading,
        actionTitle = userMediaListActionTitle(inUserList),
        onClick = onClick,
        onToggleFavourite = onToggleFavourite,
        onActionClick = { managementDialogVisible = true }
    )

    if (managementDialogVisible) {
        UserMediaListManagementDialog(
            currentStatus = item.collectionStatus,
            selectedFolders = item.readlistFolders,
            availableFolders = folders,
            inUserList = item.isInUserList(),
            onDismiss = { managementDialogVisible = false },
            onCreateFolder = onCreateFolder,
            onSave = { status, selectedFolders ->
                managementDialogVisible = false
                onAddToReadList(status, selectedFolders)
            },
            onRemoveFromList = {
                managementDialogVisible = false
                onRemoveFromReadlist()
            }
        )
    }
}

private fun MediaItemUi.isInUserList(): Boolean {
    return inReadlist || collectionStatus != null || readlistFolders.isNotEmpty()
}
@Composable
private fun <T> CatalogFilterButton(
    modifier: Modifier = Modifier,
    title: String,
    options: List<T>,
    selectedOption: T,
    optionTitle: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilledTonalButton(
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
            onDismissRequest = { expanded = false }
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

internal fun buildReadlistFolders(extraFolders: List<ReadlistFolder>): List<ReadlistFolder> {
    return extraFolders
        .filter { it.title.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
}

internal fun buildCatalogTypeFilters(): List<CatalogTypeFilter> {
    return buildList {
        add(CatalogTypeFilter.All)
        addAll(MediaType.values().map { CatalogTypeFilter.ByType(it) })
    }
}

internal fun filterCatalogItems(
    items: List<MediaItemUi>,
    activeTypeFilter: CatalogTypeFilter
): List<MediaItemUi> {
    return items.filter { activeTypeFilter.matches(it) }
}

internal fun catalogEmptyMessage(searchQuery: String): String {
    return if (searchQuery.isBlank()) {
        "Каталог пока пуст"
    } else {
        "Ничего не найдено"
    }
}
internal sealed interface CatalogTypeFilter {
    val title: String

    data object All : CatalogTypeFilter {
        override val title = "Все"
    }

    data class ByType(private val type: MediaType) : CatalogTypeFilter {
        override val title: String = type.title
        fun mediaType() = type
    }

    fun matches(item: MediaItemUi): Boolean {
        return when (this) {
            All -> true
            is ByType -> item.type == mediaType()
        }
    }
}
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
private val PreviewMediaItems = listOf(
    MediaItemUi(
        "0",
        "Клинок, рассекающий демонов: бесконечная крепость",
        type = MediaType.Anime,
        inReadlist = true,
        collectionStatus = CollectionStatus.Completed,
        userRating = 8,
        userNote = "Отличная анимация, хочется пересмотреть финальные серии.",
    ),
    MediaItemUi(
        "1",
        "Подземелье вкусностей",
        type = MediaType.Anime,
    ),
)
