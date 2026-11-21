package com.folddesk.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import com.folddesk.app.ui.DeskViewModel
import com.folddesk.app.ui.FoldDeskRoot
import com.folddesk.app.ui.theme.FoldDeskTheme

class MainActivity : ComponentActivity() {
    private val vm: DeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            FoldDeskTheme {
                // Поза устройства: сгиб, его положение и состояние (раскрыт / «ноутбук» / «книга»)
                val layoutFlow = remember { WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this) }
                val layoutInfo by layoutFlow.collectAsStateWithLifecycle<WindowLayoutInfo?>(initialValue = null)
                FoldDeskRoot(vm, layoutInfo)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_CAPTURE) vm.requestCapture()
    }

    companion object {
        const val ACTION_CAPTURE = "com.folddesk.app.CAPTURE"
    }
}
