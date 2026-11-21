@file:OptIn(ExperimentalMaterial3Api::class)

package com.folddesk.app.ui.desk

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.folddesk.app.data.Project
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.data.noteTitle
import com.folddesk.app.ui.BoardMode
import com.folddesk.app.ui.DeskData
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.Scope
import com.folddesk.app.ui.Selection
import com.folddesk.app.ui.components.BoardCard
import com.folddesk.app.ui.components.EmptyState
import com.folddesk.app.ui.components.Glyph
import com.folddesk.app.ui.components.Grabber
import com.folddesk.app.ui.components.NoteTile
import com.folddesk.app.ui.components.Pill
import com.folddesk.app.ui.components.PillButton
import com.folddesk.app.ui.components.PillStyle
import com.folddesk.app.ui.components.PlainField
import com.folddesk.app.ui.components.QuickAdd
import com.folddesk.app.ui.components.SectionHeader
import com.folddesk.app.ui.components.Eyebrow
import com.folddesk.app.ui.components.TaskRow
import com.folddesk.app.ui.components.flat
import com.folddesk.app.ui.components.glass
import com.folddesk.app.ui.detail.DayOrb
import com.folddesk.app.ui.todayStats
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewColumn
import com.folddesk.app.ui.components.pressable
import com.folddesk.app.ui.detail.DraftDetail
import com.folddesk.app.ui.detail.FocusMini
import com.folddesk.app.ui.detail.NewProjectContent
import com.folddesk.app.ui.detail.NoteDetail
import com.folddesk.app.ui.detail.TaskDetail
import com.folddesk.app.ui.detail.TodayOverview
import com.folddesk.app.ui.drag.DragPayload
import com.folddesk.app.ui.drag.DropTarget
import com.folddesk.app.ui.drag.LocalDragDrop
import com.folddesk.app.ui.drag.dragSource
import com.folddesk.app.ui.drag.dropTarget
import com.folddesk.app.ui.focus.DeskFocusPanel
import com.folddesk.app.ui.formatTimer
import com.folddesk.app.ui.label
import com.folddesk.app.ui.scopeTitle
import com.folddesk.app.ui.tasksIn
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.oklch
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.projectHue
import com.folddesk.app.ui.theme.statusHue
import com.folddesk.app.ui.theme.tone

private val PaneShape = RoundedCornerShape(30.dp)

/**
 * Внутренний экран 4:3 — рабочий стол из стеклянных панелей над живыми обоями:
 * боковая панель | доска | детали. Ширину панелей можно тянуть пальцем.
 */
