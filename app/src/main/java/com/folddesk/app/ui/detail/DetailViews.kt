package com.folddesk.app.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folddesk.app.data.Note
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.ui.DeskData
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.FocusPhase
import com.folddesk.app.ui.Selection
import com.folddesk.app.ui.components.EmptyState
import com.folddesk.app.ui.components.Eyebrow
import com.folddesk.app.ui.components.FirstLineAsTitle
import com.folddesk.app.ui.components.Glyph
import com.folddesk.app.ui.components.GroupRow
import com.folddesk.app.ui.components.GroupedRows
import com.folddesk.app.ui.components.IosSwitch
import com.folddesk.app.ui.components.LiquidCheck
import com.folddesk.app.ui.components.Orb
import com.folddesk.app.ui.components.Pill
import com.folddesk.app.ui.components.PillButton
import com.folddesk.app.ui.components.PillSize
import com.folddesk.app.ui.components.PillStyle
import com.folddesk.app.ui.components.PlainField
import com.folddesk.app.ui.components.Segmented
import com.folddesk.app.ui.components.SectionHeader
import com.folddesk.app.ui.components.TaskRow
import com.folddesk.app.ui.components.flat
import com.folddesk.app.ui.components.hueOf
import com.folddesk.app.ui.components.pressable
import com.folddesk.app.ui.dueLabel
import com.folddesk.app.ui.formatDateTime
import com.folddesk.app.ui.formatTimer
import com.folddesk.app.ui.greeting
import com.folddesk.app.ui.isToday
import com.folddesk.app.ui.label
import com.folddesk.app.ui.relativeTime
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.projectHue
import com.folddesk.app.ui.theme.projectPaletteSize
import com.folddesk.app.ui.theme.tone
import com.folddesk.app.ui.todayEpoch
import com.folddesk.app.ui.todayStats
import com.folddesk.app.ui.todayTitle
import com.folddesk.app.ui.weekdayShort
import java.time.DayOfWeek
import java.time.LocalDate

/** Иконка в цветной плитке — как в Настройках iOS. */
@Composable
fun IconTile(icon: ImageVector, hue: Float) {
    val t = tone(hue)
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(t.grad),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(18.dp), tint = Color.White) }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    Text(
        "Готово",
        style = Type.bodyStrong,
        color = tone(Hues.ACCENT).ink,
        modifier = Modifier
            .clip(Pill)
            .pressable(scale = 0.92f, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun SmallIconButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    val p = palette()
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(p.fill)
            .pressable(scale = 0.88f, haptic = true, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(19.dp), tint = tint) }
}

// ── Задача ──────────────────────────────────────────────────────────────────

