package com.example.UserMedia.model

import com.example.media.model.MediaType
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class UserMediaItem (

    val id: String = ObjectId().toString(),
    val userId:String,
    val title:String,
    val mediaType: MediaType,
    val userMediaStatus: UserMediaStatus,
    val userRating: Double?,
    val note : String?,
    val favourite: Boolean?


    )