package ru.misterpotz.listly.domain.models

import kotlinx.serialization.Serializable

/**
 * Основная доменная модель медиапозиции.
 *
 * Это "истина" предметной области, с которой работают repository, interactor и Store.
 * UI при необходимости может строить поверх неё отдельную UI-модель.
 */
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

/**
 * Категория медиапозиции.
 *
 * Используется и в UI, и в доменной логике фильтрации readlist.
 */
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
