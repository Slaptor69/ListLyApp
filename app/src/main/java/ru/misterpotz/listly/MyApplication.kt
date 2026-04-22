package ru.misterpotz.listly
import android.app.Application
import ru.misterpotz.listly.di.AppComponent
import ru.misterpotz.listly.di.DaggerAppComponent

/**
 * Application-слой приложения.
 *
 * Его роль в общей структуре проста: один раз создать Dagger-граф зависимостей,
 * чтобы дальше экраны и StoreFactory могли брать готовые репозитории и actor'ы.
 */
class MyApplication : Application() {

    companion object {
        /** Глобальная точка доступа к корневому Dagger-компоненту. */
        lateinit var component: AppComponent
    }

    /** Инициализирует граф зависимостей на старте процесса приложения. */
    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.factory().create(this)
    }
}

/**
 * Удобный accessor к корневому компоненту.
 *
 * Это инфраструктурный helper, не часть ELM.
 */
val appComponent: AppComponent
    get() {
        return (MyApplication).component
    }
