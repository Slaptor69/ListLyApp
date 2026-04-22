package ru.misterpotz.listly.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.misterpotz.listly.domain.repositories.AuthRepository
import ru.misterpotz.listly.domain.repositories.ThemeRepository
import ru.misterpotz.listly.features.catalog.CatalogStoreFactory
import ru.misterpotz.listly.features.mediaitem.MediaItemStoreFactory
import ru.misterpotz.listly.features.readlist.ReadlistStoreFactory
import ru.misterpotz.listly.features.settings.SettingsStoreFactory
import javax.inject.Singleton

/**
 * Dagger-модуль с инфраструктурными зависимостями.
 *
 * В общей структуре проекта он находится ниже UI и Store-слоя:
 * здесь мы собираем сетевые клиенты и прочие singleton-зависимости,
 * которые потом будут инжектиться в repository / actor / store factory.
 */
@Module
interface AppModule {
    companion object {
        /**
         * Создаёт общий OkHttpClient.
         *
         * В проекте это нижний уровень доступа к сети.
         * ELM-слой сам с сетью напрямую не работает: Actor вызывает repository/interactor,
         * а те уже могут использовать этот клиент.
         */
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

        /**
         * Создаёт Retrofit поверх общего OkHttpClient.
         *
         * Сейчас проект не использует готовые API-интерфейсы Retrofit активно,
         * но этот провайдер показывает, где обычно живёт сетевой слой.
         */
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

        /**
         * Создаёт общий image loader для Coil.
         *
         * Роль в структуре: UI-экран детали берёт его из DI и показывает изображение,
         * не создавая новый loader на каждый recomposition.
         */
        @Provides
        @Singleton
        fun imageLoader(appContext: Context, okHttpClient: OkHttpClient): ImageLoader {
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


/**
 * Корневой Dagger-компонент приложения.
 *
 * Через него Compose/UI и StoreFactory получают готовые зависимости.
 */
@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    /** Application context нужен для зависимостей, которым требуется Android context. */
    val appContext: Context
    /** Репозиторий авторизации нужен и стартовой навигации, и экрану логина. */
    val authRepository: AuthRepository
    /** StoreFactory каталога собирает ELM-цикл фичи каталога. */
    val catalogStoreFactory: CatalogStoreFactory
    /** StoreFactory readlist собирает ELM-цикл списка чтения. */
    val readlistStoreFactory: ReadlistStoreFactory
    /** StoreFactory детального экрана медиапозиции. */
    val mediaItemStoreFactory: MediaItemStoreFactory
    /** Общий image loader для Coil. */
    val imageLoader: ImageLoader
    val themeRepository: ThemeRepository
    val settingsStoreFactory: SettingsStoreFactory

    /** Фабрика создаёт компонент и связывает в него Android context. */
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            appContext: Context
        ): AppComponent
    }
}
