package ru.misterpotz.listly.features.mediaitem

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import ru.misterpotz.listly.BottomBarDestination
import ru.misterpotz.listly.R
import ru.misterpotz.listly.appComponent
import ru.misterpotz.listly.backstackForClosingMediaItem
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.features.favourite.FavouriteButton
import ru.misterpotz.listly.features.liststate.UserMediaBadgesRow
import ru.misterpotz.listly.features.liststate.UserMediaListActionButton
import ru.misterpotz.listly.features.liststate.UserMediaListManagementDialog
import ru.misterpotz.listly.features.liststate.userMediaListActionTitle
import ru.misterpotz.listly.features.notes.UserNoteEditorPlate
import ru.misterpotz.listly.features.rating.UserRatingSelectorButton
import ru.misterpotz.listly.ui.utils.StandardElmScreen
import ru.misterpotz.listly.toGlobalAppNavKeys
import ru.misterpotz.listly.utils.toUserFriendlyMessage
@Composable
fun MediaItemScreenEntry(
    mediaItem: String,
    returnDestination: BottomBarDestination = BottomBarDestination.Catalog
) {
    StandardElmScreen(
        storeFactory = { appComponent.mediaItemStoreFactory.create(mediaItem) },
        onEffect = {
            when (it) {
                MediaItemEffect.Close -> {
                    val targetStack = backstackForClosingMediaItem(
                        currentStack = backstack.toGlobalAppNavKeys(),
                        returnDestination = returnDestination
                    )
                    while (backstack.isNotEmpty()) {
                        backstack.removeLastOrNull()
                    }
                    targetStack.forEach { key -> backstack.add(key) }
                }
            }
        },
        body = { state, onEffect -> MediaItemScreenContent(state, onEffect) }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaItemScreenContent(
    state: MediaItemState,
    onEvent: (MediaItemEvent) -> Unit,
) {
    val item = state.mediaItem.content ?: run {
        // Для всех промежуточных состояний показываем один простой fallback.
        Box(Modifier.fillMaxSize()) {
            Text("Позиция не загружена")
        }
        return
    }
    var managementDialogVisible by remember(item.id) { mutableStateOf(false) }
    val inUserList = item.isInUserList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(MediaItemEvent.Close) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    UserMediaListActionButton(
                        title = userMediaListActionTitle(inUserList),
                        isLoading = state.readlist.isLoading,
                        onClick = { managementDialogVisible = true }
                    )
                }
                if (state.readlist.isError) {
                    Text(
                        text = state.readlist.error.toUserFriendlyMessage(),
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(inner)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MediaPoster(item)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(if (inUserList) "В моих списках" else "Не в моих списках") }
                )
                if (inUserList) {
                    UserRatingSelectorButton(
                        rating = item.userRating,
                        enabled = true,
                        isLoading = state.readlist.isLoading,
                        onRatingSelected = { rating ->
                            onEvent(MediaItemEvent.SelectUserRating(rating))
                        }
                    )
                }
                Spacer(Modifier.weight(1f))
                if (inUserList) {
                    FavouriteButton(
                        isFavourite = item.isFavourite,
                        isLoading = state.readlist.isLoading,
                        onToggle = {
                            onEvent(MediaItemEvent.ToggleFavourite)
                        }
                    )
                }
            }

            if (inUserList) {
                UserMediaBadgesRow(
                    status = item.collectionStatus,
                    folderNames = item.readlistFolders.map { it.title }
                )
            }

            if (inUserList) {
                UserNoteEditorPlate(
                    note = item.userNote,
                    canEdit = true,
                    isSaving = state.readlist.isLoading,
                    onSave = { note -> onEvent(MediaItemEvent.SaveUserNote(note)) }
                )
            }

            if (!item.annotation.isNullOrBlank()) {
                Text(
                    text = item.annotation,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "Описание отсутствует",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (managementDialogVisible) {
        UserMediaListManagementDialog(
            currentStatus = item.collectionStatus,
            selectedFolders = item.readlistFolders,
            availableFolders = state.readlistFolders,
            inUserList = inUserList,
            onDismiss = { managementDialogVisible = false },
            onCreateFolder = { folder ->
                onEvent(MediaItemEvent.CreateFolder(folder))
            },
            onSave = { status, folders ->
                managementDialogVisible = false
                onEvent(MediaItemEvent.SaveListState(status, folders))
            },
            onRemoveFromList = {
                managementDialogVisible = false
                onEvent(MediaItemEvent.RemoveFromReadlist)
            }
        )
    }
}

private fun MediaItem.isInUserList(): Boolean {
    return inReadlist || collectionStatus != null || readlistFolders.isNotEmpty()
}

@Composable
private fun MediaPoster(item: MediaItem) {
    val ctx = LocalContext.current
    var imageError by remember(item.imageUrl) { mutableStateOf<PosterError?>(null) }
    val posterModifier = Modifier
        .fillMaxWidth()
        .height(400.dp)
        .clip(RoundedCornerShape(12.dp))

    if (item.imageUrl.isNullOrBlank() || imageError != null) {
        PosterPlaceholder(
            modifier = posterModifier,
            text = if (item.imageUrl.isNullOrBlank()) {
                "Постер не пришёл с backend"
            } else if (imageError?.message?.contains("HTTP 404", ignoreCase = true) == true) {
                "Постер не найден по ссылке backend"
            } else {
                "Не удалось загрузить постер"
            },
            details = imageError?.message,
            imageUrl = item.imageUrl,
        )
        return
    }

    AsyncImage(
        modifier = posterModifier,
        imageLoader = appComponent.imageLoader,
        model = ImageRequest.Builder(ctx)
            .data(item.imageUrl)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    val throwable = result.throwable
                    imageError = PosterError(
                        message = "${throwable::class.simpleName}: ${throwable.message.orEmpty()}"
                    )
                    Log.e("Coil", "Poster load failed: ${item.imageUrl}", result.throwable)
                },
                onStart = {
                    imageError = null
                    Log.d("Coil", "Poster load start: ${item.imageUrl}")
                },
                onSuccess = { _, _ -> Log.d("Coil", "Poster load success: ${item.imageUrl}") }
            )
            .build(),
        contentDescription = item.title,
        contentScale = ContentScale.Fit
    )
}

@Immutable
private data class PosterError(
    val message: String,
)

@Composable
private fun PosterPlaceholder(
    modifier: Modifier,
    text: String,
    details: String? = null,
    imageUrl: String? = null,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!details.isNullOrBlank()) {
                Text(
                    text = details,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!imageUrl.isNullOrBlank()) {
                Text(
                    text = imageUrl,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

