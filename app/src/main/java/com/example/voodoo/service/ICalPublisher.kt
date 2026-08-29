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
                val outputter = CalendarOutputter()
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
        val startDate = net.fortuna.ical4j.model.Date(task.plannedStart!!)
        val endDate = net.fortuna.ical4j.model.Date(task.plannedEnd!!)

        val event = VEvent(startDate, endDate, task.title)

        if (task.description.isNotBlank()) {
            event.properties.add(Description(task.description))
        }

        if (task.result.isNotBlank()) {
            event.properties.add(Comment("Результат: ${task.result}"))
        }

        val statusValue = if (task.isDone) "COMPLETED" else "CONFIRMED"
        event.properties.add(Status(statusValue))

        return event
    }

    private fun createTimerSessionEvent(task: Task, session: TimerSession): VEvent {
        val startDate = net.fortuna.ical4j.model.Date(session.startTime)
        val endDate = net.fortuna.ical4j.model.Date(session.endTime)

        val durationMinutes = session.duration / (60 * 1000)
        val title = "${task.title} (работа: ${durationMinutes} мин)"

        val event = VEvent(startDate, endDate, title)

        event.properties.add(Comment("Фактическое время работы над задачей"))
        event.properties.add(Categories("Work Session"))

        return event
    }
}