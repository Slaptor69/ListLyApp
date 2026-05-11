package ru.misterpotz.listly.domain.interactors

import kotlinx.coroutines.delay
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.models.ReadlistFolders
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
     * Переключает флаг отслеживания.
     *
     * Для ELM это типичная "бизнес-операция", которую Actor вызывает в ответ на Command.
     */
    suspend fun setMediaItemTracked(mediaItemId: Int, tracked: Boolean): MediaItem? {
        delay(1000) // imitate long connection to server
        val mediaItem = mediaItemRepository.getMediaItem(mediaItemId)
        return mediaItem?.let {
            mediaItemRepository.updateMediaItem(
                mediaItem.copy(tracked = tracked)
            )
        }
    }

    /**
     * Переключает попадание элемента в readlist.
     *
     * С точки зрения общего потока это ещё одна асинхронная операция,
     * после которой Actor обычно отправляет назад внутреннее событие с новым состоянием данных.
     */
    suspend fun setMediaItemInReadlist(mediaItemId: Int, inReadlist: Boolean): MediaItem? {
        delay(900) // imitate long connection to server
        val mediaItem = mediaItemRepository.getMediaItem(mediaItemId)
        return mediaItem?.let {
            mediaItemRepository.updateMediaItem(
                mediaItem.copy(
                    inReadlist = inReadlist,
                    readlistFolder = if (inReadlist) {
                        mediaItem.readlistFolder ?: ReadlistFolders.InProgress
                    } else {
                        null
                    }
                )
            )
        }
    }

    /** Добавляет элемент в readlist и сохраняет выбранную папку. */
    suspend fun setMediaItemReadlistFolder(
        mediaItemId: Int,
        folder: ReadlistFolder
    ): MediaItem? {
        delay(900) // imitate long connection to server
        val mediaItem = mediaItemRepository.getMediaItem(mediaItemId)
        return mediaItem?.let {
            mediaItemRepository.addReadlistFolder(folder)
            mediaItemRepository.updateMediaItem(
                mediaItem.copy(
                    inReadlist = true,
                    readlistFolder = folder
                )
            )
        }
    }
}
