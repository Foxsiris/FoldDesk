package com.folddesk.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folddesk.app.data.Note
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.data.noteBody
import com.folddesk.app.data.noteTitle
import com.folddesk.app.ui.DeskData
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.dueLabel
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.projectHue
import com.folddesk.app.ui.theme.tone
import com.folddesk.app.ui.todayEpoch

fun DeskData.hueOf(task: Task): Float = task.projectId?.let { projectById[it] }?.let { projectHue(it.colorIndex) } ?: Hues.ACCENT

/** «Работа · Завтра · 1/3» — одна строка вместо россыпи чипов. Срок окрашен по срочности. */
@Composable
fun taskMeta(vm: DeskViewModel, data: DeskData, task: Task, showProject: Boolean): AnnotatedString? {
    val p = palette()
    val parts = mutableListOf<Pair<String, Color>>()
    val project = task.projectId?.let { data.projectById[it] }
    if (showProject && project != null) parts += project.name to tone(projectHue(project.colorIndex)).ink
    task.dueEpochDay?.let { due ->
        val today = todayEpoch()
        val color = when {
            task.status == TaskStatus.DONE -> p.muted
            due < today -> tone(Hues.WARN).ink
            due == today -> tone(Hues.FIRE).ink
            else -> p.muted
        }
        parts += dueLabel(due) to color
    }
    val subtasks = data.subtasksByTask[task.id].orEmpty()
    if (subtasks.isNotEmpty()) parts += "${subtasks.count { it.done }}/${subtasks.size}" to p.muted
    if (vm.taskNotes(task).isNotBlank()) parts += "✎" to p.faint
    if (parts.isEmpty()) return null
    return buildAnnotatedString {
        parts.forEachIndexed { i, (text, color) ->
            if (i > 0) withStyle(SpanStyle(color = p.faint)) { append("  ·  ") }
            withStyle(SpanStyle(color = color)) { append(text) }
        }
    }
}

/** Строка задачи в списках: бусина, заголовок, строка мета. */
@Composable
fun TaskRow(
    vm: DeskViewModel,
    data: DeskData,
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showProject: Boolean = true,
) {
    val p = palette()
    val done = task.status == TaskStatus.DONE
    val meta = taskMeta(vm, data, task, showProject)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) p.fill2 else Color.Transparent)
            .pressable(scale = 0.985f, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiquidCheck(done, { vm.toggleDone(task) }, data.hueOf(task), size = 30.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                vm.taskTitle(task).ifBlank { "Без названия" },
                style = Type.body,
                color = if (done) p.muted else p.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Spacer(Modifier.height(2.dp))
                Text(meta, style = Type.footnote, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (task.important && !done) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Flag, "Важно", Modifier.size(16.dp), tint = tone(Hues.WARN).ink)
        }
    }
}

/** Карточка задачи на доске — плоская панель внутри стеклянной колонки. */
@Composable
fun BoardCard(
    vm: DeskViewModel,
    data: DeskData,
    task: Task,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProject: Boolean = true,
) {
    val p = palette()
    val done = task.status == TaskStatus.DONE
    val meta = taskMeta(vm, data, task, showProject)
    val hue = data.hueOf(task)
    Column(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.97f, onClick = onClick)
            .flat(RoundedCornerShape(20.dp), p, if (selected) tone(hue) else null)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            LiquidCheck(done, { vm.toggleDone(task) }, hue, size = 24.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                vm.taskTitle(task).ifBlank { "Без названия" },
                style = Type.subhead.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = if (done) p.muted else p.ink,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = 2.dp),
            )
            if (task.important && !done) Icon(Icons.Filled.Flag, "Важно", Modifier.size(15.dp), tint = tone(Hues.WARN).ink)
        }
        if (meta != null) {
            Spacer(Modifier.height(6.dp))
            Text(meta, style = Type.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 34.dp))
        }
    }
}

@Composable
fun NoteTile(
    vm: DeskViewModel,
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    previewLines: Int = 5,
    asGlass: Boolean = false,
) {
    val p = palette()
    val text = vm.noteText(note)
    val body = noteBody(text)
    val sky = tone(Hues.SKY)
    val shape = RoundedCornerShape(if (asGlass) 26.dp else 18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.97f, onClick = onClick)
            .then(
                if (asGlass) Modifier.glass(shape, p, tint = if (note.pinned) sky else null)
                else Modifier.flat(shape, p, if (selected) sky else null),
            )
            .padding(if (asGlass) 16.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                if (text.isBlank()) "Новая заметка" else noteTitle(text),
                style = Type.subhead.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = p.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (note.pinned) Icon(Icons.Filled.PushPin, "Закреплено", Modifier.size(13.dp), tint = sky.ink)
        }
        if (body.isNotEmpty() && previewLines > 0) {
            Spacer(Modifier.height(3.dp))
            Text(body, style = Type.footnote.copy(fontWeight = FontWeight.Normal), color = p.muted, maxLines = previewLines, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Поле с плейсхолдером без рамок — основа всех полей ввода. */
@Composable
fun PlainField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholderColor: Color? = null,
) {
    val p = palette()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = style.copy(color = style.color.takeIf { it != Color.Unspecified } ?: p.ink),
        cursorBrush = SolidColor(tone(Hues.ACCENT).t),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style, color = placeholderColor ?: p.faint, maxLines = if (singleLine) 1 else Int.MAX_VALUE)
                inner()
            }
        },
    )
}

/** «Набрал → Enter»: клавиатура остаётся, можно сразу писать следующее. */
@Composable
fun QuickAdd(
    placeholder: String,
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    glassy: Boolean = false,
    height: Dp = 50.dp,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val p = palette()
    val accent = tone(Hues.ACCENT)
    val submit = {
        val v = text.trim()
        if (v.isNotEmpty()) onAdd(v)
        text = ""
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(if (glassy) Modifier.glass(Pill, p, thick = true) else Modifier.flat(Pill, p))
            .padding(start = 8.dp, end = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(accent.tint), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = accent.ink)
        }
        Spacer(Modifier.width(10.dp))
        PlainField(
            value = text,
            onChange = { text = it },
            placeholder = placeholder,
            style = Type.body,
            singleLine = true,
            imeAction = ImeAction.Done,
            onDone = submit,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(text.isNotBlank(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            PillButton(null, submit, icon = Icons.Rounded.ArrowUpward, style = PillStyle.Primary, size = PillSize.Sm, haptic = true)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, count: Int? = null, trailing: (@Composable () -> Unit)? = null) {
    val p = palette()
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(title)
        if (count != null && count > 0) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = Type.eyebrow, color = p.faint)
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun EmptyState(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val p = palette()
    Column(modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = Type.headline, color = p.ink2)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = Type.footnote, color = p.muted)
        }
    }
}

/** Первая строка заметки — крупным жирным, как заголовок. */
val FirstLineAsTitle = VisualTransformation { text: AnnotatedString ->
    val end = text.text.indexOf('\n').let { if (it < 0) text.length else it }
    val styled = buildAnnotatedString {
        append(text)
        if (end > 0) addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.4).sp), 0, end)
    }
    TransformedText(styled, OffsetMapping.Identity)
}
