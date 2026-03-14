package com.example.media.Catalog.dto.model

import com.example.security.UserIdProvider
import kotlinx.serialization.Serializable

@Serializable
data class ExternalRef(val provider: String, val id:String,val url:String? = null) {

}