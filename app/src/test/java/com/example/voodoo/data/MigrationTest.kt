package com.example.voodoo.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun everySupportedVersion_hasMigrationPathToCurrent() {
        // Если поднять VERSION и забыть миграцию, этот тест упадёт раньше, чем приложение у пользователя
        val byStart = AppDatabase.ALL_MIGRATIONS.associateBy { it.startVersion }
        for (start in 4 until AppDatabase.VERSION) {
            var version = start
            while (version < AppDatabase.VERSION) {
                val migration = byStart[version] ?: error("Нет миграции с версии $version (путь от $start)")
                version = migration.endVersion
            }
            assertEquals(AppDatabase.VERSION, version)
        }
    }

    @Test
    fun migrate11To12_keepsTasksAndAddsRoutineDayColumns() = runTest {
        createVersion11Database()

        val db = AppDatabase.buildDatabase(context, dbName)
        try {
            val task = db.taskDao().getTaskById(1).first()!!
            assertEquals("Задача из v11", task.title)
            assertEquals(1L, task.contextId)
            assertEquals("daily", task.routineFrequency)
            assertNull(task.routineDayOfWeek)
            assertNull(task.routineDayOfMonth)
            assertEquals(1, db.timerSessionDao().getAllTimerSessionsSync().size)
            assertEquals("Работа", db.contextDao().getAllContextsSync().single().name)
        } finally {
            db.close()
        }
    }

    @Test
    fun missingMigration_failsInsteadOfWipingData() {
        // База версии 3: миграций с неё нет. Раньше fallbackToDestructiveMigration молча стирал всё.
        val path = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { raw ->
            raw.execSQL("CREATE TABLE contexts (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
            raw.execSQL("INSERT INTO contexts (id, name) VALUES (1, 'Важное')")
            raw.version = 3
        }

        val db = AppDatabase.buildDatabase(context, dbName)
        try {
            db.openHelper.writableDatabase
            fail("Ожидалось исключение об отсутствующей миграции")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("migration", ignoreCase = true))
        } finally {
            db.close()
        }

        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            assertEquals(3, raw.version)
            raw.rawQuery("SELECT name FROM contexts", null).use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Важное", c.getString(0))
            }
        }
    }

    /** Схема версии 11: как 12, но без routineDayOfWeek и routineDayOfMonth (их добавляет MIGRATION_11_12). */
    private fun createVersion11Database() {
        val path = context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `contexts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `color` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `isHidden` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contextId` INTEGER, `parentId` INTEGER, `level` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `result` TEXT NOT NULL, `isDone` INTEGER NOT NULL, `priority` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `prioritySortOrder` INTEGER, `routineSortOrder` INTEGER, `activeSortOrder` INTEGER, `plannedStart` INTEGER, `plannedEnd` INTEGER, `deadline` INTEGER, `routineFrequency` TEXT, `routineStartMinutes` INTEGER, `routineEndMinutes` INTEGER, `reminderMinutesBefore` INTEGER, `createdAt` INTEGER NOT NULL, `completedAt` INTEGER, `timerActive` INTEGER NOT NULL, `timerStartedAt` INTEGER, FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`parentId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_contextId` ON `tasks` (`contextId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentId` ON `tasks` (`parentId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_level` ON `tasks` (`level`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sortOrder` ON `tasks` (`sortOrder`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isDone` ON `tasks` (`isDone`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `timer_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `comment` TEXT NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timer_sessions_taskId` ON `timer_sessions` (`taskId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `darkTheme` INTEGER NOT NULL, `fontSize` INTEGER NOT NULL, `noContextName` TEXT NOT NULL, `showTasks` INTEGER NOT NULL, `showSessions` INTEGER NOT NULL, `taskSortMode` TEXT NOT NULL, `groupPriority` INTEGER NOT NULL, `groupRoutine` INTEGER NOT NULL, `groupActive` INTEGER NOT NULL, `showCompletedInSwipe` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `ical_sync_settings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contextId` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ical_sync_settings_contextId` ON `ical_sync_settings` (`contextId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `calendar_context_settings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contextId` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, FOREIGN KEY(`contextId`) REFERENCES `contexts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_context_settings_contextId` ON `calendar_context_settings` (`contextId`)")

            db.execSQL("INSERT INTO contexts (id, name, color, sortOrder, isHidden, createdAt) VALUES (1, 'Работа', 0, 0, 0, 0)")
            db.execSQL("INSERT INTO tasks (id, contextId, level, title, description, result, isDone, priority, sortOrder, routineFrequency, createdAt, timerActive) VALUES (1, 1, 0, 'Задача из v11', '', '', 0, 4, 10000, 'daily', 0, 0)")
            db.execSQL("INSERT INTO timer_sessions (taskId, startTime, endTime, duration, comment) VALUES (1, 0, 60000, 60000, '')")
            db.version = 11
        }
    }
}
