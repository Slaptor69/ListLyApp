package com.example.UserMedia.dto

import com.example.UserMedia.model.UserMediaItem
import kotlinx.serialization.Serializable

@Serializable
data class UserMediaResponse(
    val id: String,
    //ссылка на глобальную медию(нужна?)
    //val mediaId: String,
    val title: String,
    val mediaType: String,
    val userMediaStatus: String?,
    val userRating: Double?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun UserMediaItem.toResponse() = UserMediaResponse(
    id = id,
    title = title,
    mediaType = mediaType.name,
    userMediaStatus = userMediaStatus.name,
    userRating = userRating,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
