package ru.misterpotz.listly.features.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.misterpotz.listly.BottomBarDestination
import ru.misterpotz.listly.GlobalAppNavKey
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.features.liststate.UserMediaBadgesRow
import ru.misterpotz.listly.features.liststate.UserMediaListActionButton
import ru.misterpotz.listly.features.liststate.UserMediaListManagementDialog
import ru.misterpotz.listly.features.liststate.userMediaListActionTitle
import ru.misterpotz.listly.features.poster.MediaPosterPlaceholder
import ru.misterpotz.listly.ui.theme.ListlyTheme
import ru.misterpotz.listly.ui.utils.ObserveLifecycleEvents
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.utils.log
import ru.misterpotz.listly.utils.toUserFriendlyMessage
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
                    onSaveListState = { status, folders ->
                        onEvent(CatalogEvent.Ui.SaveListState(item, status, folders))
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
    onSaveListState: (CollectionStatus, List<ReadlistFolder>) -> Unit,
    onCreateFolder: (ReadlistFolder) -> Unit,
    onRemoveFromReadlist: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    var managementDialogVisible by remember { mutableStateOf(false) }
    val inUserList = item.isInUserList()

    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CatalogPoster(item)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = CatalogPosterHeight),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.type.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (inUserList) {
                        CatalogFavouriteButton(
                            isFavourite = item.isFavourite,
                            isLoading = isLoading,
                            onToggle = onToggleFavourite
                        )
                    }
                }

                if (inUserList) {
                    UserMediaBadgesRow(
                        status = item.collectionStatus,
                        folderCount = item.readlistFolders.size
                    )
                }

                item.userNote?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inUserList) {
                        Text(
                            text = userRatingText(item.userRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    UserMediaListActionButton(
                        title = userMediaListActionTitle(inUserList),
                        isLoading = isLoading,
                        onClick = { managementDialogVisible = true }
                    )
                }
            }
        }
    }

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
                onSaveListState(status, selectedFolders)
            },
            onRemoveFromList = {
                managementDialogVisible = false
                onRemoveFromReadlist()
            }
        )
    }
}

@Composable
private fun CatalogPoster(item: MediaItemUi) {
    val context = LocalContext.current
    val posterModifier = Modifier
        .size(width = CatalogPosterWidth, height = CatalogPosterHeight)
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

@Composable
private fun CatalogFavouriteButton(
    isFavourite: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val drawAsFavourite = isFavourite || isPressed
    val color = if (drawAsFavourite) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .toggleable(
                value = isFavourite,
                enabled = !isLoading,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = { onToggle() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 28.dp, height = 26.dp)) {
            val heart = Path().apply {
                moveTo(size.width * 0.50f, size.height * 0.88f)
                cubicTo(
                    size.width * 0.08f,
                    size.height * 0.58f,
                    size.width * 0.03f,
                    size.height * 0.36f,
                    size.width * 0.17f,
                    size.height * 0.18f
                )
                cubicTo(
                    size.width * 0.29f,
                    size.height * 0.02f,
                    size.width * 0.45f,
                    size.height * 0.11f,
                    size.width * 0.50f,
                    size.height * 0.25f
                )
                cubicTo(
                    size.width * 0.55f,
                    size.height * 0.11f,
                    size.width * 0.71f,
                    size.height * 0.02f,
                    size.width * 0.83f,
                    size.height * 0.18f
                )
                cubicTo(
                    size.width * 0.97f,
                    size.height * 0.36f,
                    size.width * 0.92f,
                    size.height * 0.58f,
                    size.width * 0.50f,
                    size.height * 0.88f
                )
                close()
            }
            if (drawAsFavourite) {
                drawPath(
                    path = heart,
                    color = color.copy(alpha = if (isLoading) 0.42f else 1f)
                )
            } else {
                drawPath(
                    path = heart,
                    color = color.copy(alpha = if (isLoading) 0.42f else 1f),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
        }
    }
}

private fun userRatingText(userRating: Int?): String {
    return "Оценка: ${userRating?.let { "$it/10" } ?: "нет"}"
}

private fun MediaItemUi.isInUserList(): Boolean {
    return inReadlist || collectionStatus != null || readlistFolders.isNotEmpty()
}

/** Кнопка фильтра каталога со всплывающим списком вариантов. */
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

/** Локальный фильтр каталога по типу медиаконтента. */
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

private val CatalogPosterWidth = 72.dp
private val CatalogPosterHeight = 108.dp
