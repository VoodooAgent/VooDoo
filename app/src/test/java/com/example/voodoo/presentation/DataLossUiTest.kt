package com.example.voodoo.presentation

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextReplacement
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import com.example.voodoo.presentation.screens.TaskDetailScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataLossUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val db get() = AppDatabase.getDatabase(compose.activity.application)

    @Before
    fun setUp() = runBlocking {
        // Свежая база на каждый тест. Закрывать её в @After нельзя: экран
        // ещё сохраняет задачу по ON_STOP, когда правило закрывает Activity.
        AppDatabase.resetInstanceForTests()
        db.taskDao().insertWithId(Task(id = 1, title = "Задача", description = "старое описание"))
        db.taskDao().insertWithId(Task(id = 2, parentId = 1, level = 1, title = "Подзадача"))
        db.timerSessionDao().insert(TimerSession(taskId = 2, startTime = 0, endTime = 0, duration = 3_600_000))
        Unit
    }

    // Ждёт, прокручивая главный поток: корутины ViewModel возобновляются на нём
    private fun awaitMain(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Условие не выполнилось за 5 с" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }

    private fun task(id: Long): Task? = runBlocking { db.taskDao().getTaskById(id).first() }

    @Test
    fun systemBack_onTaskDetail_savesEdits() {
        var closed = false
        compose.setContent {
            TaskDetailScreen(taskId = 1, onBackClick = { closed = true })
        }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2
        }
        // Поля по порядку: название, описание, результат
        compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("новое описание")
        compose.waitForIdle()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        compose.waitUntil(5_000) { task(1)?.description == "новое описание" }
        assertTrue("Экран должен закрыться", closed)
    }

    @Test
    fun deleteTask_waitsForConfirmation_andReportsWhatWillBeLost() {
        val viewModel = TaskListViewModel(compose.activity.application)
        val root = task(1)!!

        viewModel.deleteTask(root)
        awaitMain { viewModel.pendingDeletion.value != null }

        val pending = viewModel.pendingDeletion.value!!
        assertEquals(1, pending.descendantCount)
        assertEquals(1, pending.sessionCount)
        assertEquals(3_600_000L, pending.trackedMillis)
        assertNotNull("До подтверждения задача не удаляется", task(1))

        viewModel.cancelDelete()
        assertNotNull(task(1))

        viewModel.deleteTask(root)
        awaitMain { viewModel.pendingDeletion.value != null }
        viewModel.confirmDelete()
        awaitMain { task(1) == null && task(2) == null }
    }
}
