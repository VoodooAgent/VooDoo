package com.example.voodoo.service

import android.content.Context
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

object ICalPublisher {

    suspend fun publishCalendar(context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val icalSyncDao = db.icalSyncDao()
                val taskDao = db.taskDao()
                val sessionDao = db.timerSessionDao()

                val enabledContextIds = icalSyncDao.getEnabledContextIds()
                if (enabledContextIds.isEmpty()) return@withContext null

                val calendar = Calendar()
                calendar.properties.add(ProdId("-//VooDoo Task Tracker//RU"))
                calendar.properties.add(Version.VERSION_2_0)
                calendar.properties.add(CalScale.GREGORIAN)
                calendar.properties.add(Method.PUBLISH)

                val allTasks = taskDao.getAllTasks().first()
                val filteredTasks = allTasks.filter { task ->
                    enabledContextIds.contains(task.contextId) || task.contextId == null
                }

                for (task in filteredTasks) {
                    if (task.plannedStart != null && task.plannedEnd != null) {
                        val event = createPlannedEvent(task)
                        calendar.components.add(event)
                    }

                    val sessions = sessionDao.getSessionsByTaskSync(task.id)
                    for (session in sessions) {
                        val event = createTimerSessionEvent(task, session)
                        calendar.components.add(event)
                    }
                }

                val calendarFile = File(context.getExternalFilesDir(null), "voodoo_calendar.ics")
                val outputter = CalendarOutputter(false)
                FileOutputStream(calendarFile).use { outputStream ->
                    outputter.output(calendar, outputStream)
                }

                calendarFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun createPlannedEvent(task: Task): VEvent {
        val startDate = net.fortuna.ical4j.model.DateTime(task.plannedStart!!)
        val endDate = net.fortuna.ical4j.model.DateTime(task.plannedEnd!!)

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

        // ИСПРАВЛЕНО: в ical4j 3.x нет Status.CONFIRMED, используем конструктор
        event.properties.add(Status("CONFIRMED"))

        return event
    }

    private fun createTimerSessionEvent(task: Task, session: TimerSession): VEvent {
        val startDate = net.fortuna.ical4j.model.DateTime(session.startTime)
        val endDate = net.fortuna.ical4j.model.DateTime(session.endTime)

        val durationMinutes = session.duration / (60 * 1000)
        val title = task.title + " (работа: " + durationMinutes + " мин)"

        val event = VEvent(startDate, endDate, title)

        event.properties.add(Uid("voodoo-session-" + session.id + "-" + UUID.randomUUID()))
        event.properties.add(DtStamp())
        event.properties.add(Organizer("mailto:voodoo@local.app"))
        event.properties.add(Comment("Фактическое время работы над задачей"))
        event.properties.add(Categories("Work Session"))
        // ИСПРАВЛЕНО: аналогично
        event.properties.add(Status("CONFIRMED"))

        return event
    }
}