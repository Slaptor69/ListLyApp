package com.example.media.dto

import com.example.UserMedia.model.UserMediaStatus
import com.example.media.model.MediaType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMediaRequest(
    val title:String,
    val type: MediaType,
    val status: UserMediaStatus,
    val rating: Double? = null,
    val note:String? = null

) {
}