@Composable
fun DeskScreen(vm: DeskViewModel, data: DeskData) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
    ) {
        val total = maxWidth.value
        // На узком экране открытая задача сама убирает боковую панель (как на iPad), закрыл — вернулась
        val detailOpen = vm.selection != Selection.None
        LaunchedEffect(detailOpen, total < 1000f) {
            if (total < 1000f) {
                if (detailOpen && vm.leftPaneVisible) {
                    vm.leftPaneVisible = false
                    vm.sidebarAutoHidden = true
                } else if (!detailOpen && vm.sidebarAutoHidden) {
                    vm.leftPaneVisible = true
                    vm.sidebarAutoHidden = false
                }
            }
        }
        val showLeft = vm.leftPaneVisible && total >= 640f
        val leftW = vm.leftPaneWidth
        val usedLeft = if (showLeft) leftW + 16f else 0f
        val rightW = vm.rightPaneWidth.coerceAtMost((total - usedLeft - 320f).coerceAtLeast(280f))
        // Правая панель — как инспектор на iPad: выезжает, когда что-то открыто,
        // а пустой обзор дня показываем, только если доске хватает места и без неё.
        val roomy = total - usedLeft - rightW - 16f >= 3 * 200f + 24f
        val showRight = vm.selection != Selection.None || roomy

        Row(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showLeft,
                enter = expandHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f), expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f), shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
                Row {
                    Sidebar(vm, data, Modifier.width(leftW.dp).fillMaxHeight())
                    PaneHandle(onDragDp = vm::resizeLeft)
                }
            }
            AnimatedContent(
                targetState = vm.deskFocus,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(260)) + scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f), initialScale = 0.96f)) togetherWith fadeOut(tween(140))
                },
                label = "deskFocus",
            ) { focus ->
                if (focus) {
                    DeskFocusPanel(vm, data, Modifier.fillMaxSize())
                } else {
                    Row(Modifier.fillMaxSize()) {
                        CenterColumn(vm, data, Modifier.weight(1f))
                        AnimatedVisibility(
                            visible = showRight,
                            enter = expandHorizontally(spring(dampingRatio = 0.86f, stiffness = 360f), expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f), shrinkTowards = Alignment.Start) + fadeOut(),
                        ) {
                            Row {
                                PaneHandle(onDragDp = { vm.resizeRight(-it) })
                                Box(Modifier.width(rightW.dp).fillMaxHeight().glass(PaneShape, palette())) { DetailPane(vm, data) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Ручка между панелями: тянешь — панель меняет ширину. */
@Composable
private fun PaneHandle(onDragDp: (Float) -> Unit) {
    val p = palette()
    val density = LocalDensity.current
    val currentOnDrag by rememberUpdatedState(onDragDp)
    var active by remember { mutableStateOf(false) }
    val h by animateDpAsState(if (active) 80.dp else 44.dp, spring(dampingRatio = 0.6f, stiffness = 400f), label = "handleH")
    val c by animateColorAsState(if (active) tone(Hues.ACCENT).t else p.fill3, label = "handleC")
    Box(
        Modifier
            .fillMaxHeight()
            .width(16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { active = true },
                    onDragEnd = { active = false },
                    onDragCancel = { active = false },
                ) { change, amount ->
                    change.consume()
                    currentOnDrag(amount / density.density)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(5.dp).height(h).clip(Pill).background(c))
    }
}

@Composable
private fun FloatButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val p = palette()
    Box(
        Modifier
            .size(44.dp)
            .pressable(scale = 0.9f, onClick = onClick)
            .glass(CircleShape, p),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, Modifier.size(21.dp), tint = p.ink) }
}

// ── Боковая панель ──────────────────────────────────────────────────────────

@Composable
private fun BrandMark() {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    Brush.sweepGradient(
                        listOf(
                            oklch(0.8f, 0.17f, 255f), oklch(0.8f, 0.18f, 310f), oklch(0.83f, 0.17f, 40f),
                            oklch(0.85f, 0.18f, 150f), oklch(0.8f, 0.17f, 255f),
                        ),
                    ),
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                        center = Offset(size.width * 0.32f, size.height * 0.24f),
                        radius = size.width * 0.45f,
                    ),
                )
            },
    )
}

@Composable
private fun Sidebar(vm: DeskViewModel, data: DeskData, modifier: Modifier) {
    val p = palette()
    val drag = LocalDragDrop.current
    var newProject by remember { mutableStateOf(false) }
    val todayOpen = data.tasksIn(Scope.Today, "").count { it.status != TaskStatus.DONE }
    val allOpen = data.tasks.count { it.status != TaskStatus.DONE }

    Box(modifier.glass(RoundedCornerShape(34.dp), p)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                Row(Modifier.padding(start = 8.dp, end = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandMark()
                    Spacer(Modifier.width(11.dp))
                    Text("FoldDesk", style = Type.title2.copy(fontWeight = FontWeight.ExtraBold), color = p.ink)
                }
            }
            item { SearchPill(vm.search, { vm.search = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
            item { DayCard(vm, data) }
            item {
                NavItem("Сегодня", count = todayOpen, selected = vm.scope == Scope.Today, onClick = { vm.scope = Scope.Today }) {
                    Icon(Icons.Outlined.WbSunny, null, Modifier.size(21.dp), tint = if (vm.scope == Scope.Today) tone(Hues.FIRE).ink else p.muted)
                }
            }
            item {
                NavItem("Все задачи", count = allOpen, selected = vm.scope == Scope.All, onClick = { vm.scope = Scope.All }) {
                    Icon(Icons.Outlined.Inbox, null, Modifier.size(21.dp), tint = if (vm.scope == Scope.All) tone(Hues.ACCENT).ink else p.muted)
                }
            }
            item {
                SectionHeader("Проекты", Modifier.padding(start = 8.dp, top = 14.dp)) {
                    MiniAdd("Новый проект") { newProject = true }
                }
            }
            items(data.projects, key = { "p${it.id}" }) { pr ->
                ProjectItem(vm, data, pr, hot = drag.hovered == DropTarget.ToProject(pr.id))
            }
            item {
                SectionHeader("Заметки", Modifier.padding(start = 8.dp, top = 14.dp)) {
                    MiniAdd("Новая заметка", vm::newNote)
                }
            }
            items(data.notes, key = { "n${it.id}" }) { n ->
                val text = vm.noteText(n)
                NoteTile(
                    vm,
                    n,
                    onClick = { vm.select(Selection.OfNote(n.id)) },
                    selected = vm.selection == Selection.OfNote(n.id),
                    previewLines = 2,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .animateItem()
                        .dragSource(DragPayload.OfNote(n), if (text.isBlank()) "Новая заметка" else noteTitle(text)),
                )
            }
            if (data.notes.isEmpty()) item { EmptyState("Заметок нет") }
            item {
                Spacer(Modifier.height(14.dp))
                FocusMini(vm, data)
            }
        }
    }

    if (newProject) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { newProject = false },
            sheetState = state,
            containerColor = p.glassSolid,
            scrimColor = p.scrim,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { Grabber() },
        ) {
            NewProjectContent { name, color, emoji ->
                vm.addProject(name, color, emoji)
                newProject = false
            }
        }
    }
}

