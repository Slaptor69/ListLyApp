package com.example.search.repository

import com.example.search.dto.model.SearchHit

interface SearchRepository {
    fun search(query: String, limit: Int = 12, offset: Int = 0): List<SearchHit>
}