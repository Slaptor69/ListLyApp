package ru.misterpotz.listly.domain.repositories

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthorizationHeaderTest {

    @Test
    fun `authorized request includes saved token as bearer header`() {
        val request = Request.Builder()
            .url("http://example.test/media/search")
            .withBearerAuthorization(token = "jwt-token", authorized = true)
            .build()

        assertEquals("Bearer jwt-token", request.header("Authorization"))
    }

    @Test
    fun `non authorized request does not include bearer header`() {
        val request = Request.Builder()
            .url("http://example.test/media/search")
            .withBearerAuthorization(token = "jwt-token", authorized = false)
            .build()

        assertNull(request.header("Authorization"))
    }

    @Test
    fun `blank token is not sent as bearer header`() {
        val request = Request.Builder()
            .url("http://example.test/media/search")
            .withBearerAuthorization(token = "   ", authorized = true)
            .build()

        assertNull(request.header("Authorization"))
    }
}
