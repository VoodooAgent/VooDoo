package com.example.voodoo.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.voodoo.data.Task

@Composable
fun TaskSwipeMenu(
    onDismiss: () -> Unit,
    onAddSubtaskClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveUpClick: (() -> Unit)? = null,
    onMoveDownClick: (() -> Unit)? = null,
    isDone: Boolean = false,
    onRestoreClick: (() -> Unit)? = null,
    onICalClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    completedSubtasks: List<Task> = emptyList(), // НОВОЕ
    onToggleSubtaskDone: (Task) -> Unit = {} // НОВОЕ
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Добавляем скролл для длинных списков
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Действия с задачей",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Кнопка «Вернуть в активные» — только для выполненных задач
                if (isDone && onRestoreClick != null) {
                    Button(
                        onClick = onRestoreClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Вернуть в активные")
                    }
                }

                // НОВОЕ: Кнопки перемещения (только для активных задач)
                if (!isDone && (onMoveUpClick != null || onMoveDownClick != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onMoveUpClick != null) {
                            Button(
                                onClick = onMoveUpClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Вверх")
                            }
                        }
                        if (onMoveDownClick != null) {
                            Button(
                                onClick = onMoveDownClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Вниз")
                            }
                        }
                    }
                }

                Button(
                    onClick = onAddSubtaskClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить подзадачу")
                }

                if (onEditClick != null) {
                    Button(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Редактировать")
                    }
                }

                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить")
                }

                // НОВОЕ: Список выполненных подзадач
                if (completedSubtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = "Выполненные подзадачи (${completedSubtasks.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        completedSubtasks.forEach { subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}