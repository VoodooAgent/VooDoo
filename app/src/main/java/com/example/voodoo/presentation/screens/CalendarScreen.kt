package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.*
import com.example.voodoo.presentation.CalendarViewMode
import com.example.voodoo.presentation.CalendarViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val contexts by viewModel.contexts.collectAsState()
    val calendarSettings by viewModel.calendarSettings.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    val enabledContextIds = remember(calendarSettings) {
        calendarSettings.filter { it.enabled }.map { it.contextId }.toSet()
    }

    val filteredTasks = remember(tasks, enabledContextIds, settings.showTasks) {
        if (!settings.showTasks) emptyList()
        else tasks.filter { it.contextId != null && it.contextId in enabledContextIds }
    }

    val filteredSessions = remember(sessions, tasks, enabledContextIds, settings.showSessions) {
        if (!settings.showSessions) emptyList()
        else sessions.filter { session ->
            val task = tasks.find { it.id == session.taskId }
            task?.contextId != null && task?.contextId in enabledContextIds
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.DAY) }) {
                            Text("Д", color = if (viewMode == CalendarViewMode.DAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.WEEK) }) {
                            Text("Н", color = if (viewMode == CalendarViewMode.WEEK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) }) {
                            Text("М", color = if (viewMode == CalendarViewMode.MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        TwoCheckBoxesIcon()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (viewMode) {
                CalendarViewMode.MONTH -> MonthView(
                    selectedDate = selectedDate,
                    onDateClick = { viewModel.selectDate(it) },
                    filteredTasks = filteredTasks,
                    filteredSessions = filteredSessions,
                    contexts = contexts
                )
                CalendarViewMode.WEEK -> WeekView(
                    selectedDate = selectedDate,
                    onDateClick = { viewModel.selectDate(it) },
                    filteredTasks = filteredTasks,
                    filteredSessions = filteredSessions,
                    contexts = contexts
                )
                CalendarViewMode.DAY -> DayView(
                    selectedDate = selectedDate,
                    filteredTasks = filteredTasks,
                    filteredSessions = filteredSessions,
                    contexts = contexts,
                    tasks = tasks,
                    onTaskClick = onTaskClick
                )
                else -> {}
            }
        }
    }
}

@Composable
fun TwoCheckBoxesIcon() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .border(1.dp, MaterialTheme.colorScheme.onSurface, RectangleShape)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.CheckBox,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MonthView(
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    filteredTasks: List<Task>,
    filteredSessions: List<TimerSession>,
    contexts: List<ProjectContext>
) {
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val startMonth = currentMonth.minusMonths(100)
    val endMonth = currentMonth.plusMonths(100)
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    HorizontalCalendar(
        state = state,
        dayContent = { day: CalendarDay ->
            DayCell(
                day = day,
                isSelected = day.date == selectedDate,
                tasks = filteredTasks.filter { task ->
                    task.plannedStart?.let { start ->
                        val taskDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        taskDate == day.date
                    } ?: false
                },
                sessions = filteredSessions.filter { session ->
                    val sessionDate = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    sessionDate == day.date
                },
                contexts = contexts,
                onClick = { onDateClick(day.date) }
            )
        },
        monthHeader = { month ->
            MonthHeader(month, daysOfWeek)
        }
    )
}

