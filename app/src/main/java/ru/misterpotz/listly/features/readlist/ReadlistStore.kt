package ru.misterpotz.listly.features.readlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject

/** Команды readlist-фичи управляют подпиской на данные. */
sealed interface ReadlistCommand {
    data object StartObserving : ReadlistCommand
    data object StopObserving : ReadlistCommand
    data class AddReadlistFolder(val folder: ReadlistFolder) : ReadlistCommand
}

/** В текущей версии у readlist-фичи нет одноразовых side-effect'ов. */
sealed interface ReadlistEffect {}

/** События readlist-фичи: lifecycle от UI и внутренние обновления данных. */
sealed interface ReadlistEvent {
    object Ui {
        data object OnResume : ReadlistEvent
        data object OnPause : ReadlistEvent
        data class CreateFolder(val folder: ReadlistFolder) : ReadlistEvent
    }

    object Internal {
        data class DataLoaded(
            val mediaItems: List<MediaItem>,
            val folders: List<ReadlistFolder>
        ) : ReadlistEvent
    }
}

/** Actor readlist слушает поток элементов, находящихся в readlist. */
class ReadlistActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository
) : Actor<ReadlistCommand, ReadlistEvent>() {
    /** Запускает и останавливает поток наблюдения в ответ на команды Store. */
    override fun execute(command: ReadlistCommand): Flow<ReadlistEvent> {
        return when (command) {
            ReadlistCommand.StartObserving -> mediaItemRepository.getReadlistItems()
                .combine(mediaItemRepository.getReadlistFolders()) { mediaItems, folders ->
                    mediaItems to folders
                }
                .switch(ReadlistCommand.StartObserving)
                .mapEvents({ ReadlistEvent.Internal.DataLoaded(it.first, it.second) })

            ReadlistCommand.StopObserving ->
                cancelSwitchFlows(ReadlistCommand.StartObserving).mapEvents()

            is ReadlistCommand.AddReadlistFolder -> {
                mediaItemRepository.addReadlistFolder(command.folder)
                emptyFlow()
            }
        }
    }
}

/** Состояние readlist-экрана. */
data class ReadlistState(
    val mediaItems: Loadable<List<MediaItem>> = Loadable.Loading(),
    val readlistFolders: List<ReadlistFolder> = emptyList()
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
                copy(
                    mediaItems = event.mediaItems.toLoadable(),
                    readlistFolders = event.folders
                )
            }

            is ReadlistEvent.Ui.CreateFolder -> commands {
                +ReadlistCommand.AddReadlistFolder(event.folder)
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
