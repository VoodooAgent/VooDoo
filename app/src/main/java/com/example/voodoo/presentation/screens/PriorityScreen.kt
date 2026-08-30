package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.Task
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.TaskListViewModel
import com.example.voodoo.presentation.components.TaskCard
import com.example.voodoo.presentation.components.TaskSwipeMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityScreen(
    onBackClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    taskListViewModel: TaskListViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val priorityTasks by taskListViewModel.priorityTasks.collectAsState()
    val contexts by taskListViewModel.contexts.collectAsState()
    val settings by mainViewModel.settings.collectAsState()
    val durations by taskListViewModel.taskDurations.collectAsState()

    var showSwipeMenu by remember { mutableStateOf<Task?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createParent by remember { mutableStateOf<Task?>(null) }

    // Показываем только АКТИВНЫЕ приоритетные задачи
    val activePriorityTasks = remember(priorityTasks) {
        priorityTasks.filter { !it.isDone }
    }

    val tasksByContext = activePriorityTasks.groupBy { it.contextId }
    val allContextKeys = tasksByContext.keys

    var collapsedContexts by remember { mutableStateOf<Set<Long?>>(emptySet()) }
    val allExpanded = collapsedContexts.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Приоритетные задачи")

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                collapsedContexts = if (allExpanded) {
                                    allContextKeys
                                } else {
                                    emptySet()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (allExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = if (allExpanded) {
                                    "Свернуть все контексты"
                                } else {
                                    "Развернуть все контексты"
                                },
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activePriorityTasks.isEmpty()) {
                item(key = "empty_priority") {
                    Text(
                        text = "Нет активных приоритетных задач",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            tasksByContext.forEach { (contextId, tasks) ->
                val contextName = if (contextId == null) {
                    settings.noContextName
                } else {
                    contexts.find { it.id == contextId }?.name ?: "Контекст $contextId"
                }

                item(key = contextId?.toString() ?: "no_context") {
                    PriorityContextSection(
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
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeLeft = { task -> showSwipeMenu = task }
                    )
                }
            }
        }
    }

    showSwipeMenu?.let { task ->
        TaskSwipeMenu(
            onDismiss = { showSwipeMenu = null },
            onAddSubtaskClick = {
                createParent = task
                showCreateDialog = true
                showSwipeMenu = null
            },
            onICalClick = { /* TODO */ },
            onEditClick = { onTaskClick(task.id) },
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

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title: String ->
                createParent?.let { parent ->
                    taskListViewModel.createTask(
                        title = title,
                        contextId = parent.contextId,
                        parentId = parent.id
                    )
                    taskListViewModel.expandTask(parent.id)
                }
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun PriorityContextSection(
    contextName: String,
    tasks: List<Task>,
    durations: Map<Long, Long>,
    fontSize: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeLeft: (Task) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contextName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = if (expanded) "Свернуть" else "Развернуть"
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tasks.forEach { task ->
                        TaskCard(
                            task = task,
                            pastSessionsDuration = durations[task.id] ?: 0L,
                            fontSize = fontSize,
                            onToggleDone = { viewModel.toggleTaskDone(task) },
                            onCyclePriority = { viewModel.cyclePriority(task) },
                            onToggleTimer = {
                                if (task.timerActive) {
                                    viewModel.pauseTimer(task)
                                } else {
                                    viewModel.startTimer(task)
                                }
                            },
                            onClick = { onTaskClick(task.id) },
                            onSwipeRight = { viewModel.toggleTaskDone(task) },
                            onSwipeLeft = { onSwipeLeft(task) }
                        )
                    }
                }
            }
        }
    }
}