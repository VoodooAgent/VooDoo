package com.example.voodoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voodoo.data.Project
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.ui.theme.VooDooTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SortOrder {
    MANUAL, ALPHABETICAL, DATE
}

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
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                app = app,
                onContextClick = { contextId -> navController.navigate("context/$contextId") },
                onProjectsClick = { navController.navigate("projects") }
            )
        }
        composable("projects") {
            ProjectListScreen(
                app = app,
                contextId = null,
                onProjectClick = { projectId -> navController.navigate("tasks/$projectId") },
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable(
            route = "context/{contextId}",
            arguments = listOf(navArgument("contextId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contextId = backStackEntry.arguments?.getLong("contextId") ?: 0L
            ContextProjectsScreen(
                app = app,
                contextId = contextId,
                onProjectClick = { projectId -> navController.navigate("tasks/$projectId") },
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") }
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
        composable("settings") {
            SettingsScreen(
                app = app,
                onBackClick = { navController.popBackStack() },
                onManageContexts = { navController.navigate("contexts") }
            )
        }
        composable("contexts") {
            ContextsScreen(
                app = app,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun VDLogoButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp).padding(8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("VD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(app: VooDooApp, onContextClick: (Long) -> Unit, onProjectsClick: () -> Unit) {
    var contexts by remember { mutableStateOf<List<ProjectContext>>(emptyList()) }
    LaunchedEffect(Unit) { app.database.projectContextDao().getAllContexts().collectLatest { contexts = it } }

    Scaffold(topBar = { TopAppBar(title = { Text("Контексты") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                onClick = onProjectsClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Все проекты", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(contexts, key = { it.id }) { context ->
                    ContextItem(context = context, onClick = { onContextClick(context.id) })
                }
            }
        }
    }
}

@Composable
fun ContextItem(context: ProjectContext, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(context.color))
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(context.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextProjectsScreen(
    app: VooDooApp, contextId: Long, onProjectClick: (Long) -> Unit, onBackClick: () -> Unit, onSettingsClick: () -> Unit
) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var context by remember { mutableStateOf<ProjectContext?>(null) }
    var allContexts by remember { mutableStateOf<List<ProjectContext>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.MANUAL) }
    var manualReorderMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(contextId) { app.database.projectContextDao().getContextById(contextId).collectLatest { context = it } }
    LaunchedEffect(Unit) { app.database.projectContextDao().getAllContexts().collectLatest { allContexts = it } }
    LaunchedEffect(sortOrder, contextId) {
        val flow = when (sortOrder) {
            SortOrder.MANUAL -> app.database.projectDao().getProjectsByContextByManual(contextId)
            SortOrder.ALPHABETICAL -> app.database.projectDao().getProjectsByContextByName(contextId)
            SortOrder.DATE -> app.database.projectDao().getProjectsByContextByDate(contextId)
        }
        flow.collectLatest { projects = it }
    }

    val backgroundColor = context?.let { Color(it.color) } ?: MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(context?.name ?: "Контекст") },
                navigationIcon = {
                    Row {
                        VDLogoButton(onClick = onSettingsClick)
                        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                    }
                },
                actions = {
                    SortMenu(sortOrder, { sortOrder = it; manualReorderMode = false }, sortOrder == SortOrder.MANUAL, manualReorderMode) { manualReorderMode = !manualReorderMode }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Добавить проект") } }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Нет проектов.\nНажмите + чтобы создать первый.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    val index = projects.indexOf(project)
                    ProjectItem(
                        app = app, project = project,
                        onClick = { onProjectClick(project.id) },
                        onEdit = { editingProject = project },
                        onDelete = { scope.launch { app.database.projectDao().delete(project) } },
                        showReorderButtons = manualReorderMode && sortOrder == SortOrder.MANUAL,
                        canMoveUp = index > 0, canMoveDown = index < projects.lastIndex,
                        onMoveUp = { if (index > 0) { val o = projects[index - 1]; scope.launch { app.database.projectDao().update(project.copy(sortOrder = o.sortOrder)); app.database.projectDao().update(o.copy(sortOrder = project.sortOrder)) } } },
                        onMoveDown = { if (index < projects.lastIndex) { val o = projects[index + 1]; scope.launch { app.database.projectDao().update(project.copy(sortOrder = o.sortOrder)); app.database.projectDao().update(o.copy(sortOrder = project.sortOrder)) } } }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddProjectDialog(
            contexts = allContexts,
            currentContextId = contextId,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, selectedContextId ->
                showAddDialog = false
                scope.launch {
                    app.database.projectDao().insert(
                        Project(name = name, contextId = selectedContextId, sortOrder = (projects.maxOfOrNull { it.sortOrder } ?: 0) + 1)
                    )
                }
            }
        )
    }
    editingProject?.let { p ->
        EditProjectDialog(
            project = p,
            contexts = allContexts,
            onDismiss = { editingProject = null },
            onConfirm = { name, selectedContextId ->
                scope.launch { app.database.projectDao().update(p.copy(name = name, contextId = selectedContextId)) }
                editingProject = null
            }
        )
    }
}

@Composable
fun SortMenu(currentSort: SortOrder, onSortChange: (SortOrder) -> Unit, showReorderToggle: Boolean, reorderModeActive: Boolean, onReorderToggle: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row {
        if (showReorderToggle) {
            IconButton(onClick = onReorderToggle) { Icon(if (reorderModeActive) Icons.Default.Check else Icons.Default.SwapVert, if (reorderModeActive) "Завершить" else "Режим") }
        }
        Box {
            IconButton(onClick = { showMenu = true }) { @Suppress("DEPRECATION") Icon(Icons.Filled.Sort, "Сортировка") }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem({ Text("Вручную") }, { onSortChange(SortOrder.MANUAL); showMenu = false }, leadingIcon = { if (currentSort == SortOrder.MANUAL) Icon(Icons.Default.Check, null) })
                DropdownMenuItem({ Text("По алфавиту") }, { onSortChange(SortOrder.ALPHABETICAL); showMenu = false }, leadingIcon = { if (currentSort == SortOrder.ALPHABETICAL) Icon(Icons.Default.Check, null) })
                DropdownMenuItem({ Text("По дате создания") }, { onSortChange(SortOrder.DATE); showMenu = false }, leadingIcon = { if (currentSort == SortOrder.DATE) Icon(Icons.Default.Check, null) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(app: VooDooApp, contextId: Long?, onProjectClick: (Long) -> Unit, onBackClick: () -> Unit, onSettingsClick: () -> Unit) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var allContexts by remember { mutableStateOf<List<ProjectContext>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.MANUAL) }
    var manualReorderMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { app.database.projectContextDao().getAllContexts().collectLatest { allContexts = it } }
    LaunchedEffect(sortOrder, contextId) {
        val flow = if (contextId == null) {
            when (sortOrder) {
                SortOrder.MANUAL -> app.database.projectDao().getProjectsWithoutContextByManual()
                SortOrder.ALPHABETICAL -> app.database.projectDao().getProjectsWithoutContextByName()
                SortOrder.DATE -> app.database.projectDao().getProjectsWithoutContextByDate()
            }
        } else {
            when (sortOrder) {
                SortOrder.MANUAL -> app.database.projectDao().getProjectsByContextByManual(contextId)
                SortOrder.ALPHABETICAL -> app.database.projectDao().getProjectsByContextByName(contextId)
                SortOrder.DATE -> app.database.projectDao().getProjectsByContextByDate(contextId)
            }
        }
        flow.collectLatest { projects = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Проекты") },
                navigationIcon = { Row { VDLogoButton(onClick = onSettingsClick); IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } } },
                actions = { SortMenu(sortOrder, { sortOrder = it; manualReorderMode = false }, sortOrder == SortOrder.MANUAL, manualReorderMode) { manualReorderMode = !manualReorderMode } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Добавить проект") } }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Нет проектов.\nНажмите + чтобы создать первый.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    val index = projects.indexOf(project)
                    ProjectItem(
                        app = app, project = project,
                        onClick = { onProjectClick(project.id) },
                        onEdit = { editingProject = project },
                        onDelete = { scope.launch { app.database.projectDao().delete(project) } },
                        showReorderButtons = manualReorderMode && sortOrder == SortOrder.MANUAL,
                        canMoveUp = index > 0, canMoveDown = index < projects.lastIndex,
                        onMoveUp = { if (index > 0) { val o = projects[index - 1]; scope.launch { app.database.projectDao().update(project.copy(sortOrder = o.sortOrder)); app.database.projectDao().update(o.copy(sortOrder = project.sortOrder)) } } },
                        onMoveDown = { if (index < projects.lastIndex) { val o = projects[index + 1]; scope.launch { app.database.projectDao().update(project.copy(sortOrder = o.sortOrder)); app.database.projectDao().update(o.copy(sortOrder = project.sortOrder)) } } }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddProjectDialog(
            contexts = allContexts,
            currentContextId = contextId,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, selectedContextId ->
                showAddDialog = false
                scope.launch {
                    app.database.projectDao().insert(
                        Project(name = name, contextId = selectedContextId, sortOrder = (projects.maxOfOrNull { it.sortOrder } ?: 0) + 1)
                    )
                }
            }
        )
    }
    editingProject?.let { p ->
        EditProjectDialog(
            project = p,
            contexts = allContexts,
            onDismiss = { editingProject = null },
            onConfirm = { name, selectedContextId ->
                scope.launch { app.database.projectDao().update(p.copy(name = name, contextId = selectedContextId)) }
                editingProject = null
            }
        )
    }
}

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
    LaunchedEffect(projectId) { app.database.projectDao().getProjectById(projectId).collectLatest { p -> projectName = p?.name ?: "Задачи" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { SortMenu(sortOrder, { sortOrder = it; manualReorderMode = false }, sortOrder == SortOrder.MANUAL, manualReorderMode) { manualReorderMode = !manualReorderMode } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, "Добавить задачу") } }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Нет задач.\nНажмите + чтобы добавить.") }
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
                        onToggle = { scope.launch { app.database.taskDao().update(task.copy(isDone = !task.isDone)) } },
                        onEdit = { editingTask = task },
                        onDelete = { scope.launch { app.database.taskDao().delete(task) } },
                        showReorderButtons = manualReorderMode && sortOrder == SortOrder.MANUAL,
                        canMoveUp = index > 0, canMoveDown = index < tasks.lastIndex,
                        onMoveUp = { if (index > 0) { val o = tasks[index - 1]; scope.launch { app.database.taskDao().update(task.copy(sortOrder = o.sortOrder)); app.database.taskDao().update(o.copy(sortOrder = task.sortOrder)) } } },
                        onMoveDown = { if (index < tasks.lastIndex) { val o = tasks[index + 1]; scope.launch { app.database.taskDao().update(task.copy(sortOrder = o.sortOrder)); app.database.taskDao().update(o.copy(sortOrder = task.sortOrder)) } } }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog({ showAddDialog = false }, { t, d ->
            showAddDialog = false; scope.launch { app.database.taskDao().insert(Task(projectId = projectId, title = t, description = d, sortOrder = (tasks.maxOfOrNull { it.sortOrder } ?: 0) + 1)) }
        })
    }
    editingTask?.let { t ->
        EditTaskDialog(t, { editingTask = null }, { newT, newD -> scope.launch { app.database.taskDao().update(t.copy(title = newT, description = newD)) }; editingTask = null })
    }
}

@Composable
fun ProjectItem(
    app: VooDooApp, project: Project, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    showReorderButtons: Boolean, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit
) {
    var topTask by remember { mutableStateOf<Task?>(null) }
    LaunchedEffect(project.id) { app.database.taskDao().getTopTaskByProject(project.id).collectLatest { topTask = it } }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showReorderButtons) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp), enabled = canMoveUp) { Icon(Icons.Default.KeyboardArrowUp, "Вверх", modifier = Modifier.size(16.dp)) }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp), enabled = canMoveDown) { Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(16.dp)) }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                topTask?.let { t -> Text("↳ ${t.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(18.dp)) }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun TaskItem(
    task: Task, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    showReorderButtons: Boolean, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showReorderButtons) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp), enabled = canMoveUp) { Icon(Icons.Default.KeyboardArrowUp, "Вверх", modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp), enabled = canMoveDown) { Icon(Icons.Default.KeyboardArrowDown, "Вниз", modifier = Modifier.size(16.dp)) }
                    }
                }
                Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
                Text(
                    task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Редактировать", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
            }
            if (task.description.isNotBlank()) {
                Text(task.description, modifier = Modifier.padding(start = 48.dp, bottom = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddProjectDialog(
    contexts: List<ProjectContext>,
    currentContextId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedContextId by remember { mutableStateOf(currentContextId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Название проекта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Контекст:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedContextId = null }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedContextId == null) {
                            Icon(Icons.Default.Check, "Выбран", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Без контекста")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                contexts.forEach { context ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedContextId = context.id },
                        colors = CardDefaults.cardColors(containerColor = Color(context.color).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedContextId == context.id) {
                                Icon(Icons.Default.Check, "Выбран", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(context.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim(), selectedContextId) },
                enabled = text.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun EditProjectDialog(
    project: Project,
    contexts: List<ProjectContext>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?) -> Unit
) {
    var text by remember { mutableStateOf(project.name) }
    var selectedContextId by remember { mutableStateOf(project.contextId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать проект") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Название проекта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Контекст:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedContextId = null }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedContextId == null) {
                            Icon(Icons.Default.Check, "Выбран", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Без контекста")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                contexts.forEach { context ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedContextId = context.id },
                        colors = CardDefaults.cardColors(containerColor = Color(context.color).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedContextId == context.id) {
                                Icon(Icons.Default.Check, "Выбран", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(context.name)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim(), selectedContextId) },
                enabled = text.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Новая задача") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Название задачи") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Дополнительная информация") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            }
        },
        confirmButton = { TextButton({ onConfirm(title.trim(), description.trim()) }, enabled = title.isNotBlank()) { Text("Добавить") } },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun EditTaskDialog(task: Task, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Редактировать задачу") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Название задачи") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Дополнительная информация") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            }
        },
        confirmButton = { TextButton({ onConfirm(title.trim(), description.trim()) }, enabled = title.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onDismiss) { Text("Отмена") } }
    )
}