package ru.misterpotz.listly.features.readlist

import kotlinx.coroutines.flow.Flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject

/** Команды readlist-фичи управляют подпиской на данные. */
sealed interface ReadlistCommand {
    data object StartObserving : ReadlistCommand
    data object StopObserving : ReadlistCommand
}

/** В текущей версии у readlist-фичи нет одноразовых side-effect'ов. */
sealed interface ReadlistEffect {}

/** События readlist-фичи: lifecycle от UI и внутренние обновления данных. */
sealed interface ReadlistEvent {
    object Ui {
        data object OnResume : ReadlistEvent
        data object OnPause : ReadlistEvent
    }

    object Internal {
        data class DataLoaded(val mediaItems: List<MediaItem>) : ReadlistEvent
    }
}

/** Actor readlist слушает поток элементов, находящихся в readlist. */
class ReadlistActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository
) : Actor<ReadlistCommand, ReadlistEvent>() {
    /** Запускает и останавливает поток наблюдения в ответ на команды Store. */
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

/** Состояние readlist-экрана. */
data class ReadlistState(
    val mediaItems: Loadable<List<MediaItem>> = Loadable.Loading()
)

/** Фабрика собирает Store для фичи readlist. */
class ReadlistStoreFactory @Inject constructor(
    val readlistActor: ReadlistActor
) {
    /** Создаёт новый Store экрана списка чтения. */
    fun create(/* may pass additional parameters */): ElmStore<ReadlistEvent, ReadlistState, ReadlistEffect, ReadlistCommand> {
        return ElmStore(
            initialState = ReadlistState(),
            reducer = ReadlistReducer,
            actor = readlistActor,
        )
    }
}

/** Reducer readlist-фичи отвечает за подписку и сохранение полученных элементов в State. */
object ReadlistReducer :
    StateReducer<ReadlistEvent, ReadlistState, ReadlistEffect, ReadlistCommand>() {
    /** Реакция readlist-фичи на lifecycle и обновления данных. */
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
