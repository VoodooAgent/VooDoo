package com.example.voodoo.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.AppSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val settingsDao = database.settingsDao()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDao.getSettings().collect { settings ->
                settings?.let {
                    _settings.value = it
                    _isDarkTheme.value = it.darkTheme
                }
            }
        }
    }

    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(darkTheme = enabled)
            settingsDao.upsert(updated)
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(fontSize = size.coerceIn(12, 24))
            settingsDao.upsert(updated)
        }
    }

    fun updateNoContextName(name: String) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(noContextName = name)
            settingsDao.upsert(updated)
        }
    }
}