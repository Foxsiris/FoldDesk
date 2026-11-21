@file:OptIn(ExperimentalMaterial3Api::class)

package com.folddesk.app.ui.cover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.ui.DeskData
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.FocusPhase
import com.folddesk.app.ui.Scope
import com.folddesk.app.ui.Selection
import com.folddesk.app.ui.components.EmptyState
import com.folddesk.app.ui.components.Eyebrow
import com.folddesk.app.ui.components.GlassCard
import com.folddesk.app.ui.components.GlassTabBar
import com.folddesk.app.ui.components.Grabber
import com.folddesk.app.ui.components.NoteTile
import com.folddesk.app.ui.components.PillButton
import com.folddesk.app.ui.components.PillSize
import com.folddesk.app.ui.components.PillStyle
import com.folddesk.app.ui.components.PlainField
import com.folddesk.app.ui.components.QuickAdd
import com.folddesk.app.ui.components.SectionHeader
import com.folddesk.app.ui.components.TabItem
import com.folddesk.app.ui.components.TaskRow
import com.folddesk.app.ui.components.pressable
import com.folddesk.app.ui.detail.DayOrb
import com.folddesk.app.ui.detail.NoteDetail
import com.folddesk.app.ui.detail.TaskDetail
import com.folddesk.app.ui.formatTimer
import com.folddesk.app.ui.greeting
import com.folddesk.app.ui.isToday
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.tone
import com.folddesk.app.ui.todayEpoch
import com.folddesk.app.ui.todayStats
import com.folddesk.app.ui.todayTitle
import kotlinx.coroutines.launch

private val Tabs = listOf(
    TabItem("Сегодня", Icons.Outlined.Home, Icons.Filled.Home),
    TabItem("Задачи", Icons.Outlined.TaskAlt, Icons.Filled.TaskAlt),
    TabItem("Заметки", Icons.Outlined.Lightbulb, Icons.Filled.Lightbulb),
)

/**
 * Внешний экран — «карман»: одно главное действие (записать мысль),
 * один взгляд на день, крупные цели касания для одной руки.
 */
@Composable
fun CoverScreen(vm: DeskViewModel, data: DeskData, onVoice: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(vm.captureRequest) {
        if (vm.captureRequest > 0) tab = 0
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pad = PaddingValues(start = 16.dp, end = 16.dp, top = top + 14.dp, bottom = bottom + 104.dp)

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 30 }) togetherWith fadeOut(tween(110))
            },
            label = "coverTab",
        ) { t ->
            when (t) {
                0 -> CoverToday(vm, data, onVoice, pad, onOpenTasks = { tab = 1 })
                1 -> CoverTasks(vm, data, pad)
                else -> CoverNotes(vm, data, pad)
            }
        }
        GlassTabBar(
            Tabs,
            tab,
            { tab = it },
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        )
    }
    CoverSheet(vm, data)
}

@Composable
private fun LargeHeader(eyebrow: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    val p = palette()
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(2.dp))
            Text(title, style = Type.largeTitle, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

// ── Сегодня ─────────────────────────────────────────────────────────────────

@Composable
private fun CoverToday(vm: DeskViewModel, data: DeskData, onVoice: () -> Unit, pad: PaddingValues, onOpenTasks: () -> Unit) {
    val today = todayEpoch()
    val list = data.tasks
        .filter { it.status != TaskStatus.DONE && ((it.dueEpochDay != null && it.dueEpochDay <= today) || it.status == TaskStatus.DOING) }
        .sortedBy { it.dueEpochDay ?: Long.MAX_VALUE }
    val doneToday = data.tasks.filter { it.status == TaskStatus.DONE && isToday(it.completedAt) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LargeHeader(todayTitle(), greeting()) }
        item { HeroCard(vm, data) }
        item { CaptureCard(vm, onVoice) }
        item {
            GlassCard(padding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
                SectionHeader("На сегодня", Modifier.padding(horizontal = 8.dp), count = list.size) {
                    Text(
                        "Все",
                        style = Type.subhead.copy(fontWeight = FontWeight.SemiBold),
                        color = tone(Hues.ACCENT).ink,
                        modifier = Modifier.padding(end = 6.dp).pressable(scale = 0.92f, onClick = onOpenTasks),
                    )
                }
                if (list.isEmpty() && doneToday.isEmpty()) {
                    EmptyState("Свободный день", subtitle = "Добавьте главное через быструю запись")
                }
                (list + doneToday.take(3)).forEach { t ->
                    TaskRow(vm, data, t, onClick = { vm.select(Selection.OfTask(t.id)) }, modifier = Modifier.animateContentSize())
                }
            }
        }
        val recent = data.notes.take(8)
        if (recent.isNotEmpty()) {
            item { SectionHeader("Недавние заметки", Modifier.padding(horizontal = 4.dp)) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recent, key = { it.id }) { n ->
                        NoteTile(vm, n, onClick = { vm.select(Selection.OfNote(n.id)) }, modifier = Modifier.width(172.dp).animateItem(), previewLines = 3, asGlass = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(vm: DeskViewModel, data: DeskData) {
    val p = palette()
    val (done, total) = data.todayStats()
    val work = vm.focusPhase == FocusPhase.Work
    val focusHue = if (work) Hues.ACCENT else Hues.GROW
    GlassCard(tint = tone(Hues.AIM), padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DayOrb(vm, data, 104.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Свет дня", color = tone(Hues.AIM).ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        total == 0 -> "Сроков на сегодня нет"
                        done == total -> "Всё сделано. Красота"
                        else -> "Осталось ${total - done} из $total"
                    },
                    style = Type.headline,
                    color = p.ink,
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    if (vm.focusRunning) formatTimer(vm.focusRemainingMs) else "Фокус ${vm.focusMinutes} мин",
                    vm::toggleFocusTimer,
                    icon = if (vm.focusRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    style = if (vm.focusRunning) PillStyle.Primary else PillStyle.Soft,
                    hue = focusHue,
                    size = PillSize.Sm,
                    haptic = true,
                )
            }
        }
    }
}

@Composable
private fun CaptureCard(vm: DeskViewModel, onVoice: () -> Unit) {
    val p = palette()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(vm.captureRequest) {
        if (vm.captureRequest > 0) focus.requestFocus()
    }
    GlassCard(tint = tone(Hues.ACCENT), padding = PaddingValues(18.dp), modifier = Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = tone(Hues.ACCENT).ink)
            Spacer(Modifier.width(6.dp))
            Eyebrow("Быстрая запись", color = tone(Hues.ACCENT).ink)
        }
        Spacer(Modifier.height(10.dp))
        PlainField(
            value = vm.draft,
            onChange = vm::updateDraft,
            placeholder = "Что на уме?",
            style = Type.title2.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .focusRequester(focus),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton(null, onVoice, icon = Icons.Filled.Mic, style = PillStyle.Plain, haptic = true)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(vm.draft.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillButton("В задачи", {
                        vm.draftToTask()
                        keyboard?.hide()
                    }, style = PillStyle.Plain, size = PillSize.Sm, haptic = true)
                    Spacer(Modifier.width(8.dp))
                    PillButton("Сохранить", {
                        vm.draftToNote()
                        keyboard?.hide()
                    }, style = PillStyle.Primary, size = PillSize.Sm, haptic = true)
                }
            }
        }
        AnimatedVisibility(vm.draft.isNotBlank()) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.OpenInFull, null, Modifier.size(13.dp), tint = p.muted)
                Spacer(Modifier.width(6.dp))
                Text("Раскройте телефон — черновик откроется в редакторе", style = Type.footnote, color = p.muted)
            }
        }
    }
}

