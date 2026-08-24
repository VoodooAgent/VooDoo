package com.example.voodoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voodoo.data.Project
import com.example.voodoo.data.Task
import com.example.voodoo.ui.theme.VooDooTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════
// Enum сортировки
// ═══════════════════════════════════════════════
enum class SortOrder {
    MANUAL, ALPHABETICAL, DATE
}

// ═══════════════════════════════════════════════
// Entry point
// ═══════════════════════════════════════════════
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VooDooTheme {
                val app = application as VooDooApp
                VooDooNavHost(app)
            }
        }
    }
}

@Composable
fun VooDooNavHost(app: VooDooApp) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "projects") {
        composable("projects") {
            ProjectListScreen(
                app = app,
                onProjectClick = { projectId ->
                    navController.navigate("tasks/$projectId")
                }
            )
        }
        composable(
            route = "tasks/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            TaskListScreen(
                app = app,
                projectId = projectId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

// ═══════════════════════════════════════════════
// Виджет сортировки
// ═══════════════════════════════════════════════
@Composable
fun SortMenu(
    currentSort: SortOrder,
    onSortChange: (SortOrder) -> Unit,
    showReorderToggle: Boolean,
    reorderModeActive: Boolean,
    onReorderToggle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row {
        if (showReorderToggle) {
            IconButton(onClick = onReorderToggle) {
                Icon(
                    imageVector = if (reorderModeActive) Icons.Default.Check else Icons.Default.SwapVert,
                    contentDescription = if (reorderModeActive) "Завершить перемещение" else "Режим перемещения"
                )
            }
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                @Suppress("DEPRECATION")
                Icon(Icons.Filled.Sort, contentDescription = "Сортировка")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Вручную") },
                    onClick = { onSortChange(SortOrder.MANUAL); showMenu = false },
                    leadingIcon = {
                        if (currentSort == SortOrder.MANUAL) Icon(Icons.Default.Check, null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("По алфавиту") },
                    onClick = { onSortChange(SortOrder.ALPHABETICAL); showMenu = false },
                    leadingIcon = {
                        if (currentSort == SortOrder.ALPHABETICAL) Icon(Icons.Default.Check, null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("По дате создания") },
                    onClick = { onSortChange(SortOrder.DATE); showMenu = false },
                    leadingIcon = {
                        if (currentSort == SortOrder.DATE) Icon(Icons.Default.Check, null)
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Экран проектов
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(app: VooDooApp, onProjectClick: (Long) -> Unit) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.MANUAL) }
    var manualReorderMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sortOrder) {
        val flow = when (sortOrder) {
            SortOrder.MANUAL -> app.database.projectDao().getAllProjectsByManual()
            SortOrder.ALPHABETICAL -> app.database.projectDao().getAllProjectsByName()
            SortOrder.DATE -> app.database.projectDao().getAllProjectsByDate()
        }
        flow.collectLatest { projects = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проекты") },
                actions = {
                    SortMenu(
                        currentSort = sortOrder,
                        onSortChange = {
                            sortOrder = it
                            manualReorderMode = false
                        },
                        showReorderToggle = sortOrder == SortOrder.MANUAL,
                        reorderModeActive = manualReorderMode,
                        onReorderToggle = { manualReorderMode = !manualReorderMode }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить проект")
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет проектов.\nНажмите + чтобы создать первый.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    val index = projects.indexOf(project)
                    ProjectItem(
                        app = app,
                        project = project,
                        onClick = { onProjectClick(project.id) },
                        onEdit = { editingProject = project },
                        onDelete = {
                            scope.launch { app.database.projectDao().delete(project) }
                        },
                        showReorderButtons = manualReorderMode && sortOrder == SortOrder.MANUAL,
                        canMoveUp = index > 0,
                        canMoveDown = index < projects.lastIndex,
                        onMoveUp = {
                            if (index > 0) {
                                val other = projects[index - 1]
                                scope.launch {
                                    app.database.projectDao().update(project.copy(sortOrder = other.sortOrder))
                                    app.database.projectDao().update(other.copy(sortOrder = project.sortOrder))
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < projects.lastIndex) {
                                val other = projects[index + 1]
                                scope.launch {
                                    app.database.projectDao().update(project.copy(sortOrder = other.sortOrder))
                                    app.database.projectDao().update(other.copy(sortOrder = project.sortOrder))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                scope.launch {
                    val maxSort = projects.maxOfOrNull { it.sortOrder } ?: 0
                    app.database.projectDao().insert(
                        Project(name = name, sortOrder = maxSort + 1)
                    )
                }
            }
        )
    }

    editingProject?.let { project ->
        EditProjectDialog(
            project = project,
            onDismiss = { editingProject = null },
            onConfirm = { newName ->
                scope.launch { app.database.projectDao().update(project.copy(name = newName)) }
                editingProject = null
            }
        )
    }
}

// ═══════════════════════════════════════════════
// Экран задач
// ═══════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(app: VooDooApp, projectId: Long, onBackClick: () -> Unit) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var projectName by remember { mutableStateOf("Задачи") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.MANUAL) }
    var manualReorderMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sortOrder, projectId) {
        val flow = when (sortOrder) {
            SortOrder.MANUAL -> app.database.taskDao().getTasksByProjectByManual(projectId)
            SortOrder.ALPHABETICAL -> app.database.taskDao().getTasksByProjectByName(projectId)
            SortOrder.DATE -> app.database.taskDao().getTasksByProjectByDate(projectId)
        }
        flow.collectLatest { tasks = it }
    }

    LaunchedEffect(projectId) {
        app.database.projectDao().getProjectById(projectId).collectLatest { project ->
            projectName = project?.name ?: "Задачи"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    SortMenu(
                        currentSort = sortOrder,
                        onSortChange = {
                            sortOrder = it
                            manualReorderMode = false
                        },
                        showReorderToggle = sortOrder == SortOrder.MANUAL,
                        reorderModeActive = manualReorderMode,
                        onReorderToggle = { manualReorderMode = !manualReorderMode }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить задачу")
            }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет задач.\nНажмите + чтобы добавить.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    val index = tasks.indexOf(task)
                    TaskItem(
                        task = task,
                        onToggle = {
                            scope.launch {
                                app.database.taskDao().update(task.copy(isDone = !task.isDone))
                            }
                        },
                        onEdit = { editingTask = task },
                        onDelete = {
                            scope.launch { app.database.taskDao().delete(task) }
                        },
                        showReorderButtons = manualReorderMode && sortOrder == SortOrder.MANUAL,
                        canMoveUp = index > 0,
                        canMoveDown = index < tasks.lastIndex,
                        onMoveUp = {
                            if (index > 0) {
                                val other = tasks[index - 1]
                                scope.launch {
                                    app.database.taskDao().update(task.copy(sortOrder = other.sortOrder))
                                    app.database.taskDao().update(other.copy(sortOrder = task.sortOrder))
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < tasks.lastIndex) {
                                val other = tasks[index + 1]
                                scope.launch {
                                    app.database.taskDao().update(task.copy(sortOrder = other.sortOrder))
                                    app.database.taskDao().update(other.copy(sortOrder = task.sortOrder))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, description ->
                showAddDialog = false
                scope.launch {
                    val maxSort = tasks.maxOfOrNull { it.sortOrder } ?: 0
                    app.database.taskDao().insert(
                        Task(
                            projectId = projectId,
                            title = title,
                            description = description,
                            sortOrder = maxSort + 1
                        )
                    )
                }
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onConfirm = { title, description ->
                scope.launch {
                    app.database.taskDao().update(task.copy(title = title, description = description))
                }
                editingTask = null
            }
        )
    }
}

// ═══════════════════════════════════════════════
// Карточка проекта (с верхней задачей!)
// ═══════════════════════════════════════════════
@Composable
fun ProjectItem(
    app: VooDooApp,
    project: Project,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showReorderButtons: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var topTask by remember { mutableStateOf<Task?>(null) }
    LaunchedEffect(project.id) {
        app.database.taskDao().getTopTaskByProject(project.id).collectLatest {
            topTask = it
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showReorderButtons) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(24.dp),
                        enabled = canMoveUp
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Вверх", modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(24.dp),
                        enabled = canMoveDown
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium
                )
                topTask?.let { task ->
                    Text(
                        text = "↳ ${task.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Карточка задачи (с зачёркиванием!)
// ═══════════════════════════════════════════════
@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showReorderButtons: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showReorderButtons) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onMoveUp,
                            modifier = Modifier.size(24.dp),
                            enabled = canMoveUp
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Вверх", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onMoveDown,
                            modifier = Modifier.size(24.dp),
                            enabled = canMoveDown
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Checkbox(
                    checked = task.isDone,
                    onCheckedChange = { onToggle() }
                )
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        "Удалить",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// Диалоги
// ═══════════════════════════════════════════════
@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Название проекта") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Дополнительная информация") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditProjectDialog(
    project: Project,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(project.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать проект") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Название проекта") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать задачу") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название задачи") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Дополнительная информация") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), description.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}