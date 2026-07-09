package com.example.media

import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem

class MediaCatalogService(
    private val mediaCatalogRepository: MediaCatalogRepository
) {


    fun findAllByTitle(title: String): List<MediaItem> {
        require(title.isNotBlank()) { "title must not be blank" }
        return mediaCatalogRepository.findAllByTitle(title.trim())
    }

    fun findById(mediaId: String): MediaItem? {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        return mediaCatalogRepository.findById(mediaId)
    }

    fun create(request: CreateMediaRequest): MediaItem {
        validateCreateRequest(request)

        val externalRef = request.externalRef
        request.externalRef?.let { ext ->
            val existing = mediaCatalogRepository.findByExternalRef(
                provider = ext.provider,
                externalId = ext.id
            )

            if (existing != null) {
                throw MediaAlreadyExistsException(
                    "Media with provider=${ext.provider} and externalId=${ext.id} already exists"
                )
            }
        }


        val mediaItem = MediaItem(
            title = request.title.trim(),
            description = request.description?.trim()?.takeIf { it.isNotBlank() },
            mediaType = request.mediaType,
            mediaStatus = request.mediaStatus,
            genres = request.genres.map { it.trim() }.filter { it.isNotBlank() },
            posterUrl = request.posterUrl?.trim()?.takeIf { it.isNotBlank() },
            externalRef = request.externalRef
        )

        mediaCatalogRepository.save(mediaItem)
        return mediaItem
    }

    fun updateByAdmin(id: String, request: UpdateMediaRequest) {
        require(id.isNotBlank()) { "id must not be blank" }

        mediaCatalogRepository.findById(id) ?: throw MediaNotFoundException()

        validateUpdateRequest(request)
        mediaCatalogRepository.update(id, request)
    }

    fun delete(mediaId: String) {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }

        mediaCatalogRepository.findById(mediaId) ?: throw MediaNotFoundException()
        mediaCatalogRepository.delete(mediaId)
    }

    private fun validateCreateRequest(request: CreateMediaRequest) {
        if (request.title.isBlank()) {
            throw InvalidMediaRequestException("Title must not be blank")
        }

        request.externalRef?.let {
            if (it.provider.isBlank()) {
                throw InvalidMediaRequestException("External provider must not be blank")
            }
            if (it.id.isBlank()) {
                throw InvalidMediaRequestException("External id must not be blank")
            }
        }
    }

    private fun validateUpdateRequest(request: UpdateMediaRequest) {
        request.title?.let {
            if (it.isBlank()) {
                throw InvalidMediaRequestException("Title must not be blank")
            }
        }

        request.externalRef?.let {
            if (it.provider.isBlank()) {
                throw InvalidMediaRequestException("External provider must not be blank")
            }
            if (it.id.isBlank()) {
                throw InvalidMediaRequestException("External id must not be blank")
            }
        }
    }

    fun adjustUserRating(mediaId: String, ratingDelta: Double, countDelta: Int) {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        mediaCatalogRepository.adjustUserRating(mediaId, ratingDelta, countDelta)
    }
}
