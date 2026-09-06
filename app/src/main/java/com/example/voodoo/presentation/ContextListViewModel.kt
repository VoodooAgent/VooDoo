package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ContextListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val contextDao = database.contextDao()

    // Все контексты (для экрана управления)
    private val _contexts = MutableStateFlow<List<ProjectContext>>(emptyList())
    val contexts: StateFlow<List<ProjectContext>> = _contexts.asStateFlow()

    // НОВОЕ: Только видимые контексты (для главного экрана)
    private val _visibleContexts = MutableStateFlow<List<ProjectContext>>(emptyList())
    val visibleContexts: StateFlow<List<ProjectContext>> = _visibleContexts.asStateFlow()

    init {
        viewModelScope.launch {
            // Подписываемся на все контексты (для управления)
            contextDao.getAllContexts().collect { list ->
                _contexts.value = list.sortedBy { it.sortOrder }
            }
        }

        viewModelScope.launch {
            // Подписываемся только на видимые контексты (для главного экрана)
            contextDao.getVisibleContexts().collect { list ->
                _visibleContexts.value = list.sortedBy { it.sortOrder }
            }
        }

        // ИСПРАВЛЕНО: Инициализируем sortOrder для старых контекстов с шагом 10000
        viewModelScope.launch {
            rebalanceContextsSortOrder()
        }
    }

    // ИСПРАВЛЕНО: Ребаланс сортировки контекстов (исправляет старые контексты с sortOrder = 0)
    private suspend fun rebalanceContextsSortOrder() {
        val contexts = contextDao.getAllContextsSync().sortedBy { it.sortOrder }
        var currentSortOrder = 10000
        for (context in contexts) {
            if (context.sortOrder != currentSortOrder) {
                contextDao.updateSortOrder(context.id, currentSortOrder)
            }
            currentSortOrder += 10000
        }
    }

    fun createContext(name: String, color: Long) {
        viewModelScope.launch {
            val currentContexts = contextDao.getAllContextsSync()
            val maxSortOrder = currentContexts.maxOfOrNull { it.sortOrder } ?: 0
            val newContext = ProjectContext(
                name = name,
                color = color,
                sortOrder = maxSortOrder + 10000
            )
            contextDao.insert(newContext)
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

    // ИСПРАВЛЕНО: Читаем актуальное состояние из базы перед переключением
    fun toggleContextHidden(context: ProjectContext) {
        viewModelScope.launch {
            val current = contextDao.getContextById(context.id).first()
            if (current != null) {
                contextDao.updateHidden(context.id, !current.isHidden)
            }
        }
    }

    // ИСПРАВЛЕНО: Читаем актуальный список из базы для корректной сортировки
    fun moveContextUp(context: ProjectContext) {
        viewModelScope.launch {
            val sorted = contextDao.getAllContextsSync().sortedBy { it.sortOrder }
            val index = sorted.indexOfFirst { it.id == context.id }
            if (index > 0) {
                val prev = sorted[index - 1]
                contextDao.updateSortOrder(context.id, prev.sortOrder)
                contextDao.updateSortOrder(prev.id, context.sortOrder)
            }
        }
    }

    // ИСПРАВЛЕНО: Читаем актуальный список из базы для корректной сортировки
    fun moveContextDown(context: ProjectContext) {
        viewModelScope.launch {
            val sorted = contextDao.getAllContextsSync().sortedBy { it.sortOrder }
            val index = sorted.indexOfFirst { it.id == context.id }
            if (index < sorted.size - 1) {
                val next = sorted[index + 1]
                contextDao.updateSortOrder(context.id, next.sortOrder)
                contextDao.updateSortOrder(next.id, context.sortOrder)
            }
        }
    }
}