package ru.misterpotz.demo.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.misterpotz.demo.R
import ru.misterpotz.demo.REMINDERS_CHANNEL_ID

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()

        // Android 13+: если нет разрешения на уведомления — просто выходим (воркер не должен падать)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return Result.success()
        }

        val notification = NotificationCompat.Builder(applicationContext, REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Listly • Напоминание")
            .setContentText(title)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(reminderId.hashCode(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_TITLE = "title"
    }
}
