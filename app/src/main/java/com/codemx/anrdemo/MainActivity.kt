package com.codemx.anrdemo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codemx.anrdemo.anr.diagnostics.AnrLogTags
import com.codemx.anrdemo.anr.diagnostics.DiagnosticsRepository
import com.codemx.anrdemo.anr.dispatch.AnrScenarioDispatcher
import com.codemx.anrdemo.anr.dispatch.DeepLinkParser
import com.codemx.anrdemo.ui.AnrDemoScreen
import com.codemx.anrdemo.ui.theme.ANRDemoTheme

class MainActivity : ComponentActivity() {
    private lateinit var dispatcher: AnrScenarioDispatcher
    private lateinit var diagnosticsRepository: DiagnosticsRepository
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dispatcher = AnrScenarioDispatcher(this)
        diagnosticsRepository = DiagnosticsRepository(this)
        enableEdgeToEdge()
        setContent {
            ANRDemoTheme {
                AnrDemoScreen(dispatcher = dispatcher, diagnosticsRepository = diagnosticsRepository)
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val request = DeepLinkParser.parse(intent?.data) ?: return
        Log.d(AnrLogTags.TRIGGER, "Deep link request=$request")
        mainHandler.postDelayed({ dispatcher.dispatch(request) }, 500L)
    }
}
