package com.folddesk.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/*
 * Дизайн-язык FoldDesk — Apple «Liquid Glass», как в MyGoals:
 * живые обои из цветного света, поверх — стеклянные панели с бликом и ободком.
 * Цвет — это оттенок (hue): из него выводится весь набор тона, как --t / --t-ink / --t-tint в MyGoals.
 */

/** OKLCH → sRGB, чтобы оттенки совпадали с MyGoals один в один. */
fun oklch(l: Float, c: Float, hue: Float, alpha: Float = 1f): Color {
    val h = Math.toRadians(hue.toDouble())
    val a = c * cos(h)
    val b = c * sin(h)
    val l1 = (l + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m1 = (l - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s1 = (l - 0.0894841775 * a - 1.2914855480 * b).pow(3)
    val r = 4.0767416621 * l1 - 3.3077115913 * m1 + 0.2309699292 * s1
    val g = -1.2684380046 * l1 + 2.6097574011 * m1 - 0.3413193965 * s1
    val bl = -0.0041960863 * l1 - 0.7034186147 * m1 + 1.7076147010 * s1
    fun enc(x: Double): Float {
        val v = if (x <= 0.0031308) 12.92 * x else 1.055 * x.pow(1 / 2.4) - 0.055
        return v.coerceIn(0.0, 1.0).toFloat()
    }
    return Color(enc(r), enc(g), enc(bl), alpha.coerceIn(0f, 1f))
}

@Immutable
data class Palette(
    val dark: Boolean,
    val bg: Color,
    val wallA: Color,
    val wallB: Color,
    val lightL: Float,
    val lightC: Float,
    val lightA: Float,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val faint: Color,
    val glass: Color,
    val glassThick: Color,
    val glassSolid: Color,
    val fill: Color,
    val fill2: Color,
    val fill3: Color,
    val line: Color,
    val line2: Color,
    val scrim: Color,
    val rimHi: Color,
    val rimMid: Color,
    val rimLo: Color,
    val sheen: Color,
    val tL: Float,
    val tC: Float,
    val inkL: Float,
    val inkC: Float,
)

private val White = Color.White

val DarkPalette = Palette(
    dark = true,
    bg = oklch(0.13f, 0.035f, 278f),
    wallA = oklch(0.27f, 0.1f, 270f),
    wallB = oklch(0.15f, 0.06f, 300f),
    lightL = 0.7f, lightC = 0.23f, lightA = 0.66f,
    ink = oklch(0.985f, 0.003f, 270f),
    ink2 = oklch(0.985f, 0.003f, 270f, 0.84f),
    muted = oklch(0.96f, 0.01f, 270f, 0.6f),
    faint = oklch(0.96f, 0.01f, 270f, 0.36f),
    glass = oklch(0.24f, 0.04f, 275f, 0.42f),
    glassThick = oklch(0.22f, 0.04f, 275f, 0.9f),
    glassSolid = oklch(0.22f, 0.03f, 275f),
    fill = White.copy(alpha = 0.08f),
    fill2 = White.copy(alpha = 0.14f),
    fill3 = White.copy(alpha = 0.22f),
    line = White.copy(alpha = 0.1f),
    line2 = White.copy(alpha = 0.18f),
    scrim = oklch(0.08f, 0.03f, 275f, 0.5f),
    rimHi = White.copy(alpha = 0.6f),
    rimMid = White.copy(alpha = 0.26f),
    rimLo = White.copy(alpha = 0.06f),
    sheen = White.copy(alpha = 0.12f),
    tL = 0.8f, tC = 0.18f, inkL = 0.88f, inkC = 0.11f,
)

val LightPalette = Palette(
    dark = false,
    bg = oklch(0.94f, 0.025f, 265f),
    wallA = oklch(0.95f, 0.035f, 240f),
    wallB = oklch(0.955f, 0.03f, 330f),
    lightL = 0.86f, lightC = 0.13f, lightA = 0.8f,
    ink = oklch(0.2f, 0.025f, 272f),
    ink2 = oklch(0.2f, 0.025f, 272f, 0.86f),
    muted = oklch(0.25f, 0.03f, 272f, 0.62f),
    faint = oklch(0.25f, 0.03f, 272f, 0.36f),
    glass = White.copy(alpha = 0.55f),
    glassThick = White.copy(alpha = 0.92f),
    glassSolid = oklch(0.985f, 0.005f, 270f),
    fill = oklch(0.3f, 0.04f, 272f, 0.06f),
    fill2 = oklch(0.3f, 0.04f, 272f, 0.1f),
    fill3 = oklch(0.3f, 0.04f, 272f, 0.16f),
    line = oklch(0.3f, 0.04f, 272f, 0.09f),
    line2 = oklch(0.3f, 0.04f, 272f, 0.16f),
    scrim = oklch(0.3f, 0.05f, 272f, 0.26f),
    rimHi = White.copy(alpha = 0.95f),
    rimMid = White.copy(alpha = 0.6f),
    rimLo = White.copy(alpha = 0.28f),
    sheen = White.copy(alpha = 0.5f),
    tL = 0.76f, tC = 0.17f, inkL = 0.5f, inkC = 0.19f,
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

/** Набор цветов одного оттенка — аналог класса tone-* в MyGoals. */
@Immutable
class Tone(val hue: Float, p: Palette) {
    /** Яркая заливка. */
    val t = oklch(p.tL, p.tC, hue)
    /** Партнёр для градиента. */
    val t2 = oklch(p.tL + 0.05f, p.tC, hue - 34f)
    /** Цветной текст на стекле. */
    val ink = oklch(p.inkL, p.inkC, hue)
    /** Текст на заливке. */
    val onT = oklch(0.19f, 0.06f, hue)
    /** Мягкая подложка. */
    val tint = oklch(p.inkL, p.inkC, hue, 0.16f)
    val line = oklch(p.inkL, p.inkC, hue, 0.36f)
    val glow = oklch(p.tL, p.tC, hue, 0.6f)
    val grad: Brush = Brush.linearGradient(listOf(t2, t))
    /** Свет на обоях этого оттенка. */
    val light = oklch(p.lightL, p.lightC, hue)
}

object Hues {
    const val ACCENT = 255f
    const val GROW = 152f
    const val AIM = 292f
    const val FIRE = 62f
    const val SKY = 222f
    const val WARN = 16f
}

private val ProjectHues = listOf(255f, 16f, 172f, 62f, 340f, 222f, 140f, 292f)

val projectPaletteSize get() = ProjectHues.size

fun projectHue(index: Int): Float = ProjectHues[Math.floorMod(index, ProjectHues.size)]

@Composable
@ReadOnlyComposable
fun palette(): Palette = LocalPalette.current

@Composable
fun tone(hue: Float): Tone {
    val p = LocalPalette.current
    return remember(hue, p) { Tone(hue, p) }
}
