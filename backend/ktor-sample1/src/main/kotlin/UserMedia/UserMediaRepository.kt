package com.example.UserMedia

import com.example.UserMedia.model.UserMediaItem
import com.example.config.DatabaseConfig
import com.example.media.model.MediaItem
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class UserMediaRepository {
    val collection = DatabaseConfig.mediaItems


    fun findAllByUser( userId:String):List<MediaItem> {
        return collection.find(UserMediaItem::userId eq userId).toList()
    }

    fun findById(userId:String,mediaId:String): MediaItem?{
        if ((collection.findOne(UserMediaItem::userId eq userId)==null )) return null
        else {
            return collection.findOne(MediaItem::id eq mediaId)
        }
    }

    fun save( mediaItem: MediaItem){
        collection.insertOne(mediaItem)
    }

    fun update(userId:String,mediaItem: MediaItem){

    }

    fun delete(userId:String,mediaId:String){
        if ((collection.findOne(UserMediaItem::userId eq userId)!=null )) collection.deleteOne(MediaItem::id eq mediaId)
    }

}