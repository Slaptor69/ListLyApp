package ru.misterpotz.demo.domain.interactors

import kotlinx.coroutines.flow.Flow
import ru.misterpotz.demo.domain.models.Reminder
import ru.misterpotz.demo.domain.repositories.ReminderRepository

class ReminderInteractor(
    private val repository: ReminderRepository
) {
    fun observeAll(): Flow<List<Reminder>> = repository.observeAll()

    suspend fun getByMediaId(mediaId: String): List<Reminder> = repository.getByMediaId(mediaId)

    suspend fun add(reminder: Reminder) = repository.add(reminder)

    suspend fun delete(reminderId: String) = repository.delete(reminderId)
}
