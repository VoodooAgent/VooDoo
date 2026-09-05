package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onContextsClick: () -> Unit,
    onICalSyncClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val deleteResult by viewModel.deleteResult.collectAsState()

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Обработка результата удаления
    LaunchedEffect(deleteResult) {
        deleteResult?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearDeleteResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContextsClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Контексты", style = MaterialTheme.typography.titleMedium)
                    Text("→", style = MaterialTheme.typography.titleMedium)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Название пустого контекста",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = settings.noContextName,
                        onValueChange = { viewModel.updateNoContextName(it) },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Темная тема", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.darkTheme,
                        onCheckedChange = { viewModel.updateDarkTheme(it) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Размер шрифта: ${settings.fontSize} sp",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = settings.fontSize.toFloat(),
                        onValueChange = { viewModel.updateFontSize(it.toInt()) },
                        valueRange = 12f..24f,
                        steps = 11
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Пример текста",
                        fontSize = TextUnit(settings.fontSize.toFloat(), TextUnitType.Sp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onICalSyncClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Синхронизация с iCalendar", style = MaterialTheme.typography.titleMedium)
                    Text("→", style = MaterialTheme.typography.titleMedium)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onExportClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Экспорт в CSV")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onImportClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Импорт из CSV")
                    }
                }
            }

            // Кнопка "Удалить ВСЁ"
            Card(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Удалить ВСЁ", color = Color.White)
                }
            }
        }
    }

    // Диалог подтверждения удаления
    if (showDeleteAllDialog) {
        DeleteAllConfirmDialog(
            onConfirm = {
                showDeleteAllDialog = false
                viewModel.deleteAllData()
            },
            onDismiss = {
                showDeleteAllDialog = false
            }
        )
    }
}

@Composable
fun DeleteAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Удалить ВСЁ",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = "Вы уверены что хотите удалить все задачи и сессии из базы данных?\n\nЭто действие нельзя отменить!",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Да", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отменить")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}