@Composable
fun TaskDetail(vm: DeskViewModel, data: DeskData, task: Task, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val p = palette()
    val subtasks = data.subtasksByTask[task.id].orEmpty()
    val project = task.projectId?.let { data.projectById[it] }
    val hue = data.hueOf(task)
    val done = task.status == TaskStatus.DONE
    var projectMenu by remember { mutableStateOf(false) }
    var dueOpen by rememberSaveable(task.id) { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    Modifier
                        .clip(Pill)
                        .background(p.fill)
                        .pressable(scale = 0.95f) { projectMenu = true }
                        .padding(start = 5.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (project != null) Glyph(project.emoji.ifEmpty { "•" }, projectHue(project.colorIndex), 26.dp)
                    else Box(Modifier.size(26.dp).clip(CircleShape).background(p.fill2))
                    Spacer(Modifier.width(8.dp))
                    Text(project?.name ?: "Без проекта", style = Type.subhead.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 1)
                    Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp), tint = p.muted)
                }
                DropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    DropdownMenuItem(text = { Text("Без проекта") }, onClick = {
                        vm.setProject(task.id, null)
                        projectMenu = false
                    })
                    data.projects.forEach { pr ->
                        DropdownMenuItem(
                            text = { Text(pr.name) },
                            leadingIcon = { Glyph(pr.emoji, projectHue(pr.colorIndex), 26.dp) },
                            onClick = {
                                vm.setProject(task.id, pr.id)
                                projectMenu = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            DoneButton(onClose)
        }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.Top) {
            LiquidCheck(done, { vm.toggleDone(task) }, hue, Modifier.padding(top = 1.dp), size = 34.dp)
            Spacer(Modifier.width(14.dp))
            PlainField(
                value = vm.taskTitle(task),
                onChange = { vm.editTaskTitle(task.id, it) },
                placeholder = "Название",
                style = Type.title2.copy(color = if (done) p.muted else p.ink),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))
        Segmented(
            options = TaskStatus.entries.map { it.label },
            selected = task.status.ordinal,
            onSelect = { vm.setStatus(task.id, TaskStatus.entries[it]) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(18.dp))
        GroupedRows {
            GroupRow(
                title = "Срок",
                leading = { IconTile(Icons.Outlined.Event, Hues.FIRE) },
                trailing = {
                    Text(
                        task.dueEpochDay?.let { dueLabel(it) } ?: "Нет",
                        style = Type.callout,
                        color = if (task.dueEpochDay != null) tone(Hues.FIRE).ink else p.muted,
                    )
                    Icon(Icons.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = p.faint)
                },
                onClick = { dueOpen = !dueOpen },
            )
            AnimatedVisibility(dueOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DuePicker(task.dueEpochDay) {
                    vm.setDue(task.id, it)
                    dueOpen = false
                }
            }
            GroupRow(
                title = "Важно",
                leading = { IconTile(Icons.Filled.Flag, Hues.WARN) },
                trailing = { IosSwitch(task.important, { vm.toggleImportant(task) }) },
                divider = !done,
            )
            if (!done) {
                GroupRow(
                    title = "Фокус-сессия",
                    leading = { IconTile(Icons.Outlined.Timer, Hues.ACCENT) },
                    trailing = { Icon(Icons.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = p.faint) },
                    onClick = { vm.startFocusOn(task.id) },
                    divider = false,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        val doneCount = subtasks.count { it.done }
        SectionHeader("Подзадачи", count = subtasks.size) {
            if (subtasks.isNotEmpty()) Text("$doneCount из ${subtasks.size}", style = Type.caption, color = p.muted)
        }
        if (subtasks.isNotEmpty()) {
            val g = tone(Hues.GROW)
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).height(5.dp).clip(Pill).background(g.tint)) {
                Box(Modifier.fillMaxWidth(doneCount / subtasks.size.toFloat()).height(5.dp).clip(Pill).background(g.grad))
            }
            Spacer(Modifier.height(6.dp))
        }
        GroupedRows {
            subtasks.forEach { s ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidCheck(s.done, { vm.toggleSubtask(s) }, hue, size = 24.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(s.title, style = Type.callout, color = if (s.done) p.muted else p.ink, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).pressable(scale = 0.85f) { vm.deleteSubtask(s.id) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "Удалить", Modifier.size(16.dp), tint = p.faint) }
                }
                Box(Modifier.padding(start = 48.dp).fillMaxWidth().height(0.5.dp).background(p.line2))
            }
            var sub by rememberSaveable(task.id) { mutableStateOf("") }
            Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("+", style = Type.title2, color = tone(Hues.ACCENT).ink)
                Spacer(Modifier.width(14.dp))
                PlainField(
                    value = sub,
                    onChange = { sub = it },
                    placeholder = "Добавить подзадачу",
                    style = Type.callout,
                    singleLine = true,
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (sub.isNotBlank()) vm.addSubtask(task.id, sub.trim())
                        sub = ""
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionHeader("Заметки")
        Box(Modifier.fillMaxWidth().flat(RoundedCornerShape(20.dp), p).padding(16.dp)) {
            PlainField(
                value = vm.taskNotes(task),
                onChange = { vm.editTaskNotes(task.id, it) },
                placeholder = "Детали, ссылки, мысли…",
                style = Type.callout.copy(lineHeight = 22.sp),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Создано ${formatDateTime(task.createdAt)}",
            style = Type.footnote,
            color = p.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        PillButton(
            "Удалить задачу",
            { vm.deleteTask(task.id) },
            Modifier.align(Alignment.CenterHorizontally),
            style = PillStyle.Soft,
            hue = Hues.WARN,
            size = PillSize.Sm,
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** Выбор срока без календарного диалога: быстрые варианты + лента дней. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuePicker(current: Long?, onPick: (Long?) -> Unit) {
    val p = palette()
    val accent = tone(Hues.ACCENT)
    val today = todayEpoch()
    val todayDate = LocalDate.ofEpochDay(today)
    val saturday = todayDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).toEpochDay()
    val quick = listOf("Сегодня" to today, "Завтра" to today + 1, "Выходные" to saturday, "Через неделю" to today + 7)
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quick.forEach { (label, day) ->
                val on = current == day
                Text(
                    label,
                    style = Type.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = if (on) accent.onT else p.ink,
                    modifier = Modifier
                        .clip(Pill)
                        .background(if (on) accent.t else p.fill2)
                        .pressable(scale = 0.93f, haptic = true) { onPick(day) }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                )
            }
            if (current != null) {
                Text(
                    "Без срока",
                    style = Type.subhead.copy(fontWeight = FontWeight.SemiBold),
                    color = tone(Hues.WARN).ink,
                    modifier = Modifier
                        .clip(Pill)
                        .background(tone(Hues.WARN).tint)
                        .pressable(scale = 0.93f, haptic = true) { onPick(null) }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(end = 8.dp)) {
            items((0L until 42L).map { today + it }) { day ->
                val date = LocalDate.ofEpochDay(day)
                val on = current == day
                Column(
                    Modifier
                        .width(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) accent.t else Color.Transparent)
                        .pressable(scale = 0.9f, haptic = true) { onPick(day) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(weekdayShort(date), style = Type.caption, color = if (on) accent.onT else p.muted)
                    Text(
                        "${date.dayOfMonth}",
                        style = Type.headline,
                        color = when {
                            on -> accent.onT
                            date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY -> tone(Hues.WARN).ink
                            else -> p.ink
                        },
                    )
                }
            }
        }
    }
}

// ── Заметка ─────────────────────────────────────────────────────────────────

@Composable
fun NoteDetail(vm: DeskViewModel, note: Note, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val p = palette()
    val sky = tone(Hues.SKY)
    val focus = remember { FocusRequester() }
    LaunchedEffect(note.id) {
        if (vm.noteText(note).isEmpty()) focus.requestFocus()
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Заметка")
                Text(relativeTime(note.updatedAt), style = Type.footnote, color = p.muted)
            }
            SmallIconButton(
                if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                "Закрепить",
                if (note.pinned) sky.ink else p.ink2,
            ) { vm.togglePin(note) }
            Spacer(Modifier.width(8.dp))
            SmallIconButton(Icons.Outlined.Delete, "Удалить", tone(Hues.WARN).ink) { vm.deleteNote(note.id) }
            Spacer(Modifier.width(4.dp))
            DoneButton(onClose)
        }
        Spacer(Modifier.height(16.dp))
        PlainField(
            value = vm.noteText(note),
            onChange = { vm.editNote(note.id, it) },
            placeholder = "Начните писать…",
            style = Type.body.copy(fontSize = 18.sp, lineHeight = 27.sp),
            visualTransformation = FirstLineAsTitle,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focus),
        )
        Spacer(Modifier.height(12.dp))
        PillButton(
            "Сделать задачей",
            { vm.noteToTask(note.id) },
            Modifier.fillMaxWidth(),
            icon = Icons.Outlined.TaskAlt,
            style = PillStyle.Soft,
            size = PillSize.Lg,
        )
    }
}

// ── Черновик с внешнего экрана ──────────────────────────────────────────────

@Composable
fun DraftDetail(vm: DeskViewModel, modifier: Modifier = Modifier) {
    val p = palette()
    val accent = tone(Hues.ACCENT)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.AutoAwesome, Hues.AIM)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Черновик", style = Type.headline, color = p.ink)
                Text("Продолжайте с того же места", style = Type.footnote, color = p.muted)
            }
            DoneButton { vm.select(Selection.None) }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .flat(RoundedCornerShape(24.dp), p)
                .padding(18.dp),
        ) {
            PlainField(
                value = vm.draft,
                onChange = vm::updateDraft,
                placeholder = "Что на уме?",
                style = Type.body.copy(fontSize = 18.sp, lineHeight = 27.sp),
                visualTransformation = FirstLineAsTitle,
                modifier = Modifier.fillMaxSize().focusRequester(focus),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("В заметки", { vm.draftToNote(open = true) }, Modifier.weight(1f), style = PillStyle.Primary, size = PillSize.Lg, enabled = vm.draft.isNotBlank(), haptic = true)
            PillButton("В задачи", { vm.draftToTask(open = true) }, Modifier.weight(1f), style = PillStyle.Plain, size = PillSize.Lg, enabled = vm.draft.isNotBlank(), haptic = true)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Очистить",
            style = Type.subhead,
            color = accent.ink,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(Pill)
                .pressable {
                    vm.updateDraft("")
                    vm.select(Selection.None)
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

// ── Обзор дня (правая панель, когда ничего не выбрано) ───────────────────────

@Composable
fun DayOrb(vm: DeskViewModel, data: DeskData, size: androidx.compose.ui.unit.Dp) {
    val p = palette()
    val (done, total) = data.todayStats()
    val hues = data.tasks.filter { it.status == TaskStatus.DONE && isToday(it.completedAt) }.map { data.hueOf(it) }.distinct().take(4)
    Orb(if (total == 0) 0f else done / total.toFloat(), hues, size) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done", style = Type.num.copy(fontSize = (size.value * 0.36f).sp), color = p.ink)
            Text("из $total", style = Type.caption, color = p.ink2)
        }
    }
}

@Composable
fun TodayOverview(vm: DeskViewModel, data: DeskData, modifier: Modifier = Modifier) {
    val p = palette()
    val today = todayEpoch()
    var day by remember { mutableLongStateOf(today) }
    val week = remember(today) {
        val monday = LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY)
        (0L..6L).map { monday.plusDays(it) }
    }
    val (done, total) = data.todayStats()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Eyebrow(todayTitle())
        Text(greeting(), style = Type.title, color = p.ink)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayOrb(vm, data, 108.dp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Свет дня", color = tone(Hues.ACCENT).ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        total == 0 -> "На сегодня сроков нет — выберите главное"
                        done == total -> "Всё на сегодня сделано. Красота"
                        done == 0 -> "Сегодня $total ${plural(total)}. Начните с самого лёгкого"
                        else -> "Осталось ${total - done} — импульс уже есть"
                    },
                    style = Type.headline,
                    color = p.ink,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                val epoch = date.toEpochDay()
                val dayTasks = data.tasks.filter { it.dueEpochDay == epoch }
                val prog = if (dayTasks.isEmpty()) 0f else dayTasks.count { it.status == TaskStatus.DONE } / dayTasks.size.toFloat()
                val selected = epoch == day
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) p.fill2 else Color.Transparent)
                        .pressable(scale = 0.9f, haptic = true) { day = epoch }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.then(
                            if (epoch == today) Modifier.border(1.5.dp, p.ink2, CircleShape).padding(2.5.dp) else Modifier.padding(4.dp),
                        ),
                    ) {
                        Orb(
                            if (dayTasks.isEmpty()) 0f else prog.coerceAtLeast(0.12f),
                            dayTasks.map { data.hueOf(it) }.distinct().take(3),
                            24.dp,
                            springy = false,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(weekdayShort(date), style = Type.caption, color = if (selected || epoch == today) p.ink else p.faint)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FocusMini(vm, data)

        Spacer(Modifier.height(18.dp))
        val list = data.tasks.filter {
            it.dueEpochDay == day || (day == today && it.dueEpochDay != null && it.dueEpochDay < today && it.status != TaskStatus.DONE)
        }
        SectionHeader(if (day == today) "На сегодня" else dueLabel(day), count = list.size)
        if (list.isEmpty()) {
            EmptyState("Свободный день", subtitle = "Задач со сроком нет")
        } else {
            Column(Modifier.flat(RoundedCornerShape(20.dp), p).padding(4.dp)) {
                list.forEach { t -> TaskRow(vm, data, t, onClick = { vm.select(Selection.OfTask(t.id)) }) }
            }
        }

        Spacer(Modifier.height(18.dp))
        val sky = tone(Hues.SKY)
        Text(
            "Удерживайте заметку и перетащите её на доску — она станет задачей. " +
                "Поставьте телефон «ноутбуком» — включится фокус.",
            style = Type.footnote.copy(fontWeight = FontWeight.Normal, lineHeight = 19.sp),
            color = sky.ink,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(sky.tint)
                .padding(14.dp),
        )
    }
}

