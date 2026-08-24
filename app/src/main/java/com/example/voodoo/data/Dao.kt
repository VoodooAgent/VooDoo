package com.example.voodoo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY sortOrder ASC, id ASC")
    fun getAllProjectsByManual(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY name ASC, id ASC")
    fun getAllProjectsByName(): Flow<List<Project>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC, id DESC")
    fun getAllProjectsByDate(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: Long): Flow<Project?>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY isDone ASC, sortOrder ASC, id ASC")
    fun getTasksByProjectByManual(projectId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY isDone ASC, title ASC, id ASC")
    fun getTasksByProjectByName(projectId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY isDone ASC, createdAt DESC, id DESC")
    fun getTasksByProjectByDate(projectId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isDone = 0 ORDER BY sortOrder ASC, id ASC LIMIT 1")
    fun getTopTaskByProject(projectId: Long): Flow<Task?>

    @Insert
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}