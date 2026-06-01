package ru.misterpotz.listly.features.catalog

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder

class CatalogContentPolicyTest {

    @Test
    fun `catalog type filters contain media types from docs`() {
        assertEquals(
            listOf("Все", "Фильмы", "Сериалы", "Аниме", "Игры"),
            buildCatalogTypeFilters().map { it.title }
        )
    }

    @Test
    fun `catalog all type filter keeps every item`() {
        val items = listOf(
            mediaItem(id = "movie", type = MediaType.Movie),
            mediaItem(id = "series", type = MediaType.Series),
            mediaItem(id = "anime", type = MediaType.Anime),
            mediaItem(id = "game", type = MediaType.Game)
        )

        val result = filterCatalogItems(items, CatalogTypeFilter.All)

        assertEquals(
            listOf("movie", "series", "anime", "game"),
            result.map { it.id }
        )
    }

    @Test
    fun `catalog type filter keeps only selected media type`() {
        val items = listOf(
            mediaItem(id = "movie", type = MediaType.Movie),
            mediaItem(id = "anime", type = MediaType.Anime),
            mediaItem(id = "game", type = MediaType.Game)
        )

        val result = filterCatalogItems(items, CatalogTypeFilter.ByType(MediaType.Anime))

        assertEquals(listOf("anime"), result.map { it.id })
    }

    @Test
    fun `catalog folders ignore blank titles and duplicate titles`() {
        val result = buildReadlistFolders(
            listOf(
                ReadlistFolder("Хочу посмотреть", id = "1"),
                ReadlistFolder("  "),
                ReadlistFolder("хочу посмотреть", id = "2"),
                ReadlistFolder("Игры", id = "3")
            )
        )

        assertEquals(
            listOf(ReadlistFolder("Хочу посмотреть", id = "1"), ReadlistFolder("Игры", id = "3")),
            result
        )
    }

    @Test
    fun `catalog empty message distinguishes initial state from empty search result`() {
        assertEquals("Каталог пока пуст", catalogEmptyMessage("   "))
        assertEquals("Ничего не найдено", catalogEmptyMessage("missing title"))
    }

    private fun mediaItem(
        id: String,
        type: MediaType
    ): MediaItemUi {
        return MediaItemUi(
            id = id,
            title = id,
            type = type
        )
    }
}
