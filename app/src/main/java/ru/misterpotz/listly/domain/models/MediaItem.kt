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
    val id: String,
    val title: String,
    val type: MediaType,
    val inReadlist: Boolean = false,
    val userMediaId: String? = null,
    val readlistFolder: ReadlistFolder? = null,
    val readlistFolders: List<ReadlistFolder> = emptyList(),
    val readlistAddedAt: Long? = null,
    val collectionStatus: CollectionStatus? = null,
    val isFavourite: Boolean = false,
    val userRating: Int? = null,
    val userNote: String? = null,
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
    Movie("Фильмы"),
    Book("Книги"),
    Series("Сериалы"),
    Anime("Аниме"),
    Game("Игры");

    companion object {
        fun fromBackend(value: String?): MediaType {
            return when (value?.uppercase()) {
                "MOVIE" -> Movie
                "BOOK" -> Book
                "SERIES" -> Series
                "ANIME" -> Anime
                "GAME" -> Game
                else -> Movie
            }
        }

        fun toBackend(value: MediaType): String {
            return when (value) {
                Movie -> "MOVIE"
                Book -> "BOOK"
                Series -> "SERIES"
                Anime -> "ANIME"
                Game -> "GAME"
            }
        }
    }
}

/** Пользовательская папка внутри readlist. */
@Serializable
data class ReadlistFolder(
    val title: String,
    val id: String? = null
)

enum class CollectionStatus {
    Planned,
    Watching,
    Completed,
    Dropped;

    val title: String
        get() = when (this) {
            Planned -> "Запланировано"
            Watching -> "Просмотрено"
            Completed -> "Завершено"
            Dropped -> "Брошено"
        }

    companion object {
        fun fromBackend(value: String?): CollectionStatus {
            return when (value?.uppercase()) {
                "PLANNED" -> Planned
                "WATCHING", "IN_PROGRESS" -> Watching
                "COMPLETED" -> Completed
                "DROPPED" -> Dropped
                else -> Planned
            }
        }

        fun fromBackendOrNull(value: String?): CollectionStatus? {
            return when (value?.uppercase()) {
                "PLANNED" -> Planned
                "WATCHING", "IN_PROGRESS" -> Watching
                "COMPLETED" -> Completed
                "DROPPED" -> Dropped
                else -> null
            }
        }

        fun toBackend(value: CollectionStatus): String {
            return when (value) {
                Planned -> "PLANNED"
                Watching -> "IN_PROGRESS"
                Completed -> "COMPLETED"
                Dropped -> "DROPPED"
            }
        }
    }
}
