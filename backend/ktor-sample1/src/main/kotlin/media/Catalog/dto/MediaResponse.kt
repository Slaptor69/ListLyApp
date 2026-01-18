package com.example.media.dto

import com.example.UserMedia.model.UserMediaStatus
import com.example.media.model.MediaType
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MediaResponse(val id: String = ObjectId().toString(),
                         val title:String,
                         val type: MediaType,
                         val status: UserMediaStatus,
                         val rating: Double? = null,
                         val note:String? = null,
                         val createdAt: Long) {
}