package com.example.voodoo.service

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.CalendarContextSetting
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*

object CSVManager {

    suspend fun exportToCSV(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val contextDao = db.contextDao()
                val taskDao = db.taskDao()
                val sessionDao = db.timerSessionDao()

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        val csvWriter = CSVWriter(writer)

                        csvWriter.writeNext(arrayOf("CONTEXTS"))
                        csvWriter.writeNext(arrayOf("id", "name", "color", "sortOrder", "createdAt"))
                        val contexts = contextDao.getAllContexts().first()
                        contexts.forEach { ctx ->
                            csvWriter.writeNext(arrayOf(
                                ctx.id.toString(),
                                ctx.name,
                                ctx.color.toString(),
                                ctx.sortOrder.toString(),
                                ctx.createdAt.toString()
                            ))
                        }

                        csvWriter.writeNext(arrayOf(""))
                        csvWriter.writeNext(arrayOf("TASKS"))
                        csvWriter.writeNext(arrayOf(
                            "id", "contextId", "parentId", "level", "title", "description",
                            "result", "isDone", "priority", "sortOrder", "plannedStart",
                            "plannedEnd", "reminderMinutesBefore", "createdAt", "completedAt"
                        ))
                        val tasks = taskDao.getAllTasks().first()
                        tasks.forEach { task ->
                            csvWriter.writeNext(arrayOf(
                                task.id.toString(),
                                task.contextId?.toString() ?: "",
                                task.parentId?.toString() ?: "",
                                task.level.toString(),
                                task.title,
                                task.description,
                                task.result,
                                task.isDone.toString(),
                                task.priority.toString(),
                                task.sortOrder.toString(),
                                task.plannedStart?.toString() ?: "",
                                task.plannedEnd?.toString() ?: "",
                                task.reminderMinutesBefore?.toString() ?: "",
                                task.createdAt.toString(),
                                task.completedAt?.toString() ?: ""
                            ))
                        }

                        csvWriter.writeNext(arrayOf(""))
                        csvWriter.writeNext(arrayOf("TIMER_SESSIONS"))
                        csvWriter.writeNext(arrayOf("id", "taskId", "startTime", "endTime", "duration", "comment"))
                        tasks.forEach { task ->
                            val sessions = sessionDao.getSessionsByTaskSync(task.id)
                            sessions.forEach { session ->
                                csvWriter.writeNext(arrayOf(
                                    session.id.toString(),
                                    session.taskId.toString(),
                                    session.startTime.toString(),
                                    session.endTime.toString(),
                                    session.duration.toString(),
                                    session.comment
                                ))
                            }
                        }

                        csvWriter.close()
                    }
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importFromCSV(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)

                val contexts = mutableListOf<ProjectContext>()
                val tasks = mutableListOf<Task>()
                val sessions = mutableListOf<TimerSession>()

                var currentSection = ""

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val csvReader = CSVReader(reader)

                        csvReader.forEach { line ->
                            when {
                                line.isEmpty() -> return@forEach
                                line[0] == "CONTEXTS" -> {
                                    currentSection = "contexts"
                                    return@forEach
                                }
                                line[0] == "TASKS" -> {
                                    currentSection = "tasks"
                                    return@forEach
                                }
                                line[0] == "TIMER_SESSIONS" -> {
                                    currentSection = "sessions"
                                    return@forEach
                                }
                                line[0] == "id" -> return@forEach
                                line[0].matches(Regex("\\d+")) && currentSection == "contexts" -> {
                                    val ctx = ProjectContext(
                                        id = line[0].toLong(),
                                        name = line[1],
                                        color = line[2].toLong(),
                                        sortOrder = line[3].toInt(),
                                        createdAt = line[4].toLong()
                                    )
                                    contexts.add(ctx)
                                }
                                line[0].matches(Regex("\\d+")) && currentSection == "tasks" -> {
                                    val task = Task(
                                        id = line[0].toLong(),
                                        contextId = line[1].takeIf { it.isNotBlank() }?.toLong(),
                                        parentId = line[2].takeIf { it.isNotBlank() }?.toLong(),
                                        level = line[3].toInt(),
                                        title = line[4],
                                        description = line[5],
                                        result = line[6],
                                        isDone = line[7].toBoolean(),
                                        priority = line[8].toInt(),
                                        sortOrder = line[9].toInt(),
                                        plannedStart = line[10].takeIf { it.isNotBlank() }?.toLong(),
                                        plannedEnd = line[11].takeIf { it.isNotBlank() }?.toLong(),
                                        reminderMinutesBefore = line[12].takeIf { it.isNotBlank() }?.toInt(),
                                        createdAt = line[13].toLong(),
                                        completedAt = line[14].takeIf { it.isNotBlank() }?.toLong()
                                    )
                                    tasks.add(task)
                                }
                                line[0].matches(Regex("\\d+")) && currentSection == "sessions" -> {
                                    val session = TimerSession(
                                        id = line[0].toLong(),
                                        taskId = line[1].toLong(),
                                        startTime = line[2].toLong(),
                                        endTime = line[3].toLong(),
                                        duration = line[4].toLong(),
                                        comment = if (line.size >= 6) line[5] else ""
                                    )
                                    sessions.add(session)
                                }
                            }
                        }
                        csvReader.close()
                    }
                }

                db.withTransaction {
                    db.timerSessionDao().deleteAll()
                    db.taskDao().deleteAll()
                    db.calendarContextDao().deleteAll()
                    db.contextDao().deleteAll()

                    val contextIdMapping = mutableMapOf<Long, Long>()
                    val taskIdMapping = mutableMapOf<Long, Long>()

                    contexts.forEach { oldContext ->
                        val newId = db.contextDao().insertWithId(oldContext)
                        contextIdMapping[oldContext.id] = newId

                        db.calendarContextDao().insert(CalendarContextSetting(
                            contextId = newId,
                            enabled = true
                        ))
                    }

                    tasks.forEach { oldTask ->
                        val newTask = oldTask.copy(
                            contextId = oldTask.contextId?.let { contextIdMapping[it] },
                            parentId = oldTask.parentId?.let { taskIdMapping[it] }
                        )
                        val newId = db.taskDao().insertWithId(newTask)
                        taskIdMapping[oldTask.id] = newId
                    }

                    var sessionsImported = 0
                    sessions.forEach { oldSession ->
                        val newTaskId = taskIdMapping[oldSession.taskId]
                        if (newTaskId != null) {
                            val newSession = oldSession.copy(taskId = newTaskId)
                            db.timerSessionDao().insertWithId(newSession)
                            sessionsImported++
                        }
                    }

                    android.util.Log.d("CSVManager", "Импортировано: ${contexts.size} контекстов, ${tasks.size} задач, $sessionsImported/${sessions.size} сессий")
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}