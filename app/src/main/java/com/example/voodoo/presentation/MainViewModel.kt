package com.example.voodoo.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.AppSettings
import com.example.voodoo.data.CsvHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val settingsDao = database.settingsDao()
    private val csvHelper = CsvHelper(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

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

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = csvHelper.exportToCsv(uri)
            _exportResult.value = result.getOrNull() ?: "Ошибка: ${result.exceptionOrNull()?.message}"
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val result = csvHelper.importFromCsv(uri)
            _importResult.value = result.getOrNull() ?: "Ошибка: ${result.exceptionOrNull()?.message}"
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }
}