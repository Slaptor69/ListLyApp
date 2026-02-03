package ru.misterpotz.demo.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ru.misterpotz.demo.data.local.ReminderLocalDataSource
import ru.misterpotz.demo.data.reminders.ReminderScheduler
import ru.misterpotz.demo.data.repositories.ReminderRepositoryImpl
import ru.misterpotz.demo.domain.interactors.ReminderInteractor
import ru.misterpotz.demo.domain.repositories.ReminderRepository
import javax.inject.Singleton

@Module
object AppModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Provides
    @Singleton
    @JvmStatic
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    @JvmStatic
    fun provideImageLoader(appContext: Context, okHttpClient: OkHttpClient): ImageLoader =
        ImageLoader.Builder(appContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .build()

    // -------- Reminders --------

    @Provides
    @Singleton
    @JvmStatic
    fun provideReminderLocalDataSource(appContext: Context): ReminderLocalDataSource =
        ReminderLocalDataSource(appContext)

    @Provides
    @Singleton
    @JvmStatic
    fun provideReminderScheduler(appContext: Context): ReminderScheduler =
        ReminderScheduler(appContext)

    @Provides
    @Singleton
    @JvmStatic
    fun provideReminderRepository(
        local: ReminderLocalDataSource,
        scheduler: ReminderScheduler
    ): ReminderRepository =
        ReminderRepositoryImpl(local, scheduler)

    @Provides
    @Singleton
    @JvmStatic
    fun provideReminderInteractor(repo: ReminderRepository): ReminderInteractor =
        ReminderInteractor(repo)
}
