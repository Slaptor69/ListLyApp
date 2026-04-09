package com.example.search.repository

import com.example.config.MeiliSearchConfig
import com.example.search.dto.model.SearchHit
import com.example.search.dto.request.SearchMediaRequest
import com.example.search.dto.response.SearchMediaResponse
import com.example.search.exceptions.MeiliClientException
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class MeiliMediaSearchRepository(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SearchRepository {

    override fun search(query: String, limit: Int, offset: Int): List<SearchHit> {
        val settings = MeiliSearchConfig.settings
        val url = "${settings.host}/indexes/${settings.index}/search"

        val body = json.encodeToString(
            SearchMediaRequest(
                query = query.trim(),
                limit = limit,
                offset = offset
            )
        )

        val request = requestBuilder(url, settings.apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            throw MeiliClientException("Failed to call MeiliSearch", e)
        }

        if (response.statusCode() !in 200..299) {
            throw MeiliClientException(
                "MeiliSearch request failed with status=${response.statusCode()}, body=${response.body()}"
            )
        }

        return try {
            val parsed = json.decodeFromString(SearchMediaResponse.serializer(), response.body())
            parsed.hits
        } catch (e: Exception) {
            throw MeiliClientException("Failed to parse MeiliSearch response", e)
        }
    }

    private fun requestBuilder(url: String, apiKey: String?): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")

        if (!apiKey.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        return builder
    }
}