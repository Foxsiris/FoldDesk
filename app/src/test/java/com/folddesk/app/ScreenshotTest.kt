package com.folddesk.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.FoldDeskRoot
import com.folddesk.app.ui.Selection
import com.folddesk.app.ui.theme.FoldDeskTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Рендерит экраны на компьютере и сохраняет PNG в app/build/screens —
 * чтобы проверять дизайн без телефона: ./gradlew :app:testDebugUnitTest
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String, setup: (DeskViewModel) -> Unit = {}) {
        val app = ApplicationProvider.getApplicationContext<FoldDeskApp>()
        val vm = DeskViewModel(app, SavedStateHandle())
        rule.mainClock.autoAdvance = false
        rule.setContent { FoldDeskTheme { FoldDeskRoot(vm, null) } }
        var ready = 0
        for (i in 0 until 120) {
            rule.mainClock.advanceTimeBy(100)
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(25)
            if (vm.data.value.tasks.isNotEmpty() && vm.data.value.notes.isNotEmpty()) {
                if (ready == 0) setup(vm)
                if (++ready > 20) break
            }
        }
        rule.mainClock.advanceTimeBy(3000)
        shadowOf(Looper.getMainLooper()).idle()
        // Рисуем окно напрямую: бесконечные анимации (волны в сферах) не дают дождаться «покоя»
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val out = File("build/screens/$name.png")
        out.parentFile?.mkdirs()
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w400dp-h880dp-night-xxhdpi")
    fun coverDark() = shoot("cover_dark")

    @Test
    @Config(qualifiers = "w400dp-h880dp-notnight-xxhdpi")
    fun coverLight() = shoot("cover_light")

    @Test
    @Config(qualifiers = "w820dp-h730dp-night-xhdpi")
    fun deskDark() = shoot("desk_dark")

    @Test
    @Config(qualifiers = "w820dp-h730dp-night-xhdpi")
    fun deskTaskDark() = shoot("desk_task_dark") { it.select(Selection.OfTask(1)) }

    @Test
    @Config(qualifiers = "w820dp-h730dp-notnight-xhdpi")
    fun deskTaskLight() = shoot("desk_task_light") { it.select(Selection.OfTask(1)) }

    @Test
    @Config(qualifiers = "w820dp-h730dp-night-xhdpi")
    fun deskFocusDark() = shoot("desk_focus_dark") {
        it.focusTaskId = 1
        it.deskFocus = true
    }
}
