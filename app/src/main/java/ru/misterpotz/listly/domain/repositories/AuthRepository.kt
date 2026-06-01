package ru.misterpotz.listly.domain.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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

@Singleton
class AuthRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val tokenPreferences = createEncryptedTokenPreferences(context).also { encryptedPreferences ->
        migratePlainTokenIfNeeded(encryptedPreferences)
    }

    fun isAuthorized(): Boolean = !getToken().isNullOrBlank()

    fun getToken(): String? = tokenPreferences.getString(TOKEN_KEY, null)

    fun getLogin(): String? = preferences.getString(LOGIN_KEY, null)

    fun clearToken() {
        tokenPreferences.edit()
            .remove(TOKEN_KEY)
            .apply()
        preferences.edit()
            .remove(LOGIN_KEY)
            .apply()
    }

    fun getBaseUrl(): String {
        val customUrl = preferences.getString(BASE_URL_KEY, null)
        if (customUrl.isNullOrBlank()) {
            return BuildConfig.API_BASE_URL
        }
        val normalizedCustomUrl = normalizeBaseUrl(customUrl)
        if (normalizedCustomUrl != customUrl) {
            preferences.edit().putString(BASE_URL_KEY, normalizedCustomUrl).apply()
        }
        if (normalizedCustomUrl.isStaleBackendOverride()) {
            preferences.edit().remove(BASE_URL_KEY).apply()
            Log.w("ListlyNetwork", "Dropped stale backend baseUrl override=$customUrl")
            return BuildConfig.API_BASE_URL
        }
        return normalizedCustomUrl
    }

    fun setBaseUrl(rawUrl: String) {
        val normalized = normalizeBaseUrl(rawUrl)
        preferences.edit().putString(BASE_URL_KEY, normalized).apply()
    }

    suspend fun login(login: String, password: String): Result<String> {
        return requestToken("/auth/login", login, password).onSuccess { token ->
            saveSession(token, login)
        }
    }

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

    suspend fun registerAndLogin(login: String, password: String): Result<String> {
        val registerResult = register(login, password)
        if (registerResult.isFailure) {
            return Result.failure(registerResult.exceptionOrNull()!!)
        }
        return login(login, password)
    }

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

    private fun saveSession(token: String, login: String) {
        tokenPreferences.edit()
            .putString(TOKEN_KEY, token)
            .apply()
        preferences.edit()
            .putString(LOGIN_KEY, login)
            .apply()
    }

    private fun executePost(path: String, jsonBody: JSONObject): okhttp3.Response {
        val request = Request.Builder()
            .url("${getBaseUrl()}$path")
            .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return okHttpClient.newCall(request).execute()
    }

    private fun executeGet(path: String): okhttp3.Response {
        val request = Request.Builder()
            .url("${getBaseUrl()}$path")
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

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

    private inline fun <T> catchingNetwork(block: () -> T): Result<T> {
        return runCatching(block).recoverCatching { throwable ->
            throw IllegalStateException(toUserMessage(throwable), throwable)
        }
    }

    private fun toUserMessage(throwable: Throwable): String {
        return throwable.toUserFriendlyMessage(getBaseUrl())
    }

    private fun normalizeBaseUrl(rawUrl: String): String {
        val withoutSpaces = rawUrl.trim()
        val withScheme = if (withoutSpaces.startsWith("http://") || withoutSpaces.startsWith("https://")) {
            withoutSpaces
        } else {
            "http://$withoutSpaces"
        }
        val withoutTrailingSlash = withScheme.removeSuffix("/")
        return if (withoutTrailingSlash.endsWith("/api", ignoreCase = true)) {
            withoutTrailingSlash.dropLast("/api".length)
        } else {
            withoutTrailingSlash
        }
    }

    private fun String.isStaleBackendOverride(): Boolean {
        return startsWith("http://localhost") ||
            startsWith("https://localhost") ||
            startsWith("http://127.0.0.1") ||
            startsWith("https://127.0.0.1") ||
            contains("ngrok", ignoreCase = true)
    }

    private fun createEncryptedTokenPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            TOKEN_PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migratePlainTokenIfNeeded(encryptedPreferences: SharedPreferences) {
        if (!encryptedPreferences.getString(TOKEN_KEY, null).isNullOrBlank()) {
            preferences.edit().remove(TOKEN_KEY).apply()
            return
        }
        val plainToken = preferences.getString(TOKEN_KEY, null)
        if (!plainToken.isNullOrBlank()) {
            encryptedPreferences.edit()
                .putString(TOKEN_KEY, plainToken)
                .apply()
            preferences.edit().remove(TOKEN_KEY).apply()
        }
    }

    private companion object {
        private const val PREFERENCES_NAME = "auth_preferences"
        private const val TOKEN_PREFERENCES_NAME = "auth_token_preferences"
        private const val TOKEN_KEY = "jwt_token"
        private const val LOGIN_KEY = "login"
        private const val BASE_URL_KEY = "base_url"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
