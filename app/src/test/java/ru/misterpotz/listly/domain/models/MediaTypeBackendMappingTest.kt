package ru.misterpotz.listly.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeBackendMappingTest {

    @Test
    fun `media type maps book enum from backend`() {
        assertEquals(MediaType.Book, MediaType.fromBackend("BOOK"))
    }

    @Test
    fun `media type maps book enum to backend`() {
        assertEquals("BOOK", MediaType.toBackend(MediaType.Book))
    }
}
