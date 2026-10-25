package com.example.voodoo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataSafetyTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.resetInstanceForTests()
    }

    @Test
    fun subtreeQueries_countDescendantsSessionsAndRunningTimers() = runTest {
        val taskDao = db.taskDao()
        val sessionDao = db.timerSessionDao()
        taskDao.insertWithId(Task(id = 1, title = "Корень"))
        taskDao.insertWithId(Task(id = 2, parentId = 1, level = 1, title = "Ребёнок", isDone = true))
        taskDao.insertWithId(Task(id = 3, parentId = 2, level = 2, title = "Внук", timerActive = true, timerStartedAt = 1))
        taskDao.insertWithId(Task(id = 4, title = "Чужая"))
        sessionDao.insert(TimerSession(taskId = 1, startTime = 0, endTime = 0, duration = 60_000))
        sessionDao.insert(TimerSession(taskId = 3, startTime = 0, endTime = 0, duration = 120_000))
        sessionDao.insert(TimerSession(taskId = 4, startTime = 0, endTime = 0, duration = 999_000))

        assertEquals(2, taskDao.countAllDescendants(1))
        assertEquals(0, taskDao.countAllDescendants(4))
        assertEquals(1, taskDao.countActiveTimersInSubtree(1))
        assertEquals(0, taskDao.countActiveTimersInSubtree(4))
        assertEquals(SessionStats(count = 2, totalDuration = 180_000), sessionDao.getSubtreeSessionStats(1))
        assertEquals(SessionStats(count = 1, totalDuration = 120_000), sessionDao.getSubtreeSessionStats(2))
    }

    @Test
    fun replaceAll_savesAutoBackupOfPreviousData() = runTest {
        val helper = CsvHelper(context)
        val appDb = AppDatabase.getDatabase(context)
        appDb.contextDao().insertWithId(ProjectContext(id = 1, name = "Старый контекст"))
        appDb.taskDao().insertWithId(Task(id = 1, contextId = 1, title = "Старая задача"))

        val incoming = CsvBackup(
            contexts = listOf(ProjectContext(id = 5, name = "Новый")),
            tasks = listOf(Task(id = 7, contextId = 5, title = "Новая")),
            sessions = emptyList()
        )
        assertTrue(helper.replaceAll(incoming).isSuccess)

        assertEquals(listOf("Новая"), appDb.taskDao().getAllTasksSync().map { it.title })

        val backups = helper.listAutoBackups()
        assertEquals(1, backups.size)
        val saved = helper.readCsv(backups.first()).getOrThrow()
        assertEquals(listOf("Старая задача"), saved.tasks.map { it.title })
        assertEquals(listOf("Старый контекст"), saved.contexts.map { it.name })

        // Хранится не больше MAX_AUTO_BACKUPS копий
        repeat(CsvHelper.MAX_AUTO_BACKUPS + 2) { i ->
            backups.first().parentFile!!.resolve("voodoo_auto_2000-01-0${i % 9 + 1}_00-00-0${i % 10}_import.csv").writeText("x")
        }
        helper.createAutoBackup("import")
        assertEquals(CsvHelper.MAX_AUTO_BACKUPS, helper.listAutoBackups().size)
    }
}
