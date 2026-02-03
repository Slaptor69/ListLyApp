/*планировщик.
ставит “задачу на будущее” через WorkManager
“в такое-то время запусти ReminderWorker с такими-то данными”
умеет отменить задачу (cancel)
 */

package ru.misterpotz.demo.data.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import ru.misterpotz.demo.worker.ReminderWorker

class ReminderScheduler(private val appContext: Context) {

    fun schedule(reminderId: String, mediaId: String, title: String, triggerAtMillis: Long) {
        val now = System.currentTimeMillis()
        val delayMs = (triggerAtMillis - now).coerceAtLeast(0L)

        val input = Data.Builder()
            .putString(ReminderWorker.KEY_REMINDER_ID, reminderId)
            .putString(ReminderWorker.KEY_MEDIA_ID, mediaId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .build()

        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(workName(reminderId), ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(reminderId: String) {
        WorkManager.getInstance(appContext).cancelUniqueWork(workName(reminderId))
    }

    private fun workName(reminderId: String) = "reminder_$reminderId"
}
