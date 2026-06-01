package ru.misterpotz.listly.features.readlist

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import ru.misterpotz.listly.domain.models.ReadlistQuery
import ru.misterpotz.listly.domain.models.ReadlistSortMode

class ReadlistContentPolicyTest {

    @Test
    fun `readlist type filters contain all media types from docs`() {
        assertEquals(
            listOf("Все", "Фильмы", "Сериалы", "Аниме", "Игры"),
            buildReadlistTypeFilters().map { it.title }
        )
    }

    @Test
    fun `readlist type filter maps selected type to backend query value`() {
        assertEquals(MediaType.Game, ReadlistFilter.ByType(MediaType.Game).mediaTypeOrNull())
        assertEquals(null, ReadlistFilter.All.mediaTypeOrNull())
    }

    @Test
    fun `readlist folder filter maps selected folder to backend query value`() {
        val folder = ReadlistFolder("Избранное", id = "favorites")
        val otherFolder = ReadlistFolder("Позже", id = "later")

        assertEquals(listOf(folder), ReadlistFolderFilter.ByFolder(folder).folders())
        assertEquals(listOf(folder, otherFolder), ReadlistFolderFilter.Selected(listOf(folder, otherFolder)).folders())
        assertEquals(emptyList<ReadlistFolder>(), ReadlistFolderFilter.All.folders())
    }

    @Test
    fun `readlist selected folder filter removes repeated folders`() {
        val folder = ReadlistFolder("Избранное", id = "favorites")

        val result = ReadlistFolderFilter.Selected(
            listOf(
                folder,
                ReadlistFolder("Другое название", id = "favorites")
            )
        )

        assertEquals("Избранное", result.title)
        assertEquals(listOf(folder), result.folders())
    }

    @Test
    fun `readlist status filters contain four statuses from api contract`() {
        assertEquals(
            listOf("Все", "Запланировано", "В процессе", "Завершено", "Брошено"),
            buildReadlistStatusFilters().map { it.title }
        )
    }

    @Test
    fun `readlist status filter maps selected status to backend query value`() {
        assertEquals(CollectionStatus.Watching, ReadlistStatusFilter.ByStatus(CollectionStatus.Watching).statusOrNull())
        assertEquals(null, ReadlistStatusFilter.All.statusOrNull())
    }

    @Test
    fun `readlist sort options map to backend query values`() {
        assertEquals("title", ReadlistSortMode.Alphabet.sortBy)
        assertEquals("asc", ReadlistSortMode.Alphabet.sortDir)
        assertEquals("added_date", ReadlistSortMode.ByAddedDate.sortBy)
        assertEquals("desc", ReadlistSortMode.ByAddedDate.sortDir)
    }

    @Test
    fun `readlist query keeps backend filtering and sorting options`() {
        val folders = listOf(
            ReadlistFolder("Аниме", id = "anime"),
            ReadlistFolder("Игры", id = "games")
        )

        val query = ReadlistQuery(
            mediaType = MediaType.Anime,
            folders = folders,
            status = CollectionStatus.Watching,
            favouriteOnly = true,
            sort = ReadlistSortMode.Alphabet
        )

        assertEquals(MediaType.Anime, query.mediaType)
        assertEquals(folders, query.folders)
        assertEquals(CollectionStatus.Watching, query.status)
        assertEquals(true, query.favouriteOnly)
        assertEquals(ReadlistSortMode.Alphabet, query.sort)
    }

    @Test
    fun `readlist folders ignore blank titles and duplicate titles`() {
        val result = buildReadlistFolders(
            listOf(
                ReadlistFolder("Смотреть дальше", id = "1"),
                ReadlistFolder(""),
                ReadlistFolder("смотреть дальше", id = "2"),
                ReadlistFolder("Игры", id = "3")
            )
        )

        assertEquals(
            listOf(ReadlistFolder("Смотреть дальше", id = "1"), ReadlistFolder("Игры", id = "3")),
            result
        )
    }
}
