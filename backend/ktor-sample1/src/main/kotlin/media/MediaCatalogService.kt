package com.example.media


import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem

class MediaCatalogService(private val mediaCatalogRepository: MediaCatalogRepository ) {
    fun findAllByTitle(title:String): List<MediaItem>{
        return mediaCatalogRepository.findAllByTitle(title)
    }

    fun findById(mediaId: String): MediaItem?{
        return mediaCatalogRepository.findById(mediaId);
    }

    fun create(mediaItem: MediaItem){
        val existing = findById(mediaItem.id)
        if (existing != null) throw MediaAlreadyExistsException(mediaItem.id)

        val safeItem = MediaItem(
            id = mediaItem.id,
            title = mediaItem.title,
            description = mediaItem.description,
            mediaType = mediaItem.mediaType,
            mediaStatus = mediaItem.mediaStatus,
            genres = mediaItem.genres,
            posterUrl = mediaItem.posterUrl,
            externalRef = mediaItem.externalRef,
            userRatingSum = mediaItem.userRatingSum,
            userRatingCount = mediaItem.userRatingCount,
            createdAt = mediaItem.createdAt,
            updatedAt = mediaItem.updatedAt,
        )
        mediaCatalogRepository.save(safeItem)

    }

    fun update(id:String,updateMediaRequest: UpdateMediaRequest) {


    }

    fun delete(){

    }

}

