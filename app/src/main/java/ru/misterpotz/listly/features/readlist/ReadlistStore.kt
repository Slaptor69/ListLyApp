package ru.misterpotz.listly.features.readlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.models.ReadlistQuery
import ru.misterpotz.listly.domain.models.ReadlistSortMode
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject
sealed interface ReadlistCommand {
    data class Reload(val query: ReadlistQuery) : ReadlistCommand
    data class AddReadlistFolder(val folder: ReadlistFolder, val query: ReadlistQuery) : ReadlistCommand
    data class SetFavourite(
        val mediaItemId: String,
        val isFavourite: Boolean,
        val query: ReadlistQuery
    ) : ReadlistCommand
}
sealed interface ReadlistEffect {}
sealed interface ReadlistEvent {
    object Ui {
        data object OnResume : ReadlistEvent
        data object OnPause : ReadlistEvent
        data class CreateFolder(val folder: ReadlistFolder) : ReadlistEvent
        data class ToggleFavourite(val mediaItem: MediaItem) : ReadlistEvent
        data class SelectMediaType(val mediaType: MediaType?) : ReadlistEvent
        data class SelectFolders(val folders: List<ReadlistFolder>) : ReadlistEvent
        data class SelectStatus(val status: CollectionStatus?) : ReadlistEvent
        data class SelectFavouriteOnly(val favouriteOnly: Boolean) : ReadlistEvent
        data class SelectSort(val sort: ReadlistSortMode) : ReadlistEvent
    }

    object Internal {
        data class DataLoaded(
            val mediaItems: List<MediaItem>,
            val folders: List<ReadlistFolder>
        ) : ReadlistEvent
        data class LoadError(val throwable: Throwable? = null) : ReadlistEvent
    }
}
class ReadlistActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<ReadlistCommand, ReadlistEvent>() {
    override fun execute(command: ReadlistCommand): Flow<ReadlistEvent> {
        return when (command) {
            is ReadlistCommand.Reload -> flow {
                mediaItemRepository.refreshReadlist(command.query)
                emit(
                    ReadlistEvent.Internal.DataLoaded(
                        mediaItemRepository.getReadlistItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })

            is ReadlistCommand.AddReadlistFolder -> flow {
                mediaItemRepository.addReadlistFolder(command.folder)
                mediaItemRepository.refreshReadlist(command.query)
                emit(
                    ReadlistEvent.Internal.DataLoaded(
                        mediaItemRepository.getReadlistItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })

            is ReadlistCommand.SetFavourite -> flow {
                mediaItemInteractor.setMediaItemFavourite(
                    command.mediaItemId,
                    command.isFavourite
                )
                mediaItemRepository.refreshReadlist(command.query)
                emit(
                    ReadlistEvent.Internal.DataLoaded(
                        mediaItemRepository.getReadlistItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })
        }
    }
}
data class ReadlistState(
    val mediaItems: Loadable<List<MediaItem>> = Loadable.Loading(),
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val query: ReadlistQuery = ReadlistQuery(),
    val itemToLoading: Map<String, Boolean> = emptyMap()
)
class ReadlistStoreFactory @Inject constructor(
    val readlistActor: ReadlistActor
) {
    fun create(/* may pass additional parameters */): ElmStore<ReadlistEvent, ReadlistState, ReadlistEffect, ReadlistCommand> {
        return ElmStore(
            initialState = ReadlistState(),
            reducer = ReadlistReducer,
            actor = readlistActor,
        )
    }
}
object ReadlistReducer :
    StateReducer<ReadlistEvent, ReadlistState, ReadlistEffect, ReadlistCommand>() {
    override fun Result.reduce(event: ReadlistEvent) {
        when (event) {
            is ReadlistEvent.Internal.DataLoaded -> state {
                copy(
                    mediaItems = event.mediaItems.toLoadable(),
                    readlistFolders = event.folders,
                    itemToLoading = emptyMap()
                )
            }

            is ReadlistEvent.Internal.LoadError -> state {
                copy(
                    mediaItems = event.throwable.toLoadable(),
                    itemToLoading = emptyMap()
                )
            }

            is ReadlistEvent.Ui.CreateFolder -> commands {
                +ReadlistCommand.AddReadlistFolder(event.folder, state.query)
            }

            is ReadlistEvent.Ui.ToggleFavourite -> commands {
                val nextFavourite = !event.mediaItem.isFavourite
                +ReadlistCommand.SetFavourite(
                    event.mediaItem.id,
                    nextFavourite,
                    state.query
                )
                state {
                    val updatedItems = mediaItems.content
                        ?.mapNotNull { item ->
                            if (item.id == event.mediaItem.id) {
                                item.copy(isFavourite = nextFavourite)
                            } else {
                                item
                            }.takeUnless {
                                state.query.favouriteOnly && it.id == event.mediaItem.id && !nextFavourite
                            }
                        }
                        ?.toLoadable()
                        ?: mediaItems
                    copy(
                        mediaItems = updatedItems,
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            is ReadlistEvent.Ui.SelectMediaType -> {
                val query = state.query.copy(mediaType = event.mediaType)
                state { copy(query = query, mediaItems = Loadable.Loading()) }
                commands { +ReadlistCommand.Reload(query) }
            }

            is ReadlistEvent.Ui.SelectFolders -> {
                val query = state.query.copy(folders = event.folders)
                state { copy(query = query, mediaItems = Loadable.Loading()) }
                commands { +ReadlistCommand.Reload(query) }
            }

            is ReadlistEvent.Ui.SelectStatus -> {
                val query = state.query.copy(status = event.status)
                state { copy(query = query, mediaItems = Loadable.Loading()) }
                commands { +ReadlistCommand.Reload(query) }
            }

            is ReadlistEvent.Ui.SelectFavouriteOnly -> {
                val query = state.query.copy(favouriteOnly = event.favouriteOnly)
                state { copy(query = query, mediaItems = Loadable.Loading()) }
                commands { +ReadlistCommand.Reload(query) }
            }

            is ReadlistEvent.Ui.SelectSort -> {
                val query = state.query.copy(sort = event.sort)
                state { copy(query = query, mediaItems = Loadable.Loading()) }
                commands { +ReadlistCommand.Reload(query) }
            }

            ReadlistEvent.Ui.OnPause -> Unit

            ReadlistEvent.Ui.OnResume -> commands {
                +ReadlistCommand.Reload(state.query)
            }
        }
    }
}
