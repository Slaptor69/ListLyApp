package com.example.UserMedia

import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.model.UserMediaItem
import com.example.config.DatabaseConfig
import com.example.media.model.MediaType
import com.example.user.User
import com.mongodb.client.model.Updates
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

class UserMediaRepository {
    val collection = DatabaseConfig.userMediaItems


    fun findAllByUser( userId:String):List<UserMediaItem> {
        return collection.find(UserMediaItem::userId eq userId).toList()
    }

    fun findById(userId:String,userMediaId:String): UserMediaItem?{
        return collection.findOne(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::id eq userMediaId
            )
        )
    }

    fun save( userMediaItem: UserMediaItem){
        collection.insertOne(userMediaItem)
    }

    fun update(
        userId: String,
        userMediaId: String,
        request: UpdateUserMediaRequest
    ) {
        val updates = mutableListOf<org.bson.conversions.Bson>()

        request.userMediaStatus?.let {
            updates.add(Updates.set("userMediaStatus", it))
        }
        request.userRating?.let {
            updates.add(Updates.set("userRating", it))
        }
        request.note?.let {
            updates.add(Updates.set("note", it))
        }

        if (updates.isEmpty()) return

        updates.add(
            Updates.set("updatedAt", System.currentTimeMillis())
        )

        collection.updateOne(
            and(
                UserMediaItem::id eq userMediaId,
                UserMediaItem::userId eq userId
            ),
            Updates.combine(updates)
        )
    }

    fun delete(userId:String,userMediaId:String){
        collection.deleteOne(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::id eq userMediaId
            )
        )
    }

    fun findByItemAndUserId(userId: String, title: String, mediaType: MediaType): UserMediaItem? {
        return collection.findOne(
            UserMediaItem::userId eq userId,
            UserMediaItem::title eq title,
            UserMediaItem::mediaType eq mediaType
        )
    }


    fun findByUserAndCollection(userId:String, type: MediaType): List<UserMediaItem> =
        collection.find(
            and(
                UserMediaItem::userId eq userId,
                UserMediaItem::mediaType eq type

            )
        ).toList()



    }




