package com.example.voodoo.presentation.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import com.example.voodoo.presentation.TaskListViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RoutineStatEntry(
    val task: Task,
    val doneDays: Int,
    val totalDays: Int,
    val streak: Int,
    val totalDurationMs: Long,
    val missedLast7: List<LocalDate>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineStatsScreen(
    onBackClick: () -> Unit,
    taskListViewModel: TaskListViewModel = viewModel()
) {
    val routineTasks by taskListViewModel.routineTasks.collectAsState()
    val allSessions by taskListViewModel.allSessions.collectAsState()

    val stats = remember(routineTasks, allSessions) {
        routineTasks.filter { !it.isDone && it.routineFrequency != null }
            .map { task -> computeRoutineStats(task, allSessions) }
            .sortedBy { it.task.routineStartMinutes ?: 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика рутины") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (stats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет рутинных задач", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats, key = { it.task.id }) { entry ->
                    RoutineStatCard(entry = entry)
                }
            }
        }
    }
}

private fun computeRoutineStats(
    task: Task,
    allSessions: List<TimerSession>
): RoutineStatEntry {
    val sessionsByDay = allSessions
        .filter { it.taskId == task.id }
        .groupBy { Instant.ofEpochMilli(it.startTime).atZone(ZoneId.systemDefault()).toLocalDate() }

    val startDate = Instant.ofEpochMilli(task.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()

    val scheduledDays = mutableListOf<LocalDate>()
    var cursor = startDate
    while (!cursor.isAfter(today)) {
        if (isScheduled(task, cursor)) scheduledDays.add(cursor)
        cursor = cursor.plusDays(1)
    }

    val doneDays = scheduledDays.count { sessionsByDay.containsKey(it) }
    val totalDays = scheduledDays.size

    // Текущая серия: идём назад от сегодня, пока дни выполнены
    var streak = 0
    var day = today
    while (isScheduled(task, day) && sessionsByDay.containsKey(day)) {
        streak++
        day = day.minusDays(1)
    }

    val totalDurationMs = allSessions
        .filter { it.taskId == task.id }
        .sumOf { it.duration }

    val missedLast7 = (0L..6L).map { today.minusDays(it) }
        .filter { isScheduled(task, it) && !sessionsByDay.containsKey(it) }
        .sorted()

    return RoutineStatEntry(
        task = task,
        doneDays = doneDays,
        totalDays = totalDays,
        streak = streak,
        totalDurationMs = totalDurationMs,
        missedLast7 = missedLast7
    )
}

private fun isScheduled(task: Task, date: LocalDate): Boolean {
    return when (task.routineFrequency) {
        "daily" -> true
        "weekly" -> (task.routineDayOfWeek ?: return false) == date.dayOfWeek.value
        "monthly" -> (task.routineDayOfMonth ?: return false) == date.dayOfMonth
        else -> false
    }
}

@Composable
private fun RoutineStatCard(entry: RoutineStatEntry) {
    val task = entry.task
    val percent = if (entry.totalDays > 0) {
        (entry.doneDays * 100.0 / entry.totalDays).toInt()
    } else 0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    frequencyLabel(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    timeLabel(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🟢 Выполнено: ${entry.doneDays} дн", style = MaterialTheme.typography.bodyMedium)
                Text("📊 $percent%", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🔥 Серия: ${entry.streak} дн", style = MaterialTheme.typography.bodyMedium)
                Text("⏱ Всего: ${formatDuration(entry.totalDurationMs)}", style = MaterialTheme.typography.bodyMedium)
            }

            if (entry.missedLast7.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "❌ Пропуски: ${entry.missedLast7.joinToString(", ") { it.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun frequencyLabel(task: Task): String {
    return when (task.routineFrequency) {
        "daily" -> "Ежедневно"
        "weekly" -> {
            val dayNames = listOf("", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
            "Каждую неделю (${dayNames.getOrElse(task.routineDayOfWeek ?: 0) { "" }})"
        }
        "monthly" -> "Каждый месяц (${task.routineDayOfMonth}е)"
        else -> ""
    }
}

private fun timeLabel(task: Task): String {
    val start = task.routineStartMinutes?.let { formatTime(it) } ?: "--:--"
    val end = task.routineEndMinutes?.let { formatTime(it) } ?: "--:--"
    return "$start — $end"
}

private fun formatTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
}

private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}