package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.data.Task
import com.example.voodoo.data.TimerSession
import com.example.voodoo.presentation.CalendarViewMode
import com.example.voodoo.presentation.CalendarViewModel
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.Week
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class TimelineEvent(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val title: String,
    val color: Color,
    val isSession: Boolean,
    val taskId: Long
)

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
        if (!settings.showTasks) {
            emptyList()
        } else {
            tasks.filter { it.contextId != null && it.contextId in enabledContextIds }
        }
    }

    val filteredSessions = remember(sessions, tasks, enabledContextIds, settings.showSessions) {
        if (!settings.showSessions) {
            emptyList()
        } else {
            sessions.filter { session ->
                val task = tasks.find { it.id == session.taskId }
                task?.contextId != null && task.contextId in enabledContextIds
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.DAY) }) {
                            Text(
                                text = "Д",
                                color = if (viewMode == CalendarViewMode.DAY) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.WEEK) }) {
                            Text(
                                text = "Н",
                                color = if (viewMode == CalendarViewMode.WEEK) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        TextButton(onClick = { viewModel.setViewMode(CalendarViewMode.MONTH) }) {
                            Text(
                                text = "М",
                                color = if (viewMode == CalendarViewMode.MONTH) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
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

                else -> Unit
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
            contentDescription = "Настройки календаря",
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
            val dayTasks = filteredTasks.filter { task ->
                task.plannedStart?.let { start ->
                    millisToLocalDate(start) == day.date
                } ?: false
            }

            val daySessions = filteredSessions.filter { session ->
                millisToLocalDate(session.startTime) == day.date
            }

            DayCell(
                day = day,
                isSelected = day.date == selectedDate,
                tasks = dayTasks,
                sessions = daySessions,
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
fun MonthHeader(
    month: CalendarMonth,
    daysOfWeek: List<DayOfWeek>
) {
    Column {
        Text(
            text = "${month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.yearMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
                },
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val allItems: List<Any> = tasks + sessions
                val visibleItems = allItems.take(3)

                visibleItems.forEach { item ->
                    val color = when (item) {
                        is Task -> {
                            contexts.find { it.id == item.contextId }?.color?.let { Color(it) }
                                ?: Color.Gray
                        }

                        is TimerSession -> MaterialTheme.colorScheme.secondary

                        else -> Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
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
            val dayTasks = filteredTasks.filter { task ->
                task.plannedStart?.let { start ->
                    millisToLocalDate(start) == day.date
                } ?: false
            }

            val daySessions = filteredSessions.filter { session ->
                millisToLocalDate(session.startTime) == day.date
            }

            WeekDayCell(
                day = day,
                isSelected = day.date == selectedDate,
                tasks = dayTasks,
                sessions = daySessions,
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
fun WeekHeader(
    week: Week,
    daysOfWeek: List<DayOfWeek>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val allItems: List<Any> = tasks + sessions
                val visibleItems = allItems.take(3)

                visibleItems.forEach { item ->
                    val color = when (item) {
                        is Task -> {
                            contexts.find { it.id == item.contextId }?.color?.let { Color(it) }
                                ?: Color.Gray
                        }

                        is TimerSession -> MaterialTheme.colorScheme.secondary

                        else -> Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
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
            millisToLocalDate(start) == selectedDate
        } ?: false
    }

    val daySessions = filteredSessions.filter { session ->
        millisToLocalDate(session.startTime) == selectedDate
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = selectedDate.format(
                DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DayTimeline(
                dayTasks = dayTasks,
                daySessions = daySessions,
                contexts = contexts,
                allTasks = tasks,
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
fun DayTimeline(
    dayTasks: List<Task>,
    daySessions: List<TimerSession>,
    contexts: List<ProjectContext>,
    allTasks: List<Task>,
    onTaskClick: (Long) -> Unit
) {
    val hourHeightDp = 60
    val timelineHeightDp = hourHeightDp * 24
    val sessionColor = MaterialTheme.colorScheme.tertiary

    val events = remember(dayTasks, daySessions, contexts, allTasks, sessionColor) {
        val taskEvents = dayTasks.mapNotNull { task ->
            task.plannedStart?.let { startMillis ->
                val startTime = millisToLocalTime(startMillis)
                val endTime = task.plannedEnd?.let { endMillis ->
                    millisToLocalTime(endMillis)
                } ?: startTime.plusHours(1)

                TimelineEvent(
                    startTime = startTime,
                    endTime = endTime,
                    title = task.title,
                    color = contexts.find { it.id == task.contextId }?.color?.let { Color(it) }
                        ?: Color.Gray,
                    isSession = false,
                    taskId = task.id
                )
            }
        }

        val sessionEvents = daySessions.map { session ->
            val task = allTasks.find { it.id == session.taskId }

            TimelineEvent(
                startTime = millisToLocalTime(session.startTime),
                endTime = millisToLocalTime(session.endTime),
                title = task?.title ?: "Сессия",
                color = sessionColor,
                isSession = true,
                taskId = session.taskId
            )
        }

        (taskEvents + sessionEvents).sortedBy { it.startTime }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(timelineHeightDp.dp)
    ) {
        repeat(24) { hour ->
            val topOffsetDp = hourHeightDp * hour

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(start = 56.dp, end = 8.dp)
                    .offset(y = topOffsetDp.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            )

            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .offset(y = (topOffsetDp - 8).dp)
            )
        }

        events.forEach { event ->
            TimelineEventCard(
                event = event,
                hourHeightDp = hourHeightDp,
                onTaskClick = onTaskClick
            )
        }
    }
}

@Composable
fun TimelineEventCard(
    event: TimelineEvent,
    hourHeightDp: Int,
    onTaskClick: (Long) -> Unit
) {
    val startMinutes = event.startTime.hour * 60 + event.startTime.minute
    val rawEndMinutes = event.endTime.hour * 60 + event.endTime.minute

    val durationMinutes = if (rawEndMinutes > startMinutes) {
        rawEndMinutes - startMinutes
    } else {
        24 * 60 - startMinutes
    }

    val topOffsetDp = (hourHeightDp * startMinutes / 60f).toInt()
    val eventHeightDp = (hourHeightDp * durationMinutes / 60f).toInt().coerceAtLeast(22)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 12.dp)
            .height(eventHeightDp.dp)
            .offset(y = topOffsetDp.dp)
            .clickable { onTaskClick(event.taskId) },
        colors = CardDefaults.cardColors(
            containerColor = event.color.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(event.color, MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                Text(
                    text = "${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                        event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )

                if (event.isSession) {
                    Text(
                        text = "Сессия",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
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
        colors = CardDefaults.cardColors(
            containerColor = context?.color?.let { Color(it).copy(alpha = 0.2f) }
                ?: MaterialTheme.colorScheme.surfaceVariant
        )
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

                task.plannedStart?.let { plannedStart ->
                    Text(
                        text = millisToLocalTime(plannedStart).format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        ),
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
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
                Text(
                    text = task?.title ?: "Удаленная задача",
                    style = MaterialTheme.typography.titleMedium
                )

                val startTime = millisToLocalTime(session.startTime)
                val endTime = millisToLocalTime(session.endTime)

                Text(
                    text = "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${
                        endTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun millisToLocalTime(millis: Long): LocalTime {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
}