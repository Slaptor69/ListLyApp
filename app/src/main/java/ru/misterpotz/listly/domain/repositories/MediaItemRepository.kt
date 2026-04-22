package ru.misterpotz.listly.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий медиапозиций.
 *
 * Это центральный источник данных для каталога, детали и readlist.
 * В ELM Actor обычно не знает, где именно лежат данные, и обращается именно сюда.
 */
@Singleton
class MediaItemRepository @Inject constructor() {
    // dumb in-memory runtime storage, not preserved across application reboots
    private val mediaItems = MutableStateFlow(DefaultMediaItems)

    /** Возвращает поток всех элементов каталога. */
    fun getItems(): Flow<List<MediaItem>> {
        return mediaItems
    }

    /** Возвращает только элементы, добавленные в readlist. */
    fun getReadlistItems(): Flow<List<MediaItem>> {
        return mediaItems.map { it.filter { it.inReadlist } }
    }

    /** Возвращает только элементы, помеченные как tracked. */
    fun getTrackedItems(): Flow<List<MediaItem>> {
        return mediaItems.map { it.filter { it.tracked } }
    }

    /** Ищет один элемент по id для детального экрана или точечного обновления. */
    fun getMediaItem(id: Int): MediaItem? {
        return mediaItems.value.find { it.id == id }
    }

    /**
     * Обновляет один элемент и публикует новое состояние потока.
     *
     * Именно поэтому наблюдающие Store автоматически получают новое внутреннее событие.
     */
    fun updateMediaItem(mediaItem: MediaItem): MediaItem? {
        val localMediaItems = mediaItems.value.toList()
        val newValues = localMediaItems.map {
            if (it.id == mediaItem.id) {
                mediaItem
            } else {
                it
            }
        }
        mediaItems.value = newValues
        return mediaItems.value.find { it.id == mediaItem.id }
    }
}

/** Набор стартовых данных для демо-приложения. */
private val DefaultMediaItems = listOf<MediaItem>(
    MediaItem(
        0,
        "Клинок, рассекающий демонов",
        type = MediaType.Anime,
        tracked = false,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx101922-WBsBl0ClmgYL.jpg",
        annotation = """It is the Taisho Period in Japan. Tanjiro, a kindhearted boy who sells charcoal for a living, finds his family slaughtered by a demon. To make matters worse, his younger sister Nezuko, the sole survivor, has been transformed into a demon herself.
 
 Though devastated by this grim reality, Tanjiro resolves to become a “demon slayer” so that he can turn his sister back into a human, and kill the demon that massacred his family."""
    ),
    MediaItem(
        1,
        "Подземелье вкусностей",
        type = MediaType.Anime,
        tracked = false,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx153518-IVXPDY5ph3kO.jpg",
        annotation = """Dungeons, dragons … and delicious monster stew!? Adventurers foray into a cursed buried kingdom to save their friend, cooking up a storm along the way.
"""
    ),
    MediaItem(
        2,
        "Фрирен, провожающая в последний путь",
        type = MediaType.Anime,
        tracked = true,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx154587-qQTzQnEJJ3oB.jpg",
        annotation = """"Frieren: Beyond Journey’s End is an anime adaptation of a manga series released in September 2023. This fantasy begins as heroes disband following a quest to defeat the Demon King. As decades pass, the elf mage Frieren attends a comrade’s funeral and learns new things about her old companions. This anime blends fantasy, reflection, and insightful character developments with themes of friendship and mortality.
 
 Across the 28-episode series, we follow Frieren, her apprentice Fern, and a warrior named Stark. As they travel through lands the hero party had fought to save, Frieren works to fulfill the last wishes of her former companions. Along their journey, Frieren begins to discover what it means to truly live and connect with others.
 
 In addition to Fern and Stark, flashbacks introduce Himmel, Heiter, Eisen, and others from Frieren’s past. Getting to know these characters deepens the story’s emotional weight, and contrasts Frieren’s elf life and the lives of her companions.
 """
    ),
    MediaItem(
        3,
        "Sabagebu",
        type = MediaType.Anime,
        tracked = false,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx20475-c4JrCtSDXrtM.png",
        annotation = """The story of Hidekichi Matsumoto's original Sabagebu! manga, which is running in Kodansha's Nakayoshi shojo magazine, revolves around Momoka Sonokawa, a girl who transferred into a certain girls' high school. The club she ended up joining at school was the "Sabagebu!," a club that conducts survival games.
"""
    ),
    MediaItem(
        4,
        "Akiba Maid Wars",
        type = MediaType.Anime,
        true,
        imageUrl = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx151379-JxxgTgSViXZL.png",
        annotation = """The innocent Nagomi Wahira has always admired the cute girls serving at maid cafes. Hoping to fulfill her dream of becoming one, she moves to Akihabara to work at the maid cafe Ton Tokoton.

Nagomi's first day seems completely normal—until she has to run an "errand" at a rival maid cafe along with her fellow recruit, the mature Ranko Mannen. There, things quickly go south, and Nagomi soon gets her first taste of Akihabara's violent maid wars. As she watches Ranko calmly battle her way through a horde of gun- and knife-wielding maids, Nagomi realizes that maid cafes are drastically unlike what she had envisioned.

While struggling to reconcile her expectations with the harsh reality she finds herself in, Nagomi searches for the enjoyment she once saw in the lives of maids.

"""
    )
)