// ── Задачи ──────────────────────────────────────────────────────────────────

@Composable
private fun CoverTasks(vm: DeskViewModel, data: DeskData, pad: PaddingValues) {
    val today = todayEpoch()
    val open = data.tasks.filter { it.status != TaskStatus.DONE }
    val dueNow = open.filter { it.dueEpochDay != null && it.dueEpochDay <= today }
    val doing = open.filter { it.status == TaskStatus.DOING && it !in dueNow }
    val later = open.filter { it.status == TaskStatus.TODO && it !in dueNow }
    val doneToday = data.tasks.filter { it.status == TaskStatus.DONE && isToday(it.completedAt) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { LargeHeader("${open.size} открыто", "Задачи") }
        item { QuickAdd("Новая задача", onAdd = { vm.addTask(it, scopeOverride = Scope.All) }, glassy = true) }
        group("Сегодня и раньше", dueNow, vm, data)
        group("В работе", doing, vm, data)
        group("Позже", later, vm, data)
        group("Готово сегодня", doneToday, vm, data)
        if (open.isEmpty()) item { EmptyState("Задач нет", subtitle = "Наслаждайтесь 🌿") }
    }
}

private fun LazyListScope.group(title: String, list: List<Task>, vm: DeskViewModel, data: DeskData) {
    if (list.isEmpty()) return
    item(key = "g_$title") {
        GlassCard(Modifier.animateItem(), padding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            SectionHeader(title, Modifier.padding(horizontal = 8.dp), count = list.size)
            list.forEach { t ->
                TaskRow(vm, data, t, onClick = { vm.select(Selection.OfTask(t.id)) })
            }
        }
    }
}

// ── Заметки ─────────────────────────────────────────────────────────────────

@Composable
private fun CoverNotes(vm: DeskViewModel, data: DeskData, pad: PaddingValues) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = pad,
        verticalItemSpacing = 10.dp,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Box(Modifier.padding(bottom = 6.dp)) {
                LargeHeader("${data.notes.size} всего", "Заметки") {
                    PillButton(null, vm::newNote, icon = Icons.Rounded.Add, style = PillStyle.Primary, haptic = true)
                }
            }
        }
        items(data.notes, key = { it.id }) { n ->
            NoteTile(vm, n, onClick = { vm.select(Selection.OfNote(n.id)) }, modifier = Modifier.animateItem(), asGlass = true)
        }
        if (data.notes.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { EmptyState("Пока пусто", subtitle = "Запишите первую мысль") }
        }
    }
}

// ── Шторка деталей ──────────────────────────────────────────────────────────

/** На внешнем экране детали открываются шторкой; при раскрытии — та же задача в правой панели. */
@Composable
private fun CoverSheet(vm: DeskViewModel, data: DeskData) {
    val p = palette()
    val sel = vm.selection
    val task = (sel as? Selection.OfTask)?.let { data.task(it.id) }
    val note = (sel as? Selection.OfNote)?.let { data.note(it.id) }
    if (task == null && note == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { vm.select(Selection.None) }
    }
    ModalBottomSheet(
        onDismissRequest = { vm.select(Selection.None) },
        sheetState = sheetState,
        containerColor = p.glassSolid,
        scrimColor = p.scrim,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { Grabber() },
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
            when {
                task != null -> TaskDetail(vm, data, task, onClose = close)
                note != null -> NoteDetail(vm, note, onClose = close)
            }
        }
    }
}
