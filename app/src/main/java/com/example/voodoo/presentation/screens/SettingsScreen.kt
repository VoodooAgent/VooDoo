package com.example.voodoo.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voodoo.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onContextsClick: (() -> Unit)? = null,
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    LaunchedEffect(exportResult, importResult) {
        exportResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportResult()
        }
        importResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (onContextsClick != null) {
                        IconButton(onClick = onContextsClick) {
                            Icon(Icons.Default.List, contentDescription = "Управление контекстами")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Секция: Тема
            Text(
                text = "Тема",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тёмная тема")
                Switch(
                    checked = settings.darkTheme,
                    onCheckedChange = { viewModel.updateDarkTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Секция: Текст
            Text(
                text = "Текст",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Размер шрифта: ${settings.fontSize}")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.updateFontSize(settings.fontSize - 1) },
                    enabled = settings.fontSize > 12,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-")
                }
                OutlinedButton(
                    onClick = { viewModel.updateFontSize(settings.fontSize + 1) },
                    enabled = settings.fontSize < 24,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Секция: Название контекста без проекта
            Text(
                text = "Контекст без проекта",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Название: ${settings.noContextName}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Секция Сортировка и Отображение
            Text(
                text = "Сортировка и Отображение",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Радиокнопки для типа сортировки
            Text(
                text = "Тип сортировки задач:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            val sortModes = listOf(
                "manual" to "Ручная",
                "created_at" to "По дате создания",
                "planned_start" to "По плановому старту",
                "deadline" to "По дедлайну"
            )

            sortModes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = settings.taskSortMode == mode,
                            onClick = { viewModel.updateTaskSortMode(mode) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.taskSortMode == mode,
                        onClick = { viewModel.updateTaskSortMode(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Переключатели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Группировать спец. контексты по проектам")
                Switch(
                    checked = settings.groupSpecialContexts,
                    onCheckedChange = { viewModel.updateGroupSpecialContexts(it) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Показывать выполненные подзадачи при свайпе")
                Switch(
                    checked = settings.showCompletedInSwipe,
                    onCheckedChange = { viewModel.updateShowCompletedInSwipe(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // НОВОЕ: Кнопка управления контекстами
            if (onContextsClick != null) {
                Button(
                    onClick = onContextsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Управление контекстами")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Секция: Данные
            Text(
                text = "Данные",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = {
                    exportLauncher.launch("voodoo_backup_${System.currentTimeMillis()}.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспорт в CSV")
            }

            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Импорт из CSV")
            }
        }
    }
}