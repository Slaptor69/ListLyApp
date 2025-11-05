package ru.misterpotz.demo.features.mediaitem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.demo.domain.interactors.MediaItemInteractor
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.domain.repositories.MediaItemRepository
import ru.misterpotz.demo.utils.Loadable
import ru.misterpotz.demo.utils.toLoadable
import javax.inject.Inject

class MediaItemStore {
}

sealed interface MediaItemCommand {
    data class Load(val id: Int) : MediaItemCommand
    data class SetTracked(val mediaitem: MediaItem, val tracked: Boolean) : MediaItemCommand
    data class SetReadlist(val mediaitem: MediaItem, val readlist: Boolean) : MediaItemCommand
}

sealed interface MediaItemEvent {
    data object Init : MediaItemEvent
    data object Close : MediaItemEvent
    data object ToggleTracked : MediaItemEvent
    data object ToggleReadlist : MediaItemEvent

    object Internal {
        data class Loaded(val mediaItem: MediaItem) : MediaItemEvent
    }
}

sealed interface MediaItemEffect {
    data object Close : MediaItemEffect
}

data class MediaItemState(
    val id: Int,
    val mediaItem: Loadable<MediaItem> = Loadable.Loading(),
    val readlist: Loadable<Unit> = Loadable.Content(Unit),
    val tracked: Loadable<Unit> = Loadable.Content(Unit)
)

object MediaItemReducer :
    StateReducer<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand>() {
    override fun Result.reduce(event: MediaItemEvent) {
        when (event) {
            MediaItemEvent.Init -> commands {
                +MediaItemCommand.Load(state.id)
            }

            MediaItemEvent.Close -> effects {
                +MediaItemEffect.Close
            }

            MediaItemEvent.ToggleReadlist -> commands {
                +MediaItemCommand.SetReadlist(
                    state.mediaItem.requireContent(),
                    state.mediaItem.content!!.tracked.not()
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            MediaItemEvent.ToggleTracked -> commands {
                +MediaItemCommand.SetTracked(
                    state.mediaItem.requireContent(),
                    state.mediaItem.content!!.inReadlist.not()
                )
                state {
                    copy(tracked = Loadable.Loading())
                }
            }

            is MediaItemEvent.Internal.Loaded -> state {
                copy(
                    mediaItem = event.mediaItem.toLoadable(),
                    tracked = Loadable.Content(Unit),
                    readlist = Loadable.Content(Unit)
                )
            }
        }
    }
}

class MediaItemActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<MediaItemCommand, MediaItemEvent>() {
    override fun execute(command: MediaItemCommand): Flow<MediaItemEvent> {
        return when (command) {
            is MediaItemCommand.Load -> flow {
                emit(loadItem(command.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })

            is MediaItemCommand.SetReadlist -> flow {
                mediaItemInteractor.setMediaItemInReadlist(
                    command.mediaitem.id,
                    command.mediaitem.inReadlist.not()
                )
                emit(loadItem(command.mediaitem.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })

            is MediaItemCommand.SetTracked -> flow {
                mediaItemInteractor.setMediaItemTracked(
                    command.mediaitem.id,
                    command.mediaitem.tracked.not()
                )
                emit(loadItem(command.mediaitem.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })
        }
    }

    private fun loadItem(id: Int): MediaItem? {
        return mediaItemRepository.getMediaItem(id)
    }
}


class MediaItemStoreFactory @Inject constructor(
    private val mediaItemActor: MediaItemActor
) {
    fun create(mediaItemId: Int): ElmStore<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand> {
        return ElmStore(
            initialState = MediaItemState(mediaItemId),
            reducer = MediaItemReducer,
            actor = mediaItemActor,
            startEvent = MediaItemEvent.Init
        )
    }
}