package com.example.search.service

import com.example.search.exceptions.InvalidSearchRequestException
import com.example.search.exceptions.MeiliClientException

import com.example.search.dto.model.SearchHit
import com.example.search.exceptions.SearchUnavailableException
import com.example.search.repository.SearchRepository

class MeiliMediaSearchServiceImpl(
    private val repository: SearchRepository
) : SearchService {

    override fun search(
        query: String,
        limit: Int,
        offset: Int
    ): List<SearchHit> {
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return emptyList()
        }

        if (limit !in 1..50) {
            throw InvalidSearchRequestException("Limit must be between 1 and 50")
        }

        if (offset < 0) {
            throw InvalidSearchRequestException("Offset must not be negative")
        }

        val safeQuery = normalizedQuery.take(150)

        return try {
            repository.search(safeQuery, limit, offset)
        } catch (e: MeiliClientException) {
            throw SearchUnavailableException("Search is unavailable", e)
        }
    }
}