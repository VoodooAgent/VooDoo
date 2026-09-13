package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val sessionDao = database.timerSessionDao()
    private val contextDao = database.contextDao()
    private val calendarContextDao = database.calendarContextDao()
    private val settingsDao = database.settingsDao()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _contexts = MutableStateFlow<List<ProjectContext>>(emptyList())
    val contexts: StateFlow<List<ProjectContext>> = _contexts.asStateFlow()

    private val _calendarSettings = MutableStateFlow<List<CalendarContextSetting>>(emptyList())
    val calendarSettings: StateFlow<List<CalendarContextSetting>> = _calendarSettings.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _sessions = MutableStateFlow<List<TimerSession>>(emptyList())
    val sessions: StateFlow<List<TimerSession>> = _sessions.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _viewMode = MutableStateFlow(CalendarViewMode.DAY)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDao.getSettings().collect { settings ->
                settings?.let { _settings.value = it }
            }
        }
        viewModelScope.launch {
            contextDao.getAllContexts().collect { _contexts.value = it }
        }
        viewModelScope.launch {
            calendarContextDao.getAllSettings().collect { _calendarSettings.value = it }
        }
        viewModelScope.launch {
            taskDao.getAllTasks().collect { _tasks.value = it }
        }
        viewModelScope.launch {
            sessionDao.getAllSessions().collect { _sessions.value = it }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun updateShowTasks(show: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(showTasks = show)
            settingsDao.upsert(updated)
        }
    }

    fun updateShowSessions(show: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(showSessions = show)
            settingsDao.upsert(updated)
        }
    }

    fun isRoutineScheduledOnDate(task: Task, date: LocalDate): Boolean {
        if (task.priority != 4 || task.routineFrequency == null) return false
        return when (task.routineFrequency) {
            "daily" -> true
            "weekly" -> {
                val taskDow = task.routineDayOfWeek ?: return false
                date.dayOfWeek.value == taskDow
            }
            "monthly" -> {
                val taskDom = task.routineDayOfMonth ?: return false
                date.dayOfMonth == taskDom
            }
            else -> false
        }
    }

    fun isRoutineDoneOnDate(taskId: Long, date: LocalDate, allSessions: List<TimerSession>): Boolean {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return allSessions.any { it.taskId == taskId && it.startTime in dayStart until dayEnd }
    }

    fun getRoutineMarkers(
        date: LocalDate,
        allTasks: List<Task>,
        allSessions: List<TimerSession>
    ): Map<Long, Boolean> {
        val routine = allTasks.filter {
            it.priority == 4 && !it.isDone && it.routineFrequency != null && isRoutineScheduledOnDate(it, date)
        }
        return routine.associate { it.id to isRoutineDoneOnDate(it.id, date, allSessions) }
    }

    fun toggleContextEnabled(contextId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val existing = _calendarSettings.value.find { it.contextId == contextId }
            if (existing != null) {
                calendarContextDao.upsert(existing.copy(enabled = enabled))
            } else {
                calendarContextDao.upsert(CalendarContextSetting(contextId = contextId, enabled = enabled))
            }
        }
    }
}

enum class CalendarViewMode {
    DAY, WEEK, MONTH
}