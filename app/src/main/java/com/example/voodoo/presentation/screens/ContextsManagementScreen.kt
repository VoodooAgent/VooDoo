package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.presentation.ContextListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextsManagementScreen(
    onBackClick: () -> Unit,
    viewModel: ContextListViewModel = viewModel()
) {
    val contexts by viewModel.contexts.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
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
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(contexts, key = { it.id }) { context ->
                ContextItem(
                    context = context,
                    onEditClick = { editingContext = context },
                    onDeleteClick = { viewModel.deleteContext(context) },
                    onToggleHidden = { viewModel.toggleContextHidden(context) },
                    onMoveUp = { viewModel.moveContextUp(context) },
                    onMoveDown = { viewModel.moveContextDown(context) }
                )
            }

            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить контекст")
                }
            }
        }
    }

    if (showCreateDialog || editingContext != null) {
        ContextEditDialog(
            context = editingContext,
            onDismiss = {
                showCreateDialog = false
                editingContext = null
            },
            onSave = { name, color ->
                if (editingContext != null) {
                    viewModel.updateContext(editingContext!!.copy(name = name, color = color))
                } else {
                    viewModel.createContext(name, color)
                }
                showCreateDialog = false
                editingContext = null
            }
        )
    }
}

@Composable
private fun ContextItem(
    context: ProjectContext,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleHidden: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветовой индикатор
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(context.color), shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Название контекста
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = context.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Кнопка скрытия/показа
        IconButton(onClick = onToggleHidden) {
            Icon(
                imageVector = if (context.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (context.isHidden) "Показать" else "Скрыть",
                tint = if (context.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
            )
        }

        // Кнопки сортировки
        IconButton(onClick = onMoveUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onMoveDown) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз", modifier = Modifier.size(20.dp))
        }

        // Кнопка редактирования
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(20.dp))
        }

        // Кнопка удаления
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ContextEditDialog(
    context: ProjectContext?,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var name by remember(context) { mutableStateOf(context?.name ?: "") }
    var color by remember(context) { mutableStateOf(context?.color ?: 0xFFE0E0E0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (context == null) "Новый контекст" else "Редактировать контекст") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название контекста") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Палитра цветов
                Text("Цвет фона экрана контекста:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                val colors = listOf(
                    0xFFEF5350L, 0xFFEC407AL, 0xFFAB47BCL, 0xFF7E57C2L, 0xFF5C6BC0L,
                    0xFF42A5F5L, 0xFF29B6F6L, 0xFF26C6DAL, 0xFF26A69AL, 0xFF66BB6AL,
                    0xFF9CCC65L, 0xFFD4E157L, 0xFFFFEE58L, 0xFFFFCA28L, 0xFFFFA726L,
                    0xFFFF7043L, 0xFF8D6E63L, 0xFF78909CL, 0xFFE57373L, 0xFFF06292L,
                    0xFFBA68C8L, 0xFF9575CDL, 0xFF7986CBL, 0xFF64B5F6L, 0xFF4FC3F7L,
                    0xFF4DD0E1L, 0xFF4DB6ACL, 0xFF81C784L, 0xFFAED581L, 0xFFFFB74DL
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colors.chunked(6).forEach { colorRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorRow.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(c), MaterialTheme.shapes.small)
                                        .then(
                                            if (color == c) {
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
                onClick = { onSave(name, color) },
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