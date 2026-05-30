package ru.misterpotz.listly.features.readlist

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject

/** Команды readlist-фичи управляют подпиской на данные. */
sealed interface ReadlistCommand {
    data object Reload : ReadlistCommand
    data class AddReadlistFolder(val folder: ReadlistFolder) : ReadlistCommand
    data class SetFavourite(val mediaItemId: String, val isFavourite: Boolean) : ReadlistCommand
}

/** В текущей версии у readlist-фичи нет одноразовых side-effect'ов. */
sealed interface ReadlistEffect {}

/** События readlist-фичи: lifecycle от UI и внутренние обновления данных. */
sealed interface ReadlistEvent {
    object Ui {
        data object OnResume : ReadlistEvent
        data object OnPause : ReadlistEvent
        data class CreateFolder(val folder: ReadlistFolder) : ReadlistEvent
        data class ToggleFavourite(val mediaItem: MediaItem) : ReadlistEvent
    }

    object Internal {
        data class DataLoaded(
            val mediaItems: List<MediaItem>,
            val folders: List<ReadlistFolder>
        ) : ReadlistEvent
        data class ItemUpdated(val mediaItem: MediaItem) : ReadlistEvent
        data class LoadError(val throwable: Throwable? = null) : ReadlistEvent
    }
}

/** Actor readlist слушает поток элементов, находящихся в readlist. */
class ReadlistActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<ReadlistCommand, ReadlistEvent>() {
    /** Запускает и останавливает поток наблюдения в ответ на команды Store. */
    override fun execute(command: ReadlistCommand): Flow<ReadlistEvent> {
        return when (command) {
            ReadlistCommand.Reload -> flow {
                mediaItemRepository.refreshReadlist()
                emit(
                    ReadlistEvent.Internal.DataLoaded(
                        mediaItemRepository.getReadlistItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })

            is ReadlistCommand.AddReadlistFolder -> flow {
                mediaItemRepository.addReadlistFolder(command.folder)
                emit(
                    ReadlistEvent.Internal.DataLoaded(
                        mediaItemRepository.getReadlistItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })

            is ReadlistCommand.SetFavourite -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemFavourite(
                    command.mediaItemId,
                    command.isFavourite
                )
                emit(
                    ReadlistEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { ReadlistEvent.Internal.LoadError(it) })
        }
    }
}

/** Состояние readlist-экрана. */
data class ReadlistState(
    val mediaItems: Loadable<List<MediaItem>> = Loadable.Loading(),
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val itemToLoading: Map<String, Boolean> = emptyMap()
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
                    readlistFolders = event.folders,
                    itemToLoading = emptyMap()
                )
            }

            is ReadlistEvent.Internal.ItemUpdated -> state {
                copy(
                    mediaItems = mediaItems.requireContent().map {
                        if (it.id == event.mediaItem.id) {
                            event.mediaItem
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

            is ReadlistEvent.Internal.LoadError -> state {
                copy(
                    mediaItems = event.throwable.toLoadable(),
                    itemToLoading = emptyMap()
                )
            }

            is ReadlistEvent.Ui.CreateFolder -> commands {
                +ReadlistCommand.AddReadlistFolder(event.folder)
            }

            is ReadlistEvent.Ui.ToggleFavourite -> commands {
                +ReadlistCommand.SetFavourite(
                    event.mediaItem.id,
                    !event.mediaItem.isFavourite
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            ReadlistEvent.Ui.OnPause -> Unit

            ReadlistEvent.Ui.OnResume -> commands {
                +ReadlistCommand.Reload
            }
        }
    }
}
