package com.folddesk.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.folddesk.app.data.TaskStatus

/** Шкала шрифтов в духе iOS: крупные жирные заголовки, плотный трекинг, спокойный текст. */
object Type {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.032).em, lineHeight = 38.sp)
    val title = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.028).em, lineHeight = 30.sp)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.024).em, lineHeight = 26.sp)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.018).em, lineHeight = 22.sp)
    val body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.011).em, lineHeight = 22.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val callout = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.011).em, lineHeight = 20.sp)
    val subhead = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.008).em, lineHeight = 19.sp)
    val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
    val eyebrow = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.05.em)
    val tab = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    /** Цифры: жирные, табличные — чтобы таймер не «прыгал». */
    val num = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em, fontFeatureSettings = "tnum")
}

/** Тёмная тема по умолчанию, как в MyGoals; светлая — если так выбрано в системе. */
@Composable
fun FoldDeskTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val accent = remember(p) { Tone(Hues.ACCENT, p) }
    val scheme = remember(p) {
        val base = if (p.dark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = accent.t,
            onPrimary = accent.onT,
            primaryContainer = accent.tint,
            onPrimaryContainer = accent.ink,
            secondaryContainer = p.fill2,
            onSecondaryContainer = p.ink,
            background = p.bg,
            onBackground = p.ink,
            surface = p.glassSolid,
            onSurface = p.ink,
            onSurfaceVariant = p.muted,
            surfaceContainer = p.glassSolid,
            surfaceContainerLow = p.glassSolid,
            surfaceContainerHigh = p.glassSolid,
            outline = p.line2,
            outlineVariant = p.line,
        )
    }
    val selection = remember(accent) { TextSelectionColors(handleColor = accent.t, backgroundColor = accent.t.copy(alpha = 0.35f)) }
    CompositionLocalProvider(LocalPalette provides p, LocalTextSelectionColors provides selection) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(10.dp),
                small = RoundedCornerShape(14.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(30.dp),
            ),
        ) {
            // MaterialTheme переопределяет цвета выделения — возвращаем свои
            CompositionLocalProvider(LocalTextSelectionColors provides selection, content = content)
        }
    }
}

fun statusHue(status: TaskStatus): Float = when (status) {
    TaskStatus.TODO -> Hues.ACCENT
    TaskStatus.DOING -> Hues.FIRE
    TaskStatus.DONE -> Hues.GROW
}
