package com.example.UserMedia.dto

import com.example.UserMedia.model.UserMediaStatus


import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserMediaRequest(
    val userMediaStatus: UserMediaStatus? = null,
    val userRating : Double? = null,
    val note : String? = null,

)