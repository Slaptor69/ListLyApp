//крч модель для напоминалок
package ru.misterpotz.demo.domain.models

data class Reminder(
    val id: String,         // уникальный id напоминания
    val mediaId: String,     // к какому медиа относится
    val title: String,       // что напоминать (заголовок в уведомлении)
    val triggerAtMillis: Long // когда сработать (в миллисекундах)
)
