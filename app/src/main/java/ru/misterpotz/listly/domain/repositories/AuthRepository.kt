package ru.misterpotz.listly.domain.repositories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import ru.misterpotz.listly.BuildConfig
import ru.misterpotz.listly.utils.toUserFriendlyMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий авторизации.
 *
 * В общей структуре это gateway к данным авторизации: токен, базовый URL сервера,
 * login/register-запросы.
 * Этот экран сейчас не построен через ELM, но остальные слои всё равно используют те же принципы:
 * UI не знает деталей сети и делегирует их repository.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Проверяет, сохранён ли токен и можно ли пропустить экран логина. */
    fun isAuthorized(): Boolean = !getToken().isNullOrBlank()

    /** Возвращает сохранённый токен, если пользователь уже входил в систему. */
    fun getToken(): String? = preferences.getString(TOKEN_KEY, null)

    /** Возвращает сохранённый логин текущего пользователя. */
    fun getLogin(): String? = preferences.getString(LOGIN_KEY, null)

    /** Удаляет данные сессии, если потребуется разлогинить пользователя. */
    fun clearToken() {
        preferences.edit()
            .remove(TOKEN_KEY)
            .remove(LOGIN_KEY)
            .apply()
    }

    /** Возвращает текущий базовый URL backend'а с учётом локальной override-настройки. */
    fun getBaseUrl(): String {
        val customUrl = preferences.getString(BASE_URL_KEY, null)
        if (customUrl.isNullOrBlank()) {
            return BuildConfig.API_BASE_URL
        }
        if (customUrl.isStaleBackendOverride()) {
            preferences.edit().remove(BASE_URL_KEY).apply()
            Log.w("ListlyNetwork", "Dropped stale backend baseUrl override=$customUrl")
            return BuildConfig.API_BASE_URL
        }
        return customUrl
    }

    /** Сохраняет базовый URL, который пользователь ввёл на debug-экране. */
    fun setBaseUrl(rawUrl: String) {
        val normalized = normalizeBaseUrl(rawUrl)
        preferences.edit().putString(BASE_URL_KEY, normalized).apply()
    }

    /** Выполняет логин и при успехе сохраняет токен локально. */
    suspend fun login(login: String, password: String): Result<String> {
        return requestToken("/auth/login", login, password).onSuccess { token ->
            saveSession(token, login)
        }
    }

    /** Выполняет регистрацию нового пользователя на backend'е. */
    suspend fun register(login: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            catchingNetwork {
                val bodyJson = JSONObject()
                    .put("login", login)
                    .put("password", password)
                val response = executePost("/auth/register", bodyJson)
                response.use {
                    if (!it.isSuccessful) {
                        throw IllegalStateException(
                            extractErrorMessage(
                                code = it.code,
                                body = it.body?.string(),
                                fallbackPrefix = "Ошибка backend"
                            )
                        )
                    }
                }
            }
        }
    }

    /** Проверяет, что backend доступен. Требует серверный endpoint GET /health. */
    suspend fun checkHealth(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            catchingNetwork {
                val response = executeGet("/health")
                response.use {
                    if (!it.isSuccessful) {
                        throw IllegalStateException(extractErrorMessage(it.code, it.body?.string()))
                    }
                }
            }
        }
    }

    /** Сценарий регистрации с немедленным логином для удобства UI. */
    suspend fun registerAndLogin(login: String, password: String): Result<String> {
        val registerResult = register(login, password)
        if (registerResult.isFailure) {
            return Result.failure(registerResult.exceptionOrNull()!!)
        }
        return login(login, password)
    }

    /** Общий helper для эндпоинтов, которые должны вернуть JWT token. */
    private suspend fun requestToken(path: String, login: String, password: String): Result<String> {
        return withContext(Dispatchers.IO) {
            catchingNetwork {
                val bodyJson = JSONObject()
                    .put("login", login)
                    .put("password", password)
                val response = executePost(path, bodyJson)
                response.use {
                    if (!it.isSuccessful) {
                        throw IllegalStateException(extractErrorMessage(it.code, it.body?.string()))
                    }
                    val responseBody = it.body?.string().orEmpty()
                    val token = JSONObject(responseBody).optString("token")
                    if (token.isBlank()) {
                        throw IllegalStateException("Сервер не вернул token")
                    }
                    token
                }
            }
        }
    }

    /** Изолирует запись данных сессии в SharedPreferences. */
    private fun saveSession(token: String, login: String) {
        preferences.edit()
            .putString(TOKEN_KEY, token)
            .putString(LOGIN_KEY, login)
            .apply()
    }

    /** Выполняет POST-запрос с JSON-телом на backend. */
    private fun executePost(path: String, jsonBody: JSONObject): okhttp3.Response {
        val request = Request.Builder()
            .url("${getBaseUrl()}$path")
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    /** Выполняет GET-запрос на backend. */
    private fun executeGet(path: String): okhttp3.Response {
        val request = Request.Builder()
            .url("${getBaseUrl()}$path")
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    /** Достаёт человекочитаемую ошибку из тела ответа сервера. */
    private fun extractErrorMessage(
        code: Int,
        body: String?,
        fallbackPrefix: String = "Ошибка авторизации"
    ): String {
        val serverMessage = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }

        return serverMessage ?: "$fallbackPrefix ($code)"
    }

    /** Оборачивает сетевой вызов в Result и нормализует текст ошибок для UI. */
    private inline fun <T> catchingNetwork(block: () -> T): Result<T> {
        return runCatching(block).recoverCatching { throwable ->
            throw IllegalStateException(toUserMessage(throwable), throwable)
        }
    }

    /** Переводит технические сетевые ошибки в текст, понятный пользователю. */
    private fun toUserMessage(throwable: Throwable): String {
        return throwable.toUserFriendlyMessage(getBaseUrl())
    }

    /** Добавляет схему и убирает завершающий слэш, чтобы URL был единообразным. */
    private fun normalizeBaseUrl(rawUrl: String): String {
        val withoutSpaces = rawUrl.trim()
        val withScheme = if (withoutSpaces.startsWith("http://") || withoutSpaces.startsWith("https://")) {
            withoutSpaces
        } else {
            "http://$withoutSpaces"
        }
        return withScheme.removeSuffix("/")
    }

    private fun String.isStaleBackendOverride(): Boolean {
        return startsWith("http://localhost") ||
            startsWith("https://localhost") ||
            startsWith("http://127.0.0.1") ||
            startsWith("https://127.0.0.1") ||
            contains("ngrok", ignoreCase = true)
    }

    /** Ключи локального хранения служебных данных авторизации. */
    private companion object {
        private const val PREFERENCES_NAME = "auth_preferences"
        private const val TOKEN_KEY = "jwt_token"
        private const val LOGIN_KEY = "login"
        private const val BASE_URL_KEY = "base_url"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
