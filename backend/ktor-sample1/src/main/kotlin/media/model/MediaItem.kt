package com.example.media.model


import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class MediaItem (
    val id: String = ObjectId().toString(),
    val title:String,
    val type: MediaType,
    val status: MediaStatus,
    val rating:Int?,
    val note:String?,
    val createdAt: Long = System.currentTimeMillis()


)