package com.folddesk.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,
    val emoji: String = "",
    val position: Int = 0,
)

enum class TaskStatus { TODO, DOING, DONE }

@Entity(tableName = "tasks", indices = [Index("projectId")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long? = null,
    val title: String,
    val notes: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val important: Boolean = false,
    /** Срок как LocalDate.toEpochDay() — без часовых поясов. */
    val dueEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    /** Чем больше, тем выше в колонке. */
    val position: Long = 0,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val done: Boolean = false,
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Первая непустая строка заметки — её заголовок. */
fun noteTitle(text: String): String =
    text.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "Пустая заметка"

/** Всё после заголовка. */
fun noteBody(text: String): String =
    text.lines().dropWhile { it.isBlank() }.drop(1).joinToString("\n").trim()

class Converters {
    @TypeConverter
    fun statusToString(status: TaskStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}
