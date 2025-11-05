package ru.misterpotz.demo.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: Int,
    val title: String,
    val tracked: Boolean = false,
    val inReadlist: Boolean = false,
    val imageUrl: String? = null,
    val annotation: String? = null
)

data class NewsItem(
    val id: Int,
    val text: String
)