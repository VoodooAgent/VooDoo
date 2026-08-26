package com.example.voodoo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.example.voodoo.data.ProjectContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextsScreen(app: VooDooApp, onBackClick: () -> Unit) {
    var contexts by remember { mutableStateOf<List<ProjectContext>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContext by remember { mutableStateOf<ProjectContext?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        app.database.projectContextDao().getAllContexts().collectLatest { contexts = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Контексты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить контекст")
            }
        }
    ) { padding ->
        if (contexts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет контекстов.\nНажмите + чтобы создать первый.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(contexts, key = { it.id }) { context ->
                    ContextItemEdit(
                        context = context,
                        onEdit = { editingContext = context },
                        onDelete = {
                            scope.launch { app.database.projectContextDao().delete(context) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddContextDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color ->
                showAddDialog = false
                scope.launch {
                    app.database.projectContextDao().insert(
                        ProjectContext(name = name, color = color)
                    )
                }
            }
        )
    }

    editingContext?.let { context ->
        EditContextDialog(
            context = context,
            onDismiss = { editingContext = null },
            onConfirm = { name, color ->
                scope.launch {
                    app.database.projectContextDao().update(context.copy(name = name, color = color))
                }
                editingContext = null
            }
        )
    }
}

@Composable
fun ContextItemEdit(
    context: ProjectContext,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(context.color)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    "Редактировать",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Удалить",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddContextDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFFEF5350L) }
    val colors = listOf(
        0xFFEF5350L, 0xFFEC407AL, 0xFFAB47BCL, 0xFF7E57C2L, 0xFF5C6BC0L,
        0xFF42A5F5L, 0xFF29B6F6L, 0xFF26C6DAL, 0xFF26A69AL, 0xFF66BB6AL,
        0xFF9CCC65L, 0xFFD4E157L, 0xFFFFCA28L, 0xFFFFA726L, 0xFFFF7043L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый контекст") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название контекста") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Цвет фона:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.take(5).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(color), MaterialTheme.shapes.small)
                                .clickable { selectedColor = color }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Выбран",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun EditContextDialog(
    context: ProjectContext,
    onDismiss: () -> Unit,
    onConfirm: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(context.name) }
    var selectedColor by remember { mutableStateOf(context.color) }
    val colors = listOf(
        0xFFEF5350L, 0xFFEC407AL, 0xFFAB47BCL, 0xFF7E57C2L, 0xFF5C6BC0L,
        0xFF42A5F5L, 0xFF29B6F6L, 0xFF26C6DAL, 0xFF26A69AL, 0xFF66BB6AL,
        0xFF9CCC65L, 0xFFD4E157L, 0xFFFFCA28L, 0xFFFFA726L, 0xFFFF7043L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать контекст") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название контекста") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Цвет фона:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.take(5).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(color), MaterialTheme.shapes.small)
                                .clickable { selectedColor = color }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Выбран",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}