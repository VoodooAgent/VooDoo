package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.VooDooApp
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.service.ReminderScheduler
import com.example.voodoo.service.TimerServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val contextDao = database.contextDao()

    private val _selectedContextId = MutableStateFlow<Long?>(null)
    val selectedContextId: StateFlow<Long?> = _selectedContextId.asStateFlow()

    private val _expandedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedTaskIds: StateFlow<Set<Long>> = _expandedTaskIds.asStateFlow()

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val contexts: StateFlow<List<ProjectContext>> = contextDao.getAllContexts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val tasks: StateFlow<List<Task>> = _selectedContextId
        .flatMapLatest { contextId ->
            if (contextId != null) {
                taskDao.getTasksByContext(contextId)
            } else {
                taskDao.getTasksWithoutContext()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val priorityTasks: StateFlow<List<Task>> = taskDao.getPriorityTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectContext(contextId: Long?) {
        _selectedContextId.value = contextId
    }

    fun toggleTaskExpanded(taskId: Long) {
        val current = _expandedTaskIds.value
        _expandedTaskIds.value = if (current.contains(taskId)) {
            current - taskId
        } else {
            current + taskId
        }
    }

    fun expandTask(taskId: Long) {
        val current = _expandedTaskIds.value
        if (!current.contains(taskId)) {
            _expandedTaskIds.value = current + taskId
        }
    }

    // СВЕРНУТЬ/РАЗВЕРНУТЬ ВСЕ ВЕТКИ СРАЗУ
    fun toggleExpandAll() {
        val current = _expandedTaskIds.value
        if (current.isNotEmpty()) {
            _expandedTaskIds.value = emptySet()
        } else {
            val all = allTasks.value
            val parentIds = all.mapNotNull { it.parentId }.toSet()
            _expandedTaskIds.value = all
                .filter { parentIds.contains(it.id) }
                .map { it.id }
                .toSet()
        }
    }

    fun createTask(title: String, contextId: Long?, parentId: Long?) {
        viewModelScope.launch {
            val level = if (parentId != null) {
                val parent = withContext(Dispatchers.IO) {
                    taskDao.getTaskById(parentId).first()
                }
                (parent?.level ?: 0) + 1
            } else {
                0
            }
            if (level > 6) {
                throw IllegalStateException("Максимум 7 уровней вложенности")
            }
            val task = Task(
                title = title,
                contextId = contextId,
                parentId = parentId,
                level = level
            )
            withContext(Dispatchers.IO) {
                taskDao.insert(task)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                taskDao.update(task)
            }
            ReminderScheduler.scheduleReminder(getApplication<VooDooApp>(), task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication<VooDooApp>(), task.id)
            if (task.timerActive) {
                TimerServiceManager.stopAllTimersForTask(getApplication<VooDooApp>(), task.id)
            }
            withContext(Dispatchers.IO) {
                taskDao.delete(task)
            }
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isDone = !task.isDone
            val completedAt = if (isDone) now else null
            withContext(Dispatchers.IO) {
                taskDao.updateTaskStatus(task.id, isDone, completedAt)
            }
            if (isDone && task.timerActive) {
                TimerServiceManager.pauseTimer(getApplication<VooDooApp>(), task.id)
            }
        }
    }

    fun cyclePriority(task: Task) {
        viewModelScope.launch {
            val newPriority = when (task.priority) {
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 0
                else -> 0
            }
            withContext(Dispatchers.IO) {
                taskDao.updatePriority(task.id, newPriority)
            }
        }
    }

    fun startTimer(task: Task) {
        viewModelScope.launch {
            try {
                TimerServiceManager.startTimer(getApplication<VooDooApp>(), task.id)
            } catch (e: IllegalStateException) {
                // Превышен лимит таймеров
            }
        }
    }

    fun pauseTimer(task: Task) {
        viewModelScope.launch {
            TimerServiceManager.pauseTimer(getApplication<VooDooApp>(), task.id)
        }
    }

    fun updateSortOrder(taskId: Long, newSortOrder: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                taskDao.updateSortOrder(taskId, newSortOrder)
            }
        }
    }

    fun moveTask(task: Task, newContextId: Long?, newParentId: Long?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                taskDao.updateContext(task.id, newContextId)
            }
            if (newParentId != null) {
                val parent = withContext(Dispatchers.IO) {
                    taskDao.getTaskById(newParentId).first()
                }
                val newLevel = (parent?.level ?: 0) + 1
                if (newLevel <= 6) {
                    withContext(Dispatchers.IO) {
                        taskDao.updateParent(task.id, newParentId, newLevel)
                    }
                }
            } else {
                withContext(Dispatchers.IO) {
                    taskDao.updateParent(task.id, null, 0)
                }
            }
        }
    }

    fun reorderTasks(reorderedTasks: List<Task>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                reorderedTasks.forEachIndexed { index, task ->
                    if (task.sortOrder != index) {
                        taskDao.updateSortOrder(task.id, index)
                    }
                }
            }
        }
    }

    fun swapTasks(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentTasks = tasks.value.toMutableList()
            val task = currentTasks.removeAt(fromIndex)
            currentTasks.add(toIndex, task)
            withContext(Dispatchers.IO) {
                currentTasks.forEachIndexed { index, task ->
                    taskDao.updateSortOrder(task.id, index)
                }
            }
        }
    }
}