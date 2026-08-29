package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
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
        }
    }
}