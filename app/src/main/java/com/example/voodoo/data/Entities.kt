package com.example.voodoo.data

import androidx.room.*

@Entity(tableName = "contexts")
data class ProjectContext(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long = 0xFFE0E0E0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectContext::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contextId"]),
        Index(value = ["parentId"]),
        Index(value = ["level"])
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contextId: Long? = null,
    val parentId: Long? = null,
    val level: Int = 0,
    val title: String,
    val description: String = "",
    val result: String = "",
    val isDone: Boolean = false,
    val priority: Int = 0,
    val sortOrder: Int = 0,

    val plannedStart: Long? = null,
    val plannedEnd: Long? = null,
    val reminderMinutesBefore: Int? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,

    val timerActive: Boolean = false,
    val timerStartedAt: Long? = null
)

@Entity(
    tableName = "timer_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class TimerSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val darkTheme: Boolean = true,
    val fontSize: Int = 16,
    val noContextName: String = "Без контекста"
)

@Entity(
    tableName = "ical_sync_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectContext::class,
            parentColumns = ["id"],
            childColumns = ["contextId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contextId"])]
)
data class ICalSyncSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contextId: Long,
    val enabled: Boolean = true
)

data class TaskWithChildren(
    val task: Task,
    val children: List<TaskWithChildren> = emptyList(),
    val isExpanded: Boolean = false
)

data class TaskWithSessions(
    val task: Task,
    val sessions: List<TimerSession> = emptyList()
) {
    val totalDuration: Long
        get() = sessions.sumOf { it.duration }
}

data class ProjectContextWithTasks(
    val context: ProjectContext,
    val tasks: List<Task>
)