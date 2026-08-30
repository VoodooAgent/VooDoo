package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
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
    private val sessionDao = database.timerSessionDao()

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

    fun createTask(title: String, contextId: Long?, parentId: Long?) {
        viewModelScope.launch {
            val parentTask = parentId?.let { taskDao.getTaskById(it).first() }
            val newTask = Task(
                title = title,
                contextId = contextId,
                parentId = parentId,
                level = (parentTask?.level ?: 0) + if (parentId != null) 1 else 0,
                sortOrder = (tasks.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            )
            taskDao.insert(newTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isBecomingDone = !task.isDone
            val completedAt = if (isBecomingDone) now else null

            withContext(Dispatchers.IO) {
                // ЗАПРОС АКТУАЛЬНОГО СОСТОЯНИЯ: берем задачу прямо из БД,
                // чтобы исключить рассинхронизацию с UI (например, при очень быстром клике)
                val currentTask = taskDao.getTaskById(task.id).first()

                if (currentTask != null) {
                    if (isBecomingDone && currentTask.timerActive && currentTask.timerStartedAt != null) {
                        val duration = now - currentTask.timerStartedAt
                        val session = TimerSession(
                            taskId = task.id,
                            startTime = currentTask.timerStartedAt,
                            endTime = now,
                            duration = duration
                        )
                        sessionDao.insert(session)
                        taskDao.updateTimerStatus(task.id, false, null)
                    }

                    taskDao.updateTaskStatus(task.id, isBecomingDone, completedAt)
                }
            }

            if (isBecomingDone) {
                collapseTask(task.id)
            }
        }
    }

    fun cyclePriority(task: Task) {
        viewModelScope.launch {
            val newPriority = when (task.priority) {
                0 -> 1
                1 -> 2
                2 -> 3
                else -> 0
            }
            taskDao.updatePriority(task.id, newPriority)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.delete(task)
        }
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
        _expandedTaskIds.value = _expandedTaskIds.value + taskId
    }

    fun collapseTask(taskId: Long) {
        _expandedTaskIds.value = _expandedTaskIds.value - taskId
    }

    fun toggleExpandAll() {
        val current = _expandedTaskIds.value
        val visibleTasks = tasks.value.filter { !it.isDone }
        if (visibleTasks.isEmpty()) return

        val parentIds = visibleTasks.mapNotNull { it.parentId }.toSet()
        val visibleParentIds = visibleTasks
            .filter { it.id in parentIds }
            .map { it.id }
            .toSet()

        if (visibleParentIds.isEmpty()) return

        val allExpanded = visibleParentIds.all { it in current }
        _expandedTaskIds.value = if (allExpanded) {
            current - visibleParentIds
        } else {
            current + visibleParentIds
        }
    }

    fun startTimer(task: Task) {
        viewModelScope.launch {
            taskDao.updateTimerStatus(task.id, true, System.currentTimeMillis())
        }
    }

    fun pauseTimer(task: Task) {
        viewModelScope.launch {
            val currentTask = taskDao.getTaskById(task.id).first()
            currentTask?.let {
                if (it.timerActive && it.timerStartedAt != null) {
                    val now = System.currentTimeMillis()
                    val duration = now - it.timerStartedAt
                    val session = TimerSession(
                        taskId = task.id,
                        startTime = it.timerStartedAt,
                        endTime = now,
                        duration = duration
                    )
                    withContext(Dispatchers.IO) {
                        sessionDao.insert(session)
                        taskDao.updateTimerStatus(task.id, false, null)
                    }
                } else {
                    taskDao.updateTimerStatus(task.id, false, null)
                }
            }
        }
    }

    fun moveTaskUp(task: Task) {
        viewModelScope.launch {
            val all = if (task.contextId != null) {
                taskDao.getTasksByContext(task.contextId).first()
            } else {
                taskDao.getTasksWithoutContext().first()
            }.filter { it.parentId == task.parentId && !it.isDone }
                .sortedBy { it.sortOrder }
            val index = all.indexOfFirst { it.id == task.id }
            if (index > 0) {
                val prev = all[index - 1]
                taskDao.updateSortOrder(task.id, prev.sortOrder)
                taskDao.updateSortOrder(prev.id, task.sortOrder)
            }
        }
    }

    fun moveTaskDown(task: Task) {
        viewModelScope.launch {
            val all = if (task.contextId != null) {
                taskDao.getTasksByContext(task.contextId).first()
            } else {
                taskDao.getTasksWithoutContext().first()
            }.filter { it.parentId == task.parentId && !it.isDone }
                .sortedBy { it.sortOrder }
            val index = all.indexOfFirst { it.id == task.id }
            if (index < all.size - 1) {
                val next = all[index + 1]
                taskDao.updateSortOrder(task.id, next.sortOrder)
                taskDao.updateSortOrder(next.id, task.sortOrder)
            }
        }
    }
}