package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.presentation.ContextListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextsManagementScreen(
    onBackClick: () -> Unit,
    viewModel: ContextListViewModel = viewModel()
) {
    val contexts by viewModel.contexts.collectAsState()
    var editingContext by remember { mutableStateOf<ProjectContext?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление контекстами") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contexts) { context ->
                ContextManagementCard(
                    context = context,
                    onEdit = { editingContext = context },
                    onDelete = { viewModel.deleteContext(context) }
                )
            }
        }
    }

    editingContext?.let { context ->
        EditContextDialog(
            context = context,
            onDismiss = { editingContext = null },
            onSave = { name, color ->
                viewModel.updateContext(context.copy(name = name, color = color))
                editingContext = null
            }
        )
    }
}

@Composable
fun ContextManagementCard(
    context: ProjectContext,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(context.color), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = context.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
fun EditContextDialog(
    context: ProjectContext,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf(context.name) }
    var selectedColor by remember { mutableLongStateOf(context.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать контекст") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Цвет фона экрана контекста:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CONTEXT_COLORS.chunked(6).forEach { colorRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorRow.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(color), MaterialTheme.shapes.small)
                                        .clickable { selectedColor = color }
                                        .then(
                                            if (selectedColor == color) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}