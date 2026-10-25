package com.example.voodoo.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvHelper(private val context: Context) {

    private val database get() = AppDatabase.getDatabase(context)

    suspend fun exportToCsv(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val backup = readDatabase()
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть файл для записи")
            stream.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    CsvBackupFormat.write(writer, backup)
                }
            }
            Result.success("Экспорт завершён успешно")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Разбирает файл, ничего не меняя в базе. */
    suspend fun readCsv(uri: Uri): Result<CsvBackup> = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть файл")
            val backup = stream.use { inputStream ->
                InputStreamReader(inputStream).use { reader -> CsvBackupFormat.parse(reader) }
            }
            Result.success(backup)
        } catch (e: CsvFormatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось прочитать файл: ${e.message}", e))
        }
    }

    suspend fun readCsv(file: File): Result<CsvBackup> = withContext(Dispatchers.IO) {
        try {
            Result.success(file.reader().use { CsvBackupFormat.parse(it) })
        } catch (e: CsvFormatException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось прочитать файл: ${e.message}", e))
        }
    }

    /**
     * Заменяет всю базу содержимым [backup]. Перед заменой сохраняет
     * текущие данные в автоматическую резервную копию.
     */
    suspend fun replaceAll(backup: CsvBackup): Result<String> = withContext(Dispatchers.IO) {
        try {
            val autoBackup = createAutoBackup("import")
            val sessionsImported = database.withTransaction {
                database.timerSessionDao().deleteAll()
                database.taskDao().deleteAll()
                database.calendarContextDao().deleteAll()
                database.contextDao().deleteAll()
                insertBackup(backup)
            }
            android.util.Log.d(
                "CsvHelper",
                "Импортировано: ${backup.contexts.size} контекстов, ${backup.tasks.size} задач, " +
                    "$sessionsImported/${backup.sessions.size} сессий"
            )
            Result.success(
                "Импорт завершён: ${backup.contexts.size} контекстов, ${backup.tasks.size} задач. " +
                    "Прежние данные сохранены в копию ${autoBackup.name}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Ошибка импорта, данные не изменены: ${e.message}", e))
        }
    }

    /** Вставляет данные; возвращает число вставленных сессий. Вызывать внутри транзакции. */
    private suspend fun insertBackup(backup: CsvBackup): Int {
        val contextIdMapping = mutableMapOf<Long, Long>()
        val taskIdMapping = mutableMapOf<Long, Long>()

        // 1. Вставляем контексты
        backup.contexts.forEach { oldContext ->
            val newId = database.contextDao().insertWithId(oldContext)
            contextIdMapping[oldContext.id] = newId

            database.calendarContextDao().insert(CalendarContextSetting(
                contextId = newId,
                enabled = true
            ))
        }

        // 2. ПЕРВЫЙ ПРОХОД: Вставляем ВСЕ задачи БЕЗ родительских связей
        backup.tasks.forEach { oldTask ->
            val newTask = oldTask.copy(
                contextId = oldTask.contextId?.let { contextIdMapping[it] },
                parentId = null  // Временно убираем родителя
            )
            val newId = database.taskDao().insertWithId(newTask)
            taskIdMapping[oldTask.id] = newId
        }

        // 3. ВТОРОЙ ПРОХОД: Обновляем родительские связи
        backup.tasks.filter { it.parentId != null }.forEach { oldTask ->
            val newTaskId = taskIdMapping[oldTask.id]
            val newParentId = oldTask.parentId?.let { taskIdMapping[it] }
            if (newTaskId != null && newParentId != null) {
                database.taskDao().updateParent(newTaskId, newParentId, oldTask.level)
            }
        }

        // 4. Вставляем сессии
        var sessionsImported = 0
        backup.sessions.forEach { oldSession ->
            val newTaskId = taskIdMapping[oldSession.taskId]
            if (newTaskId != null) {
                database.timerSessionDao().insertWithId(oldSession.copy(taskId = newTaskId))
                sessionsImported++
            }
        }
        return sessionsImported
    }

    private suspend fun readDatabase(): CsvBackup = CsvBackup(
        contexts = database.contextDao().getAllContextsSync(),
        tasks = database.taskDao().getAllTasksSync(),
        sessions = database.timerSessionDao().getAllTimerSessionsSync()
    )

    /**
     * Сохраняет всю базу в файл в папке приложения и оставляет
     * последние [MAX_AUTO_BACKUPS] копий.
     */
    suspend fun createAutoBackup(reason: String): File = withContext(Dispatchers.IO) {
        val dir = backupDir()
        dir.mkdirs()
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "voodoo_auto_${stamp}_$reason.csv")
        file.writer().use { CsvBackupFormat.write(it, readDatabase()) }
        listAutoBackups().drop(MAX_AUTO_BACKUPS).forEach { it.delete() }
        file
    }

    /** Автоматические копии, самые свежие первыми. */
    fun listAutoBackups(): List<File> =
        backupDir().listFiles { f -> f.isFile && f.name.startsWith("voodoo_auto_") && f.name.endsWith(".csv") }
            ?.sortedByDescending { it.name }
            ?: emptyList()

    private fun backupDir(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")

    companion object {
        const val MAX_AUTO_BACKUPS = 7
    }
}
