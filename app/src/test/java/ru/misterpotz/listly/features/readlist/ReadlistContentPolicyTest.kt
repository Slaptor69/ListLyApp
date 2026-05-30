package ru.misterpotz.listly.features.readlist

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder

class ReadlistContentPolicyTest {

    @Test
    fun `readlist type filter keeps only selected media type`() {
        val items = listOf(
            mediaItem(id = "movie", type = MediaType.Movie),
            mediaItem(id = "book", type = MediaType.Book),
            mediaItem(id = "anime", type = MediaType.Anime),
            mediaItem(id = "game", type = MediaType.Game)
        )

        val result = visibleReadlistItems(
            items = items,
            typeFilter = ReadlistFilter.ByType(MediaType.Game),
            folderFilter = ReadlistFolderFilter.All,
            sort = ReadlistSort.Alphabet
        )

        assertEquals(listOf("game"), result.map { it.id })
    }

    @Test
    fun `readlist folder filter combines with type filter`() {
        val folder = ReadlistFolder("Избранное", id = "favorites")
        val otherFolder = ReadlistFolder("Позже", id = "later")
        val items = listOf(
            mediaItem(id = "movie-in-folder", type = MediaType.Movie, folder = folder),
            mediaItem(id = "game-in-folder", type = MediaType.Game, folder = folder),
            mediaItem(id = "game-in-other-folder", type = MediaType.Game, folder = otherFolder),
            mediaItem(id = "game-without-folder", type = MediaType.Game)
        )

        val result = visibleReadlistItems(
            items = items,
            typeFilter = ReadlistFilter.ByType(MediaType.Game),
            folderFilter = ReadlistFolderFilter.ByFolder(folder),
            sort = ReadlistSort.Alphabet
        )

        assertEquals(listOf("game-in-folder"), result.map { it.id })
    }

    @Test
    fun `readlist folder filter keeps item when folder is one of many`() {
        val folder = ReadlistFolder("Избранное", id = "favorites")
        val otherFolder = ReadlistFolder("Позже", id = "later")
        val items = listOf(
            mediaItem(id = "multi-folder", folders = listOf(folder, otherFolder)),
            mediaItem(id = "other-folder", folders = listOf(otherFolder))
        )

        val result = visibleReadlistItems(
            items = items,
            typeFilter = ReadlistFilter.All,
            folderFilter = ReadlistFolderFilter.ByFolder(folder),
            sort = ReadlistSort.Alphabet
        )

        assertEquals(listOf("multi-folder"), result.map { it.id })
    }

    @Test
    fun `readlist alphabet sort orders by title ignoring case`() {
        val items = listOf(
            mediaItem(id = "third", title = "zeta"),
            mediaItem(id = "first", title = "Alpha"),
            mediaItem(id = "second", title = "beta")
        )

        val result = visibleReadlistItems(
            items = items,
            typeFilter = ReadlistFilter.All,
            folderFilter = ReadlistFolderFilter.All,
            sort = ReadlistSort.Alphabet
        )

        assertEquals(listOf("first", "second", "third"), result.map { it.id })
    }

    @Test
    fun `readlist added date sort puts newest items first and null dates last`() {
        val items = listOf(
            mediaItem(id = "old", title = "Old", addedAt = 10L),
            mediaItem(id = "new", title = "New", addedAt = 30L),
            mediaItem(id = "without-date", title = "Without date", addedAt = null),
            mediaItem(id = "same-date-alpha", title = "Alpha", addedAt = 20L),
            mediaItem(id = "same-date-beta", title = "Beta", addedAt = 20L)
        )

        val result = visibleReadlistItems(
            items = items,
            typeFilter = ReadlistFilter.All,
            folderFilter = ReadlistFolderFilter.All,
            sort = ReadlistSort.ByAddedDate
        )

        assertEquals(
            listOf("new", "same-date-alpha", "same-date-beta", "old", "without-date"),
            result.map { it.id }
        )
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

    private fun mediaItem(
        id: String,
        title: String = id,
        type: MediaType = MediaType.Movie,
        folder: ReadlistFolder? = null,
        folders: List<ReadlistFolder> = listOfNotNull(folder),
        addedAt: Long? = null
    ): MediaItem {
        return MediaItem(
            id = id,
            title = title,
            type = type,
            inReadlist = true,
            readlistFolder = folders.firstOrNull(),
            readlistFolders = folders,
            readlistAddedAt = addedAt
        )
    }
}
