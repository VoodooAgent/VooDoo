package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val contextDao = database.contextDao()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Task>> = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            taskDao.getAllTasks().map { allTasks ->
                if (q.isBlank()) {
                    emptyList()
                } else {
                    val needle = q.trim()
                    allTasks
                        .filter { task ->
                            task.title.contains(needle, ignoreCase = true) ||
                                task.description.contains(needle, ignoreCase = true) ||
                                task.result.contains(needle, ignoreCase = true)
                        }
                        .sortedWith(
                            compareBy<Task> { it.isDone }
                                .thenByDescending { it.priority }
                                .thenByDescending { it.createdAt }
                        )
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _contexts = MutableStateFlow<List<ProjectContext>>(emptyList())
    val contexts: StateFlow<List<ProjectContext>> = _contexts.asStateFlow()

    init {
        viewModelScope.launch {
            contextDao.getAllContexts().collect { _contexts.value = it }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }
}