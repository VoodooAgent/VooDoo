package com.example.voodoo.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.Task
import com.example.voodoo.presentation.ContextListViewModel
import com.example.voodoo.presentation.TaskListViewModel
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextDeadlineScreen(
    contextId: Long?,
    onBackClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    contextViewModel: ContextListViewModel = viewModel(),
    taskListViewModel: TaskListViewModel = viewModel()
) {
    val contexts by contextViewModel.contexts.collectAsState()

    val tasksByDeadline = if (contextId != null) {
        taskListViewModel.getTasksWithDeadlineByContext(contextId).collectAsState(initial = emptyList()).value
    } else {
        taskListViewModel.getTasksWithDeadlineWithoutContext().collectAsState(initial = emptyList()).value
    }

    val context = if (contextId != null) contexts.find { it.id == contextId } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Задачи по дедлайну") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (tasksByDeadline.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет задач с дедлайном", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(tasksByDeadline, key = { it.id }) { task ->
                    DeadlineTaskRow(task = task, onClick = { onTaskClick(task.id) })
                }
            }
        }
    }
}

@Composable
private fun DeadlineTaskRow(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            task.deadline?.let { deadline ->
                Text(
                    text = formatDeadline(deadline),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDeadline(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val date = instant.toLocalDate()
    val hour = instant.hour
    val dateStr = String.format(
        Locale.getDefault(),
        "%02d.%02d.%02d",
        date.dayOfMonth,
        date.monthValue,
        date.year % 100
    )
    return "$dateStr ${hour}ч"
}