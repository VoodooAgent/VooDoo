package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.data.TaskWithChildren
import com.example.voodoo.data.TimerSession
import com.example.voodoo.util.RebalanceRequiredException
import com.example.voodoo.util.SortOrderManager
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

    private val _pendingCompletionTask = MutableStateFlow<Task?>(null)
    val pendingCompletionTask: StateFlow<Task?> = _pendingCompletionTask.asStateFlow()

    val allTasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val taskDurations: StateFlow<Map<Long, Long>> = sessionDao.getAllSessions()
        .map { sessions ->
            sessions.groupBy { it.taskId }.mapValues { it.value.sumOf { s -> s.duration } }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

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

    val routineTasks: StateFlow<List<Task>> = taskDao.getRoutineTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeTimerTasks: StateFlow<List<Task>> = taskDao.getActiveTimerTasksFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectContext(contextId: Long?) {
        _selectedContextId.value = contextId
    }

    fun createTask(title: String, contextId: Long?, parentId: Long?) {
        viewModelScope.launch {
            val parentTask = parentId?.let { taskDao.getTaskById(it).first() }

            val siblings = if (parentId != null) {
                taskDao.getTasksByParent(parentId).first()
            } else if (contextId != null) {
                taskDao.getTasksByContext(contextId).first().filter { it.parentId == null }
            } else {
                taskDao.getTasksWithoutContext().first().filter { it.parentId == null }
            }.sortedBy { it.sortOrder }

            val maxSortOrder = siblings.maxOfOrNull { it.sortOrder } ?: 0
            val newSortOrder = if (maxSortOrder == 0) 10000 else maxSortOrder + 10000

            val newTask = Task(
                title = title,
                contextId = contextId,
                parentId = parentId,
                level = (parentTask?.level ?: 0) + if (parentId != null) 1 else 0,
                sortOrder = newSortOrder
            )
            taskDao.insert(newTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun requestComplete(task: Task) {
        viewModelScope.launch {
            if (task.isDone) {
                toggleTaskDone(task)
                return@launch
            }

            val activeChildrenCount = taskDao.countActiveDescendants(task.id)
            if (activeChildrenCount > 0) {
                _pendingCompletionTask.value = task
            } else {
                toggleTaskDone(task)
            }
        }
    }

    fun confirmCascadeComplete() {
        val task = _pendingCompletionTask.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                val currentTask = taskDao.getTaskById(task.id).first()
                if (currentTask != null && currentTask.timerActive && currentTask.timerStartedAt != null) {
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

                taskDao.cascadeComplete(task.id, now)
                taskDao.updateTaskStatus(task.id, true, now)
            }

            _pendingCompletionTask.value = null
            collapseTask(task.id)
        }
    }

    fun cancelCascadeComplete() {
        _pendingCompletionTask.value = null
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val isBecomingDone = !task.isDone
            val completedAt = if (isBecomingDone) now else null

            withContext(Dispatchers.IO) {
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
                3 -> 4
                4 -> 0
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

    // Сортировка в основном списке задач
    fun moveTaskUp(task: Task) {
        viewModelScope.launch {
            val siblings = getSiblings(task).sortedBy { it.sortOrder }
            val index = siblings.indexOfFirst { it.id == task.id }
            if (index > 0) {
                val prev = siblings[index - 1]
                taskDao.updateSortOrder(task.id, prev.sortOrder)
                taskDao.updateSortOrder(prev.id, task.sortOrder)
            }
        }
    }

    fun moveTaskDown(task: Task) {
        viewModelScope.launch {
            val siblings = getSiblings(task).sortedBy { it.sortOrder }
            val index = siblings.indexOfFirst { it.id == task.id }
            if (index < siblings.size - 1) {
                val next = siblings[index + 1]
                taskDao.updateSortOrder(task.id, next.sortOrder)
                taskDao.updateSortOrder(next.id, task.sortOrder)
            }
        }
    }

    // НОВОЕ: Универсальный метод перемещения с пересчетом всех значений
    private suspend fun moveInSpecialList(
        taskId: Long,
        direction: Int, // -1 = вверх, +1 = вниз
        getSortedTasks: suspend () -> List<Task>,
        updateSortOrder: suspend (Long, Int) -> Unit
    ) {
        val sorted = getSortedTasks()
        val index = sorted.indexOfFirst { it.id == taskId }
        if (index < 0) return

        val targetIndex = index + direction
        if (targetIndex < 0 || targetIndex >= sorted.size) return

        // Создаем новый список с перемещенной задачей
        val mutableList = sorted.toMutableList()
        val task = mutableList.removeAt(index)
        mutableList.add(targetIndex, task)

        // Пересчитываем все значения с шагом 10000
        var newOrder = 10000
        for (t in mutableList) {
            updateSortOrder(t.id, newOrder)
            newOrder += 10000
        }
    }

    // Сортировка в Приоритетном экране
    fun movePriorityTaskUp(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = -1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.priority > 0 && it.priority < 4 && !it.isDone }
                        .sortedBy { it.prioritySortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updatePrioritySortOrder(id, order) }
            )
        }
    }

    fun movePriorityTaskDown(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = 1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.priority > 0 && it.priority < 4 && !it.isDone }
                        .sortedBy { it.prioritySortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updatePrioritySortOrder(id, order) }
            )
        }
    }

    // Сортировка в экране Рутины
    fun moveRoutineTaskUp(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = -1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.priority == 4 && !it.isDone }
                        .sortedBy { it.routineSortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updateRoutineSortOrder(id, order) }
            )
        }
    }

    fun moveRoutineTaskDown(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = 1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.priority == 4 && !it.isDone }
                        .sortedBy { it.routineSortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updateRoutineSortOrder(id, order) }
            )
        }
    }

    // Сортировка в экране Активных таймеров
    fun moveActiveTaskUp(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = -1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.timerActive }
                        .sortedBy { it.activeSortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updateActiveSortOrder(id, order) }
            )
        }
    }

    fun moveActiveTaskDown(task: Task) {
        viewModelScope.launch {
            moveInSpecialList(
                taskId = task.id,
                direction = 1,
                getSortedTasks = {
                    taskDao.getAllTasksSync()
                        .filter { it.timerActive }
                        .sortedBy { it.activeSortOrder ?: it.sortOrder }
                },
                updateSortOrder = { id, order -> taskDao.updateActiveSortOrder(id, order) }
            )
        }
    }

    private suspend fun getSiblings(task: Task): List<Task> {
        return if (task.parentId != null) {
            taskDao.getTasksByParent(task.parentId).first()
        } else if (task.contextId != null) {
            taskDao.getTasksByContext(task.contextId).first().filter { it.parentId == null }
        } else {
            taskDao.getTasksWithoutContext().first().filter { it.parentId == null }
        }.filter { !it.isDone }
    }

    fun moveTaskBetween(taskId: Long, prevTaskId: Long?, nextTaskId: Long?, contextId: Long?, parentId: Long?) {
        viewModelScope.launch {
            val siblings = if (parentId != null) {
                taskDao.getTasksByParent(parentId).first()
            } else if (contextId != null) {
                taskDao.getTasksByContext(contextId).first().filter { it.parentId == null }
            } else {
                taskDao.getTasksWithoutContext().first().filter { it.parentId == null }
            }.filter { !it.isDone }.sortedBy { it.sortOrder }

            val prevSortOrder = siblings.find { it.id == prevTaskId }?.sortOrder
            val nextSortOrder = siblings.find { it.id == nextTaskId }?.sortOrder

            val result = SortOrderManager.calculateNewSortOrder(prevSortOrder, nextSortOrder)

            result.onSuccess { newSortOrder ->
                taskDao.updateSortOrder(taskId, newSortOrder)
            }.onFailure { exception ->
                if (exception is RebalanceRequiredException) {
                    rebalanceTasks(contextId, parentId, taskId)
                }
            }
        }
    }

    private suspend fun rebalanceTasks(contextId: Long?, parentId: Long?, insertedTaskId: Long) {
        val siblings = if (parentId != null) {
            taskDao.getTasksByParent(parentId).first()
        } else if (contextId != null) {
            taskDao.getTasksByContext(contextId).first().filter { it.parentId == null }
        } else {
            taskDao.getTasksWithoutContext().first().filter { it.parentId == null }
        }.filter { !it.isDone }.sortedBy { it.sortOrder }

        var currentSortOrder = 10000
        for (task in siblings) {
            taskDao.updateSortOrder(task.id, currentSortOrder)
            currentSortOrder += 10000
        }
    }

    fun buildTreeForPeriod(tasks: List<Task>): List<TaskWithChildren> {
        if (tasks.isEmpty()) return emptyList()

        val taskIds = tasks.map { it.id }.toSet()
        val childrenMap = tasks.groupBy { it.parentId }

        fun buildNode(task: Task): TaskWithChildren {
            val children = childrenMap[task.id] ?: emptyList()
            return TaskWithChildren(
                task = task,
                children = children.map { buildNode(it) }
            )
        }

        val roots = tasks.filter { task ->
            task.parentId == null || !taskIds.contains(task.parentId)
        }.sortedByDescending { it.completedAt ?: it.createdAt }

        return roots.map { buildNode(it) }
    }

    suspend fun getCompletedSubtasksSync(taskId: Long): List<Task> {
        return taskDao.getCompletedSubtasks(taskId).first()
    }
}