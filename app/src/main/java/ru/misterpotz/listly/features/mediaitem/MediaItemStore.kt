package ru.misterpotz.listly.features.mediaitem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.MediaItem
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
    data class Load(val id: Int) : MediaItemCommand
    data class SetTracked(val mediaitem: MediaItem, val tracked: Boolean) : MediaItemCommand
    data class SetReadlist(val mediaitem: MediaItem, val readlist: Boolean) : MediaItemCommand
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
    data object ToggleTracked : MediaItemEvent
    data object ToggleReadlist : MediaItemEvent

    object Internal {
        data class Loaded(val mediaItem: MediaItem) : MediaItemEvent
    }
}

sealed interface MediaItemEffect {
    data object Close : MediaItemEffect
}

/** Состояние экрана детали. */
data class MediaItemState(
    val id: Int,
    val mediaItem: Loadable<MediaItem> = Loadable.Loading(),
    val readlist: Loadable<Unit> = Loadable.Content(Unit),
    val tracked: Loadable<Unit> = Loadable.Content(Unit)
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

            MediaItemEvent.ToggleReadlist -> commands {
                +MediaItemCommand.SetReadlist(
                    state.mediaItem.requireContent(),
                    state.mediaItem.content!!.inReadlist.not()
                )
                state {
                    copy(readlist = Loadable.Loading())
                }
            }

            MediaItemEvent.ToggleTracked -> commands {
                +MediaItemCommand.SetTracked(
                    state.mediaItem.requireContent(),
                    state.mediaItem.content!!.tracked.not()
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
                emit(loadItem(command.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })

            is MediaItemCommand.SetReadlist -> flow {
                mediaItemInteractor.setMediaItemInReadlist(
                    command.mediaitem.id,
                    command.readlist
                )
                emit(loadItem(command.mediaitem.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })

            is MediaItemCommand.SetTracked -> flow {
                mediaItemInteractor.setMediaItemTracked(
                    command.mediaitem.id,
                    command.tracked
                )
                emit(loadItem(command.mediaitem.id) ?: return@flow)
            }.mapEvents({ MediaItemEvent.Internal.Loaded(it) })
        }
    }

    /** Изолирует чтение одного элемента из repository. */
    private fun loadItem(id: Int): MediaItem? {
        return mediaItemRepository.getMediaItem(id)
    }
}


/** Собирает Store для одного детального экрана медиапозиции. */
class MediaItemStoreFactory @Inject constructor(
    private val mediaItemActor: MediaItemActor
) {
    /** Создаёт новый Store с id выбранного элемента в initial state. */
    fun create(mediaItemId: Int): ElmStore<MediaItemEvent, MediaItemState, MediaItemEffect, MediaItemCommand> {
        return ElmStore(
            initialState = MediaItemState(mediaItemId),
            reducer = MediaItemReducer,
            actor = mediaItemActor,
            startEvent = MediaItemEvent.Init
        )
    }
}
