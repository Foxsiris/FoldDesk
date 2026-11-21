package com.folddesk.app.ui

import android.app.Application
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.folddesk.app.FoldDeskApp
import com.folddesk.app.data.Note
import com.folddesk.app.data.Subtask
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.data.noteBody
import com.folddesk.app.data.noteTitle
import com.folddesk.app.ui.drag.DragPayload
import com.folddesk.app.ui.drag.DropTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Единое состояние для обоих экранов. Activity не пересоздаётся при складывании,
 * а всё, что пользователь «держит в руках» (черновик, выбор, таймер, правки), живёт здесь —
 * поэтому внешний и внутренний экраны показывают одно и то же рабочее пространство.
 */
class DeskViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = (app as FoldDeskApp).repository

    val data: StateFlow<DeskData> =
        combine(repo.projects, repo.tasks, repo.subtasks, repo.notes) { p, t, s, n ->
            DeskData(p, t, s, n, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DeskData())

    private val d get() = data.value

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    fun toast(text: String) {
        messages.tryEmit(text)
    }

    // ── Навигация и раскладка ───────────────────────────────────────────────

    var scope by mutableStateOf<Scope>(Scope.All)
    var selection by mutableStateOf<Selection>(Selection.None)
        private set
    var boardMode by mutableStateOf(BoardMode.Board)
    var search by mutableStateOf("")
    var leftPaneVisible by mutableStateOf(true)
    /** Панель спрятана автоматически (открыли задачу на узком экране) — вернём при закрытии. */
    var sidebarAutoHidden = false
    var leftPaneWidth by mutableFloatStateOf(236f)
        private set
    var rightPaneWidth by mutableFloatStateOf(340f)
        private set
    var deskFocus by mutableStateOf(false)
    var layoutMode by mutableStateOf(LayoutMode.Cover)
        private set

    fun resizeLeft(deltaDp: Float) {
        leftPaneWidth = (leftPaneWidth + deltaDp).coerceIn(200f, 360f)
    }

    fun resizeRight(deltaDp: Float) {
        rightPaneWidth = (rightPaneWidth + deltaDp).coerceIn(280f, 560f)
    }

    fun select(next: Selection) {
        val prev = selection
        // Пустая заметка, из которой ушли, не нужна
        if (prev is Selection.OfNote && prev != next) {
            val text = noteBuffer[prev.id] ?: d.note(prev.id)?.text
            if (text != null && text.isBlank()) viewModelScope.launch { repo.deleteNote(prev.id) }
        }
        selection = next
    }

    /** Ключевой сценарий: черновик с внешнего экрана сам открывается в редакторе при раскрытии. */
    fun onLayoutChanged(mode: LayoutMode) {
        val prev = layoutMode
        layoutMode = mode
        if (mode == LayoutMode.Desk && prev != LayoutMode.Desk && draft.isNotBlank() && selection == Selection.None) {
            selection = Selection.Draft
        }
        if (mode == LayoutMode.Cover && selection == Selection.Draft) selection = Selection.None
    }

    // ── Черновик быстрой записи ─────────────────────────────────────────────

    var draft by mutableStateOf(saved.get<String>(KEY_DRAFT).orEmpty())
        private set
    var captureRequest by mutableIntStateOf(0)
        private set

    fun updateDraft(text: String) {
        draft = text
        saved[KEY_DRAFT] = text
    }

    fun appendDraft(text: String) = updateDraft(if (draft.isBlank()) text else draft.trimEnd() + " " + text)

    fun requestCapture() {
        captureRequest++
    }

    fun draftToNote(open: Boolean = false) {
        val text = draft.trim()
        if (text.isEmpty()) return
        updateDraft("")
        viewModelScope.launch {
            val id = repo.addNote(text)
            selection = if (open) Selection.OfNote(id) else if (selection == Selection.Draft) Selection.None else selection
            toast("Сохранено в заметки")
        }
    }

    fun draftToTask(open: Boolean = false) {
        val text = draft.trim()
        if (text.isEmpty()) return
        updateDraft("")
        viewModelScope.launch {
            val id = repo.addTask(
                title = noteTitle(text),
                notes = noteBody(text),
                projectId = (scope as? Scope.InProject)?.id,
                due = if (scope == Scope.Today) todayEpoch() else null,
            )
            selection = if (open) Selection.OfTask(id) else if (selection == Selection.Draft) Selection.None else selection
            toast("Задача создана")
        }
    }

    // ── Буферы правок ───────────────────────────────────────────────────────
    // Текст хранится здесь и пишется в БД с задержкой: при переходе между экранами
    // редактор на другом экране сразу видит последние набранные символы.

    private val noteBuffer = mutableStateMapOf<Long, String>()
    private val titleBuffer = mutableStateMapOf<Long, String>()
    private val taskNotesBuffer = mutableStateMapOf<Long, String>()
    private val saveJobs = HashMap<String, Job>()

    private fun debounce(key: String, block: suspend () -> Unit) {
        saveJobs[key]?.cancel()
        saveJobs[key] = viewModelScope.launch {
            delay(350)
            block()
        }
    }

    fun noteText(note: Note): String = noteBuffer[note.id] ?: note.text
    fun taskTitle(task: Task): String = titleBuffer[task.id] ?: task.title
    fun taskNotes(task: Task): String = taskNotesBuffer[task.id] ?: task.notes

    fun editNote(id: Long, text: String) {
        noteBuffer[id] = text
        debounce("n$id") { repo.setNoteText(id, text) }
    }

    fun editTaskTitle(id: Long, text: String) {
        titleBuffer[id] = text
        debounce("t$id") { repo.setTitle(id, text) }
    }

    fun editTaskNotes(id: Long, text: String) {
        taskNotesBuffer[id] = text
        debounce("tn$id") { repo.setTaskNotes(id, text) }
    }

    // ── Задачи ──────────────────────────────────────────────────────────────

    fun addTask(title: String, scopeOverride: Scope? = null, status: TaskStatus = TaskStatus.TODO) {
        val s = scopeOverride ?: scope
        viewModelScope.launch {
            repo.addTask(
                title = title,
                projectId = (s as? Scope.InProject)?.id,
                status = status,
                due = if (s == Scope.Today) todayEpoch() else null,
            )
        }
    }

    fun toggleDone(task: Task) = setStatus(task.id, if (task.status == TaskStatus.DONE) TaskStatus.TODO else TaskStatus.DONE)

    fun setStatus(id: Long, status: TaskStatus) {
        viewModelScope.launch { repo.setStatus(id, status) }
    }

    fun setProject(id: Long, projectId: Long?) {
        viewModelScope.launch { repo.setProject(id, projectId) }
    }

    fun setDue(id: Long, epochDay: Long?) {
        viewModelScope.launch { repo.setDue(id, epochDay) }
    }

    fun toggleImportant(task: Task) {
        viewModelScope.launch { repo.setImportant(task.id, !task.important) }
    }

    fun deleteTask(id: Long) {
        if (selection == Selection.OfTask(id)) selection = Selection.None
        if (focusTaskId == id) focusTaskId = null
        viewModelScope.launch { repo.deleteTask(id) }
        toast("Задача удалена")
    }

    fun addSubtask(taskId: Long, title: String) {
        viewModelScope.launch { repo.addSubtask(taskId, title) }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch { repo.setSubtaskDone(subtask.id, !subtask.done) }
    }

    fun deleteSubtask(id: Long) {
        viewModelScope.launch { repo.deleteSubtask(id) }
    }

    // ── Заметки ─────────────────────────────────────────────────────────────

    fun newNote() {
        viewModelScope.launch {
            val id = repo.addNote("")
            select(Selection.OfNote(id))
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repo.setNotePinned(note.id, !note.pinned) }
    }

    fun deleteNote(id: Long) {
        if (selection == Selection.OfNote(id)) selection = Selection.None
        viewModelScope.launch { repo.deleteNote(id) }
        toast("Заметка удалена")
    }

    fun noteToTask(noteId: Long, status: TaskStatus = TaskStatus.TODO, projectId: Long? = (scope as? Scope.InProject)?.id) {
        val text = (noteBuffer[noteId] ?: d.note(noteId)?.text).orEmpty().trim()
        saveJobs.remove("n$noteId")?.cancel()
        viewModelScope.launch {
            val id = repo.noteToTask(noteId, text, status, projectId)
            noteBuffer.remove(noteId)
            if (selection == Selection.OfNote(noteId) || layoutMode == LayoutMode.Desk) selection = Selection.OfTask(id)
            toast("Заметка стала задачей")
        }
    }

    // ── Проекты ─────────────────────────────────────────────────────────────

    fun addProject(name: String, colorIndex: Int, emoji: String) {
        viewModelScope.launch {
            val id = repo.addProject(name, colorIndex, emoji)
            scope = Scope.InProject(id)
        }
    }

    fun deleteProject(id: Long) {
        if (scope == Scope.InProject(id)) scope = Scope.All
        viewModelScope.launch { repo.deleteProject(id) }
        toast("Проект удалён, задачи сохранены")
    }

    // ── Перетаскивание ──────────────────────────────────────────────────────

    fun handleDrop(payload: DragPayload, target: DropTarget) {
        when (payload) {
            is DragPayload.OfTask -> when (target) {
                is DropTarget.Column ->
                    if (payload.task.status != target.status) setStatus(payload.task.id, target.status)
                is DropTarget.ToProject -> {
                    setProject(payload.task.id, target.projectId)
                    d.projectById[target.projectId]?.let { toast("Перенесено в «${it.name}»") }
                }
            }
            is DragPayload.OfNote -> when (target) {
                is DropTarget.Column -> noteToTask(payload.note.id, target.status)
                is DropTarget.ToProject -> noteToTask(payload.note.id, TaskStatus.TODO, target.projectId)
            }
        }
    }

    // ── Фокус-таймер (Pomodoro) ─────────────────────────────────────────────

    var focusPhase by mutableStateOf(FocusPhase.Work)
        private set
    var focusMinutes by mutableIntStateOf(25)
        private set
    var focusRemainingMs by mutableLongStateOf(25 * 60_000L)
        private set
    var focusRunning by mutableStateOf(false)
        private set
    var focusTaskId by mutableStateOf<Long?>(null)
    var focusSessions by mutableIntStateOf(0)
        private set
    private var tickJob: Job? = null

    private fun breakMinutes() = if (focusSessions > 0 && focusSessions % 4 == 0) 15 else 5

    fun phaseDurationMs(): Long = (if (focusPhase == FocusPhase.Work) focusMinutes else breakMinutes()) * 60_000L

    fun toggleFocusTimer() = if (focusRunning) pauseFocus() else startFocus()

    private fun startFocus() {
        if (focusRemainingMs <= 0) focusRemainingMs = phaseDurationMs()
        focusRunning = true
        val endAt = SystemClock.elapsedRealtime() + focusRemainingMs
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    focusRemainingMs = 0
                    finishPhase()
                    break
                }
                focusRemainingMs = left
                delay(200)
            }
        }
    }

    private fun pauseFocus() {
        tickJob?.cancel()
        focusRunning = false
    }

    fun resetFocus() {
        pauseFocus()
        focusRemainingMs = phaseDurationMs()
    }

    fun skipPhase() {
        pauseFocus()
        switchPhase(countSession = false)
    }

    fun setFocusPreset(minutes: Int) {
        if (focusRunning) return
        focusMinutes = minutes
        focusPhase = FocusPhase.Work
        focusRemainingMs = minutes * 60_000L
    }

    fun startFocusOn(taskId: Long) {
        focusTaskId = taskId
        if (d.task(taskId)?.status == TaskStatus.TODO) setStatus(taskId, TaskStatus.DOING)
        deskFocus = true
        if (!focusRunning) {
            if (focusPhase != FocusPhase.Work) {
                focusPhase = FocusPhase.Work
                focusRemainingMs = phaseDurationMs()
            }
            startFocus()
        }
    }

    private fun finishPhase() {
        focusRunning = false
        buzz()
        val wasWork = focusPhase == FocusPhase.Work
        switchPhase(countSession = true)
        toast(if (wasWork) "Отличная работа! Время перерыва ☕" else "Перерыв окончен — снова в фокус")
    }

    private fun switchPhase(countSession: Boolean) {
        if (focusPhase == FocusPhase.Work) {
            if (countSession) focusSessions++
            focusPhase = FocusPhase.Break
        } else {
            focusPhase = FocusPhase.Work
        }
        focusRemainingMs = phaseDurationMs()
    }

    private fun buzz() {
        val ctx = getApplication<Application>()
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            ctx.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180, 120, 320), -1))
    }

    init {
        viewModelScope.launch { repo.seedIfNeeded(app) }
    }

    private companion object {
        const val KEY_DRAFT = "draft"
    }
}
