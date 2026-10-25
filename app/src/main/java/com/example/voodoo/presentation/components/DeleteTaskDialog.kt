package com.example.voodoo.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.voodoo.presentation.PendingDeletion
import com.example.voodoo.presentation.TaskListViewModel

/** Показывает подтверждение, когда [TaskListViewModel.deleteTask] ждёт ответа. */
@Composable
fun DeleteTaskConfirmation(viewModel: TaskListViewModel) {
    val pending by viewModel.pendingDeletion.collectAsState()
    pending?.let {
        DeleteTaskDialog(
            pending = it,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }
}

@Composable
fun DeleteTaskDialog(
    pending: PendingDeletion,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить «${pending.task.title}»?") },
        text = {
            Text(
                text = describeDeletion(pending),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отменить")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    )
}

fun describeDeletion(pending: PendingDeletion): String {
    val lines = mutableListOf<String>()
    if (pending.descendantCount > 0) {
        lines += "Вместе с задачей удалятся подзадачи: ${pending.descendantCount}."
    }
    if (pending.sessionCount > 0) {
        lines += "Будет удалено учтённое время: ${formatTrackedTime(pending.trackedMillis)} " +
            "(сессий: ${pending.sessionCount})."
    }
    if (pending.activeTimerCount > 0) {
        lines += "Запущенный таймер будет потерян вместе с текущим отрезком времени."
    }
    lines += "Отменить удаление нельзя."
    return lines.joinToString("\n\n")
}

fun formatTrackedTime(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        totalMinutes > 0 -> "$minutes мин"
        else -> "меньше минуты"
    }
}
