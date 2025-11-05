package ru.misterpotz.demo.features.readlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.domain.repositories.MediaItemRepository
import ru.misterpotz.demo.utils.Loadable
import ru.misterpotz.demo.utils.toLoadable
import javax.inject.Inject

sealed interface ReadlistCommand {
    data object StartObserving : ReadlistCommand
    data object StopObserving : ReadlistCommand
}

sealed interface ReadlistEffect {}

sealed interface ReadlistEvent {
    object Ui {
        data object OnResume : ReadlistEvent
        data object OnPause : ReadlistEvent
    }

    object Internal {
        data class DataLoaded(val mediaItems: List<MediaItem>) : ReadlistEvent
    }
}

class ReadlistActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository
) : Actor<ReadlistCommand, ReadlistEvent>() {
    override fun execute(command: ReadlistCommand): Flow<ReadlistEvent> {
        return when (command) {
            ReadlistCommand.StartObserving -> mediaItemRepository.getReadlistItems().switch(
                ReadlistCommand.StartObserving
            ).mapEvents({ ReadlistEvent.Internal.DataLoaded(it) })

            ReadlistCommand.StopObserving ->
                cancelSwitchFlows(ReadlistCommand.StartObserving).mapEvents()
        }
    }
}

data class ReadlistState(
    val mediaItems: Loadable<List<MediaItem>> = Loadable.Loading()
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
                copy(mediaItems = event.mediaItems.toLoadable())
            }

            ReadlistEvent.Ui.OnPause -> commands {
                +ReadlistCommand.StopObserving
            }

            ReadlistEvent.Ui.OnResume -> commands {
                +ReadlistCommand.StartObserving
            }
        }
    }
}