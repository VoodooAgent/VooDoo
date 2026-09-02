package com.example.voodoo.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.voodoo.VooDooApp
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.data.ICalSyncSetting
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.service.ExportResult
import com.example.voodoo.service.ICalDateFilter
import com.example.voodoo.service.ICalPublisher
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val ctx = LocalContext.current

    var filterMode by remember { mutableIntStateOf(0) }
    var customStartMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var customEndMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isExporting by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Выберите контексты для синхронизации:",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                contexts.forEach { context ->
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

            Text(
                text = "Фильтр по дате:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = filterMode == 0,
                            onClick = { filterMode = 0 }
                        )
                        Text(
                            text = "Без фильтра (все записи)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = filterMode == 1,
                            onClick = {
                                filterMode = 1
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                                cal.set(Calendar.MINUTE, 0)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                customEndMillis = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_MONTH, -1)
                                customStartMillis = cal.timeInMillis
                            }
                        )
                        Text(
                            text = "Вчера",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = filterMode == 2,
                            onClick = { filterMode = 2 }
                        )
                        Text(
                            text = "Свой период",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (filterMode == 1) {
                        val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        Text(
                            text = "📅 ${df.format(Date(customStartMillis))} — ${(customEndMillis - 1).let { df.format(Date(it)) }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 32.dp)
                        )
                    }

                    if (filterMode == 2) {
                        val dfDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val dfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Начало периода:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                                        DatePickerDialog(
                                            ctx,
                                            { _, year, month, day ->
                                                cal.set(year, month, day)
                                                customStartMillis = cal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(dfDate.format(Date(customStartMillis)))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                                        TimePickerDialog(
                                            ctx,
                                            { _, hour, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hour)
                                                cal.set(Calendar.MINUTE, minute)
                                                cal.set(Calendar.SECOND, 0)
                                                cal.set(Calendar.MILLISECOND, 0)
                                                customStartMillis = cal.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(dfTime.format(Date(customStartMillis)))
                                }
                            }

                            Text("Конец периода:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                                        DatePickerDialog(
                                            ctx,
                                            { _, year, month, day ->
                                                cal.set(year, month, day)
                                                customEndMillis = cal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(dfDate.format(Date(customEndMillis)))
                                }
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                                        TimePickerDialog(
                                            ctx,
                                            { _, hour, minute ->
                                                cal.set(Calendar.HOUR_OF_DAY, hour)
                                                cal.set(Calendar.MINUTE, minute)
                                                cal.set(Calendar.SECOND, 0)
                                                cal.set(Calendar.MILLISECOND, 0)
                                                customEndMillis = cal.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    }
                                ) {
                                    Text(dfTime.format(Date(customEndMillis)))
                                }
                            }

                            if (customStartMillis > customEndMillis) {
                                Text(
                                    text = "⚠️ Начало периода позже конца!",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        try {
                            val filter = when (filterMode) {
                                0 -> ICalDateFilter()
                                else -> ICalDateFilter(customStartMillis, customEndMillis)
                            }

                            val result = ICalPublisher.publishCalendar(VooDooApp.instance, filter)

                            when (result) {
                                is ExportResult.Success -> {
                                    val file = result.file
                                    val uri = FileProvider.getUriForFile(
                                        VooDooApp.instance,
                                        "${VooDooApp.instance.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/calendar"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "VooDoo Экспорт календаря")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooserIntent = Intent.createChooser(shareIntent, "Отправить календарь")
                                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    VooDooApp.instance.startActivity(chooserIntent)
                                }
                                is ExportResult.Error -> {
                                    Toast.makeText(ctx, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting && !(filterMode == 2 && customStartMillis > customEndMillis)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                val label = when (filterMode) {
                    0 -> "Экспортировать всё"
                    1 -> "Экспортировать за вчера"
                    else -> "Экспортировать за выбранный период"
                }
                Text(label)
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