package com.folddesk.app.ui

import com.folddesk.app.data.Note
import com.folddesk.app.data.Project
import com.folddesk.app.data.Subtask
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus

/** Снимок всех данных приложения — их немного, поэтому держим целиком в памяти. */
data class DeskData(
    val projects: List<Project> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val notes: List<Note> = emptyList(),
    val loaded: Boolean = false,
) {
    val subtasksByTask: Map<Long, List<Subtask>> by lazy { subtasks.groupBy { it.taskId } }
    val projectById: Map<Long, Project> by lazy { projects.associateBy { it.id } }

    fun task(id: Long): Task? = tasks.firstOrNull { it.id == id }
    fun note(id: Long): Note? = notes.firstOrNull { it.id == id }
}

sealed interface Scope {
    data object Today : Scope
    data object All : Scope
    data class InProject(val id: Long) : Scope
}

sealed interface Selection {
    data object None : Selection
    /** Черновик быстрой записи, начатый на внешнем экране. */
    data object Draft : Selection
    data class OfTask(val id: Long) : Selection
    data class OfNote(val id: Long) : Selection
}

enum class BoardMode { Board, List }

enum class FocusPhase { Work, Break }

enum class LayoutMode { Cover, Desk, Tabletop }

val TaskStatus.label: String
    get() = when (this) {
        TaskStatus.TODO -> "Сделать"
        TaskStatus.DOING -> "В работе"
        TaskStatus.DONE -> "Готово"
    }

private fun Task.isDueBy(day: Long) = dueEpochDay != null && dueEpochDay <= day

fun DeskData.tasksIn(scope: Scope, query: String): List<Task> {
    val today = todayEpoch()
    val base = when (scope) {
        Scope.All -> tasks
        Scope.Today -> tasks.filter {
            when (it.status) {
                TaskStatus.DONE -> isToday(it.completedAt)
                TaskStatus.DOING -> true
                TaskStatus.TODO -> it.isDueBy(today)
            }
        }
        is Scope.InProject -> tasks.filter { it.projectId == scope.id }
    }
    val q = query.trim()
    if (q.isEmpty()) return base
    return base.filter { it.title.contains(q, ignoreCase = true) || it.notes.contains(q, ignoreCase = true) }
}

/** Прогресс дня: (выполнено, всего) среди задач со сроком «сегодня или раньше». */
fun DeskData.todayStats(): Pair<Int, Int> {
    val today = todayEpoch()
    val relevant = tasks.filter {
        if (it.status == TaskStatus.DONE) isToday(it.completedAt) && (it.dueEpochDay == null || it.dueEpochDay <= today)
        else it.isDueBy(today)
    }
    return relevant.count { it.status == TaskStatus.DONE } to relevant.size
}

/** Самая актуальная открытая задача: ближайший срок, затем «в работе», затем важные. */
fun DeskData.nextUp(): Task? = tasks
    .filter { it.status != TaskStatus.DONE }
    .sortedWith(
        compareBy<Task>(
            { it.dueEpochDay ?: Long.MAX_VALUE },
            { if (it.status == TaskStatus.DOING) 0 else 1 },
            { if (it.important) 0 else 1 },
        ),
    )
    .firstOrNull()

fun DeskData.scopeTitle(scope: Scope): String = when (scope) {
    Scope.Today -> "Сегодня"
    Scope.All -> "Все задачи"
    is Scope.InProject -> projectById[scope.id]?.let { "${it.emoji} ${it.name}".trim() } ?: "Проект"
}
