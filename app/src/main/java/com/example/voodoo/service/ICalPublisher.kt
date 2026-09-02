package com.example.voodoo.service

import android.content.Context
import android.util.Log
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.component.VTimeZone
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.model.TimeZone as ICalTimeZone
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import java.io.File
import java.io.FileOutputStream
import java.util.*

data class ICalDateFilter(
    val startTime: Long? = null,
    val endTime: Long? = null
)

sealed class ExportResult {
    data class Success(val file: File) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

object ICalPublisher {

    private const val TAG = "ICalPublisher"

    suspend fun publishCalendar(context: Context, filter: ICalDateFilter = ICalDateFilter()): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val icalSyncDao = db.icalSyncDao()
                val taskDao = db.taskDao()
                val sessionDao = db.timerSessionDao()

                val enabledContextIds = icalSyncDao.getEnabledContextIds()
                if (enabledContextIds.isEmpty()) {
                    return@withContext ExportResult.Error("Не выбрано ни одного контекста для экспорта")
                }

                val calendar = Calendar()
                calendar.properties.add(ProdId("-//VooDoo Task Tracker//RU"))
                calendar.properties.add(Version.VERSION_2_0)
                calendar.properties.add(CalScale.GREGORIAN)
                calendar.properties.add(Method.PUBLISH)

                // Получаем локальный часовой пояс устройства
                val localTimeZoneId = TimeZone.getDefault().id
                Log.d(TAG, "Local timezone: $localTimeZoneId")

                val registry = TimeZoneRegistryFactory.getInstance().createRegistry()
                val icalTimeZone: ICalTimeZone = registry.getTimeZone(localTimeZoneId)
                    ?: registry.getTimeZone("UTC")
                    ?: throw Exception("Не удалось получить часовой пояс")

                // Добавляем VTimeZone компонент в календарь
                val vTimeZone: VTimeZone = icalTimeZone.vTimeZone
                calendar.components.add(vTimeZone)

                val allTasks = taskDao.getAllTasks().first()
                Log.d(TAG, "Total tasks: ${allTasks.size}")

                val filteredTasks = allTasks.filter { task ->
                    enabledContextIds.contains(task.contextId) || task.contextId == null
                }
                Log.d(TAG, "Filtered tasks by context: ${filteredTasks.size}")

                var eventsCount = 0

                for (task in filteredTasks) {
                    // Планируемые задачи
                    if (task.plannedStart != null && task.plannedEnd != null) {
                        val passesFilter = filter.startTime == null || filter.endTime == null ||
                                (task.plannedStart >= filter.startTime && task.plannedStart <= filter.endTime)

                        if (passesFilter) {
                            try {
                                val event = createPlannedEvent(task, icalTimeZone)
                                calendar.components.add(event)
                                eventsCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error creating planned event for task ${task.id}", e)
                            }
                        }
                    }

                    // Сессии таймера
                    try {
                        val sessions = sessionDao.getSessionsByTaskSync(task.id)
                        for (session in sessions) {
                            val passesFilter = filter.startTime == null || filter.endTime == null ||
                                    (session.startTime >= filter.startTime && session.startTime <= filter.endTime)

                            if (passesFilter) {
                                val event = createTimerSessionEvent(task, session, icalTimeZone)
                                calendar.components.add(event)
                                eventsCount++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing sessions for task ${task.id}", e)
                    }
                }

                Log.d(TAG, "Total events created: $eventsCount")

                if (eventsCount == 0) {
                    return@withContext ExportResult.Error("Нет записей для экспорта в выбранном периоде")
                }

                // Используем внутреннее хранилище вместо внешнего для лучшей совместимости
                val calendarFile = File(context.filesDir, "voodoo_calendar.ics")

                // Удаляем старый файл, если он есть
                if (calendarFile.exists()) {
                    calendarFile.delete()
                }

                val outputter = CalendarOutputter(false)
                FileOutputStream(calendarFile).use { outputStream ->
                    outputter.output(calendar, outputStream)
                }

                if (!calendarFile.exists() || calendarFile.length() == 0L) {
                    return@withContext ExportResult.Error("Не удалось создать файл экспорта")
                }

                // Устанавливаем права на чтение для всех
                calendarFile.setReadable(true, false)

                Log.d(TAG, "Calendar file created: ${calendarFile.absolutePath}, size: ${calendarFile.length()} bytes")
                ExportResult.Success(calendarFile)
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                ExportResult.Error("Ошибка экспорта: ${e.message}")
            }
        }
    }

    private fun createPlannedEvent(task: Task, timeZone: ICalTimeZone): VEvent {
        val startDate = net.fortuna.ical4j.model.DateTime(Date(task.plannedStart!!), timeZone)
        val endDate = net.fortuna.ical4j.model.DateTime(Date(task.plannedEnd!!), timeZone)

        val title = if (task.isDone) "✓ " + task.title else task.title
        val event = VEvent(startDate, endDate, title)

        event.properties.add(Uid("voodoo-task-" + task.id + "-" + UUID.randomUUID()))
        event.properties.add(DtStamp())
        event.properties.add(Organizer("mailto:voodoo@local.app"))

        val descriptionParts = mutableListOf<String>()
        if (task.description.isNotBlank()) {
            descriptionParts.add("Описание: " + task.description)
        }
        if (task.result.isNotBlank()) {
            descriptionParts.add("Результат: " + task.result)
        }
        if (descriptionParts.isNotEmpty()) {
            event.properties.add(Description(descriptionParts.joinToString("\n\n")))
        }

        event.properties.add(Status("CONFIRMED"))

        return event
    }

    private fun createTimerSessionEvent(task: Task, session: TimerSession, timeZone: ICalTimeZone): VEvent {
        val startDate = net.fortuna.ical4j.model.DateTime(Date(session.startTime), timeZone)
        val endDate = net.fortuna.ical4j.model.DateTime(Date(session.endTime), timeZone)

        val durationMinutes = session.duration / (60 * 1000)
        val title = task.title + " (работа: " + durationMinutes + " мин)"

        val event = VEvent(startDate, endDate, title)

        event.properties.add(Uid("voodoo-session-" + session.id + "-" + UUID.randomUUID()))
        event.properties.add(DtStamp())
        event.properties.add(Organizer("mailto:voodoo@local.app"))
        event.properties.add(Comment("Фактическое время работы над задачей"))
        event.properties.add(Categories("Work Session"))
        event.properties.add(Status("CONFIRMED"))

        return event
    }
}