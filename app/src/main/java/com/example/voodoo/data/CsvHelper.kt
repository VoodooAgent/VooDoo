package com.example.voodoo.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CsvHelper(private val context: Context) {

    suspend fun exportToCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val contextDao = database.contextDao()
            val taskDao = database.taskDao()
            val timerSessionDao = database.timerSessionDao()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    val csvWriter = CSVWriter(writer)

                    csvWriter.writeNext(arrayOf("# CONTEXTS"))
                    csvWriter.writeNext(arrayOf("id", "name", "color", "sortOrder", "createdAt"))
                    contextDao.getAllContextsSync().forEach { ctx ->
                        csvWriter.writeNext(arrayOf(
                            ctx.id.toString(),
                            ctx.name,
                            ctx.color.toString(),
                            ctx.sortOrder.toString(),
                            ctx.createdAt.toString()
                        ))
                    }

                    csvWriter.writeNext(arrayOf(""))
                    csvWriter.writeNext(arrayOf("# TASKS"))
                    csvWriter.writeNext(arrayOf(
                        "id", "contextId", "parentId", "level", "title", "description",
                        "result", "isDone", "priority", "sortOrder", "plannedStart",
                        "plannedEnd", "deadline", "reminderMinutesBefore", "createdAt", "completedAt",
                        "timerActive", "timerStartedAt"
                    ))
                    taskDao.getAllTasksSync().forEach { task ->
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
                            task.deadline?.toString() ?: "",
                            task.reminderMinutesBefore?.toString() ?: "",
                            task.createdAt.toString(),
                            task.completedAt?.toString() ?: "",
                            task.timerActive.toString(),
                            task.timerStartedAt?.toString() ?: ""
                        ))
                    }

                    csvWriter.writeNext(arrayOf(""))
                    csvWriter.writeNext(arrayOf("# TIMER_SESSIONS"))
                    csvWriter.writeNext(arrayOf("id", "taskId", "startTime", "endTime", "duration", "comment"))
                    timerSessionDao.getAllTimerSessionsSync().forEach { session ->
                        csvWriter.writeNext(arrayOf(
                            session.id.toString(),
                            session.taskId.toString(),
                            session.startTime.toString(),
                            session.endTime.toString(),
                            session.duration.toString(),
                            session.comment
                        ))
                    }

                    csvWriter.close()
                }
            }

            Result.success("Экспорт завершён успешно")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromCsv(uri: Uri, replaceAll: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)

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
                            line[0] == "# CONTEXTS" -> {
                                currentSection = "contexts"
                                return@forEach
                            }
                            line[0] == "# TASKS" -> {
                                currentSection = "tasks"
                                return@forEach
                            }
                            line[0] == "# TIMER_SESSIONS" -> {
                                currentSection = "sessions"
                                return@forEach
                            }
                            line[0] == "id" -> return@forEach
                            else -> {
                                when (currentSection) {
                                    "contexts" -> {
                                        if (line.size >= 5) {
                                            contexts.add(ProjectContext(
                                                id = line[0].toLongOrNull() ?: 0,
                                                name = line[1],
                                                color = line[2].toLongOrNull() ?: 0xFFE0E0E0,
                                                sortOrder = line[3].toIntOrNull() ?: 0,
                                                createdAt = line[4].toLongOrNull() ?: System.currentTimeMillis()
                                            ))
                                        }
                                    }
                                    "tasks" -> {
                                        // Новый формат (18 колонок) включает deadline, старый (17) — нет.
                                        // Определяем по длине строки.
                                        val hasDeadline = line.size >= 18
                                        tasks.add(Task(
                                            id = line.getOrNull(0)?.toLongOrNull() ?: 0,
                                            contextId = line.getOrNull(1)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                                            parentId = line.getOrNull(2)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                                            level = line.getOrNull(3)?.toIntOrNull() ?: 0,
                                            title = line.getOrNull(4) ?: "",
                                            description = line.getOrNull(5) ?: "",
                                            result = line.getOrNull(6) ?: "",
                                            isDone = line.getOrNull(7)?.toBooleanStrictOrNull() ?: false,
                                            priority = line.getOrNull(8)?.toIntOrNull() ?: 0,
                                            sortOrder = line.getOrNull(9)?.toIntOrNull() ?: 0,
                                            plannedStart = line.getOrNull(10)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                                            plannedEnd = line.getOrNull(11)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                                            deadline = if (hasDeadline) line.getOrNull(12)?.takeIf { it.isNotBlank() }?.toLongOrNull() else null,
                                            reminderMinutesBefore = if (hasDeadline) line.getOrNull(13)?.takeIf { it.isNotBlank() }?.toIntOrNull() else line.getOrNull(12)?.takeIf { it.isNotBlank() }?.toIntOrNull(),
                                            createdAt = if (hasDeadline) (line.getOrNull(14)?.toLongOrNull() ?: System.currentTimeMillis()) else (line.getOrNull(13)?.toLongOrNull() ?: System.currentTimeMillis()),
                                            completedAt = if (hasDeadline) line.getOrNull(15)?.takeIf { it.isNotBlank() }?.toLongOrNull() else line.getOrNull(14)?.takeIf { it.isNotBlank() }?.toLongOrNull(),
                                            timerActive = if (hasDeadline) line.getOrNull(16)?.toBooleanStrictOrNull() ?: false else line.getOrNull(15)?.toBooleanStrictOrNull() ?: false,
                                            timerStartedAt = if (hasDeadline) line.getOrNull(17)?.takeIf { it.isNotBlank() }?.toLongOrNull() else line.getOrNull(16)?.takeIf { it.isNotBlank() }?.toLongOrNull()
                                        ))
                                    }
                                    "sessions" -> {
                                        if (line.size >= 5) {
                                            sessions.add(TimerSession(
                                                id = line[0].toLongOrNull() ?: 0,
                                                taskId = line[1].toLongOrNull() ?: 0,
                                                startTime = line[2].toLongOrNull() ?: 0,
                                                endTime = line[3].toLongOrNull() ?: 0,
                                                duration = line[4].toLongOrNull() ?: 0,
                                                comment = if (line.size >= 6) line[5] else ""
                                            ))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    csvReader.close()
                }
            }

            database.withTransaction {
                if (replaceAll) {
                    database.timerSessionDao().deleteAll()
                    database.taskDao().deleteAll()
                    database.calendarContextDao().deleteAll()
                    database.contextDao().deleteAll()
                }

                val contextIdMapping = mutableMapOf<Long, Long>()
                val taskIdMapping = mutableMapOf<Long, Long>()

                // 1. Вставляем контексты
                contexts.forEach { oldContext ->
                    val newId = database.contextDao().insertWithId(oldContext)
                    contextIdMapping[oldContext.id] = newId

                    database.calendarContextDao().insert(CalendarContextSetting(
                        contextId = newId,
                        enabled = true
                    ))
                }

                // 2. ПЕРВЫЙ ПРОХОД: Вставляем ВСЕ задачи БЕЗ родительских связей
                tasks.forEach { oldTask ->
                    val newTask = oldTask.copy(
                        contextId = oldTask.contextId?.let { contextIdMapping[it] },
                        parentId = null  // Временно убираем родителя
                    )
                    val newId = database.taskDao().insertWithId(newTask)
                    taskIdMapping[oldTask.id] = newId
                }

                // 3. ВТОРОЙ ПРОХОД: Обновляем родительские связи
                tasks.filter { it.parentId != null }.forEach { oldTask ->
                    val newTaskId = taskIdMapping[oldTask.id]
                    val newParentId = oldTask.parentId?.let { taskIdMapping[it] }
                    if (newTaskId != null && newParentId != null) {
                        database.taskDao().updateParent(newTaskId, newParentId, oldTask.level)
                    }
                }

                // 4. Вставляем сессии
                var sessionsImported = 0
                sessions.forEach { oldSession ->
                    val newTaskId = taskIdMapping[oldSession.taskId]
                    if (newTaskId != null) {
                        val newSession = oldSession.copy(taskId = newTaskId)
                        database.timerSessionDao().insertWithId(newSession)
                        sessionsImported++
                    }
                }

                android.util.Log.d("CsvHelper", "Импортировано: ${contexts.size} контекстов, ${tasks.size} задач, $sessionsImported/${sessions.size} сессий")
            }

            Result.success("Импорт завершён успешно: ${tasks.size} задач")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Ошибка импорта: ${e.message}"))
        }
    }
}