package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.Task
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.TaskListViewModel
import com.example.voodoo.presentation.components.TaskCard
import com.example.voodoo.presentation.components.TaskSwipeMenu
import java.util.Calendar

private fun isColorDark(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.55
}

private fun Task.completedTime(): Long {
    return completedAt ?: createdAt
}

private fun startOfToday(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun startOfWeek(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.firstDayOfWeek = Calendar.MONDAY
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    return calendar.timeInMillis
}

private fun startOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}

private fun startOfYear(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.MONTH, Calendar.JANUARY)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    contextId: Long?,
    contextName: String,
    onBackClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onPriorityClick: () -> Unit,
    taskListViewModel: TaskListViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val tasks by taskListViewModel.tasks.collectAsState()
    val contexts by taskListViewModel.contexts.collectAsState()
    val expandedIds by taskListViewModel.expandedTaskIds.collectAsState()
    val settings by mainViewModel.settings.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSwipeMenu by remember { mutableStateOf<Task?>(null) }
    var createParentId by remember { mutableStateOf<Long?>(null) }

    var completedExpanded by remember(contextId) { mutableStateOf(false) }
    var expandedPeriods by remember(contextId) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(contextId) {
        taskListViewModel.selectContext(contextId)
    }

    val currentContext = contexts.find { it.id == contextId }

    val displayName = if (contextId == null) {
        settings.noContextName
    } else {
        currentContext?.name ?: contextName
    }

    val backgroundColor = if (contextId == null) {
        MaterialTheme.colorScheme.background
    } else {
        Color(currentContext?.color ?: 0xFFE0E0E0L)
    }

    val barContentColor = if (contextId == null) {
        MaterialTheme.colorScheme.onSurface
    } else {
        if (isColorDark(backgroundColor)) Color.White else Color(0xFF111111)
    }

    val anyExpanded = expandedIds.isNotEmpty()

    val activeTasks = remember(tasks) {
        tasks.filter { !it.isDone }.sortedBy { it.sortOrder }
    }

    val visibleRootTasks = remember(activeTasks) {
        activeTasks.filter { task ->
            task.parentId == null || activeTasks.none { it.id == task.parentId }
        }
    }

    val doneTasks = remember(tasks) {
        tasks.filter { it.isDone }.sortedByDescending { it.completedTime() }
    }

    val todayStart = startOfToday()
    val weekStart = startOfWeek()
    val monthStart = startOfMonth()
    val yearStart = startOfYear()

    val doneToday = doneTasks.filter { it.completedTime() >= todayStart }
    val doneWeek = doneTasks.filter { it.completedTime() in weekStart until todayStart }
    val doneMonth = doneTasks.filter { it.completedTime() in monthStart until weekStart }
    val doneYear = doneTasks.filter { it.completedTime() in yearStart until monthStart }
    val doneAllTime = doneTasks.filter { it.completedTime() < yearStart }

    val togglePeriod: (String) -> Unit = { key ->
        expandedPeriods = if (key in expandedPeriods) expandedPeriods - key else expandedPeriods + key
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { taskListViewModel.toggleExpandAll() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (anyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (anyExpanded) "Свернуть все ветки" else "Развернуть все ветки",
                                tint = barContentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = barContentColor,
                    navigationIconContentColor = barContentColor,
                    actionIconContentColor = barContentColor
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onPriorityClick) {
                        Icon(Icons.Default.PriorityHigh, contentDescription = "Приоритетные задачи")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                createParentId = null
                showCreateDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(backgroundColor),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(visibleRootTasks, key = { it.id }) { task ->
                TaskTreeItem(
                    task = task,
                    expandedIds = expandedIds,
                    fontSize = settings.fontSize,
                    allTasks = activeTasks,
                    viewModel = taskListViewModel,
                    onTaskClick = onTaskClick,
                    onSwipeLeft = { swipedTask -> showSwipeMenu = swipedTask }
                )
            }

            if (doneTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    CompletedSectionHeader(
                        title = "Выполнено",
                        count = doneTasks.size,
                        expanded = completedExpanded,
                        contentColor = barContentColor,
                        onToggle = { completedExpanded = !completedExpanded }
                    )
                }

                if (completedExpanded) {
                    if (doneToday.isNotEmpty()) {
                        item(key = "label_today") {
                            PeriodLabel(text = "Сегодня", contentColor = barContentColor)
                        }
                        doneItems(
                            prefix = "today",
                            list = doneToday,
                            fontSize = settings.fontSize,
                            viewModel = taskListViewModel,
                            onTaskClick = onTaskClick,
                            onSwipeMenuRequest = { showSwipeMenu = it }
                        )
                    }

                    collapsiblePeriod(
                        periodKey = "week",
                        title = "За неделю",
                        list = doneWeek,
                        expandedPeriods = expandedPeriods,
                        onToggle = togglePeriod,
                        contentColor = barContentColor,
                        fontSize = settings.fontSize,
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeMenuRequest = { showSwipeMenu = it }
                    )

                    collapsiblePeriod(
                        periodKey = "month",
                        title = "За месяц",
                        list = doneMonth,
                        expandedPeriods = expandedPeriods,
                        onToggle = togglePeriod,
                        contentColor = barContentColor,
                        fontSize = settings.fontSize,
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeMenuRequest = { showSwipeMenu = it }
                    )

                    collapsiblePeriod(
                        periodKey = "year",
                        title = "За год",
                        list = doneYear,
                        expandedPeriods = expandedPeriods,
                        onToggle = togglePeriod,
                        contentColor = barContentColor,
                        fontSize = settings.fontSize,
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeMenuRequest = { showSwipeMenu = it }
                    )

                    collapsiblePeriod(
                        periodKey = "all_time",
                        title = "За все время",
                        list = doneAllTime,
                        expandedPeriods = expandedPeriods,
                        onToggle = togglePeriod,
                        contentColor = barContentColor,
                        fontSize = settings.fontSize,
                        viewModel = taskListViewModel,
                        onTaskClick = onTaskClick,
                        onSwipeMenuRequest = { showSwipeMenu = it }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title: String ->
                taskListViewModel.createTask(title, contextId, createParentId)
                createParentId?.let { taskListViewModel.expandTask(it) }
                showCreateDialog = false
            }
        )
    }

    showSwipeMenu?.let { task ->
        TaskSwipeMenu(
            onDismiss = { showSwipeMenu = null },
            onAddSubtaskClick = {
                createParentId = task.id
                showCreateDialog = true
                showSwipeMenu = null
            },
            onICalClick = { },
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
}

@Composable
fun TaskTreeItem(
    task: Task,
    expandedIds: Set<Long>,
    fontSize: Int,
    allTasks: List<Task>,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeLeft: (Task) -> Unit
) {
    val children = allTasks.filter { it.parentId == task.id }
    val isExpanded = expandedIds.contains(task.id)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (children.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.toggleTaskExpanded(task.id) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(28.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                TaskCard(
                    task = task,
                    fontSize = fontSize,
                    onToggleDone = { viewModel.toggleTaskDone(task) },
                    onCyclePriority = { viewModel.cyclePriority(task) },
                    onToggleTimer = {
                        if (task.timerActive) viewModel.pauseTimer(task)
                        else viewModel.startTimer(task)
                    },
                    onClick = { onTaskClick(task.id) },
                    onSwipeRight = { viewModel.toggleTaskDone(task) },
                    onSwipeLeft = { onSwipeLeft(task) }
                )
            }
        }

        if (isExpanded && children.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
            ) {
                Spacer(modifier = Modifier.width(13.dp))
                Box(
                    modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    children.forEach { child ->
                        TaskTreeItem(
                            task = child,
                            expandedIds = expandedIds,
                            fontSize = fontSize,
                            allTasks = allTasks,
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
private fun CompletedSectionHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    contentColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            color = contentColor
        )
    }
}

@Composable
private fun PeriodLabel(
    text: String,
    contentColor: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor.copy(alpha = 0.75f),
        modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun CollapsiblePeriodHeader(
    title: String,
    count: Int,
    expanded: Boolean,
    contentColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 18.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.labelLarge,
            color = contentColor.copy(alpha = 0.85f)
        )
    }
}

private fun LazyListScope.doneItems(
    prefix: String,
    list: List<Task>,
    fontSize: Int,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeMenuRequest: (Task) -> Unit
) {
    items(list, key = { "${prefix}_${it.id}" }) { task ->
        TaskCard(
            task = task,
            fontSize = fontSize,
            onToggleDone = { viewModel.toggleTaskDone(task) },
            onCyclePriority = { viewModel.cyclePriority(task) },
            onToggleTimer = {
                if (task.timerActive) viewModel.pauseTimer(task)
                else viewModel.startTimer(task)
            },
            onClick = { onTaskClick(task.id) },
            onSwipeRight = { viewModel.toggleTaskDone(task) },
            onSwipeLeft = { onSwipeMenuRequest(task) }
        )
    }
}

private fun LazyListScope.collapsiblePeriod(
    periodKey: String,
    title: String,
    list: List<Task>,
    expandedPeriods: Set<String>,
    onToggle: (String) -> Unit,
    contentColor: Color,
    fontSize: Int,
    viewModel: TaskListViewModel,
    onTaskClick: (Long) -> Unit,
    onSwipeMenuRequest: (Task) -> Unit
) {
    if (list.isEmpty()) return

    val expanded = periodKey in expandedPeriods

    item(key = "header_$periodKey") {
        CollapsiblePeriodHeader(
            title = title,
            count = list.size,
            expanded = expanded,
            contentColor = contentColor,
            onToggle = { onToggle(periodKey) }
        )
    }

    if (expanded) {
        doneItems(
            prefix = periodKey,
            list = list,
            fontSize = fontSize,
            viewModel = viewModel,
            onTaskClick = onTaskClick,
            onSwipeMenuRequest = onSwipeMenuRequest
        )
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название задачи") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}