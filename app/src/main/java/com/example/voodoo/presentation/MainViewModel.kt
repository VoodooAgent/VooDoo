package com.example.voodoo.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.AppSettings
import com.example.voodoo.data.CsvBackup
import com.example.voodoo.data.CsvHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val _deleteResult = MutableStateFlow<String?>(null)
    val deleteResult: StateFlow<String?> = _deleteResult.asStateFlow()

    // Разобранный файл, ожидающий подтверждения замены данных
    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _autoBackups = MutableStateFlow<List<File>>(emptyList())
    val autoBackups: StateFlow<List<File>> = _autoBackups.asStateFlow()

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

    // НОВОЕ: Обновление типа сортировки задач
    fun updateTaskSortMode(mode: String) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(taskSortMode = mode)
            settingsDao.upsert(updated)
        }
    }

    // НОВОЕ: Обновление группировки в спец. контекстах (по отдельности)
    fun updateGroupPriority(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(groupPriority = enabled)
            settingsDao.upsert(updated)
        }
    }

    fun updateGroupRoutine(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(groupRoutine = enabled)
            settingsDao.upsert(updated)
        }
    }

    fun updateGroupActive(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(groupActive = enabled)
            settingsDao.upsert(updated)
        }
    }

    // НОВОЕ: Обновление показа выполненных подзадач при свайпе
    fun updateShowCompletedInSwipe(enabled: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = current.copy(showCompletedInSwipe = enabled)
            settingsDao.upsert(updated)
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = csvHelper.exportToCsv(uri)
            _exportResult.value = result.getOrNull() ?: "Ошибка: ${result.exceptionOrNull()?.message}"
        }
    }

    /** Читает и проверяет файл; база меняется только после [confirmImport]. */
    fun importData(uri: Uri) {
        viewModelScope.launch {
            csvHelper.readCsv(uri)
                .onSuccess { _pendingImport.value = PendingImport(it, sourceName = null) }
                .onFailure { _importResult.value = "Импорт отменён: ${it.message}" }
        }
    }

    fun restoreFromAutoBackup(file: File) {
        viewModelScope.launch {
            csvHelper.readCsv(file)
                .onSuccess { _pendingImport.value = PendingImport(it, sourceName = file.name) }
                .onFailure { _importResult.value = "Восстановление отменено: ${it.message}" }
        }
    }

    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            val result = csvHelper.replaceAll(pending.backup)
            _importResult.value = result.getOrNull() ?: "Ошибка: ${result.exceptionOrNull()?.message}"
            refreshAutoBackups()
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun refreshAutoBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _autoBackups.value = csvHelper.listAutoBackups()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                csvHelper.createAutoBackup("delete_all")
                refreshAutoBackups()
                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        database.timerSessionDao().deleteAll()
                        database.taskDao().deleteAll()
                        database.calendarContextDao().deleteAll()
                        database.icalSyncDao().deleteAll()
                        database.contextDao().deleteAll()
                    }
                }
                _deleteResult.value = "Все данные успешно удалены"
            } catch (e: Exception) {
                _deleteResult.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
}

data class PendingImport(
    val backup: CsvBackup,
    // null — файл выбран пользователем; иначе имя автоматической копии
    val sourceName: String?
)
