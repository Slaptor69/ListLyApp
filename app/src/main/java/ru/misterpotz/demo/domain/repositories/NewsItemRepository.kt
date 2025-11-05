package ru.misterpotz.demo.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.misterpotz.demo.domain.models.NewsItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsItemRepository @Inject constructor() {
    // dumb in-memory runtime storage, not preserved across application reboots
    private val newsItems = MutableStateFlow(DefaultNews)

    fun getNews(): Flow<List<NewsItem>> {
        return newsItems
    }
}

val DefaultNews = listOf<NewsItem>(
    NewsItem(0, "Выходит новый сезон Sousou no Frieren!"),
    NewsItem(1, "В продажу поступит саундтрек Akiba Maid Wars на виниле")
)