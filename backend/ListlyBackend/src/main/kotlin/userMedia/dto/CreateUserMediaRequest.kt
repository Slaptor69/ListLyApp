package com.example.UserMedia.dto

import com.example.UserMedia.model.UserMediaStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserMediaRequest(
    val mediaId: String,
    val userMediaStatus: UserMediaStatus? = null,
    val userRating: Double? = null,
    val note : String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
