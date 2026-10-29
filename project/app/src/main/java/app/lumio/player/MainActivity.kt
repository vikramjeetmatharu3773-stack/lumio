package app.lumio.player

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.lumio.player.ui.nav.AppRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    private val _openPlayer = MutableStateFlow(0)
    private val openPlayer: StateFlow<Int> = _openPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applicationContext.container.player.connect()
        handleIntent(intent)
        setContent { AppRoot(openPlayer) }
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
