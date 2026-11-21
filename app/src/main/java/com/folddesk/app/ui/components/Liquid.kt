package com.folddesk.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.folddesk.app.ui.theme.Palette
import com.folddesk.app.ui.theme.oklch
import com.folddesk.app.ui.theme.palette
import com.folddesk.app.ui.theme.tone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Контур жидкости: волна по поверхности y, ниже — заливка до дна. */
private fun wavePath(w: Float, h: Float, y: Float, amp: Float, phase: Float): Path {
    val wl = w / 2f
    val path = Path()
    var x = -wl + phase * wl
    path.moveTo(x, y)
    var up = true
    while (x < w + wl) {
        path.quadraticBezierTo(x + wl / 4f, y + if (up) -amp else amp, x + wl / 2f, y)
        x += wl / 2f
        up = !up
    }
    path.lineTo(x, h + 2f)
    path.lineTo(-wl, h + 2f)
    path.close()
    return path
}

private fun circlePath(s: DrawScope) = Path().apply {
    addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, s.size))
}

/** Стеклянная сфера: блики и ободок поверх содержимого. */
private fun DrawScope.glassSphere(p: Palette, behind: Boolean) {
    val r = size.minDimension / 2f
    if (behind) {
        drawCircle(p.fill, r)
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), center = Offset(size.width / 2, size.height * 1.12f), radius = r * 1.2f),
            r,
        )
    } else {
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.42f), Color.Transparent), center = Offset(size.width * 0.32f, size.height * 0.2f), radius = r * 0.62f),
            r,
        )
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), center = Offset(size.width * 0.7f, size.height * 0.86f), radius = r * 0.42f),
            r,
        )
        drawCircle(p.line2, r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
        drawArc(
            p.rimHi,
            startAngle = 200f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
            style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round),
            alpha = 0.7f,
        )
    }
}

/**
 * Сфера с жидким светом. Наполняется по progress; жидкость — смесь оттенков
 * (например, проектов выполненных дел), поэтому у каждого дня свой «узор».
 */
@Composable
fun Orb(
    progress: Float,
    hues: List<Float>,
    size: Dp,
    modifier: Modifier = Modifier,
    still: Boolean = size < 70.dp,
    springy: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p = palette()
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, spring(dampingRatio = 0.7f, stiffness = 35f), label = "orbLevel")
    val level = if (springy) animated else target
    val phase = if (still) {
        0f
    } else {
        val inf = rememberInfiniteTransition(label = "orbWave")
        inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    }
    val colors = remember(hues, p) {
        val list = hues.ifEmpty { listOf(255f) }
        if (list.size == 1) listOf(oklch(p.tL, p.tC + 0.03f, list[0] - 34f), oklch(p.tL, p.tC + 0.03f, list[0]))
        else list.map { oklch(p.tL, p.tC + 0.03f, it) }
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            glassSphere(p, behind = true)
            if (level > 0.002f) {
                val w = this.size.width
                val h = this.size.height
                val y = h * (1.06f - level * 1.12f)
                val amp = h * if (still) 0.016f else 0.026f
                val brush = Brush.verticalGradient(colors, startY = h, endY = y)
                clipPath(circlePath(this)) {
                    drawPath(wavePath(w, h, y, amp * 1.25f, (phase + 0.35f) % 1f), brush, alpha = 0.45f)
                    drawPath(wavePath(w, h, y, amp, phase), brush)
                    drawPath(
                        wavePath(w, h, y, amp, phase),
                        Color.White.copy(alpha = 0.5f),
                        style = Stroke(0.9.dp.toPx()),
                    )
                }
            }
            glassSphere(p, behind = false)
        }
        content()
    }
}

/**
 * Главный контрол — стеклянная бусина. Выполнил дело — в неё «наливается» цвет,
 * вылетают капли и проявляется галочка.
 */
@Composable
fun LiquidCheck(
    checked: Boolean,
    onToggle: () -> Unit,
    hue: Float,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    val p = palette()
    val t = tone(hue)
    val hf = LocalHapticFeedback.current
    val fill by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.62f, stiffness = 160f), label = "pour")
    val mark by animateFloatAsState(if (checked) 1f else 0.2f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "mark")
    val burst = remember { Animatable(1f) }
    var was by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked && !was) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        }
        was = checked
    }
    Box(
        modifier
            .size(size)
            .pressable(scale = 0.82f) {
                hf.performHapticFeedback(if (checked) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val c = center
            // капли и кольцо
            val b = burst.value
            if (b < 1f) {
                drawCircle(t.t, radius = r * (0.95f + 0.75f * b), center = c, alpha = (1f - b) * 0.8f, style = Stroke(2.dp.toPx()))
                for (i in 0 until 8) {
                    val a = (i * 45f - 90f) * PI.toFloat() / 180f
                    val dist = r * (0.9f + 1.25f * b)
                    val dot = if (i % 2 == 0) 3.5.dp.toPx() else 2.5.dp.toPx()
                    drawCircle(
                        if (i % 2 == 0) t.t else t.t2,
                        radius = dot * (1.2f - b),
                        center = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist),
                        alpha = 1f - b,
                    )
                }
            }
            glassSphere(p, behind = true)
            if (fill > 0.01f) {
                val w = this.size.width
                val h = this.size.height
                val y = h * (1.04f - fill * 1.14f)
                clipPath(circlePath(this)) {
                    drawPath(wavePath(w, h, y, h * 0.035f, 0.25f), Brush.verticalGradient(listOf(t.t, t.t2), startY = h, endY = y))
                }
            }
            glassSphere(p, behind = false)
            if (!checked) drawCircle(p.line2, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            // галочка
            val alpha = fill.coerceIn(0f, 1f)
            if (alpha > 0.02f) {
                val s = mark
                val w = this.size.width
                val h = this.size.height
                fun pt(x: Float, y: Float) = Offset(c.x + (x * w - c.x) * s, c.y + (y * h - c.y) * s)
                val path = Path().apply {
                    val a = pt(0.3f, 0.52f)
                    val m = pt(0.44f, 0.66f)
                    val e = pt(0.71f, 0.37f)
                    moveTo(a.x, a.y)
                    lineTo(m.x, m.y)
                    lineTo(e.x, e.y)
                }
                drawPath(path, t.onT, alpha = alpha, style = Stroke(r * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
