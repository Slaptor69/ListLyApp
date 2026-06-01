package ru.misterpotz.listly.features.catalog

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
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject
sealed interface CatalogCommand {
    data class Search(val query: String) : CatalogCommand
    data class AddReadlistFolder(val folder: ReadlistFolder) : CatalogCommand
    data class AddToReadList(
        val mediaItemId: String,
        val status: CollectionStatus,
        val folders: List<ReadlistFolder>
    ) : CatalogCommand
    data class SetFavourite(val mediaItemId: String, val isFavourite: Boolean) : CatalogCommand
    data class RemoveFromReadlist(val mediaItemId: String) : CatalogCommand
}
sealed interface CatalogEffect {
    data class NavigateToItem(val mediaItemId: String) : CatalogEffect
}
sealed interface CatalogEvent {
    data object Init : CatalogEvent

    data object Ui {
        data object OnResume : CatalogEvent
        data class SearchChanged(val query: String) : CatalogEvent
        data class AddToReadList(
            val mediaItem: MediaItemUi,
            val status: CollectionStatus,
            val folders: List<ReadlistFolder>
        ) : CatalogEvent
        data class CreateFolder(val folder: ReadlistFolder) : CatalogEvent
        data class RemoveFromReadingList(val mediaItem: MediaItemUi) : CatalogEvent
        data class ToggleFavourite(val mediaItem: MediaItemUi) : CatalogEvent
        data class ClickItem(val mediaItem: MediaItemUi) : CatalogEvent
    }

    object Internal {
        data class LoadError(val throwable: Throwable? = null) : CatalogEvent
        data class LoadedData(
            val items: List<MediaItem>,
            val folders: List<ReadlistFolder>
        ) : CatalogEvent
        data class FoldersLoaded(val folders: List<ReadlistFolder>) : CatalogEvent
        data class ItemUpdated(val mediaItem: MediaItem) : CatalogEvent
        data class ItemActionError(
            val mediaItemId: String,
            val throwable: Throwable? = null
        ) : CatalogEvent
    }
}
class CatalogActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<CatalogCommand, CatalogEvent>() {
    override fun execute(command: CatalogCommand): Flow<CatalogEvent> {
        return when (command) {
            is CatalogCommand.Search -> flow {
                mediaItemRepository.searchCatalog(command.query)
                emit(
                    CatalogEvent.Internal.LoadedData(
                        mediaItemRepository.getItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.AddReadlistFolder -> flow {
                emit(
                    CatalogEvent.Internal.FoldersLoaded(
                        mediaItemRepository.addReadlistFolder(command.folder)
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.AddToReadList -> flow {
                val updatedItem = mediaItemInteractor.addMediaItemToReadList(
                    command.mediaItemId,
                    command.status,
                    command.folders
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.ItemActionError(command.mediaItemId, it) })

            is CatalogCommand.RemoveFromReadlist -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemInReadlist(command.mediaItemId, false)
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.ItemActionError(command.mediaItemId, it) })

            is CatalogCommand.SetFavourite -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemFavourite(
                    command.mediaItemId,
                    command.isFavourite
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.ItemActionError(command.mediaItemId, it) })
        }
    }
}
class CatalogStoreFactory @Inject constructor(
    private val catalogActor: CatalogActor
) {
    fun create(): ElmStore<CatalogEvent, CatalogState, CatalogEffect, CatalogCommand> {
        return ElmStore(
            initialState = CatalogState(),
            reducer = CatalogReducer,
            actor = catalogActor,
            startEvent = CatalogEvent.Init
        )
    }
}
data class CatalogState(
    val items: Loadable<List<MediaItemUi>> = Loadable.Loading(),
    val searchQuery: String = "",
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val itemToLoading: Map<String, Boolean> = mapOf(),
    val actionError: Throwable? = null
)
data class MediaItemUi(
    val id: String,
    val title: String,
    val type: MediaType,
    val inReadlist: Boolean = false,
    val readlistFolder: ReadlistFolder? = null,
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val readlistAddedAt: Long? = null,
    val collectionStatus: CollectionStatus? = null,
    val isFavourite: Boolean = false,
    val userRating: Int? = null,
    val userNote: String? = null,
    val imageUrl: String? = null,
)
fun MediaItem.toMediaItemUi() = MediaItemUi(
    id = id,
    title = title,
    type = type,
    inReadlist = inReadlist,
    readlistFolder = readlistFolder,
    readlistFolders = readlistFolders,
    readlistAddedAt = readlistAddedAt,
    collectionStatus = collectionStatus,
    isFavourite = isFavourite,
    userRating = userRating,
    userNote = userNote,
    imageUrl = imageUrl
)
object CatalogReducer : StateReducer<CatalogEvent, CatalogState, CatalogEffect, CatalogCommand>() {
    override fun Result.reduce(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Init -> Unit
            CatalogEvent.Ui.OnResume -> {
                commands {
                    // На resume просим Actor перезагрузить список.
                    +CatalogCommand.Search(state.searchQuery)
                }
            }

            is CatalogEvent.Ui.SearchChanged -> {
                state {
                    copy(
                        searchQuery = event.query,
                        items = Loadable.Loading(items.content),
                        actionError = null
                    )
                }
                commands {
                    +CatalogCommand.Search(event.query)
                }
            }

            is CatalogEvent.Ui.ClickItem -> {
                effects {
                    +CatalogEffect.NavigateToItem(event.mediaItem.id)
                }
            }

            is CatalogEvent.Ui.AddToReadList -> commands {
                +CatalogCommand.AddToReadList(
                    event.mediaItem.id,
                    event.status,
                    event.folders
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        },
                        actionError = null
                    )
                }
            }

            is CatalogEvent.Ui.CreateFolder -> commands {
                +CatalogCommand.AddReadlistFolder(event.folder)
            }

            is CatalogEvent.Ui.RemoveFromReadingList -> commands {
                +CatalogCommand.RemoveFromReadlist(event.mediaItem.id)
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        },
                        actionError = null
                    )
                }
            }

            is CatalogEvent.Ui.ToggleFavourite -> commands {
                +CatalogCommand.SetFavourite(
                    event.mediaItem.id,
                    !event.mediaItem.isFavourite
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        },
                        actionError = null
                    )
                }
            }

            is CatalogEvent.Internal.ItemUpdated -> state {
                copy(
                    items = items.requireContent().map {
                        if (it.id == event.mediaItem.id) {
                            event.mediaItem.toMediaItemUi()
                        } else {
                            it
                        }
                    }.toLoadable(),
                    itemToLoading = buildMap {
                        putAll(state.itemToLoading)
                        remove(event.mediaItem.id)
                    },
                    actionError = null
                )
            }

            is CatalogEvent.Internal.LoadedData -> state {
                copy(
                    items = event.items.map { it.toMediaItemUi() }.toLoadable(),
                    readlistFolders = event.folders,
                    actionError = null
                )
            }

            is CatalogEvent.Internal.FoldersLoaded -> state {
                copy(readlistFolders = event.folders)
            }

            is CatalogEvent.Internal.LoadError -> state {
                copy(
                    items = event.throwable.toLoadable(),
                    itemToLoading = emptyMap()
                )
            }

            is CatalogEvent.Internal.ItemActionError -> state {
                copy(
                    itemToLoading = buildMap {
                        putAll(state.itemToLoading)
                        remove(event.mediaItemId)
                    },
                    actionError = event.throwable
                )
            }
        }
    }
}
