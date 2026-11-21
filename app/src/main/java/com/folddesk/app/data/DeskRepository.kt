package com.folddesk.app.data

import android.content.Context
import androidx.room.withTransaction
import java.time.LocalDate

class DeskRepository(private val db: AppDatabase) {
    private val dao = db.dao()

    val projects = dao.observeProjects()
    val tasks = dao.observeTasks()
    val subtasks = dao.observeSubtasks()
    val notes = dao.observeNotes()

    private fun now() = System.currentTimeMillis()

    suspend fun addTask(
        title: String,
        notes: String = "",
        projectId: Long? = null,
        status: TaskStatus = TaskStatus.TODO,
        due: Long? = null,
        important: Boolean = false,
    ): Long = dao.insertTask(
        Task(
            projectId = projectId,
            title = title,
            notes = notes,
            status = status,
            important = important,
            dueEpochDay = due,
            completedAt = if (status == TaskStatus.DONE) now() else null,
            position = dao.maxTaskPosition() + 1,
        ),
    )

    /** Смена статуса поднимает задачу наверх целевой колонки. */
    suspend fun setStatus(id: Long, status: TaskStatus) = dao.setTaskStatus(
        id = id,
        status = status,
        completedAt = if (status == TaskStatus.DONE) now() else null,
        position = dao.maxTaskPosition() + 1,
        now = now(),
    )

    suspend fun setTitle(id: Long, title: String) = dao.setTaskTitle(id, title, now())
    suspend fun setTaskNotes(id: Long, notes: String) = dao.setTaskNotes(id, notes, now())
    suspend fun setProject(id: Long, projectId: Long?) = dao.setTaskProject(id, projectId, now())
    suspend fun setDue(id: Long, due: Long?) = dao.setTaskDue(id, due, now())
    suspend fun setImportant(id: Long, important: Boolean) = dao.setTaskImportant(id, important, now())
    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    suspend fun addSubtask(taskId: Long, title: String) = dao.insertSubtask(Subtask(taskId = taskId, title = title))
    suspend fun setSubtaskDone(id: Long, done: Boolean) = dao.setSubtaskDone(id, done)
    suspend fun deleteSubtask(id: Long) = dao.deleteSubtask(id)

    suspend fun addNote(text: String): Long = dao.insertNote(Note(text = text))
    suspend fun setNoteText(id: Long, text: String) = dao.setNoteText(id, text, now())
    suspend fun setNotePinned(id: Long, pinned: Boolean) = dao.setNotePinned(id, pinned)
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    /** Заметка → задача: первая строка становится заголовком, остальное — описанием. */
    suspend fun noteToTask(noteId: Long, text: String, status: TaskStatus, projectId: Long?): Long =
        db.withTransaction {
            val title = if (text.isBlank()) "Новая задача" else noteTitle(text)
            val id = addTask(title = title, notes = noteBody(text), projectId = projectId, status = status)
            dao.deleteNote(noteId)
            id
        }

    suspend fun addProject(name: String, colorIndex: Int, emoji: String): Long =
        dao.insertProject(
            Project(name = name, colorIndex = colorIndex, emoji = emoji, position = dao.maxProjectPosition() + 1),
        )

    suspend fun deleteProject(id: Long) = db.withTransaction {
        dao.detachTasks(id)
        dao.deleteProject(id)
    }

    suspend fun seedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("folddesk", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        if (dao.projectCount() == 0) seed()
        prefs.edit().putBoolean("seeded", true).apply()
    }

    private suspend fun seed() = db.withTransaction {
        val today = LocalDate.now().toEpochDay()
        val work = dao.insertProject(Project(name = "Работа", colorIndex = 0, emoji = "💼", position = 0))
        val home = dao.insertProject(Project(name = "Личное", colorIndex = 2, emoji = "🏡", position = 1))
        val study = dao.insertProject(Project(name = "Учёба", colorIndex = 3, emoji = "📚", position = 2))

        val budget = addTask(
            title = "Подготовить бюджет Q4",
            notes = "Согласовать с финансовым отделом до пятницы.",
            projectId = work,
            status = TaskStatus.DOING,
            due = today + 1,
            important = true,
        )
        dao.insertSubtask(Subtask(taskId = budget, title = "Собрать расходы за квартал", done = true))
        dao.insertSubtask(Subtask(taskId = budget, title = "Согласовать с финотделом"))
        dao.insertSubtask(Subtask(taskId = budget, title = "Отправить на утверждение"))

        addTask(title = "Ответить на письмо клиента", projectId = work, due = today)
        addTask(title = "Созвон с командой по релизу", projectId = work, due = today + 2)
        addTask(title = "Купить подарок маме", projectId = home, due = today + 5)
        addTask(title = "Записаться в спортзал", projectId = home)
        addTask(title = "Оплатить интернет", projectId = home, status = TaskStatus.DONE)
        addTask(title = "Прочитать главу про Jetpack Compose", projectId = study, status = TaskStatus.DOING)

        dao.insertNote(Note(text = "Список покупок\n— молоко\n— кофе в зёрнах\n— авокадо"))
        dao.insertNote(Note(text = "Идея для статьи\nКак складной телефон меняет привычки работы"))
        dao.insertNote(
            Note(
                text = "Добро пожаловать в FoldDesk 👋\n" +
                    "Запишите мысль на внешнем экране и раскройте телефон: черновик уже ждёт в редакторе.\n\n" +
                    "Удерживайте заметку и перетащите её на доску, чтобы она стала задачей.",
                pinned = true,
            ),
        )
    }
}
