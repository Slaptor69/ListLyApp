package ru.misterpotz.listly.features.catalog

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.utils.Loadable
import ru.misterpotz.listly.utils.toLoadable
import javax.inject.Inject

/**
 * Команды каталога.
 *
 * В ELM команда - это инструкция Actor'у выполнить побочное действие.
 * Reducer сам не ходит за данными и ничего асинхронного не делает,
 * он только решает, какую Command нужно запустить.
 */
sealed interface CatalogCommand {
    data object ReloadData : CatalogCommand
    data class SetItemTracked(val mediaItemId: Int, val value: Boolean) : CatalogCommand
    data class SetInReadlist(val mediaItemId: Int, val value: Boolean) : CatalogCommand
}

/**
 * Одноразовые эффекты каталога.
 *
 * Effect не хранится в State, потому что навигация должна произойти один раз,
 * а не переисполняться на каждом новом рендере.
 */
sealed interface CatalogEffect {
    data class NavigateToItem(val mediaItemId: Int) : CatalogEffect
}

/**
 * Все события каталога.
 *
 * Полезно читать ELM именно отсюда:
 * - `Ui` события приходят от пользователя или lifecycle.
 * - `Internal` события возвращаются из Actor после работы с данными.
 */
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

/**
 * Actor каталога.
 *
 * Это "исполнитель" команд в ELM: он умеет делать асинхронную и внешнюю работу,
 * а результат обязательно возвращает обратно в Store новым Internal event.
 */
class CatalogActor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository,
    private val mediaItemInteractor: MediaItemInteractor
) : Actor<CatalogCommand, CatalogEvent>() {
    /**
     * Выполняет команду и превращает её результат в поток событий.
     *
     * Важно: Actor не меняет State напрямую.
     * Он только возвращает новые события, а итоговое изменение State всё равно делает Reducer.
     */
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

/**
 * Фабрика Store для каталога.
 *
 * Её роль - один раз собрать все части ELM-фичи вместе:
 * начальное состояние, reducer, actor и стартовое событие.
 */
class CatalogStoreFactory @Inject constructor(
    private val catalogActor: CatalogActor
) {
    /** Создаёт новый Store для одного экземпляра экрана каталога. */
    fun create(): ElmStore<CatalogEvent, CatalogState, CatalogEffect, CatalogCommand> {
        return ElmStore(
            initialState = CatalogState(),
            reducer = CatalogReducer,
            actor = catalogActor,
            startEvent = CatalogEvent.Init
        )
    }
}

/** Состояние каталога, которое экран читает и рисует. */
data class CatalogState(
    val items: Loadable<List<MediaItemUi>> = Loadable.Loading(),
    val itemToLoading: Map<Int, Boolean> = mapOf()
)

/**
 * UI-модель элемента каталога.
 *
 * Нужна, чтобы экран меньше зависел от доменной модели и мог хранить удобную для UI форму.
 */
data class MediaItemUi(
    val id: Int,
    val title: String,
    val type: MediaType,
    val tracked: Boolean = false,
    val inReadlist: Boolean = false,
)

/** Переводит доменную модель в формат, удобный для списка на экране. */
fun MediaItem.toMediaItemUi() = MediaItemUi(id, title, type, tracked, inReadlist)

/**
 * Reducer каталога.
 *
 * Это сердце ELM:
 * он получает каждый Event и решает, как поменять State,
 * какие Commands запустить и какие Effects выдать наружу.
 */
object CatalogReducer : StateReducer<CatalogEvent, CatalogState, CatalogEffect, CatalogCommand>() {
    /** Описывает реакцию фичи на каждое возможное событие. */
    override fun Result.reduce(event: CatalogEvent) {
        when (event) {
            CatalogEvent.Init -> Unit
            CatalogEvent.Ui.OnResume -> {
                commands {
                    // На resume просим Actor перезагрузить список.
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
                            // Пока идёт операция, помечаем конкретную карточку как "в загрузке".
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
