package com.example.UserMedia.model

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class UserMediaItem (

    val id: String = ObjectId().toString(),
    val userId: String,
    val mediaId: String,
    val userMediaStatus: UserMediaStatus,
    val userRating: Double? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
