package ru.misterpotz.listly
import android.app.Application
import android.util.Log
import ru.misterpotz.listly.di.AppComponent
import ru.misterpotz.listly.di.DaggerAppComponent

class MyApplication : Application() {

    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.factory().create(this)
        Log.d("ListlyNetwork", "Effective backend baseUrl=${component.authRepository.getBaseUrl()}")
    }
}

val appComponent: AppComponent
    get() {
        return (MyApplication).component
    }
