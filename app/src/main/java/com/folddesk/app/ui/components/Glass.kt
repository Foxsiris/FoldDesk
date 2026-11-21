@file:OptIn(ExperimentalFoundationApi::class)

package com.folddesk.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.folddesk.app.ui.theme.Hues
import com.folddesk.app.ui.theme.Palette
import com.folddesk.app.ui.theme.Tone
import com.folddesk.app.ui.theme.Type
import com.folddesk.app.ui.theme.oklch
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.tone
import java.time.LocalTime
import kotlin.math.max

val Pill = RoundedCornerShape(50)

// ── Обои: свет дня ───────────────────────────────────────────────────────────
// Три базовых пятна света по времени суток; каждое выполненное сегодня дело
// добавляет пятно своего цвета — приложение светится тем, что ты сделал.

private val BaseHues = mapOf(
    "night" to listOf(268f, 236f, 304f),
    "morning" to listOf(44f, 338f, 224f),
    "day" to listOf(218f, 188f, 268f),
    "evening" to listOf(302f, 28f, 262f),
)
private val BasePos = listOf(Triple(0.1f, 0.06f, 1.05f), Triple(0.94f, 0.16f, 0.95f), Triple(0.58f, 1.04f, 1.1f))
private val Slots = listOf(
    Triple(0.26f, 0.34f, 0.8f), Triple(0.8f, 0.6f, 0.85f), Triple(0.42f, 0.84f, 0.75f), Triple(0.98f, 0.94f, 0.8f),
    Triple(0.02f, 0.7f, 0.8f), Triple(0.62f, 0.2f, 0.7f), Triple(0.3f, 1.02f, 0.8f), Triple(0.76f, 0.36f, 0.7f),
)

private fun partOfDay(hour: Int) = when {
    hour >= 22 || hour < 5 -> "night"
    hour < 11 -> "morning"
    hour < 17 -> "day"
    else -> "evening"
}

private fun lightBrush(p: Palette, hue: Float, center: Offset, radius: Float): Brush {
    val c = oklch(p.lightL, p.lightC, hue)
    return Brush.radialGradient(
        0f to c.copy(alpha = p.lightA),
        0.42f to c.copy(alpha = p.lightA * 0.45f),
        1f to c.copy(alpha = 0f),
        center = center,
        radius = radius,
    )
}

/** lit — (id, оттенок) дел, выполненных сегодня. */
@Composable
fun Sky(lit: List<Pair<Long, Float>>, modifier: Modifier = Modifier) {
    val p = palette()
    val base = remember { BaseHues.getValue(partOfDay(LocalTime.now().hour)) }
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val wall = Brush.linearGradient(
                    listOf(p.wallA, p.wallB),
                    start = Offset(size.width * 0.35f, 0f),
                    end = Offset(size.width * 0.65f, size.height),
                )
                val vmax = max(size.width, size.height)
                val lights = base.mapIndexed { i, hue ->
                    val (x, y, s) = BasePos[i]
                    val center = Offset(x * size.width, y * size.height)
                    val r = s * 0.78f * vmax / 2f
                    Triple(lightBrush(p, hue, center, r), center, r)
                }
                onDrawBehind {
                    drawRect(wall)
                    lights.forEach { (brush, center, r) -> drawCircle(brush, r, center, alpha = 0.75f) }
                }
            },
    ) {
        lit.take(Slots.size).forEachIndexed { i, (id, hue) ->
            key(id) { LitLight(p, hue, Slots[i]) }
        }
    }
}

@Composable
private fun LitLight(p: Palette, hue: Float, slot: Triple<Float, Float, Float>) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(1800, easing = FastOutSlowInEasing)) }
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val (x, y, s) = slot
                val center = Offset(x * size.width, y * size.height)
                val r = s * 0.78f * max(size.width, size.height) / 2f * (0.5f + 0.5f * anim.value)
                drawCircle(lightBrush(p, hue, center, r), r, center, alpha = anim.value)
            },
    )
}

// ── Стекло ──────────────────────────────────────────────────────────────────

private fun rimBrush(p: Palette) = Brush.linearGradient(
    0f to p.rimHi,
    0.26f to p.rimLo,
    0.68f to p.rimLo,
    1f to p.rimMid,
)

