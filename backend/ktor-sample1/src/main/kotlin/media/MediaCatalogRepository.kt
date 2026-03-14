package com.example.media

import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.model.UserMediaItem
import com.example.config.DatabaseConfig
import com.example.media.dto.UpdateMediaRequest
import com.example.media.model.MediaItem
import com.mongodb.client.model.Updates
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.save

class MediaCatalogRepository {

    val collection = DatabaseConfig.globalMediaItems()

    fun findAllByTitle(title: String):List<MediaItem>{
        return collection.find(MediaItem::title eq title).toList()
    }

    fun save(mediaItem: MediaItem){
        collection.insertOne(mediaItem)
    }

    fun findById(mediaId:String): MediaItem?{
        return collection.findOne(
            MediaItem::id eq mediaId
        )
    }

    fun update(
        mediaId: String,
        request: UpdateMediaRequest
    ) {
        val updates = mutableListOf<org.bson.conversions.Bson>()


        request.title?.let{
            updates.add(Updates.set("title",it))
        }

        request.description?.let { updates.add(Updates.set("description", it)) }
        request.posterUrl?.let { updates.add(Updates.set("posterUrl", it)) }
        request.genres?.let{
            updates.add(Updates.set("genres",it))
        }


        request.mediaStatus?.let {
            updates.add(Updates.set("mediaStatus", it))
        }
        request.externalRef?.let {
            updates.add(Updates.set("externalUrl", it))
        }

        if (updates.isEmpty()) return

        updates.add(
            Updates.set("updatedAt", System.currentTimeMillis())
        )

        collection.updateOne(
            and(
                MediaItem::id eq mediaId,
            ),
            Updates.combine(updates)
        )

    }




    fun delete(mediaId:String){
        collection.deleteOne(
            and(
                MediaItem::id eq mediaId,
            )
        )
    }
}