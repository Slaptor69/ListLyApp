package com.example.UserMedia.dto

import com.example.UserMedia.model.UserMediaStatus
import com.example.media.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
class CreateUserMediaRequest(
    //ссылка на глобальную медию(нужна?)
    // val mediaId:String

    val title:String,
    val mediaType: MediaType,
    val userMediaStatus: UserMediaStatus,
    val userRating: Double? = null,
    val note : String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

