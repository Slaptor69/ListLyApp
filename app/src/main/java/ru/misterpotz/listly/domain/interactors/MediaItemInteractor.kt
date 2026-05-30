package ru.misterpotz.listly.domain.interactors

import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import javax.inject.Inject

/**
 * Interactor для бизнес-операций над медиапозицией.
 *
 * В общей структуре это слой между Actor и Repository.
 * Actor знает "какую бизнес-операцию надо сделать", а interactor инкапсулирует её шаги.
 */
class MediaItemInteractor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository
) {

    /**
     * Переключает попадание элемента в readlist.
     *
     * С точки зрения общего потока это ещё одна асинхронная операция,
     * после которой Actor обычно отправляет назад внутреннее событие с новым состоянием данных.
     */
    suspend fun setMediaItemInReadlist(mediaItemId: String, inReadlist: Boolean): MediaItem? {
        return if (inReadlist) {
            mediaItemRepository.addMediaToReadlist(mediaItemId, null, CollectionStatus.Planned)
        } else {
            mediaItemRepository.removeMediaFromReadlist(mediaItemId)
        }
    }

    /** Добавляет элемент в readlist и сохраняет выбранную папку. */
    suspend fun setMediaItemReadlistFolder(
        mediaItemId: String,
        folder: ReadlistFolder
    ): MediaItem? {
        return setMediaItemReadlistFolders(mediaItemId, listOf(folder))
    }

    suspend fun setMediaItemReadlistFolders(
        mediaItemId: String,
        folders: List<ReadlistFolder>
    ): MediaItem? {
        val backendFolders = folders.map { folder ->
            if (folder.id == null) {
                mediaItemRepository.addReadlistFolder(folder)
                    .firstOrNull { it.title.equals(folder.title, ignoreCase = true) }
                    ?: folder
            } else {
                folder
            }
        }.distinctBy { it.id ?: it.title.trim().lowercase() }
        return mediaItemRepository.updateReadlistFolders(mediaItemId, backendFolders)
    }

    suspend fun setMediaItemListState(
        mediaItemId: String,
        status: CollectionStatus,
        folders: List<ReadlistFolder>
    ): MediaItem? {
        val backendFolders = folders.map { folder ->
            if (folder.id == null) {
                mediaItemRepository.addReadlistFolder(folder)
                    .firstOrNull { it.title.equals(folder.title, ignoreCase = true) }
                    ?: folder
            } else {
                folder
            }
        }.distinctBy { it.id ?: it.title.trim().lowercase() }
        return mediaItemRepository.updateUserMediaListState(mediaItemId, status, backendFolders)
    }

    suspend fun setMediaItemNote(mediaItemId: String, note: String): MediaItem? {
        return mediaItemRepository.updateUserNote(mediaItemId, note)
    }

    suspend fun setMediaItemStatus(mediaItemId: String, status: CollectionStatus?): MediaItem? {
        return mediaItemRepository.updateCollectionStatus(mediaItemId, status)
    }

    suspend fun setMediaItemFavourite(mediaItemId: String, isFavourite: Boolean): MediaItem? {
        return mediaItemRepository.updateFavourite(mediaItemId, isFavourite)
    }

    suspend fun setMediaItemRating(mediaItemId: String, rating: Int): MediaItem? {
        return mediaItemRepository.updateUserRating(mediaItemId, rating)
    }
}
