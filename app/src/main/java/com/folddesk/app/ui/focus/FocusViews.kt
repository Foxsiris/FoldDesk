package com.folddesk.app.ui.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.ui.DeskData
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.FocusPhase
import com.folddesk.app.ui.components.EmptyState
import com.folddesk.app.ui.components.Eyebrow
import com.folddesk.app.ui.components.LiquidCheck
import com.folddesk.app.ui.components.Orb
import com.folddesk.app.ui.components.PillButton
import com.folddesk.app.ui.components.PillStyle
import com.folddesk.app.ui.components.SectionHeader
import com.folddesk.app.ui.components.Segmented
import com.folddesk.app.ui.components.TaskRow
import com.folddesk.app.ui.components.glass
import com.folddesk.app.ui.components.hueOf
import com.folddesk.app.ui.components.pressable
import com.folddesk.app.ui.detail.TaskDetail
import com.folddesk.app.ui.formatTimer
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.tone

private val Presets = listOf(15, 25, 50)

/** Таймер — сфера, которая наполняется светом по мере сессии. */
@Composable
fun FocusOrb(vm: DeskViewModel, data: DeskData, size: Dp) {
    val p = palette()
    val work = vm.focusPhase == FocusPhase.Work
    val hue = if (work) Hues.ACCENT else Hues.GROW
    val task = vm.focusTaskId?.let { data.task(it) }
    val hues = if (task != null && work) listOf(hue, data.hueOf(task)).distinct() else listOf(hue)
    Orb(1f - vm.focusRemainingMs / vm.phaseDurationMs().toFloat(), hues, size, still = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = size / 6)) {
            Eyebrow(if (work) "Фокус" else "Перерыв", color = tone(hue).ink)
            Text(
                formatTimer(vm.focusRemainingMs),
                style = Type.num.copy(fontSize = (size.value / 4.6f).sp),
                color = p.ink,
                maxLines = 1,
            )
            if (task != null) {
                Text(
                    vm.taskTitle(task),
                    style = Type.footnote,
                    color = p.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun FocusControls(vm: DeskViewModel, modifier: Modifier = Modifier) {
    val p = palette()
    val hue = if (vm.focusPhase == FocusPhase.Work) Hues.ACCENT else Hues.GROW
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            PillButton(null, vm::resetFocus, icon = Icons.Rounded.Refresh, style = PillStyle.Plain, haptic = true)
            val t = tone(hue)
            Box(
                Modifier
                    .size(80.dp)
                    .pressable(scale = 0.9f, haptic = true, onClick = vm::toggleFocusTimer)
                    .shadow(16.dp, CircleShape, ambientColor = t.glow, spotColor = t.glow)
                    .clip(CircleShape)
                    .background(t.grad)
                    .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.3f), 0.58f to Color.Transparent)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(vm.focusRunning, label = "play") { running ->
                    Icon(
                        if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (running) "Пауза" else "Старт",
                        Modifier.size(38.dp),
                        tint = tone(hue).onT,
                    )
                }
            }
            PillButton(null, vm::skipPhase, icon = Icons.Rounded.SkipNext, style = PillStyle.Plain, haptic = true)
        }
        Spacer(Modifier.height(20.dp))
        Segmented(
            Presets.map { "$it мин" },
            Presets.indexOf(vm.focusMinutes).coerceAtLeast(0),
            { vm.setFocusPreset(Presets[it]) },
            Modifier.width(240.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val inCycle = if (vm.focusSessions > 0 && vm.focusSessions % 4 == 0) 4 else vm.focusSessions % 4
            repeat(4) { i ->
                Orb(if (i < inCycle) 1f else 0f, listOf(Hues.ACCENT), 16.dp, Modifier.padding(horizontal = 3.dp), springy = false)
            }
            Spacer(Modifier.width(10.dp))
            Text("Сессий сегодня: ${vm.focusSessions}", style = Type.footnote, color = p.muted)
        }
    }
}

/** Пока идёт фокус-сессия — экран не гаснет. */
@Composable
private fun KeepScreenOnWhileRunning(vm: DeskViewModel) {
    val view = LocalView.current
    DisposableEffect(vm.focusRunning) {
        view.keepScreenOn = vm.focusRunning
        onDispose { view.keepScreenOn = false }
    }
}

/** Фокус на раскрытом экране: слева сфера-таймер, справа задача, над которой работаем. */
@Composable
fun DeskFocusPanel(vm: DeskViewModel, data: DeskData, modifier: Modifier = Modifier) {
    val p = palette()
    KeepScreenOnWhileRunning(vm)
    Row(modifier) {
        Box(Modifier.weight(1f).fillMaxSize().glass(RoundedCornerShape(30.dp), p, tint = tone(Hues.AIM))) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val orb = min(maxWidth - 80.dp, maxHeight - 250.dp).coerceIn(180.dp, 360.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FocusOrb(vm, data, orb)
                    Spacer(Modifier.height(30.dp))
                    FocusControls(vm)
                }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(40.dp)
                    .pressable(scale = 0.88f) { vm.deskFocus = false }
                    .glass(CircleShape, p),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, "Выйти из фокуса", Modifier.size(20.dp), tint = p.ink) }
        }
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f).fillMaxSize().glass(RoundedCornerShape(30.dp), p)) {
            val task = vm.focusTaskId?.let { data.task(it) }
            if (task != null) TaskDetail(vm, data, task, onClose = { vm.focusTaskId = null }) else TaskPicker(vm, data)
        }
    }
}

@Composable
private fun TaskPicker(vm: DeskViewModel, data: DeskData) {
    val p = palette()
    val open = data.tasks
        .filter { it.status != TaskStatus.DONE }
        .sortedBy { if (it.status == TaskStatus.DOING) 0 else 1 }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp)) {
        item {
            Text("Над чем работаем?", style = Type.title, color = p.ink)
            Text("Выберите задачу — она будет рядом с таймером", style = Type.callout, color = p.muted)
            Spacer(Modifier.height(12.dp))
        }
        if (open.isEmpty()) item { EmptyState("Открытых задач нет") }
        items(open, key = { it.id }) { t ->
            TaskRow(vm, data, t, onClick = { vm.focusTaskId = t.id }, modifier = Modifier.animateItem())
        }
    }
}

/**
 * Поза «ноутбук» (полураскрыт, стоит на столе): верхняя половина — сфера-таймер,
 * нижняя — кнопки и подзадачи. Граница точно по линии сгиба.
 */
@Composable
fun TabletopScreen(vm: DeskViewModel, data: DeskData, foldTopPx: Int) {
    KeepScreenOnWhileRunning(vm)
    val p = palette()
    val topHeight = with(LocalDensity.current) { foldTopPx.toDp() }
    val task = vm.focusTaskId?.let { data.task(it) }
    val subtasks = task?.let { data.subtasksByTask[it.id] }.orEmpty()
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(topHeight)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            val orb = min(maxHeight - 28.dp, maxWidth - 48.dp).coerceIn(140.dp, 420.dp)
            FocusOrb(vm, data, orb)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { FocusControls(vm) }
            if (subtasks.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Подзадачи", Modifier.widthIn(max = 520.dp))
                }
                items(subtasks, key = { it.id }) { s ->
                    Row(
                        Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LiquidCheck(s.done, { vm.toggleSubtask(s) }, data.hueOf(task!!), size = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(s.title, style = Type.body.copy(fontWeight = FontWeight.Medium), color = if (s.done) p.muted else p.ink)
                    }
                }
            }
        }
    }
}
