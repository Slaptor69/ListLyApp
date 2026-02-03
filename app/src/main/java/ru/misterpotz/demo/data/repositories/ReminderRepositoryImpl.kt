package ru.misterpotz.demo.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import ru.misterpotz.demo.data.local.ReminderLocalDataSource
import ru.misterpotz.demo.data.reminders.ReminderScheduler
import ru.misterpotz.demo.domain.models.Reminder
import ru.misterpotz.demo.domain.repositories.ReminderRepository

class ReminderRepositoryImpl(
    private val local: ReminderLocalDataSource,
    private val scheduler: ReminderScheduler
) : ReminderRepository {

    override fun observeAll(): Flow<List<Reminder>> = local.observeAll()

    override suspend fun getByMediaId(mediaId: String): List<Reminder> {
        return local.observeAll().first().filter { it.mediaId == mediaId }
    }

    override suspend fun add(reminder: Reminder) {
        val current = local.observeAll().first()
        local.saveAll(current + reminder)

        scheduler.schedule(
            reminderId = reminder.id,
            mediaId = reminder.mediaId,
            title = reminder.title,
            triggerAtMillis = reminder.triggerAtMillis
        )
    }

    override suspend fun delete(reminderId: String) {
        val current = local.observeAll().first()
        local.saveAll(current.filterNot { it.id == reminderId })

        scheduler.cancel(reminderId)
    }
}