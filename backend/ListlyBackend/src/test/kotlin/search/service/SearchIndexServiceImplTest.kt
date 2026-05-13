package search.service

import com.example.media.Catalog.dto.model.MediaStatus
import com.example.media.MediaCatalogRepository
import com.example.media.model.MediaItem
import com.example.media.model.MediaType
import com.example.search.dto.model.SearchDocument
import com.example.search.repository.SearchIndexRepository
import com.example.search.service.SearchIndexServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchIndexServiceImplTest {
    private val mediaCatalogRepository = mockk<MediaCatalogRepository>()
    private val searchIndexRepository = mockk<SearchIndexRepository>(relaxed = true)
    private val service = SearchIndexServiceImpl(mediaCatalogRepository, searchIndexRepository)

    @Test
    fun `reindex clears index and indexes media in batches`() {
        val batch1 = listOf(mediaItem("m1"), mediaItem("m2"))
        val batch2 = listOf(mediaItem("m3"))

        every { mediaCatalogRepository.findPage(500, 0) } returns batch1
        every { mediaCatalogRepository.findPage(500, 2) } returns batch2
        every { mediaCatalogRepository.findPage(500, 3) } returns emptyList()

        service.reindex()

        verifyOrder {
            searchIndexRepository.clearIndex()
            searchIndexRepository.upsertDocuments(listOf(
                SearchDocument(id = "m1", title = "Title-m1"),
                SearchDocument(id = "m2", title = "Title-m2")
            ))
            searchIndexRepository.upsertDocuments(listOf(
                SearchDocument(id = "m3", title = "Title-m3")
            ))
        }
        verify(exactly = 1) { mediaCatalogRepository.findPage(500, 0) }
        verify(exactly = 1) { mediaCatalogRepository.findPage(500, 2) }
        verify(exactly = 1) { mediaCatalogRepository.findPage(500, 3) }
    }

    @Test
    fun `reindex clears index even when catalog is empty`() {
        every { mediaCatalogRepository.findPage(500, 0) } returns emptyList()

        service.reindex()

        verify(exactly = 1) { searchIndexRepository.clearIndex() }
        verify(exactly = 0) { searchIndexRepository.upsertDocuments(any()) }
    }

    @Test
    fun `indexMediaItem maps item to single search document`() {
        service.indexMediaItem(mediaItem("m42"))

        verify(exactly = 1) {
            searchIndexRepository.upsertDocument(SearchDocument(id = "m42", title = "Title-m42"))
        }
    }

    @Test
    fun `deleteFromIndex trims id and forwards to repository`() {
        service.deleteFromIndex("  m777  ")

        verify(exactly = 1) { searchIndexRepository.deleteDocument("m777") }
    }

    @Test
    fun `deleteFromIndex ignores blank id`() {
        service.deleteFromIndex("   ")
        verify(exactly = 0) { searchIndexRepository.deleteDocument(any()) }
    }

    private fun mediaItem(id: String): MediaItem = MediaItem(
        id = id,
        title = "Title-$id",
        mediaType = MediaType.MOVIE,
        mediaStatus = MediaStatus.FINISHED
    )
}
