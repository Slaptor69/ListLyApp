package com.example.UserMedia.model

import com.example.media.model.MediaType
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class UserMediaItem (

    val id: String = ObjectId().toString(),
    val userId: String,
    //ссылка на глобальную медию(нужна?)
    //val mediaId: String
    val title: String,
    val mediaType: MediaType,
    val userMediaStatus: UserMediaStatus,
    val userRating: Double? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()



    )