package ru.misterpotz.listly.domain.repositories

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import ru.misterpotz.listly.domain.models.CollectionStatus
import ru.misterpotz.listly.domain.models.MediaItem
import ru.misterpotz.listly.domain.models.MediaType
import ru.misterpotz.listly.domain.models.ReadlistFolder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend-backed repository for catalog, user collection and folders.
 *
 * The repository keeps the latest loaded snapshots in StateFlow so the existing
 * ELM screens can keep observing data, while every mutation is sent to backend.
 */
@Singleton
class MediaItemRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authRepository: AuthRepository,
) {
    private val mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val readlistFolders = MutableStateFlow<List<ReadlistFolder>>(emptyList())

    fun getItems(): Flow<List<MediaItem>> = mediaItems

    fun getReadlistItems(): Flow<List<MediaItem>> {
        return mediaItems.map { it.filter { item -> item.inReadlist } }
    }

    fun getReadlistFolders(): Flow<List<ReadlistFolder>> = readlistFolders

    fun getMediaItem(id: String): MediaItem? {
        return mediaItems.value.find { it.id == id }
    }

    suspend fun searchCatalog(query: String) {
        withContext(Dispatchers.IO) {
            val userMedia = fetchUserMediaOrEmpty()
            val folders = fetchFoldersOrEmpty()
            readlistFolders.value = folders
            val foldersById = readlistFolders.value.associateBy { it.id }
            val response = executeGet(catalogPath(query), authorized = false)
            response.use {
                ensureSuccess(it.code, it.body?.string()).let { body ->
                    mediaItems.value = parseMediaArray(body)
                        .map { item -> item.withUserMedia(userMedia[item.id], foldersById) }
                }
            }
        }
    }

    suspend fun refreshReadlist() {
        withContext(Dispatchers.IO) {
            refreshFolders()
            val foldersById = readlistFolders.value.associateBy { it.id }
            val userMediaItems = fetchUserMediaOrEmpty()
            val loadedItems = userMediaItems.values.mapNotNull { userMedia ->
                fetchMedia(userMedia.mediaId)?.withUserMedia(userMedia, foldersById)
            }
            mediaItems.value = mergeById(mediaItems.value, loadedItems)
        }
    }

    suspend fun refreshFolders(): List<ReadlistFolder> {
        return withContext(Dispatchers.IO) {
            val response = executeGet("/folders", authorized = true)
            response.use {
                val body = ensureSuccess(it.code, it.body?.string())
                parseFolders(body).also { folders -> readlistFolders.value = folders }
            }
        }
    }

    suspend fun addReadlistFolder(folder: ReadlistFolder): List<ReadlistFolder> {
        return withContext(Dispatchers.IO) {
            if (folder.title.isBlank()) {
                return@withContext readlistFolders.value
            }

            val bodyJson = JSONObject().put("name", folder.title.trim())
            val response = executePost("/folders", bodyJson, authorized = true)
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            refreshFolders()
        }
    }

    suspend fun renameReadlistFolder(folder: ReadlistFolder, newTitle: String): List<ReadlistFolder> {
        return withContext(Dispatchers.IO) {
            val folderId = folder.id ?: return@withContext readlistFolders.value
            val normalizedTitle = newTitle.trim()
            if (normalizedTitle.isBlank()) {
                return@withContext readlistFolders.value
            }

            val response = executePatch(
                "/folders/$folderId",
                JSONObject().put("name", normalizedTitle),
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            refreshFolders()
        }
    }

    suspend fun deleteReadlistFolder(folder: ReadlistFolder): List<ReadlistFolder> {
        return withContext(Dispatchers.IO) {
            val folderId = folder.id ?: return@withContext readlistFolders.value
            val response = executeDelete("/folders/$folderId", authorized = true)
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            refreshFolders()
        }
    }

    suspend fun addMediaToReadlist(
        mediaItemId: String,
        folder: ReadlistFolder?,
        status: CollectionStatus? = null,
        isFavourite: Boolean = false
    ): MediaItem? {
        return addMediaToReadlist(mediaItemId, listOfNotNull(folder), status, isFavourite)
    }

    suspend fun addMediaToReadlist(
        mediaItemId: String,
        folders: List<ReadlistFolder> = emptyList(),
        status: CollectionStatus? = null,
        isFavourite: Boolean = false
    ): MediaItem? {
        return withContext(Dispatchers.IO) {
            val existing = getMediaItem(mediaItemId)
            val folderIds = folders.mapNotNull { it.id }.distinct()
            val response = executePost(
                "/user-media",
                JSONObject().apply {
                    put("mediaId", mediaItemId)
                    status?.let { put("collectionStatus", CollectionStatus.toBackend(it)) }
                    put("isFavourite", isFavourite)
                    put("folderIds", JSONArray(folderIds))
                },
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = (existing ?: fetchMedia(mediaItemId))?.withUserMedia(userMedia, foldersById)
            updated?.also { mediaItems.value = mergeById(mediaItems.value, listOf(it)) }
        }
    }

    suspend fun updateReadlistFolders(mediaItemId: String, folders: List<ReadlistFolder>): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId)
            val userMediaId = current?.userMediaId
            if (userMediaId == null && folders.isEmpty()) {
                return@withContext current
            }
            if (userMediaId == null) {
                return@withContext addMediaToReadlist(mediaItemId, folders, CollectionStatus.Planned)
            }
            if (folders.isEmpty() && current.collectionStatus == null) {
                return@withContext removeMediaFromReadlist(mediaItemId)
            }
            val folderIds = folders.mapNotNull { it.id }.distinct()
            val response = executePatch(
                "/user-media/$userMediaId/folders",
                JSONObject().put("folderIds", JSONArray(folderIds)),
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            if (folders.isNotEmpty() && current.collectionStatus == null) {
                patchUserMediaStatus(userMediaId, CollectionStatus.Planned)
            }
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun updateCollectionStatus(mediaItemId: String, status: CollectionStatus?): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId
            if (userMediaId == null) {
                return@withContext if (status == null) {
                    current.copy(collectionStatus = null)
                } else {
                    addMediaToReadlist(
                        mediaItemId = mediaItemId,
                        folders = emptyList(),
                        status = status
                    )
                }
            }
            if (status == null && current.readlistFolders.isEmpty()) {
                return@withContext removeMediaFromReadlist(mediaItemId)
            }
            patchUserMediaStatus(userMediaId, status)
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
                .copy(collectionStatus = status)
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun updateUserMediaListState(
        mediaItemId: String,
        status: CollectionStatus,
        folders: List<ReadlistFolder>
    ): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId
            if (userMediaId == null) {
                return@withContext addMediaToReadlist(mediaItemId, folders, status)
            }

            val folderIds = folders.mapNotNull { it.id }.distinct()
            val foldersResponse = executePatch(
                "/user-media/$userMediaId/folders",
                JSONObject().put("folderIds", JSONArray(folderIds)),
                authorized = true
            )
            foldersResponse.use {
                ensureSuccess(it.code, it.body?.string())
            }
            patchUserMediaStatus(userMediaId, status)
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
                .copy(collectionStatus = status)
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun updateFavourite(mediaItemId: String, isFavourite: Boolean): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId
            if (userMediaId == null) {
                return@withContext current
            }
            val response = executePatch(
                "/user-media/$userMediaId/favourite",
                JSONObject().put("isFavourite", isFavourite),
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
                .copy(isFavourite = isFavourite)
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun updateUserRating(mediaItemId: String, rating: Int): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId ?: return@withContext current
            val normalizedRating = rating.coerceIn(0, 10)
            val response = executePatch(
                "/user-media/$userMediaId",
                userMediaDetailsJson(
                    rating = normalizedRating,
                    note = current.userNote.orEmpty()
                ),
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
                .copy(userRating = normalizedRating)
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun updateUserNote(mediaItemId: String, note: String): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId ?: return@withContext current
            val normalizedNote = note.trim()
            val response = executePatch(
                "/user-media/$userMediaId",
                userMediaDetailsJson(
                    rating = current.userRating,
                    note = normalizedNote
                ),
                authorized = true
            )
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            val userMedia = fetchUserMediaOrEmpty()[mediaItemId]
            val foldersById = readlistFolders.value.associateBy { it.id }
            val updated = current.withUserMedia(userMedia, foldersById)
                .copy(userNote = normalizedNote.takeIf { it.isNotBlank() })
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    suspend fun removeMediaFromReadlist(mediaItemId: String): MediaItem? {
        return withContext(Dispatchers.IO) {
            val current = getMediaItem(mediaItemId) ?: return@withContext null
            val userMediaId = current.userMediaId ?: return@withContext current
            val response = executeDelete("/user-media/$userMediaId", authorized = true)
            response.use {
                ensureSuccess(it.code, it.body?.string())
            }
            val updated = current.copy(
                inReadlist = false,
                userMediaId = null,
                readlistFolder = null,
                readlistFolders = emptyList(),
                readlistAddedAt = null,
                collectionStatus = null,
                isFavourite = false,
                userRating = null,
                userNote = null
            )
            mediaItems.value = mergeById(mediaItems.value, listOf(updated))
            updated
        }
    }

    private fun patchUserMediaStatus(userMediaId: String, status: CollectionStatus?) {
        val response = executePatch(
            "/user-media/$userMediaId/status",
            JSONObject().put(
                "status",
                status?.let { CollectionStatus.toBackend(it) } ?: JSONObject.NULL
            ),
            authorized = true
        )
        response.use {
            ensureSuccess(it.code, it.body?.string())
        }
    }

    private fun fetchMedia(mediaId: String): MediaItem? {
        val response = executeGet("/media/$mediaId", authorized = false)
        response.use {
            if (it.code == 404) {
                return null
            }
            return parseMediaObject(ensureSuccess(it.code, it.body?.string()))
        }
    }

    private fun fetchUserMediaOrEmpty(): Map<String, UserMediaDto> {
        if (!authRepository.isAuthorized()) {
            return emptyMap()
        }
        val response = executeGet("/user-media", authorized = true)
        response.use {
            if (it.code == 401 || it.code == 403) {
                return emptyMap()
            }
            val body = ensureSuccess(it.code, it.body?.string())
            val array = JSONArray(body)
            return (0 until array.length())
                .map { index -> parseUserMedia(array.getJSONObject(index)) }
                .associateBy { it.mediaId }
        }
    }

    private fun fetchFoldersOrEmpty(): List<ReadlistFolder> {
        if (!authRepository.isAuthorized()) {
            return emptyList()
        }
        val response = executeGet("/folders", authorized = true)
        response.use {
            if (it.code == 401 || it.code == 403) {
                return emptyList()
            }
            return parseFolders(ensureSuccess(it.code, it.body?.string()))
        }
    }

    private fun parseMediaArray(body: String): List<MediaItem> {
        val array = JSONArray(body)
        return (0 until array.length()).map { index ->
            parseMediaObject(array.getJSONObject(index).toString())
        }
    }

    private fun parseMediaObject(body: String): MediaItem {
        val json = JSONObject(body)
        return MediaItem(
            id = json.optString("id"),
            title = json.optString("title"),
            type = MediaType.fromBackend(json.optString("mediaType")),
            imageUrl = json.posterUrl(),
            annotation = json.optString("description").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.posterUrl(): String? {
        val directUrl = listOf("posterUrl", "imageUrl", "poster_url", "image_url", "poster")
            .firstNotNullOfOrNull { key -> optString(key).validUrlOrNull() }
        if (directUrl != null) {
            return directUrl
        }

        return optString("posterPath")
            .takeIf { it.isNotBlank() && it != "null" }
            ?.let { path ->
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    path
                } else {
                    "https://image.tmdb.org/t/p/w500/${path.removePrefix("/")}"
                }
            }
    }

    private fun String.validUrlOrNull(): String? {
        return trim().takeIf { it.isNotBlank() && it != "null" }
    }

    private fun parseUserMedia(json: JSONObject): UserMediaDto {
        val folderIdsJson = json.optJSONArray("folderIds") ?: JSONArray()
        val folderIds = (0 until folderIdsJson.length()).mapNotNull { index ->
            folderIdsJson.optString(index).takeIf { it.isNotBlank() }
        }
        return UserMediaDto(
            id = json.optString("id"),
            mediaId = json.optString("mediaId"),
            folderIds = folderIds,
            createdAt = json.optLong("createdAt", 0L),
            status = CollectionStatus.fromBackendOrNull(
                json.optString("status")
                    .takeIf { it.isNotBlank() && it != "null" }
                    ?: json.optString("collectionStatus").takeIf { it.isNotBlank() && it != "null" }
            ),
            isFavourite = json.optBoolean("isFavourite", false),
            userRating = json.optUserRating(),
            note = json.optString("note").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    private fun JSONObject.optUserRating(): Int? {
        if (!has("userRating") || isNull("userRating")) {
            return null
        }
        return optDouble("userRating")
            .takeIf { !it.isNaN() }
            ?.let { rating -> kotlin.math.round(rating).toInt().coerceIn(0, 10) }
    }

    private fun parseFolders(body: String): List<ReadlistFolder> {
        val array = JSONArray(body)
        return (0 until array.length()).map { index ->
            val json = array.getJSONObject(index)
            ReadlistFolder(
                title = json.optString("name"),
                id = json.optString("id").takeIf { it.isNotBlank() }
            )
        }.filterNot { it.title.isStatusFolderName() }
    }

    private fun String.isStatusFolderName(): Boolean {
        return trim().lowercase() in STATUS_FOLDER_NAMES
    }

    private fun MediaItem.withUserMedia(
        userMedia: UserMediaDto?,
        foldersById: Map<String?, ReadlistFolder>
    ): MediaItem {
        val itemFolders = userMedia?.folderIds
            ?.mapNotNull { folderId -> foldersById[folderId] }
            .orEmpty()
        val effectiveStatus = userMedia?.status ?: if (itemFolders.isNotEmpty()) {
            CollectionStatus.Planned
        } else {
            null
        }
        val hasUserListState = userMedia != null && effectiveStatus != null
        return copy(
            inReadlist = hasUserListState,
            userMediaId = userMedia?.id,
            readlistFolder = itemFolders.firstOrNull().takeIf { hasUserListState },
            readlistFolders = itemFolders.takeIf { hasUserListState }.orEmpty(),
            readlistAddedAt = userMedia?.createdAt.takeIf { hasUserListState },
            collectionStatus = effectiveStatus,
            isFavourite = userMedia?.isFavourite?.takeIf { hasUserListState } ?: false,
            userRating = userMedia?.userRating.takeIf { hasUserListState },
            userNote = userMedia?.note.takeIf { hasUserListState }
        )
    }

    private fun mergeById(current: List<MediaItem>, updated: List<MediaItem>): List<MediaItem> {
        val updatedById = updated.associateBy { it.id }
        val merged = current.map { item -> updatedById[item.id] ?: item }
        val knownIds = current.map { it.id }.toSet()
        return merged + updated.filterNot { it.id in knownIds }
    }

    private fun executeGet(path: String, authorized: Boolean): okhttp3.Response {
        return okHttpClient.newCall(
            requestBuilder(path, authorized).get().build()
        ).execute()
    }

    private fun executePost(path: String, jsonBody: JSONObject, authorized: Boolean): okhttp3.Response {
        return okHttpClient.newCall(
            requestBuilder(path, authorized)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        ).execute()
    }

    private fun executePatch(path: String, jsonBody: JSONObject, authorized: Boolean): okhttp3.Response {
        return okHttpClient.newCall(
            requestBuilder(path, authorized)
                .patch(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        ).execute()
    }

    private fun executeDelete(path: String, authorized: Boolean): okhttp3.Response {
        return okHttpClient.newCall(
            requestBuilder(path, authorized).delete().build()
        ).execute()
    }

    private fun requestBuilder(path: String, authorized: Boolean): Request.Builder {
        val url = "${authRepository.getBaseUrl()}$path"
        Log.d("ListlyNetwork", "Backend request url=$url authorized=$authorized")
        return Request.Builder()
            .url(url)
            .withBearerAuthorization(
                token = authRepository.getToken(),
                authorized = authorized
            )
    }

    private fun ensureSuccess(code: Int, body: String?): String {
        if (code in 200..299) {
            return body.orEmpty()
        }
        val serverMessage = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() ?: it }
            ?.takeIf { it.isNotBlank() }
        throw IllegalStateException(serverMessage ?: "Ошибка backend ($code)")
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    private fun catalogPath(query: String): String {
        return if (query.isBlank()) {
            "/media/discover?limit=$CATALOG_PAGE_LIMIT&offset=0"
        } else {
            "/media/search?query=${encode(query)}&limit=$CATALOG_PAGE_LIMIT&offset=0"
        }
    }

    private fun userMediaDetailsJson(rating: Int?, note: String?): JSONObject {
        return JSONObject().apply {
            if (rating != null) {
                put("userRating", rating.toDouble())
            }
            if (note != null) {
                put("note", note)
            }
        }
    }

    private data class UserMediaDto(
        val id: String,
        val mediaId: String,
        val folderIds: List<String>,
        val createdAt: Long,
        val status: CollectionStatus?,
        val isFavourite: Boolean,
        val userRating: Int?,
        val note: String?,
    )

    private companion object {
        private const val CATALOG_PAGE_LIMIT = 12
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val STATUS_FOLDER_NAMES = setOf(
            "planned",
            "watched",
            "watching",
            "in_progress",
            "completed",
            "dropped",
            "запланировано",
            "просмотрено",
            "завершено",
            "брошено"
        )
    }
}

internal fun Request.Builder.withBearerAuthorization(
    token: String?,
    authorized: Boolean
): Request.Builder {
    if (authorized && !token.isNullOrBlank()) {
        header("Authorization", "Bearer $token")
    }
    return this
}
