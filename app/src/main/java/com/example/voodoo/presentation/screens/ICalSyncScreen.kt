package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.voodoo.VooDooApp
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.ICalSyncSetting
import com.example.voodoo.service.ICalPublisher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ICalSyncScreen(
    onBackClick: () -> Unit
) {
    val database = AppDatabase.getDatabase(VooDooApp.instance)
    val contextDao = database.contextDao()
    val icalSyncDao = database.icalSyncDao()

    val contexts by contextDao.getAllContexts().collectAsState(emptyList())
    val syncSettings by icalSyncDao.getAllSyncSettings().collectAsState(emptyList())

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Синхронизация iCalendar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Выберите контексты для синхронизации:",
                style = MaterialTheme.typography.titleMedium
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contexts) { context ->
                    val setting = syncSettings.find { it.contextId == context.id }
                    val isEnabled = setting?.enabled ?: false

                    ContextSyncCard(
                        context = context,
                        isEnabled = isEnabled,
                        onToggle = { enabled ->
                            scope.launch {
                                icalSyncDao.upsert(
                                    ICalSyncSetting(
                                        contextId = context.id,
                                        enabled = enabled
                                    )
                                )
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val file = ICalPublisher.publishCalendar(VooDooApp.instance)
                        file?.let {
                            // Показать сообщение об успехе
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Экспортировать календарь")
            }
        }
    }
}

@Composable
fun ContextSyncCard(
    context: ProjectContext,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onToggle(!isEnabled) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}