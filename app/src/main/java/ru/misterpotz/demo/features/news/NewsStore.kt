package ru.misterpotz.demo.features.news

import kotlinx.coroutines.flow.Flow
import money.vivid.elmslie.core.store.Actor
import money.vivid.elmslie.core.store.ElmStore
import money.vivid.elmslie.core.store.StateReducer
import ru.misterpotz.demo.domain.models.NewsItem
import ru.misterpotz.demo.domain.repositories.MediaItemRepository
import ru.misterpotz.demo.domain.repositories.NewsItemRepository
import ru.misterpotz.demo.utils.Loadable
import ru.misterpotz.demo.utils.toLoadable
import javax.inject.Inject

sealed interface NewsCommand {
    data object Observe : NewsCommand
    data object StopObserving : NewsCommand
}

sealed interface NewsEffect {}

sealed interface NewsEvent {
    object Ui {
        data object OnResume : NewsEvent
        data object OnPause : NewsEvent
    }

    object Internal {
        data class Loaded(val list: List<NewsItem>) : NewsEvent
    }
}

class NewsActor @Inject constructor(
    private val newsRepository: NewsItemRepository
) : Actor<NewsCommand, NewsEvent>() {
    override fun execute(command: NewsCommand): Flow<NewsEvent> {
        return when (command) {
            NewsCommand.Observe -> newsRepository.getNews()
                .switch(NewsCommand.Observe)
                .mapEvents({ NewsEvent.Internal.Loaded(it) })

            NewsCommand.StopObserving -> cancelSwitchFlows(NewsCommand.Observe).mapEvents()
        }
    }
}

class NewsStoreFactory @Inject constructor(
    val newsActor: NewsActor
) {
    fun create(/* may pass additional parameters */): ElmStore<NewsEvent, NewState, NewsEffect, NewsCommand> {
        return ElmStore(
            initialState = NewState(),
            reducer = NewsReducer,
            actor = newsActor,
        )
    }
}

data class NewState(
    val news: Loadable<List<NewsItem>> = Loadable.Loading()
)

object NewsReducer : StateReducer<NewsEvent, NewState, NewsEffect, NewsCommand>() {
    override fun Result.reduce(event: NewsEvent) {
        when (event) {
            is NewsEvent.Internal.Loaded -> state {
                copy(news = event.list.toLoadable())
            }

            NewsEvent.Ui.OnPause -> commands {
                +NewsCommand.StopObserving
            }

            NewsEvent.Ui.OnResume -> commands {
                +NewsCommand.Observe
            }
        }
    }
}