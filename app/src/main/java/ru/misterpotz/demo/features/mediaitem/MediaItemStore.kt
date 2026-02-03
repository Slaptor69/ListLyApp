package ru.misterpotz.demo.features.mediaitem

import kotlinx.coroutines.flow.Flow
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

sealed interface MediaItemCommand {
    data class Load(val id: Int) : MediaItemCommand
    data class SetTracked(val item: MediaItem, val tracked: Boolean) : MediaItemCommand
    data class SetReadlist(val item: MediaItem, val inReadlist: Boolean) : MediaItemCommand
}

sealed interface MediaItemEvent {
    data object Init : MediaItemEvent
    data object Close : MediaItemEvent
    data object ToggleTracked : MediaItemEvent
    data object ToggleReadlist : MediaItemEvent

    object Internal {
        data class Loaded(val mediaItem: MediaItem) : MediaItemEvent
        data class LoadError(val throwable: Throwable) : MediaItemEvent
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
            MediaItemEvent.Init -> commands { +MediaItemCommand.Load(state.id) }

            MediaItemEvent.Close -> effects { +MediaItemEffect.Close }

            MediaItemEvent.ToggleReadlist -> {
                val item = state.mediaItem.requireContent()
                commands { +MediaItemCommand.SetReadlist(item, !item.inReadlist) }
                state { copy(readlist = Loadable.Loading()) }
            }

            MediaItemEvent.ToggleTracked -> {
                val item = state.mediaItem.requireContent()
                commands { +MediaItemCommand.SetTracked(item, !item.tracked) }
                state { copy(tracked = Loadable.Loading()) }
            }

            is MediaItemEvent.Internal.Loaded -> state {
                copy(
                    mediaItem = event.mediaItem.toLoadable(),
                    tracked = Loadable.Content(Unit),
                    readlist = Loadable.Content(Unit)
                )
            }

            is MediaItemEvent.Internal.LoadError -> state {
                copy(
                    mediaItem = event.throwable.toLoadable(),
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

    override fun execute(command: MediaItemCommand): Flow<MediaItemEvent> = flow {
        try {
            when (command) {
                is MediaItemCommand.Load -> {
                    val item = mediaItemRepository.getMediaItem(command.id)
                    if (item != null) emit(MediaItemEvent.Internal.Loaded(item))
                }

                is MediaItemCommand.SetReadlist -> {
                    mediaItemInteractor.setMediaItemInReadlist(command.item.id, command.inReadlist)
                    val item = mediaItemRepository.getMediaItem(command.item.id)
                    if (item != null) emit(MediaItemEvent.Internal.Loaded(item))
                }

                is MediaItemCommand.SetTracked -> {
                    mediaItemInteractor.setMediaItemTracked(command.item.id, command.tracked)
                    val item = mediaItemRepository.getMediaItem(command.item.id)
                    if (item != null) emit(MediaItemEvent.Internal.Loaded(item))
                }
            }
        } catch (t: Throwable) {
            emit(MediaItemEvent.Internal.LoadError(t))
        }
    }
}

class MediaItemStoreFactory @Inject constructor(
    private val actor: MediaItemActor
) {
    fun create(mediaItemId: Int): ElmStore<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand> =
        ElmStore(
            initialState = MediaItemState(id = mediaItemId),
            reducer = MediaItemReducer,
            actor = actor,
            startEvent = MediaItemEvent.Init
        )
}
