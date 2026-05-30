package ru.misterpotz.listly.features.readlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.misterpotz.listly.BottomBarDestination
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.R
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.features.favourite.FavouriteButton
import ru.misterpotz.listly.features.folders.CreateFolderDialog
import ru.misterpotz.listly.features.folders.CreateFolderDropdownMenuItem
import ru.misterpotz.listly.features.notes.UserMediaSummaryPlate
import ru.misterpotz.listly.features.poster.MediaPosterPlaceholder
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.LocalGlobalBackstackProvider
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log
import ru.misterpotz.listly.utils.toUserFriendlyMessage


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
    val backstack = LocalGlobalBackstackProvider.current

    val typeFilters = buildReadlistTypeFilters()
    val folders = remember(state.readlistFolders, items) {
        buildReadlistFolders(state.readlistFolders + items.orEmpty().flatMap { it.readlistFolders })
    }
    val folderFilters = remember(folders) {
        listOf(ReadlistFolderFilter.All) + folders.map { ReadlistFolderFilter.ByFolder(it) }
    }
    var activeTypeFilter by remember { mutableStateOf<ReadlistFilter>(ReadlistFilter.All) }
    var activeFolderFilter by remember { mutableStateOf<ReadlistFolderFilter>(ReadlistFolderFilter.All) }
    var activeSort by remember { mutableStateOf(ReadlistSort.ByAddedDate) }
    val visibleItems = visibleReadlistItems(
        items = items.orEmpty(),
        typeFilter = activeTypeFilter,
        folderFilter = activeFolderFilter,
        sort = activeSort
    )

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            ReadlistSortButton(
                selectedSort = activeSort,
                onSortSelected = { activeSort = it }
            )
        }

        if (state.mediaItems.isError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.mediaItems.error.toUserFriendlyMessage())
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
                ElevatedCard(
                    onClick = {
                        backstack.add(
                            GlobalAppNavKey.MediaItemScreen(
                                id = item.id,
                                returnDestination = BottomBarDestination.Readlist
                            )
                        )
                    },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReadlistPoster(item)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                            UserMediaSummaryPlate(
                                status = item.collectionStatus,
                                userRating = item.userRating,
                                note = item.userNote
                            )
                        }
                        FavouriteButton(
                            isFavourite = item.isFavourite,
                            isLoading = state.itemToLoading.contains(item.id),
                            onToggle = {
                                onEvent(ReadlistEvent.Ui.ToggleFavourite(item))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadlistPoster(item: MediaItem) {
    val context = LocalContext.current
    val posterModifier = Modifier
        .size(width = 56.dp, height = 84.dp)
        .clip(RoundedCornerShape(8.dp))

    if (item.imageUrl.isNullOrBlank()) {
        MediaPosterPlaceholder(modifier = posterModifier)
        return
    }

    AsyncImage(
        modifier = posterModifier,
        imageLoader = appComponent.imageLoader,
        model = ImageRequest.Builder(context)
            .data(item.imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = item.title,
        contentScale = ContentScale.Crop
    )
}

/** Кнопка сортировки readlist. */
@Composable
private fun ReadlistSortButton(
    selectedSort: ReadlistSort,
    onSortSelected: (ReadlistSort) -> Unit
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
            ReadlistSort.values().forEach { sort ->
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

internal fun buildReadlistTypeFilters(): List<ReadlistFilter> {
    return buildList {
        add(ReadlistFilter.All)
        addAll(MediaType.values().map { ReadlistFilter.ByType(it) })
    }
}

internal fun visibleReadlistItems(
    items: List<MediaItem>,
    typeFilter: ReadlistFilter,
    folderFilter: ReadlistFolderFilter,
    sort: ReadlistSort
): List<MediaItem> {
    return items
        .filter { item ->
            typeFilter.matches(item) && folderFilter.matches(item)
        }
        .let { filteredItems ->
            sort.sort(filteredItems)
        }
}

/** Варианты сортировки readlist. */
internal enum class ReadlistSort(val title: String) {
    Alphabet("По алфавиту"),
    ByAddedDate("По дате добавления");

    fun sort(items: List<MediaItem>): List<MediaItem> {
        return when (this) {
            Alphabet -> items.sortedBy { it.title.lowercase() }
            ByAddedDate -> items.sortedWith(
                compareByDescending<MediaItem> { it.readlistAddedAt ?: Long.MIN_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        }
    }
}

/**
 * Локальные фильтры только для отображения readlist.
 *
 * Они не вынесены в Store, потому что пока не влияют на доменные данные
 * и нужны лишь для текущей отрисовки.
 */
internal sealed interface ReadlistFilter {
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
internal sealed interface ReadlistFolderFilter {
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
            is ByFolder -> item.readlistFolders.any { it == folder }
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
