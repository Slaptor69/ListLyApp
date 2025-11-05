package ru.misterpotz.demo

import android.app.Application
import ru.misterpotz.demo.di.AppComponent
import ru.misterpotz.demo.di.DaggerAppComponent

class MyApplication : Application() {

    companion object {
        lateinit var component: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        component = DaggerAppComponent.factory().create(this)
    }
}

val appComponent: AppComponent
    get() {
        return (MyApplication).component
    }