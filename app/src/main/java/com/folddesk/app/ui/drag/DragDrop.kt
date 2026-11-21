package com.folddesk.app.ui.drag

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.folddesk.app.ui.components.glass
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.tone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.folddesk.app.data.Note
import com.folddesk.app.data.Task
import com.folddesk.app.data.TaskStatus
import kotlin.math.roundToInt

sealed interface DragPayload {
    data class OfTask(val task: Task) : DragPayload
    data class OfNote(val note: Note) : DragPayload
}

sealed interface DropTarget {
    data class Column(val status: TaskStatus) : DropTarget
    data class ToProject(val projectId: Long) : DropTarget
}

/**
 * Перетаскивание между панелями одного окна: заметку из левой панели — в колонку доски,
 * задачу — в другую колонку или на проект. Координаты — в системе корня Compose.
 */
@Stable
class DragDropState {
    var payload by mutableStateOf<DragPayload?>(null)
        private set
    var label by mutableStateOf("")
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var hovered by mutableStateOf<DropTarget?>(null)
        private set

    val isDragging get() = payload != null

    var onDrop: (DragPayload, DropTarget) -> Unit = { _, _ -> }

    private val targets = HashMap<DropTarget, Rect>()

    fun register(target: DropTarget, bounds: Rect) {
        targets[target] = bounds
    }

    fun unregister(target: DropTarget) {
        targets.remove(target)
    }

    fun start(p: DragPayload, text: String, at: Offset) {
        payload = p
        label = text
        pointer = at
        hovered = hit(at)
    }

    fun move(delta: Offset) {
        pointer += delta
        hovered = hit(pointer)
    }

    /** true, если отпустили над целью. */
    fun end(): Boolean {
        val p = payload
        val t = hovered
        cancel()
        if (p != null && t != null) {
            onDrop(p, t)
            return true
        }
        return false
    }

    fun cancel() {
        payload = null
        hovered = null
    }

    private fun hit(at: Offset): DropTarget? = targets.entries.firstOrNull { it.value.contains(at) }?.key
}

val LocalDragDrop = compositionLocalOf { DragDropState() }

private class OffsetHolder {
    var value = Offset.Zero
}

/** Долгое нажатие — и элемент «поднимается» под пальцем. */
@Composable
fun Modifier.dragSource(payload: DragPayload, label: String): Modifier {
    val state = LocalDragDrop.current
    val haptic = LocalHapticFeedback.current
    val currentPayload by rememberUpdatedState(payload)
    val currentLabel by rememberUpdatedState(label)
    val origin = remember { OffsetHolder() }
    val lifted = state.payload == payload
    return this
        .onGloballyPositioned { origin.value = it.positionInRoot() }
        .graphicsLayer { alpha = if (lifted) 0.35f else 1f }
        .pointerInput(state) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.start(currentPayload, currentLabel, origin.value + offset)
                },
                onDrag = { change, amount ->
                    change.consume()
                    state.move(amount)
                },
                onDragEnd = {
                    if (state.end()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onDragCancel = { state.cancel() },
            )
        }
}

@Composable
fun Modifier.dropTarget(target: DropTarget): Modifier {
    val state = LocalDragDrop.current
    DisposableEffect(state, target) {
        onDispose { state.unregister(target) }
    }
    return onGloballyPositioned { state.register(target, it.boundsInRoot()) }
}

/** «Призрак» под пальцем во время перетаскивания. */
@Composable
fun DragGhost(state: DragDropState) {
    val payload = state.payload ?: return
    val icon = when (payload) {
        is DragPayload.OfTask -> Icons.Outlined.TaskAlt
        is DragPayload.OfNote -> Icons.Outlined.Lightbulb
    }
    val overTarget = state.hovered != null
    val p = palette()
    val accent = tone(Hues.ACCENT)
    val tilt by animateFloatAsState(if (overTarget) 0f else -3f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "tilt")
    val scale by animateFloatAsState(if (overTarget) 0.95f else 1.05f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "lift")
    Row(
        Modifier
            .offset {
                IntOffset(
                    (state.pointer.x - 32.dp.toPx()).roundToInt(),
                    (state.pointer.y - 36.dp.toPx()).roundToInt(),
                )
            }
            .graphicsLayer {
                rotationZ = tilt
                scaleX = scale
                scaleY = scale
            }
            .widthIn(max = 280.dp)
            .shadow(20.dp, RoundedCornerShape(20.dp))
            .glass(RoundedCornerShape(20.dp), p, thick = true, tint = accent)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = accent.ink)
        Spacer(Modifier.width(10.dp))
        Text(state.label, style = Type.subhead.copy(fontWeight = FontWeight.SemiBold), color = p.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
