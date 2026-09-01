package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.VooDooApp
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import com.example.voodoo.service.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val sessionDao = database.timerSessionDao()
    private val contextDao = database.contextDao()

    private val _task = MutableStateFlow<Task?>(null)
    val task: StateFlow<Task?> = _task.asStateFlow()

    private val _sessions = MutableStateFlow<List<TimerSession>>(emptyList())
    val sessions: StateFlow<List<TimerSession>> = _sessions.asStateFlow()

    // ГЛОБАЛЬНЫЙ список задач (для поиска родителя и выбора нового родителя)
    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val contexts: StateFlow<List<ProjectContext>> = contextDao.getAllContexts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadTask(taskId: Long) {
        viewModelScope.launch {
            taskDao.getTaskById(taskId).collect { t ->
                _task.value = t
            }
        }
        viewModelScope.launch {
            sessionDao.getSessionsByTask(taskId).collect { s ->
                _sessions.value = s
            }
        }
    }

    fun updateTaskDetails(
        description: String,
        result: String,
        plannedStart: Long?,
        plannedEnd: Long?,
        reminderMinutesBefore: Int?
    ) {
        viewModelScope.launch {
            _task.value?.let { currentTask ->
                val updated = currentTask.copy(
                    description = description,
                    result = result,
                    plannedStart = plannedStart,
                    plannedEnd = plannedEnd,
                    reminderMinutesBefore = reminderMinutesBefore
                )
                withContext(Dispatchers.IO) {
                    taskDao.update(updated)
                }
                ReminderScheduler.scheduleReminder(getApplication<VooDooApp>(), updated)
            }
        }
    }

    // ============ СЕССИИ ТАЙМЕРА ============

    fun addSession(taskId: Long, startTime: Long, endTime: Long, comment: String) {
        viewModelScope.launch {
            val duration = (endTime - startTime).coerceAtLeast(0L)
            val session = TimerSession(
                taskId = taskId,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                comment = comment
            )
            withContext(Dispatchers.IO) {
                sessionDao.insert(session)
            }
        }
    }

    fun updateSession(session: TimerSession, startTime: Long, endTime: Long, comment: String) {
        viewModelScope.launch {
            val duration = (endTime - startTime).coerceAtLeast(0L)
            val updated = session.copy(
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                comment = comment
            )
            withContext(Dispatchers.IO) {
                sessionDao.update(updated)
            }
        }
    }

    fun deleteSession(session: TimerSession) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionDao.delete(session)
            }
        }
    }

    // ============ СМЕНА КОНТЕКСТА ============

    fun updateContext(newContextId: Long?) {
        viewModelScope.launch {
            val current = _task.value ?: return@launch
            withContext(Dispatchers.IO) {
                val all = taskDao.getAllTasks().first()
                val descendants = collectDescendants(current.id, all)
                taskDao.updateContext(current.id, newContextId)
                descendants.forEach { id ->
                    taskDao.updateContext(id, newContextId)
                }
            }
        }
    }

    // ============ СМЕНА РОДИТЕЛЯ ============

    fun updateParent(newParentId: Long?) {
        viewModelScope.launch {
            val current = _task.value ?: return@launch
            withContext(Dispatchers.IO) {
                if (newParentId == null) {
                    taskDao.updateParent(current.id, null, 0)
                    relevelDescendants(current.id)
                    return@withContext
                }
                if (newParentId == current.id) return@withContext

                val all = taskDao.getAllTasks().first()
                val descendants = collectDescendants(current.id, all)

                // Защита от зацикливания: родитель не может быть потомком
                if (descendants.contains(newParentId)) return@withContext

                val parent = all.find { it.id == newParentId } ?: return@withContext

                // Проверка лимита 7 уровней с учётом глубины своего поддерева
                val depth = subtreeDepth(current.id, all)
                val newLevel = parent.level + 1
                if (newLevel + depth > 6) return@withContext

                taskDao.updateParent(current.id, newParentId, newLevel)
                relevelDescendants(current.id)
            }
        }
    }

    // ============ ДОПУСТИМЫЕ РОДИТЕЛИ ДЛЯ ДИАЛОГА ============

    fun eligibleParents(taskId: Long, all: List<Task>): List<Task> {
        val desc = collectDescendants(taskId, all).toSet()
        val depth = subtreeDepth(taskId, all)
        return all.filter { t ->
            t.id != taskId && !desc.contains(t.id) && (t.level + 1 + depth <= 6)
        }.sortedWith(compareBy({ it.contextId ?: -1L }, { it.sortOrder }, { it.id }))
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ ============

    private fun collectDescendants(rootId: Long, all: List<Task>): List<Long> {
        val byParent = all.groupBy { it.parentId }
        val result = mutableListOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.addLast(rootId)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            byParent[id]?.forEach { child ->
                result.add(child.id)
                queue.addLast(child.id)
            }
        }
        return result
    }

    private fun subtreeDepth(rootId: Long, all: List<Task>): Int {
        val byParent = all.groupBy { it.parentId }
        fun depth(id: Long): Int {
            val kids = byParent[id] ?: return 0
            return 1 + kids.maxOf { depth(it.id) }
        }
        return depth(rootId)
    }

    private suspend fun relevelDescendants(rootId: Long) {
        val all = taskDao.getAllTasks().first()
        val byParent = all.groupBy { it.parentId }
        val root = all.find { it.id == rootId } ?: return

        val queue = ArrayDeque<Pair<Task, Int>>()
        queue.addLast(root to root.level)
        while (queue.isNotEmpty()) {
            val (node, lvl) = queue.removeFirst()
            byParent[node.id]?.forEach { child ->
                if (child.level != lvl + 1) {
                    taskDao.updateParent(child.id, child.parentId, lvl + 1)
                }
                queue.addLast(child to (lvl + 1))
            }
        }
    }

    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        return String.format("%02d:%02d", hours, minutes)
    }
}