package ru.misterpotz.listly.features.readlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.misterpotz.listly.BottomBarDestination
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.R
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.models.ReadlistSortMode
import ru.misterpotz.listly.features.folders.CreateFolderDialog
import ru.misterpotz.listly.features.folders.CreateFolderDropdownMenuItem
import ru.misterpotz.listly.features.media.MediaListItemCard
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.LocalGlobalBackstackProvider
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log
import ru.misterpotz.listly.utils.toUserFriendlyMessage
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
    val backstack = LocalGlobalBackstackProvider.current

    val typeFilters = buildReadlistTypeFilters()
    val statusFilters = buildReadlistStatusFilters()
    val folders = remember(state.readlistFolders, items) {
        buildReadlistFolders(state.readlistFolders + items.orEmpty().flatMap { it.readlistFolders })
    }
    val folderFilters = remember(folders) {
        listOf(ReadlistFolderFilter.All) + folders.map { ReadlistFolderFilter.ByFolder(it) }
    }
    val activeTypeFilter = state.query.mediaType?.let { ReadlistFilter.ByType(it) } ?: ReadlistFilter.All
    val activeFolderFilter = ReadlistFolderFilter.Selected(state.query.folders)
    val activeStatusFilter = state.query.status?.let { ReadlistStatusFilter.ByStatus(it) } ?: ReadlistStatusFilter.All
    val visibleItems = items.orEmpty()

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
                onOptionSelected = { onEvent(ReadlistEvent.Ui.SelectMediaType(it.mediaTypeOrNull())) }
            )
            ReadlistFolderFilterButton(
                modifier = Modifier.weight(1f),
                title = "Папка: ${activeFolderFilter.title}",
                options = folderFilters,
                selectedOption = activeFolderFilter,
                onOptionSelected = { onEvent(ReadlistEvent.Ui.SelectFolders(it.folders())) },
                onFolderCreated = { folder ->
                    onEvent(ReadlistEvent.Ui.CreateFolder(folder))
                }
            )
            ReadlistFilterButton(
                modifier = Modifier.weight(1f),
                title = "Статус: ${activeStatusFilter.title}",
                options = statusFilters,
                selectedOption = activeStatusFilter,
                optionTitle = { it.title },
                onOptionSelected = { onEvent(ReadlistEvent.Ui.SelectStatus(it.statusOrNull())) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReadlistSortButton(
                selectedSort = state.query.sort,
                onSortSelected = { onEvent(ReadlistEvent.Ui.SelectSort(it)) }
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Только избранное",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(
                    modifier = Modifier.scale(0.78f),
                    checked = state.query.favouriteOnly,
                    onCheckedChange = { onEvent(ReadlistEvent.Ui.SelectFavouriteOnly(it)) }
                )
            }
        }

        if (state.mediaItems.isError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.mediaItems.error.toUserFriendlyMessage())
            }
            return
        }

        if (state.mediaItems.isLoading && items == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Загрузка...")
            }
            return
        }

        if (visibleItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список пуст")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibleItems, key = { it.id }) { item ->
                MediaListItemCard(
                    title = item.title,
                    typeTitle = item.type.title,
                    imageUrl = item.imageUrl,
                    inUserList = item.isInUserList(),
                    status = item.collectionStatus,
                    folderNames = item.readlistFolders.map { it.title },
                    isFavourite = item.isFavourite,
                    userRating = item.userRating,
                    userNote = item.userNote,
                    isLoading = state.itemToLoading.contains(item.id),
                    actionTitle = null,
                    onClick = {
                        backstack.add(
                            GlobalAppNavKey.MediaItemScreen(
                                id = item.id,
                                returnDestination = BottomBarDestination.Readlist
                            )
                        )
                    },
                    onToggleFavourite = {
                        onEvent(ReadlistEvent.Ui.ToggleFavourite(item))
                    },
                    onActionClick = null
                )
            }
        }
    }
}

