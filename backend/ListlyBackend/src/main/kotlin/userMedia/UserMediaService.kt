package com.example.UserMedia

import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaAlreadyExistsException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import com.example.media.MediaCatalogService
import com.example.media.MediaNotFoundException

class UserMediaService(
    private val userMediaRepository: UserMediaRepository,
    private val mediaCatalogService: MediaCatalogService,
) {

    fun getAllMediaItemsByUserId(userId: String): List<UserMediaItem> {
        return userMediaRepository.findAllByUser(userId)
    }

    fun getById(userId: String, userMediaId: String): UserMediaItem {
        return userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)
    }

    fun create(userId: String, request: CreateUserMediaRequest) {
        val item = UserMediaItem(
            userId = userId,
            mediaId = request.mediaId,
            userMediaStatus = request.userMediaStatus ?: UserMediaStatus.PLANNED,
            userRating = request.userRating,
            note = request.note
        )
        create(userId, item)
    }

    fun create(userId: String, item: UserMediaItem) {
        validateRating(item.userRating)
        validateNote(item.note)

        val mediaId = item.mediaId
        mediaCatalogService.findById(mediaId) ?: throw MediaNotFoundException()

        val existing = userMediaRepository.findByMediaIdAndUserId(userId, mediaId)
        if (existing != null) throw UserMediaAlreadyExistsException(userId, mediaId)

        val safeItem = UserMediaItem(
            id = item.id,
            userId = userId,
            mediaId = mediaId,
            userMediaStatus = item.userMediaStatus,
            userRating = item.userRating,
            note = item.note,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )

        userMediaRepository.save(safeItem)

        safeItem.userRating?.let { rating ->
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = rating, countDelta = 1)
        }
    }

    fun update(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaRequest
    ) {
        val existing = userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        validateRating(request.userRating)
        validateNote(request.note)

        userMediaRepository.update(userId, userMediaId, request)

        val newRating = request.userRating ?: return
        val oldRating = existing.userRating
        val mediaId = existing.mediaId

        if (oldRating == null) {
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = newRating, countDelta = 1)
            return
        }

        val delta = newRating - oldRating
        if (delta != 0.0) {
            mediaCatalogService.adjustUserRating(mediaId, ratingDelta = delta, countDelta = 0)
        }
    }

    fun delete(userId: String, userMediaId: String) {
        val existing = userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        userMediaRepository.delete(userId, userMediaId)

        existing.userRating?.let { rating ->
            mediaCatalogService.adjustUserRating(existing.mediaId, ratingDelta = -rating, countDelta = -1)
        }
    }

    private fun validateRating(rating: Double?) {
        if (rating != null && rating !in 0.0..10.0) {
            throw InvalidUserMediaRequestException("Your Rate must be from 0.0 to 10.0, not $rating")
        }
    }

    private fun validateNote(note: String?) {
        if (note != null && note.length > 400) {
            throw InvalidUserMediaRequestException(
                "The note must be less than 400 characters long. ${note.length} is too much"
            )
        }
    }
}
