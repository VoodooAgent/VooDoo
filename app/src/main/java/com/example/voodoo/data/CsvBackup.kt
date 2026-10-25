package com.example.voodoo.data

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.Reader
import java.io.Writer

/**
 * Содержимое CSV-экспорта VooDoo. Разбирается целиком до того, как что-либо
 * удаляется из базы, чтобы неподходящий файл не стирал данные.
 */
data class CsvBackup(
    val contexts: List<ProjectContext>,
    val tasks: List<Task>,
    val sessions: List<TimerSession>
)

class CsvFormatException(message: String) : Exception(message)

object CsvBackupFormat {

    private const val SECTION_CONTEXTS = "# CONTEXTS"
    private const val SECTION_TASKS = "# TASKS"
    private const val SECTION_SESSIONS = "# TIMER_SESSIONS"

    // Форматы задач: 17 колонок (старый, без deadline), 18 (с deadline), 23 (с рутиной)
    private const val MIN_TASK_COLUMNS = 17

    fun write(writer: Writer, backup: CsvBackup) {
        val csvWriter = CSVWriter(writer)

        csvWriter.writeNext(arrayOf(SECTION_CONTEXTS))
        csvWriter.writeNext(arrayOf("id", "name", "color", "sortOrder", "createdAt"))
        backup.contexts.forEach { ctx ->
            csvWriter.writeNext(arrayOf(
                ctx.id.toString(),
                ctx.name,
                ctx.color.toString(),
                ctx.sortOrder.toString(),
                ctx.createdAt.toString()
            ))
        }

        csvWriter.writeNext(arrayOf(""))
        csvWriter.writeNext(arrayOf(SECTION_TASKS))
        csvWriter.writeNext(arrayOf(
            "id", "contextId", "parentId", "level", "title", "description",
            "result", "isDone", "priority", "sortOrder", "plannedStart",
            "plannedEnd", "deadline", "reminderMinutesBefore", "createdAt", "completedAt",
            "timerActive", "timerStartedAt",
            "routineFrequency", "routineStartMinutes", "routineEndMinutes",
            "routineDayOfWeek", "routineDayOfMonth"
        ))
        backup.tasks.forEach { task ->
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
                task.timerStartedAt?.toString() ?: "",
                task.routineFrequency?.toString() ?: "",
                task.routineStartMinutes?.toString() ?: "",
                task.routineEndMinutes?.toString() ?: "",
                task.routineDayOfWeek?.toString() ?: "",
                task.routineDayOfMonth?.toString() ?: ""
            ))
        }

        csvWriter.writeNext(arrayOf(""))
        csvWriter.writeNext(arrayOf(SECTION_SESSIONS))
        csvWriter.writeNext(arrayOf("id", "taskId", "startTime", "endTime", "duration", "comment"))
        backup.sessions.forEach { session ->
            csvWriter.writeNext(arrayOf(
                session.id.toString(),
                session.taskId.toString(),
                session.startTime.toString(),
                session.endTime.toString(),
                session.duration.toString(),
                session.comment
            ))
        }

        csvWriter.flush()
    }

    /**
     * Разбирает файл и проверяет, что это действительно экспорт VooDoo.
     * При любой проблеме бросает [CsvFormatException] с понятным сообщением.
     */
    fun parse(reader: Reader): CsvBackup {
        val contexts = mutableListOf<ProjectContext>()
        val tasks = mutableListOf<Task>()
        val sessions = mutableListOf<TimerSession>()

        var currentSection = ""
        var hasContextsSection = false
        var hasTasksSection = false
        var lineNumber = 0

        val csvReader = CSVReader(reader)
        csvReader.use {
            it.forEach { line ->
                lineNumber++
                // Пустые строки-разделители между секциями
                if (line.isEmpty() || line.all { cell -> cell.isBlank() }) return@forEach
                when (line[0].trim()) {
                    SECTION_CONTEXTS -> { currentSection = "contexts"; hasContextsSection = true; return@forEach }
                    SECTION_TASKS -> { currentSection = "tasks"; hasTasksSection = true; return@forEach }
                    SECTION_SESSIONS -> { currentSection = "sessions"; return@forEach }
                    "id" -> return@forEach
                }
                when (currentSection) {
                    "contexts" -> contexts.add(parseContext(line, lineNumber))
                    "tasks" -> tasks.add(parseTask(line, lineNumber))
                    "sessions" -> sessions.add(parseSession(line, lineNumber))
                    else -> throw CsvFormatException(
                        "Файл не похож на экспорт VooDoo: строка $lineNumber находится вне секций # CONTEXTS / # TASKS"
                    )
                }
            }
        }

        if (!hasContextsSection || !hasTasksSection) {
            throw CsvFormatException(
                "Файл не похож на экспорт VooDoo: нет секций # CONTEXTS и # TASKS"
            )
        }

        val contextIds = contexts.map { it.id }
        if (contextIds.size != contextIds.toSet().size) {
            throw CsvFormatException("В файле повторяются id контекстов")
        }
        val taskIds = tasks.map { it.id }
        if (taskIds.size != taskIds.toSet().size) {
            throw CsvFormatException("В файле повторяются id задач")
        }

        return CsvBackup(contexts, tasks, sessions)
    }

    private fun parseContext(line: Array<String>, lineNumber: Int): ProjectContext {
        if (line.size < 5) {
            throw CsvFormatException("Строка $lineNumber: у контекста ${line.size} колонок вместо 5")
        }
        return ProjectContext(
            id = requireId(line[0], lineNumber),
            name = line[1],
            color = line[2].toLongOrNull() ?: 0xFFE0E0E0,
            sortOrder = line[3].toIntOrNull() ?: 0,
            createdAt = line[4].toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    private fun parseTask(line: Array<String>, lineNumber: Int): Task {
        if (line.size < MIN_TASK_COLUMNS) {
            throw CsvFormatException(
                "Строка $lineNumber: у задачи ${line.size} колонок, ожидалось не меньше $MIN_TASK_COLUMNS"
            )
        }
        val hasDeadline = line.size >= 18
        val hasRoutine = line.size >= 23
        // В старом формате (17 колонок) после plannedEnd сразу идёт reminderMinutesBefore
        val shift = if (hasDeadline) 0 else -1
        fun cell(index: Int) = line.getOrNull(index)?.takeIf { it.isNotBlank() }

        return Task(
            id = requireId(line[0], lineNumber),
            contextId = cell(1)?.toLongOrNull(),
            parentId = cell(2)?.toLongOrNull(),
            level = cell(3)?.toIntOrNull() ?: 0,
            title = line[4],
            description = line[5],
            result = line[6],
            isDone = cell(7)?.toBooleanStrictOrNull() ?: false,
            priority = cell(8)?.toIntOrNull() ?: 0,
            sortOrder = cell(9)?.toIntOrNull() ?: 0,
            plannedStart = cell(10)?.toLongOrNull(),
            plannedEnd = cell(11)?.toLongOrNull(),
            deadline = if (hasDeadline) cell(12)?.toLongOrNull() else null,
            reminderMinutesBefore = cell(13 + shift)?.toIntOrNull(),
            createdAt = cell(14 + shift)?.toLongOrNull() ?: System.currentTimeMillis(),
            completedAt = cell(15 + shift)?.toLongOrNull(),
            timerActive = cell(16 + shift)?.toBooleanStrictOrNull() ?: false,
            timerStartedAt = cell(17 + shift)?.toLongOrNull(),
            routineFrequency = if (hasRoutine) cell(18) else null,
            routineStartMinutes = if (hasRoutine) cell(19)?.toIntOrNull() else null,
            routineEndMinutes = if (hasRoutine) cell(20)?.toIntOrNull() else null,
            routineDayOfWeek = if (hasRoutine) cell(21)?.toIntOrNull() else null,
            routineDayOfMonth = if (hasRoutine) cell(22)?.toIntOrNull() else null
        )
    }

    private fun parseSession(line: Array<String>, lineNumber: Int): TimerSession {
        if (line.size < 5) {
            throw CsvFormatException("Строка $lineNumber: у сессии ${line.size} колонок вместо 6")
        }
        return TimerSession(
            id = requireId(line[0], lineNumber),
            taskId = requireId(line[1], lineNumber),
            startTime = line[2].toLongOrNull() ?: 0,
            endTime = line[3].toLongOrNull() ?: 0,
            duration = line[4].toLongOrNull() ?: 0,
            comment = line.getOrNull(5) ?: ""
        )
    }

    private fun requireId(value: String, lineNumber: Int): Long =
        value.trim().toLongOrNull()
            ?: throw CsvFormatException("Строка $lineNumber: «$value» не является id")
}