private fun plural(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> "дело"
        m10 in 2..4 && m100 !in 12..14 -> "дела"
        else -> "дел"
    }
}

@Composable
fun FocusMini(vm: DeskViewModel, data: DeskData, modifier: Modifier = Modifier) {
    val p = palette()
    val work = vm.focusPhase == FocusPhase.Work
    val hue = if (work) Hues.ACCENT else Hues.GROW
    val task = vm.focusTaskId?.let { data.task(it) }
    Row(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.98f) { vm.deskFocus = true }
            .flat(RoundedCornerShape(22.dp), p)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Orb(1f - vm.focusRemainingMs / vm.phaseDurationMs().toFloat(), listOf(hue), 46.dp, springy = false)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow(if (work) "Фокус" else "Перерыв", color = tone(hue).ink)
            Text(formatTimer(vm.focusRemainingMs), style = Type.num.copy(fontSize = 24.sp), color = p.ink)
            if (task != null) {
                Text(vm.taskTitle(task), style = Type.footnote, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        PillButton(
            null,
            vm::toggleFocusTimer,
            icon = if (vm.focusRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            style = PillStyle.Primary,
            hue = hue,
            haptic = true,
        )
    }
}

// ── Новый проект ────────────────────────────────────────────────────────────

private val ProjectEmojis = listOf("💼", "🏡", "📚", "🚀", "🎯", "💡", "🛒", "✈️", "💪", "🎨", "💰", "❤️")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProjectContent(onCreate: (name: String, colorIndex: Int, emoji: String) -> Unit) {
    val p = palette()
    var name by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(0) }
    var emoji by remember { mutableStateOf(ProjectEmojis[3]) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph(emoji, projectHue(color), 52.dp)
            Spacer(Modifier.width(14.dp))
            Text("Новый проект", style = Type.title, color = p.ink)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(52.dp).flat(Pill, p).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
            PlainField(name, { name = it }, "Название", Type.body, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true)
        }
        Spacer(Modifier.height(18.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProjectEmojis.forEach { e ->
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (e == emoji) tone(projectHue(color)).tint else p.fill)
                        .then(if (e == emoji) Modifier.border(1.5.dp, tone(projectHue(color)).line, RoundedCornerShape(14.dp)) else Modifier)
                        .pressable(scale = 0.88f, haptic = true) { emoji = e },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 22.sp) }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (0 until projectPaletteSize).forEach { i ->
                val t = tone(projectHue(i))
                Box(
                    Modifier
                        .size(34.dp)
                        .then(if (i == color) Modifier.border(2.5.dp, p.ink, CircleShape).padding(4.dp) else Modifier)
                        .clip(CircleShape)
                        .background(t.grad)
                        .pressable(scale = 0.85f, haptic = true) { color = i },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        PillButton(
            "Создать",
            { onCreate(name.trim(), color, emoji) },
            Modifier.fillMaxWidth(),
            style = PillStyle.Primary,
            size = PillSize.Lg,
            hue = projectHue(color),
            enabled = name.isNotBlank(),
            haptic = true,
        )
    }
}
