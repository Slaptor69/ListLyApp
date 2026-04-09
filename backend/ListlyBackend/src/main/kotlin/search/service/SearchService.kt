package com.example.search.service

import com.example.search.dto.model.SearchHit

interface SearchService {
    fun search (query: String, limit: Int = 12,offset:Int = 0) : List<SearchHit>
}