@Composable
fun MonthHeader(month: CalendarMonth, daysOfWeek: List<DayOfWeek>) {
    Column {
        Text(
            text = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    tasks: List<Task>,
    sessions: List<TimerSession>,
    contexts: List<ProjectContext>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable(enabled = day.position == DayPosition.MonthDate) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val allItems = tasks + sessions
                val visibleItems = allItems.take(3)
                visibleItems.forEach { item ->
                    val color = when (item) {
                        is Task -> contexts.find { it.id == item.contextId }?.color?.let { Color(it) } ?: Color.Gray
                        is TimerSession -> MaterialTheme.colorScheme.secondary
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, CircleShape)
                            .padding(1.dp)
                    )
                }
                if (allItems.size > 3) {
                    Text(
                        text = "+${allItems.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    filteredTasks: List<Task>,
    filteredSessions: List<TimerSession>,
    contexts: List<ProjectContext>
) {
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val startMonth = currentMonth.minusMonths(100)
    val endMonth = currentMonth.plusMonths(100)
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberWeekCalendarState(
        startDate = startMonth.atDay(1),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = daysOfWeek.first()
    )

    WeekCalendar(
        state = state,
        dayContent = { day: WeekDay ->
            WeekDayCell(
                day = day,
                isSelected = day.date == selectedDate,
                tasks = filteredTasks.filter { task ->
                    task.plannedStart?.let { start ->
                        val taskDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
                        taskDate == day.date
                    } ?: false
                },
                sessions = filteredSessions.filter { session ->
                    val sessionDate = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                    sessionDate == day.date
                },
                contexts = contexts,
                onClick = { onDateClick(day.date) }
            )
        },
        weekHeader = { week: Week ->
            WeekHeader(week, daysOfWeek)
        }
    )
}

@Composable
fun WeekHeader(week: Week, daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        daysOfWeek.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun WeekDayCell(
    day: WeekDay,
    isSelected: Boolean,
    tasks: List<Task>,
    sessions: List<TimerSession>,
    contexts: List<ProjectContext>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val allItems = tasks + sessions
                val visibleItems = allItems.take(3)
                visibleItems.forEach { item ->
                    val color = when (item) {
                        is Task -> contexts.find { it.id == item.contextId }?.color?.let { Color(it) } ?: Color.Gray
                        is TimerSession -> MaterialTheme.colorScheme.secondary
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, CircleShape)
                            .padding(1.dp)
                    )
                }
                if (allItems.size > 3) {
                    Text(
                        text = "+${allItems.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DayView(
    selectedDate: LocalDate,
    filteredTasks: List<Task>,
    filteredSessions: List<TimerSession>,
    contexts: List<ProjectContext>,
    tasks: List<Task>,
    onTaskClick: (Long) -> Unit
) {
    val dayTasks = filteredTasks.filter { task ->
        task.plannedStart?.let { start ->
            val taskDate = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
            taskDate == selectedDate
        } ?: false
    }
    val daySessions = filteredSessions.filter { session ->
        val sessionDate = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
        sessionDate == selectedDate
    }

    LazyColumn {
        item {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (dayTasks.isNotEmpty()) {
            item {
                Text(
                    text = "Задачи",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(dayTasks) { task ->
                TaskCalendarItem(task = task, contexts = contexts, onClick = { onTaskClick(task.id) })
            }
        }
        if (daySessions.isNotEmpty()) {
            item {
                Text(
                    text = "Сессии",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(daySessions) { session ->
                SessionCalendarItem(session = session, tasks = tasks, contexts = contexts, onClick = {
                    onTaskClick(session.taskId)
                })
            }
        }
        if (dayTasks.isEmpty() && daySessions.isEmpty()) {
            item {
                Text(
                    text = "Нет событий на этот день",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun TaskCalendarItem(
    task: Task,
    contexts: List<ProjectContext>,
    onClick: () -> Unit
) {
    val context = contexts.find { it.id == task.contextId }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = context?.color?.let { Color(it).copy(alpha = 0.2f) } ?: MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(context?.color?.let { Color(it) } ?: Color.Gray, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                if (task.plannedStart != null) {
                    val time = Instant.ofEpochMilli(task.plannedStart).atZone(ZoneId.systemDefault()).toLocalTime()
                    Text(
                        text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCalendarItem(
    session: TimerSession,
    tasks: List<Task>,
    contexts: List<ProjectContext>,
    onClick: () -> Unit
) {
    val task = tasks.find { it.id == session.taskId }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(task?.title ?: "Удаленная задача", style = MaterialTheme.typography.titleMedium)
                val startTime = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalTime()
                val endTime = Instant.ofEpochMilli(session.endTime).atZone(ZoneId.systemDefault()).toLocalTime()
                Text(
                    text = "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}