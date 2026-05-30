package ru.misterpotz.listly.features.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.listly.domain.interactors.MediaItemInteractor
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
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
    data class Search(val query: String) : CatalogCommand
    data class AddReadlistFolder(val folder: ReadlistFolder) : CatalogCommand
    data class SetReadlistFolder(
        val mediaItemId: String,
        val folders: List<ReadlistFolder>
    ) : CatalogCommand
    data class SetCollectionStatus(
        val mediaItemId: String,
        val status: CollectionStatus?
    ) : CatalogCommand
    data class SetListState(
        val mediaItemId: String,
        val status: CollectionStatus,
        val folders: List<ReadlistFolder>
    ) : CatalogCommand
    data class SetFavourite(val mediaItemId: String, val isFavourite: Boolean) : CatalogCommand
    data class RemoveFromReadlist(val mediaItemId: String) : CatalogCommand
}

/**
 * Одноразовые эффекты каталога.
 *
 * Effect не хранится в State, потому что навигация должна произойти один раз,
 * а не переисполняться на каждом новом рендере.
 */
sealed interface CatalogEffect {
    data class NavigateToItem(val mediaItemId: String) : CatalogEffect
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
        data class SearchChanged(val query: String) : CatalogEvent
        data class AddToReadingList(
            val mediaItem: MediaItemUi,
            val folders: List<ReadlistFolder>
        ) : CatalogEvent
        data class SelectCollectionStatus(
            val mediaItem: MediaItemUi,
            val status: CollectionStatus?
        ) : CatalogEvent
        data class SaveListState(
            val mediaItem: MediaItemUi,
            val status: CollectionStatus,
            val folders: List<ReadlistFolder>
        ) : CatalogEvent
        data class CreateFolder(val folder: ReadlistFolder) : CatalogEvent
        data class RemoveFromReadingList(val mediaItem: MediaItemUi) : CatalogEvent
        data class ToggleFavourite(val mediaItem: MediaItemUi) : CatalogEvent
        data class ClickItem(val mediaItem: MediaItemUi) : CatalogEvent
    }

    object Internal {
        data class LoadError(val throwable: Throwable? = null) : CatalogEvent
        data class LoadedData(
            val items: List<MediaItem>,
            val folders: List<ReadlistFolder>
        ) : CatalogEvent
        data class FoldersLoaded(val folders: List<ReadlistFolder>) : CatalogEvent
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
            is CatalogCommand.Search -> flow {
                mediaItemRepository.searchCatalog(command.query)
                emit(
                    CatalogEvent.Internal.LoadedData(
                        mediaItemRepository.getItems().first(),
                        mediaItemRepository.getReadlistFolders().first()
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.AddReadlistFolder -> flow {
                emit(
                    CatalogEvent.Internal.FoldersLoaded(
                        mediaItemRepository.addReadlistFolder(command.folder)
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.SetReadlistFolder -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemReadlistFolders(
                    command.mediaItemId,
                    command.folders
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.SetCollectionStatus -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemStatus(
                    command.mediaItemId,
                    command.status
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.SetListState -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemListState(
                    command.mediaItemId,
                    command.status,
                    command.folders
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.RemoveFromReadlist -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemInReadlist(command.mediaItemId, false)
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })

            is CatalogCommand.SetFavourite -> flow {
                val updatedItem = mediaItemInteractor.setMediaItemFavourite(
                    command.mediaItemId,
                    command.isFavourite
                )
                emit(
                    CatalogEvent.Internal.ItemUpdated(
                        updatedItem ?: return@flow
                    )
                )
            }.mapEvents({ it }, { CatalogEvent.Internal.LoadError(it) })
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
    val searchQuery: String = "",
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val itemToLoading: Map<String, Boolean> = mapOf()
)

/**
 * UI-модель элемента каталога.
 *
 * Нужна, чтобы экран меньше зависел от доменной модели и мог хранить удобную для UI форму.
 */
data class MediaItemUi(
    val id: String,
    val title: String,
    val type: MediaType,
    val inReadlist: Boolean = false,
    val readlistFolder: ReadlistFolder? = null,
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val readlistAddedAt: Long? = null,
    val collectionStatus: CollectionStatus? = null,
    val isFavourite: Boolean = false,
    val userRating: Int? = null,
    val userNote: String? = null,
    val imageUrl: String? = null,
)

/** Переводит доменную модель в формат, удобный для списка на экране. */
fun MediaItem.toMediaItemUi() = MediaItemUi(
    id = id,
    title = title,
    type = type,
    inReadlist = inReadlist,
    readlistFolder = readlistFolder,
    readlistFolders = readlistFolders,
    readlistAddedAt = readlistAddedAt,
    collectionStatus = collectionStatus,
    isFavourite = isFavourite,
    userRating = userRating,
    userNote = userNote,
    imageUrl = imageUrl
)

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
                    +CatalogCommand.Search(state.searchQuery)
                }
            }

            is CatalogEvent.Ui.SearchChanged -> {
                state {
                    copy(
                        searchQuery = event.query,
                        items = Loadable.Loading(items.content)
                    )
                }
                commands {
                    +CatalogCommand.Search(event.query)
                }
            }

            is CatalogEvent.Ui.ClickItem -> {
                effects {
                    +CatalogEffect.NavigateToItem(event.mediaItem.id)
                }
            }

            is CatalogEvent.Ui.AddToReadingList -> commands {
                +CatalogCommand.SetReadlistFolder(
                    event.mediaItem.id,
                    event.folders
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

            is CatalogEvent.Ui.SelectCollectionStatus -> commands {
                +CatalogCommand.SetCollectionStatus(
                    event.mediaItem.id,
                    event.status
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            is CatalogEvent.Ui.SaveListState -> commands {
                +CatalogCommand.SetListState(
                    event.mediaItem.id,
                    event.status,
                    event.folders
                )
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            is CatalogEvent.Ui.CreateFolder -> commands {
                +CatalogCommand.AddReadlistFolder(event.folder)
            }

            is CatalogEvent.Ui.RemoveFromReadingList -> commands {
                +CatalogCommand.RemoveFromReadlist(event.mediaItem.id)
                state {
                    copy(
                        itemToLoading = itemToLoading.toMutableMap().apply {
                            put(event.mediaItem.id, true)
                        }
                    )
                }
            }

            is CatalogEvent.Ui.ToggleFavourite -> commands {
                +CatalogCommand.SetFavourite(
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
                    readlistFolders = event.folders,
                )
            }

            is CatalogEvent.Internal.FoldersLoaded -> state {
                copy(readlistFolders = event.folders)
            }

            is CatalogEvent.Internal.LoadError -> state {
                copy(
                    items = event.throwable.toLoadable(),
                    itemToLoading = emptyMap()
                )
            }
        }
    }
}
