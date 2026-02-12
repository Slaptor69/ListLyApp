package ru.misterpotz.demo.features.catalog

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.demo.domain.interactors.MediaItemInteractor
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.domain.models.MediaType
import ru.misterpotz.demo.domain.repositories.MediaItemRepository
import ru.misterpotz.demo.utils.Loadable
import ru.misterpotz.demo.utils.toLoadable
import javax.inject.Inject

sealed interface CatalogCommand {
    data object ReloadData : CatalogCommand
    data class SetItemTracked(val mediaItemId: Int, val value: Boolean) : CatalogCommand
    data class SetInReadlist(val mediaItemId: Int, val value: Boolean) : CatalogCommand
}

sealed interface CatalogEffect {
    data class NavigateToItem(val mediaItemId: Int) : CatalogEffect
}

sealed interface CatalogEvent {
    data object Init : CatalogEvent

    data object Ui {
        data object OnResume : CatalogEvent
        data class TrackItem(val mediaItem: MediaItemUi) : CatalogEvent
        data class AddToReadingList(val mediaItem: MediaItemUi) : CatalogEvent
        data class ClickItem(val mediaItem: MediaItemUi) : CatalogEvent
    }

    object Internal {
        data class LoadError(val throwable: Throwable? = null) : CatalogEvent
        data class LoadedData(val items: List<MediaItem>) : CatalogEvent
        data class ItemUpdated(val mediaItem: MediaItem) : CatalogEvent
    }
}

class CatalogActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<CatalogCommand, CatalogEvent>() {
    override fun execute(command: CatalogCommand): Flow<CatalogEvent> {
        return when (command) {
            CatalogCommand.ReloadData -> flow {
                delay(1000) // имитация асинхронной работы, загрузка
                emitAll(
                    mediaItemRepository.getItems()
                        .mapEvents({
                            CatalogEvent.Internal.LoadedData(it)
                        }, {
                            CatalogEvent.Internal.LoadError(it)
                        })
                )
            }

            is CatalogCommand.SetItemTracked -> flow {
                mediaItemInteractor.setMediaItemTracked(
                    command.mediaItemId,
                    command.value
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        mediaItemRepository.getMediaItem(command.mediaItemId) ?: return@flow
                    )
                )
            }

            is CatalogCommand.SetInReadlist -> flow {
                mediaItemInteractor.setMediaItemInReadlist(
                    command.mediaItemId,
                    command.value
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        mediaItemRepository.getMediaItem(command.mediaItemId) ?: return@flow
                    )
                )
            }
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
    val itemToLoading: Map<Int, Boolean> = mapOf()
)

data class MediaItemUi(
    val id: Int,
    val title: String,
    val type: MediaType,
    val tracked: Boolean = false,
    val inReadlist: Boolean = false,
)

fun MediaItem.toMediaItemUi() = MediaItemUi(id, title, type, tracked, inReadlist)

object CatalogReducer : StateReducer<CatalogEvent, CatalogState, CatalogEffect, CatalogCommand>() {
    override fun Result.reduce(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Init -> Unit
            CatalogEvent.Ui.OnResume -> {
                commands {
                    +CatalogCommand.ReloadData
                }
            }

            is CatalogEvent.Ui.ClickItem -> {
                effects {
                    +CatalogEffect.NavigateToItem(event.mediaItem.id)
                }
            }

            is CatalogEvent.Ui.TrackItem -> commands {
                +CatalogCommand.SetItemTracked(
                    event.mediaItem.id, !event.mediaItem.tracked
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            is CatalogEvent.Ui.AddToReadingList -> commands {
                +CatalogCommand.SetInReadlist(
                    event.mediaItem.id, !event.mediaItem.inReadlist
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
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
                    }
                )
            }

            is CatalogEvent.Internal.LoadedData -> state {
                copy(
                    items = event.items.map { it.toMediaItemUi() }.toLoadable(),
                )
            }

            is CatalogEvent.Internal.LoadError -> state {
                copy(items = event.throwable.toLoadable())
            }
        }
    }
}