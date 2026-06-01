package ru.misterpotz.listly.domain.models

import kotlinx.serialization.Serializable

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

@Serializable
enum class MediaType(val title: String) {
    Movie("Фильмы"),
    Series("Сериалы"),
    Anime("Аниме"),
    Game("Игры");

    companion object {
        fun fromBackend(value: String?): MediaType {
            return when (value?.uppercase()) {
                "MOVIE" -> Movie
                "SERIES" -> Series
                "ANIME" -> Anime
                "GAME" -> Game
                else -> Movie
            }
        }

        fun toBackend(value: MediaType): String {
            return when (value) {
                Movie -> "MOVIE"
                Series -> "SERIES"
                Anime -> "ANIME"
                Game -> "GAME"
            }
        }
    }
}

@Serializable
data class ReadlistFolder(
    val title: String,
    val id: String? = null
)

data class ReadlistQuery(
    val mediaType: MediaType? = null,
    val folders: List<ReadlistFolder> = emptyList(),
    val status: CollectionStatus? = null,
    val favouriteOnly: Boolean = false,
    val sort: ReadlistSortMode = ReadlistSortMode.ByAddedDate
)

enum class ReadlistSortMode(
    val title: String,
    val sortBy: String,
    val sortDir: String
) {
    Alphabet("По алфавиту", "title", "asc"),
    ByAddedDate("По дате добавления", "added_date", "desc")
}

enum class CollectionStatus {
    Planned,
    Watching,
    Completed,
    Dropped;

    val title: String
        get() = when (this) {
            Planned -> "Запланировано"
            Watching -> "В процессе"
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
