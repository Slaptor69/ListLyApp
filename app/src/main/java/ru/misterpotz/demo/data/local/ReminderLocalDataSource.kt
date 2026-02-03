package ru.misterpotz.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import ru.misterpotz.demo.domain.models.Reminder

private val Context.reminderDataStore by preferencesDataStore(name = "reminders_store")

class ReminderLocalDataSource(private val appContext: Context) {

    private object Keys {
        val REMINDERS_JSON = stringPreferencesKey("reminders_json")
    }

    fun observeAll(): Flow<List<Reminder>> =
        appContext.reminderDataStore.data.map { prefs ->
            val json = prefs[Keys.REMINDERS_JSON] ?: ""
            decode(json)
        }

    suspend fun saveAll(list: List<Reminder>) {
        appContext.reminderDataStore.edit { prefs ->
            prefs[Keys.REMINDERS_JSON] = encode(list)
        }
    }

    private fun encode(list: List<Reminder>): String {
        val arr = JSONArray()
        for (r in list) {
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("mediaId", r.mediaId)
                    .put("title", r.title)
                    .put("triggerAtMillis", r.triggerAtMillis)
            )
        }
        return arr.toString()
    }

    private fun decode(json: String): List<Reminder> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Reminder(
                            id = o.getString("id"),
                            mediaId = o.getString("mediaId"),
                            title = o.getString("title"),
                            triggerAtMillis = o.getLong("triggerAtMillis")
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
