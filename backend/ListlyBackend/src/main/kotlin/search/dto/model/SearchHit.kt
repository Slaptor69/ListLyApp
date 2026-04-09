package com.example.search.dto.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer

@Serializable
class SearchHit(
    val id: String,
    val title: String,
    val description: String? = null,
    val mediaType: String,
    val genres: List<String> = emptyList()
) {

}