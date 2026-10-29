package app.lumio.player

import android.content.Intent
import android.os.Bundle
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.lumio.player.ui.nav.AppRoot
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val _openPlayer = MutableStateFlow(0)
    private val openPlayer: StateFlow<Int> = _openPlayer
    private val ready = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !ready.value }
        lifecycleScope.launch { delay(1000); ready.value = true }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 120 Hz support: request max surface refresh rate for smoother animations
        window.setFrameRate(120f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
        window.insetsController?.hide(WindowInsets.Type.systemBars())
        applicationContext.container.player.connect()
        handleIntent(intent)
        setContent { AppRoot(openPlayer) { ready.value = true } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i == null) return
        val data = i.data
        if (i.action == Intent.ACTION_VIEW && data != null) {
            applicationContext.container.player.playUri(data)
            _openPlayer.value = _openPlayer.value + 1
        } else if (i.getBooleanExtra("open_now_playing", false)) {
            _openPlayer.value = _openPlayer.value + 1
        }
    }
}