/** Стеклянная панель: полупрозрачная заливка, блик сверху, светлый ободок. */
fun Modifier.glass(shape: Shape, p: Palette, thick: Boolean = false, tint: Tone? = null): Modifier =
    this
        .clip(shape)
        .background(if (thick) p.glassThick else p.glass)
        .drawBehind {
            if (tint != null) {
                drawRect(
                    Brush.radialGradient(
                        listOf(tint.t.copy(alpha = if (p.dark) 0.26f else 0.22f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 1.15f,
                    ),
                )
            }
            drawRect(Brush.verticalGradient(0f to p.sheen, 0.42f to Color.Transparent))
        }
        .border(1.dp, rimBrush(p), shape)

/** Внутренняя панель (внутри стекла): плоская заливка, без своего «стекла». */
fun Modifier.flat(shape: Shape, p: Palette, selected: Tone? = null): Modifier =
    this
        .clip(shape)
        .background(selected?.tint ?: p.fill)
        .border(if (selected != null) 1.5.dp else 0.5.dp, selected?.line ?: p.line, shape)

/** Пружинное нажатие без ряби — как на iOS. */
fun Modifier.pressable(
    enabled: Boolean = true,
    scale: Float = 0.96f,
    haptic: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, spring(dampingRatio = 0.55f, stiffness = 520f), label = "press")
    val hf = LocalHapticFeedback.current
    this
        .graphicsLayer {
            scaleX = s
            scaleY = s
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick?.let { long ->
                {
                    hf.performHapticFeedback(HapticFeedbackType.LongPress)
                    long()
                }
            },
            onClick = {
                if (haptic) hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    thick: Boolean = false,
    tint: Tone? = null,
    padding: PaddingValues = PaddingValues(20.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = palette()
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressable(scale = 0.98f, onClick = onClick) else Modifier)
            .glass(shape, p, thick, tint)
            .padding(padding),
        content = content,
    )
}

// ── Кнопки-капсулы ──────────────────────────────────────────────────────────

enum class PillStyle { Primary, Soft, Plain, Ghost }
enum class PillSize(val height: Dp) { Sm(36.dp), Md(44.dp), Lg(52.dp) }

@Composable
fun PillButton(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillStyle = PillStyle.Plain,
    hue: Float = Hues.ACCENT,
    size: PillSize = PillSize.Md,
    enabled: Boolean = true,
    haptic: Boolean = false,
) {
    val p = palette()
    val t = tone(hue)
    val content = when (style) {
        PillStyle.Primary -> t.onT
        PillStyle.Soft -> t.ink
        PillStyle.Plain -> p.ink
        PillStyle.Ghost -> p.ink2
    }
    val iconOnly = text == null
    Row(
        modifier
            .height(size.height)
            .then(if (iconOnly) Modifier.width(size.height) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .pressable(enabled, scale = 0.94f, haptic = haptic, onClick = onClick)
            .then(
                if (style == PillStyle.Primary) {
                    Modifier.shadow(10.dp, Pill, ambientColor = t.glow, spotColor = t.glow)
                } else {
                    Modifier
                },
            )
            .clip(Pill)
            .then(
                when (style) {
                    PillStyle.Primary -> Modifier
                        .background(t.grad)
                        .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.3f), 0.58f to Color.Transparent))
                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), Pill)
                    PillStyle.Soft -> Modifier.background(t.tint).border(0.5.dp, t.line, Pill)
                    PillStyle.Plain -> Modifier.background(p.fill2).border(0.5.dp, p.line2, Pill)
                    PillStyle.Ghost -> Modifier
                },
            )
            .padding(horizontal = if (iconOnly) 0.dp else if (size == PillSize.Sm) 14.dp else 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, text, Modifier.size(if (size == PillSize.Lg) 22.dp else 19.dp), tint = content)
        }
        if (text != null) {
            if (icon != null) Spacer(Modifier.width(7.dp))
            Text(text, style = if (size == PillSize.Sm) Type.subhead.copy(fontWeight = Type.bodyStrong.fontWeight) else Type.bodyStrong, color = content, maxLines = 1)
        }
    }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, hue: Float? = null, leading: (@Composable () -> Unit)? = null) {
    val p = palette()
    val t = hue?.let { tone(it) }
    Row(
        modifier
            .height(26.dp)
            .clip(Pill)
            .background(t?.tint ?: p.fill2)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = Type.caption, color = t?.ink ?: p.ink2, maxLines = 1)
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = palette().muted) {
    Text(text.uppercase(), style = Type.eyebrow, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Стеклянная плитка с эмодзи — значок проекта. */
@Composable
fun Glyph(emoji: String, hue: Float, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    val t = tone(hue)
    val p = palette()
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(t.tint)
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = if (p.dark) 0.14f else 0.4f), 0.55f to Color.Transparent))
            .border(0.5.dp, t.line, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = Type.body.copy(fontSize = (size.value * 0.5f).sp, lineHeight = (size.value * 0.6f).sp))
    }
}

