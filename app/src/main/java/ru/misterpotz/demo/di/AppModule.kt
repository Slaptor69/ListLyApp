package ru.misterpotz.demo.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.misterpotz.demo.features.catalog.CatalogStoreFactory
import ru.misterpotz.demo.features.mediaitem.MediaItemStoreFactory
import ru.misterpotz.demo.features.news.NewsStoreFactory
import ru.misterpotz.demo.features.readlist.ReadlistStoreFactory
import javax.inject.Singleton

@Module
interface AppModule {
    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            /**
             * Это http-client. С его помощью вы можете перехватывать запросы, добавлять туда
             * дополнительный хедеры (для аутентификации, например), делать перехваченные запросы
             * повторно (если до этого, например, свалилось из-за того, что не аутентифицирован
             * пользак)
             */
            return OkHttpClient.Builder()
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            /**
             * Это Retrofit, он необходим для генерации кода запросов по интерфейсу.
             */
            return Retrofit.Builder()
                .client(okHttpClient)
                .build()
        }

        @Provides
        @Singleton
        fun imageLoader(appContext: Context, okHttpClient: OkHttpClient):  ImageLoader {
            return ImageLoader.Builder(appContext)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { okHttpClient }
                        )
                    )
                }
                .build()
        }
    }
}


@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    val appContext: Context
    val catalogStoreFactory: CatalogStoreFactory
    val readlistStoreFactory: ReadlistStoreFactory
    val newsStoreFactory: NewsStoreFactory
    val mediaItemStoreFactory: MediaItemStoreFactory
    val imageLoader: ImageLoader

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            appContext: Context
        ): AppComponent
    }
}