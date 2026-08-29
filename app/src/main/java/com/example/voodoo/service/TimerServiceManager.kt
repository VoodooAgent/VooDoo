package com.example.voodoo.service

import android.content.Context
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object TimerServiceManager {

    private const val MAX_CONCURRENT_TIMERS = 7

    suspend fun startTimer(context: Context, taskId: Long) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()

        val activeTimers = withContext(Dispatchers.IO) {
            taskDao.getActiveTimerTasks()
        }

        if (activeTimers.size >= MAX_CONCURRENT_TIMERS) {
            throw IllegalStateException("Превышен лимит одновременных таймеров (максимум $MAX_CONCURRENT_TIMERS)")
        }

        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            taskDao.updateTimerStatus(taskId, true, now)
        }
    }

    suspend fun pauseTimer(context: Context, taskId: Long) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()
        val sessionDao = db.timerSessionDao()

        val task = withContext(Dispatchers.IO) {
            taskDao.getTaskById(taskId).first()
        }

        task?.let { currentTask ->
            if (currentTask.timerActive && currentTask.timerStartedAt != null) {
                val now = System.currentTimeMillis()
                val duration = now - currentTask.timerStartedAt

                val session = TimerSession(
                    taskId = taskId,
                    startTime = currentTask.timerStartedAt,
                    endTime = now,
                    duration = duration
                )

                withContext(Dispatchers.IO) {
                    sessionDao.insert(session)
                    taskDao.updateTimerStatus(taskId, false, null)
                }
            }
        }
    }

    suspend fun stopAllTimersForTask(context: Context, taskId: Long) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()

        withContext(Dispatchers.IO) {
            taskDao.updateTimerStatus(taskId, false, null)
        }
    }

    suspend fun restoreTimers(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val taskDao = db.taskDao()

        withContext(Dispatchers.IO) {
            val activeTasks = taskDao.getActiveTimerTasks()
            // Таймеры продолжают работать через startTime в БД
        }
    }

    fun getTimerDuration(task: Task): Long {
        return task.timerStartedAt?.let {
            System.currentTimeMillis() - it
        } ?: 0L
    }
}