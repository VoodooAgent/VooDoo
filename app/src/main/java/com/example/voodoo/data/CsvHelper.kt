package com.example.voodoo.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
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
                val writer = OutputStreamWriter(outputStream)

                // Экспорт контекстов
                writer.write("# CONTEXTS\n")
                writer.write("id,name,color,sortOrder,createdAt\n")
                contextDao.getAllContextsSync().forEach { ctx ->
                    writer.write("${ctx.id},${escapeCsv(ctx.name)},${ctx.color},${ctx.sortOrder},${ctx.createdAt}\n")
                }

                // Экспорт задач
                writer.write("\n# TASKS\n")
                writer.write("id,contextId,parentId,level,title,description,result,isDone,priority,sortOrder,plannedStart,plannedEnd,reminderMinutesBefore,createdAt,completedAt,timerActive,timerStartedAt\n")
                taskDao.getAllTasksSync().forEach { task ->
                    writer.write("${task.id},${task.contextId ?: ""},${task.parentId ?: ""},${task.level},${escapeCsv(task.title)},${escapeCsv(task.description)},${escapeCsv(task.result)},${task.isDone},${task.priority},${task.sortOrder},${task.plannedStart ?: ""},${task.plannedEnd ?: ""},${task.reminderMinutesBefore ?: ""},${task.createdAt},${task.completedAt ?: ""},${task.timerActive},${task.timerStartedAt ?: ""}\n")
                }

                // Экспорт сессий таймера
                writer.write("\n# TIMER_SESSIONS\n")
                writer.write("id,taskId,startTime,endTime,duration\n")
                timerSessionDao.getAllTimerSessionsSync().forEach { session ->
                    writer.write("${session.id},${session.taskId},${session.startTime},${session.endTime},${session.duration}\n")
                }

                writer.flush()
            }

            Result.success("Экспорт завершён успешно")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromCsv(uri: Uri, replaceAll: Boolean = true): Result<String> = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val contextDao = database.contextDao()
            val taskDao = database.taskDao()
            val timerSessionDao = database.timerSessionDao()

            if (replaceAll) {
                // Очистка существующих данных
                database.clearAllTables()
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                var currentSection = ""
                val contexts = mutableListOf<ProjectContext>()
                val tasks = mutableListOf<Task>()
                val sessions = mutableListOf<TimerSession>()

                for (line in lines) {
                    when {
                        line.startsWith("# CONTEXTS") -> currentSection = "contexts"
                        line.startsWith("# TASKS") -> currentSection = "tasks"
                        line.startsWith("# TIMER_SESSIONS") -> currentSection = "sessions"
                        line.startsWith("id,") -> {} // Пропускаем заголовок
                        line.isNotBlank() -> {
                            val parts = parseCsvLine(line)
                            when (currentSection) {
                                "contexts" -> {
                                    if (parts.size >= 5) {
                                        contexts.add(ProjectContext(
                                            id = parts[0].toLongOrNull() ?: 0,
                                            name = unescapeCsv(parts[1]),
                                            color = parts[2].toLongOrNull() ?: 0xFFE0E0E0,
                                            sortOrder = parts[3].toIntOrNull() ?: 0,
                                            createdAt = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                                        ))
                                    }
                                }
                                "tasks" -> {
                                    if (parts.size >= 17) {
                                        tasks.add(Task(
                                            id = parts[0].toLongOrNull() ?: 0,
                                            contextId = parts[1].toLongOrNull(),
                                            parentId = parts[2].toLongOrNull(),
                                            level = parts[3].toIntOrNull() ?: 0,
                                            title = unescapeCsv(parts[4]),
                                            description = unescapeCsv(parts[5]),
                                            result = unescapeCsv(parts[6]),
                                            isDone = parts[7].toBooleanStrictOrNull() ?: false,
                                            priority = parts[8].toIntOrNull() ?: 0,
                                            sortOrder = parts[9].toIntOrNull() ?: 0,
                                            plannedStart = parts[10].toLongOrNull(),
                                            plannedEnd = parts[11].toLongOrNull(),
                                            reminderMinutesBefore = parts[12].toIntOrNull(),
                                            createdAt = parts[13].toLongOrNull() ?: System.currentTimeMillis(),
                                            completedAt = parts[14].toLongOrNull(),
                                            timerActive = parts[15].toBooleanStrictOrNull() ?: false,
                                            timerStartedAt = parts[16].toLongOrNull()
                                        ))
                                    }
                                }
                                "sessions" -> {
                                    if (parts.size >= 5) {
                                        sessions.add(TimerSession(
                                            id = parts[0].toLongOrNull() ?: 0,
                                            taskId = parts[1].toLongOrNull() ?: 0,
                                            startTime = parts[2].toLongOrNull() ?: 0,
                                            endTime = parts[3].toLongOrNull() ?: 0,
                                            duration = parts[4].toLongOrNull() ?: 0
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }

                // Импортируем данные в правильном порядке
                contexts.forEach { contextDao.insert(it) }
                tasks.forEach { taskDao.insert(it) }
                sessions.forEach { timerSessionDao.insert(it) }
            }

            Result.success("Импорт завершён успешно")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(text: String): String {
        return if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            "\"${text.replace("\"", "\"\"")}\""
        } else {
            text
        }
    }

    private fun unescapeCsv(text: String): String {
        return if (text.startsWith("\"") && text.endsWith("\"")) {
            text.substring(1, text.length - 1).replace("\"\"", "\"")
        } else {
            text
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                char == '"' -> inQuotes = !inQuotes
                else -> current.append(char)
            }
        }
        result.add(current.toString())

        return result
    }
}