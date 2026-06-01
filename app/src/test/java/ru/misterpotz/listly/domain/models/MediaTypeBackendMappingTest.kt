package ru.misterpotz.listly.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaTypeBackendMappingTest {

    @Test
    fun `backend media types map to app enum values`() {
        assertEquals(MediaType.Movie, MediaType.fromBackend("MOVIE"))
        assertEquals(MediaType.Series, MediaType.fromBackend("SERIES"))
        assertEquals(MediaType.Anime, MediaType.fromBackend("ANIME"))
        assertEquals(MediaType.Game, MediaType.fromBackend("GAME"))
    }

    @Test
    fun `unknown backend media type falls back to movie`() {
        assertEquals(MediaType.Movie, MediaType.fromBackend("UNKNOWN"))
    }

    @Test
    fun `media types map to backend values from api contract`() {
        assertEquals("MOVIE", MediaType.toBackend(MediaType.Movie))
        assertEquals("SERIES", MediaType.toBackend(MediaType.Series))
        assertEquals("ANIME", MediaType.toBackend(MediaType.Anime))
        assertEquals("GAME", MediaType.toBackend(MediaType.Game))
    }
}
