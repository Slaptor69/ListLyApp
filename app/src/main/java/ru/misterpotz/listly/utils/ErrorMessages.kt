package ru.misterpotz.listly.utils

import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val DEFAULT_ERROR_MESSAGE = "Что-то пошло не так. Попробуйте ещё раз."
fun Throwable?.toUserFriendlyMessage(serverBaseUrl: String? = null): String {
    if (this == null) {
        return DEFAULT_ERROR_MESSAGE
    }

    val chain = generateSequence(this) { it.cause }.toList()
    val rawMessage = chain
        .firstNotNullOfOrNull { throwable -> throwable.message?.trim()?.takeIf { it.isNotBlank() } }
        .orEmpty()
    val combinedTechnicalText = chain.joinToString(" ") { throwable ->
        "${throwable.javaClass.name} ${throwable.message.orEmpty()}"
    }
    val normalizedMessage = rawMessage.lowercase()

    return when {
        combinedTechnicalText.contains("Search Service unavailable", ignoreCase = true) ->
            "Сервис поиска сейчас недоступен. Попробуйте позже."

        normalizedMessage.contains("service unavailable") ->
            "Сервис сейчас недоступен. Попробуйте позже."

        normalizedMessage.contains("bad credentials") ||
            normalizedMessage.contains("invalid credentials") ||
            normalizedMessage.contains("invalid login") ||
            normalizedMessage.contains("invalid password") ->
            "Неверный логин или пароль."

        normalizedMessage.contains("already exists") ||
            normalizedMessage.contains("duplicate") ->
            "Такая запись уже существует."

        chain.any { it is SocketTimeoutException } ||
            combinedTechnicalText.contains("timeout", ignoreCase = true) ->
            "Сервер слишком долго не отвечает. Проверьте подключение и попробуйте ещё раз."

        chain.any { it is UnknownHostException } ->
            "Не удалось найти сервер. Проверьте адрес backend'а и подключение к интернету."

        chain.any { it is ConnectException } ||
            combinedTechnicalText.contains("failed to connect", ignoreCase = true) ->
            buildConnectionMessage(serverBaseUrl)

        chain.any { it is SocketException || it is IOException } ->
            "Не удалось связаться с сервером. Проверьте интернет и попробуйте ещё раз."

        rawMessage.contains("(401)") || rawMessage.contains("unauthorized", ignoreCase = true) ->
            "Сессия устарела. Войдите в аккаунт ещё раз."

        rawMessage.contains("(403)") || rawMessage.contains("forbidden", ignoreCase = true) ->
            "Нет доступа к этому действию."

        rawMessage.contains("(400)") ->
            "Некорректные данные. Проверьте ввод и попробуйте ещё раз."

        rawMessage.contains("(404)") || rawMessage.contains("not found", ignoreCase = true) ->
            "Данные не найдены. Обновите экран и попробуйте ещё раз."

        rawMessage.contains("(409)") ->
            "Такая запись уже существует."

        rawMessage.contains("(500)") || rawMessage.contains("(502)") ||
            rawMessage.contains("(503)") || rawMessage.contains("(504)") ->
            "На сервере произошла ошибка. Попробуйте позже."

        rawMessage.isUserReadableRussian() -> rawMessage

        else -> DEFAULT_ERROR_MESSAGE
    }
}

private fun buildConnectionMessage(serverBaseUrl: String?): String {
    val prefix = if (serverBaseUrl.isNullOrBlank()) {
        "Не удалось подключиться к серверу."
    } else {
        "Не удалось подключиться к $serverBaseUrl."
    }
    return "$prefix Проверьте интернет, адрес backend'а и что сервер запущен."
}

private fun String.isUserReadableRussian(): Boolean {
    return any { it in 'А'..'я' || it == 'ё' || it == 'Ё' } && !looksTechnical()
}

private fun String.looksTechnical(): Boolean {
    return contains("java.", ignoreCase = true) ||
        contains("kotlin.", ignoreCase = true) ||
        contains("okhttp", ignoreCase = true) ||
        contains("Exception", ignoreCase = true) ||
        contains("Throwable", ignoreCase = true) ||
        contains(" at ", ignoreCase = true)
}