private fun MediaItem.isInUserList(): Boolean {
    return inReadlist || collectionStatus != null || readlistFolders.isNotEmpty()
}
@Composable
private fun ReadlistSortButton(
    selectedSort: ReadlistSortMode,
    onSortSelected: (ReadlistSortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_sort),
                contentDescription = "Сортировка"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp)
        ) {
            ReadlistSortMode.values().forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.title) },
                    trailingIcon = {
                        if (sort == selectedSort) {
                            Text("✓")
                        }
                    },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
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
    val selectedFolders = selectedOption.folders()

    Box(modifier = modifier) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
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
            DropdownMenuItem(
                text = { Text(ReadlistFolderFilter.All.title) },
                leadingIcon = {
                    Checkbox(
                        checked = selectedFolders.isEmpty(),
                        onCheckedChange = null
                    )
                },
                onClick = {
                    onOptionSelected(ReadlistFolderFilter.All)
                }
            )
            options.forEach { option ->
                if (option is ReadlistFolderFilter.All) {
                    return@forEach
                }
                val folder = (option as ReadlistFolderFilter.ByFolder).folder
                val checked = selectedFolders.any { it.sameFolder(folder) }
                DropdownMenuItem(
                    text = { Text(option.title) },
                    leadingIcon = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        val updatedFolders = if (checked) {
                            selectedFolders.filterNot { it.sameFolder(folder) }
                        } else {
                            selectedFolders + folder
                        }
                        onOptionSelected(ReadlistFolderFilter.Selected(updatedFolders))
                    }
                )
            }
            CreateFolderDropdownMenuItem(
                onMenuDismiss = { expanded = false },
                onClick = {
                    createDialogVisible = true
                }
            )
        }
    }

    CreateFolderDialog(
        visible = createDialogVisible,
        onDismiss = { createDialogVisible = false },
        onFolderCreated = { folder ->
            createDialogVisible = false
            onFolderCreated(folder)
        }
    )
}

internal fun buildReadlistFolders(extraFolders: List<ReadlistFolder>): List<ReadlistFolder> {
    return extraFolders
        .filter { it.title.isNotBlank() }
        .distinctBy { it.title.trim().lowercase() }
}

private fun ReadlistFolder.sameFolder(other: ReadlistFolder): Boolean {
    return folderKey() == other.folderKey()
}

private fun ReadlistFolder.folderKey(): String {
    return id ?: title.trim().lowercase()
}

internal fun buildReadlistTypeFilters(): List<ReadlistFilter> {
    return buildList {
        add(ReadlistFilter.All)
        addAll(MediaType.values().map { ReadlistFilter.ByType(it) })
    }
}

internal fun buildReadlistStatusFilters(): List<ReadlistStatusFilter> {
    return buildList {
        add(ReadlistStatusFilter.All)
        addAll(CollectionStatus.values().map { ReadlistStatusFilter.ByStatus(it) })
    }
}
internal sealed interface ReadlistFilter {
    val title: String

    data object All : ReadlistFilter {
        override val title = "Все"
    }

    data class ByType(private val type: MediaType) : ReadlistFilter {
        override val title: String = type.title

        fun mediaType() = type
    }

    fun mediaTypeOrNull(): MediaType? {
        return when (this) {
            All -> null
            is ByType -> mediaType()
        }
    }
}
internal sealed interface ReadlistFolderFilter {
    val title: String

    data object All : ReadlistFolderFilter {
        override val title = "Все"
    }

    data class ByFolder(val folder: ReadlistFolder) : ReadlistFolderFilter {
        override val title: String = folder.title
    }

    data class Selected(private val folders: List<ReadlistFolder>) : ReadlistFolderFilter {
        private val distinctFolders = folders.distinctBy { it.folderKey() }
        override val title: String = when (distinctFolders.size) {
            0 -> All.title
            1 -> distinctFolders.first().title
            else -> "Выбрано: ${distinctFolders.size}"
        }

        fun selectedFolders(): List<ReadlistFolder> = distinctFolders
    }

    fun folders(): List<ReadlistFolder> {
        return when (this) {
            All -> emptyList()
            is ByFolder -> listOf(folder)
            is Selected -> selectedFolders()
        }
    }
}
internal sealed interface ReadlistStatusFilter {
    val title: String

    data object All : ReadlistStatusFilter {
        override val title = "Все"
    }

    data class ByStatus(val status: CollectionStatus) : ReadlistStatusFilter {
        override val title: String = status.title
    }

    fun statusOrNull(): CollectionStatus? {
        return when (this) {
            All -> null
            is ByStatus -> status
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun Preview() {
    ListlyTheme {
        ReadlistScreen()
    }
}
