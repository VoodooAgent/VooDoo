package com.example.voodoo.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ShowCompletedState {
    private val _ids = MutableStateFlow<Set<Long>>(emptySet())
    val ids: StateFlow<Set<Long>> = _ids.asStateFlow()

    fun toggle(taskId: Long) {
        val current = _ids.value
        _ids.value = if (current.contains(taskId)) current - taskId else current + taskId
    }
}