package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.TaskDetailViewModel
import com.example.voodoo.presentation.TaskListViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBackClick: () -> Unit,
    detailViewModel: TaskDetailViewModel = viewModel(),
    listViewModel: TaskListViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val task by detailViewModel.task.collectAsState()
    val sessions by detailViewModel.sessions.collectAsState()
    val allTasks by detailViewModel.allTasks.collectAsState()
    val contexts by detailViewModel.contexts.collectAsState()
    val settings by mainViewModel.settings.collectAsState()

    LaunchedEffect(taskId) {
        detailViewModel.loadTask(taskId)
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var plannedStart by remember { mutableStateOf<Long?>(null) }
    var plannedEnd by remember { mutableStateOf<Long?>(null) }
    var reminderMinutes by remember { mutableStateOf<Int?>(null) }
    var isInitialized by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showContextDialog by remember { mutableStateOf(false) }
    var showParentDialog by remember { mutableStateOf(false) }
    var pendingStartDate by remember { mutableStateOf<Long?>(null) }
    var pendingEndDate by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(task) {
        task?.let { t ->
            if (!isInitialized) {
                title = t.title
                description = t.description
                result = t.result
                plannedStart = t.plannedStart
                plannedEnd = t.plannedEnd
                reminderMinutes = t.reminderMinutesBefore
                isInitialized = true
            }
        }
    }

    fun saveChanges() {
        task?.let { t ->
            listViewModel.updateTask(
                t.copy(
                    title = title.trim().ifBlank { t.title },
                    description = description,
                    result = result,
                    plannedStart = plannedStart,
                    plannedEnd = plannedEnd,
                    reminderMinutesBefore = reminderMinutes
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifBlank { "Задача" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        saveChanges()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        task?.let { currentTask ->
            val parentTask = allTasks.find { it.id == currentTask.parentId }
            val contextName = contexts.find { it.id == currentTask.contextId }?.name
                ?: settings.noContextName

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // НАЗВАНИЕ (редактируемое)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Название задачи", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Введите название...") }
                        )
                    }
                }

                // Описание
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Описание", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            placeholder = { Text("Введите описание...") }
                        )
                    }
                }

                // Результат
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Результат", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = result,
                            onValueChange = { result = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            placeholder = { Text("Ожидаемый результат...") }
                        )
                    }
                }

                // Принадлежность
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Принадлежность", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Контекст:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showContextDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(contextName, modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Изменить контекст",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Родительская задача:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showParentDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                parentTask?.title ?: "Нет",
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Изменить родителя",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Планирование
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Планирование", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Начало:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(plannedStart?.let { detailViewModel.formatDateTime(it) } ?: "Не задано")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Конец:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(plannedEnd?.let { detailViewModel.formatDateTime(it) } ?: "Не задано")
                        }
                        if (plannedStart != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Напоминание: ${reminderMinutes?.let { formatReminder(it) } ?: "выключено"}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = (reminderMinutes ?: 0).toFloat(),
                                onValueChange = {
                                    reminderMinutes = if (it <= 0f) null else it.toInt()
                                },
                                valueRange = 0f..1440f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Выкл", style = MaterialTheme.typography.labelSmall)
                                Text("5 мин", style = MaterialTheme.typography.labelSmall)
                                Text("1 час", style = MaterialTheme.typography.labelSmall)
                                Text("24 ч", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Временные метки
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Временные метки", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Создано: ${detailViewModel.formatDateTime(currentTask.createdAt)}")
                        Text("Выполнено: ${detailViewModel.formatDateTime(currentTask.completedAt)}")
                    }
                }

                // Сессии таймера
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Сессии таймера", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (sessions.isEmpty()) {
                            Text(
                                "Нет сессий",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            sessions.forEach { session ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = detailViewModel.formatDateTime(session.startTime),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = detailViewModel.formatDuration(session.duration),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalDuration = sessions.sumOf { it.duration }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Итого:", style = MaterialTheme.typography.bodyMedium)
                                Text(detailViewModel.formatDuration(totalDuration), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Диалог выбора контекста
    if (showContextDialog) {
        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            title = { Text("Выбрать контекст") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(
                        onClick = {
                            detailViewModel.updateContext(null)
                            showContextDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(settings.noContextName, modifier = Modifier.fillMaxWidth())
                    }
                    contexts.forEach { ctx ->
                        TextButton(
                            onClick = {
                                detailViewModel.updateContext(ctx.id)
                                showContextDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(ctx.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог выбора родителя
    if (showParentDialog) {
        AlertDialog(
            onDismissRequest = { showParentDialog = false },
            title = { Text("Родительская задача") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TextButton(
                        onClick = {
                            detailViewModel.updateParent(null)
                            showParentDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Нет (корневая задача)", modifier = Modifier.fillMaxWidth())
                    }
                    detailViewModel.eligibleParents(taskId, allTasks).forEach { t ->
                        TextButton(
                            onClick = {
                                detailViewModel.updateParent(t.id)
                                showParentDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "· ".repeat(t.level) + t.title,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showParentDialog = false }) { Text("Отмена") }
            }
        )
    }

    // DatePicker начала
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = plannedStart ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        pendingStartDate = date
                        showStartDatePicker = false
                        showStartTimePicker = true
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // TimePicker начала
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = plannedStart?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 12,
            initialMinute = plannedStart?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingStartDate?.let { date ->
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        plannedStart = cal.timeInMillis
                    }
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
    // DatePicker конца
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = plannedEnd ?: plannedStart ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        pendingEndDate = date
                        showEndDatePicker = false
                        showEndTimePicker = true
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    // TimePicker конца
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = plannedEnd?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 12,
            initialMinute = plannedEnd?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingEndDate?.let { date ->
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        cal.set(Calendar.MINUTE, timePickerState.minute)
                        plannedEnd = cal.timeInMillis
                    }
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

private fun formatReminder(minutes: Int): String {
    return when {
        minutes < 60 -> "$minutes мин"
        minutes < 1440 -> "${minutes / 60} ч ${minutes % 60} мин"
        else -> "${minutes / 1440} дн ${(minutes % 1440) / 60} ч"
    }
}