package com.example.media.model



import com.example.media.Catalog.dto.model.mediaStatus
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MediaItem (
    val id: String = ObjectId().toString(),
    val title:String,
    val description:String?,
    val mediaStatus: mediaStatus,
    val type: MediaType,
    val globalRating: Double,
    val createdAt: Long = System.currentTimeMillis()

)