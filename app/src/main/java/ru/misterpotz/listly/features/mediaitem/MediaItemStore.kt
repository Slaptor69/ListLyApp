package ru.misterpotz.listly.features.mediaitem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject

/**
 * Заготовка под namespace/расширение фичи.
 *
 * Сейчас класс не несёт логики, но файл содержит весь ELM-набор детального экрана.
 */
class MediaItemStore {
}

/** Команды детального экрана, которые должен выполнить Actor. */
sealed interface MediaItemCommand {
    data class Load(val id: String) : MediaItemCommand
    data class AddReadlistFolder(val mediaItemId: String, val folder: ReadlistFolder) : MediaItemCommand
    data class SetReadlistFolders(val mediaItem: MediaItem, val folders: List<ReadlistFolder>) : MediaItemCommand
    data class SetCollectionStatus(val mediaItem: MediaItem, val status: CollectionStatus) : MediaItemCommand
    data class SetListState(
        val mediaItem: MediaItem,
        val status: CollectionStatus,
        val folders: List<ReadlistFolder>
    ) : MediaItemCommand
    data class SetFavourite(val mediaItem: MediaItem, val isFavourite: Boolean) : MediaItemCommand
    data class SetUserRating(val mediaItem: MediaItem, val rating: Int) : MediaItemCommand
    data class SaveUserNote(val mediaItem: MediaItem, val note: String) : MediaItemCommand
    data class RemoveFromReadlist(val mediaItem: MediaItem) : MediaItemCommand
}

/**
 * События детального экрана.
 *
 * Для чтения ELM это выглядит так:
 * UI нажал кнопку -> пришёл Toggle* event ->
 * reducer выдал command -> actor сделал работу ->
 * actor вернул Internal.Loaded -> reducer обновил State.
 */
sealed interface MediaItemEvent {
    data object Init : MediaItemEvent
    data object Close : MediaItemEvent
    data class SelectReadlistFolders(val folders: List<ReadlistFolder>) : MediaItemEvent
    data class SelectCollectionStatus(val status: CollectionStatus) : MediaItemEvent
    data class SaveListState(
        val status: CollectionStatus,
        val folders: List<ReadlistFolder>
    ) : MediaItemEvent
    data class CreateFolder(val folder: ReadlistFolder) : MediaItemEvent
    data object ToggleFavourite : MediaItemEvent
    data class SelectUserRating(val rating: Int) : MediaItemEvent
    data class SaveUserNote(val note: String) : MediaItemEvent
    data object RemoveFromReadlist : MediaItemEvent

    object Internal {
        data class Loaded(
            val mediaItem: MediaItem,
            val folders: List<ReadlistFolder>
        ) : MediaItemEvent
        data class LoadError(val throwable: Throwable? = null) : MediaItemEvent
    }
}

sealed interface MediaItemEffect {
    data object Close : MediaItemEffect
}

/** Состояние экрана детали. */
data class MediaItemState(
    val id: String,
    val mediaItem: Loadable<MediaItem> = Loadable.Loading(),
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val readlist: Loadable<Unit> = Loadable.Content(Unit),
)

