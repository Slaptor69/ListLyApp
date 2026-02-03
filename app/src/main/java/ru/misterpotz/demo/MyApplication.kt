package ru.misterpotz.demo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ru.misterpotz.demo.di.AppComponent
import ru.misterpotz.demo.di.DaggerAppComponent

class MyApplication : Application() {

    companion object {
        lateinit var component: AppComponent
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Dagger
        component = DaggerAppComponent.factory().create(this)

        // Notification channel for reminders
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            REMINDERS_CHANNEL_ID,
            "Listly reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Напоминания о медиа в Listly"
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

}

const val REMINDERS_CHANNEL_ID = "listly_reminders"
