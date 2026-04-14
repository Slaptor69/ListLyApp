package com.example.search.repository

import com.example.media.model.MediaItem
import com.example.search.dto.model.SearchDocument

interface SearchRepository {
    fun searchIds(query: String, limit: Int = 12, offset: Int = 0): List<String>

    fun upsertDocuments(documents:List<SearchDocument>)

    fun upsertDocument(document: SearchDocument) {
        upsertDocuments(listOf(document))
    }

    fun deleteDocument(documentId:String)
}