/** Reducer детального экрана управляет локальным состоянием загрузки и навигацией назад. */
object MediaItemReducer :
    StateReducer<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand>() {
    /** Реакция Store на события детального экрана. */
    override fun Result.reduce(event: MediaItemEvent) {
        when (event) {
            MediaItemEvent.Init -> commands {
                +MediaItemCommand.Load(state.id)
            }

            MediaItemEvent.Close -> effects {
                +MediaItemEffect.Close
            }

            is MediaItemEvent.SelectReadlistFolders -> commands {
                +MediaItemCommand.SetReadlistFolders(
                    state.mediaItem.requireContent(),
                    event.folders
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.SelectCollectionStatus -> commands {
                +MediaItemCommand.SetCollectionStatus(
                    state.mediaItem.requireContent(),
                    event.status
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.SaveListState -> commands {
                +MediaItemCommand.SetListState(
                    state.mediaItem.requireContent(),
                    event.status,
                    event.folders
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.CreateFolder -> commands {
                +MediaItemCommand.AddReadlistFolder(state.id, event.folder)
            }

            MediaItemEvent.ToggleFavourite -> commands {
                val mediaItem = state.mediaItem.requireContent()
                +MediaItemCommand.SetFavourite(
                    mediaItem,
                    !mediaItem.isFavourite
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.SelectUserRating -> commands {
                +MediaItemCommand.SetUserRating(
                    state.mediaItem.requireContent(),
                    event.rating
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.SaveUserNote -> commands {
                +MediaItemCommand.SaveUserNote(
                    state.mediaItem.requireContent(),
                    event.note
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            MediaItemEvent.RemoveFromReadlist -> commands {
                +MediaItemCommand.RemoveFromReadlist(state.mediaItem.requireContent())
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            is MediaItemEvent.Internal.Loaded -> state {
                copy(
                    mediaItem = event.mediaItem.toLoadable(),
                    readlistFolders = event.folders,
                    readlist = Loadable.Content(Unit)
                )
            }

            is MediaItemEvent.Internal.LoadError -> state {
                copy(readlist = event.throwable.toLoadable())
            }
        }
    }
}

/**
 * Actor детального экрана.
 *
 * Здесь выполняются побочные действия: загрузка элемента и обновление его флагов.
 * После каждой операции Actor снова загружает актуальную версию объекта
 * и возвращает её в Store как Internal event.
 */
class MediaItemActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<MediaItemCommand, MediaItemEvent>() {
    /** Исполняет команды Store и публикует результат как поток событий. */
    override fun execute(command: MediaItemCommand): Flow<MediaItemEvent> {
        return when (command) {
            is MediaItemCommand.Load -> flow {
                emit(loadedEvent(command.id) ?: return@flow)
            }.mapEvents({ it })

            is MediaItemCommand.AddReadlistFolder -> flow {
                mediaItemRepository.addReadlistFolder(command.folder)
                emit(loadedEvent(command.mediaItemId) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SetReadlistFolders -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemReadlistFolders(
                    command.mediaItem.id,
                    command.folders
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SetCollectionStatus -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemStatus(
                    command.mediaItem.id,
                    command.status
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SetListState -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemListState(
                    command.mediaItem.id,
                    command.status,
                    command.folders
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SetFavourite -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemFavourite(
                    command.mediaItem.id,
                    command.isFavourite
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SetUserRating -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemRating(
                    command.mediaItem.id,
                    command.rating
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.SaveUserNote -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemNote(
                    command.mediaItem.id,
                    command.note
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )

            is MediaItemCommand.RemoveFromReadlist -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemInReadlist(
                    command.mediaItem.id,
                    false
                )
                emit(loadedEvent(updatedItem?.id ?: command.mediaItem.id) ?: return@flow)
            }.mapEvents(
                { it },
                { MediaItemEvent.Internal.LoadError(it) }
            )
        }
    }

    /** Изолирует чтение данных детального экрана из repository. */
    private suspend fun loadedEvent(id: String): MediaItemEvent.Internal.Loaded? {
        val item = mediaItemRepository.getMediaItem(id) ?: return null
        return MediaItemEvent.Internal.Loaded(
            mediaItem = item,
            folders = mediaItemRepository.getReadlistFolders().first()
        )
    }
}


/** Собирает Store для одного детального экрана медиапозиции. */
class MediaItemStoreFactory @Inject constructor(
    private val mediaItemActor: MediaItemActor
) {
    /** Создаёт новый Store с id выбранного элемента в initial state. */
    fun create(mediaItemId: String): ElmStore<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand> {
        return ElmStore(
            initialState = MediaItemState(mediaItemId),
            reducer = MediaItemReducer,
            actor = mediaItemActor,
            startEvent = MediaItemEvent.Init
        )
    }
}
