package com.example.voodoo.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.Task
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.TaskListViewModel
import com.example.voodoo.presentation.components.TaskCard
import com.example.voodoo.presentation.components.TaskSwipeMenu
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    onBackClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onDetailsClick: () -> Unit = {},
    taskListViewModel: TaskListViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val routineTasks by taskListViewModel.routineTasks.collectAsState()
    val contexts by taskListViewModel.contexts.collectAsState()
    val settings by mainViewModel.settings.collectAsState()
    val durations by taskListViewModel.taskDurations.collectAsState()
    val routineDoneToday by taskListViewModel.routineDoneToday.collectAsState()

    var showSwipeMenu by remember { mutableStateOf<Task?>(null) }

    val activeRoutineTasks = remember(routineTasks) {
        val today = LocalDate.now()
        routineTasks.filter { !it.isDone && isRoutineScheduledToday(it, today) }
            .sortedBy { it.routineStartMinutes ?: 0 }
    }

    val tasksByContext = activeRoutineTasks.groupBy { it.contextId }
    val allContextKeys = tasksByContext.keys

    var collapsedContexts by remember { mutableStateOf<Set<Long?>>(emptySet()) }
    val allExpanded = collapsedContexts.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔄",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Рутина",
                            modifier = Modifier.clickable { onDetailsClick() }
                        )

                        if (settings.groupRoutine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    collapsedContexts = if (allExpanded) allContextKeys else emptySet()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (allExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (allExpanded) "Свернуть все контексты" else "Развернуть все контексты",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeRoutineTasks.isEmpty()) {
                item(key = "empty_routine") {
                    Text(
                        text = "Нет активных рутинных задач",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            if (settings.groupRoutine) {
                tasksByContext.forEach { (contextId, tasks) ->
                    val contextName = if (contextId == null) {
                        settings.noContextName
                    } else {
                        contexts.find { it.id == contextId }?.name ?: "Контекст $contextId"
                    }

                    item(key = "routine_${contextId?.toString() ?: "no_context"}") {
                        RoutineContextSection(
                            contextName = contextName,
                            tasks = tasks,
                            durations = durations,
                            fontSize = settings.fontSize,
                            expanded = !collapsedContexts.contains(contextId),
                            onToggleExpanded = {
                                collapsedContexts = if (collapsedContexts.contains(contextId)) {
                                    collapsedContexts - contextId
                                } else {
                                    collapsedContexts + contextId
                                }
                            },
                            doneToday = routineDoneToday,
                            viewModel = taskListViewModel,
                            onTaskClick = onTaskClick,
                            onSwipeLeft = { task -> showSwipeMenu = task }
                        )
                    }
                }
            } else {
                items(activeRoutineTasks, key = { "routine_${it.id}" }) { task ->
                    RoutineTaskRow(
                        task = task,
                        doneToday = task.id in routineDoneToday,
                        durations = durations,
                        fontSize = settings.fontSize,
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeLeft = { showSwipeMenu = task }
                    )
                }
            }
        }
    }

    showSwipeMenu?.let { task ->
        TaskSwipeMenu(
            onDismiss = { showSwipeMenu = null },
            onAddSubtaskClick = { },
            onICalClick = { },
            onEditClick = { onTaskClick(task.id) },
            onMoveUpClick = {
                taskListViewModel.moveRoutineTaskUp(task)
                showSwipeMenu = null
            },
            onMoveDownClick = {
                taskListViewModel.moveRoutineTaskDown(task)
                showSwipeMenu = null
            },
            onDeleteClick = {
                taskListViewModel.deleteTask(task)
                showSwipeMenu = null
            },
            isDone = task.isDone,
            onRestoreClick = {
                taskListViewModel.toggleTaskDone(task)
                showSwipeMenu = null
            }
        )
    }
}

@Composable
fun RoutineContextSection(
    contextName: String,
    tasks: List<Task>,
    durations: Map<Long, Long>,
    fontSize: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    doneToday: Set<Long>,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeLeft: (Task) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contextName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tasks.forEach { task ->
                        RoutineTaskRow(
                            task = task,
                            doneToday = task.id in doneToday,
                            durations = durations,
                            fontSize = fontSize,
                            viewModel = viewModel,
                            onTaskClick = onTaskClick,
                            onSwipeLeft = onSwipeLeft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineTaskRow(
    task: Task,
    doneToday: Boolean,
    durations: Map<Long, Long>,
    fontSize: Int,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeLeft: (Task) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = doneToday,
            onCheckedChange = null,
            enabled = false,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(modifier = Modifier.weight(1f)) {
            TaskCard(
                task = task,
                pastSessionsDuration = durations[task.id] ?: 0L,
                fontSize = fontSize,
                onToggleDone = { viewModel.requestComplete(task) },
                onCyclePriority = { viewModel.cyclePriority(task) },
                onToggleTimer = {
                    if (task.timerActive) viewModel.pauseTimer(task)
                    else viewModel.startTimer(task)
                },
                onClick = { onTaskClick(task.id) },
                onSwipeRight = { viewModel.requestComplete(task) },
                onSwipeLeft = { onSwipeLeft(task) }
            )
        }
    }
}

private fun isRoutineScheduledToday(task: Task, date: LocalDate): Boolean {
    if (task.routineFrequency == null) return false
    val createdAt = Instant.ofEpochMilli(task.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    if (date.isBefore(createdAt)) return false
    return when (task.routineFrequency) {
        "daily" -> true
        "weekly" -> (task.routineDayOfWeek ?: return false) == date.dayOfWeek.value
        "monthly" -> (task.routineDayOfMonth ?: return false) == date.dayOfMonth
        else -> false
    }
}