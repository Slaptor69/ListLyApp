package ru.misterpotz.demo.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class MediaItem(
    val id: Int,
    val title: String,
    val type: MediaType,
    val tracked: Boolean = false,
    val inReadlist: Boolean = false,
    val imageUrl: String? = null,
    val annotation: String? = null
)
@Serializable
enum class MediaType(val title: String) {
    Anime("Аниме"),
    Manga("Манга"),
    Manhwa("Манхва"),
    Manhua("Маньхуа"),
    WebNovel("Веб-новеллы"),
    Book("Книги"),
    Series("Сериалы"),
    Games("Игры")
}

data class NewsItem(
    val id: Int,
    val text: String
)