package com.folddesk.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import com.folddesk.app.data.TaskStatus
import com.folddesk.app.ui.components.Sky
import com.folddesk.app.ui.components.ToastHost
import com.folddesk.app.ui.components.ToastState
import com.folddesk.app.ui.components.hueOf
import com.folddesk.app.ui.cover.CoverScreen
import com.folddesk.app.ui.desk.DeskScreen
import com.folddesk.app.ui.drag.DragDropState
import com.folddesk.app.ui.drag.DragGhost
import com.folddesk.app.ui.drag.LocalDragDrop
import com.folddesk.app.ui.focus.TabletopScreen
import com.folddesk.app.ui.theme.palette

/**
 * Раскладка выбирается по фактическому окну, а не по модели телефона:
 *  • узкое окно (внешний экран) → «карман»;
 *  • широкое (внутренний экран, DeX, split-screen) → «рабочий стол»;
 *  • полураскрыт со сгибом по горизонтали → «ноутбук» для фокуса.
 */
@Composable
fun FoldDeskRoot(vm: DeskViewModel, layoutInfo: WindowLayoutInfo?) {
    val data by vm.data.collectAsStateWithLifecycle()
    val p = palette()
    val dragState = remember { DragDropState() }
    dragState.onDrop = vm::handleDrop
    val toast = remember { ToastState() }
    LaunchedEffect(Unit) { vm.messages.collect { toast.show(it) } }

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(vm::appendDraft)
    }
    val onVoice: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите…")
        try {
            voice.launch(intent)
        } catch (e: ActivityNotFoundException) {
            vm.toast("Голосовой ввод недоступен")
        }
    }

    val fold = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()?.firstOrNull()
    val tabletop = fold != null &&
        fold.state == FoldingFeature.State.HALF_OPENED &&
        fold.orientation == FoldingFeature.Orientation.HORIZONTAL

    // Каждое выполненное сегодня дело зажигает на обоях свет своего цвета
    val lit = data.tasks
        .filter { it.status == TaskStatus.DONE && isToday(it.completedAt) }
        .sortedBy { it.completedAt }
        .map { it.id to data.hueOf(it) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(p.bg),
    ) {
        val mode = when {
            tabletop -> LayoutMode.Tabletop
            maxWidth >= 600.dp && maxHeight >= 480.dp -> LayoutMode.Desk
            else -> LayoutMode.Cover
        }
        LaunchedEffect(mode) { vm.onLayoutChanged(mode) }

        Sky(lit)

        CompositionLocalProvider(LocalDragDrop provides dragState) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(280)) + scaleIn(spring(dampingRatio = 0.85f, stiffness = 260f), initialScale = 0.95f)) togetherWith fadeOut(tween(140))
                },
                label = "layout",
            ) { m ->
                when (m) {
                    LayoutMode.Cover -> CoverScreen(vm, data, onVoice)
                    LayoutMode.Desk -> DeskScreen(vm, data)
                    LayoutMode.Tabletop -> TabletopScreen(vm, data, fold?.bounds?.top ?: 0)
                }
            }
            DragGhost(dragState)
        }

        ToastHost(
            toast,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (mode == LayoutMode.Cover) 92.dp else 24.dp),
        )
    }
}
