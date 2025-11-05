package ru.misterpotz.demo.domain.interactors

import kotlinx.coroutines.delay
import ru.misterpotz.demo.domain.models.MediaItem
import ru.misterpotz.demo.domain.repositories.MediaItemRepository
import javax.inject.Inject

class MediaItemInteractor @Inject constructor(
    private val mediaItemRepository: MediaItemRepository
) {

    suspend fun setMediaItemTracked(mediaItemId: Int, tracked: Boolean): MediaItem? {
        delay(1000) // imitate long connection to server
        val mediaItem = mediaItemRepository.getMediaItem(mediaItemId)
        return mediaItem?.let {
            mediaItemRepository.updateMediaItem(
                mediaItem.copy(tracked = tracked)
            )
        }
    }

    suspend fun setMediaItemInReadlist(mediaItemId: Int, inReadlist: Boolean): MediaItem? {
        delay(900) // imitate long connection to server
        val mediaItem = mediaItemRepository.getMediaItem(mediaItemId)
        return mediaItem?.let {
            mediaItemRepository.updateMediaItem(
                mediaItem.copy(inReadlist = inReadlist)
            )
        }
    }
}