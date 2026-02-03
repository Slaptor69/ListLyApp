package ru.misterpotz.demo.di

import android.content.Context
import coil3.ImageLoader
import dagger.BindsInstance
import dagger.Component
import ru.misterpotz.demo.domain.interactors.ReminderInteractor
import ru.misterpotz.demo.features.catalog.CatalogStoreFactory
import ru.misterpotz.demo.features.mediaitem.MediaItemStoreFactory
import ru.misterpotz.demo.features.news.NewsStoreFactory
import ru.misterpotz.demo.features.readlist.ReadlistStoreFactory
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    val appContext: Context

    val catalogStoreFactory: CatalogStoreFactory
    val readlistStoreFactory: ReadlistStoreFactory
    val newsStoreFactory: NewsStoreFactory
    val mediaItemStoreFactory: MediaItemStoreFactory

    val imageLoader: ImageLoader

    // reminders
    val reminderInteractor: ReminderInteractor

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance appContext: Context): AppComponent
    }
}
