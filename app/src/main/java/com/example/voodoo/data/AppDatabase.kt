package com.example.voodoo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectContext::class,
        Task::class,
        TimerSession::class,
        AppSettings::class,
        ICalSyncSetting::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contextDao(): ProjectContextDao
    abstract fun taskDao(): TaskDao
    abstract fun timerSessionDao(): TimerSessionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun icalSyncDao(): ICalSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contextId INTEGER DEFAULT NULL,
                        parentId INTEGER DEFAULT NULL,
                        level INTEGER NOT NULL DEFAULT 0,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        result TEXT NOT NULL DEFAULT '',
                        isDone INTEGER NOT NULL DEFAULT 0,
                        priority INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        plannedStart INTEGER DEFAULT NULL,
                        plannedEnd INTEGER DEFAULT NULL,
                        reminderMinutesBefore INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER DEFAULT NULL,
                        timerActive INTEGER NOT NULL DEFAULT 0,
                        timerStartedAt INTEGER DEFAULT NULL,
                        FOREIGN KEY(contextId) REFERENCES contexts(id) ON DELETE SET NULL,
                        FOREIGN KEY(parentId) REFERENCES tasks_new(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    INSERT INTO tasks_new (contextId, title, description, isDone, sortOrder, createdAt)
                    SELECT contextId, name, '', 0, sortOrder, createdAt
                    FROM projects
                """)

                database.execSQL("""
                    INSERT INTO tasks_new (contextId, parentId, title, description, isDone, sortOrder, createdAt)
                    SELECT
                        p.contextId,
                        (SELECT t.id FROM tasks_new t WHERE t.title = p.name AND t.contextId = p.contextId LIMIT 1),
                        title,
                        description,
                        isDone,
                        sortOrder,
                        createdAt
                    FROM tasks
                    JOIN projects p ON tasks.projectId = p.id
                """)

                database.execSQL("DROP TABLE tasks")
                database.execSQL("DROP TABLE projects")
                database.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

                database.execSQL("CREATE INDEX index_tasks_contextId ON tasks(contextId)")
                database.execSQL("CREATE INDEX index_tasks_parentId ON tasks(parentId)")
                database.execSQL("CREATE INDEX index_tasks_level ON tasks(level)")

                database.execSQL("""
                    CREATE TABLE timer_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX index_timer_sessions_taskId ON timer_sessions(taskId)")

                database.execSQL("""
                    CREATE TABLE app_settings (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        darkTheme INTEGER NOT NULL DEFAULT 1,
                        fontSize INTEGER NOT NULL DEFAULT 16,
                        noContextName TEXT NOT NULL DEFAULT 'Без контекста'
                    )
                """)

                database.execSQL("""
                    INSERT INTO app_settings (id, darkTheme, fontSize, noContextName)
                    VALUES (1, 1, 16, 'Без контекста')
                """)

                database.execSQL("""
                    CREATE TABLE ical_sync_settings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        contextId INTEGER NOT NULL,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(contextId) REFERENCES contexts(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("CREATE INDEX index_ical_sync_settings_contextId ON ical_sync_settings(contextId)")
            }
        }

        // Миграция 5 → 6: добавление колонки comment в timer_sessions
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE timer_sessions ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voodoo_database"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}