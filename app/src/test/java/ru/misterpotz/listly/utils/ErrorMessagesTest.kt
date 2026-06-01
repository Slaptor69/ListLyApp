package ru.misterpotz.listly.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

class ErrorMessagesTest {
    @Test
    fun `search service error is shown in Russian without exception class`() {
        val message = IllegalStateException("Search Service unavailable").toUserFriendlyMessage()

        assertEquals("Сервис поиска сейчас недоступен. Попробуйте позже.", message)
        assertFalse(message.contains("IllegalStateException"))
    }

    @Test
    fun `technical exception text is hidden from user`() {
        val message = IllegalStateException(
            "java.lang.IllegalStateException: Search Service unavailable"
        ).toUserFriendlyMessage()

        assertEquals("Сервис поиска сейчас недоступен. Попробуйте позже.", message)
        assertFalse(message.contains("java.lang"))
    }

    @Test
    fun `connect exception explains server connection problem`() {
        val message = ConnectException("failed to connect").toUserFriendlyMessage("http://10.0.2.2:8080")

        assertEquals(
            "Не удалось подключиться к http://10.0.2.2:8080. Проверьте интернет, адрес backend'а и что сервер запущен.",
            message
        )
    }

    @Test
    fun `timeout exception has readable Russian message`() {
        val message = SocketTimeoutException("timeout").toUserFriendlyMessage()

        assertEquals(
            "Сервер слишком долго не отвечает. Проверьте подключение и попробуйте ещё раз.",
            message
        )
    }

    @Test
    fun `unknown english backend message is replaced with Russian fallback`() {
        val message = IllegalStateException("Unexpected backend failure").toUserFriendlyMessage()

        assertEquals("Что-то пошло не так. Попробуйте ещё раз.", message)
    }
}
