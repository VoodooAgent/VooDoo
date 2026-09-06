package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.CalendarContextSetting
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContextListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val contextDao = database.contextDao()
    private val calendarContextDao = database.calendarContextDao()
    private val taskDao = database.taskDao()

    val contexts: StateFlow<List<ProjectContext>> = contextDao.getAllContexts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createContext(name: String, color: Long) {
        viewModelScope.launch {
            // 1. Создаём контекст и получаем его новый ID
            val newContextId = contextDao.insert(ProjectContext(name = name, color = color))

            // 2. Автоматически создаём запись для календаря (включён по умолчанию)
            calendarContextDao.insert(CalendarContextSetting(
                contextId = newContextId,
                enabled = true
            ))
        }
    }

    fun updateContext(context: ProjectContext) {
        viewModelScope.launch {
            contextDao.update(context)
        }
    }

    fun deleteContext(context: ProjectContext) {
        viewModelScope.launch {
            // 1. Удаляем запись из настроек календаря
            calendarContextDao.deleteByContextId(context.id)

            // 2. Удаляем сам контекст
            contextDao.delete(context)
        }
    }

    fun getTasksByContext(contextId: Long?): Flow<List<Task>> {
        return if (contextId != null) {
            taskDao.getTasksByContext(contextId)
        } else {
            taskDao.getTasksWithoutContext()
        }
    }
}