/** Сегментный переключатель с «линзой», которая перетекает под выбранный пункт. */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
) {
    val p = palette()
    val hf = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .height(height)
            .clip(Pill)
            .background(p.fill)
            .border(0.5.dp, p.line, Pill)
            .padding(3.dp),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.72f, stiffness = 420f), label = "lens")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(Pill)
                .background(if (p.dark) p.fill3 else Color.White)
                .border(0.5.dp, if (p.dark) p.rimMid else p.line, Pill),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, o ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (i != selected) hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(o, style = Type.subhead.copy(fontWeight = Type.bodyStrong.fontWeight), color = if (i == selected) p.ink else p.muted, maxLines = 1)
                }
            }
        }
    }
}

/** Переключатель как в iOS. */
@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit, hue: Float = Hues.GROW) {
    val p = palette()
    val t = tone(hue)
    val hf = LocalHapticFeedback.current
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.65f, stiffness = 500f), label = "switch")
    Box(
        Modifier
            .size(51.dp, 31.dp)
            .clip(Pill)
            .background(if (checked) t.t else p.fill3)
            .combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onChange(!checked)
            }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

data class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/** Плавающая стеклянная панель вкладок, линза скользит под активной вкладкой. */
@Composable
fun GlassTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = palette()
    val accent = tone(Hues.ACCENT)
    val hf = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .widthIn(max = 430.dp)
            .fillMaxWidth()
            .height(64.dp)
            .glass(Pill, p, thick = true)
            .padding(5.dp),
    ) {
        val w = maxWidth / items.size
        val x by animateDpAsState(w * selected, spring(dampingRatio = 0.62f, stiffness = 380f), label = "tabLens")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .clip(Pill)
                .background(p.fill2)
                .border(1.dp, rimBrush(p), Pill),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, item ->
                val active = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressable(scale = 0.9f) {
                            if (!active) hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (active) item.selectedIcon else item.icon, null, Modifier.size(23.dp), tint = if (active) accent.ink else p.muted)
                    Spacer(Modifier.height(2.dp))
                    Text(item.label, style = Type.tab, color = if (active) accent.ink else p.muted)
                }
            }
        }
    }
}

/** «Ручка» шторки. */
@Composable
fun Grabber() {
    val p = palette()
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(38.dp, 5.dp).clip(Pill).background(p.fill3))
    }
}

/** Группа строк как в настройках iOS: плоская панель с тонкими разделителями. */
@Composable
fun GroupedRows(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = palette()
    Column(modifier.fillMaxWidth().flat(RoundedCornerShape(20.dp), p), content = content)
}

@Composable
fun GroupRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    title: String,
    divider: Boolean = true,
) {
    val p = palette()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .then(if (onClick != null) Modifier.pressable(scale = 0.985f, onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Text(title, style = Type.body, color = p.ink, modifier = Modifier.weight(1f), maxLines = 1)
            trailing?.invoke(this)
        }
        if (divider) Box(Modifier.padding(start = if (leading != null) 58.dp else 14.dp).fillMaxWidth().height(0.5.dp).background(p.line2))
    }
}

// ── Тосты ───────────────────────────────────────────────────────────────────

@Stable
class ToastState {
    var text by mutableStateOf<String?>(null)
        private set
    var serial by mutableIntStateOf(0)
        private set

    fun show(message: String) {
        text = message
        serial++
    }

    fun hide() {
        text = null
    }
}

@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val p = palette()
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(state.serial) {
        state.text?.let { shown = it }
        if (state.text != null) {
            kotlinx.coroutines.delay(2200)
            state.hide()
        }
    }
    AnimatedVisibility(
        visible = state.text != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 400f)) { it } + fadeIn() + scaleIn(initialScale = 0.9f),
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        Box(
            Modifier
                .glass(Pill, p, thick = true)
                .padding(horizontal = 20.dp, vertical = 13.dp),
        ) {
            Text(shown, style = Type.subhead.copy(fontWeight = Type.bodyStrong.fontWeight), color = p.ink)
        }
    }
}
