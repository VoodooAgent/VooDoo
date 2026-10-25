package com.example.voodoo.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class CsvBackupFormatTest {

    private val sample = CsvBackup(
        contexts = listOf(
            ProjectContext(id = 1, name = "Работа", color = 0xFF112233, sortOrder = 10, createdAt = 1000),
            ProjectContext(id = 2, name = "Дом, \"дача\"", color = 0xFF445566, sortOrder = 20, createdAt = 2000)
        ),
        tasks = listOf(
            Task(id = 10, contextId = 1, title = "Родитель", description = "многострочное\nописание",
                sortOrder = 10000, createdAt = 3000, deadline = 5000, reminderMinutesBefore = 15),
            Task(id = 11, contextId = 1, parentId = 10, level = 1, title = "Подзадача", isDone = true,
                completedAt = 4000, createdAt = 3500, timerActive = true, timerStartedAt = 4500),
            Task(id = 12, title = "Рутина", priority = 4, routineFrequency = "weekly",
                routineStartMinutes = 480, routineEndMinutes = 540, routineDayOfWeek = 3, createdAt = 6000)
        ),
        sessions = listOf(
            TimerSession(id = 100, taskId = 10, startTime = 1, endTime = 61_000, duration = 60_000, comment = "звонок"),
            TimerSession(id = 101, taskId = 11, startTime = 2, endTime = 3, duration = 1)
        )
    )

    private fun roundTrip(backup: CsvBackup): CsvBackup {
        val writer = StringWriter()
        CsvBackupFormat.write(writer, backup)
        return CsvBackupFormat.parse(StringReader(writer.toString()))
    }

    private fun assertRejected(csv: String, messagePart: String) {
        try {
            CsvBackupFormat.parse(StringReader(csv))
            fail("Ожидалась CsvFormatException")
        } catch (e: CsvFormatException) {
            assertTrue("Сообщение «${e.message}» не содержит «$messagePart»", e.message!!.contains(messagePart))
        }
    }

    @Test
    fun exportThenImport_returnsSameData() {
        val parsed = roundTrip(sample)
        assertEquals(sample.contexts, parsed.contexts)
        assertEquals(sample.tasks, parsed.tasks)
        assertEquals(sample.sessions, parsed.sessions)
    }

    @Test
    fun blankSeparatorLines_doNotCreateEmptyTasks() {
        // Раньше пустая строка перед # TIMER_SESSIONS превращалась в задачу без названия
        val parsed = roundTrip(sample)
        assertEquals(3, parsed.tasks.size)
        assertTrue(parsed.tasks.none { it.title.isEmpty() })
    }

    @Test
    fun emptyDatabase_roundTripsAsValidFile() {
        val parsed = roundTrip(CsvBackup(emptyList(), emptyList(), emptyList()))
        assertEquals(0, parsed.tasks.size)
    }

    @Test
    fun foreignCsv_isRejected() {
        assertRejected("name,amount\nкофе,100\nчай,50\n", "не похож на экспорт VooDoo")
    }

    @Test
    fun emptyFile_isRejected() {
        assertRejected("", "нет секций")
    }

    @Test
    fun fileWithoutTasksSection_isRejected() {
        assertRejected("\"# CONTEXTS\"\n\"id\",\"name\",\"color\",\"sortOrder\",\"createdAt\"\n\"1\",\"A\",\"0\",\"0\",\"0\"\n", "нет секций")
    }

    @Test
    fun truncatedTaskRow_isRejected() {
        val csv = "# CONTEXTS\n# TASKS\n10,,,0,Обрезанная строка\n"
        assertRejected(csv, "Строка 3")
    }

    @Test
    fun nonNumericId_isRejected() {
        val row = (listOf("abc", "", "", "0", "T") + List(18) { "" }).joinToString(",")
        assertRejected("# CONTEXTS\n# TASKS\n$row\n", "не является id")
    }

    @Test
    fun duplicateTaskIds_areRejected() {
        val row = (listOf("5", "", "", "0", "T") + List(18) { "" }).joinToString(",")
        assertRejected("# CONTEXTS\n# TASKS\n$row\n$row\n", "повторяются id задач")
    }

    @Test
    fun oldFormatWithoutDeadline_isParsedWithShiftedColumns() {
        // 17 колонок: ... plannedEnd, reminderMinutesBefore, createdAt, completedAt, timerActive, timerStartedAt
        val row = listOf("7", "", "", "0", "Старая", "", "", "true", "2", "100", "", "", "30", "1234", "5678", "false", "")
        val parsed = CsvBackupFormat.parse(StringReader("# CONTEXTS\n# TASKS\n${row.joinToString(",")}\n"))
        val task = parsed.tasks.single()
        assertNull(task.deadline)
        assertEquals(30, task.reminderMinutesBefore)
        assertEquals(1234L, task.createdAt)
        assertEquals(5678L, task.completedAt)
        assertEquals(true, task.isDone)
        assertEquals(2, task.priority)
        assertNull(task.routineFrequency)
    }
}
