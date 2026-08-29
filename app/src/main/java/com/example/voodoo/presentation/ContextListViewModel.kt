package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContextListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val contextDao = database.contextDao()
    private val taskDao = database.taskDao()

    val contexts: StateFlow<List<ProjectContext>> = contextDao.getAllContexts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createContext(name: String, color: Long) {
        viewModelScope.launch {
            contextDao.insert(ProjectContext(name = name, color = color))
        }
    }

    fun updateContext(context: ProjectContext) {
        viewModelScope.launch {
            contextDao.update(context)
        }
    }

    fun deleteContext(context: ProjectContext) {
        viewModelScope.launch {
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