package ru.misterpotz.listly.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import retrofit2.Retrofit
import ru.misterpotz.listly.BuildConfig
import ru.misterpotz.listly.domain.repositories.AuthRepository
import ru.misterpotz.listly.domain.repositories.MediaItemRepository
import ru.misterpotz.listly.domain.repositories.ThemeRepository
import ru.misterpotz.listly.features.catalog.CatalogStoreFactory
import ru.misterpotz.listly.features.mediaitem.MediaItemStoreFactory
import ru.misterpotz.listly.features.readlist.ReadlistStoreFactory
import ru.misterpotz.listly.features.settings.SettingsStoreFactory
import java.net.InetAddress
import javax.inject.Singleton

@Module
interface AppModule {
    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("${BuildConfig.API_BASE_URL}/")
                .build()
        }

        @Provides
        @Singleton
        fun imageLoader(appContext: Context, okHttpClient: OkHttpClient): ImageLoader {
            val imageOkHttpClient = okHttpClient.newBuilder()
                .dns(DohFallbackDns())
                .build()
            return ImageLoader.Builder(appContext)
                .components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { imageOkHttpClient }
                        )
                    )
                }
                .build()
        }
    }
}

private class DohFallbackDns : Dns {
    private val dohDns by lazy {
        DnsOverHttps.Builder()
            .client(OkHttpClient.Builder().build())
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .build()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val systemAddresses = runCatching { Dns.SYSTEM.lookup(hostname) }.getOrElse { emptyList() }
        if (systemAddresses.any { !it.isLoopbackAddress }) {
            return systemAddresses
        }

        val dohAddresses = runCatching { dohDns.lookup(hostname) }.getOrElse { emptyList() }
        return dohAddresses.ifEmpty { systemAddresses }
    }
}


@Component(modules = [AppModule::class])
@Singleton
interface AppComponent {
    val appContext: Context
    val authRepository: AuthRepository
    val mediaItemRepository: MediaItemRepository
    val catalogStoreFactory: CatalogStoreFactory
    val readlistStoreFactory: ReadlistStoreFactory
    val mediaItemStoreFactory: MediaItemStoreFactory
    val imageLoader: ImageLoader
    val themeRepository: ThemeRepository
    val settingsStoreFactory: SettingsStoreFactory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            appContext: Context
        ): AppComponent
    }
}
