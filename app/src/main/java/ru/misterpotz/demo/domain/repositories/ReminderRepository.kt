//
package ru.misterpotz.demo.domain.repositories

import kotlinx.coroutines.flow.Flow
import ru.misterpotz.demo.domain.models.Reminder

interface ReminderRepository {
    fun observeAll(): Flow<List<Reminder>>// показывает все заметки
    suspend fun getByMediaId(mediaId: String): List<Reminder>//берётся весь список напоминаний и фильтруется по mediaId

    suspend fun add(reminder: Reminder)/*две задачи:
    1)сохраняет напоминание локально в нашем хранилище
    2)Ставит WorkManager задачу чтобы чтобы уведомление пришло в нужное время */

    suspend fun delete(reminderId: String)/*так же две задачи: 1)удалить из локального хранилища
    2)Отменить WorkManager задачу*/
}
