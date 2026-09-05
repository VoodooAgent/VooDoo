package com.example.voodoo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectContextDao {
    @Query("SELECT * FROM contexts ORDER BY sortOrder ASC, id ASC")
    fun getAllContexts(): Flow<List<ProjectContext>>

    @Query("SELECT * FROM contexts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllContextsSync(): List<ProjectContext>

    @Query("SELECT * FROM contexts WHERE id = :contextId")
    fun getContextById(contextId: Long): Flow<ProjectContext?>

    @Insert
    suspend fun insert(context: ProjectContext): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(context: ProjectContext): Long

    @Insert
    suspend fun insertAll(contexts: List<ProjectContext>)

    @Update
    suspend fun update(context: ProjectContext)

    @Delete
    suspend fun delete(context: ProjectContext)

    @Query("DELETE FROM contexts")
    suspend fun deleteAll()

    @Query("UPDATE contexts SET sortOrder = :sortOrder WHERE id = :contextId")
    suspend fun updateSortOrder(contextId: Long, sortOrder: Int)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE contextId = :contextId ORDER BY sortOrder ASC, id ASC")
    fun getTasksByContext(contextId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE contextId IS NULL ORDER BY sortOrder ASC, id ASC")
    fun getTasksWithoutContext(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId ORDER BY sortOrder ASC, id ASC")
    fun getTasksByParent(parentId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE priority > 0 AND priority < 4 ORDER BY priority DESC, sortOrder ASC")
    fun getPriorityTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE priority = 4 ORDER BY contextId ASC, sortOrder ASC")
    fun getRoutineTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE timerActive = 1")
    suspend fun getActiveTimerTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE timerActive = 1")
    fun getActiveTimerTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE plannedStart IS NOT NULL AND plannedEnd IS NOT NULL")
    suspend fun getPlannedTasks(): List<Task>

    @Insert
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(task: Task): Long

    @Insert
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET isDone = :isDone, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, isDone: Boolean, completedAt: Long?)

    @Query("UPDATE tasks SET priority = :priority WHERE id = :taskId")
    suspend fun updatePriority(taskId: Long, priority: Int)

    @Query("UPDATE tasks SET sortOrder = :sortOrder WHERE id = :taskId")
    suspend fun updateSortOrder(taskId: Long, sortOrder: Int)

    @Query("UPDATE tasks SET contextId = :contextId WHERE id = :taskId")
    suspend fun updateContext(taskId: Long, contextId: Long?)

    @Query("UPDATE tasks SET parentId = :parentId, level = :level WHERE id = :taskId")
    suspend fun updateParent(taskId: Long, parentId: Long?, level: Int)

    @Query("UPDATE tasks SET timerActive = :active, timerStartedAt = :startedAt WHERE id = :taskId")
    suspend fun updateTimerStatus(taskId: Long, active: Boolean, startedAt: Long?)

    @Query("""
        UPDATE tasks 
        SET parentId = NULL, level = 0 
        WHERE id IN (SELECT id FROM tasks WHERE parentId IN (
            SELECT id FROM tasks WHERE parentId = :parentId
        ))
    """)
    suspend fun resetChildrenLevels(parentId: Long)
}

@Dao
interface TimerSessionDao {
    @Query("SELECT * FROM timer_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsByTask(taskId: Long): Flow<List<TimerSession>>

    @Query("SELECT * FROM timer_sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    suspend fun getSessionsByTaskSync(taskId: Long): List<TimerSession>

    @Query("SELECT * FROM timer_sessions ORDER BY startTime DESC")
    suspend fun getAllTimerSessionsSync(): List<TimerSession>

    @Query("SELECT * FROM timer_sessions")
    fun getAllSessions(): Flow<List<TimerSession>>

    @Insert
    suspend fun insert(session: TimerSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithId(session: TimerSession): Long

    @Insert
    suspend fun insertAll(sessions: List<TimerSession>)

    @Update
    suspend fun update(session: TimerSession)

    @Delete
    suspend fun delete(session: TimerSession)

    @Query("DELETE FROM timer_sessions WHERE taskId = :taskId")
    suspend fun deleteSessionsByTask(taskId: Long)

    @Query("DELETE FROM timer_sessions")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettings)
}

@Dao
interface ICalSyncDao {
    @Query("SELECT * FROM ical_sync_settings")
    fun getAllSyncSettings(): Flow<List<ICalSyncSetting>>

    @Query("SELECT contextId FROM ical_sync_settings WHERE enabled = 1")
    suspend fun getEnabledContextIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: ICalSyncSetting)

    @Delete
    suspend fun delete(setting: ICalSyncSetting)

    @Query("DELETE FROM ical_sync_settings")
    suspend fun deleteAll()
}

@Dao
interface CalendarContextDao {
    @Query("SELECT * FROM calendar_context_settings")
    fun getAllSettings(): Flow<List<CalendarContextSetting>>

    @Query("SELECT contextId FROM calendar_context_settings WHERE enabled = 1")
    suspend fun getEnabledContextIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: CalendarContextSetting)

    @Insert
    suspend fun insert(setting: CalendarContextSetting): Long

    @Delete
    suspend fun delete(setting: CalendarContextSetting)

    @Query("DELETE FROM calendar_context_settings")
    suspend fun deleteAll()

    @Query("DELETE FROM calendar_context_settings WHERE contextId = :contextId")
    suspend fun deleteByContextId(contextId: Long)
}