/** «Свет дня» в боковой панели: сфера прогресса и одна понятная фраза. */
@Composable
private fun DayCard(vm: DeskViewModel, data: DeskData) {
    val p = palette()
    val (done, total) = data.todayStats()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .pressable(scale = 0.98f) {
                vm.scope = Scope.Today
                vm.select(Selection.None)
            }
            .flat(RoundedCornerShape(22.dp), p)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayOrb(vm, data, 54.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow("Свет дня", color = tone(Hues.AIM).ink)
            Text(
                when {
                    total == 0 -> "Сроков нет"
                    done == total -> "Всё сделано"
                    else -> "Осталось ${total - done} из $total"
                },
                style = Type.subhead.copy(fontWeight = FontWeight.SemiBold),
                color = p.ink,
            )
        }
    }
}

@Composable
private fun MiniAdd(description: String, onClick: () -> Unit) {
    val p = palette()
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(p.fill)
            .pressable(scale = 0.85f, haptic = true, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Add, description, Modifier.size(17.dp), tint = p.ink2) }
}

@Composable
private fun NavItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hot: Boolean = false,
    hotHue: Float = Hues.ACCENT,
    leading: @Composable () -> Unit,
) {
    val p = palette()
    val hotTone = tone(hotHue)
    val bg by animateColorAsState(
        when {
            hot -> hotTone.tint
            selected -> p.fill2
            else -> Color.Transparent
        },
        label = "navBg",
    )
    val scale by animateFloatAsState(if (hot) 1.05f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "navScale")
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pressable(scale = 0.97f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(
                when {
                    hot -> Modifier.border(1.5.dp, hotTone.line, RoundedCornerShape(16.dp))
                    selected -> Modifier.border(0.5.dp, p.rimMid, RoundedCornerShape(16.dp))
                    else -> Modifier
                },
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = Type.callout.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) p.ink else p.ink2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) Text("$count", style = Type.footnote, color = p.muted)
    }
}

@Composable
private fun ProjectItem(vm: DeskViewModel, data: DeskData, project: Project, hot: Boolean) {
    val count = data.tasks.count { it.projectId == project.id && it.status != TaskStatus.DONE }
    val hue = projectHue(project.colorIndex)
    NavItem(
        label = project.name,
        count = count,
        selected = vm.scope == Scope.InProject(project.id),
        onClick = { vm.scope = Scope.InProject(project.id) },
        hot = hot,
        hotHue = hue,
        modifier = Modifier.dropTarget(DropTarget.ToProject(project.id)),
    ) {
        Glyph(project.emoji.ifEmpty { "•" }, hue, 28.dp)
    }
}

// ── Центр: заголовок, доска, быстрый ввод ───────────────────────────────────

