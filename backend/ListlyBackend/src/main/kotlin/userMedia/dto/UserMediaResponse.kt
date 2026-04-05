package com.example.UserMedia.dto

import com.example.UserMedia.model.UserMediaItem
import kotlinx.serialization.Serializable

@Serializable
data class UserMediaResponse(
    val id: String,
    val mediaId: String,
    val userMediaStatus: String?,
    val userRating: Double?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun UserMediaItem.toResponse() = UserMediaResponse(
    id = id,
    mediaId = mediaId,
    userMediaStatus = userMediaStatus.name,
    userRating = userRating,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
