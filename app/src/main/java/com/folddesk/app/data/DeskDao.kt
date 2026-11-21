package com.folddesk.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Обновления — точечными запросами по полям, а не целыми объектами:
 * так редактирование заголовка не затрёт одновременную смену статуса.
 */
@Dao
interface DeskDao {
    @Query("SELECT * FROM projects ORDER BY position, id")
    fun observeProjects(): Flow<List<Project>>

    @Query("SELECT * FROM tasks ORDER BY position DESC, id DESC")
    fun observeTasks(): Flow<List<Task>>

    @Query("SELECT * FROM subtasks ORDER BY id")
    fun observeSubtasks(): Flow<List<Subtask>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<Note>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun projectCount(): Int

    @Query("SELECT COALESCE(MAX(position), 0) FROM tasks")
    suspend fun maxTaskPosition(): Long

    @Query("SELECT COALESCE(MAX(position), 0) FROM projects")
    suspend fun maxProjectPosition(): Int

    // Проекты
    @Insert
    suspend fun insertProject(project: Project): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("UPDATE tasks SET projectId = NULL WHERE projectId = :projectId")
    suspend fun detachTasks(projectId: Long)

    // Задачи
    @Insert
    suspend fun insertTask(task: Task): Long

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE tasks SET title = :title, updatedAt = :now WHERE id = :id")
    suspend fun setTaskTitle(id: Long, title: String, now: Long)

    @Query("UPDATE tasks SET notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun setTaskNotes(id: Long, notes: String, now: Long)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, position = :position, updatedAt = :now WHERE id = :id")
    suspend fun setTaskStatus(id: Long, status: TaskStatus, completedAt: Long?, position: Long, now: Long)

    @Query("UPDATE tasks SET projectId = :projectId, updatedAt = :now WHERE id = :id")
    suspend fun setTaskProject(id: Long, projectId: Long?, now: Long)

    @Query("UPDATE tasks SET dueEpochDay = :due, updatedAt = :now WHERE id = :id")
    suspend fun setTaskDue(id: Long, due: Long?, now: Long)

    @Query("UPDATE tasks SET important = :important, updatedAt = :now WHERE id = :id")
    suspend fun setTaskImportant(id: Long, important: Boolean, now: Long)

    // Подзадачи
    @Insert
    suspend fun insertSubtask(subtask: Subtask): Long

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: Long)

    // Заметки
    @Insert
    suspend fun insertNote(note: Note): Long

    @Query("UPDATE notes SET text = :text, updatedAt = :now WHERE id = :id")
    suspend fun setNoteText(id: Long, text: String, now: Long)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setNotePinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)
}