@Composable
private fun CenterColumn(vm: DeskViewModel, data: DeskData, modifier: Modifier) {
    val p = palette()
    val tasks = data.tasksIn(vm.scope, vm.search)
    val all = data.tasksIn(vm.scope, "")
    val open = all.count { it.status != TaskStatus.DONE }
    val scope = vm.scope
    var menu by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxHeight()) {
    val compact = maxWidth < 460.dp
    Column(Modifier.fillMaxHeight()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FloatButton(
                if (vm.leftPaneVisible) Icons.AutoMirrored.Outlined.MenuOpen else Icons.Outlined.Menu,
                "Боковая панель",
            ) {
                vm.leftPaneVisible = !vm.leftPaneVisible
                vm.sidebarAutoHidden = false
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(data.scopeTitle(scope), style = Type.title, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$open открыто · ${all.size - open} готово", style = Type.footnote, color = p.muted, maxLines = 1)
            }
            ModeSwitch(vm)
            Spacer(Modifier.width(10.dp))
            PillButton(
                when {
                    vm.focusRunning -> formatTimer(vm.focusRemainingMs)
                    compact -> null
                    else -> "Фокус"
                },
                { vm.deskFocus = true },
                icon = if (vm.focusRunning) Icons.Filled.Pause else Icons.Outlined.Timer,
                style = if (vm.focusRunning) PillStyle.Primary else PillStyle.Soft,
                haptic = true,
            )
            if (scope is Scope.InProject) {
                Spacer(Modifier.width(8.dp))
                Box {
                    FloatButton(Icons.Outlined.MoreHoriz, "Ещё") { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Удалить проект", color = tone(Hues.WARN).ink) },
                            onClick = {
                                menu = false
                                vm.deleteProject(scope.id)
                            },
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f)) {
            if (tasks.isEmpty() && vm.search.isNotBlank()) {
                Box(Modifier.fillMaxSize().glass(PaneShape, p), contentAlignment = Alignment.Center) {
                    EmptyState("Ничего не найдено", subtitle = "по запросу «${vm.search}»")
                }
            } else {
                AnimatedContent(
                    targetState = vm.boardMode,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(110)) },
                    label = "boardMode",
                ) { mode ->
                    if (mode == BoardMode.Board) BoardView(vm, data, tasks) else ListView(vm, data, tasks)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        QuickAdd(
            placeholder = when (scope) {
                Scope.Today -> "Новая задача на сегодня"
                Scope.All -> "Новая задача"
                is Scope.InProject -> "Новая задача в «${data.projectById[scope.id]?.name ?: "проект"}»"
            },
            onAdd = { vm.addTask(it) },
            glassy = true,
            height = 54.dp,
        )
    }
    }
}

/** Доска / список — две иконки в стеклянной капсуле с перетекающей линзой. */
@Composable
private fun ModeSwitch(vm: DeskViewModel) {
    val p = palette()
    val board = vm.boardMode == BoardMode.Board
    val x by animateDpAsState(if (board) 0.dp else 42.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "modeLens")
    Box(Modifier.size(92.dp, 44.dp).glass(Pill, p).padding(4.dp)) {
        Box(Modifier.padding(start = x).size(42.dp, 36.dp).clip(Pill).background(p.fill3))
        Row {
            listOf(BoardMode.Board to Icons.Outlined.ViewColumn, BoardMode.List to Icons.Outlined.ViewAgenda).forEach { (mode, icon) ->
                Box(
                    Modifier.size(42.dp, 36.dp).clip(Pill).pressable(scale = 0.9f, haptic = vm.boardMode != mode) { vm.boardMode = mode },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        if (mode == BoardMode.Board) "Доска" else "Список",
                        Modifier.size(20.dp),
                        tint = if (vm.boardMode == mode) p.ink else p.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPill(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = palette()
    Row(
        modifier
            .height(42.dp)
            .flat(Pill, p)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(19.dp), tint = p.muted)
        Spacer(Modifier.width(8.dp))
        PlainField(value, onChange, "Поиск", Type.callout, Modifier.weight(1f), singleLine = true)
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(p.fill2).pressable(scale = 0.85f) { onChange("") },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Очистить", Modifier.size(15.dp), tint = p.ink2) }
        }
    }
}

@Composable
private fun BoardView(vm: DeskViewModel, data: DeskData, tasks: List<Task>) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fits = maxWidth >= 168.dp * 3 + 24.dp
        Row(
            Modifier
                .fillMaxSize()
                .then(if (fits) Modifier else Modifier.horizontalScroll(rememberScrollState())),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                BoardColumn(
                    vm,
                    data,
                    status,
                    tasks.filter { it.status == status },
                    if (fits) Modifier.weight(1f) else Modifier.width(236.dp),
                )
            }
        }
    }
}

@Composable
private fun BoardColumn(vm: DeskViewModel, data: DeskData, status: TaskStatus, tasks: List<Task>, modifier: Modifier) {
    val p = palette()
    val drag = LocalDragDrop.current
    val target = DropTarget.Column(status)
    val hot = drag.hovered == target
    val t = tone(statusHue(status))
    val scale by animateFloatAsState(if (hot) 1.015f else 1f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "colScale")
    Column(
        modifier
            .fillMaxHeight()
            .dropTarget(target)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(PaneShape, p, tint = if (hot) t else null)
            .then(if (hot) Modifier.border(2.dp, t.line, PaneShape) else Modifier),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(t.grad))
            Spacer(Modifier.width(9.dp))
            Text(status.label, style = Type.headline, color = p.ink, modifier = Modifier.weight(1f))
            Text(
                "${tasks.size}",
                style = Type.caption,
                color = t.ink,
                modifier = Modifier.clip(Pill).background(t.tint).padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                BoardCard(
                    vm,
                    data,
                    task,
                    selected = vm.selection == Selection.OfTask(task.id),
                    onClick = { vm.select(Selection.OfTask(task.id)) },
                    showProject = vm.scope !is Scope.InProject,
                    modifier = Modifier
                        .animateItem()
                        .dragSource(DragPayload.OfTask(task), vm.taskTitle(task)),
                )
            }
            if (tasks.isEmpty()) {
                item {
                    Text(
                        if (drag.isDragging) "Отпустите здесь" else "Пусто",
                        style = Type.footnote,
                        color = if (drag.isDragging) t.ink else p.faint,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListView(vm: DeskViewModel, data: DeskData, tasks: List<Task>) {
    val p = palette()
    LazyColumn(
        modifier = Modifier.fillMaxSize().glass(PaneShape, p),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TaskStatus.entries.forEach { s ->
            val list = tasks.filter { it.status == s }
            if (list.isNotEmpty()) {
                item(key = "h$s") { SectionHeader(s.label, Modifier.padding(start = 10.dp, top = 6.dp).animateItem(), count = list.size) }
                items(list, key = { it.id }) { task ->
                    TaskRow(
                        vm,
                        data,
                        task,
                        onClick = { vm.select(Selection.OfTask(task.id)) },
                        selected = vm.selection == Selection.OfTask(task.id),
                        modifier = Modifier
                            .animateItem()
                            .dragSource(DragPayload.OfTask(task), vm.taskTitle(task)),
                    )
                }
            }
        }
        if (tasks.isEmpty()) item { EmptyState("Здесь пока пусто", subtitle = "Добавьте задачу внизу") }
    }
}

// ── Правая панель ──────────────────────────────────────────────────────────

@Composable
private fun DetailPane(vm: DeskViewModel, data: DeskData) {
    AnimatedContent(
        targetState = vm.selection,
        transitionSpec = {
            (fadeIn(tween(240)) + slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 380f)) { it / 14 }) togetherWith fadeOut(tween(110))
        },
        label = "detail",
    ) { sel ->
        val close = { vm.select(Selection.None) }
        when (sel) {
            Selection.None -> TodayOverview(vm, data)
            Selection.Draft -> DraftDetail(vm)
            is Selection.OfTask -> {
                val task = data.task(sel.id)
                if (task != null) TaskDetail(vm, data, task, onClose = close) else Box(Modifier.fillMaxSize())
            }
            is Selection.OfNote -> {
                val note = data.note(sel.id)
                if (note != null) NoteDetail(vm, note, onClose = close) else Box(Modifier.fillMaxSize())
            }
        }
    }
}
